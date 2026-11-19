package com.careful.hyperfvm.compose.data.catalog

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import com.careful.hyperfvm.compose.platform.createDataImageDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 合集预览图的解码与缓存。
 *
 * 预览图是统一的小图（约 142×184，单张百来 KB），整图解码即可；
 * 缓存上限按像素字节数算，瀑布流来回滚动时不重复解码，也不会把几百张图全压在内存里。
 *
 * 缓存 key 带上文件的 lastModified：图片更新时同名覆盖，旧位图自然失效。
 */
object CatalogImages {

    private val decoder = createDataImageDecoder()

    private const val CacheBytes = 12 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(CacheBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /**
     * 解码任务表：同一张图只解一次。
     *
     * 解码跑在独立作用域里：卡片滚动出屏时调用方协程被取消，但解码已经开始的
     * 那张图会跑完并落进缓存——用户很可能马上又滚回来。
     */
    private val jobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(4))

    /** 同步取已解码的图，不解码；没命中返回 null。 */
    fun peek(file: File): ImageBitmap? = cache.get(fileKey(file))

    /** 读取一张预览图；失败返回 null，由卡片显示占位。 */
    suspend fun load(file: File): ImageBitmap? {
        val key = fileKey(file)
        cache.get(key)?.let { return it }
        val job = jobs.computeIfAbsent(key) {
            scope.async {
                try {
                    // 预览图多为带透明底的 WebP，走字节解码（ARGB_8888 / Skia）；
                    // 按路径的 decodeRegion 在 Android 上是 RGB_565，透明区域会变黑块。
                    cache.get(key) ?: runCatching { decoder.decodeBytes(file.readBytes()) }
                        .getOrNull()
                        ?.let { bitmap ->
                            cache.put(key, bitmap)
                            bitmap
                        }
                } finally {
                    // 成败都释放占位，失败后下次还能重试。
                    jobs.remove(key)
                }
            }
        }
        return job.await()
    }

    private fun fileKey(file: File): String = "${file.absolutePath}@${file.lastModified()}"
}
