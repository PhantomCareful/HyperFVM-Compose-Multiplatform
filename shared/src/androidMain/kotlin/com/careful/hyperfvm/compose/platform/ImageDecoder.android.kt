package com.careful.hyperfvm.compose.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun createDataImageDecoder(): DataImageDecoder = BitmapRegionImageDecoder()

/** Android 侧：区域解码 + 2 的幂采样，原实现的直接搬运。 */
private class BitmapRegionImageDecoder : DataImageDecoder {

    override fun size(path: String): ImageSize? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return ImageSize(options.outWidth, options.outHeight)
    }

    // API 31 起 newInstance(String, boolean) 被标记废弃，但替代的 FileDescriptor 版本
    // 需要自己管 fd 的生命周期，对这里的「只解一次区域」反而更麻烦。
    @Suppress("DEPRECATION")
    override fun decodeRegion(
        path: String,
        regionWidth: Int,
        regionHeight: Int,
        sampleSize: Int,
    ): ImageBitmap? {
        var decoder: BitmapRegionDecoder? = null
        return try {
            decoder = BitmapRegionDecoder.newInstance(path, false)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                // 缩略图不需要透明度，RGB_565 能少占一半内存；原图仍是 PNG，不影响查看与保存。
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val rect = Rect(0, 0, regionWidth.coerceAtLeast(1), regionHeight.coerceAtLeast(1))
            decoder.decodeRegion(rect, options)?.asImageBitmap()
        } catch (_: Exception) {
            // 文件损坏、磁盘不可读都走这里：卡片退化为占位图，不影响其它图片。
            null
        } finally {
            decoder?.recycle()
        }
    }

    override fun decodeBytes(bytes: ByteArray, sampleSize: Int): ImageBitmap? = try {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            // 数据包里的卡图 / 图标多为带透明底的 WebP，不能用 RGB_565 解码，
            // 否则透明区域会变成黑块；缓存按 4 字节/像素计费，与 ARGB_8888 一致。
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
