package com.careful.hyperfvm.compose.screen.tools

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import com.careful.hyperfvm.compose.data.resourceCalculator.ResourceCalculatorData
import com.careful.hyperfvm.compose.data.resourceCalculator.ResourceSection
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.screen.ThemeStyleFadeMillis
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.isTabletLayout
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** TabRow 在顶栏 bottomContent 里的上下留白，与食神谱目录页的取值一致。 */
private val TabRowVerticalPadding = 6.dp

/** 输入数值或计算结果超出 Long 范围时顶栏副标题的兜底文案，对应 Java 版的兜底分支。 */
private const val ReserveOverflowText = "再玩就玩坏了"

/**
 * 资源储备计算器：按「数量 × 币值」把各类道具折算成储备总量。
 *
 * 顶栏下方用 TabRow 分成威望 / 金币 / 礼券 / 魅力币四页，页面间也可以左右滑动
 * （[HorizontalPager]）；每页的输入行表与各行币值都来自 [ResourceCalculatorData]，
 * 金币 / 礼券 / 魅力币三页的数据待补充，补齐后界面不用改。
 *
 * 与 Java 版 `calculateTotal` 的区别：不再给每个输入框挂 TextWatcher 手动触发计算，
 * 输入列表本身就是状态，任意一项变化都会重组并派生该页新的总储备文案；
 * 总储备显示在顶栏副标题上（Java 版是标题下方的独立 TextView），跟着选中的子页切换。
 */
@Composable
fun ResourceCalculatorScreen(onBack: () -> Unit) {
    val sections = ResourceCalculatorData.sections
    val settings = LocalAppSettings.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { sections.size })
    // 每个子页各存各的滚动位置：切 Tab 再切回来仍停在原处。
    val listStates = remember { List(sections.size) { LazyListState() } }
    // 每个子页各存各的输入，行数跟随该页的输入行表；重建后（深浅色切换等）继续保留。
    var inputs by rememberSaveable {
        mutableStateOf(sections.map { section -> List(section.fields.size) { "" } })
    }

    ScreenScaffold(
        title = "资源储备计算器",
        // 副标题跟着选中的子页走：用 targetPage 而不是 currentPage，弹簧动画跨页时
        // currentPage 会途经中间页，targetPage 在动画启动时就是终点（与 TabRow 选中态同源）。
        subtitle = computeReserveTotal(sections[pagerState.targetPage], inputs[pagerState.targetPage]),
        // 二级页没有外层 Pager 的底栏，底部安全区由内层 Scaffold 自己吃。
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        bottomContent = {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = TabRowVerticalPadding),
            ) {
                // 主题风格切换时 TabRow 跟顶栏一起淡入淡出，不要瞬间换形。
                Crossfade(
                    targetState = settings.isGlassStyle,
                    animationSpec = tween(ThemeStyleFadeMillis),
                    label = "resourceTabRowStyle",
                ) { glassStyle ->
                    if (glassStyle) {
                        // 柔光玻璃：每个 Tab 一枚玻璃胶囊，材质自动继承顶栏（GlassTabRow 读顶栏上下文）。
                        GlassTabRow(
                            tabs = sections.map { it.title },
                            selectedIndex = pagerState.targetPage,
                            onSelect = { index ->
                                // 切页先收掉焦点：不然后一页可能一进来就被弹出的输入法盖住。
                                focusManager.clearFocus()
                                scope.launch { pagerState.springAnimateToPage(index) }
                            },
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
                        TabRow(
                            tabs = sections.map { it.title },
                            selectedTabIndex = pagerState.targetPage,
                            onTabSelected = { index ->
                                // 切页先收掉焦点：不然后一页可能一进来就被弹出的输入法盖住。
                                focusManager.clearFocus()
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
            ReserveSectionPage(
                section = sections[page],
                inputs = inputs[page],
                onInputChange = { index, digits ->
                    inputs = inputs.toMutableList().also { pages ->
                        pages[page] = pages[page].toMutableList().also { it[index] = digits }
                    }
                },
                listState = listStates[page],
                padding = padding,
            )
        }
    }
}

/**
 * 单个子页的输入列表：一行一个道具，左右各一个 60dp 图标，中间是该道具的数量输入框。
 * 行表为空（数据待补充）时只显示顶部的说明卡。
 */
@Composable
private fun ReserveSectionPage(
    section: ResourceSection,
    inputs: List<String>,
    onInputChange: (index: Int, digits: String) -> Unit,
    listState: LazyListState,
    padding: PaddingValues,
) {
    val focusManager = LocalFocusManager.current
    // Java 版按设备动态设置容器水平边距：最短边 < 600dp 用 10dp，否则 20dp
    val containerPadding = if (isTabletLayout()) 20.dp else 10.dp

    LazyColumn(
        state = listState,
        // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，滚动与顶栏折叠才会响应。
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding(),
        ),
    ) {
        item(key = "tips") {
            TipsCard("请根据提示输入您在游戏中拥有的对应物品的数量，输入后将自动计算出结果。")
        }

        itemsIndexed(section.fields, key = { index, _ -> "field-$index" }) { index, field ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = containerPadding)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReserveFieldIcon(id = field.leadingIconRes)
                TextField(
                    value = inputs.getOrElse(index) { "" },
                    onValueChange = { raw ->
                        // 只保留数字：number 键盘之外还能粘贴进来任意文本
                        onInputChange(index, raw.filter { it in '0'..'9' })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    label = field.label,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index == section.fields.lastIndex) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = if (index == section.fields.lastIndex) {
                        KeyboardActions(onDone = { focusManager.clearFocus() })
                    } else {
                        KeyboardActions.Default
                    },
                )
                ReserveFieldIcon(id = field.trailingIconRes)
            }
        }
    }
}

/**
 * 行首/行尾的 60dp 图标：有 id 时交给 [RemoteItemImage]——游戏道具 id 走图床，
 * 自制的数据包图片 id（如威望页的储备图标）回退读数据包；
 * 没有 id 时画 Miuix 的图片占位图标，与数据图合集里未下载卡片的占位完全一致。
 */
@Composable
private fun ReserveFieldIcon(id: String?) {
    if (id != null) {
        RemoteItemImage(
            id = id,
            modifier = Modifier.size(60.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = MiuixIcons.Image,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}

/**
 * 总储备文案：该页所有输入按「数量 × 币值」求和。
 *
 * 空输入按 0 计；数值或累加/乘法结果超出 Long 范围时返回兜底文案
 * （对应 Java 版捕获 NumberFormatException / ArithmeticException 的分支）。
 */
private fun computeReserveTotal(section: ResourceSection, inputs: List<String>): String {
    var total = 0L
    try {
        section.fields.forEachIndexed { index, field ->
            val text = inputs.getOrNull(index)?.trim().orEmpty()
            if (text.isEmpty()) return@forEachIndexed
            total = Math.addExact(total, Math.multiplyExact(text.toLong(), field.coinValue))
        }
    } catch (_: NumberFormatException) {
        return ReserveOverflowText
    } catch (_: ArithmeticException) {
        return ReserveOverflowText
    }
    return "总储备：$total"
}
