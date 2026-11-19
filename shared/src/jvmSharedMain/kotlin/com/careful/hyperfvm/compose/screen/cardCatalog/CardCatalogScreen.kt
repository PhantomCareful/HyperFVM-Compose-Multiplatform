package com.careful.hyperfvm.compose.screen.cardCatalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.careful.hyperfvm.compose.component.card.DefenseCardRow
import com.careful.hyperfvm.compose.data.cardCatalog.CardCatalog
import com.careful.hyperfvm.compose.data.cardCatalog.DefenseCard
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.ui.AppSearchCancelAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索框上下留白；与 InputField 最小高度一起构成顶栏 bottomContent 的高度。 */
private val SearchFieldVerticalPadding = 6.dp

/** 结果列表的左右内边距，状态小标题、计数行与卡片共用。 */
private val ResultSidePadding = 12.dp

/** 列表内文字的左侧缩进：叠在列表 12dp 上后与卡片内文字（12 + 16dp）对齐。 */
private val SectionHeaderIndent = 16.dp

/**
 * 「防御卡」Tab 的目录页，对应 Java 版 `CardDataIndexActivity`。
 *
 * 顶部的版本 / 收录数量概览区由 [CatalogOverview] 提供；详情页还没重制，
 * 所以卡片点击回调留空，只保留按压反馈。
 *
 * 布局要点：
 * - 搜索框作为顶栏的 `bottomContent` 挂在标题下方，属于顶栏的一部分——顶栏的
 *   模糊层把搜索框一起盖住，列表穿过搜索框时背后是模糊的。
 * - 搜索交互与动画照搬 miuix 官方 `SearchBar`：取消按钮用
 *   `expandHorizontally + slideInHorizontally` 从右侧滑出、结果区用
 *   `AnimatedVisibility` 从搜索框下方展开。
 * - 右侧索引条是悬浮层：顶部钉在「大标题展开时」的顶栏底部（取观察到的
 *   最大顶栏高度），滚动收起大标题后不再跟着下降；越界回弹时由 overlay 保证不动。
 */
@Composable
internal fun CardCatalogScreen(outerPadding: PaddingValues) {
    val catalogListState = rememberLazyListState()
    val resultListState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val keyword = query.trim()
    val results = remember(keyword) {
        if (keyword.isEmpty()) emptyList() else CardCatalog.search(keyword)
    }

    val sections = CardCatalog.sections
    val labels = remember(sections) { sections.map { it.label } }
    // 每个分节的第一张卡在 LazyColumn 里的下标（分节标题已去掉，只剩卡片；
    // 下标 0 是概览行，所以卡片从 1 开始）。
    val sectionItemIndices = remember(sections) {
        var index = 1
        sections.map { section -> index.also { index += section.cards.size } }
    }
    var jumpJob by remember { mutableStateOf<Job?>(null) }

    fun closeSearch() {
        searchExpanded = false
        query = ""
        focusManager.clearFocus()
    }

    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = searchExpanded,
        onBackCompleted = { closeSearch() },
    )

    // 进入搜索态时把大标题平滑收起，给结果区腾出高度。
    LaunchedEffect(searchExpanded) {
        if (!searchExpanded) return@LaunchedEffect
        val limit = scrollBehavior.state.heightOffsetLimit
        if (!limit.isFinite() || limit >= 0f) return@LaunchedEffect
        animate(scrollBehavior.state.heightOffset, limit) { value, _ ->
            scrollBehavior.state.heightOffset = value
        }
    }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    // 索引条钉在大标题展开时的顶栏底部。顶栏高度只在滚动时变矮，
    // 所以观察到的最大值就是展开态高度；0 表示还没测到，先用当前值。
    val indexTop = remember { mutableStateOf(0.dp) }

    ScreenScaffold(
        title = "防御卡全能数据库",
        outerPadding = outerPadding,
        scrollBehavior = scrollBehavior,
        bottomContent = {
            CatalogSearchBar(
                query = query,
                expanded = searchExpanded,
                onQueryChange = { query = it },
                onSearch = { focusManager.clearFocus() },
                onExpandedChange = { searchExpanded = it },
                onCancel = { closeSearch() },
            )
        },
        overlay = { padding ->
            // 大标题展开时顶栏最高，把观察到的最大高度钉住，后续滚动收起不再变化。
            val barBottom = padding.calculateTopPadding()
            SideEffect {
                if (barBottom > indexTop.value) indexTop.value = barBottom
            }
            // 索引条放在 overlay：不进 overScrollVertical 的位移层，
            // 越界回弹时不会跟着内容被拖动。
            AnimatedVisibility(
                visible = !searchExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CardCatalogLetterIndex(
                        labels = labels,
                        onLabelPicked = { index ->
                            // animateScrollToItem 是程序化滚动，不派发 nested scroll，
                            // 顶栏不会自己收起；先把折叠状态同步到目标位置再滚。
                            scrollBehavior.state.heightOffset =
                                scrollBehavior.state.heightOffsetLimit
                            jumpJob?.cancel()
                            jumpJob = scope.launch {
                                catalogListState.animateScrollToItem(sectionItemIndices[index])
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(
                                // 钉在展开态顶栏底部：顶栏有多高都不遮它，收起后也不下降。
                                top = if (indexTop.value > 0.dp) indexTop.value else barBottom,
                                bottom = padding.calculateBottomPadding(),
                            )
                            .padding(end = 2.dp),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !searchExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn(
                    state = catalogListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        // 右侧给索引条让位：条宽 16dp，卡片不能钻到它底下。
                        end = 28.dp,
                        // 顶栏（含搜索框）的高度由脚手架给出；列表自身铺满整屏，
                        // 滚动时内容会从搜索框和顶栏底下穿过（顶栏模糊依赖这一点）。
                        // 额外 8dp 让第一张卡与搜索框拉开一点距离。
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "catalog-overview") {
                        CatalogOverview()
                    }
                    sections.forEach { section ->
                        items(
                            items = section.cards,
                            key = { "card-${it.primaryName}" },
                        ) { card ->
                            DefenseCardRow(card)
                        }
                    }
                }
            }

            // 结果区只用淡入淡出：AnimatedVisibility 默认的 expandIn 以左上角为原点缩放，
            // 顶部那行状态小字会像从左上角“飞”进来，观感很怪。
            AnimatedVisibility(
                visible = searchExpanded,
                enter = fadeIn(tween(durationMillis = 200)),
                exit = fadeOut(tween(durationMillis = 120)),
            ) {
                SearchResultList(
                    keyword = keyword,
                    results = results,
                    listState = resultListState,
                    // 结果列表同样铺满内容区，靠 top padding 让出顶栏，
                    // 否则展开时首屏卡片会钻到搜索框和顶栏背后。
                    topPadding = padding.calculateTopPadding() + 2.dp,
                    bottomPadding = maxOf(padding.calculateBottomPadding(), imeBottomPadding),
                )
            }
        }
    }
}

/**
 * 顶栏里的搜索框：输入框 + 展开时从右侧滑出的取消动作。
 *
 * 结构与 miuix 官方 `SearchBar` 的首行一致：输入框两侧 12dp 内边距，
 * 取消按钮紧跟其后（间距即这 12dp），滑出 / 滑回使用相同的过渡参数。
 * 轻透磨砂下取消是文字，柔光玻璃下是玻璃圆钮上的 ×。
 */
@Composable
private fun CatalogSearchBar(
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
                label = "搜索防御卡名称",
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

/**
 * 结果区的内容：顶部一行状态小字，或者「计数 + 卡片列表」。
 *
 * 文案与列表都固化在状态实例里：淡化期间旧内容仍用旧数据渲染，
 * 不会被已经变化的 keyword / results 改写（否则会出现「匹配到 0 张卡」
 * 「没有找到与（）匹配的卡」这类一闪而过的中间态）。
 */
private sealed interface SearchResultState {
    /** 顶部一行小字。 */
    data class Hint(val text: String) : SearchResultState

    /** 有结果的卡片列表。 */
    data class Result(val cards: List<DefenseCard>) : SearchResultState
}

/**
 * 搜索结果列表：空态与结果之间淡入淡出，结果项自身带出现动画
 * （对齐 miuix SearchBar demo 的内容展开效果）。
 */
@Composable
private fun SearchResultList(
    keyword: String,
    results: List<DefenseCard>,
    listState: LazyListState,
    topPadding: Dp,
    bottomPadding: Dp,
) {
    val state: SearchResultState = when {
        keyword.isEmpty() -> SearchResultState.Hint("输入卡名或拼音开始搜索")
        results.isEmpty() -> SearchResultState.Hint("没有找到与「$keyword」匹配的卡")
        else -> SearchResultState.Result(results)
    }
    AnimatedContent(
        targetState = state,
        // 只有「一行小字 ⇄ 卡片列表」之间才做交叉淡化；同类之间直接更新，
        // 列表增删继续交给 animateItem，不会整块闪一下。
        contentKey = { it is SearchResultState.Result },
        transitionSpec = {
            fadeIn(tween(durationMillis = 180)) togetherWith fadeOut(tween(durationMillis = 180))
        },
        label = "searchResultState",
    ) { target ->
        when (target) {
            is SearchResultState.Hint -> SearchResultHint(
                text = target.text,
                topPadding = topPadding,
            )

            is SearchResultState.Result -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ResultSidePadding,
                    end = ResultSidePadding,
                    top = topPadding,
                    bottom = bottomPadding + ResultSidePadding,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "result-count") {
                    Text(
                        text = "匹配到 ${target.cards.size} 张卡",
                        modifier = Modifier.padding(start = SectionHeaderIndent, top = 2.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                items(
                    items = target.cards,
                    key = { "result-${it.primaryName}" },
                ) { card ->
                    DefenseCardRow(
                        card = card,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/**
 * 结果区顶部的一行小字：位置与「匹配到 N 张卡」对齐（左缩进 28dp、顶部 2dp），
 * 样式同样用 footnote1 + onBackgroundVariant。
 */
@Composable
private fun SearchResultHint(text: String, topPadding: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = ResultSidePadding + SectionHeaderIndent,
                end = ResultSidePadding,
                top = topPadding + 2.dp,
            ),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}
