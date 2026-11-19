package com.careful.hyperfvm.compose.screen.dataCenter.dataImages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewer
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerActionsSheet
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerItem
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerState
import com.careful.hyperfvm.compose.data.image.AllDataImages
import com.careful.hyperfvm.compose.data.image.DataImageCategories
import com.careful.hyperfvm.compose.data.image.DataImageDownloadController
import com.careful.hyperfvm.compose.data.image.DataImageDownloadState
import com.careful.hyperfvm.compose.data.image.DataImageRemoteInfo
import com.careful.hyperfvm.compose.data.image.DataImageStore
import com.careful.hyperfvm.compose.data.image.DataImageThumbnails
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.rememberNotificationPermissionRequester
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.ui.isTabletLayout
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppIconDropdownMenu
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ListView
import com.careful.hyperfvm.compose.ui.AppDialog
import com.careful.hyperfvm.compose.ui.AppDialogButton
import com.careful.hyperfvm.compose.ui.AppDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.io.File

/** 瀑布流列间距，同时也是卡片之间与页面左右两侧的间距。 */
private val GridSpacing = 12.dp

/** 没读到真实宽高比时用的占位比例（宽/高），比多数数据图偏方一点。 */
private const val FallbackAspectRatio = 0.5f

/** 一次待确认的下载任务。 */
private data class PendingDownload(val list: List<DataImageRemoteInfo>)

/**
 * 数据图合集页的入口：页面内容之上叠查看器覆盖层。
 *
 * 查看器做成页面内的覆盖层而不是独立 Activity，是为了让转场（从卡片矩形
 * 缩放到全屏）和返回处理都待在同一棵组合树里；跨体系的 View/Compose 边界
 * 正是 mojito 时代反复出问题的地方。
 */
@Composable
fun DataImagesScreen(onBack: () -> Unit) {
    // 查看器与页面内容做兄弟节点，才能盖住顶栏与页面弹层。
    var viewer by remember { mutableStateOf<DataImageViewerState?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        DataImagesPage(
            onBack = onBack,
            onOpenViewer = { viewer = it },
        )
        val state = viewer
        if (state != null) {
            DataImageViewer(state = state, onDismiss = { viewer = null })
        }
    }
}

/**
 * 页面本体：顶部是「检查更新」与「删除所有图片」两张卡，下面是全部数据图的瀑布流。
 *
 * 与 Java 版的差别在于不再按分类分 Activity、按图片分 ArrowPreference，而是把六份目录
 * 拍平成一屏瀑布流；点开图片拉起 [DataImageViewer]。
 *
 * 列数：手机固定双列；PAD 横屏四列、竖屏双列。
 */
@Composable
private fun DataImagesPage(
    onBack: () -> Unit,
    onOpenViewer: (DataImageViewerState) -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val dark = isInDarkTheme()
    val revision by DataImageStore.revision.collectAsState()
    val download by DataImageDownloadController.state.collectAsState()

    // 完成态的通知是 promoted ongoing（用户滑不掉），由本页替用户撤下——但只撤「进页面时结果已经存在」的那种：
    // 那说明用户早就知道结果，或者是点通知回来的。
    // 不能跟着 download.phase 走：下载往往就在本页跑，结果一变就把刚发的通知撤了。
    // 「更新完成」的提醒连一帧都看不到。留在通知栏里，等用户下次进来或点通知时再撤。
    val finishedOnEntry = remember {
        download.phase == DataImageDownloadState.Phase.Success ||
            download.phase == DataImageDownloadState.Phase.Failed
    }
    LaunchedEffect(Unit) {
        if (finishedOnEntry) {
            // 桌面端没有通知栏，这一步是空实现。
            env.dismissBackgroundNotification()
        }
    }

    val windowSize = LocalWindowInfo.current.containerSize
    val isTablet = isTabletLayout()
    val columns = if (isTablet && windowSize.width > windowSize.height) 4 else 2

    // ==================== 目录菜单 ====================

    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val gridState = rememberLazyStaggeredGridState()
    // 自己持有顶栏滚动状态：目录跳转是程序化滚动，要手动把折叠状态同步过去。
    val scrollBehavior = MiuixScrollBehavior()
    // 目录跳转的目标条目下标；-1 表示没有待处理任务（落点还要减去顶栏高度，只能在 content 里算）。
    var jumpTarget by remember { mutableIntStateOf(-1) }
    // 目录菜单：条目与分类标题同源，天然一一对应；没有选中态，只是跳转入口。
    val indexMenu = remember {
        listOf(
            DropdownEntry(
                items = DataImageCategories.mapIndexed { index, category ->
                    DropdownItem(
                        text = category.title,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            // animateScrollToItem 是程序化滚动，不派发 nested scroll。
                            // 顶栏不会自己收起；先把折叠状态同步到目标位置，再交给下方滚动。
                            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
                            jumpTarget = DataImageCategoryItemIndices[index]
                        },
                    )
                },
            ),
        )
    }

    // ==================== 云端清单 ====================

    var remote by remember { mutableStateOf<List<DataImageRemoteInfo>?>(null) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(true) }
    // 检查失败后重试、或手动重新检查时自增，驱动下面的 LaunchedEffect 重跑。
    var checkToken by remember { mutableIntStateOf(0) }

    // ==================== 下拉检查更新 ====================

    // 下拉只负责「重新拉一遍云端清单」：自增 checkToken 让上面的 LaunchedEffect 重跑。
    // 更新卡与「需要更新」的计数全都复用同一套判定，不另写一条检查路径。
    var pullRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    // 同一份回调喂两处：Android 端管触摸下拉，桌面端管顶栏上的刷新按钮。
    val requestRefresh: () -> Unit = {
        pullRefreshing = true
        // 同步置位：等 LaunchedEffect 里再置的话，中间那一帧卡片会先闪回旧结论。
        checking = true
        // 上一次下载的「更新完成」是结果态，优先级高于检查态；用户主动
        // 重新检查时先清掉，卡片才会回到「正在检查更新」。
        DataImageDownloadController.clearResult()
        checkToken += 1
    }
    // 检查落定（成功或失败）就收起指示器；失败原因会显示在更新卡上。
    LaunchedEffect(checking) {
        if (!checking && pullRefreshing) pullRefreshing = false
    }

    // 与 Java 版一致：进页面就拉一次清单，成功后才能算出「有多少张图需要更新」。
    LaunchedEffect(checkToken) {
        checking = true
        remoteError = null
        val result = withContext(Dispatchers.IO) { runCatching { DataImageStore.fetchRemoteList() } }
        result
            .onSuccess {
                remote = it
                remoteError = null
            }
            .onFailure {
                remote = null
                remoteError = it.message ?: "内容获取失败，请检查网络后重试"
            }
        checking = false
    }

    // ==================== 本地文件 ====================

    // 深浅色切换与下载/删除都会改文件集合，这两件事任一变化都要重新扫一遍目录。
    val localFiles by produceState(
        initialValue = emptyMap<String, File?>(),
        dark,
        revision,
    ) {
        value = withContext(Dispatchers.IO) {
            AllDataImages.associate { it.name to DataImageStore.resolveFile(env, it.name, dark) }
        }
    }

    // 缩略图缓存按文件路径与修改时间做 key，图片被覆盖后旧条目会残留，整批清掉最省心。
    LaunchedEffect(revision) { DataImageThumbnails.clear() }

    // 瀑布流要按真实宽高比算预览高度；只读文件头，近百张图批量跑也很快。
    // 每算好十几张就发布一次：下载完成后这里会重算，如果攒到最后一次性提交，
    // 那段时间里刚下载的图查不到比例，查看器转场只能退化成淡入。
    // revision 也要当键：同名文件覆盖后 localFiles 的内容相等、不会通知，只有它。
    // 叫醒重算；比例缓存的键带修改时间，重算自然拿到新值。
    val ratios by produceState(
        initialValue = emptyMap<String, Float>(),
        localFiles,
        revision,
    ) {
        withContext(Dispatchers.IO) {
            val accumulator = mutableMapOf<String, Float>()
            var pending = 0
            localFiles.forEach { (name, file) ->
                if (file != null) {
                    val ratio = DataImageThumbnails.ratioOf(file) ?: return@forEach
                    accumulator[name] = ratio
                    if (++pending >= 8) {
                        pending = 0
                        withContext(Dispatchers.Main.immediate) { value = accumulator.toMap() }
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) { value = accumulator.toMap() }
        }
    }

    val localVersion = remember(revision) { DataImageStore.localVersion(env) }

    // ==================== 查看器数据 ====================

    // 查看器只能翻「已下载」的图，所以这里先按瀑布流顺序过滤出一份独立的列表。
    // 点击卡片时再换算成它在其中的下标（未下载的卡片根本不会调起查看器）。
    val viewerEntries = remember(localFiles) {
        buildList {
            DataImageGridItems.forEach { item ->
                if (item is DataImageGridItem.Image) {
                    localFiles[item.entry.name]?.let { file ->
                        add(
                            item.entry.name to DataImageViewerItem(
                                file = file,
                                title = item.entry.title,
                                name = item.entry.name,
                            ),
                        )
                    }
                }
            }
        }
    }
    val viewerIndexByName = remember(viewerEntries) {
        viewerEntries.withIndex().associate { (index, entry) -> entry.first to index }
    }
    val viewerItems = remember(viewerEntries) { viewerEntries.map { it.second } }

    // 卡片预览区的窗口矩形，按文件名收集。用普通 Map 而不是状态：只有打开查看器的那一刻
    // 读一次并快照，滚动过程中的写入不应该触发任何重组。
    val previewBoundsByName = remember { mutableMapOf<String, Rect>() }
    // 卡片正在显示的缩略图，同样按文件名收集。查看器用它当转场首帧内容：
    // 打开时用户眼前的卡片一定有这张位图（没有就还是在解码占位状态）。
    val previewBitmapByName = remember { mutableMapOf<String, ImageBitmap>() }

    // ==================== 弹窗 ====================

    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // 长按预览卡弹出的保存 / 分享面板：只对这张图做事，不进查看器。
    var actionsItem by remember { mutableStateOf<DataImageViewerItem?>(null) }

    // 通知权限只影响进度能否在通知栏展示，拒绝了也照常下载，所以回调里不做事。
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    fun startDownload(list: List<DataImageRemoteInfo>) {
        if (list.isEmpty()) return
        // 桌面端是空实现，不会弹任何东西。
        requestNotificationPermission()
        // 版本号取云端全量清单的最大值：只有这一批全部成功，服务才会把它落库。
        val newestVersion = remote?.let { DataImageStore.newestVersion(it) } ?: 0L
        DataImageDownloadController.start(env, list, newestVersion)
    }

    /** 单击更新卡：只下「云端有更新或本地缺失」的那部分。 */
    fun requestIncrementalDownload() {
        val list = remote
        if (list == null) {
            checkToken += 1
            return
        }
        scope.launch {
            val plan = withContext(Dispatchers.IO) {
                DataImageStore.buildDownloadPlan(env, list, incremental = true)
            }
            if (plan.isEmpty()) {
                toaster.show("图片已经是最新版")
            } else {
                pendingDownload = PendingDownload(plan)
            }
        }
    }

    /** 长按更新卡：整份清单重下。 */
    fun requestFullDownload() {
        val list = remote
        if (list == null) {
            checkToken += 1
        } else {
            pendingDownload = PendingDownload(list)
        }
    }

    // ==================== 更新卡状态 ====================

    val downloadCard = when (download.phase) {
        DataImageDownloadState.Phase.Downloading -> DownloadCard.Downloading(
            completed = download.completed,
            total = download.total,
            percent = download.percent,
        )
        DataImageDownloadState.Phase.Paused -> DownloadCard.Paused(
            completed = download.completed,
            total = download.total,
            percent = download.percent,
        )
        DataImageDownloadState.Phase.Success,
        DataImageDownloadState.Phase.Failed,
        -> DownloadCard.Finished(
            success = download.total - download.failedCount,
            failed = download.failedCount,
        )

        DataImageDownloadState.Phase.Idle -> when {
            checking -> DownloadCard.Checking
            remoteError != null -> DownloadCard.Error(remoteError.orEmpty())
            remote == null -> DownloadCard.Checking
            // 0 = 从没下过；1 = 用户主动删空过。两种都要走全量下载。
            localVersion == DataImageStore.VersionNeverDownloaded -> DownloadCard.Missing(deleted = false)
            localVersion == DataImageStore.VersionDeleted -> DownloadCard.Missing(deleted = true)
            else -> {
                val updateCount = remote.orEmpty().count { it.version > localVersion }
                if (updateCount > 0) DownloadCard.Update(updateCount) else DownloadCard.UpToDate
            }
        }
    }

    val onDownloadCardClick: () -> Unit = when (downloadCard) {
        is DownloadCard.Missing, is DownloadCard.Update -> ({ requestIncrementalDownload() })
        is DownloadCard.Error -> ({ checkToken += 1 })
        is DownloadCard.Paused -> ({ DataImageDownloadController.resume() })
        is DownloadCard.Finished -> if (downloadCard.failed > 0) {
            // 只重试失败的那几张：从当前云端清单里按文件名捞回完整条目。
            ({
                val plan = remote.orEmpty().filter { it.fileName in download.failedNames }
                if (plan.isEmpty()) checkToken += 1 else pendingDownload = PendingDownload(plan)
            })
        } else {
            {}
        }

        else -> ({})
    }

    val onDownloadCardLongClick: (() -> Unit)? = when (downloadCard) {
        is DownloadCard.Update, is DownloadCard.UpToDate, is DownloadCard.Finished -> ({ requestFullDownload() })
        is DownloadCard.Error -> ({ checkToken += 1 })
        else -> null
    }

    ScreenScaffold(
        title = "数据图合集",
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        // 顶栏的滚动连接这次不交给脚手架，改由下面的 PullToRefresh 统一编排。
        // 两者都挂会在滚动时把同一个 behavior 调用两次。
        attachScrollBehavior = false,
        // 桌面端没有下拉手势，刷新入口挂在顶栏右侧的按钮上。
        onRefresh = requestRefresh,
        refreshing = pullRefreshing,
        actions = {
            // 目录按钮：菜单以按钮为锚点从右上角弹出。
            AppIconDropdownMenu(entries = indexMenu) {
                Icon(imageVector = MiuixIcons.ListView, contentDescription = "目录")
            }
        },
    ) { padding ->
        // 目录跳转的落点要减去顶栏高度：直接把目标条目滚到 y=0 会被顶栏盖住。
        LaunchedEffect(jumpTarget) {
            val target = jumpTarget
            if (target < 0) return@LaunchedEffect
            val topInset = with(density) { padding.calculateTopPadding().roundToPx() }
            gridState.animateScrollToItem(target, -topInset)
            jumpTarget = -1
        }

        // 下拉检查更新：手势与指示器交给 PullToRefresh，触发后只自增 checkToken。
        // 由上面的 LaunchedEffect 重新拉云端清单——与点更新卡重试走同一条路。
        PullToRefresh(
            isRefreshing = pullRefreshing,
            onRefresh = requestRefresh,
            pullToRefreshState = pullToRefreshState,
            // 把顶栏交给 PullToRefresh 编排：下拉时它先展开大标题，顶栏完全展开后
            // 剩余的下拉量才驱动刷新。只在脚手架挂连接的话，它会在 post 阶段
            // 先把下拉位移全部吃掉，顶栏收不到，折叠后就没法靠下拉回到大标题。
            topAppBarScrollBehavior = scrollBehavior,
            // 指示器要避开折叠顶栏：内容顶部内边距正好就是顶栏高度。
            contentPadding = PaddingValues(top = padding.calculateTopPadding()),
            // 默认文案与颜色是英文/固定灰，换成中文并跟随主题。
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            refreshTexts = listOf(
                "下拉检查更新",
                "松开立即检查",
                "正在检查更新",
                "检查完成",
            ),
        ) {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(columns),
                // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，顶栏折叠才会响应。
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GridSpacing,
                    end = GridSpacing,
                    top = padding.calculateTopPadding() + GridSpacing,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
                verticalItemSpacing = GridSpacing,
                horizontalArrangement = Arrangement.spacedBy(GridSpacing),
            ) {
                item(key = "download", span = StaggeredGridItemSpan.FullLine) {
                    DataImageDownloadCard(
                        card = downloadCard,
                        onClick = onDownloadCardClick,
                        onLongClick = onDownloadCardLongClick,
                    )
                }
                item(key = "delete", span = StaggeredGridItemSpan.FullLine) {
                    DataImageDeleteCard(
                        enabled = !download.isActive,
                        onLongClick = { showDeleteConfirm = true },
                    )
                }
                DataImageGridItems.forEach { item ->
                    when (item) {
                        is DataImageGridItem.CategoryHeader -> item(
                            key = "category/${item.category.id}",
                            span = StaggeredGridItemSpan.FullLine,
                        ) {
                            DataImageCategoryHeader(category = item.category)
                        }

                        is DataImageGridItem.GroupHeader -> item(
                            key = "group/${item.title}",
                            span = StaggeredGridItemSpan.FullLine,
                        ) {
                            DataImageGroupHeader(title = item.title)
                        }

                        is DataImageGridItem.Image -> {
                            val name = item.entry.name
                            val file = localFiles[name]
                            item(key = "image/${item.categoryId}/$name") {
                                DataImageCard(
                                    entry = item.entry,
                                    file = file,
                                    ratio = ratios[name] ?: FallbackAspectRatio,
                                    fileRevision = revision,
                                    onPreviewBounds = { cardName, bounds ->
                                        if (bounds == null) {
                                            previewBoundsByName.remove(cardName)
                                        } else {
                                            previewBoundsByName[cardName] = bounds
                                        }
                                    },
                                    onPreviewBitmap = { cardName, bitmap ->
                                        if (bitmap == null) {
                                            previewBitmapByName.remove(cardName)
                                        } else {
                                            previewBitmapByName[cardName] = bitmap
                                        }
                                    },
                                    onClick = { bounds ->
                                        if (file == null) {
                                            toaster.show("请先下载图片资源")
                                        } else {
                                            // 查看器只能翻「已下载」的图，下标要换算到过滤后的列表里。
                                            val index = viewerIndexByName[name]
                                            if (index != null) {
                                                // 快照所有可见卡片的位置：翻页后退出时要飞回当前那张的位置。
                                                val boundsByIndex = buildMap {
                                                    previewBoundsByName.forEach { (cardName, rect) ->
                                                        viewerIndexByName[cardName]?.let { put(it, rect) }
                                                    }
                                                    if (bounds != null) put(index, bounds)
                                                }
                                                // 比例表缺这一张时（刚下载完、还没算到）补上卡片当前
                                                // 使用的比例，转场至少能跑，不会直接退化成淡入。
                                                val viewerRatios = if (ratios.containsKey(name)) {
                                                    ratios
                                                } else {
                                                    ratios + (name to FallbackAspectRatio)
                                                }
                                                onOpenViewer(
                                                    DataImageViewerState(
                                                        items = viewerItems,
                                                        startIndex = index,
                                                        boundsByIndex = boundsByIndex,
                                                        ratios = viewerRatios,
                                                        preview = previewBitmapByName[name],
                                                        // 卡片稳定显示时已预热好的整图低清图：命中则转。
                                                        // 全程都是完整画面（包括卡片外裁掉的部分）。
                                                        fullPreview = DataImageThumbnails
                                                            .peekViewerPreview(file),
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        // 保存 / 分享面板直接用查看器那份条目：标题、文件名与查看器完全一致。
                                        val index = viewerIndexByName[name]
                                        if (index == null) {
                                            toaster.show("请先下载图片资源")
                                        } else {
                                            // 长按手势靠触觉确认已识别，与公告正文里的配图一致。
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            actionsItem = viewerItems[index]
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 弹窗必须放在 Scaffold 的 content 里：miuix 的弹层宿主由 Scaffold 提供。
        ConfirmDialog(
            show = pendingDownload != null,
            content = "将开始下载图片资源，请注意网络流量消耗。",
            confirmText = "开始下载",
            onDismiss = { pendingDownload = null },
            onConfirm = {
                val list = pendingDownload?.list
                if (list != null) {
                    pendingDownload = null
                    startDownload(list)
                }
            },
        )
        ConfirmDialog(
            show = showDeleteConfirm,
            content = "将删除所有本地图片。删除后，您需要重新下载才能查看。",
            confirmText = "开始删除",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                scope.launch {
                    withContext(Dispatchers.IO) { DataImageStore.deleteAll(env) }
                    // 结果态清掉，卡片才会回到「点击本卡片获取图片资源」。
                    DataImageDownloadController.clearResult()
                }
            },
        )

        // 长按预览卡只弹保存 / 分享面板，不进查看器：与公告正文里的配图共用同一个弹层。
        DataImageViewerActionsSheet(
            item = actionsItem,
            show = actionsItem != null,
            onDismiss = { actionsItem = null },
        )
    }
}

// ==================== 顶部两张卡 ====================

/**
 * 检查更新卡：标题与描述随「拉取状态 / 本地版本 / 是否有更新 / 下载进度」变化，
 * 文案与 Java 版 `refreshUpdateInfoCard` 一一对应。
 */
@Composable
private fun DataImageDownloadCard(
    card: DownloadCard,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val title: String
    val description: String
    var progress: Float? = null
    when (card) {
        DownloadCard.Checking -> {
            title = "正在检查更新⏳⏳⏳"
            description = "别着急，很快就好"
        }

        is DownloadCard.Error -> {
            title = "出现问题❌❌❌"
            description = card.message
        }

        is DownloadCard.Missing -> {
            title = "点击本卡片获取图片资源📣📣📣"
            description = if (card.deleted) {
                "删除图片后需要重新下载才能查看哦"
            } else {
                "第一次使用需要先下载图片才能查看哦"
            }
        }

        is DownloadCard.Update -> {
            title = "${card.count}张图片需要更新📣📣📣"
            description = "单击本卡片仅下载有更新的图片，长按本卡片下载全部图片(耗时更长，仅在需要时使用)"
        }

        DownloadCard.UpToDate -> {
            title = "已经是最新版本😋😋😋"
            description = "如有需要，可长按本卡片重新下载图片"
        }

        is DownloadCard.Downloading -> {
            title = "正在下载，已完成(${card.completed}/${card.total})⏳⏳⏳"
            description = "下载过程中请注意不要杀死App"
            progress = card.percent / 100f
        }

        is DownloadCard.Paused -> {
            title = "下载已暂停(${card.completed}/${card.total})"
            description = "点击本卡片继续下载"
            progress = card.percent / 100f
        }

        is DownloadCard.Finished -> {
            title = "更新完成🎉🎉🎉"
            description = if (card.failed == 0) {
                "${card.success}张图片更新成功"
            } else {
                "${card.success}张图片更新成功，${card.failed}张图片更新失败\n点击本卡片可重新下载更新失败的图片"
            }
        }
    }

    // 进度条要作为 bottomAction 进闭包，先落成 val 才能通过类型检查。
    val progressValue = progress

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
        onLongPress = onLongClick,
    ) {
        // 排版整个交给 BasicComponent：标题/摘要的字号、配色与最小高度都与其它页面一致。
        BasicComponent(
            title = title,
            summary = description,
            bottomAction = if (progressValue != null) {
                { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = progressValue) }
            } else {
                null
            },
        )
    }
}

/** 删除卡：点击不做事，只响应长按；下载中禁用长按但保留按压反馈。 */
@Composable
private fun DataImageDeleteCard(
    enabled: Boolean,
    onLongClick: (() -> Unit)?,
) {
    // miuix 的 Card 只要有任一回调就挂 combinedClickable，按下才有 Sink 反馈与波纹。
    // 可用时长按就是那个回调，点击仍是空操作；下载中长按被禁用，改用一个空的
    // onClick 撑起手势——反馈保留，但点下去不会有任何动作。
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = if (enabled) null else ({}),
        onLongPress = if (enabled) onLongClick else null,
    ) {
        BasicComponent(
            title = "删除所有图片",
            summary = "长按本卡片可执行删除操作\n如果您遇到图片无法查看的问题，可尝试先删除所有图片再重新下载",
        )
    }
}

// ==================== 二次确认弹窗 ====================

/**
 * 「二次确认」弹窗。
 *
 * 对应 Java 版 `DialogBuilderManager.showDialogWithCallBack` 的那个布局，
 * 但去掉了标题上方的 emoji 插图，只保留标题、正文和两个按钮。
 */
@Composable
private fun ConfirmDialog(
    show: Boolean,
    content: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        show = show,
        title = "二次确认",
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = content,
                modifier = Modifier.fillMaxWidth(),
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                fontSize = MiuixTheme.textStyles.body1.fontSize,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppDialogButton(
                    text = "咱手滑了",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                AppDialogButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    role = AppDialogButtonRole.Primary,
                )
            }
        }
    }
}

// ==================== 状态与工具 ====================

/** 更新卡要展示的状态，由云端清单、本地版本与下载进度共同决定。 */
private sealed interface DownloadCard {
    data object Checking : DownloadCard

    data class Error(val message: String) : DownloadCard

    /** 一张图都没有：[deleted] 区分「第一次使用」与「主动删除过」。 */
    data class Missing(val deleted: Boolean) : DownloadCard

    data class Update(val count: Int) : DownloadCard

    data object UpToDate : DownloadCard

    data class Downloading(val completed: Int, val total: Int, val percent: Int) : DownloadCard

    data class Paused(val completed: Int, val total: Int, val percent: Int) : DownloadCard

    data class Finished(val success: Int, val failed: Int) : DownloadCard
}
