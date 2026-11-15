package com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.card.importantCardColors
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDisclosureData
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsItem
import com.careful.hyperfvm.compose.data.oddsDisclosure.oddsTextPrecision
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 详情页第二张卡（错误色容器）：这一箱概率公示的勘误说明。 */
private const val InfoErrataTips =
    "本概率公示获取自官方发布的帖子。\n内容为人工整理，若您发现可能有误的地方，请先前往原帖进行核实，如确实有误，请向开发者反馈。"

/**
 * 宝箱概率公示详情页，对应 Java 版 `OddsDisclosureInfoActivity`：
 * 宝箱说明（Important 卡）+ 勘误提醒 + 逐条掉落物概率。
 *
 * 数据不随导航参数传递，而是用类别下标 / 宝箱下标从 [OddsDisclosureData] 里取，
 * 进程重建后恢复同一个 Route 也能拿到完全一样的内容。
 */
@Composable
fun OddsDisclosureInfoScreen(
    categoryIndex: Int,
    boxIndex: Int,
    onBack: () -> Unit,
) {
    val box = OddsDisclosureData.categories.getOrNull(categoryIndex)?.boxes?.getOrNull(boxIndex)
    if (box == null) {
        // 数据表调整后旧返回栈可能指向已经不存在的下标，直接退回目录页。
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // 同一箱内所有概率统一小数位数，避免 "3%" 与 "0.003%" 混排。
    val precision = remember(box) { box.items.oddsTextPrecision }

    ScreenScaffold(
        title = box.name,
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                // 底部与 Java 版一致留 150dp。
                bottom = padding.calculateBottomPadding() + 150.dp,
            ),
        ) {
            item(key = "description") {
                TipsCard(tips = box.description)
            }
            item(key = "errata") {
                ErrataCard(text = InfoErrataTips)
            }
            itemsIndexed(
                items = box.items,
                key = { index, _ -> "item-$index" },
            ) { _, item ->
                OddsItemCard(item = item, precision = precision)
            }
        }
    }
}

/**
 * 单条掉落物：左边 44dp 图标，中间「名称×数量」，右边概率。
 *
 * 重点关注项（转职凭证 / 武器等）用 Important 卡突出，与 Java 版的
 * `item_odds_item_expected` 一致；其余用普通内容卡。
 */
@Composable
private fun OddsItemCard(item: OddsItem, precision: Int) {
    val colors = if (item.expected) importantCardColors() else CardDefaults.defaultColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
        cornerRadius = 20.dp,
        colors = colors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(44.dp),
                painter = painterResource(item.image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
            Text(
                text = item.nameAndCount,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                color = colors.contentColor,
                // 与目录页 ArrowPreference 的 title 同号（miuix headline1 = 17sp），
                // 两页的条目名称才不会一粗一大。
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.oddsText(precision),
                // 普通卡的概率用 Java 版 colorOnSurfaceVariant 对应的次级文字色；
                // Important 卡底色不是 surface，改用配套的 on*Container 才不糊。
                color = if (item.expected) {
                    colors.contentColor
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
