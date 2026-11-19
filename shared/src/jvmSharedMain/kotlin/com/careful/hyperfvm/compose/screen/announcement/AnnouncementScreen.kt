package com.careful.hyperfvm.compose.screen.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.dialog.UrlConfirmDialog
import com.careful.hyperfvm.compose.component.dialog.UrlTarget
import com.careful.hyperfvm.compose.component.dialog.openUrl
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewer
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerActionsSheet
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerItem
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerState
import com.careful.hyperfvm.compose.component.imageviewer.StaticSubSamplingImage
import com.careful.hyperfvm.compose.data.announcement.Announcement
import com.careful.hyperfvm.compose.data.announcement.AnnouncementBlock
import com.careful.hyperfvm.compose.data.announcement.AnnouncementEntry
import com.careful.hyperfvm.compose.data.announcement.AnnouncementException
import com.careful.hyperfvm.compose.data.announcement.AnnouncementImages
import com.careful.hyperfvm.compose.data.announcement.AnnouncementRepository
import com.careful.hyperfvm.compose.data.image.DataImageThumbnails
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.HardwareShortcutsSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image as ImageIcon
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/** 公告页的三种状态：加载中、已拿到、失败（带这条公告的网页地址兜底）。 */
private sealed interface AnnouncementUiState {
    data object Loading : AnnouncementUiState
    data class Ready(val announcement: Announcement) : AnnouncementUiState
    data class Failed(val message: String, val url: String?) : AnnouncementUiState
}

/** 正文左右留白：图片卡与文字段共用，与仪表盘的内容边距同量级。 */
private val ContentSidePadding = 12.dp

/**
 * 正文栏在宽屏上的最大宽度。
 *
 * 公告配图是官方按手机宽度做的长截图（原图约 1080px 宽），一块图可能有好几千 px 高。
 * 平板上如果铺满窗口，一张图就会变成横跨 1280dp、几千 dp 高的巨幅长条，一屏连一张
 * 都看不全；这里把正文收窄成居中的「文档栏」，600dp 在常见 1.8x 平板上正好是
 * 1080px，图片接近 1:1 不会发虚。手机上窗口本来就窄，不会触发。
 */
private val ContentMaxWidth = 600.dp

/** 图片之间的间距；配上圆角，整期公告看起来是一组分段长图，而不是一张贴满屏的长条。 */
private val ImageBlockGap = 8.dp

/** 图片卡的圆角，与数据图卡片的 16dp 对齐。 */
private val ImageBlockCornerRadius = 16.dp

/**
 * 小于这个宽度（像素）的图按原尺寸居中显示。
 *
 * 老公告里常混着 428×32 的分隔条、124×32 的小图标，按整栏宽度铺开会被放大成
 * 一整条色带；官方网页上它们也是原尺寸。截图类的大图不受影响，照旧铺满。
 */
private const val MiniImageMaxWidth = 600

/**
 * 公告 / B 站图文动态阅读页：在 App 内直接看哔哩哔哩的图文内容。
 *
 * 不带 [articleCvId] 与 [opusId] 时是「最新一期」：云端仓库的 m3u → opus 详情。
 * 带上 cv 号时是合集里翻到的那一期：先试 opus 接口，老专栏退回正文接口 / 专栏网页。
 * 带上 [opusId] 时是任意一条图文动态（巅峰对决的「赛季规则 / 赛季奖励」），
 * 只走 opus 详情接口；[title] 就是那页的顶栏标题。
 * 正文与图片都会在磁盘上缓存，再看一次不联网。
 *
 * 正文由 [AnnouncementRepository] 解析，整期的配图一进来就在后台
 * 逐张下载到缓存，列表里用 SubSamplingImage 区域解码，点开图片交给
 * [DataImageViewer]（与数据图合集同一个查看器）：可以翻页、双指放大；
 * 正文里长按配图不进查看器，只拉起保存 / 分享面板。
 *
 * 解析在转场期间就发出（IO 线程，不占主线程），但整页内容要等转场结束后的下一帧
 * 才渲染，转场期间只显示加载指示器——与查黑详情页同一套做法，避免正文重组
 * 与转场动画抢同一帧预算。
 *
 * 顶栏的入口不再是刷新：公告每次进页面都会自动确认有没有新版，刷新没有单独放的
 * 必要；这里换成「在浏览器中打开原帖」，一点直达，不再二次确认。
 *
 * 解析不了的情况（接口风控、格式不支持）在失败页给「在哔哩哔哩中打开」，
 * 与 Java 版行为一致。
 */
@Composable
fun AnnouncementScreen(
    onBack: () -> Unit,
    articleCvId: String? = null,
    opusId: String? = null,
    title: String = "更新公告",
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    var reloadToken by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<AnnouncementUiState>(AnnouncementUiState.Loading) }
    var pendingUrl by remember { mutableStateOf<UrlTarget?>(null) }
    var viewer by remember { mutableStateOf<DataImageViewerState?>(null) }

    // 正文在转场期间就开拉（IO 线程），但整页内容的渲染要等转场结束后的下一帧，
    // 避免列表重组与转场动画同帧抢帧。ready 一旦置 true 就不再回退：
    // 返回转场时详情内容不会突然消失。
    val transitionRunning = LocalNavTransitionScope.current.isRunning
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(transitionRunning) {
        if (transitionRunning || contentReady) return@LaunchedEffect
        // 多等一帧：转场收尾那一帧仍在播旧内容，同帧重组会抢帧。
        withFrameNanos { }
        contentReady = true
    }

    LaunchedEffect(reloadToken, articleCvId, opusId) {
        val refresh = reloadToken > 0
        // 只有「最新一期」有那层预填充：任意动态没有旧的同篇内容可以直接上屏。
        if (articleCvId == null && opusId == null) {
            // 同一期公告有内存缓存时先上屏，后台再去确认有没有新版，避免每次进页面都白屏。
            AnnouncementRepository.cached()?.let { if (!refresh) state = AnnouncementUiState.Ready(it) }
        }
        if (state !is AnnouncementUiState.Ready) state = AnnouncementUiState.Loading

        try {
            state = AnnouncementUiState.Ready(
                when {
                    opusId != null -> AnnouncementRepository.loadOpus(env, opusId, refresh)
                    articleCvId == null -> AnnouncementRepository.load(env, refresh)
                    else -> AnnouncementRepository.loadArticle(env, articleEntryOf(articleCvId), refresh)
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (state is AnnouncementUiState.Ready) {
                // 已有旧内容时刷新失败只提示一下，不把页面清空。
                toaster.show(e.message ?: "刷新失败")
            } else {
                state = AnnouncementUiState.Failed(
                    message = e.message ?: "公告加载失败。",
                    url = (e as? AnnouncementException)?.url,
                )
            }
        }
    }

    // 转场未结束前一律按加载中渲染：正文可能已经回来，但整页内容要等页面静止再上场。
    val visibleState = if (contentReady) state else AnnouncementUiState.Loading
    // 副标题和顶栏按钮也跟着 visibleState 走，否则转场期间它们会先刷出来再补内容。
    val announcement = (visibleState as? AnnouncementUiState.Ready)?.announcement

    // 查看器与页面内容做兄弟节点，才能盖住顶栏。
    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            title = title,
            // 不是「最新一期」时用发布时间当副标题，一眼能确认看的是哪一篇。
            subtitle = if (articleCvId == null && opusId == null) {
                ""
            } else {
                announcement?.publishedAt.orEmpty()
            },
            outerPadding = PaddingValues(0.dp),
            onBack = onBack,
            actions = {
                AppTopBarIconButton(
                    onClick = { announcement?.let { openUrl(env, toaster, it.url) } },
                    enabled = announcement != null,
                ) {
                    Icon(imageVector = MiuixIcons.Link, contentDescription = "在浏览器中打开")
                }
            },
        ) { padding ->
            when (val current = visibleState) {
                AnnouncementUiState.Loading -> AnnouncementLoading(padding)

                is AnnouncementUiState.Failed -> AnnouncementFailed(
                    padding = padding,
                    message = current.message,
                    url = current.url,
                    onRetry = { reloadToken += 1 },
                    onOpenBrowser = { url -> pendingUrl = announcementUrlTarget(url, title) },
                )

                is AnnouncementUiState.Ready -> AnnouncementContent(
                    padding = padding,
                    announcement = current.announcement,
                    onOpenViewer = { viewer = it },
                )
            }

            UrlConfirmDialog(target = pendingUrl, onDismiss = { pendingUrl = null })
        }

        val currentViewer = viewer
        if (currentViewer != null) {
            DataImageViewer(state = currentViewer, onDismiss = { viewer = null })
        }
    }
}

@Composable
private fun AnnouncementLoading(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InfiniteProgressIndicator()
        Text(
            text = "公告加载中",
            modifier = Modifier.padding(top = 12.dp),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

@Composable
private fun AnnouncementFailed(
    padding: PaddingValues,
    message: String,
    url: String?,
    onRetry: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .padding(horizontal = ContentSidePadding),
    ) {
        Spacer(Modifier.height(24.dp))
        TipsCard(tips = message, outerPadding = PaddingValues(0.dp))
        TextButton(
            text = "重新加载",
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
        if (url != null) {
            TextButton(
                text = "在哔哩哔哩中打开",
                onClick = { onOpenBrowser(url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AnnouncementContent(
    padding: PaddingValues,
    announcement: Announcement,
    onOpenViewer: (DataImageViewerState) -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current

    // 这一期的图片块。公告基本全是长图拼接，文字段很少，分开排版。
    val images = remember(announcement) {
        announcement.blocks.filterIsInstance<AnnouncementBlock.Image>()
    }
    val indexByUrl = remember(images) {
        images.withIndex().associate { (index, image) -> image.url to index }
    }
    // 查看器转场用：图片名（URL 里的文件哈希）→ 宽高比。接口给了原图尺寸，不用读文件头。
    val ratios = remember(images) {
        images.mapNotNull { image ->
            if (image.width > 0 && image.height > 0) {
                imageKey(image) to image.width.toFloat() / image.height
            } else {
                null
            }
        }.toMap()
    }

    // 整期配图一进来就全部下到本地：查看器要能翻到任意一张，不能指望用户先把列表滚一遍。
    // 三条并发够跑满一般带宽，也不会让十几张图同时挤进写盘与解码队列。
    val files = remember(announcement.id) { mutableStateMapOf<String, File>() }
    LaunchedEffect(announcement.id) {
        val dispatcher = Dispatchers.IO.limitedParallelism(3)
        coroutineScope {
            images.forEach { image ->
                launch(dispatcher) {
                    val file = AnnouncementImages.load(env, announcement.id, image.url) ?: return@launch
                    withContext(Dispatchers.Main.immediate) { files[image.url] = file }
                }
            }
        }
    }

    // 每张图在窗口里的矩形，按 URL 收集。用普通 Map 而不是状态：只有打开查看器的那一刻
    // 读一次并快照，滚动过程中的写入不应该触发重组。
    val boundsByUrl = remember(announcement.id) { mutableMapOf<String, Rect>() }

    // 长按配图弹出的保存 / 分享面板。正文里长按只对这张图做事，不进查看器。
    var actionsImage by remember(announcement.id) { mutableStateOf<AnnouncementBlock.Image?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    fun openViewer(index: Int) {
        // 查看器翻的是本地文件，整期的图到齐才放行；点得太早先提示一下。
        if (images.any { files[it.url] == null }) {
            toaster.show("公告配图还在加载，请稍候")
            return
        }
        val items = images.map { image -> viewerItemOf(image, files.getValue(image.url)) }
        val target = images[index]
        onOpenViewer(
            DataImageViewerState(
                items = items,
                startIndex = index,
                // 快照所有还在屏幕上的图片位置：翻页后退出，要飞回当前那张所在的位置。
                boundsByIndex = images.mapIndexedNotNull { i, image ->
                    boundsByUrl[image.url]?.let { i to it }
                }.toMap(),
                ratios = ratios,
                // 列表里没有单独维护缩略图位图；命中预热好的整图低清图就用它垫转场，
                // 没命中时查看器会自己补解一张，转场退化成渐显。
                fullPreview = DataImageThumbnails.peekViewerPreview(files.getValue(target.url)),
            ),
        )
    }

    fun showImageActions(image: AnnouncementBlock.Image) {
        // 保存 / 分享都直接读本地文件；还没落盘的先提示一下，与查看器同一口径。
        if (files[image.url] == null) {
            toaster.show("公告配图还在加载，请稍候")
            return
        }
        // 长按手势靠触觉确认已识别，与查看器里长按一致。
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        actionsImage = image
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 宽屏（平板 / 横屏手机）把内容栏收窄居中；手机上 maxWidth 不到上限，两侧不缩。
        val sideGutter = if (maxWidth > ContentMaxWidth) (maxWidth - ContentMaxWidth) / 2 else 0.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // 整列仍铺满屏幕：两侧空白处的手势也能滚列表，只是内容被收进中间那一栏。
            contentPadding = PaddingValues(
                start = sideGutter,
                end = sideGutter,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "header") { AnnouncementHeader(announcement) }
            itemsIndexed(
                items = announcement.blocks,
                key = { index, _ -> "block-$index" },
            ) { _, block ->
                when (block) {
                    is AnnouncementBlock.Image -> AnnouncementImageBlock(
                        image = block,
                        file = files[block.url],
                        onBounds = { rect ->
                            if (rect == null) {
                                boundsByUrl.remove(block.url)
                            } else {
                                boundsByUrl[block.url] = rect
                            }
                        },
                        onClick = { openViewer(indexByUrl.getValue(block.url)) },
                        onLongClick = { showImageActions(block) },
                    )

                    is AnnouncementBlock.Text -> AnnouncementTextBlock(block)

                    is AnnouncementBlock.Heading -> AnnouncementHeadingBlock(block)
                }
            }
        }
    }

    // 正文里长按配图只弹保存 / 分享面板，不进查看器；面板与查看器共用同一个弹层，
    // 保存 / 分享的样式与提示完全一致。
    val actionsTarget = actionsImage
    DataImageViewerActionsSheet(
        item = actionsTarget?.let { image ->
            files[image.url]?.let { file -> viewerItemOf(image, file) }
        },
        show = actionsTarget != null,
        onDismiss = { actionsImage = null },
    )
}

@Composable
private fun AnnouncementHeader(announcement: Announcement) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        if (announcement.title.isNotBlank()) {
            Text(
                text = announcement.title,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = MiuixTheme.textStyles.title2.fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
        val meta = listOf(announcement.author, announcement.publishedAt).filter { it.isNotBlank() }
        if (meta.isNotEmpty()) {
            Text(
                text = meta.joinToString(" · "),
                modifier = Modifier.padding(top = 6.dp),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
            )
        }
    }
}

@Composable
private fun AnnouncementTextBlock(block: AnnouncementBlock.Text) {
    // 接口用富文本样式标出了部分加粗（老公告的小标题都这么写），在原文上直接叠样式，
    // 不再把一段话拆成多个 Text。
    val content = remember(block) {
        if (block.spans.isEmpty()) {
            AnnotatedString(block.content)
        } else {
            buildAnnotatedString {
                append(block.content)
                block.spans.forEach { span ->
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), span.start, span.end)
                }
            }
        }
    }
    Text(
        text = content,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = MiuixTheme.textStyles.body1.fontSize,
    )
}

/** 小节标题：级别越低字号越大，老公告的「活动一」「维护内容」靠它分层次。 */
@Composable
private fun AnnouncementHeadingBlock(block: AnnouncementBlock.Heading) {
    val fontSize = when (block.level) {
        1 -> MiuixTheme.textStyles.title3.fontSize
        2 -> MiuixTheme.textStyles.headline1.fontSize
        else -> MiuixTheme.textStyles.headline2.fontSize
    }
    Text(
        text = block.content,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
    )
}

/**
 * 一张公告配图：圆角卡片 + 按压水波纹，点开进查看器；长按只拉起保存 / 分享面板，
 * 不跟着跳进查看器——那是「对这张图做事」，不是「看大图」。
 *
 * 官方公告是一张长图被接口切成十几段，这里每段单独成卡、留间距，滚起来是一组
 * 分段长图而不是一整条。长宽比取自接口返回的原图尺寸，图片没下完也能先占好位，
 * 列表不会跳版；没下载完时显示占位图标。
 *
 * 宽度不是一律铺满：老公告里夹着 428×32 的分隔条、124×32 的小图标，铺满整栏
 * 会被放大成一整条色带，这类小图按原像素尺寸居中显示。
 */
@Composable
private fun AnnouncementImageBlock(
    image: AnnouncementBlock.Image,
    file: File?,
    onBounds: (Rect?) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val ratio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height
    } else {
        // 接口极少不给尺寸，给个竖图的常规比例兜底。
        3f / 4f
    }

    // 滚出屏幕被回收时撤掉矩形，避免查看器拿到过期的位置。
    DisposableEffect(image.url) {
        onDispose { onBounds(null) }
    }

    // 小图按原像素换算成 dp：接口给的是真实像素，密度一除才是它在界面上应有的尺寸。
    val naturalWidth = with(LocalDensity.current) { image.width.toDp() }
    val keepOriginalSize = image.width in 1 until MiniImageMaxWidth

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentSidePadding)
            .padding(bottom = ImageBlockGap),
        contentAlignment = Alignment.Center,
    ) {
        AppCard(
            modifier = Modifier
                .then(if (keepOriginalSize) Modifier.widthIn(max = naturalWidth) else Modifier)
                .fillMaxWidth()
                // 不能用 boundsInWindow：它会把矩形裁到窗口内，卡片有一部分滚出屏幕时
                // top 会被压成 0、高度变矮，查看器的起落点跟着算错。
                .onGloballyPositioned { onBounds(Rect(it.positionInWindow(), it.size.toSize())) },
            cornerRadius = ImageBlockCornerRadius,
            // 图片要贴到卡片边缘，内边距由外层负责。
            insideMargin = PaddingValues(0.dp),
            showIndication = true,
            onClick = onClick,
            onLongPress = onLongClick,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .background(MiuixTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val loaded = file
                if (loaded != null) {
                    val zoomableState = rememberZoomableState(
                        // 纯展示图不参与键盘/鼠标快捷键，省掉每个列表项一次 focusable。
                        hardwareShortcutsSpec = HardwareShortcutsSpec.Disabled,
                    )
                    StaticSubSamplingImage(
                        path = loaded.absolutePath,
                        zoomableState = zoomableState,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // 稳定一会儿后给查看器预热一张「整图低清图」：转场时窗口从卡片矩形扩到
                    // 全屏，有它垫底才不会有半截空白。延迟避开快速滚动，扫过去的卡片不白解码。
                    LaunchedEffect(loaded) {
                        delay(250)
                        DataImageThumbnails.loadViewerPreview(loaded)
                    }
                } else {
                    Icon(
                        imageVector = MiuixIcons.ImageIcon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }
        }
    }
}

/** 图片在 URL 里的文件名（去掉 @720w 之类的规格后缀），查看器用它当宽高比与转场快照的键。 */
private fun imageKey(image: AnnouncementBlock.Image): String =
    image.url.substringBefore('@').substringAfterLast('/').ifEmpty { image.url }

/** 公告配图在查看器 / 保存分享面板里的条目：标题固定「公告配图」，文件名取 URL 里的哈希。 */
private fun viewerItemOf(image: AnnouncementBlock.Image, file: File): DataImageViewerItem =
    DataImageViewerItem(file = file, title = "公告配图", name = imageKey(image))

/** 外链确认框的目标：标题太长，副标题用页面顶栏的短标题。 */
private fun announcementUrlTarget(url: String, subtitle: String): UrlTarget = UrlTarget(
    title = "哔哩哔哩",
    subtitle = subtitle,
    url = url,
)

/**
 * 详情页需要的合集条目：opusId 取得到就先走 opus 接口；取不到（进程回收后
 * 合集缓存没了）也不要紧，空 opusId 会让仓库直接走正文接口 / 专栏网页。
 */
private fun articleEntryOf(cvId: String): AnnouncementEntry =
    AnnouncementRepository.cachedArchive()
        ?.entries
        ?.firstOrNull { it.cvId == cvId }
        ?: AnnouncementEntry(
            cvId = cvId,
            opusId = "",
            title = "",
            publishedAt = 0L,
            summary = "",
            coverUrl = "",
        )
