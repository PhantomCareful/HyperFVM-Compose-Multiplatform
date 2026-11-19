package com.careful.hyperfvm.compose.screen.dataCenter.cookery

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.DefenseCardRow
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.image.PackImage
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import com.careful.hyperfvm.compose.data.cardCatalog.CardCatalog
import com.careful.hyperfvm.compose.data.datapack.PackImages
import com.careful.hyperfvm.compose.data.cookery.CookeryAuxiliaryCard
import com.careful.hyperfvm.compose.data.cookery.CookeryCard
import com.careful.hyperfvm.compose.data.cookery.CookeryRecipe
import com.careful.hyperfvm.compose.data.cookery.CookerySource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 弹层内分节标题的边距，照搬 miuix demo 的 BottomSheet：水平 16dp 叠在弹层
 * 24dp 的 insideMargin 上，正好与卡片内文字（16dp insideMargin）对齐；
 * 纵向 8dp 保留标题上下的呼吸。
 */
private val SheetSectionTitleMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/**
 * 弹层内容卡之间、卡与分节标题之间的标准间距。
 * Java 版里普通卡是 10dp、段末是 25dp，这里统一成 10dp，交给标题的上边距补段间距。
 */
private val SheetCardBottomPadding = 10.dp

/**
 * 食神谱详情弹层，对应 Java 版 `CookeryBottomSheetModal` 加载
 * `fragment_bottom_sheet_cookery_X_Y` 的效果：标题固定在弹层顶部，
 * 下面依次是基础信息、描述、获取途径、合成材料与辅助卡组。
 *
 * [show] 与 [card] 分开传：收起动画期间 [card] 仍需有值，
 * 内容先被清空的话弹层会在半空变空白再滑走。
 */
@Composable
internal fun CookeryDetailSheet(
    card: CookeryCard?,
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (card == null) return
    OverlayBottomSheet(
        show = show,
        title = card.name,
        onDismissRequest = onDismissRequest,
    ) {
        CookeryDetailContent(card)
    }
}

/** 弹层内容：一屏放不下时整段滚动，标题行留给 miuix 的固定槽位。 */
@Composable
private fun CookeryDetailContent(card: CookeryCard) {
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + navigationBarBottom),
    ) {
        item(key = "base-title") { SheetSectionTitle("基础信息") }
        item(key = "base") { BaseInfoCard(card) }
        item(key = "description") { DescriptionCard(card.description) }

        item(key = "sources-title") { SheetSectionTitle("获取途径(不含各种返利活动)") }
        itemsIndexed(
            items = card.sources,
            key = { index, _ -> "source-$index" },
        ) { _, source ->
            SourceCard(source)
        }

        item(key = "recipes-title") { SheetSectionTitle("合成材料") }
        // 每行只放两张材料卡，最后一行的空位补 Spacer 保持列宽一致。
        itemsIndexed(
            items = card.recipes.chunked(2),
            key = { index, _ -> "recipe-row-$index" },
        ) { _, row ->
            RecipeRow(row)
        }

        card.auxiliaryGroups.forEachIndexed { groupIndex, group ->
            item(key = "aux-title-$groupIndex") {
                // 个别分组的标题在数据里是空串，空标题不占位。
                if (group.title.isNotEmpty()) SheetSectionTitle(group.title)
            }
            itemsIndexed(
                items = group.tips,
                key = { index, _ -> "aux-tip-$groupIndex-$index" },
            ) { _, tip ->
                if (tip.isWarning) {
                    ErrataCard(
                        text = tip.text,
                        outerPadding = PaddingValues(bottom = SheetCardBottomPadding),
                    )
                } else {
                    TipsCard(
                        tips = tip.text,
                        outerPadding = PaddingValues(bottom = SheetCardBottomPadding),
                    )
                }
            }
            if (group.cards.isNotEmpty()) {
                itemsIndexed(
                    items = group.cards,
                    key = { index, _ -> "aux-card-$groupIndex-$index" },
                ) { _, auxiliary ->
                    AuxiliaryCardRow(auxiliary)
                }
            }
        }
    }
}

/** 弹层内的分节标题：subtitle 样式的小标题，缩进与卡片内文字对齐。 */
@Composable
private fun SheetSectionTitle(text: String) {
    SmallTitle(text = text, insideMargin = SheetSectionTitleMargin)
}

/** 基础信息卡：左边卡名 + 分类·奖励，右边 60dp 卡图。字号对齐 BasicComponent。 */
@Composable
private fun BaseInfoCard(card: CookeryCard) {
    SheetCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) {
                Text(
                    text = card.name,
                    // 食神谱名称是整张基础信息卡的主角，比 BasicComponent 的
                    // headline1 大两档（title2）。
                    fontSize = MiuixTheme.textStyles.title2.fontSize,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = card.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                )
            }
            PackImage(
                id = card.image,
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** 描述卡：一句话介绍，Java 版是 Summary 色的 15sp 文本。 */
@Composable
private fun DescriptionCard(description: String) {
    SheetCard {
        Text(
            text = description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

/** 获取途径的一条来源：左标题 + 说明，右侧一排 44dp 图标（数量不定）。 */
@Composable
private fun SourceCard(source: CookerySource) {
    SheetCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) {
                Text(
                    text = source.title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = source.description,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                )
            }
            source.icons.forEach { icon ->
                RemoteItemImage(
                    id = icon,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

/** 配方的一行：最多两格，不满时补空位，保证每格宽度与满行一致。 */
@Composable
private fun RecipeRow(row: List<CookeryRecipe>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        row.forEach { recipe ->
            RecipeCell(recipe = recipe, modifier = Modifier.weight(1f))
        }
        repeat(2 - row.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 配方的一格：卡图与星级图分列左右两端，中间留白；图高与增幅名单卡图一致。
 *
 * 内边距与 BasicComponent 保持一致（16dp）。点击暂不响应：防御卡详情页在 Compose 端尚未重制。
 */
@Composable
private fun RecipeCell(recipe: CookeryRecipe, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
        cornerRadius = 20.dp,
        // 同 SheetCard：弹层内的卡统一 secondaryContainer 底色。
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipeImage(id = recipe.cardRes)
            RecipeImage(id = recipe.starRes)
        }
    }
}

/** 配方图高度：与增幅名单卡（DefenseCardRow）的卡图等高，宽度随原图比例。 */
private val RecipeImageHeight = 50.dp

/**
 * 配方图：高度固定，宽度按原图宽高比反推；由所在行的 SpaceBetween 实现左右靠边。
 *
 * 不能只给 height：这批 webp 是 40×50 / 57×55 像素的小图，宽度未约束时按原图像素
 * 尺寸作画会非常小。宽高比改从数据包解码出的 bitmap 取，再配一个固定高度，
 * 实际显示尺寸才与增幅名单卡图（40×50dp）一致。
 */
@Composable
private fun RecipeImage(id: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = PackImages.peek(id), id) {
        if (value == null) value = PackImages.load(id)
    }
    val current = bitmap ?: return
    val aspectRatio = if (current.height > 0) current.width.toFloat() / current.height else 1f
    Image(
        modifier = Modifier
            .height(RecipeImageHeight)
            .aspectRatio(aspectRatio),
        bitmap = current,
        contentDescription = null,
        contentScale = ContentScale.Fit,
    )
}

/**
 * 增幅名单的一行：与防御卡全能数据库共用 [DefenseCardRow]。
 *
 * 生成数据里只记了主名和一张形态图，这里用卡图反查出完整的防御卡，
 * 卡片因此能像目录页一样展示全部名字与形态图；目录里没登记的图
 * （目前不存在）才回退到生成时的名字与单图。
 */
@Composable
private fun AuxiliaryCardRow(card: CookeryAuxiliaryCard) {
    val defenseCard = remember(card.image) { CardCatalog.cardOf(card.image) }
    if (defenseCard != null) {
        DefenseCardRow(
            card = defenseCard,
            modifier = Modifier.padding(bottom = SheetCardBottomPadding),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        )
    } else {
        SheetCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.name,
                    modifier = Modifier.weight(1f),
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                )
                PackImage(
                    id = card.image,
                    modifier = Modifier.size(AuxiliaryCardImageSize),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

/** 回退行的卡图尺寸：Java 版是 wrap_content（随图自身像素），统一到 48dp 更整齐。 */
private val AuxiliaryCardImageSize: Dp = 48.dp

/** 弹层里通用内容卡的壳：20dp 圆角、secondaryContainer 底色、底部 10dp 间距。 */
@Composable
private fun SheetCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
        cornerRadius = 20.dp,
        // 与 miuix demo 的 BottomSheet 一致：弹层内容卡用 secondaryContainer，
        // 与弹层背景区分开。
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
    ) {
        content()
    }
}
