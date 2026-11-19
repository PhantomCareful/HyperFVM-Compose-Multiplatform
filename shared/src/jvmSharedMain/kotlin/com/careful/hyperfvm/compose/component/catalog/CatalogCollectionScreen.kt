package com.careful.hyperfvm.compose.component.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.careful.hyperfvm.compose.data.catalog.CatalogImages
import com.careful.hyperfvm.compose.data.catalog.CatalogItem
import com.careful.hyperfvm.compose.data.catalog.CatalogKind
import com.careful.hyperfvm.compose.data.catalog.CatalogLoadState
import com.careful.hyperfvm.compose.data.catalog.CatalogRepository
import com.careful.hyperfvm.compose.data.catalog.CatalogSex
import com.careful.hyperfvm.compose.data.catalog.CatalogUpdateResult
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.isTabletLayout
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppIconDropdownMenu
import com.careful.hyperfvm.compose.ui.AppSearchCancelAction
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.GlassFloatingActionButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Update
import com.careful.hyperfvm.compose.ui.AppDialog
import com.careful.hyperfvm.compose.ui.AppDialogButton
import com.careful.hyperfvm.compose.ui.AppDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 瀑布流列间距，同时也是卡片之间与页面左右两侧的间距。 */
private val GridSpacing = 12.dp

/** 搜索框上下留白，与查黑系统、防御卡目录同款。 */
private val SearchFieldVerticalPadding = 6.dp

/** 预览图的宽高比（142×184），卡片按它给图片留位，整页高度才会齐整。 */
private const val PreviewAspect = 142f / 184f

/** 预览卡圆角，比页面里的信息卡小一号：卡片本身只有半屏宽。 */
private val PreviewCardCornerRadius = 16.dp

/** 更新弹窗所处的阶段。 */
private enum class UpdatePhase { Confirm, Updating, Done, Failed }

/**
 * 更新弹窗的一次状态快照。
 *
 * @param maleAdded 更新完成后男款新增条数（[UpdatePhase.Done] 时有效）
 * @param femaleAdded 更新完成后女款新增条数（[UpdatePhase.Done] 时有效）
 * @param message 失败原因（[UpdatePhase.Failed] 时有效）
 */
private data class UpdateDialogState(
    val phase: UpdatePhase,
    val maleAdded: Int = 0,
    val femaleAdded: Int = 0,
    val message: String = "",
)

/** 顶栏筛选菜单里的一项。只有男女两档加「全部」，直接写死。 */
private data class SexFilterOption(
    val label: String,
    val sex: CatalogSex?,
)

private val SexFilterOptions = listOf(
    SexFilterOption("全部", null),
    SexFilterOption("男", CatalogSex.Male),
    SexFilterOption("女", CatalogSex.Female),
)

/**
 * 合集页（服装合集与翅膀合集共用）：顶栏右上角筛选男女，顶栏下方是搜索框，
 * 下面是全部条目的瀑布流，右下角是回到顶部的悬浮按钮。
 *
 * [kind] 决定数据来源与文案（服装/翅膀）；条目与预览图都来自云端，第一次进入时
 * 自动下载并落盘，期间显示进度提示，断网时给错误与重试入口，不会闪退。
 *
 * 顶栏的更新按钮会重新拉取条目文件，完成后弹窗报告男女款各新增多少。
 *
 * 列数：手机固定双列；PAD 横屏八列、竖屏四列。预览图是统一的小图（142×184），
 * 列数按横竖屏拉开，才能让一屏里看到足够多的条目。
 *
 * 点任意一张卡拉起 [CatalogDetailSheet]；滚过一屏半后浮出回到顶部的 FAB，
 * 点击后列表按弹簧动画滚回顶部，而不是瞬移。
 */
@Composable
fun CatalogCollectionScreen(
    kind: CatalogKind,
    onBack: () -> Unit,
) {
    val env = LocalAppEnv.current
    val loadState by CatalogRepository.state(kind).collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollBehavior = MiuixScrollBehavior()
    val gridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    // 性别筛选存选项下标（0 = 全部）：枚举不方便跨平台存进 Bundle，下标最稳。
    var sexFilterIndex by rememberSaveable { mutableStateOf(0) }
    val sexFilter = SexFilterOptions[sexFilterIndex].sex
    val keyword = query.trim()

    val items = (loadState as? CatalogLoadState.Ready)?.items.orEmpty()
    val filtered = remember(keyword, sexFilter, items) {
        items.filter { item ->
            (sexFilter == null || item.sex == sexFilter) &&
                (keyword.isEmpty() || item.name.contains(keyword, ignoreCase = true))
        }
    }

    // 详情弹层的两段状态分开存：收起动画期间条目仍需有值，内容会先被清空。
    var detailItem by remember { mutableStateOf<CatalogItem?>(null) }
    var showDetail by remember { mutableStateOf(false) }

    // 更新弹窗：null 即关闭；Updating 阶段不允许通过点遮罩关闭。
    var updateDialog by remember { mutableStateOf<UpdateDialogState?>(null) }

    // 第一次进入本页时开始加载：本地资源齐全就直接就绪，否则自动开始首次下载。
    LaunchedEffect(kind) { CatalogRepository.ensureLoaded(env, kind) }

    fun closeSearch() {
        searchExpanded = false
        query = ""
        focusManager.clearFocus()
    }

    fun startUpdate() {
        updateDialog = UpdateDialogState(UpdatePhase.Updating)
        scope.launch {
            updateDialog = when (val result = CatalogRepository.update(env, kind)) {
                is CatalogUpdateResult.Done -> UpdateDialogState(
                    phase = UpdatePhase.Done,
                    maleAdded = result.maleAdded,
                    femaleAdded = result.femaleAdded,
                )
                is CatalogUpdateResult.Failed -> UpdateDialogState(
                    phase = UpdatePhase.Failed,
                    message = result.message,
                )
            }
        }
    }

    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = searchExpanded,
        onBackCompleted = { closeSearch() },
    )

    // 进入搜索态时把大标题平滑收起，给瀑布流腾出高度（与查黑系统一致）。
    LaunchedEffect(searchExpanded) {
        if (!searchExpanded) return@LaunchedEffect
        val limit = scrollBehavior.state.heightOffsetLimit
        if (!limit.isFinite() || limit >= 0f) return@LaunchedEffect
        animate(scrollBehavior.state.heightOffset, limit) { value, _ ->
            scrollBehavior.state.heightOffset = value
        }
    }

    val windowSize = LocalWindowInfo.current.containerSize
    val isTablet = isTabletLayout()
    val columns = when {
        !isTablet -> 2
        windowSize.width > windowSize.height -> 8
        else -> 4
    }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // 滚过两行才浮出回到顶部按钮；列数越多一行越长，阈值跟着列数走（双列约一屏半）。
    val showFab by remember(columns) {
        derivedStateOf { gridState.firstVisibleItemIndex >= columns * 2 }
    }

    // 筛选菜单每次按当前选中态重建：勾选标记跟着 sexFilterIndex 走。
    val filterMenu = remember(sexFilterIndex, items, kind) {
        listOf(
            DropdownEntry(
                items = SexFilterOptions.mapIndexed { index, option ->
                    DropdownItem(
                        text = option.label,
                        summary = when (option.sex) {
                            null -> "共 ${items.size} ${kind.unit}"
                            else -> "共 ${items.count { it.sex == option.sex }} ${kind.unit}"
                        },
                        selected = sexFilterIndex == index,
                        onClick = { sexFilterIndex = index },
                    )
                },
            ),
        )
    }

    ScreenScaffold(
        title = "${kind.noun}合集",
        subtitle = when (val state = loadState) {
            is CatalogLoadState.Ready -> "共 ${filtered.size} ${kind.unit}"
            else -> ""
        },
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        // 本页没有下拉刷新，滚动连接照旧由脚手架统一挂。
        actions = {
            AppTopBarIconButton(
                onClick = { updateDialog = UpdateDialogState(UpdatePhase.Confirm) },
                // 首次加载进行中不让点，避免和自动加载抢同一把锁；加载失败时允许，
                // 更新也能当「重试」用。
                enabled = loadState !is CatalogLoadState.Loading,
            ) {
                Icon(imageVector = MiuixIcons.Update, contentDescription = "更新${kind.noun}资源")
            }
            if (loadState is CatalogLoadState.Ready) {
                AppIconDropdownMenu(entries = filterMenu) {
                    Icon(imageVector = MiuixIcons.Filter, contentDescription = "筛选性别")
                }
            }
        },
        bottomContent = if (loadState is CatalogLoadState.Ready) {
            {
                CatalogSearchBar(
                    noun = kind.noun,
                    query = query,
                    expanded = searchExpanded,
                    onQueryChange = { query = it },
                    onSearch = { focusManager.clearFocus() },
                    onExpandedChange = { searchExpanded = it },
                    onCancel = { closeSearch() },
                )
            }
        } else {
            null
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab && !searchExpanded && loadState is CatalogLoadState.Ready,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                val onFabClick: () -> Unit = {
                    // animateScrollToItem 是弹簧动画，回顶过程有过渡而不是瞬移。
                    scope.launch { gridState.animateScrollToItem(0) }
                }
                if (LocalAppSettings.current.isGlassStyle) {
                    // 柔光玻璃：OS4 的玻璃圆钮；backdrop 为 null（模糊关或设备不支持）时它自己用主色填充。
                    GlassFloatingActionButton(
                        onClick = onFabClick,
                        backdrop = LocalBlurBackdrop.current,
                    ) {
                        BackToTopIcon()
                    }
                } else {
                    FloatingActionButton(onClick = onFabClick) {
                        BackToTopIcon()
                    }
                }
            }
        },
        overlay = { _ ->
            CatalogDetailSheet(
                item = detailItem,
                show = showDetail,
                onDismissRequest = { showDetail = false },
                noun = kind.noun,
            )
            CatalogUpdateDialog(
                state = updateDialog,
                kind = kind,
                onConfirm = { startUpdate() },
                onDismiss = { updateDialog = null },
            )
        },
    ) { padding ->
        when (val state = loadState) {
            is CatalogLoadState.Loading -> CatalogFirstLoadView(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            is CatalogLoadState.Error -> CatalogLoadErrorView(
                message = state.message,
                onRetry = { scope.launch { CatalogRepository.ensureLoaded(env, kind) } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            is CatalogLoadState.Ready -> LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(columns),
                // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，顶栏折叠才会响应。
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GridSpacing,
                    end = GridSpacing,
                    top = padding.calculateTopPadding() + GridSpacing,
                    bottom = maxOf(padding.calculateBottomPadding(), imeBottomPadding) + 32.dp,
                ),
                verticalItemSpacing = GridSpacing,
                horizontalArrangement = Arrangement.spacedBy(GridSpacing),
            ) {
                items(
                    items = filtered,
                    // key 用数据里的稳定字符串 id：Android 上 Lazy 列表的 key 必须能
                    // 存进 Bundle，用对象或资源当 key 会直接崩溃。
                    key = { item -> item.id },
                ) { item ->
                    CatalogCard(
                        item = item,
                        onClick = {
                            // 先收起键盘：弹层与转场都不该带着输入法一起动。
                            focusManager.clearFocus()
                            detailItem = item
                            showDetail = true
                        },
                    )
                }
                if (filtered.isEmpty()) {
                    item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = if (keyword.isEmpty()) "没有符合筛选条件的${kind.noun}" else "没有找到匹配的${kind.noun}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 首次加载视图：环形进度 + 「第一次使用需要先加载一次资源哦，很快就好」。
 *
 * 还在下条目文件（约 5 MB）时没有可报的进度，用无限动画；进入图片下载阶段后
 * 换成带百分比的确定进度。
 */
@Composable
private fun CatalogFirstLoadView(
    state: CatalogLoadState.Loading,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.total > 0) {
            CircularProgressIndicator(
                progress = state.done / state.total.toFloat(),
                modifier = Modifier.width(40.dp).height(40.dp),
            )
        } else {
            CircularProgressIndicator(
                progress = null,
                modifier = Modifier.width(40.dp).height(40.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "首次使用需要加载资源，很快就好",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = MiuixTheme.textStyles.body1.fontSize,
            textAlign = TextAlign.Center,
        )
        if (state.total > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "正在下载预览图 ${state.done} / ${state.total}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 加载失败视图：断网时给错误与重试入口，而不是闪退或白屏。 */
@Composable
private fun CatalogLoadErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = MiuixTheme.textStyles.body1.fontSize,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            text = "重新加载",
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}

/**
 * 更新弹窗：确认 → 更新中（进度） → 完成（新增数量） / 失败（原因）。
 *
 * [state] 为 null 即关闭；各阶段的标题按需求固定为「确认更新 / 正在更新 /
 * 更新完成」。更新中不允许点遮罩关闭——更新在下发协程里跑，关掉弹窗会让过程
 * 无反馈。
 */
@Composable
private fun CatalogUpdateDialog(
    state: UpdateDialogState?,
    kind: CatalogKind,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 退出动画期间调用方已把 state 置空，但弹窗容器还会滑出约 260ms：
    // 记住最后一次的状态继续渲染，内容才会随容器一起退出。
    var lastState by remember { mutableStateOf<UpdateDialogState?>(null) }
    if (state != null) lastState = state
    val content = state ?: lastState

    AppDialog(
        show = state != null,
        title = when (content?.phase) {
            UpdatePhase.Confirm -> "确认更新"
            UpdatePhase.Updating -> "正在更新"
            UpdatePhase.Done -> "更新完成"
            UpdatePhase.Failed -> "更新失败"
            null -> ""
        },
        onDismissRequest = {
            // 更新中不允许关闭；其余阶段点遮罩等同「稍后再说」。
            if (content?.phase != UpdatePhase.Updating) onDismiss()
        },
    ) {
        if (content == null) return@AppDialog
        Column(modifier = Modifier.fillMaxWidth()) {
            when (content.phase) {
                UpdatePhase.Confirm -> {
                    Text(
                        text = "将开始更新${kind.noun}资源",
                        modifier = Modifier.fillMaxWidth(),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppDialogButton(
                            text = "取消",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(20.dp))
                        AppDialogButton(
                            text = "确定",
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            role = AppDialogButtonRole.Primary,
                        )
                    }
                }

                UpdatePhase.Updating -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
                    }
                    Text(
                        text = "正在更新${kind.noun}资源，请稍候",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                }

                UpdatePhase.Done -> {
                    Text(
                        text = "${kind.maleLabel}新增${content.maleAdded}${kind.unit}，" +
                            "${kind.femaleLabel}新增${content.femaleAdded}${kind.unit}",
                        modifier = Modifier.fillMaxWidth(),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                    AppDialogButton(
                        text = "我知道了",
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        role = AppDialogButtonRole.Primary,
                    )
                }

                UpdatePhase.Failed -> {
                    Text(
                        text = content.message,
                        modifier = Modifier.fillMaxWidth(),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                    AppDialogButton(
                        text = "我知道了",
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        role = AppDialogButtonRole.Primary,
                    )
                }
            }
        }
    }
}

/**
 * 一张预览卡：上面是预览图，下面是名字与「性别 · 体力」一行摘要。
 *
 * 预览图按 [PreviewAspect] 固定占位：预览图尺寸统一，卡片高度也就一致，
 * 瀑布流在手机上读起来就是规整的网格。图片来自本地缓存（首次进入时已下载），
 * 罕见缺图时先显示空白占位，下次更新会补。
 */
@Composable
private fun CatalogCard(item: CatalogItem, onClick: () -> Unit) {
    val env = LocalAppEnv.current
    val imageFile = remember(item.id) { CatalogRepository.imageFile(env, item.id) }
    // 先同步取缓存：滚回屏幕的卡片不会闪一下占位再出图。
    val bitmap by produceState<ImageBitmap?>(
        initialValue = CatalogImages.peek(imageFile),
        imageFile,
    ) {
        if (value == null) value = CatalogImages.load(imageFile)
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = PreviewCardCornerRadius,
        // 图片要贴到卡片边缘，内边距由内容自己加。
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PreviewAspect)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val image = bitmap
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
                Text(
                    text = item.name,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = MiuixTheme.textStyles.body1.fontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.sex.label} · 体力+${item.hp}",
                    modifier = Modifier.padding(top = 2.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 顶栏里的搜索框：输入框 + 展开时从右侧滑出的取消动作，与查黑系统、防御卡目录同款。
 * 轻透磨砂下取消是文字，柔光玻璃下是玻璃圆钮上的 ×。
 */
@Composable
private fun CatalogSearchBar(
    noun: String,
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = SearchBarDefaults.InsideMargin.width,
                    vertical = SearchFieldVerticalPadding,
                ),
        ) {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch() },
                expanded = expanded,
                // 点击输入框获得焦点时由 InputField 回调 true，搜索态随之展开。
                onExpandedChange = onExpandedChange,
                label = "搜索${noun}名称",
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
            exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
        ) {
            AppSearchCancelAction(
                onCancel = onCancel,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}

/** 回到顶部的箭头：Back 是左箭头，rotate 正角度为顺时针，转 90° 后箭头朝上。 */
@Composable
private fun BackToTopIcon() {
    Icon(
        imageVector = MiuixIcons.Back,
        contentDescription = "回到顶部",
        modifier = Modifier.rotate(90f),
        tint = Color.White,
    )
}