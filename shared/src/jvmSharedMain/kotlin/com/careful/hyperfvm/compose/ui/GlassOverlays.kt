package com.careful.hyperfvm.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.careful.hyperfvm.compose.component.card.errataContentColor
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 弹层取样用的页面背板。
 *
 * 弹层渲染在根 Scaffold 的宿主里，不在页面内容区子树中，所以取样页面背板是安全的；
 * 模糊关闭或设备不支持着色器时返回 null，玻璃面板会退化为不透明实色。
 */
@Composable
internal fun overlayBackdrop(): Backdrop? {
    val settings = LocalAppSettings.current
    if (!settings.enableBlur || !isRuntimeShaderSupported()) return null
    return LocalBlurBackdrop.current
}

/**
 * 全站统一的底部弹层：柔光玻璃下换成 OS4 Glass 面板，轻透磨砂下保持原生
 * [OverlayBottomSheet]。
 *
 * 参数取两个实现共有的交集，调用方换名字即可。
 *
 * @param enableNestedScroll 内容区滚到尽头后是否把剩余的向下滑动转给弹层（继续下滑即关闭）。
 *   内容很长的弹层传 false，避免滑列表时误把弹层拖下去；拖顶部手柄和返回手势仍可关闭。
 */
@Composable
fun AppBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    onDismissFinished: (() -> Unit)? = null,
    enableNestedScroll: Boolean = true,
    content: @Composable () -> Unit,
) {
    val settings = LocalAppSettings.current
    if (settings.isGlassStyle) {
        GlassBottomSheet(
            onDismissRequest = onDismissRequest,
            show = show,
            backdrop = overlayBackdrop(),
            title = title,
            onDismissFinished = onDismissFinished,
            enableNestedScroll = enableNestedScroll,
            content = content,
        )
        return
    }
    OverlayBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        enableNestedScroll = enableNestedScroll,
        content = content,
    )
}

/**
 * 全站统一的对话框：柔光玻璃下换成 OS4 Glass 面板，轻透磨砂下保持原生 [OverlayDialog]。
 *
 * 参数取两个实现共有的交集，调用方换名字即可。
 */
@Composable
fun AppDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    summary: String? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val settings = LocalAppSettings.current
    if (settings.isGlassStyle) {
        GlassDialog(
            onDismissRequest = onDismissRequest,
            show = show,
            backdrop = overlayBackdrop(),
            title = title,
            summary = summary,
            onDismissFinished = onDismissFinished,
            content = content,
        )
        return
    }
    OverlayDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = content,
    )
}

/** 弹层动作按钮的语气：普通 / 主操作 / 破坏性操作。 */
enum class AppDialogButtonRole {
    Normal,
    Primary,
    Destructive,
}

/**
 * 对话框与底部弹层里的动作按钮。
 *
 * 柔光玻璃下换成 OS4 的 [GlassDialogButton]：普通操作是半透明的 tonal 底 + 正文色文字，
 * 主操作是实心主色，破坏性操作是勘误红文字——和原生磨砂下「纯文字按钮」的观感完全不同，
 * 所以不能直接沿用 [TextButton]。轻透磨砂下保持原来的 [TextButton]。
 */
@Composable
fun AppDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: AppDialogButtonRole = AppDialogButtonRole.Normal,
) {
    if (LocalAppSettings.current.isGlassStyle) {
        GlassDialogButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            role = when (role) {
                AppDialogButtonRole.Normal -> GlassDialogButtonRole.Normal
                AppDialogButtonRole.Primary -> GlassDialogButtonRole.Primary
                AppDialogButtonRole.Destructive -> GlassDialogButtonRole.Destructive
            },
        ) {
            Text(
                text = text,
                style = MiuixTheme.textStyles.button,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        return
    }
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = when (role) {
            AppDialogButtonRole.Normal -> ButtonDefaults.textButtonColors()
            AppDialogButtonRole.Primary -> ButtonDefaults.textButtonColorsPrimary()
            AppDialogButtonRole.Destructive -> ButtonDefaults.textButtonColors(textColor = errataContentColor())
        },
    )
}

/**
 * 弹层内容卡的底色。
 *
 * 轻透磨砂的弹层面板与 Card 默认的 surface 同色，用 secondaryContainer 才能分辨卡片边界；
 * 柔光玻璃的面板是半透明的，灰调卡片叠上去会跟着发透、发灰，换回不透明的
 * surfaceContainer（纯白 / 纯黑）卡片才立得住。
 */
@Composable
fun sheetCardColor(): Color = if (LocalAppSettings.current.isGlassStyle) {
    MiuixTheme.colorScheme.surfaceContainer
} else {
    MiuixTheme.colorScheme.secondaryContainer
}
