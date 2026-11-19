package com.careful.hyperfvm.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings

/**
 * 当前页面内容区的取样层。顶栏从它取样；关闭模糊或设备不支持 RuntimeShader（Android 13 以下）
 * 时为 null，所有模糊分支自动退化为纯色。
 *
 * 注意：它只能挂在内容区上，使用它的模糊条必须是它的兄弟节点。若模糊条落在它的子树里，
 * 录制中的图层会引用自己，RenderThread 在 prepareTree 时无限递归导致栈溢出闪退。
 */
val LocalBlurBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
fun rememberAppBlurBackdrop(): LayerBackdrop? {
    val settings = LocalAppSettings.current
    if (!settings.enableBlur || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 全屏图片查看器的取样层。
 *
 * 查看器铺在根层、不在任何页面内容里，[LocalBlurBackdrop] 到这里是 null，
 * 长按操作面板的玻璃会退化成不透明实色。这里给查看器自己的画面垫一层：
 * 铺黑底再录「黑底 + 图片 + 页码」，面板取样到的就是查看器此刻的样子。
 *
 * 与 [rememberAppBlurBackdrop] 的唯一区别是不铺 surface 色——查看器是黑底场景。
 */
@Composable
fun rememberViewerBackdrop(): LayerBackdrop? {
    val settings = LocalAppSettings.current
    if (!settings.enableBlur || !isRuntimeShaderSupported()) return null
    return rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }
}

/**
 * 透明窗口（链接中转页）的取样层。
 *
 * 窗口背后是**别的应用**，Compose 采不到它的画面；窗口本身又必须全透明。
 * 所以这里在离屏层里自画一层很淡的渐变当「假背板」：`layerBackdrop` 只把内容
 * 录进离屏层（屏幕绘制走 [androidx.compose.ui.graphics.drawscope.ContentDrawScope.drawContent]，
 * 录制本身不上屏），屏幕仍然透明、不会盖住来源应用，而玻璃面板能采到明暗，
 * 不会退化成不透明实色。
 */
@Composable
fun rememberWindowBackdrop(): LayerBackdrop? {
    val settings = LocalAppSettings.current
    if (!settings.enableBlur || !isRuntimeShaderSupported()) return null
    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to surface.copy(alpha = 0.12f),
            ),
        )
        drawContent()
    }
}
