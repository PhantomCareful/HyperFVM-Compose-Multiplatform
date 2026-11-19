package com.careful.hyperfvm.compose.data.datapack

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
import java.util.concurrent.atomic.AtomicReference

/**
 * 数据包内图片的解码与缓存。
 *
 * 数据包里的图以 WebP / PNG 存储，尺寸都不大（44×44、40×50、200×175 等），
 * 统一按内存字节解码：Android 走 `BitmapFactory`，桌面走 Skia——JDK 自带的 ImageIO
 * 没有 WebP 读器，不能用 `decodeRegion` 那条按文件路径走的通道。
 *
 * 缓存上限按像素字节数算；数据包安装 / 卸载时调用 [install] 整体替换索引并清空缓存。
 */
object PackImages {

    private val decoder = createDataImageDecoder()

    private const val CacheBytes = 8 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(CacheBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** 图片 id（文件名去扩展名）→ 文件。数据包未就绪时为空表。 */
    private val files = AtomicReference<Map<String, File>>(emptyMap())

    /** 解码任务表：同一张图只解一次。 */
    private val jobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(4))

    /** 安装（或替换）图片索引并清空旧缓存；随后所有 PackImage 立即改读新文件。 */
    fun install(index: Map<String, File>) {
        files.set(index)
        cache.evictAll()
    }

    /** 数据包未安装时返回 false，界面据此提示「先下载数据包」。 */
    fun isInstalled(): Boolean = files.get().isNotEmpty()

    fun file(id: String): File? = files.get()[id]

    /** 同步取已解码的图，不解码；没命中返回 null。 */
    fun peek(id: String): ImageBitmap? = cache.get(id)

    /** 读取一张数据包图片；失败返回 null，由界面显示空位。 */
    suspend fun load(id: String): ImageBitmap? {
        cache.get(id)?.let { return it }
        val target = file(id) ?: return null
        val job = jobs.computeIfAbsent(id) {
            scope.async {
                try {
                    cache.get(id) ?: runCatching { decoder.decodeBytes(target.readBytes()) }
                        .getOrNull()?.let { bitmap ->
                            cache.put(id, bitmap)
                            bitmap
                        }
                } finally {
                    jobs.remove(id)
                }
            }
        }
        return job.await()
    }
}
