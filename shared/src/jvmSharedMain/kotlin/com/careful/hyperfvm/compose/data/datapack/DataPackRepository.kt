package com.careful.hyperfvm.compose.data.datapack

import com.careful.hyperfvm.compose.data.catalog.CatalogKind
import com.careful.hyperfvm.compose.data.catalog.CatalogRepository
import com.careful.hyperfvm.compose.data.itemQuery.ItemQueryRepository
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDataRepository
import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** 数据包的加载状态，首次启动界面据此渲染。 */
sealed interface DataPackState {

    /** 正在检查本地是否已安装数据包。 */
    data object Checking : DataPackState

    /** 本地没有数据包：首次使用，或数据被清理过。 */
    data object Missing : DataPackState

    /**
     * 正在下载数据包。[total] 为 0 表示版本文件没给出大小，
     * 界面只显示已下载的字节数。
     */
    data class Downloading(val received: Long, val total: Long) : DataPackState

    /** 压缩包已解压，正在准备道具索引与套装 / 翅膀数据，并下载对应图片。 */
    data object Preparing : DataPackState

    /** 数据包就绪，可以进入主界面。 */
    data object Ready : DataPackState

    /** 下载、校验或解压失败：[message] 直接展示给用户，附带重试入口。 */
    data class Error(val message: String) : DataPackState
}

/**
 * 数据包仓储：所有随包分发的图片与数据（防御卡图鉴、美食数据、概率公示、
 * 套装 / 翅膀条目文件）都不再打进安装包，而是从 Gitee 仓库下载一个 zip。
 *
 * 目录布局（zip 内与本地一致）：
 * ```
 * manifest.json                   版本信息，{ version, builtAt, fileCount }
 * data/card_desc.xml              官方道具表（套装 / 翅膀条目来源）
 * data/market_item.xml            官方续费表
 * data/odds_boxes.json            宝箱概率公示
 * data/odds_draws.json            抽奖概率公示
 * data/odds_sources.json          官方公示合集（概率公示页的来源列表）
 * images/{id}.webp|png            全部图片，id 即原来的资源名
 * ```
 *
 * 版本文件放在仓库根目录（版本信息很小，raw 直链不触发鉴权），
 * 数据包本体则放在 Releases 附件里——Gitee 对超过 10 MB 的仓库文件强制要求登录，
 * 附件地址（`/releases/download/...`）不需要。下载地址由版本文件的 url 字段给出，
 * 换版本只需重新发一个 Release 并改版本文件。
 *
 * 安装是「解压到 staging → 整体替换」的原子过程，中途失败不会破坏已有数据包。
 */
object DataPackRepository {

    /** 版本文件地址；仓库是发布公告 / 数据图共用的 hyper-fvm-updater。 */
    const val VersionUrl =
        "https://raw.giteeusercontent.com/phantom-careful/hyper-fvm-updater/raw/main/hyperfvm_data_version.json"

    /** 同一仓库的备用 raw 域名：主域名被 CDN 拦截时退到这里。 */
    private const val VersionUrlFallback =
        "https://gitee.com/phantom-careful/hyper-fvm-updater/raw/main/hyperfvm_data_version.json"

    private const val ConnectTimeoutMs = 15_000
    private const val ReadTimeoutMs = 60_000

    private val _state = MutableStateFlow<DataPackState>(DataPackState.Checking)
    val state: StateFlow<DataPackState> = _state.asStateFlow()

    /**
     * 下载 / 准备工作是否正在进行（不含失败）的布尔流。
     *
     * 进度字节每 64 KB 刷一次，普通订阅会跟着重组；这里先收敛成布尔值再去重，
     * 导航层用它做返回拦截时只在「开始 / 结束」两档变化。
     */
    val running: Flow<Boolean> = state
        .map { it is DataPackState.Downloading || it == DataPackState.Preparing }
        .distinctUntilChanged()

    /** 下载与安装不能并发：重复点「下载」只跑一次。 */
    private val mutex = Mutex()

    /**
     * 合集资源是否正在准备。
     *
     * Preparing 阶段的协程跟着 Activity 走，重建时会被取消；[check] 靠这个标志
     * 判断残留的 Preparing 是「还在跑」还是「被中断了」，后者重新补一次。
     */
    @Volatile
    private var preparing = false

    /** 本次进程是否已完成本地索引与合集准备；Activity 重建会再调 [check]，靠它不重复建。 */
    @Volatile
    private var initialized = false

    fun packDir(env: AppEnv): File = File(env.filesDir, "datapack")

    fun dataDir(env: AppEnv): File = File(packDir(env), "data")

    fun imageDir(env: AppEnv): File = File(packDir(env), "images")

    /** 数据包内的一份数据文件；未安装或不存在时返回 null。 */
    fun dataFile(env: AppEnv, name: String): File? =
        File(dataDir(env), name).takeIf { it.isFile }

    /** 本地是否已安装数据包（以 manifest.json 为标记）。 */
    fun isInstalled(env: AppEnv): Boolean = File(packDir(env), "manifest.json").isFile

    /** 本地数据包版本号；未安装返回 0。 */
    fun installedVersion(env: AppEnv): Int = runCatching {
        JSONObject(File(packDir(env), "manifest.json").readText()).optInt("version")
    }.getOrDefault(0)

    /**
     * 在首帧之前同步定下初始状态：本地已安装就是 [DataPackState.Ready]，否则是
     * [DataPackState.Missing]。只做 manifest 存在性检查与图片索引（列一次文件名），
     * 不碰网络也不解析概率表；那些交给后台的 [check]。
     *
     * 没有这一步时状态永远是 [DataPackState.Checking]，日常启动会先闪一帧「正在检查」；
     * 图片索引也一并建好，否则主界面首帧会有一瞬看到包里图片没加载出来。
     */
    fun prime(env: AppEnv) {
        if (_state.value != DataPackState.Checking) return
        if (!isInstalled(env)) {
            _state.value = DataPackState.Missing
            return
        }
        installImageIndex(env)
        _state.value = DataPackState.Ready
    }

    /**
     * 进程启动时调用一次：本地已安装就直接就绪（图片索引立即可用），
     * 否则进入 [DataPackState.Missing]，由首启界面引导下载。
     *
     * 数据包在、但服装 / 翅膀还没备齐（老版本装的包、上次准备被中断）时，
     * 先在 [DataPackState.Preparing] 里补齐再就绪，进合集页就不用再临时下载。
     *
     * 就绪前后台预热道具查询表（数据包没换时只是读回上次的磁盘缓存）。
     *
     * 下载 / 准备中的状态不会被重置（Activity 重建会再调一次），
     * 失败状态也保留到用户主动重试。
     */
    suspend fun check(env: AppEnv) {
        if (initialized) return
        when (_state.value) {
            DataPackState.Preparing -> if (preparing) return
            is DataPackState.Downloading, is DataPackState.Error -> return
            else -> Unit
        }
        withContext(Dispatchers.IO) {
            if (!isInstalled(env)) {
                // 索引与概率表都清空：首次启动时它们本来就是空的，走到这里多是为了兼容数据被清理。
                installIndex(env)
                _state.value = DataPackState.Missing
                initialized = true
                return@withContext
            }
            installIndex(env)
            // 道具表解析量大，后台提前做好，用户第一次查询就是秒出。
            ItemQueryRepository.warmUp(env)
            val prepared = CatalogRepository.isPrepared(env, CatalogKind.Clothing) &&
                CatalogRepository.isPrepared(env, CatalogKind.Wing)
            if (!prepared) {
                _state.value = DataPackState.Preparing
                prepare(env)
            }
            _state.value = DataPackState.Ready
            initialized = true
        }
    }

    /** 准备两个合集的条目与预览图；被取消时靠 finally 复位，下次启动重新补。 */
    private suspend fun prepare(env: AppEnv) {
        preparing = true
        try {
            CatalogRepository.ensureAll(env)
        } finally {
            preparing = false
        }
    }

    /**
     * 建立「图片 id → 文件」索引并清空内存缓存；顺带重新解析数据包内的概率公示表。
     *
     * 磁盘上的道具索引不动：它由文件头的版本 / 大小 / 修改时间校验自动失效，
     * 启动时没必要每次重写。
     */
    private fun installIndex(env: AppEnv) {
        installImageIndex(env)
        OddsDataRepository.load(env)
        ItemQueryRepository.clear()
    }

    /** 只重建图片索引：列一遍 images 目录的文件名，不解析概率表，启动首帧前也能跑。 */
    private fun installImageIndex(env: AppEnv) {
        val index = imageDir(env).listFiles()
            ?.filter { it.isFile }
            ?.associateBy { it.name.substringBeforeLast('.') }
            ?: emptyMap()
        PackImages.install(index)
    }

    /**
     * 下载并安装数据包，随后用包内的两个 XML 构建套装 / 翅膀数据并下载预览图。
     *
     * 任何网络 / 校验 / 解压失败都收敛成 [DataPackState.Error]，不向界面抛异常；
     * 套装数据准备失败不回滚数据包状态——数据包本身已可用，合集页有自己的错误与重试。
     *
     * 装完在「准备数据」阶段就把道具表索引建好落盘，新包第一次检索直接读盘；
     * 合集素材与索引并行准备，两者都完成才算就绪。
     */
    suspend fun download(env: AppEnv) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    _state.value = DataPackState.Downloading(0, 0)
                    val meta = fetchVersion()
                    val total = meta.optLong("size")
                    val urls = buildList {
                        meta.optString("url").takeIf { it.isNotBlank() }?.let(::add)
                        meta.optJSONArray("mirrors")?.let { mirrors ->
                            for (index in 0 until mirrors.length()) {
                                mirrors.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                    }
                    if (urls.isEmpty()) error("版本文件里没有下载地址")

                    val zip = File(env.cacheDir, "hyperfvm_data.zip")
                    var lastError: Exception? = null
                    for (url in urls) {
                        try {
                            downloadZip(withCacheBuster(url), zip, total) { received ->
                                _state.value = DataPackState.Downloading(received, total)
                            }
                            lastError = null
                            break
                        } catch (e: Exception) {
                            lastError = e
                        }
                    }
                    lastError?.let { throw it }

                    val expected = meta.optString("sha256")
                    if (expected.isNotBlank() && !expected.equals(sha256Of(zip), ignoreCase = true)) {
                        zip.delete()
                        error("数据包校验失败，请重新下载")
                    }

                    installZip(env, zip)
                    zip.delete()

                    // 数据包换了，旧的条目缓存与就绪标记作废（XML 可能已更新）；预览图按 id 复用，不必删。
                    CatalogRepository.invalidate(env)
                    installIndex(env)
                    ItemQueryRepository.invalidate(env)

                    _state.value = DataPackState.Preparing
                    // 道具表索引和合集素材一起准备：等这里做完，Ready 后第一次检索已经是读盘命中。
                    coroutineScope {
                        val index = launch { ItemQueryRepository.prebuild(env) }
                        prepare(env)
                        index.join()
                    }
                    _state.value = DataPackState.Ready
                    initialized = true
                } catch (e: CancellationException) {
                    // Activity 重建 / 退出页面：不是错误，但状态不能停在 Downloading / Preparing——
                    // 旧协程已死，没人会把进度推下去，界面会卡死且锁住返回。
                    // 按本地是否存在数据包退回可重试的状态：首次安装回到 Missing，
                    // 已装包的更新回到 Ready（下次进更新页重新开始）。
                    _state.value = if (isInstalled(env)) DataPackState.Ready else DataPackState.Missing
                    throw e
                } catch (e: Exception) {
                    _state.value = DataPackState.Error(friendlyMessage(e))
                }
            }
        }
    }

    // ==================== 版本文件与下载 ====================

    /**
     * 云端是否发布了比本地更新的数据包，供仪表盘的更新提示卡使用。
     *
     * 只比版本号、不看文件：版本文件里的 `version` 比本地 manifest 大就提示更新。
     * 网络或解析失败原样抛出，由调用方决定要不要保留上一次的结果。
     */
    suspend fun hasNewVersion(env: AppEnv): Boolean = withContext(Dispatchers.IO) {
        fetchVersion().optInt("version") > installedVersion(env)
    }

    private fun fetchVersion(): JSONObject {
        var lastError: Exception? = null
        for (url in listOf(VersionUrl, VersionUrlFallback)) {
            try {
                return JSONObject(downloadText(withCacheBuster(url)))
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("版本文件获取失败")
    }

    /** raw.giteeusercontent 与 gitee.com/raw 都有 CDN 缓存，版本号 / 包都带时间戳拿最新。 */
    private fun withCacheBuster(url: String): String {
        val separator = if (url.contains('?')) '&' else '?'
        return "$url${separator}t=${System.currentTimeMillis()}"
    }

    private fun downloadText(url: String): String {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) {
                error("内容获取失败，请检查网络后重试（HTTP ${connection.responseCode}）")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** 下载到 `.part` 再改名：中途失败不会留下半个 zip。 */
    private fun downloadZip(url: String, target: File, expectedSize: Long, onProgress: (Long) -> Unit) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.part")
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) error("数据包下载失败（HTTP ${connection.responseCode}）")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize
            if (total > 0) onProgress(0)
            var received = 0L
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        onProgress(received)
                    }
                }
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = ConnectTimeoutMs
            readTimeout = ReadTimeoutMs
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }

    // ==================== 校验与解压 ====================

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 解压到 staging 目录，校验 manifest.json 存在后整体替换旧数据包。
     *
     * 压缩包来自自己维护的仓库，仍做一次路径校验：条目路径跑出目标目录的一律跳过。
     */
    private fun installZip(env: AppEnv, zip: File) {
        val staging = File(env.cacheDir, "datapack_staging")
        staging.deleteRecursively()
        staging.mkdirs()
        val root = staging.canonicalFile
        ZipInputStream(zip.inputStream().buffered()).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                val out = File(staging, entry.name)
                if (!out.canonicalFile.path.startsWith(root.path + File.separator)) {
                    stream.closeEntry()
                    continue
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { output -> stream.copyTo(output) }
                }
                stream.closeEntry()
            }
        }
        require(File(staging, "manifest.json").isFile) { "数据包内容不完整，请重新下载" }

        val target = packDir(env)
        if (target.exists() && !target.deleteRecursively()) {
            error("旧数据文件被占用，删除失败，请重启 App 后重试")
        }
        target.parentFile?.mkdirs()
        if (!staging.renameTo(target)) {
            staging.copyRecursively(target, overwrite = true)
            staging.deleteRecursively()
        }
    }

    // ==================== 错误文案 ====================

    private fun friendlyMessage(e: Exception): String = when (e) {
        is UnknownHostException, is ConnectException -> "网络连接失败，请检查网络后重试"
        is SocketTimeoutException -> "网络请求超时，请检查网络后重试"
        else -> e.message?.takeIf { it.isNotBlank() } ?: "数据包下载失败，请重试"
    }
}
