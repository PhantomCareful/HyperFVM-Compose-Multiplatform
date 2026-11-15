package com.careful.hyperfvm.compose.component.imageviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.careful.hyperfvm.compose.platform.createDataImageDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.zoomable

/** 两端同一套区域解码器；桌面用它在解码阶段就完成降采样。 */
private val decoder = createDataImageDecoder()

/** 查看器一页最多解到这么宽 / 这么高（像素）。 */
private const val ViewerMaxDecodedWidth = 2560
private const val ViewerMaxDecodedHeight = 7000

/** 公告正文里的插图是 720 宽的图，留一倍余量足够，也不至于占太多内存。 */
private const val InlineMaxDecodedWidth = 1600
private const val InlineMaxDecodedHeight = 4000

@Composable
actual fun ViewerSubSamplingImage(
    path: String,
    contentDescription: String?,
    zoomableState: ZoomableState,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDisplayed: (Boolean) -> Unit,
) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    var reported by remember(path) { mutableStateOf(false) }
    val latestOnDisplayed by rememberUpdatedState(onDisplayed)

    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            decodeForDisplay(path, ViewerMaxDecodedWidth, ViewerMaxDecodedHeight)
        }
    }

    // 首帧真的画出来之后才上报，调用方据此撤掉缩略图占位（同 Android 侧的时机）。
    LaunchedEffect(bitmap) {
        if (bitmap != null && !reported) {
            // 等一帧：绘制与解码是两回事，这一帧之后图才真的在屏幕上。
            withFrameNanos { }
            reported = true
            latestOnDisplayed(true)
        }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier.zoomable(
                state = zoomableState,
                onClick = { onClick() },
                onLongClick = { onLongClick() },
            ),
        )
    } else {
        // 解码没回来之前保留手势区域，点击 / 长按行为与 Android 一致。
        Box(
            modifier = modifier.zoomable(
                state = zoomableState,
                onClick = { onClick() },
                onLongClick = { onLongClick() },
            ),
        )
    }
}

@Composable
actual fun StaticSubSamplingImage(
    path: String,
    zoomableState: ZoomableState,
    modifier: Modifier,
) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            decodeForDisplay(path, InlineMaxDecodedWidth, InlineMaxDecodedHeight)
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

/**
 * 按目标尺寸算出整数倍采样率再整图解码。
 *
 * 采样在解码阶段生效（ImageIO 的 `setSourceSubsampling`），5000×13000 的长图
 * 采到 1/3 后只有约 1667×4333，几十 MB 的解码峰值不会出现。
 */
private fun decodeForDisplay(path: String, maxWidth: Int, maxHeight: Int): ImageBitmap? {
    val size = decoder.size(path) ?: return null
    val sample = maxOf(
        (size.width + maxWidth - 1) / maxWidth,
        (size.height + maxHeight - 1) / maxHeight,
        1,
    )
    return decoder.decodeFull(path, sample)
}
