package com.careful.hyperfvm.compose.data.image

import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** 一次数据图下载任务对外暴露的状态快照。 */
data class DataImageDownloadState(
    val phase: Phase = Phase.Idle,
    /** 已处理完的文件数（成功 + 失败）。 */
    val completed: Int = 0,
    val total: Int = 0,
    /** 0..100 的总体进度，含在飞文件的部分进度。 */
    val percent: Int = 0,
    val failedCount: Int = 0,
    /** 本批失败的文件名，用于「点击卡片重试失败项」。 */
    val failedNames: List<String> = emptyList(),
    /** 图片源不支持断点续传，恢复后已从头重下。 */
    val rangeRestarted: Boolean = false,
) {
    enum class Phase { Idle, Downloading, Paused, Success, Failed }

    /** 任务是否还在进行（含暂停态）。 */
    val isActive: Boolean get() = phase == Phase.Downloading || phase == Phase.Paused
}

/**
 * 下载任务的进程内单例状态：引擎负责写，界面负责读。
 *
 * 页面被销毁后下载继续在后台跑，重新进入页面时用 [state] 直接回显当前进度。
 */
object DataImageDownloadController {

    private val _state = MutableStateFlow(DataImageDownloadState())
    val state: StateFlow<DataImageDownloadState> = _state.asStateFlow()

    internal fun publish(state: DataImageDownloadState) {
        _state.value = state
    }

    /**
     * 真正干活的人。
     *
     * Android 交给前台服务（要通知栏进度与暂停/取消按钮），桌面端就地跑 [LocalDownloadRunner]。
     * 各端在入口处覆盖，默认值保证桌面端零配置即可用。
     */
    var runner: DataImageDownloadRunner = LocalDownloadRunner()

    /** 发起下载；[list] 是要下载的清单，[newestVersion] 是云端全量清单的最大版本号。 */
    fun start(env: AppEnv, list: List<DataImageRemoteInfo>, newestVersion: Long) {
        if (list.isEmpty()) return
        runner.start(env, list, newestVersion)
    }

    fun pause() = runner.pause()

    fun resume() = runner.resume()

    fun cancel() = runner.cancel()

    /**
     * 丢弃上一次任务留下的结果态（成功 / 失败），回到 [DataImageDownloadState.Phase.Idle]。
     *
     * 删除图片后调用：否则页面会一直停在上一次的「更新完成」上，看不见「需要重新下载」的提示。
     * 有任务在跑时什么都不做，避免把正在跑的进度抹掉。
     */
    fun clearResult() {
        if (!_state.value.isActive) _state.value = DataImageDownloadState()
    }
}

/** 下载任务的执行者，两端实现不同：前台服务 / 进程内线程池。 */
interface DataImageDownloadRunner {
    fun start(env: AppEnv, list: List<DataImageRemoteInfo>, newestVersion: Long)
    fun pause()
    fun resume()
    fun cancel()
}

/** 桌面端：进程内直接跑引擎。没有系统通知，进度只走界面。 */
class LocalDownloadRunner : DataImageDownloadRunner {

    @Volatile
    private var engine: DataImageDownloadEngine? = null

    override fun start(env: AppEnv, list: List<DataImageRemoteInfo>, newestVersion: Long) {
        if (engine?.isActive == true) return
        engine = DataImageDownloadEngine(env).also { it.start(list, newestVersion) }
    }

    override fun pause() = engine?.pause() ?: Unit

    override fun resume() = engine?.resume() ?: Unit

    override fun cancel() = engine?.cancel() ?: Unit
}

/** 暂停 / 继续 / 取消三态阀门，工作线程在每次读流前查询。 */
internal class DownloadControl {
    private val lock = ReentrantLock()
    private val resumed = lock.newCondition()

    @Volatile
    private var paused = false

    @Volatile
    private var cancelled = false

    fun pause() = lock.withLock { paused = true }

    fun resume() = lock.withLock {
        paused = false
        resumed.signalAll()
    }

    fun cancel() = lock.withLock {
        cancelled = true
        paused = false
        resumed.signalAll()
    }

    val isCancelled: Boolean get() = cancelled

    val isPaused: Boolean get() = paused

    /** 阻塞到恢复为止；返回 true 表示期间被取消了。 */
    fun awaitResumed(): Boolean = lock.withLock {
        while (paused && !cancelled) {
            resumed.await()
        }
        cancelled
    }
}

/**
 * 数据图下载引擎：4 个并发线程、单文件 Range 断点续传、暂停/继续/取消。
 *
 * 与 Java 版 `DownloadService` 的数据图分支对齐，但把「下载」与「通知栏」彻底拆开：
 * 引擎只管字节与状态，[onProgress] / [onFinished] 由宿主决定要不要展示（Android 是通知，
 * 桌面端不需要）。只有整批全部成功才把「云端清单的最大版本号」落库。
 */
class DataImageDownloadEngine(
    private val env: AppEnv,
    private val onProgress: (DataImageDownloadState) -> Unit = {},
    private val onFinished: (DataImageDownloadState) -> Unit = {},
) {

    private companion object {
        const val ThreadCount = 4
        const val ProgressPublishIntervalMs = 300L
        const val ReadBufferSize = 32 * 1024
    }

    private val control = DownloadControl()
    private val executor = Executors.newFixedThreadPool(ThreadCount)

    /** 槽位=文件下标，记录每个在飞文件的千分比进度，用来聚合平滑的总体进度。 */
    private val inflightPercents = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    private val failedInfos: MutableList<DataImageRemoteInfo> = Collections.synchronizedList(mutableListOf())

    private val completedCount = AtomicInteger(0)

    @Volatile
    private var totalCount = 0

    @Volatile
    private var newestVersion = 0L

    @Volatile
    private var phase = DataImageDownloadState.Phase.Idle

    @Volatile
    private var rangeRestarted = false

    @Volatile
    private var lastPublishAt = 0L

    @Volatile
    private var worker: Thread? = null

    val isActive: Boolean get() = phase == DataImageDownloadState.Phase.Downloading || phase == DataImageDownloadState.Phase.Paused

    fun start(list: List<DataImageRemoteInfo>, newestVersion: Long) {
        // 已有任务在跑时忽略重复启动（防止连点重复下载）。
        if (worker != null) return
        if (list.isEmpty()) {
            phase = DataImageDownloadState.Phase.Idle
            DataImageDownloadController.publish(DataImageDownloadState())
            return
        }

        this.newestVersion = newestVersion
        totalCount = list.size
        completedCount.set(0)
        inflightPercents.clear()
        failedInfos.clear()
        rangeRestarted = false
        phase = DataImageDownloadState.Phase.Downloading

        publish(force = true)
        worker = Thread({ runBatch(list) }, "data-image-download").apply { start() }
    }

    fun pause() {
        if (phase != DataImageDownloadState.Phase.Downloading) return
        control.pause()
        phase = DataImageDownloadState.Phase.Paused
        publish(force = true)
    }

    fun resume() {
        if (phase != DataImageDownloadState.Phase.Paused) return
        control.resume()
        phase = DataImageDownloadState.Phase.Downloading
        publish(force = true)
    }

    fun cancel() {
        if (worker == null) return
        control.cancel()
    }

    /** 宿主销毁时调用：停掉线程池，避免线程泄漏。 */
    fun shutdown() {
        control.cancel()
        executor.shutdownNow()
    }

    // ==================== 批任务 ====================

    private fun runBatch(list: List<DataImageRemoteInfo>) {
        val dir = DataImageStore.directory(env)
        if (!dir.exists()) dir.mkdirs()

        try {
            // 结果列表与输入一一对应，失败时才能准确记录是哪一张没有下成。
            val futures = list.mapIndexed { index, info ->
                executor.submit<Boolean> { downloadOne(info, index) }
            }
            futures.forEachIndexed { index, future ->
                val success = try {
                    future.get()
                } catch (_: Exception) {
                    // 单个任务异常按失败计，不能让整批卡死。
                    false
                }
                if (control.isCancelled) return@forEachIndexed
                if (!success) failedInfos.add(list[index])
                completedCount.incrementAndGet()
                publish(force = false)
            }
        } finally {
            executor.shutdown()
        }

        if (control.isCancelled) {
            DataImageStore.deleteTempFiles(env)
            DataImageDownloadController.publish(DataImageDownloadState())
            onFinished(DataImageDownloadState())
            return
        }

        // 全部成功才落库：失败重试时这批只是全量的子集，按子集算版本号会低估，下次进页面会误报更新。
        if (failedInfos.isEmpty()) {
            DataImageStore.setLocalVersion(env, newestVersion)
        }
        DataImageStore.notifyFilesChanged()

        phase = if (failedInfos.isEmpty()) {
            DataImageDownloadState.Phase.Success
        } else {
            DataImageDownloadState.Phase.Failed
        }
        val final = publish(force = true)
        onFinished(final)
    }

    // ==================== 单文件下载 ====================

    /**
     * 下载一张图，内部循环处理暂停与断点续传。
     *
     * @return true 表示成功（临时文件已改名为正式文件），false 表示失败或被取消。
     */
    private fun downloadOne(info: DataImageRemoteInfo, index: Int): Boolean {
        val dir = DataImageStore.directory(env)
        val temp = File(dir, info.fileName + ".tmp")
        val target = File(dir, info.fileName + ".png")

        try {
            while (true) {
                if (control.awaitResumed()) return false

                when (val result = transfer(info, temp, index)) {
                    TransferResult.Success -> {
                        if (target.exists()) target.delete()
                        return temp.renameTo(target)
                    }
                    TransferResult.Cancelled -> return false
                    // 暂停后回到循环顶部等恢复，恢复后从临时文件断点续传。
                    TransferResult.Paused -> continue
                    TransferResult.Failed -> return false
                }
            }
        } finally {
            inflightPercents.remove(index)
        }
    }

    private enum class TransferResult { Success, Paused, Cancelled, Failed }

    private fun transfer(info: DataImageRemoteInfo, temp: File, index: Int): TransferResult {
        var connection: HttpURLConnection? = null
        var output: FileOutputStream? = null
        try {
            val existing = if (temp.isFile) temp.length() else 0L
            connection = (URL(info.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
                connect()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return TransferResult.Failed

            val append = responseCode == HttpURLConnection.HTTP_PARTIAL && existing > 0
            if (existing > 0 && !append) {
                // 图片源不支持断点续传：已下载的字节作废，从头重下并告知界面。
                rangeRestarted = true
                inflightPercents[index] = 0
                publish(force = true)
            }

            val contentLength = connection.contentLengthLong
            val totalBytes = if (contentLength > 0) contentLength + if (append) existing else 0L else -1L
            var downloaded = if (append) existing else 0L

            output = FileOutputStream(temp, append)
            connection.inputStream.use { input ->
                val buffer = ByteArray(ReadBufferSize)
                while (true) {
                    if (control.isCancelled) {
                        output.flush()
                        return TransferResult.Cancelled
                    }
                    if (control.isPaused) {
                        output.flush()
                        return TransferResult.Paused
                    }
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0) {
                        inflightPercents[index] = (downloaded * 1000 / totalBytes).toInt()
                        publish(force = false)
                    }
                }
            }
            output.flush()
            // 大小对不上（连接被中途掐断）按失败处理，避免把半截文件改名成正式图。
            if (totalBytes > 0 && downloaded != totalBytes) return TransferResult.Failed
            return TransferResult.Success
        } catch (_: Exception) {
            return when {
                control.isCancelled -> TransferResult.Cancelled
                // 暂停时主动断开连接也会抛异常，这种情况按暂停而不是失败处理，
                // 否则一张几十 MB 的图会因为暂停被记为「下载失败」。
                control.isPaused -> TransferResult.Paused
                else -> TransferResult.Failed
            }
        } finally {
            try {
                output?.close()
            } catch (_: Exception) {
            }
            connection?.disconnect()
        }
    }

    // ==================== 状态 ====================

    private fun publish(force: Boolean): DataImageDownloadState {
        if (control.isCancelled) return DataImageDownloadController.state.value
        val now = System.currentTimeMillis()
        if (!force && now - lastPublishAt < ProgressPublishIntervalMs) {
            return DataImageDownloadController.state.value
        }
        lastPublishAt = now

        val completed = completedCount.get()
        val inflightSum = inflightPercents.values.sum()
        val percent = if (totalCount > 0) {
            ((completed * 1000 + inflightSum) / totalCount / 10).coerceIn(0, 100)
        } else {
            0
        }

        val state = DataImageDownloadState(
            phase = phase,
            completed = completed,
            total = totalCount,
            percent = percent,
            failedCount = failedInfos.size,
            // 复制一份：服务重跑批任务时会 clear() 原列表，快照不能跟着变。
            failedNames = failedInfos.map { it.fileName },
            rangeRestarted = rangeRestarted,
        )
        DataImageDownloadController.publish(state)
        onProgress(state)
        return state
    }
}
