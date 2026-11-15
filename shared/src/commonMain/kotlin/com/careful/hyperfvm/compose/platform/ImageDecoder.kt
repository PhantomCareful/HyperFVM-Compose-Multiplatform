package com.careful.hyperfvm.compose.platform

import androidx.compose.ui.graphics.ImageBitmap

/** 图片的像素尺寸。 */
data class ImageSize(val width: Int, val height: Int)

/**
 * 数据图解码器：只读文件头拿尺寸、只解左上角一块区域。
 *
 * 数据图接近 5000×13000，整图解码会吃掉几十 MB，瀑布流卡片只要露出顶部一小块。
 * 两端各用系统能力实现同一套语义：
 * - Android：`BitmapRegionDecoder` + `BitmapFactory.Options.inSampleSize`
 * - 桌面：`ImageIO` 的 `setSourceRegion` + `setSourceSubsampling`
 *
 * 用字符串路径而不是 `File`，是为了让接口能声明在 commonMain 里。
 */
interface DataImageDecoder {

    /** 只读文件头返回像素尺寸，不解像素；读不出来返回 null。 */
    fun size(path: String): ImageSize?

    /**
     * 解左上角 (0,0) 起、宽 [regionWidth]、高 [regionHeight]（均为源图像素）的区域，
     * 并按 [sampleSize] 做整数倍降采样。
     */
    fun decodeRegion(path: String, regionWidth: Int, regionHeight: Int, sampleSize: Int): ImageBitmap?

    /** 整图解码（小图用：封面缩略图等），同样支持整数倍降采样。 */
    fun decodeFull(path: String, sampleSize: Int = 1): ImageBitmap? {
        val size = size(path) ?: return null
        return decodeRegion(path, size.width, size.height, sampleSize)
    }

    /** 从内存字节解码（Compose 资源里的动图帧等）。 */
    fun decodeBytes(bytes: ByteArray, sampleSize: Int = 1): ImageBitmap?
}

expect fun createDataImageDecoder(): DataImageDecoder
