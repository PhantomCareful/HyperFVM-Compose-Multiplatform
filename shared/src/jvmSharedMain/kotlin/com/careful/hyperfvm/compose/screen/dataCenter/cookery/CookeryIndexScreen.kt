package com.careful.hyperfvm.compose.screen.dataCenter.cookery

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.cookery.CookeryCard
import com.careful.hyperfvm.compose.data.cookery.CookeryData
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.screen.about.AboutContentCard
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** TabRow 在顶栏 bottomContent 里的上下留白，与防御卡目录页搜索框区块的取值一致。 */
private val TabRowVerticalPadding = 6.dp

/**
 * 食神谱合集目录页，对应 Java 版 `CookeryIndexActivity`：
 * 按上等佳肴 / 秘制佳肴 / 极品佳肴分成三页，顶栏下方用 TabRow 横向切换，
 * 页面间也可以左右滑动（[HorizontalPager]）；点卡片从底部拉起详情弹层（[CookeryDetailSheet]）。
 *
 * 分段选择器挂在 `ScreenScaffold` 的 `bottomContent` 槽位——与防御卡目录页的搜索框一样，
 * 属于顶栏的一部分：跟着大标题一起折叠，并处于顶栏的模糊区域内。
 */
@Composable
fun CookeryIndexScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    val pagerState = rememberPagerState(pageCount = { CookeryData.sections.size })
    val scope = rememberCoroutineScope()
    // 每个分组各存各的滚动位置：切 Tab 再切回来仍停在原处。
    val listStates = remember { List(CookeryData.sections.size) { LazyListState() } }

    // 详情卡与弹层开关分开存：收起动画期间卡片仍需有值，标题与内容才能正常滑出。
    var openedCard by remember { mutableStateOf<CookeryCard?>(null) }
    var showDetail by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "食神谱合集",
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        bottomContent = {
            // 取样层必须在这里读，不能在函数开头读：`LocalBlurBackdrop` 由 ScreenScaffold
            // 提供在它自己的子组合里，而本 composable 的调用点（NavDisplay 的 entry）
            // 在提供者之外，那时读到的是 staticCompositionLocalOf 的默认值 null。
            val blurAvailable = if (settings.enableBlur) LocalBlurBackdrop.current != null else false
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = TabRowVerticalPadding),
            ) {
                val tabs = CookeryData.sections.map { it.title }
                // 点 Tab 用弹簧动画翻页，左右滑动时选中态也同步。
                val onTabSelected: (Int) -> Unit = { index ->
                    scope.launch { pagerState.springAnimateToPage(index) }
                }
                // 模糊开启时轨道用透明底：高斯模式透出顶栏的磨砂层，渐进模式透出顶栏的
                // 渐进模糊带（含外延部分）；关掉模糊时退回实色 surface，与顶栏一致。
                val colors = TabRowDefaults.tabRowColors(
                    backgroundColor = if (blurAvailable) {
                        Color.Transparent
                    } else {
                        MiuixTheme.colorScheme.surface
                    },
                )
                // 选中态跟 targetPage 而不是 currentPage：弹簧动画跨页时 currentPage 会
                // 途经中间页（从第 1 切第 3 会先亮第 2），targetPage 在动画启动时就是终点。
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = pagerState.targetPage,
                    onTabSelected = onTabSelected,
                    colors = colors,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                val section = CookeryData.sections[page]
                LazyColumn(
                    state = listStates[page],
                    // 铺满内容区：即使内容不满一屏，空白处的手势也能进列表，顶栏折叠与回弹才会响应。
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 12.dp,
                        // 底部与 Java 版一致留 150dp，滚到列表末尾时内容不会贴死在屏幕最下面。
                        bottom = padding.calculateBottomPadding() + 150.dp,
                    ),
                ) {
                    itemsIndexed(
                        items = section.cards,
                        key = { cardIndex, _ -> "card-$page-$cardIndex" },
                    ) { _, card ->
                        AboutContentCard {
                            ArrowPreference(
                                title = card.name,
                                summary = card.description,
                                startAction = {
                                    Image(
                                        modifier = Modifier.padding(end = 12.dp).size(44.dp),
                                        painter = painterResource(card.image),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                    )
                                },
                                onClick = {
                                    openedCard = card
                                    showDetail = true
                                },
                            )
                        }
                    }
                }
            }

            // 详情弹层必须放在 Scaffold 的 content 里：miuix 的弹窗宿主由 Scaffold 提供，
            // 声明在 Scaffold 外面会落进没人渲染的默认状态列表，点了等于没反应。
            CookeryDetailSheet(
                card = openedCard,
                show = showDetail,
                onDismissRequest = { showDetail = false },
            )
        }
    }
}
