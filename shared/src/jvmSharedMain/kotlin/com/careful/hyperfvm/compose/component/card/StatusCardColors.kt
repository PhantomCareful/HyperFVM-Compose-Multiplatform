package com.careful.hyperfvm.compose.component.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** miuix 状态卡的固定绿色，即 Java 版 `miuix_vector_graphic_color_on_status_cardview`。 */
val StatusCardIconGreen = Color(0xFF36D167)

// ---- 温馨礼包卡：Miuix secondaryContainer 那套 ----

/**
 * 状态卡的背景色，照搬 KernelSU 的 miuix 版运行状态卡（`HomeMiuix.kt` 的 `StatusCard`）：
 * Monet 动态取色下取 secondaryContainer；miuix 静态色板下用它自带的状态卡绿
 * （浅色 #DFFAE4 / 深色 #1A3825，即 Java 版 `miuix_status_cardview_background` 的值）。
 *
 * 防御卡概览大卡与查黑详情页的「没有记录」提示卡共用这一份取值，两处观感一致。
 */
@Composable
fun statusCardBackgroundColor(): Color = when {
    MiuixTheme.isDynamicColor -> MiuixTheme.colorScheme.secondaryContainer
    isInDarkTheme() -> Color(0xFF1A3825)
    else -> Color(0xFFDFFAE4)
}

/**
 * 状态卡上的强调色（图标、强调文字），与 [statusCardBackgroundColor] 成对：
 * Monet 下取主题色 0.8 透明度（同 KernelSU），miuix 静态色板下用固定的状态绿。
 */
@Composable
fun statusCardAccentColor(): Color = if (MiuixTheme.isDynamicColor) {
    MiuixTheme.colorScheme.primary.copy(alpha = 0.8f)
} else {
    StatusCardIconGreen
}

// ---- 仪表盘「活动进行中」绿卡：Miuix tertiaryContainer 那套 ----

/**
 * 「活动进行中」的绿，三处共用同一份取值：
 * - 卡面底色：全天双爆、今日开启的走马观花与甜蜜岛、可以领奖的每日签到；
 * - 进度条：声望翻倍开启的美食悬赏（卡面仍是白底）；
 * - 右侧状态文字的强调色，见 [activeStatusCardAccentColor]。
 *
 * 非 Monet 下与温馨礼包同一份固定绿；Monet 下改用 tertiaryContainer，
 * 与温馨礼包用的 secondaryContainer 区分开：两种绿同屏时不会糊成一片。
 */
@Composable
fun activeStatusCardBackgroundColor(): Color = when {
    MiuixTheme.isDynamicColor -> MiuixTheme.colorScheme.tertiaryContainer
    isInDarkTheme() -> Color(0xFF1A3825)
    else -> Color(0xFFDFFAE4)
}

/**
 * 绿卡的卡片内容色，作为 [top.yukonga.miuix.kmp.basic.Card] 的 `contentColor` 使用：
 * 决定水波纹的颜色。标题和状态文字各自在配色表里显式指定，不跟它走。
 */
@Composable
fun activeStatusCardContentColor(): Color = if (MiuixTheme.isDynamicColor) {
    MiuixTheme.colorScheme.onTertiaryContainer
} else {
    MiuixTheme.colorScheme.onSurfaceContainer
}

/**
 * 绿卡右侧状态文字的强调色：同温馨礼包圆圈对勾那根绿。
 *
 * 用于「今日开启 / 可以领奖 / 持续到…」这类绿卡状态文字，以及声望翻倍开启时
 * 美食悬赏的 X/Y。Monet 下改用 onTertiaryContainer，跟着动态色板走。
 */
@Composable
fun activeStatusCardAccentColor(): Color = if (MiuixTheme.isDynamicColor) {
    MiuixTheme.colorScheme.onTertiaryContainer
} else {
    StatusCardIconGreen
}
