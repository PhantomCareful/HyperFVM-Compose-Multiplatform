package com.careful.hyperfvm.compose.component.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.data.catalog.CatalogItem
import com.careful.hyperfvm.compose.data.catalog.CatalogRenewal
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 弹层内分节标题的边距，与仪表盘、食神谱弹层保持一致。 */
private val SheetSectionTitleMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** 弹层内容卡之间的标准间距。 */
private val SheetCardBottomPadding = 10.dp

/** 并排卡片之间的横向间距，与概览卡的卡间距一致。 */
private val SheetGridGap = 10.dp

/** 弹层内容卡统一圆角，与项目其它弹层一致。 */
private val SheetCardCornerRadius = 20.dp

/**
 * 合集条目的详情弹层：标题固定为条目名，内容分「基础信息」与「续费信息」两节。
 *
 * 基础信息是性别、体力两张日期卡加一整行描述；续费信息按 7天 / 30天 / 永久
 * 依次排进双列卡片，落单的「永久」与日期卡一样保持半宽。个别条目在
 * market_item.xml 里查不到（如 5级公会制服(男)），此时给一条提示卡。
 *
 * [show] 与 [item] 分开传：收起动画期间 [item] 仍需有值，
 * 内容先被清空的话弹层会在半空变空白再滑走。
 */
@Composable
fun CatalogDetailSheet(
    item: CatalogItem?,
    show: Boolean,
    onDismissRequest: () -> Unit,
    noun: String,
) {
    if (item == null) return
    OverlayBottomSheet(
        show = show,
        title = item.name,
        onDismissRequest = onDismissRequest,
    ) {
        CatalogDetailContent(item = item, noun = noun)
    }
}

/** 弹层内容：一屏放不下时整段滚动，标题行留给 miuix 的固定槽位。 */
@Composable
private fun CatalogDetailContent(item: CatalogItem, noun: String) {
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + navigationBarBottom),
    ) {
        item(key = "base-title") { SheetSectionTitle("基础信息") }
        item(key = "base") { BaseInfoGrid(item) }
        item(key = "description") { DescriptionCard(item.description) }

        item(key = "renewal-title") { SheetSectionTitle("续费信息") }
        if (item.renewals.isEmpty()) {
            item(key = "renewal-empty") {
                TipsCard(
                    tips = "该${noun}在兑换商店里没有续费档位，暂未收录价格。",
                    outerPadding = PaddingValues(bottom = SheetCardBottomPadding),
                )
            }
        } else {
            item(key = "renewal") { RenewalGrid(item.renewals) }
        }
    }
}

/** 弹层内的分节标题：边距与其它详情弹层一致。 */
@Composable
private fun SheetSectionTitle(text: String) {
    SmallTitle(text = text, insideMargin = SheetSectionTitleMargin)
}

/** 基础信息一行两张卡：性别、体力，行内按内容高度对齐。 */
@Composable
private fun BaseInfoGrid(item: CatalogItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(SheetGridGap),
    ) {
        DetailFieldCard(
            label = "性别",
            value = item.sex.label,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        DetailFieldCard(
            label = "体力",
            value = "+${item.hp}",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

/** 一张基础信息卡：body2 标签 + title3 加粗数值，与仪表盘弹层的日期卡同规格。 */
@Composable
private fun DetailFieldCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        cornerRadius = SheetCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // 信息卡只有按压反馈，没有点击动作。
        onClick = {},
    ) {
        Column {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}

/**
 * 描述卡：整行一段说明，样式与温馨礼包的「使用说明」卡一致
 * （body2、onBackground 色、secondaryContainer 底）。
 * 源数据里没有描述的条目显示一行变体色的「暂无描述」。
 */
@Composable
private fun DescriptionCard(description: String?) {
    val text = description?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        cornerRadius = SheetCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
    ) {
        Text(
            text = text ?: "暂无描述",
            style = MiuixTheme.textStyles.body2,
            color = if (text != null) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.onSurfaceContainerVariant
            },
        )
    }
}

/**
 * 续费双列：7天 / 30天 / 永久 依次排进两列，每行按内容高度对齐。
 * 三档落单时最后一张卡保持半宽，右侧补空位，不会被拉满一行。
 */
@Composable
private fun RenewalGrid(renewals: List<CatalogRenewal>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        verticalArrangement = Arrangement.spacedBy(SheetGridGap),
    ) {
        renewals.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(SheetGridGap),
            ) {
                row.forEach { renewal ->
                    RenewalCard(
                        renewal = renewal,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 一档续费：body2 档位名 + title3 加粗售价。 */
@Composable
private fun RenewalCard(renewal: CatalogRenewal, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        cornerRadius = SheetCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
    ) {
        Column {
            Text(
                text = renewal.duration,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = renewal.price,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}
