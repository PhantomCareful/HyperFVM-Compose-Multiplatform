package com.careful.hyperfvm.compose.component.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.saket.telephoto.zoomable.ZoomableState

/**
 * 本地大图的渲染宿主。
 *
 * - Android：telephoto 的 `SubSamplingImage`，按瓦片只解视野内的那一块，长图不占整张内存；
 * - 桌面：没有这个产物，用 ImageIO 在**解码阶段**降采样后整张画出来，内存同样可控。
 *
 * 参数用字符串路径而不是 `File`，这样声明才能放在 commonMain。
 */
@Composable
expect fun ViewerSubSamplingImage(
    path: String,
    contentDescription: String?,
    zoomableState: ZoomableState,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    /** 图真的画出来了（不是「开始解码」），调用方据此撤掉缩略图占位。 */
    onDisplayed: (Boolean) -> Unit,
)

/**
 * 列表里的纯展示图：手势全关，只按容器大小 fit 显示。
 *
 * Android 侧仍需要 [zoomableState]（SubSamplingImage 的绘制区域由它的 viewportSize 推导），
 * 桌面侧只拿它占位。
 */
@Composable
expect fun StaticSubSamplingImage(
    path: String,
    zoomableState: ZoomableState,
    modifier: Modifier,
)
