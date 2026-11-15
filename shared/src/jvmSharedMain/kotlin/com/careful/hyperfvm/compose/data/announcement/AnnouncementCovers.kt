package com.careful.hyperfvm.compose.data.announcement

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.createDataImageDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 合集列表的封面缩略图。
 *
 * 合集接口给的是横版封面（约 3:1，640×188 / 2350×691 混着来），列表里占 148×74dp：
 * 高密度屏上需要约 444px 宽，而按 2:1 裁切后只有 300px 的图会发虚，所以统一换
 * `@720w.jpg` 规格下载（解码后 720×~212，一张几十 KB），内存里留最近 20 张。
 *
 * 文件落在 `cacheDir/announcement/covers/`，与正文配图目录分开——
 * [AnnouncementImages.prune] 换期只清 `images/`，封面是列表的门面，不能跟着删。
 */
object AnnouncementCovers {

    /** 与正文配图同款规格前缀：转成指定宽度、JPEG 编码。 */
    private const val SpecSuffix = "@720w.jpg"

    private const val MemoryEntries = 20

    private const val UserAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 20_000

    private val memory = object : LruCache<String, ImageBitmap>(MemoryEntries) {}

    /** 两端同一套区域解码器；封面是小图，直接整图解码。 */
    private val decoder = createDataImageDecoder()

    /** 内存里已经解好的封面，滚动回来时不用再读盘。 */
    fun cached(url: String): ImageBitmap? = memory.get(url)

    /** 命中内存直接返回；否则读盘或下载。失败返回 null，列表显示占位图标。 */
    suspend fun load(env: AppEnv, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!url.startsWith("http")) return@withContext null
        memory.get(url)?.let { return@withContext it }

        val file = cacheFile(env, url)
        if (file.length() <= 0L) {
            // 先写 .part 再改名：中途被杀掉时不会留下半张图让解码器反复失败。
            val temp = File(file.parentFile, "${file.name}.${System.nanoTime()}.part")
            try {
                file.parentFile?.mkdirs()
                download(specUrl(url), temp)
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
            } catch (_: Exception) {
                temp.delete()
                return@withContext null
            }
        }

        val bitmap = try {
            decoder.decodeFull(file.path)
        } catch (_: Exception) {
            null
        }
        if (bitmap == null) {
            // 下坏了（比如服务器回了错误页）就删掉，下次重新下。
            file.delete()
        } else {
            memory.put(url, bitmap)
        }
        bitmap
    }

    /** 缓存路径：原图文件名就是内容哈希，不会重名。 */
    private fun cacheFile(env: AppEnv, url: String): File {
        val baseName = url.substringBefore('@').substringAfterLast('/').substringBefore('.')
        val safeName = baseName
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .ifEmpty { url.hashCode().toString() }
        // 文件名带上规格：调大过封面后，上一版 300w 的旧图不会被当成命中继续发虚。
        val spec = SpecSuffix.removePrefix("@").substringBefore('.')
        return File(directory(env), "$safeName.$spec.jpg")
    }

    private fun directory(env: AppEnv): File =
        File(File(env.cacheDir, "announcement"), "covers")

    /** `https://.../xxx.png` → `https://.../xxx.png@720w.jpg`。 */
    private fun specUrl(url: String): String =
        url.substringBefore('@').replaceFirst("http://", "https://") + SpecSuffix

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
