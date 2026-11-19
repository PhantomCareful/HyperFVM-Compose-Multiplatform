package com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.card.importantCardColors
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import com.careful.hyperfvm.compose.component.image.prefetchItemImages
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDataRepository
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsItem
import com.careful.hyperfvm.compose.data.oddsDisclosure.oddsTextPrecision
import com.careful.hyperfvm.compose.data.oddsDisclosure.resolvedErrata
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 宝箱概率公示详情页，对应 Java 版 `OddsDisclosureInfoActivity`：
 * 宝箱说明（Important 卡）+ 勘误提醒 + 逐条掉落物概率。
 *
 * 数据不随导航参数传递，而是用类别下标 / 宝箱下标从 [OddsDataRepository]
 * （数据包内的 `odds_boxes.json`）里取，进程重建后恢复同一个 Route 也能拿到完全一样的内容。
 */
@Composable
fun OddsDisclosureInfoScreen(
    categoryIndex: Int,
    boxIndex: Int,
    onBack: () -> Unit,
) {
    val box = OddsDataRepository.boxCategories.getOrNull(categoryIndex)?.boxes?.getOrNull(boxIndex)
    if (box == null) {
        // 数据表调整后旧返回栈可能指向已经不存在的下标，直接退回目录页。
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // 同一箱内所有概率统一小数位数，避免 "3%" 与 "0.003%" 混排。
    val precision = remember(box) { box.items.oddsTextPrecision }
    // 图标从图床按 id 取：进页先把整箱的图拉下来，滚动时不用逐行等下载。
    val env = LocalAppEnv.current
    LaunchedEffect(box) {
        prefetchItemImages(env, box.items.mapNotNull { it.remoteId ?: it.image })
    }
    // 说明卡：描述文字没有默认文案，null 表示这箱不显示说明卡。
    val description = box.description
    // 勘误卡：null 不显示；空串用统一默认文案；非空为宝箱自己的文案。
    val errata = box.errata.resolvedErrata

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
            if (description != null) {
                item(key = "description") {
                    TipsCard(tips = description)
                }
            }
            if (errata != null) {
                item(key = "errata") {
                    ErrataCard(text = errata)
                }
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
 * [OddsItem.image] 与 [OddsItem.remoteId] 都是道具 id，统一由 [RemoteItemImage] 从图床取图，
 * 不再读数据包；两者都为空时（如幸运转转）左侧图标位整个省掉，文字直接顶到卡片内边距。
 * 下载中或失败时留 44dp 空位，卡片高度不会因图片晚到而抖动。
 * 重点关注项（转职凭证 / 武器等）用 Important 卡突出，与 Java 版的
 * `item_odds_item_expected` 一致；其余用普通内容卡。
 *
 * 抽奖详情页（[LotteryOddsInfoScreen]）的奖品列表复用同一张卡，保证两页排版一致。
 */
@Composable
internal fun OddsItemCard(item: OddsItem, precision: Int) {
    val colors = if (item.expected) importantCardColors() else CardDefaults.defaultColors()
    val imageId = item.remoteId ?: item.image
    AppCard(
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
            if (imageId != null) {
                RemoteItemImage(
                    id = imageId,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = item.nameAndCount,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (imageId != null) 16.dp else 0.dp,
                        end = 16.dp,
                    ),
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
