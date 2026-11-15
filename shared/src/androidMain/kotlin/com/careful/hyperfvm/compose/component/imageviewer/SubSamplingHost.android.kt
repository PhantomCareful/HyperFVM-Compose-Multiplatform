package com.careful.hyperfvm.compose.component.imageviewer

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import me.saket.telephoto.subsamplingimage.SubSamplingImage
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.subsamplingimage.rememberSubSamplingImageState
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.zoomable
import java.io.File

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
    // file:// 会被解析成最快的文件描述符路径，比 content:// 少一层代理。
    val imageSource = remember(path) {
        SubSamplingImageSource.contentUri(uri = Uri.fromFile(File(path)))
    }
    val imageState = rememberSubSamplingImageState(
        imageSource = imageSource,
        zoomableState = zoomableState,
    )
    val displayed = imageState.isImageDisplayedInFullQuality
    LaunchedEffect(displayed) { onDisplayed(displayed) }

    SubSamplingImage(
        state = imageState,
        contentDescription = contentDescription,
        modifier = modifier.zoomable(
            state = zoomableState,
            onClick = { onClick() },
            onLongClick = { onLongClick() },
        ),
    )
}

@Composable
actual fun StaticSubSamplingImage(
    path: String,
    zoomableState: ZoomableState,
    modifier: Modifier,
) {
    val imageState = rememberSubSamplingImageState(
        imageSource = remember(path) {
            SubSamplingImageSource.contentUri(uri = Uri.fromFile(File(path)))
        },
        zoomableState = zoomableState,
    )
    SubSamplingImage(
        state = imageState,
        contentDescription = null,
        modifier = modifier.zoomable(
            state = zoomableState,
            gestures = EnabledZoomGestures.None,
            // 双击回调默认不是空的，留着会让点击检测器重新装回去消费事件，
            // 清零后检测器整体退化为什么都不拦，竖向滑动照常交给列表。
            onDoubleClick = null,
        ),
    )
}
