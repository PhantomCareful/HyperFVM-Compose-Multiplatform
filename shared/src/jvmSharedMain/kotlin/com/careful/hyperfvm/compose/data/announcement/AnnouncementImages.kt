package com.careful.hyperfvm.compose.data.announcement

import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 公告长图的本地缓存。
 *
 * 官方公告基本是 900 宽、动辄两千高的长图，十几张全量解码会把内存顶满；
 * 统一换成 B 站图片服务的 `@720w.jpg` 规格（一张约 200KB）再交给
 * SubSamplingImage 按区域解码，滚动时就只解当前视口那一块。
 *
 * 文件放在 `cacheDir/announcement/images/<公告 id>_<原图名>.jpg`：带公告 id
 * 是为了换公告时能把上一份一次清掉，其余交给系统的缓存回收。
 */
object AnnouncementImages {

    private const val DirectoryName = "images"

    /** B 站图片服务的规格后缀：转成 720 宽、JPEG 编码（保证区域解码可用）。 */
    private const val SpecSuffix = "@720w.jpg"

    private const val UserAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 30_000

    /** 超过这个时间的 .part 视为下载途中被杀掉留下的垃圾，清理时一并删掉。 */
    private const val PartMaxAgeMs = 30 * 60 * 1000L

    /**
     * 删掉不属于 [keepIds] 这几期公告的图片，避免每期都留一份在缓存里；
     * 本期的 .part 只有放了很久（下载中断）才删，免得误杀正在写的临时文件。
     *
     * 允许传多个 id：从合集里翻历史公告时，要同时留住当前这篇与「最新公告」那篇，
     * 两边共用同一个图片缓存目录。
     */
    suspend fun prune(env: AppEnv, keepIds: Set<String>) = withContext(Dispatchers.IO) {
        val partExpiry = System.currentTimeMillis() - PartMaxAgeMs
        directory(env).listFiles()?.forEach { file ->
            val stalePart = file.name.endsWith(".part") && file.lastModified() < partExpiry
            val staleImage = keepIds.none { keepId -> file.name.startsWith("${keepId}_") }
            if (staleImage || stalePart) file.delete()
        }
    }

    /** 读本地缓存，没有就下载；失败返回 null，由界面留占位。 */
    suspend fun load(env: AppEnv, announcementId: String, imageUrl: String): File? =
        withContext(Dispatchers.IO) {
            val file = cacheFile(env, announcementId, imageUrl)
            if (file.length() > 0L) return@withContext file

            // 临时名带唯一后缀：同一张图被重复请求时两边不会把同一个 .part 写坏。
            val temp = File(file.parentFile, "${file.name}.${System.nanoTime()}.part")
            try {
                file.parentFile?.mkdirs()
                download(specUrl(imageUrl), temp)
                // 先写 .part 再改名：中途被杀掉时不会留下半张图让解码器反复失败。
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
                file
            } catch (_: Exception) {
                temp.delete()
                null
            }
        }

    /** 缓存路径；同一期公告里原图名就是内容哈希，不会重名。 */
    private fun cacheFile(env: AppEnv, announcementId: String, imageUrl: String): File {
        val baseName = imageUrl.substringBefore('@').substringAfterLast('/')
        val safeName = buildString {
            baseName.forEach { char ->
                append(if (char.isLetterOrDigit() || char == '.' || char == '_' || char == '-') char else '_')
            }
        }.ifEmpty { "image" }
        return File(directory(env), "${announcementId}_$safeName.jpg")
    }

    private fun directory(env: AppEnv): File =
        File(File(env.cacheDir, "announcement"), DirectoryName)

    /** `https://.../xxx.png` → `https://.../xxx.png@720w.jpg`。 */
    private fun specUrl(imageUrl: String): String =
        imageUrl.substringBefore('@').replaceFirst("http://", "https://") + SpecSuffix

    private fun download(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.setRequestProperty("User-Agent", UserAgent)
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }
}
