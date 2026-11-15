package com.careful.hyperfvm.compose.component.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.DefenseCardRow
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.card.activeStatusCardBackgroundColor
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerItem
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerState
import com.careful.hyperfvm.compose.data.cardCatalog.CardCatalog
import com.careful.hyperfvm.compose.data.dashboard.DashboardCardId
import com.careful.hyperfvm.compose.data.dashboard.DashboardLink
import com.careful.hyperfvm.compose.data.dashboard.DashboardPreviousRoundHint
import com.careful.hyperfvm.compose.data.dashboard.DashboardSession
import com.careful.hyperfvm.compose.data.dashboard.DashboardSessionLayout
import com.careful.hyperfvm.compose.data.dashboard.DashboardSheet
import com.careful.hyperfvm.compose.data.dashboard.DashboardTimeLine
import com.careful.hyperfvm.compose.data.dashboard.DashboardTimesLayout
import com.careful.hyperfvm.compose.data.image.AllDataImages
import com.careful.hyperfvm.compose.data.image.DataImageEntry
import com.careful.hyperfvm.compose.data.image.DataImageStore
import com.careful.hyperfvm.compose.data.image.DataImageThumbnails
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.Toaster
import com.careful.hyperfvm.compose.screen.dataCenter.dataImages.DataImageCard
import com.careful.hyperfvm.compose.ui.isTabletLayout
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.io.File

/** 弹层内分节标题的边距，与其它详情弹层保持一致。 */
private val SheetSectionTitleMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** 弹层内容卡之间的标准间距。 */
private val SheetCardBottomPadding = 10.dp

/** 并排卡片之间的横向间距，与概览卡的卡间距一致。 */
private val SheetGridGap = 10.dp

/** 底部按钮之间的横向间距，也是按钮区与上文之间的间距。 */
private val SheetButtonGap = 8.dp

/** 卡片统一圆角，与项目其它弹层内容卡一致。 */
private val SheetCardCornerRadius = 20.dp

/** 「米鼠的图」预览卡没读到真实宽高比时的占位比例（宽/高），与数据图合集页一致。 */
private const val FallbackAspectRatio = 0.5f

/** 绿底时间卡摘要的透明度：对“当前版本”那一档，取内容色的 70%。 */
private const val TimeLineSummaryAlpha = 0.7f

/**
 * 数据层在 note 里写「长按场次可添加到系统日历」用的特征词。
 *
 * 桌面端没有系统日历（[com.careful.hyperfvm.compose.platform.PlatformCapabilities.addToCalendar] 为 false），
 * 整句说明一并隐藏，否则会告诉用户一个不存在的操作。
 */
private const val CalendarNoteKey = "添加到系统日历"

/**
 * 仪表盘卡片的通用详情弹层。
 *
 * 各卡按数据里带回来的字段自由组合：时间概览卡、返场卡片名单、版本不足提示、
 * 米鼠的图预览卡、场次列表（长按加日程）与底部链接按钮（外链或站内阅读的
 * B 站图文动态）。
 * 活动状态不在这里重复展示——卡片右侧已经写了一遍；弹层也不是弹窗，
 * 下滑、点空白或返回手势就能收起，所以没有关闭按钮。
 *
 * [show] 与 [sheet] 分开传：收起动画期间 [sheet] 仍需有值，
 * 内容先被清空的话弹层会在半空变空白再滑走。
 * 点开米鼠的图时弹层保持不动：查看器挂在应用根层（见 DataImageViewerHost），
 * 盖在弹层之上，关掉查看器正好回到这里接着看。
 */
@Composable
internal fun DashboardDetailSheet(
    sheet: DashboardSheet?,
    show: Boolean,
    onDismissRequest: () -> Unit,
    onOpenImage: (DataImageViewerState) -> Unit,
    onLink: (DashboardLink) -> Unit,
) {
    if (sheet == null) return
    OverlayBottomSheet(
        show = show,
        title = sheet.title,
        onDismissRequest = onDismissRequest,
    ) {
        DashboardDetailContent(
            sheet = sheet,
            onOpenImage = onOpenImage,
            onLink = onLink,
        )
    }
}

/** 弹层内容：一屏放不下时整段滚动，标题行留给 miuix 的固定槽位。 */
@Composable
private fun DashboardDetailContent(
    sheet: DashboardSheet,
    onOpenImage: (DataImageViewerState) -> Unit,
    onLink: (DashboardLink) -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    // 桌面端没有系统日历，长按场次的入口与「长按可添加到日历」的说明一并消失。
    val canAddToCalendar = env.capabilities.addToCalendar
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + navigationBarBottom),
    ) {
        // 错误色提示卡固定在最顶部（百万消费的「适度游戏，理性消费」）。
        val errata = sheet.errata
        if (errata != null) {
            item(key = "errata") {
                ErrataCard(
                    text = errata,
                    // 弹层内容区已有左右内边距，这里只补卡间距。
                    outerPadding = PaddingValues(bottom = SheetCardBottomPadding),
                )
            }
        }

        // 状态一行已删除：卡片右侧就是活动状态，弹层里不再重复。
        // 只有「这一轮没有活动」时才补一句说明，免得把上一轮的时间当成这一轮。
        if (sheet.previousRound) {
            item(key = "previous-round") {
                TipsCard(
                    tips = DashboardPreviousRoundHint,
                    outerPadding = PaddingValues(bottom = SheetCardBottomPadding),
                )
            }
        }

        if (sheet.times.isNotEmpty()) {
            item(key = "times") {
                when (sheet.timesLayout) {
                    // 概览卡：一行两张，标签在上、日期在下（开始时间 / 结束时间…）。
                    DashboardTimesLayout.Grid -> DashboardTimeGrid(sheet.times)
                    // 标准卡：一行一张，标题在上、摘要在下（双爆的全天/限时列表）。
                    DashboardTimesLayout.Full -> DashboardTimeList(sheet.times)
                }
            }
        }

        if (sheet.cards.isNotEmpty()) {
            item(key = "cards-title") {
                SheetSectionTitle(sheet.sectionTitle ?: "返场卡片名单")
            }
            items(items = sheet.cards, key = { "card/$it" }) { name ->
                DashboardCardRow(name)
            }
        }

        val tips = sheet.tips
        if (tips != null) {
            item(key = "tips") {
                TipsCard(
                    tips = tips,
                    outerPadding = PaddingValues(bottom = SheetCardBottomPadding),
                )
            }
        }

        if (sheet.images.isNotEmpty()) {
            item(key = "images-title") { SheetSectionTitle("米鼠的图") }
            item(key = "images") {
                DashboardImageCards(names = sheet.images, onOpenImage = onOpenImage)
            }
        }

        if (sheet.sessions.isNotEmpty()) {
            item(key = "sessions-title") { SheetSectionTitle("全部场次") }
            when (sheet.sessionLayout) {
                // 岛类活动：日期当标题、时段当摘要，一行两张卡。
                DashboardSessionLayout.Grid -> items(
                    items = sheet.sessions.chunked(2),
                    key = { row -> "session/${row.first().beginEpochSecond}" },
                ) { row ->
                    DashboardSessionRow(
                        sessions = row,
                        onLongClick = if (canAddToCalendar) {
                            { session -> addSessionToCalendar(env, toaster, sheet.title, session) }
                        } else {
                            null
                        },
                    )
                }
                // 抢红包：整行大卡，时间下面补一行红包信息，同 Java 版布局。
                DashboardSessionLayout.Full -> items(
                    items = sheet.sessions,
                    key = { "session/${it.beginEpochSecond}" },
                ) { session ->
                    DashboardSessionFullCard(
                        session = session,
                        onLongClick = if (canAddToCalendar) {
                            { addSessionToCalendar(env, toaster, sheet.title, session) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        val note = sheet.note
        // 数据层给「长按场次可添加到系统日历」写的固定说明：桌面端没这个动作，整句不显示。
        val noteApplies = note != null && (canAddToCalendar || !note.contains(CalendarNoteKey))
        if (note != null && noteApplies) {
            item(key = "note") { DashboardNote(note) }
        }

        if (sheet.links.isNotEmpty()) {
            item(key = "links") {
                // 多个入口（巅峰对决的赛季规则 / 赛季奖励）并排一行平分宽度；
                // 只有一个时（营地任务顺序表）自然铺满整行。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SheetButtonGap),
                    horizontalArrangement = Arrangement.spacedBy(SheetButtonGap),
                ) {
                    sheet.links.forEach { link ->
                        Button(
                            onClick = { onLink(link) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text(text = link.label, style = MiuixTheme.textStyles.button)
                        }
                    }
                }
            }
        }
    }
}

/** 弹层内的分节标题：与其它详情弹层的边距一致。 */
@Composable
private fun SheetSectionTitle(text: String) {
    SmallTitle(text = text, insideMargin = SheetSectionTitleMargin)
}

/**
 * 时间点概览区，样式参照防御卡目录的概览小卡（标题 + 数值两行）：
 * 标题是「开始时间 / 结束时间」这类标签，数值是具体日期，一行两张卡。
 *
 * 行内用 [IntrinsicSize.Min] 对齐高度，同一行的两张卡总是一样高；
 * 值为空或缺失时由数据层直接不传，不会出现空卡。
 */
@Composable
private fun DashboardTimeGrid(times: List<DashboardTimeLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        verticalArrangement = Arrangement.spacedBy(SheetGridGap),
    ) {
        times.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(SheetGridGap),
            ) {
                row.forEach { line ->
                    DashboardTimeCard(
                        line = line,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // 奇数个时间点时补一个空位，最后一张卡保持半宽，不会拉满一行。
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 一张时间概览卡：body2 标签 + title3 加粗数值，与防御卡目录的概览小卡同规格。 */
@Composable
private fun DashboardTimeCard(line: DashboardTimeLine, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        cornerRadius = SheetCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // 概览卡只有按压反馈，没有点击动作。
        onClick = {},
    ) {
        Column {
            Text(
                text = line.label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = line.value,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                // 日期较长时允许换行：同一行的两张卡会跟着一起变高。
            )
        }
    }
}

/**
 * 每行一张的「标题 + 摘要」时间卡（双爆的时段列表用）：
 * 标题是「全天双爆 / 限时双爆」，摘要是整段的「开始日期 至 结束日期，共 XX 天」。
 *
 * 普通行与其它内容卡同底色；[DashboardTimeLine.highlight] 的行换成「活动进行中」的绿底，
 * 文字配色对齐防御卡目录的版本卡——标题取卡片内容色（同版本号），
 * 摘要取它的 70% 透明度（同「当前版本」）。
 */
@Composable
private fun DashboardTimeList(times: List<DashboardTimeLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        verticalArrangement = Arrangement.spacedBy(SheetGridGap),
    ) {
        times.forEach { line -> DashboardTimeFullCard(line) }
    }
}

/** 一张整行时间卡：title3 加粗标题 + body2 摘要，标题在上、摘要在下。 */
@Composable
private fun DashboardTimeFullCard(line: DashboardTimeLine) {
    val colors = if (line.highlight) {
        CardDefaults.defaultColors(color = activeStatusCardBackgroundColor())
    } else {
        CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer)
    }
    val titleColor = if (line.highlight) colors.contentColor else MiuixTheme.colorScheme.onSurfaceContainer
    val summaryColor = if (line.highlight) {
        colors.contentColor.copy(alpha = TimeLineSummaryAlpha)
    } else {
        MiuixTheme.colorScheme.onSurfaceContainerVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = SheetCardCornerRadius,
        // 内容边距与 BasicComponent 对齐：四边 16dp。
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = colors,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // 时间卡只有按压反馈，没有点击动作。
        onClick = {},
    ) {
        Column {
            Text(
                text = line.label,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = line.value,
                style = MiuixTheme.textStyles.body2,
                color = summaryColor,
            )
        }
    }
}

/**
 * 名单里的一张防御卡：与防御卡目录页共用 [DefenseCardRow]，
 * 目录里查不到时退回一行同名文字卡，至少让名单完整可读。
 */
@Composable
private fun DashboardCardRow(name: String) {
    val card = remember(name) { CardCatalog.cardNamed(name) }
    if (card != null) {
        DefenseCardRow(
            card = card,
            modifier = Modifier.padding(bottom = SheetCardBottomPadding),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        )
    } else {
        SheetCard {
            Text(
                text = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ==================== 米鼠的图 ====================

/** 一张「米鼠的图」预览：静态元数据 + 本地文件与宽高比。 */
private data class DashboardImage(
    val entry: DataImageEntry,
    val file: File?,
    val ratio: Float,
) {
    /** 已下载的图才能进查看器；未下载的只剩卡片上的「未下载」占位。 */
    fun viewerItemOrNull(): DataImageViewerItem? = file?.let {
        DataImageViewerItem(file = it, title = entry.title, name = entry.name)
    }
}

/**
 * 「米鼠的图」预览卡：直接复用数据图合集页的 [DataImageCard]，
 * 标题、副标题、未下载占位与查看器预热都与那一页完全一致。
 *
 * 排布：单图整行铺满；多图按两列瀑布流铺开（按序号交替分列，
 * 卡片高度由各自的宽高比决定，自然形成高低错落），点开直接进数据图查看器。
 */
@Composable
private fun DashboardImageCards(
    names: List<String>,
    onOpenImage: (DataImageViewerState) -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val dark = isInDarkTheme()
    val revision by DataImageStore.revision.collectAsState()

    // 文件名来自静态目录：这里只补本地文件与宽高比，下载/删除后靠 revision 重算。
    val images by produceState(
        initialValue = emptyList<DashboardImage>(),
        names,
        dark,
        revision,
    ) {
        value = withContext(Dispatchers.IO) {
            names.mapNotNull { name ->
                val entry = AllDataImages.firstOrNull { it.name == name } ?: return@mapNotNull null
                val file = DataImageStore.resolveFile(env, name, dark)
                DashboardImage(
                    entry = entry,
                    file = file,
                    ratio = file?.let { DataImageThumbnails.ratioOf(it) } ?: FallbackAspectRatio,
                )
            }
        }
    }

    // 卡片预览区的窗口矩形与正在显示的缩略图，按文件名收集。用普通 Map 而不是状态：
    // 只有打开查看器的那一刻读一次并快照，滚动 / 布局过程中的写入不该触发重组。
    val boundsByName = remember { mutableMapOf<String, Rect>() }
    val bitmapByName = remember { mutableMapOf<String, ImageBitmap>() }

    fun open(name: String) {
        val viewable = images.mapNotNull { image ->
            image.viewerItemOrNull()?.let { image to it }
        }
        val index = viewable.indexOfFirst { it.first.entry.name == name }
        val file = viewable.getOrNull(index)?.first?.file
        if (index < 0 || file == null) {
            toaster.show("请先下载图片资源")
            return
        }
        onOpenImage(
            DataImageViewerState(
                items = viewable.map { it.second },
                startIndex = index,
                boundsByIndex = buildMap {
                    viewable.forEachIndexed { viewerIndex, (image, _) ->
                        boundsByName[image.entry.name]?.let { put(viewerIndex, it) }
                    }
                },
                ratios = viewable.associate { (image, _) -> image.entry.name to image.ratio },
                preview = bitmapByName[name],
                // 卡片稳定显示时预热好的整图低清图：命中则转场全程都是完整画面。
                fullPreview = DataImageThumbnails.peekViewerPreview(file),
            ),
        )
    }

    @Composable
    fun ImageCard(image: DashboardImage) {
        DataImageCard(
            entry = image.entry,
            file = image.file,
            ratio = image.ratio,
            fileRevision = revision,
            cornerRadius = SheetCardCornerRadius,
            // 弹层里其它内容卡都是 secondaryContainer：预览卡跟着用同一底色，
            // 否则默认的 surfaceContainer 在弹层背景上会是一块颜色发灰的卡。
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
            onPreviewBounds = { cardName, bounds ->
                if (bounds == null) boundsByName.remove(cardName) else boundsByName[cardName] = bounds
            },
            onPreviewBitmap = { cardName, bitmap ->
                if (bitmap == null) bitmapByName.remove(cardName) else bitmapByName[cardName] = bitmap
            },
            onClick = { open(image.entry.name) },
        )
    }

    if (images.size <= 1) {
        // 单图：一张卡铺满整行。
        val image = images.firstOrNull()
        if (image != null) {
            Column(modifier = Modifier.padding(bottom = SheetCardBottomPadding)) {
                ImageCard(image)
            }
        }
    } else {
        // 多图：两列瀑布流，按序号交替分列。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SheetCardBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(SheetGridGap),
        ) {
            listOf(0, 1).forEach { column ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SheetGridGap),
                ) {
                    images.filterIndexed { index, _ -> index % 2 == column }
                        .forEach { image -> ImageCard(image) }
                }
            }
        }
    }
}

// ==================== 场次 ====================

/** 两列场次卡的一行：按内容高度对齐，两张卡一样高。 */
@Composable
private fun DashboardSessionRow(
    sessions: List<DashboardSession>,
    onLongClick: ((DashboardSession) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(SheetGridGap),
    ) {
        sessions.forEach { session ->
            DashboardSessionCard(
                session = session,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                // 卡片的 onLongClick 非空：没有动作时给一个空实现（长按不出日历也不报错）。
                onLongClick = { onLongClick?.invoke(session) },
            )
        }
        // 本场活动的最后一场落单时补一个空位，卡片保持半宽。
        if (sessions.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}

/** 一场活动：日期当标题、时段当摘要，长按把它加进系统日历。 */
@Composable
private fun DashboardSessionCard(
    session: DashboardSession,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = SheetCardCornerRadius,
        // 日期卡的标题/摘要间距与 BasicComponent 对齐：四边 16dp。
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // 只有长按有动作：点击不做事，但仍保留按压反馈。
        onClick = {},
        onLongPress = onLongClick,
    ) {
        Column {
            Text(
                text = session.date,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = session.time,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

/**
 * 整行场次卡（抢红包）：时间一行、红包信息一行，同 Java 版
 * `item_activity_time_lucky_money.xml` 的排版。
 * 红包信息在手机上是两行，PAD 的场次卡够宽，合成一行读起来更顺。
 */
@Composable
private fun DashboardSessionFullCard(
    session: DashboardSession,
    onLongClick: (() -> Unit)?,
) {
    val isTablet = isTabletLayout()
    val description = session.description?.let { if (isTablet) it.replace('\n', '，') else it }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SheetCardBottomPadding),
        cornerRadius = SheetCardCornerRadius,
        // 日期卡的标题/摘要间距与 BasicComponent 对齐：四边 16dp。
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
        onLongPress = onLongClick,
    ) {
        Column {
            Text(
                text = "${session.date} ${session.time}",
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
    }
}

/** 场次列表下方的补充说明（例如长按可以加日程）。 */
@Composable
private fun DashboardNote(note: String) {
    Text(
        text = note,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = SheetCardBottomPadding),
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
    )
}

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
        cornerRadius = SheetCardCornerRadius,
        // 与其它弹层一致：内容卡用 secondaryContainer，与弹层背景区分开。
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
    ) {
        content()
    }
}

/**
 * 把一场活动预填进系统日历，由用户在日历里确认保存（不需要日历权限）。
 *
 * 任务标题与描述沿用 Java 版弹窗里的文案：抢红包单独一套，其余按卡片标题拼。
 * 桌面端走不到这里：[PlatformCapabilities.addToCalendar] 为 false，入口不会出现。
 */
private fun addSessionToCalendar(
    env: AppEnv,
    toaster: Toaster,
    sheetTitle: String,
    session: DashboardSession,
) {
    val isLuckyMoney = sheetTitle.contains(DashboardCardId.LuckyMoney.title)
    val title = if (isLuckyMoney) {
        "美食大战老鼠抢红包"
    } else {
        "美食大战老鼠${sheetTitle.substringBefore("（")}"
    }
    val description = if (isLuckyMoney) "这次你一定能抢到！！！" else "这次你一定比上次更能肝！！！"

    val handled = env.addToCalendar(
        title = title,
        description = description,
        beginMillis = session.beginEpochSecond * 1000L,
        endMillis = session.endEpochSecond * 1000L,
    )
    if (!handled) toaster.show("未找到日历应用")
}
