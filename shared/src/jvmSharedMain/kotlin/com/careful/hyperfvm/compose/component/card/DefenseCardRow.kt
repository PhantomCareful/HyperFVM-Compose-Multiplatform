package com.careful.hyperfvm.compose.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.image.PackImage
import com.careful.hyperfvm.compose.data.cardCatalog.DefenseCard
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 卡片文字与右侧卡图之间的间距（BasicComponent 的 start/center/end 间距）。 */
private val CardActionSpacing = 8.dp

/**
 * 一张防御卡：左侧主名 + 其余形态名，右侧按顺序排全部卡图。
 *
 * 排版参照 `BasicComponent`：16dp insideMargin、headline1 Medium 标题、body2 摘要、
 * 文字区与右侧卡图之间 8dp 间距。防御卡全能数据库与食神谱的增幅名单共用本组件，
 * 两处的卡片布局因此始终一致。
 *
 * @param colors 卡片配色；两处场景底色不同由调用方传入。
 */
@Composable
internal fun DefenseCardRow(
    card: DefenseCard,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.defaultColors(),
) {
    val alias = remember(card) { card.aliasText }
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        insideMargin = BasicComponentDefaults.InsideMargin,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        colors = colors,
        // 详情页还没重制：点击只给按压反馈，不做跳转。
        onClick = {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.primaryName,
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (alias.isNotEmpty()) {
                    Text(
                        text = alias,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(CardActionSpacing))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                card.forms.forEach { form ->
                    PackImage(
                        id = form.image,
                        modifier = Modifier.size(width = 40.dp, height = 50.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}
