package com.careful.hyperfvm.compose.data.image

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import com.careful.hyperfvm.compose.platform.createDataImageDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据图缩略图解码与缓存。
 *
 * 数据图的原始尺寸接近 5000×13000，直接把整张图采样进来会吃掉几十 MB；
 * 而瀑布流卡片只露出图片顶部的一小块，所以这里只解顶部区域，
 * 采样率按卡片的显示宽度定，分辨率够用又不会把内存撑爆。
 * 具体解码由平台层的 [com.careful.hyperfvm.compose.platform.DataImageDecoder] 完成。
 */
object DataImageThumbnails {

    private val decoder = createDataImageDecoder()

    /** 转场占位用的整图宽度：铺满全屏够看，再高只是徒增解码与内存。 */
    private const val ViewerPreviewWidth = 540

    /**
     * 放大态退出用的整图上限像素数。
     *
     * 放大倍数封顶 1.5 倍，图片最大也就铺到 1.5 屏（约 1620×3600），6 000 000
     * 像素刚好覆盖；再大的图会退一档采样率——那时图片本来也是按高度 fit 的，
     * 落位宽度远小于屏宽，降一档仍然对得上。
     */
    private const val ZoomPreviewMaxPixels = 6_000_000L

    /** 缓存上限取可用堆的 1/8，按像素字节数计。 */
    private val cache = object : LruCache<String, ImageBitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4
    }

    private fun cacheSizeBytes(): Int {
        val maxBytes = Runtime.getRuntime().maxMemory() / 8
        return maxBytes.coerceIn(8L * 1024 * 1024, 64L * 1024 * 1024).toInt()
    }

    private fun cacheKey(file: File, targetWidthPx: Int, heightRatio: Float): String =
        "${file.absolutePath}@${file.lastModified()}#$targetWidthPx#$heightRatio"

    private fun fileKey(file: File): String = "${file.absolutePath}@${file.lastModified()}"

    /**
     * 按文件索引的最近缩略图，供查看器转场占位使用。
     *
     * 只留最后 4 张：被点击的卡片必然刚加载过，能同步命中；
     * 每张约卡片宽×卡片高（RGB_565），4 张的内存开销可以忽略。
     */
    private val recentThumbs = object : LruCache<String, ImageBitmap>(4) {}

    /**
     * 同步取最近加载过的缩略图，不做任何解码；没命中返回 null。
     *
     * 查看器打开时把它当条带占位用：它只盖住卡片那块内容，是完整低清图
     * （[peekViewerPreview]）缺失时的兜底。
     */
    fun peek(file: File): ImageBitmap? = recentThumbs.get(fileKey(file))

    /**
     * 查看器预热图：整张图的低清采样，按文件索引的 LRU。
     *
     * 卡片稳定显示后由瀑布流在后台预热（见 [loadViewerPreview]）；查看器打开时
     * 同步取这里，命中就以它为转场全程的内容——窗口从卡片矩形扩展到全屏时，
     * 任何时刻露出的都是完整画面（竖长图被卡片裁掉的下半截也在其中）。
     *
     * 上限按**字节**算（重写了 [LruCache.sizeOf]）：竖长图的低清图约 1.5–2 MB，
     * 24 MB 能同时留住最近一屏的十来张。之前这里误写成条目数 6，put 进去的图
     * 因为 sizeOf 远大于上限而被立刻 trim 掉，peek 永远拿不到——转场才只有条带。
     */
    private val viewerPreviews = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 2
    }

    /** 同步取预热好的整图低清图；没预热到返回 null。 */
    fun peekViewerPreview(file: File): ImageBitmap? = viewerPreviews.get(fileKey(file))

    /**
     * 预热解码专用线程池：最多两路并行。
     *
     * 整图 PNG 采样解码是重 CPU 活，屏幕上同时有五六张卡轮流预热时，
     * 不限并发会把滚动期间的 CPU 抢光；限成两路后干脆排队，宁可慢一点。
     */
    private val previewDispatcher = Dispatchers.IO.limitedParallelism(2)

    /**
     * 预热解码的作用域，独立于调用方：卡片滚出屏幕时预热的协程会被取消，
     * 但解码本身应该跑完并落进缓存——用户很可能马上又滚回来。
     *
     * 用 [SupervisorJob]：单张图解码失败不能带走整个作用域。
     */
    private val previewScope = CoroutineScope(SupervisorJob() + previewDispatcher)

    /** 正在解码中的预热任务：同一张图只解一次，后来者等同一个结果。 */
    private val previewJobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    /**
     * 放大态退出用的整图高清图，只留当前这一张。
     *
     * 退出转场里整张图要和缩略图连线，内容只能来自同一张位图：清晰区和四周
     * 来自两张不同的图，交界处永远是一道分辨率硬缝，看上去像两张图拼在一起。
     * 所以这里按「放大后图片实际占的像素」解一张，缩到哪一帧都和放大时看到的一致。
     *
     * 只缓存一张：它只服务于当前正在放大的那张图，换一张放大时旧的就没用了。
     */
    private val zoomPreviews = object : LruCache<String, ImageBitmap>(1) {}

    /** 同步取放大态退出的整图；没预热到返回 null（调用方退回低清垫层那套）。 */
    fun peekZoomPreview(file: File): ImageBitmap? = zoomPreviews.get(fileKey(file))

    /** 正在解码中的高清整图任务，同一张图只解一次。 */
    private val zoomJobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    /**
     * 解码一张「整图高清」供放大态退出使用，已缓存时直接返回。
     *
     * 由查看器在用户放大后调用（见 DataImageViewer）：那时图片已经稳定显示，
     * 解码在后台跑完，等用户退出时几乎总是同步命中。
     *
     * @param targetWidthPx 期望的整图宽度，按最大放大倍数下图片的实际落位宽度给。
     */
    suspend fun loadZoomPreview(file: File, targetWidthPx: Int): ImageBitmap? {
        val key = fileKey(file)
        zoomPreviews.get(key)?.let { return it }
        val job = zoomJobs.computeIfAbsent(key) {
            previewScope.async {
                try {
                    zoomPreviews.get(key) ?: decodeFullPreview(
                        file = file,
                        targetWidthPx = targetWidthPx,
                        maxPixels = ZoomPreviewMaxPixels,
                    )?.let { decoded ->
                        zoomPreviews.put(key, decoded)
                        decoded
                    }
                } finally {
                    zoomJobs.remove(key)
                }
            }
        }
        return job.await()
    }

    /**
     * 解码一张「整图低清」供查看器转场使用，已缓存时直接返回。
     *
     * 目标宽度 500 多像素：转场中最多放大到全屏宽，再高的分辨率换来的只是
     * 更大的内存与更长的整图解码；对超长 PNG 采样解码本身就要几百毫秒，
     * 所以在 IO 线程跑且只由卡片稳定后的预热调用，不碰卡片首帧的路径。
     *
     * 同一张图正在解码时不重复解：预热刚排上队、用户就点开查看器时，
     * 查看器的兜底会走到这里，直接等同一个结果，免得两路解码互相抢 CPU。
     */
    suspend fun loadViewerPreview(file: File): ImageBitmap? {
        val key = fileKey(file)
        viewerPreviews.get(key)?.let { return it }
        val job = previewJobs.computeIfAbsent(key) {
            previewScope.async {
                try {
                    viewerPreviews.get(key) ?: decodeFullPreview(file, ViewerPreviewWidth)?.let { decoded ->
                        viewerPreviews.put(key, decoded)
                        decoded
                    }
                } finally {
                    // 无论成败都释放占位，失败后下次调用还能重试。
                    previewJobs.remove(key)
                }
            }
        }
        // 调用方被取消（转场结束、卡片被回收）不影响解码本身，它会在
        // [previewScope] 里跑完并落进缓存。
        return job.await()
    }

    /**
     * 读取一张缩略图。
     *
     * @param targetWidthPx 卡片可显示的宽度（像素），用于决定采样率。
     * @param heightRatio 卡片显示区域的高宽比，用来决定纵向要解多高。
     */
    suspend fun load(file: File, targetWidthPx: Int, heightRatio: Float): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val key = cacheKey(file, targetWidthPx, heightRatio)
            val cached = cache.get(key)
            val image = cached
                ?: decodeTopRegion(file, targetWidthPx, heightRatio)
                    ?.let { decoded ->
                        cache.put(key, decoded)
                        decoded
                    }
                ?: return@withContext null
            recentThumbs.put(fileKey(file), image)
            image
        }

    private fun decodeFullPreview(
        file: File,
        targetWidthPx: Int,
        maxPixels: Long = Long.MAX_VALUE,
    ): ImageBitmap? {
        val size = decoder.size(file.absolutePath) ?: return null
        val srcWidth = size.width
        val srcHeight = size.height
        if (srcWidth <= 0 || srcHeight <= 0) return null

        // 采样率取 2 的幂，保证解码宽度仍不小于需要的宽度。
        var sampleSize = 1
        while (sampleSize < 64 && srcWidth / (sampleSize * 2) >= targetWidthPx) {
            sampleSize *= 2
        }
        // 超长图按宽度采样后仍然很大时再退一档：像素预算优先，宁可略糊也别把
        // 堆撑爆。宽高一起缩，比例不变，转场里的落位不会因此错位。
        while (sampleSize < 64 &&
            (srcWidth / sampleSize).toLong() * (srcHeight / sampleSize) > maxPixels
        ) {
            sampleSize *= 2
        }
        return decoder.decodeRegion(file.absolutePath, srcWidth, srcHeight, sampleSize)
    }

    private fun decodeTopRegion(file: File, targetWidthPx: Int, heightRatio: Float): ImageBitmap? {
        val size = decoder.size(file.absolutePath) ?: return null
        val srcWidth = size.width
        val srcHeight = size.height
        if (srcWidth <= 0 || srcHeight <= 0) return null

        // 首帧列宽可能还是 0、比例也可能是除零得到的 Infinity，
        // 不夹住的话下面的采样循环会死循环、区域高度会溢出成整张图。
        val targetWidth = targetWidthPx.coerceAtLeast(1)
        val safeRatio = if (heightRatio.isFinite()) heightRatio.coerceIn(0.05f, 4f) else 0.5f

        // 采样率取 2 的幂，保证解码宽度仍然不小于卡片显示宽度。
        var sampleSize = 1
        while (sampleSize < 64 && srcWidth / (sampleSize * 2) >= targetWidth) {
            sampleSize *= 2
        }
        val scaledWidth = srcWidth / sampleSize
        // 多解一点点高度，避免卡片因取整差 1px 露出空白。
        val neededHeight = (scaledWidth * safeRatio * 1.1f).toInt()
        val regionHeight = (neededHeight * sampleSize).coerceIn(1, srcHeight)

        return decoder.decodeRegion(file.absolutePath, srcWidth, regionHeight, sampleSize)
    }

    /** 图片被删除或覆盖后清掉缓存。 */
    fun clear() {
        cache.evictAll()
        recentThumbs.evictAll()
        viewerPreviews.evictAll()
        zoomPreviews.evictAll()
        ratios.clear()
    }

    /** 宽高比缓存（宽/高），key 带修改时间，图片被覆盖后会自然失效。 */
    private val ratios = java.util.concurrent.ConcurrentHashMap<String, Float>()

    /**
     * 读取图片的宽高比（宽/高），读不出来返回 null。
     *
     * 只读文件头，不解像素，近百张图批量跑也只要百来毫秒；
     * 瀑布流用它算预览高度，保证不同长宽比的图能参差排列。
     */
    suspend fun ratioOf(file: File): Float? = withContext(Dispatchers.IO) {
        val key = "${file.absolutePath}@${file.lastModified()}"
        ratios[key]?.let { return@withContext it }

        val size = decoder.size(file.absolutePath) ?: return@withContext null
        if (size.width <= 0 || size.height <= 0) return@withContext null

        val ratio = size.width.toFloat() / size.height
        ratios[key] = ratio
        ratio
    }
}
