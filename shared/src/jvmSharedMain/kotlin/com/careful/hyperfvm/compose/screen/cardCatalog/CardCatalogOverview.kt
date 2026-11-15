package com.careful.hyperfvm.compose.screen.cardCatalog

import com.careful.hyperfvm.compose.shared.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.statusCardAccentColor
import com.careful.hyperfvm.compose.component.card.statusCardBackgroundColor
import com.careful.hyperfvm.compose.data.cardCatalog.CardCatalog
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 概览卡片之间的间距，与目录列表的卡片间距保持一致。 */
private val OverviewGap = 10.dp

/** 概览卡内边距，对应 Java 版大卡 / 小卡各自的 10dp padding。 */
private val OverviewInsidePadding = 10.dp

/** 概览卡圆角，与目录卡一致。 */
private val OverviewCornerRadius = 20.dp

/** 大卡右下角水印图标的尺寸。图标紧贴右下角，又要避开左下角数值：
 * 「373张」右缘、版本号底边到卡片边缘都只剩约 88dp，所以取 84dp（图形本身还有约 6dp 透明边）。 */
private val StatusIconSize = 84.dp

/**
 * 目录顶部的概览区，对应 Java 版 `item_card_catalog_page_header.xml` 里的概览卡片：
 * 左边一张「当前版本 / 收录卡片」大卡（占 2/4 宽），右边两列各两张分表小卡
 * （普通卡、金卡｜融合卡、生肖卡）。
 *
 * Java 版页首还带一个随滚动淡出的大标题，Compose 版由脚手架顶栏自己的大标题承担，
 * 这里只保留卡片。五张卡都可点：大卡 TILT、小卡 SINK 的按压形变 + miuix 水波纹，
 * 与 Java 版一致没有跳转动作。
 *
 * 名字不带 `CardCatalog` 前缀，避免与数据层的 `CardCatalogOverview` 撞名。
 */
@Composable
internal fun CatalogOverview(modifier: Modifier = Modifier) {
    val overview = CardCatalog.overview
    val entries = remember(overview) {
        listOf(
            "普通卡" to overview.normalCount,
            "金卡" to overview.goldenCount,
            "融合卡" to overview.fusionCount,
            "生肖卡" to overview.zodiacCount,
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 大卡要跟右侧两列小卡等高：先按内容固有高度定下行高，再按权重分配宽度。
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(OverviewGap),
    ) {
        OverviewBigCard(
            version = overview.version,
            totalCount = overview.totalCount,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
        )
        OverviewSmallColumn(
            entries = entries.take(2),
            modifier = Modifier.weight(1f),
        )
        OverviewSmallColumn(
            entries = entries.drop(2),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 大卡配色：背景与正文色都用共享的状态卡取值（[statusCardBackgroundColor]），
 * 正文色沿用默认的 onSurfaceContainer，深浅色下都与底色成对。
 */
@Composable
private fun statusCardColors(): CardColors = CardDefaults.defaultColors(
    color = statusCardBackgroundColor(),
)

/**
 * 概览大卡：左上「当前版本 + 版本号」、左下「收录卡片 + 张数」，
 * 右下角放一张卡片堆叠图标当水印，配色对齐 KernelSU 的运行状态卡。
 * 图标要贴住右下角，所以卡片本身不留内边距（insideMargin = 0），文字自已带 10dp 内边距。
 */
@Composable
private fun OverviewBigCard(
    version: String,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = statusCardColors()
    // 标题用内容色降透明（同 Java 版状态卡的摘要色），数值用不透明的正文色。
    val titleColor = colors.contentColor.copy(alpha = 0.7f)
    // 同 KernelSU：Monet 下图标取主题色 0.8 透明度，静态色板下用固定的状态绿。
    val iconTint = statusCardAccentColor()
    Card(
        modifier = modifier,
        cornerRadius = OverviewCornerRadius,
        insideMargin = PaddingValues(0.dp),
        colors = colors,
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = true,
        // 只有按压反馈，没有跳转动作（同 Java 版概览卡）。
        onClick = {},
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 图标贴着卡片右下角；矢量图四周的透明留白已在 ic_cards.xml 里裁掉，
            // 图形轮廓直接顶到卡片边缘，且图形右下角本身是空的，不会被 20dp 圆角切到。
            Icon(
                painter = painterResource(Res.drawable.ic_cards),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(StatusIconSize),
                tint = iconTint,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OverviewInsidePadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                OverviewStat(
                    title = "当前版本",
                    value = version,
                    titleColor = titleColor,
                    valueColor = colors.contentColor,
                )
                OverviewStat(
                    title = "收录卡片",
                    value = "${totalCount}张",
                    titleColor = titleColor,
                    valueColor = colors.contentColor,
                )
            }
        }
    }
}

/** 一列两张分表小卡，两张之间同样留 [OverviewGap]。 */
@Composable
private fun OverviewSmallColumn(
    entries: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(OverviewGap),
    ) {
        entries.forEach { (title, count) ->
            OverviewSmallCard(
                title = title,
                count = count,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 概览小卡：一行标题 + 一行张数，一列四张分别统计各卡表。 */
@Composable
private fun OverviewSmallCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        cornerRadius = OverviewCornerRadius,
        insideMargin = PaddingValues(OverviewInsidePadding),
        colors = CardDefaults.defaultColors(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
    ) {
        OverviewStat(
            title = title,
            value = count.toString(),
            titleColor = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            valueColor = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}

/**
 * 概览卡里的「小标题 + 数值」两行：标题 15sp 量级（body2）、数值 20sp 加粗（title3），
 * 行间 2dp，同 Java 版布局的 textSize / marginBottom。
 */
@Composable
private fun OverviewStat(
    title: String,
    value: String,
    titleColor: Color,
    valueColor: Color,
) {
    Column {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body2,
            color = titleColor,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
        )
    }
}
