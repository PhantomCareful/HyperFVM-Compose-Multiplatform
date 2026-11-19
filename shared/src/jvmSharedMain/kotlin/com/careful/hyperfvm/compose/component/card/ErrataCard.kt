package com.careful.hyperfvm.compose.component.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 勘误预警卡的配色，取自 Java 版 `?attr/colorErrorContainer` / `?attr/colorOnErrorContainer`。
 *
 * 这两个值来自 miuix Android 的色板（values / values-night），和 miuix Compose
 * 按主题推导出来的 errorContainer 并不是同一个颜色：Java 版是淡粉底 + 亮红文字，
 * 观感更醒目。这里直接写死 Java 的值，两个版本才一致。
 */
private val ErrataContainerLight = Color(0xFFF8E2E2)
private val ErrataContainerDark = Color(0xFF310808)
private val ErrataContent = Color(0xFFF72727)

/** [ErrataCard] 的配色：淡粉底（深色下暗红底）+ 亮红文字。 */
@Composable
fun errataCardColors(): CardColors {
    return CardDefaults.defaultColors(
        color = if (isInDarkTheme()) ErrataContainerDark else ErrataContainerLight,
        contentColor = ErrataContent,
    )
}

/**
 * 勘误卡的文字色，单独给需要「提醒」语义的地方复用（如弹层里的「删除账号」按钮）。
 *
 * 与 [errataCardColors] 的 contentColor 是同一个值，取色处只有这一份。
 */
fun errataContentColor(): Color = ErrataContent

/** [ErrataCard] 的默认外边距：左右 12dp、底部 12dp，即页面里卡片之间的标准间距。 */
val ErrataCardOuterPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)

/**
 * 勘误预警卡：对应 Java 版用 `colorErrorContainer` 的那张卡。
 *
 * 概率公示（目录、详情）与食神谱详情的勘误提示共用；卡片不跳转，只保留按压反馈。
 * [outerPadding] 的约定与 [TipsCard] 一致：已经自带内容边距的容器传 `PaddingValues(0.dp)`
 * 避免双重缩进。
 */
@Composable
fun ErrataCard(
    text: String,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = ErrataCardOuterPadding,
) {
    val colors = errataCardColors()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(outerPadding),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // showIndication = true，同时要给一个 onClick 事件，才能让 Card 显示点击时的视觉反馈效果
        onClick = {},
        cornerRadius = 20.dp,
        colors = colors,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = colors.contentColor,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}
