package com.careful.hyperfvm.compose.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream

actual fun createDataImageDecoder(): DataImageDecoder = ImageIoDataImageDecoder()

/**
 * 桌面侧：`ImageIO` 与 Android 的 `BitmapRegionDecoder` 语义几乎一一对应——
 * `setSourceRegion` 就是 decodeRegion，`setSourceSubsampling` 就是 inSampleSize。
 *
 * 关键在于采样发生在解码阶段：5000×13000 的 PNG 采样 8 倍后只有约 625×1625，
 * 内存几 MB；换成先整图 decode 再缩放的写法会直接吃掉几百 MB。
 * Java 的 PNG/JPEG 解码器都支持这两个参数，不需要额外依赖。
 */
private class ImageIoDataImageDecoder : DataImageDecoder {

    override fun size(path: String): ImageSize? = withReader(path) { reader ->
        val width = reader.getWidth(0)
        val height = reader.getHeight(0)
        if (width <= 0 || height <= 0) null else ImageSize(width, height)
    }

    override fun decodeRegion(
        path: String,
        regionWidth: Int,
        regionHeight: Int,
        sampleSize: Int,
    ): ImageBitmap? = withReader(path) { reader ->
        val param = reader.defaultReadParam
        val step = sampleSize.coerceAtLeast(1)
        param.setSourceRegion(Rectangle(0, 0, regionWidth.coerceAtLeast(1), regionHeight.coerceAtLeast(1)))
        param.setSourceSubsampling(step, step, 0, 0)
        reader.read(0, param)?.toComposeImageBitmap()
    }

    override fun decodeBytes(bytes: ByteArray, sampleSize: Int): ImageBitmap? = withInput(
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)),
    ) { reader ->
        val param = reader.defaultReadParam
        val step = sampleSize.coerceAtLeast(1)
        param.setSourceSubsampling(step, step, 0, 0)
        reader.read(0, param)?.toComposeImageBitmap()
    }

    /**
     * 打开读器并保证释放：`ImageReader` 与 `ImageInputStream` 都持有文件句柄，
     * Windows 上没关干净会让同一个文件删不掉、覆盖写失败。
     */
    private fun <T> withReader(path: String, block: (ImageReader) -> T?): T? {
        val file = File(path)
        if (!file.isFile) return null
        return withInput(ImageIO.createImageInputStream(file), block)
    }

    private fun <T> withInput(input: ImageInputStream?, block: (ImageReader) -> T?): T? {
        if (input == null) return null
        var reader: ImageReader? = null
        return try {
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return null
            reader = readers.next()
            reader.setInput(input, true, true)
            block(reader)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { reader?.dispose() }
            runCatching { input.close() }
        }
    }
}
