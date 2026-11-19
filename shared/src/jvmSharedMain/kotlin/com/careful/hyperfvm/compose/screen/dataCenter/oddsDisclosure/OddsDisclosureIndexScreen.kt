package com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import com.careful.hyperfvm.compose.data.oddsDisclosure.LotteryCategory
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsCategory
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDataRepository
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsErrataTips
import com.careful.hyperfvm.compose.data.oddsDisclosure.TurntableOddsRepository
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.screen.ThemeStyleFadeMillis
import com.careful.hyperfvm.compose.screen.about.AboutContentCard
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppIconDropdownMenu
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** 抽奖页一次都没拉成功时的失败文案：网络与解析分开说，方便判断是断网还是页面改版。 */
private const val LotteryNetworkErrorTips = "网络异常，没能加载幸运转转的概率公示，请检查网络后重试。"
private const val LotteryParseErrorTips = "公示页的结构变了，暂时无法解析幸运转转的概率，稍后再试试。"

/** 已有上次结果时后台刷新失败的轻提示；刷新成功不打扰。 */
private const val LotteryRefreshFailedTips = "概率刷新失败，当前为上次解析的结果"

/**
 * 抽奖页的数据：动态解析的幸运转转在前，手工维护的数据表（欢乐扭蛋、诸神宝殿、
 * 塔罗寻宝、法老宝藏、许愿池）在后。
 *
 * 两页都用这套下标（类别 + 抽奖），所以列表与详情页必须共用同一个函数，不能各拼一份。
 */
internal fun lotteryCategories(): List<LotteryCategory> =
    TurntableOddsRepository.cached + OddsDataRepository.lotteryCategories

/** TabRow 在顶栏 bottomContent 里的上下留白，与食神谱目录页的取值一致。 */
private val TabRowVerticalPadding = 6.dp

/** 两个子页的标签，顺序与 [HorizontalPager] 的页序一致。 */
private val DisclosureTabTitles = listOf("宝箱概率", "抽奖概率")

/** 宝箱页在 Pager 里的下标：目录菜单只在这一页显示。 */
private const val BoxDisclosurePageIndex = 0

/**
 * 目录菜单跳转用的 LazyColumn 下标。
 *
 * 每个类别标题前面固定有 1 个条目（勘误提醒），之后每个类别占
 * 「1 个标题 + 宝箱数」条。分类表来自数据包，进页时按当前数据算一次。
 */
private fun categoryItemIndices(categories: List<OddsCategory>): List<Int> {
    var index = 1
    return categories.map { category ->
        index.also { index += 1 + category.boxes.size }
    }
}

/**
 * 概率公示合集目录页，对应 Java 版 `OddsDisclosureIndexActivity`：
 * 顶栏下方用 TabRow 分成「宝箱概率」「抽奖概率」两页，页面间也可以左右滑动
 * （[HorizontalPager]）。
 *
 * 宝箱页 = 勘误提醒 + 按类别分节的宝箱列表，右上角目录按钮可跳转任意分节；
 * 抽奖页 = 每次进页动态解析官方「幸运转转」公示（[TurntableOddsRepository]），
 * 按类别分节展示，详见 [LotteryDisclosurePage]。
 *
 * 两页右上角都有链接按钮，点开「官方公示合集」弹层（[OddsSourcesSheet]）查看数据的
 * 官方来源；页面里的勘误卡都只是文字，不再挂外链。
 *
 * 分段选择器挂在 `ScreenScaffold` 的 `bottomContent` 槽位——与食神谱目录页一样，
 * 属于顶栏的一部分：跟着大标题一起折叠，并处于顶栏的模糊区域内。
 */
@Composable
fun OddsDisclosureIndexScreen(onBack: () -> Unit) {
    val navigator = LocalNavigator.current
    val settings = LocalAppSettings.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { DisclosureTabTitles.size })
    // 两页各存各的滚动位置：切 Tab 再切回来仍停在原处。
    val listStates = remember { List(DisclosureTabTitles.size) { LazyListState() } }
    // 宝箱分类表来自数据包，进页时取一次；目录菜单的下标按它现算。
    val boxCategories = OddsDataRepository.boxCategories
    val categoryIndices = remember(boxCategories) { categoryItemIndices(boxCategories) }
    // 宝箱页的列表状态单独取出来：顶栏目录菜单要按它跳转。
    val boxListState = listStates[BoxDisclosurePageIndex]
    // 自己持有顶栏滚动状态，目录跳转时才能手动同步折叠。
    val scrollBehavior = MiuixScrollBehavior()

    // 来源弹层提到列表外面，列表项回收时弹层不会被一起销毁。
    var showSources by remember { mutableStateOf(false) }
    // 目录菜单：条目与分节标题同源，天然一一对应；没有选中态，只是跳转入口。
    val indexMenu = remember(boxCategories, categoryIndices) {
        listOf(
            DropdownEntry(
                items = boxCategories.mapIndexed { index, category ->
                    DropdownItem(
                        text = category.title,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            // animateScrollToItem 是程序化滚动，不派发 nested scroll，
                            // 顶栏不会自己收起；先把折叠状态同步到目标位置，再滚。
                            // 列表位置按内容坐标计算，顶栏高度变化不影响落点。
                            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
                            scope.launch { boxListState.animateScrollToItem(categoryIndices[index]) }
                        },
                    )
                },
            ),
        )
    }

    ScreenScaffold(
        title = "概率公示合集",
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        actions = {
            // 官方公示来源对两页都适用，放在最前面；目录菜单紧随其后，留在最右。
            AppTopBarIconButton(onClick = { showSources = true }) {
                Icon(imageVector = MiuixIcons.Link, contentDescription = "官方公示合集")
            }
            // 目录菜单只服务于宝箱页：抽奖页的分节随活动轮换（幸运转转为动态解析），不提供目录。
            if (pagerState.targetPage == BoxDisclosurePageIndex) {
                // 目录按钮：菜单以按钮为锚点从右上角弹出。
                AppIconDropdownMenu(entries = indexMenu) {
                    Icon(imageVector = MiuixIcons.ListView, contentDescription = "目录")
                }
            }
        },
        bottomContent = {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = TabRowVerticalPadding),
            ) {
                // 主题风格切换时 TabRow 跟顶栏一起淡入淡出，不要瞬间换形。
                Crossfade(
                    targetState = settings.isGlassStyle,
                    animationSpec = tween(ThemeStyleFadeMillis),
                    label = "oddsTabRowStyle",
                ) { glassStyle ->
                    if (glassStyle) {
                        // 柔光玻璃：每个 Tab 一枚玻璃胶囊，材质自动继承顶栏（GlassTabRow 读顶栏上下文）。
                        GlassTabRow(
                            tabs = DisclosureTabTitles,
                            selectedIndex = pagerState.targetPage,
                            onSelect = { index -> scope.launch { pagerState.springAnimateToPage(index) } },
                            backdrop = LocalBlurBackdrop.current,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        // 取样层必须在这里读，不能在函数开头读：`LocalBlurBackdrop` 由 ScreenScaffold
                        // 提供在它自己的子组合里，而本 composable 的调用点（NavDisplay 的 entry）
                        // 在提供者之外，那时读到的是 staticCompositionLocalOf 的默认值 null。
                        val blurAvailable = if (settings.enableBlur) LocalBlurBackdrop.current != null else false
                        // 模糊开启时轨道用透明底：透出顶栏的磨砂层；关掉模糊时退回实色 surface，
                        // 与顶栏一致。
                        val colors = TabRowDefaults.tabRowColors(
                            backgroundColor = if (blurAvailable) {
                                Color.Transparent
                            } else {
                                MiuixTheme.colorScheme.surface
                            },
                        )
                        // 选中态跟 targetPage 而不是 currentPage：弹簧动画跨页时 currentPage 会
                        // 途经中间页，targetPage 在动画启动时就是终点。
                        TabRow(
                            tabs = DisclosureTabTitles,
                            selectedTabIndex = pagerState.targetPage,
                            onTabSelected = { index ->
                                scope.launch { pagerState.springAnimateToPage(index) }
                            },
                            colors = colors,
                        )
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // 与主界面一致：只有设置里打开「左右滑动切换页面」才允许手势翻页，
            // Tab 点击走的程序化动画不受影响。
            userScrollEnabled = settings.pageUserScroll,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = PagerNavigationSpringSpec,
            ),
            key = { it },
        ) { page ->
            when (page) {
                BoxDisclosurePageIndex -> BoxDisclosurePage(
                    categories = boxCategories,
                    listState = boxListState,
                    padding = padding,
                    onOpenBox = { categoryIndex, boxIndex ->
                        navigator.push(Route.OddsDisclosureInfo(categoryIndex, boxIndex))
                    },
                )

                else -> LotteryDisclosurePage(
                    listState = listStates[page],
                    padding = padding,
                    onOpenDraw = { categoryIndex, drawIndex ->
                        navigator.push(Route.LotteryOddsInfo(categoryIndex, drawIndex))
                    },
                )
            }
        }

        // 弹层必须放在 Scaffold 的 content 里：miuix 的弹窗宿主由 Scaffold 提供，
        // 声明在 Scaffold 外面会落进没人渲染的默认状态列表，点了等于没反应。
        OddsSourcesSheet(
            show = showSources,
            onDismissRequest = { showSources = false },
        )
    }
}

/** 宝箱页：勘误提醒 + 按类别分节的宝箱列表。 */
@Composable
private fun BoxDisclosurePage(
    categories: List<OddsCategory>,
    listState: LazyListState,
    padding: PaddingValues,
    onOpenBox: (categoryIndex: Int, boxIndex: Int) -> Unit,
) {
    LazyColumn(
        state = listState,
        // 铺满内容区：即使内容不满一屏，空白处的手势也能进列表，顶栏折叠与回弹才会响应。
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 12.dp,
            // 底部与 Java 版一致留 150dp，滚到最后一节时标题不会贴死在屏幕最下面。
            bottom = padding.calculateBottomPadding() + 150.dp,
        ),
    ) {
        item(key = "errata") {
            ErrataCard(text = OddsErrataTips)
        }
        categories.forEachIndexed { categoryIndex, category ->
            item(key = "category-$categoryIndex") {
                SmallTitle(category.title)
            }
            itemsIndexed(
                items = category.boxes,
                key = { boxIndex, _ -> "box-$categoryIndex-$boxIndex" },
            ) { boxIndex, box ->
                AboutContentCard {
                    ArrowPreference(
                        title = box.name,
                        startAction = {
                            // 封面与奖品图标一样按道具 id 从图床取，不进数据包。
                            RemoteItemImage(
                                id = box.image,
                                modifier = Modifier.padding(end = 12.dp).size(44.dp),
                                contentScale = ContentScale.Fit,
                            )
                        },
                        onClick = { onOpenBox(categoryIndex, boxIndex) },
                    )
                }
            }
        }
    }
}

/**
 * 抽奖页：幸运转转（每次进页动态解析官方公示）+ 按类别分节的抽奖列表。
 *
 * 公示页的物品会随活动轮换，所以不做本地数据表：进页就重新拉一次，解析结果按
 * 「幸运转转」一个分节、四个抽奖（普通转转 / 35000 保底奖励 / 140000 保底奖励 /
 * 豪华转转）展示；手工维护的抽奖按玩法分节（欢乐扭蛋 / 诸神宝殿 / 塔罗寻宝 /
 * 法老宝藏 / 许愿池），保底奖励归在各自玩法下。点条目进入 [LotteryOddsInfoScreen]。
 *
 * 进程内保留上次的解析结果：再次进页先显示旧数据再静默刷新，刷新失败只轻提示，
 * 不至于把能看的列表变成失败页；只有一次都没成功过时才整页报错并给「重新加载」。
 */
@Composable
private fun LotteryDisclosurePage(
    listState: LazyListState,
    padding: PaddingValues,
    onOpenDraw: (categoryIndex: Int, drawIndex: Int) -> Unit,
) {
    val toaster = LocalToaster.current
    var categories by remember { mutableStateOf(lotteryCategories()) }
    var loading by remember { mutableStateOf(categories.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadToken) {
        if (categories.isEmpty()) loading = true
        error = null
        val hasCache = categories.isNotEmpty()
        when (TurntableOddsRepository.refresh()) {
            is TurntableOddsRepository.Result.Success -> categories = lotteryCategories()
            TurntableOddsRepository.Result.NetworkError -> if (hasCache) {
                toaster.show(LotteryRefreshFailedTips)
            } else {
                error = LotteryNetworkErrorTips
            }

            TurntableOddsRepository.Result.ParseError -> if (hasCache) {
                toaster.show(LotteryRefreshFailedTips)
            } else {
                error = LotteryParseErrorTips
            }
        }
        loading = false
    }

    if (loading) {
        LotteryLoading(padding)
        return
    }
    if (categories.isEmpty()) {
        LotteryFailed(
            padding = padding,
            message = error ?: LotteryNetworkErrorTips,
            onRetry = { reloadToken += 1 },
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 150.dp,
        ),
    ) {
        // 勘误提醒与宝箱页置顶的卡同文案，四个页面保持一致。
        item(key = "lottery-errata") {
            ErrataCard(text = OddsErrataTips)
        }
        categories.forEachIndexed { categoryIndex, category ->
            item(key = "lottery-category-$categoryIndex") {
                SmallTitle(category.title)
            }
            itemsIndexed(
                items = category.draws,
                key = { drawIndex, _ -> "lottery-$categoryIndex-$drawIndex" },
            ) { drawIndex, draw ->
                AboutContentCard {
                    ArrowPreference(
                        title = draw.name,
                        // 幸运转转没有缩略图：startAction 传 null，目录行只显示文字。
                        startAction = draw.image?.let { image ->
                            {
                                RemoteItemImage(
                                    id = image,
                                    modifier = Modifier.padding(end = 12.dp).size(44.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        },
                        onClick = { onOpenDraw(categoryIndex, drawIndex) },
                    )
                }
            }
        }
    }
}

/** 首次进页的加载态：与查黑名单页同款的无限指示器 + 文案。 */
@Composable
private fun LotteryLoading(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InfiniteProgressIndicator()
            Text(
                text = "正在解析幸运转转概率",
                modifier = Modifier.padding(top = 12.dp),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
            )
        }
    }
}

/** 一次都没拉成功时的失败态：说明卡 + 「重新加载」，文案由调用方按失败类型选好。 */
@Composable
private fun LotteryFailed(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding() + 24.dp)
            .padding(horizontal = 12.dp),
    ) {
        TipsCard(tips = message, outerPadding = PaddingValues(0.dp))
        TextButton(
            text = "重新加载",
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
