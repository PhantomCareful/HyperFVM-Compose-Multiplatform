package com.careful.hyperfvm.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
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
