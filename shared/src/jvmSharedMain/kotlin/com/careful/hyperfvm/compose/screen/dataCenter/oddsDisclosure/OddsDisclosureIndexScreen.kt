package com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.dialog.UrlTarget
import com.careful.hyperfvm.compose.component.dialog.VisitUrlDialog
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDisclosureData
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.screen.about.AboutContentCard
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.preference.ArrowPreference

/** 目录页第一张 Important 卡：说明数据来源，点击打开官方原帖。 */
private const val SourceTips =
    "以下概率公示均获取自官方发布的帖子，部分冷门宝箱未收录，您可以点击本卡片访问原帖查看完整内容。"

/** 目录页第二张卡（错误色容器）：整理说明与勘误纪律。 */
private const val IndexErrataTips =
    "本页面所有内容均为人工整理，若您发现可能有误的地方，请先前往原帖进行核实，如确实有误，请向开发者反馈。\n" +
        "请不要在官方评论区展示本页面的任何内容，尤其是出现勘误的时候，谢谢您的配合。"

/** 点击来源卡弹出的外链确认框目标，对应 Java 版 `showDialogAndVisitUrl` 的参数。 */
private val SourcePostTarget = UrlTarget(
    title = "4399美食大战老鼠",
    subtitle = "概率公示说明",
    url = "https://my.4399.com/forums/thread-63198880",
)

/**
 * 目录菜单跳转用的 LazyColumn 下标。
 *
 * 每个类别标题前面固定有 2 个条目（来源提示、勘误提醒），之后每个类别占
 * 「1 个标题 + 宝箱数」条。数据是静态的，第一次组合时算好后不再变化。
 */
private val CategoryItemIndices: List<Int> = run {
    var index = 2
    OddsDisclosureData.categories.map { category ->
        index.also { index += 1 + category.boxes.size }
    }
}

/**
 * 宝箱概率公示合集目录页，对应 Java 版 `OddsDisclosureIndexActivity`：
 * 顶部两张提示卡 + 按类别分节的宝箱列表，右上角目录按钮可跳转到任意分节。
 */
@Composable
fun OddsDisclosureIndexScreen(onBack: () -> Unit) {
    val navigator = LocalNavigator.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    // 自己持有顶栏滚动状态，目录跳转时才能手动同步折叠。
    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()
    val dropdownColors = DropdownDefaults.dropdownColors()

    // 外链确认框与目录菜单的状态提到列表外面，列表项回收时弹层不会被一起销毁。
    var pendingUrl by remember { mutableStateOf<UrlTarget?>(null) }
    var showIndex by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "宝箱概率公示合集",
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        actions = {
            // 目录按钮：菜单以按钮为锚点从右上角弹出，条目与分节标题同源，天然一一对应。
            Box {
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        showIndex = true
                    },
                ) {
                    Icon(imageVector = MiuixIcons.ListView, contentDescription = "目录")
                }
                OverlayListPopup(
                    show = showIndex,
                    popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(verticalMargin = 4.dp),
                    // 菜单右缘与按钮右缘对齐，与 Java 版 ListPopupWindow 的定位一致。
                    alignment = PopupPositionProvider.Align.End,
                    onDismissRequest = { showIndex = false },
                    onDismissFinished = {},
                ) {
                    ListPopupColumn {
                        OddsDisclosureData.categories.forEachIndexed { index, category ->
                            DropdownImpl(
                                text = category.title,
                                optionSize = OddsDisclosureData.categories.size,
                                // 目录只是跳转入口，没有任何一条处于选中态。
                                isSelected = false,
                                index = index,
                                dropdownColors = dropdownColors,
                                onSelectedIndexChange = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showIndex = false
                                    // animateScrollToItem 是程序化滚动，不派发 nested scroll，
                                    // 顶栏不会自己收起；先把折叠状态同步到目标位置，再滚。
                                    // 列表位置按内容坐标计算，顶栏高度变化不影响落点。
                                    scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
                                    scope.launch { listState.animateScrollToItem(CategoryItemIndices[it]) }
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
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
            item(key = "tips") {
                TipsCard(tips = SourceTips, onClick = { pendingUrl = SourcePostTarget })
            }
            item(key = "errata") {
                ErrataCard(text = IndexErrataTips)
            }
            OddsDisclosureData.categories.forEachIndexed { categoryIndex, category ->
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
                                Image(
                                    modifier = Modifier.padding(end = 12.dp).size(44.dp),
                                    painter = painterResource(box.image),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onClick = { navigator.push(Route.OddsDisclosureInfo(categoryIndex, boxIndex)) },
                        )
                    }
                }
            }
        }

        // 弹窗必须放在 Scaffold 的 content 里：miuix 的弹窗宿主由 Scaffold 提供，
        // 声明在 Scaffold 外面会落进没人渲染的默认状态列表，点了等于没反应。
        VisitUrlDialog(
            target = pendingUrl,
            onDismiss = { pendingUrl = null },
        )
    }
}
