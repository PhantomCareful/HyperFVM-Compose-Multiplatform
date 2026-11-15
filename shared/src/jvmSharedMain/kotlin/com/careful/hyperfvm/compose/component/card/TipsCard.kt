package com.careful.hyperfvm.compose.component.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_MONET_DARK
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_MONET_SYSTEM
import com.careful.hyperfvm.compose.ui.theme.LocalColorMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * Important 卡的配色，对应 Java 版的 `ImportantCardViewBackground` / `TextViewColorOnImportantCardView`。
 * miuix 没有「警告/提示」语义色：Monet 动态取色下用 secondaryContainer 系，
 * miuix 静态色板下用 tertiaryContainer 系，都取各自的 on* 配套色。
 */
@Composable
fun importantCardColors(): CardColors {
    val isMonet = LocalColorMode.current in COLOR_MODE_MONET_SYSTEM..COLOR_MODE_MONET_DARK
    return CardDefaults.defaultColors(
        color = if (isMonet) {
            MiuixTheme.colorScheme.secondaryContainer
        } else {
            MiuixTheme.colorScheme.tertiaryContainer
        },
        contentColor = if (isMonet) {
            MiuixTheme.colorScheme.onSecondaryContainer
        } else {
            MiuixTheme.colorScheme.onTertiaryContainer
        },
    )
}

/** [TipsCard] 的默认外边距：左右 12dp、底部 12dp，即页面里卡片之间的标准间距。 */
val TipsCardOuterPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)

/** 页面顶部的 tips 卡：一行说明文字，对应 Java 版的 Important 卡。
 *
 * [onClick] 不为空时整张卡可点击（如概率公示目录页点击它打开官方原帖）；
 * 无论是否可点击都保留 Sink 按压反馈，与 Java 版的统一手感一致。
 * [outerPadding] 按调用方的版心调整：已经自带内容边距的容器（如瀑布流）传
 * `PaddingValues(0.dp)` 即可，避免双重缩进。
 * [colors] 默认是 Important 配色；个别页面需要别的语义色（如查黑「没有记录」用状态卡绿）
 * 时传入自定义 [CardColors]，正文色跟随 `contentColor`。
 */
@Composable
fun TipsCard(
    tips: String,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = TipsCardOuterPadding,
    onClick: (() -> Unit)? = null,
    colors: CardColors? = null,
) {
    val cardColors = colors ?: importantCardColors()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(outerPadding),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // showIndication = true，同时要给一个 onClick 事件，才能让 Card 显示点击时的视觉反馈效果
        onClick = onClick ?: {},
        cornerRadius = 20.dp,
        colors = cardColors,
    ) {
        Text(
            text = tips,
            modifier = Modifier.padding(16.dp),
            color = cardColors.contentColor,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}
