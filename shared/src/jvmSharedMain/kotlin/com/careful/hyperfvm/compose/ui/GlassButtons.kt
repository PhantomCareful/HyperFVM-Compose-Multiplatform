package com.careful.hyperfvm.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏里的图标按钮：柔光玻璃下换成 OS4 的玻璃胶囊按钮，轻透磨砂下保持原生 [IconButton]。
 *
 * [GlassIconButton] 没有 enabled 参数：禁用时吞掉点击并压暗内容，避免误触。
 * 放在 [top.yukonga.miuix.kmp.glass.GlassTopAppBar] 内会自动继承顶栏的材质与滚动胶囊。
 */
@Composable
fun AppTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (LocalAppSettings.current.isGlassStyle) {
        GlassIconButton(
            onClick = { if (enabled) onClick() },
            modifier = modifier.alpha(if (enabled) 1f else 0.4f),
            content = content,
        )
        return
    }
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

/**
 * 顶栏搜索框展开时右侧的取消动作。
 *
 * 柔光玻璃下是 demo 同款的实色圆钮：44dp 圆底取 surfaceContainerHigh——
 * 与输入框（`InputField` 默认底色）同一种材质，× 图标用正文色。
 * 这里不能用 [GlassIconButton]：它继承顶栏材质，而页面停在顶部时顶栏的材质进度是 0，
 * 按钮整个透明，看起来就像没有背景。
 * 轻透磨砂下保持与 miuix `SearchBar` 一致的文字「取消」。
 * 调用方负责滑出 / 滑回的过渡，这里只提供两种形态本身。
 */
@Composable
fun AppSearchCancelAction(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalAppSettings.current.isGlassStyle) {
        IconButton(
            onClick = onCancel,
            modifier = modifier
                .size(44.dp)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape),
        ) {
            Icon(
                imageVector = MiuixIcons.Basic.Close,
                contentDescription = "取消",
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
        return
    }
    Text(
        text = "取消",
        modifier = modifier.clickable(interactionSource = null, indication = null, onClick = onCancel),
        style = MiuixTheme.textStyles.main,
        color = MiuixTheme.colorScheme.primary,
    )
}
