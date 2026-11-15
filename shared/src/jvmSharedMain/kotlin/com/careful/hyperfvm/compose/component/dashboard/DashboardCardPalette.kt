package com.careful.hyperfvm.compose.component.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.careful.hyperfvm.compose.component.card.activeStatusCardAccentColor
import com.careful.hyperfvm.compose.component.card.activeStatusCardBackgroundColor
import com.careful.hyperfvm.compose.component.card.activeStatusCardContentColor
import com.careful.hyperfvm.compose.component.card.errataCardColors
import com.careful.hyperfvm.compose.data.dashboard.DashboardStyle
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 进度背景的浓度：太深会盖住文字，太浅又看不出进度。 */
private const val ProgressAlpha = 0.15f

/** 彩色卡上副标题的透明度：比标题淡一档，又不至于看不清。 */
private const val DetailAlpha = 0.7f

/**
 * 一张状态卡解析完成的五种颜色，卡片只从这里取色，不再自己判断场景。
 *
 * @param card 卡片底色与内容色，也决定水波纹的颜色
 * @param title 标题文字
 * @param detail 副标题文字
 * @param status 右侧状态文字
 * @param progress 进度条的底色
 */
internal data class DashboardCardPalette(
    val card: CardColors,
    val title: Color,
    val detail: Color,
    val status: Color,
    val progress: Color,
)

/**
 * 仪表盘状态卡的全部配色规则，按 [DashboardStyle] 分四支写完，改配色只改这一处。
 *
 * | 外观 | 卡面 | 标题 | 右侧状态 | 进度条 |
 * |---|---|---|---|---|
 * | [DashboardStyle.Neutral] | 白 | 常规标题色 | 有进度用主题色，否则次要字色 | 主题色 × 15% |
 * | [DashboardStyle.Green] | 状态绿 | 常规标题色 | 强调绿 | 内容色 × 15% |
 * | [DashboardStyle.GreenProgress] | 白 | 常规标题色 | 强调绿 | 状态绿（不透明） |
 * | [DashboardStyle.Red] | 勘误红 | 勘误红 | 勘误红 | 勘误红 × 15% |
 *
 * 绿卡与白卡共用「常规标题色」，两种卡同屏时标题不会深浅不一；绿只出现在底色
 * 和右侧那句状态上。红色卡是例外：它是整卡醒目的提示，标题也跟着染红。
 *
 * [hasProgress] 只影响白卡：只有「12/21」这种进度状态才把右侧文字提到主题色。
 */
@Composable
internal fun dashboardCardPalette(style: DashboardStyle, hasProgress: Boolean): DashboardCardPalette {
    val scheme = MiuixTheme.colorScheme
    return when (style) {
        DashboardStyle.Neutral -> {
            val card = CardDefaults.defaultColors()
            DashboardCardPalette(
                card = card,
                title = card.contentColor,
                detail = scheme.onSurfaceContainerVariant,
                status = if (hasProgress) scheme.primary else scheme.onSurfaceContainerVariant,
                progress = scheme.primary.copy(alpha = ProgressAlpha),
            )
        }

        DashboardStyle.Green -> {
            val card = CardDefaults.defaultColors(
                color = activeStatusCardBackgroundColor(),
                contentColor = activeStatusCardContentColor(),
            )
            DashboardCardPalette(
                card = card,
                title = scheme.onSurfaceContainer,
                detail = card.contentColor.copy(alpha = DetailAlpha),
                status = activeStatusCardAccentColor(),
                progress = card.contentColor.copy(alpha = ProgressAlpha),
            )
        }

        DashboardStyle.GreenProgress -> {
            // 卡面同白卡，只在进度条与右侧的 X/Y 上用绿。
            val card = CardDefaults.defaultColors()
            DashboardCardPalette(
                card = card,
                title = card.contentColor,
                detail = scheme.onSurfaceContainerVariant,
                status = activeStatusCardAccentColor(),
                // 状态绿本身就很浅，再叠一层 alpha 就看不见了，这里直接不透明铺。
                progress = activeStatusCardBackgroundColor(),
            )
        }

        DashboardStyle.Red -> {
            val card = errataCardColors()
            DashboardCardPalette(
                card = card,
                title = card.contentColor,
                detail = card.contentColor.copy(alpha = DetailAlpha),
                status = card.contentColor,
                progress = card.contentColor.copy(alpha = ProgressAlpha),
            )
        }
    }
}
