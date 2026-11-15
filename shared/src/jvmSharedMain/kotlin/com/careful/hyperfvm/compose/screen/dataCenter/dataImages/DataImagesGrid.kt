package com.careful.hyperfvm.compose.screen.dataCenter.dataImages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.data.image.DataImageCategories
import com.careful.hyperfvm.compose.data.image.DataImageCategory
import com.careful.hyperfvm.compose.data.image.DataImageEntry
import com.careful.hyperfvm.compose.data.image.DataImageThumbnails
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
 * 分类/分组标题相对瀑布流内容边距的额外缩进。
 *
 * 瀑布流的内容边距是 12dp，加上这里的 16dp 正好 28dp，
 * 与 miuix 的 `SmallTitle` 默认内边距一致，各级标题的左缘才对得上其它页面。
 */
val HeaderInset = 16.dp

/** 瀑布流里的一条：分类标题、分类内分组标题、或一张图。 */
sealed interface DataImageGridItem {
    data class CategoryHeader(val category: DataImageCategory) : DataImageGridItem

    data class GroupHeader(val title: String) : DataImageGridItem

    data class Image(val entry: DataImageEntry, val categoryId: String) : DataImageGridItem
}

/**
 * 把六份分类目录拍平成瀑布流的一维列表。
 *
 * 目录是静态的，展开一次即可；顺序与 Java 版 index 页的卡片顺序一致，
 * 分类内如果声明了 [DataImageEntry.group]，在组切换处插一条分组标题。
 */
val DataImageGridItems: List<DataImageGridItem> = buildList {
    DataImageCategories.forEach { category ->
        add(DataImageGridItem.CategoryHeader(category))
        var currentGroup: String? = null
        category.images.forEach { entry ->
            val group = entry.group
            if (group != null && group != currentGroup) {
                add(DataImageGridItem.GroupHeader(group))
                currentGroup = group
            }
            add(DataImageGridItem.Image(entry, category.id))
        }
    }
}

/** 瀑布流最前面固定放着「检查更新」与「删除所有图片」两张卡，目录跳转要加上它们。 */
const val DataImageLeadingItemCount = 2

/**
 * 每个分类标题在瀑布流里的条目下标，目录菜单靠它做 [androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState.animateScrollToItem]。
 *
 * 目录是静态的，展开一次即可；下标与 [DataImagesScreen] 里 item 的声明顺序严格对应。
 */
val DataImageCategoryItemIndices: List<Int> = buildList {
    DataImageGridItems.forEachIndexed { index, item ->
        if (item is DataImageGridItem.CategoryHeader) add(index + DataImageLeadingItemCount)
    }
}

/** 分类标题行：左侧标题，右侧作者署名（部分分类没有作者），下面可跟一张 tips 卡。 */
@Composable
fun DataImageCategoryHeader(category: DataImageCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    ) {
        // 只有标题这一行多缩进；下面的 tips 卡要跟普通卡片一样贴齐内容边距。
        Row(
            modifier = Modifier.padding(horizontal = HeaderInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.title,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = MiuixTheme.textStyles.title3.fontSize,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f))
            val author = category.author
            if (!author.isNullOrEmpty()) {
                // 署名用 miuix SmallTitle 的样式（subtitle：比标题小一号的加粗小字 +
                // 次要色），与页面其它位置的 SmallTitle 同规格，把「作者」降为附注。
                Text(
                    text = author,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val tips = category.tips
        if (!tips.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            // 复用页面通用的 TipsCard，配色与排版跟其它页保持一致；
            // 瀑布流自身已有 12dp 内容边距，这里把它的外边距清零，避免双重缩进。
            TipsCard(tips = tips, outerPadding = PaddingValues(0.dp))
        }
    }
}

/** 分类内的分组标题（目前只有米鼠的图用到：氪金活动类 / 游戏资料类）。 */
@Composable
fun DataImageGroupHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = HeaderInset, top = 12.dp, bottom = 2.dp),
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        fontWeight = FontWeight.Medium,
    )
}

/**
 * 瀑布流里的一张数据图卡片：顶部是图片预览，下面是标题与副标题。
 *
 * [ratio] 是原图的宽高比（宽/高）。预览高度按列宽换算后夹在 140..280dp：
 * 数据图普遍极长，不夹的话一张卡就能占满整屏；夹住以后不同长宽比的图会有自然的高低差，
 * 瀑布流才有意义。未下载时用 0.5 的默认比例占个位。
 *
 * [onClick] 的回调参数是**预览区**的窗口矩形，而不是整张卡的：查看器拿它当转场起点，
 * 用整卡矩形会让动画凭空多出下面标题那半截。
 *
 * [onPreviewBounds] 把同一矩形按文件名持续上报给页面，由页面收集成快照：查看器里
 * 翻到任意一张再退出时，要飞回的是**当前这张**卡片的位置，只有页面手里有全量矩形才做得到。
 * 卡片滚出屏幕被回收时上报 null，避免快照里留下过期的位置。
 *
 * [onPreviewBitmap] 上报卡片正在显示的那张缩略图，同样按文件名收集：查看器打开
 * 那一刻直接拿它当转场首帧内容——它一定和用户眼前这块预览是同一张，比去缩略图缓存里
 * 碰运气（LRU 只有几张）可靠得多。
 *
 * [fileRevision] 是本地文件集的版本号：图片被同名覆盖时 [file] 与路径都不变，
 * [DataImagePreview] 内部靠它知道该重新解码。
 *
 * [cornerRadius] 与 [colors] 用来适配不同容器：合集页用默认值，仪表盘详情弹层里
 * 要跟弹层其它内容卡一样（20dp 圆角、secondaryContainer 底色），否则在弹层背景上
 * 会是一块颜色对不上的卡片。
 *
 * [onLongClick] 是「对这张图做事」：长按不进查看器，只拉起保存 / 分享面板，
 * 与公告正文里的配图一致。未下载时传 null。
 */
@Composable
fun DataImageCard(
    entry: DataImageEntry,
    file: File?,
    ratio: Float,
    fileRevision: Long = 0L,
    cornerRadius: Dp = 16.dp,
    colors: CardColors = CardDefaults.defaultColors(),
    onPreviewBounds: (String, Rect?) -> Unit = { _, _ -> },
    onPreviewBitmap: (String, ImageBitmap?) -> Unit = { _, _ -> },
    onClick: (Rect?) -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    var previewBounds by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current

    DisposableEffect(entry.name) {
        onDispose {
            onPreviewBounds(entry.name, null)
            onPreviewBitmap(entry.name, null)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = cornerRadius,
        // 预览图要贴到卡片边缘，内边距由下面的内容自己加。
        insideMargin = PaddingValues(0.dp),
        colors = colors,
        showIndication = true,
        onClick = { onClick(previewBounds) },
        onLongPress = onLongClick,
    ) {
        Column {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        // 不能用 boundsInWindow：它会把矩形裁到窗口内，卡片有一部分
                        // 滚出屏幕上（下）边界时 top 会被压成 0、高度变矮，查看器的
                        // 起落点跟着算错。positionInWindow + 自身尺寸才是完整矩形，
                        // 屏幕外那部分交给屏幕自己裁。
                        val bounds = Rect(it.positionInWindow(), it.size.toSize())
                        previewBounds = bounds
                        onPreviewBounds(entry.name, bounds)
                    },
            ) {
                val columnWidth = maxWidth
                val previewHeight = (columnWidth / ratio.coerceIn(0.15f, 2f))
                    .coerceIn(140.dp, 280.dp)
                if (file != null) {
                    DataImagePreview(
                        file = file,
                        fileRevision = fileRevision,
                        height = previewHeight,
                        columnWidth = with(density) { columnWidth },
                        onBitmap = { onPreviewBitmap(entry.name, it) },
                    )
                } else {
                    DataImageMissingPreview(height = previewHeight)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = entry.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = MiuixTheme.textStyles.body1.fontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val summary = entry.summary
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        modifier = Modifier.padding(top = 2.dp),
                        // 摘要色同 miuix 的 BasicComponent：两处都写成同一个色，
                        // 卡片跟着容器换底色时（如仪表盘弹层）也不会偏色。
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DataImagePreview(
    file: File,
    fileRevision: Long,
    height: Dp,
    columnWidth: Dp,
    onBitmap: (ImageBitmap?) -> Unit,
) {
    val targetWidthPx = with(LocalDensity.current) { columnWidth.roundToPx() }
    // 预览区域的高宽比，告诉解码器纵向需要解多高。
    val heightRatio = height / columnWidth

    // file 在图片更新时路径不变（同名覆盖）：localFiles 重算出来的 File 与旧值相等，
    // 页面不会因此重组，所以单独读一遍 lastModified() 不够——能不能重组都成问题。
    // fileRevision 是下载/删除时自增的文件集版本，作为 key 才能确保覆盖后重新解码。
    val thumbnail by produceState<ImageBitmap?>(
        initialValue = null,
        file,
        fileRevision,
        file.lastModified(),
        targetWidthPx,
        heightRatio,
    ) {
        value = DataImageThumbnails.load(file, targetWidthPx, heightRatio)
    }

    // 解码结果同步给页面：打开查看器时它就是转场的第一帧内容。
    // 复用缓存里的同一张位图，不再多占内存。
    LaunchedEffect(thumbnail) { onBitmap(thumbnail) }

    // 卡片稳定显示后再给查看器预热一张「整图低清」：用户点开时它就是转场
    // 全程的内容（完整图），而不是只有卡片这块条带。延迟是为了避开首帧与
    // 快速滚动：扫过去的卡片 LaunchedEffect 会被取消，不会把整图解码堆到滚动上。
    // 250ms 对应「停下来看一眼」的停顿：再长的话，看到图就点开的人容易扑空；
    // 解码本身不受取消影响，一旦开始就会在后台跑完并进缓存。
    LaunchedEffect(thumbnail, file) {
        if (thumbnail != null) {
            delay(250)
            DataImageThumbnails.loadViewerPreview(file)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = thumbnail
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                // 顶部对齐裁切：数据图的标题区总在顶部，裁掉下半截不会让人看不懂。
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        } else {
            DecodingPlaceholder()
        }
    }
}

@Composable
private fun DecodingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = MiuixIcons.Image,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

@Composable
private fun DataImageMissingPreview(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.Image,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Text(
                text = "未下载",
                modifier = Modifier.padding(top = 6.dp),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
            )
        }
    }
}
