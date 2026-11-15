package com.careful.hyperfvm.compose.data.image

import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 云端清单里的一条：一张图（或它的深浅色变体）的文件名、版本号与下载地址。
 *
 * 实现 [java.io.Serializable] 是为了能整批塞进 Intent 传给下载服务；
 * 清单只有百来条，序列化开销可以忽略。
 */
data class DataImageRemoteInfo(
    val fileName: String,
    val version: Long,
    val url: String,
) : java.io.Serializable

/**
 * 数据图的本地仓储：文件定位、版本号持久化、云端清单拉取与下载计划计算。
 *
 * 沿用 Java 版的存储约定，老用户升级后已下载的图片可以直接复用：
 * 图片放在 `filesDir/data_images/<基础名>[_dark|_light].png`，版本号存 SharedPreferences。
 */
object DataImageStore {

    /** 图片目录名，与 Java 版一致。 */
    private const val DirectoryName = "data_images"

    private const val PrefsName = "data_images"
    private const val VersionKey = "image_resources_version_code"

    /** 版本号 0：从没下载过。 */
    const val VersionNeverDownloaded = 0L

    /** 版本号 1：用户主动删除了全部图片。 */
    const val VersionDeleted = 1L

    /** 远端清单地址，与 Java 版 `DataImageIndexActivity.DATA_IMAGE_URL` 相同。 */
    private const val RemoteListUrl =
        "https://raw.giteeusercontent.com/phantom-careful/hyper-fvm-updater/raw/main/DataImages/DataImagesUrl.m3u"

    private const val ConnectTimeoutMs = 15_000
    private const val ReadTimeoutMs = 15_000

    private val _revision = MutableStateFlow(0L)

    /**
     * 本地文件集合的版本号：下载完成、删除图片后自增。
     *
     * 缩略图与「这张图是否已下载」都从磁盘现读，靠这个信号触发页面重算缓存，
     * 避免每帧都去 `File.exists()` 扫一遍目录。
     */
    val revision: StateFlow<Long> = _revision.asStateFlow()

    fun directory(env: AppEnv): File = File(env.filesDir, DirectoryName)

    /** 通知界面磁盘内容已变（服务在后台写完文件时调用）。 */
    fun notifyFilesChanged() {
        _revision.value += 1
    }

    // ==================== 版本号 ====================

    fun localVersion(env: AppEnv): Long = env.prefs(PrefsName).getLong(VersionKey, VersionNeverDownloaded)

    fun setLocalVersion(env: AppEnv, version: Long) {
        val prefs = env.prefs(PrefsName)
        prefs.putLong(VersionKey, version)
        prefs.commit()
        notifyFilesChanged()
    }

    // ==================== 文件定位 ====================

    /**
     * 解析一张基础图对应的本地文件。
     *
     * 与 Java 版 `DataImageViewerHelper` 一致：先找当前深浅色对应的变体，
     * 找不到再回退到不带后缀的基础图（部分数据图没有深浅色双版本）。
     */
    fun resolveFile(env: AppEnv, name: String, dark: Boolean): File? {
        val dir = directory(env)
        val themed = File(dir, name + if (dark) "_dark.png" else "_light.png")
        if (themed.isFile) return themed
        val plain = File(dir, name + ".png")
        return plain.takeIf { it.isFile }
    }

    /** 某条云端记录（含变体后缀的完整文件名）是否已经落在本地。 */
    fun exists(env: AppEnv, fileName: String): Boolean =
        File(directory(env), "$fileName.png").isFile

    /** 一张基础图是否有可用的本地文件。 */
    fun hasImage(env: AppEnv, name: String): Boolean =
        resolveFile(env, name, dark = true) != null || resolveFile(env, name, dark = false) != null

    /**
     * 云端文件名是否属于某张基础图。
     *
     * 必须按「完全相等 或 加变体后缀后相等」判断，不能用前缀：
     * `data_image_card_12_` 是一张独立的数据图，用前缀会把 `data_image_card_12` 也算进去。
     */
    fun matchesBase(fileName: String, baseName: String): Boolean =
        fileName == baseName || fileName == "${baseName}_dark" || fileName == "${baseName}_light"

    // ==================== 远端清单 ====================

    /**
     * 拉取云端清单。网络异常、JSON 结构异常都会抛出来，由调用方展示错误态。
     */
    fun fetchRemoteList(): List<DataImageRemoteInfo> {
        val connection = URL(RemoteListUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.requestMethod = "GET"
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("内容获取失败，请检查网络后重试（HTTP ${connection.responseCode}）")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            val entries = buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val version = item.getString("version").toLongOrNull() ?: 0L
                    add(
                        DataImageRemoteInfo(
                            fileName = item.getString("fileName"),
                            version = version,
                            url = item.getString("url"),
                        ),
                    )
                }
            }
            // 清单与 Java 版共用，其中有几张只给旧版入口（如大赛任务图 2–4 号）。
            // 目录里没有的图不下载、也不计入「需要更新」，免得白占一份空间。
            return entries.filter { info -> AllDataImages.any { matchesBase(info.fileName, it.name) } }
        } finally {
            connection.disconnect()
        }
    }

    /** 全量清单里的最大版本号：只有这一批全部下载成功才落库。 */
    fun newestVersion(remote: List<DataImageRemoteInfo>): Long =
        remote.maxOfOrNull { it.version } ?: VersionNeverDownloaded

    /**
     * 计算要下载的清单。
     *
     * [incremental] 为 true 时只挑「云端版本比本地新」或「本地文件缺失」的条目；
     * 为 false 时整份清单都下。增量里带上文件缺失判断，是为了兜住「用户手动清理了目录
     * 但版本号还留着」的情况，否则那几张图会永远显示为未下载且无法补下。
     */
    fun buildDownloadPlan(
        env: AppEnv,
        remote: List<DataImageRemoteInfo>,
        incremental: Boolean,
    ): List<DataImageRemoteInfo> {
        if (!incremental) return remote
        val localVersion = localVersion(env)
        return remote.filter { info ->
            info.version > localVersion || !exists(env, info.fileName)
        }
    }

    /** 删除全部图片，版本号置为「已删除」。 */
    fun deleteAll(env: AppEnv) {
        val dir = directory(env)
        dir.listFiles()?.forEach { if (it.isFile) it.delete() }
        setLocalVersion(env, VersionDeleted)
    }

    /** 清理下载中被取消留下的临时文件。 */
    fun deleteTempFiles(env: AppEnv) {
        directory(env).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".tmp") }
            ?.forEach { it.delete() }
    }
}
