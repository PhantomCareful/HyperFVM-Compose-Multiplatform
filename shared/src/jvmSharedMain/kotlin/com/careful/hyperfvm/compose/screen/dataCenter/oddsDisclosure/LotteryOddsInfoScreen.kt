package com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.image.prefetchItemImages
import com.careful.hyperfvm.compose.data.oddsDisclosure.TurntableOddsRepository
import com.careful.hyperfvm.compose.data.oddsDisclosure.oddsTextPrecision
import com.careful.hyperfvm.compose.data.oddsDisclosure.resolvedErrata
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.screen.ScreenScaffold

/**
 * 抽奖概率详情页，结构与 [OddsDisclosureInfoScreen] 同构：
 * 抽奖说明（TipsCard）+ 勘误提醒 + 逐条奖品概率。
 *
 * 说明卡与勘误卡：说明文案按抽奖取用（见 [TurntableOddsRepository] 里的 `DrawDescriptions`，
 * 四个抽奖各一段），没配文案的场次不显示说明卡；勘误卡默认用统一文案
 * [com.careful.hyperfvm.compose.data.oddsDisclosure.OddsErrataTips]，抽奖数据里可以单独覆盖，
 * 配成 null 时整张卡不显示。两张卡都只展示文字，不挂外链：官方来源统一从目录页右上角的
 * 「官方公示合集」查看。
 *
 * 数据不随导航参数传递，而是用类别下标 / 抽奖下标从 [lotteryCategories] 里取；
 * 进程重建后内存里可能还没有解析结果，这时先补拉一次，确实没有这场抽奖才退回目录页。
 */
@Composable
fun LotteryOddsInfoScreen(
    categoryIndex: Int,
    drawIndex: Int,
    onBack: () -> Unit,
) {
    var categories by remember { mutableStateOf(lotteryCategories()) }
    LaunchedEffect(Unit) {
        if (categories.getOrNull(categoryIndex)?.draws?.getOrNull(drawIndex) == null) {
            TurntableOddsRepository.refresh()
            categories = lotteryCategories()
            if (categories.getOrNull(categoryIndex)?.draws?.getOrNull(drawIndex) == null) onBack()
        }
    }
    val category = categories.getOrNull(categoryIndex)
    // 补拉期间先留白，不闪一下空详情页；拉不到就由上面的回调退回目录页。
    val draw = category?.draws?.getOrNull(drawIndex) ?: return

    // 同一场抽奖内所有概率统一小数位数，避免 "3%" 与 "0.003%" 混排。
    val precision = remember(draw) { draw.prizes.oddsTextPrecision }
    // 图标从图床按 id 取：进页先把整场抽奖的图拉下来，滚动时不用逐行等下载。
    val env = LocalAppEnv.current
    LaunchedEffect(draw) {
        prefetchItemImages(env, draw.prizes.mapNotNull { it.remoteId ?: it.image })
    }
    // 说明卡：描述文字没有默认文案，null 表示这场不显示说明卡。
    val description = draw.description
    // 勘误卡：null 不显示；空串用统一默认文案；非空为这场抽奖自己的文案。
    val errata = draw.errata.resolvedErrata

    ScreenScaffold(
        title = draw.name,
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
                item(key = "tips") {
                    // 文案按抽奖取用：在 TurntableOddsRepository 的 DrawDescriptions 里维护。
                    TipsCard(tips = description)
                }
            }
            if (errata != null) {
                item(key = "errata") {
                    ErrataCard(text = errata)
                }
            }
            itemsIndexed(
                items = draw.prizes,
                key = { index, _ -> "prize-$index" },
            ) { _, prize ->
                OddsItemCard(item = prize, precision = precision)
            }
        }
    }
}
