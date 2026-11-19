package com.careful.hyperfvm.compose.screen.tools

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.card.importantCardColors
import com.careful.hyperfvm.compose.component.image.PackImage
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import com.careful.hyperfvm.compose.data.cardOdds.CardOddsData
import com.careful.hyperfvm.compose.platform.KeyValueStore
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.math.BigDecimal
import java.math.RoundingMode

/** 各组选中值持久化用的文件名，对应 Java 版 activity 私有的 SharedPreferences。 */
private const val CardOddsPrefsName = "card_odds_calculator"

// 各组选中状态的保存键，键名与 Java 版完全一致
private const val KeyMain = "state_selected_main_card"
private const val KeySub1 = "state_selected_sub_1"
private const val KeySub1Category = "state_selected_sub_1_category"
private const val KeySub2 = "state_selected_sub_2"
private const val KeySub2Category = "state_selected_sub_2_category"
private const val KeySub3 = "state_selected_sub_3"
private const val KeySub3Category = "state_selected_sub_3_category"
private const val KeyClover = "state_selected_four_leaf_clover"
private const val KeyVip = "state_selected_vip"
private const val KeyConsortia = "state_selected_consortia"
private const val KeyPreset = "state_selected_preset"

/** 图标组菜单的首项：逻辑值 -1，展示位上显示占位图。 */
private const val NotSelect = "不选"

/** 公会合成屋未选中时展示位上的文字。 */
private const val NoneText = "无"

/** 副卡类型菜单顺序（好卡/中卡/差卡），对应逻辑值 2/1/0。 */
private val CategoryLabels = listOf("好卡", "中卡", "差卡")

private const val CategoryTips =
    "副卡类型分为好卡、中卡、差卡3种，相同星级下的成功率：好卡>中卡>差卡。您可以在防御卡全能数据库中查询卡片的副卡类型。"

private const val GuaranteeTips =
    "启用保险金进行强化时会激活保底机制，当强化失败的累积概率（等号右边的值）超过120%后，下一次强化（无论是否启用保险金）必成。\n利用保底机制，您可以在强化高星级卡片时节省一次保险金费用。"

/**
 * 数值展示的缩字策略：Java 版每次 setText 后按可用宽度等比缩小字号让文案单行放下，
 * 这里交给 miuix Text 的 StepBased 自动适配（10sp 下限，20sp 原始字号）。
 */
private val ValueAutoSize = TextAutoSize.StepBased(
    minFontSize = 10.sp,
    maxFontSize = 20.sp,
    stepSize = 0.5.sp,
)

/**
 * 计算器的 10 组选中值 + 生效预设下标。
 *
 * 逻辑值语义与 Java 版 `DropdownGroup` 一致：图标组 -1=不选，副卡类型 0=差卡 / 1=中卡 / 2=好卡。
 * 状态直接挂在类上（mutableIntStateOf），任意一组变化都会驱动读取它的界面重组，
 * 不再需要 Java 版那样手动调用 recalculate。
 */
@Stable
private class CardOddsSelection(
    main: Int = -1,
    sub1: Int = -1,
    sub1Category: Int = 2,
    sub2: Int = -1,
    sub2Category: Int = 2,
    sub3: Int = -1,
    sub3Category: Int = 2,
    clover: Int = -1,
    vip: Int = -1,
    consortia: Int = -1,
    appliedPreset: Int = -1,
) {
    var main: Int by mutableIntStateOf(main)
        private set
    var sub1: Int by mutableIntStateOf(sub1)
        private set
    var sub1Category: Int by mutableIntStateOf(sub1Category)
        private set
    var sub2: Int by mutableIntStateOf(sub2)
        private set
    var sub2Category: Int by mutableIntStateOf(sub2Category)
        private set
    var sub3: Int by mutableIntStateOf(sub3)
        private set
    var sub3Category: Int by mutableIntStateOf(sub3Category)
        private set
    var clover: Int by mutableIntStateOf(clover)
        private set
    var vip: Int by mutableIntStateOf(vip)
        private set
    var consortia: Int by mutableIntStateOf(consortia)
        private set

    /** 当前生效的预设下标，-1 表示没有命中任何预设。 */
    var appliedPreset: Int by mutableIntStateOf(appliedPreset)
        private set

    fun selectMain(value: Int) = markCustom { main = value }

    fun selectSub1(value: Int) = markCustom { sub1 = value }

    fun selectSub1Category(value: Int) = markCustom { sub1Category = value }

    fun selectSub2(value: Int) = markCustom { sub2 = value }

    fun selectSub2Category(value: Int) = markCustom { sub2Category = value }

    fun selectSub3(value: Int) = markCustom { sub3 = value }

    fun selectSub3Category(value: Int) = markCustom { sub3Category = value }

    fun selectClover(value: Int) = markCustom { clover = value }

    fun selectVip(value: Int) = markCustom { vip = value }

    fun selectConsortia(value: Int) = markCustom { consortia = value }

    /** 手动改动任意一组后组合已偏离预设，清除预设命中标记（与 Java 版 selectByMenuIndex 一致）。 */
    private fun markCustom(block: () -> Unit) {
        block()
        appliedPreset = -1
    }

    /**
     * 套用预设：只写 8 个预设字段，VIP 与公会合成屋保持现状；
     * 类型字段越界时回退「好卡」（对应 Java 版 setLogic 的默认值兜底）。
     */
    fun applyPreset(index: Int) {
        val preset = CardOddsData.Presets.getOrNull(index) ?: return
        main = preset.main
        sub1 = preset.sub1
        sub1Category = sanitizeCategory(preset.sub1Category)
        sub2 = preset.sub2
        sub2Category = sanitizeCategory(preset.sub2Category)
        sub3 = preset.sub3
        sub3Category = sanitizeCategory(preset.sub3Category)
        clover = preset.clover
        appliedPreset = index
    }

    companion object {
        /** 界面重建（深浅色切换、旋转等）后恢复全部选中值与预设标记。 */
        val Saver = listSaver<CardOddsSelection, Int>(
            save = {
                listOf(
                    it.main, it.sub1, it.sub1Category, it.sub2, it.sub2Category,
                    it.sub3, it.sub3Category, it.clover, it.vip, it.consortia, it.appliedPreset,
                )
            },
            restore = {
                CardOddsSelection(
                    main = it[0],
                    sub1 = it[1],
                    sub1Category = it[2],
                    sub2 = it[3],
                    sub2Category = it[4],
                    sub3 = it[5],
                    sub3Category = it[6],
                    clover = it[7],
                    vip = it[8],
                    consortia = it[9],
                    appliedPreset = it[10],
                )
            },
        )
    }
}

/** 一次计算的完整结果：三段百分比（不含 %）、保底文案与按主卡等级取的消耗。 */
private data class CardOddsResult(
    val percentA: String,
    val percentB: String,
    val percentC: String,
    val guarantee: String,
    val coin: Int,
    val insurance: Int,
)

/**
 * 强卡成功率计算器：复刻 Java 版 `CardOddsCalculatorActivity`。
 *
 * 与 Java 版的区别只有状态管理：10 组选中值是 Compose 状态，任意一组变化都会自动重算，
 * 不再手动调用 recalculate；右上角的预设组合菜单改用 miuix 的 OverlayBottomSheet 承载。
 */
@Composable
fun CardOddsCalculatorScreen(onBack: () -> Unit) {
    val selection = rememberCardOddsSelection()
    // 直接读状态派生结果：任意一组选中值变化都会在这里重组并重新计算
    val result = computeCardOdds(selection)
    var showPresets by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(
        title = "强卡成功率计算器",
        // 二级页没有外层 Pager 的底栏，底部安全区由内层 Scaffold 自己吃。
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        actions = {
            IconButton(onClick = { showPresets = true }) {
                Icon(
                    imageVector = MiuixIcons.ListView,
                    contentDescription = "预设组合",
                )
            }
        },
    ) { padding ->
        LazyColumn(
            // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，滚动与顶栏折叠才会响应。
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 12.dp,
            ),
        ) {
            item(key = "selectors") { SelectorSection(selection) }
            item(key = "resultTitle") { SmallTitle("计算结果") }
            item(key = "percent") { PercentRow(result) }
            item(key = "cost") { CostRow(result) }
            item(key = "categoryTitle") { SmallTitle("副卡类型") }
            item(key = "categoryTips") { TipsCard(CategoryTips) }
            item(key = "categoryCard") { CategoryCard(selection) }
            item(key = "guaranteeTitle") { SmallTitle("保底说明") }
            item(key = "guaranteeTips") { TipsCard(GuaranteeTips) }
        }

        PresetSheet(
            show = showPresets,
            onDismissRequest = { showPresets = false },
            onSelect = { index ->
                selection.applyPreset(index)
                showPresets = false
            },
        )
    }
}

/** 读取上次的选中值（越界记录回退默认值），并把每次变化写回键值存储。 */
@Composable
private fun rememberCardOddsSelection(): CardOddsSelection {
    val env = LocalAppEnv.current
    val prefs = remember(env) { env.prefs(CardOddsPrefsName) }
    val selection = rememberSaveable(saver = CardOddsSelection.Saver) {
        CardOddsSelection(
            main = prefs.readRange(KeyMain, -1, -1, 15),
            sub1 = prefs.readRange(KeySub1, -1, -1, 16),
            sub1Category = prefs.readRange(KeySub1Category, 2, 0, 2),
            sub2 = prefs.readRange(KeySub2, -1, -1, 16),
            sub2Category = prefs.readRange(KeySub2Category, 2, 0, 2),
            sub3 = prefs.readRange(KeySub3, -1, -1, 16),
            sub3Category = prefs.readRange(KeySub3Category, 2, 0, 2),
            clover = prefs.readRange(KeyClover, -1, -1, CardOddsData.FourLeafClover.lastIndex),
            vip = prefs.readRange(KeyVip, -1, -1, CardOddsData.Vip.lastIndex),
            consortia = prefs.readRange(KeyConsortia, -1, -1, CardOddsData.Consortia.lastIndex),
            appliedPreset = prefs.readRange(KeyPreset, -1, -1, CardOddsData.Presets.lastIndex),
        )
    }
    LaunchedEffect(
        selection.main, selection.sub1, selection.sub1Category,
        selection.sub2, selection.sub2Category, selection.sub3, selection.sub3Category,
        selection.clover, selection.vip, selection.consortia, selection.appliedPreset,
    ) {
        prefs.putInt(KeyMain, selection.main)
        prefs.putInt(KeySub1, selection.sub1)
        prefs.putInt(KeySub1Category, selection.sub1Category)
        prefs.putInt(KeySub2, selection.sub2)
        prefs.putInt(KeySub2Category, selection.sub2Category)
        prefs.putInt(KeySub3, selection.sub3)
        prefs.putInt(KeySub3Category, selection.sub3Category)
        prefs.putInt(KeyClover, selection.clover)
        prefs.putInt(KeyVip, selection.vip)
        prefs.putInt(KeyConsortia, selection.consortia)
        prefs.putInt(KeyPreset, selection.appliedPreset)
        prefs.commit()
    }
    return selection
}

/** 从持久化读取一个逻辑值，记录缺失或越界时回退 [default]。 */
private fun KeyValueStore.readRange(
    key: String,
    default: Int,
    min: Int,
    max: Int,
): Int = getInt(key, default).takeIf { it in min..max } ?: default

/** 三个选中区的排列：副卡1 单独居中，副卡2/主卡/副卡3 一行，四叶草/公会/VIP 三等分。 */
@Composable
private fun SelectorSection(selection: CardOddsSelection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconSelector(
            title = "副卡1",
            imageId = starDrawable(selection.sub1),
            entry = starMenu(selection.sub1, 16) { selection.selectSub1(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSelector(
                title = "副卡2",
                imageId = starDrawable(selection.sub2),
                entry = starMenu(selection.sub2, 16) { selection.selectSub2(it) },
            )
            Spacer(modifier = Modifier.width(20.dp))
            IconSelector(
                title = "主卡",
                imageId = starDrawable(selection.main),
                entry = starMenu(selection.main, 15) { selection.selectMain(it) },
            )
            Spacer(modifier = Modifier.width(20.dp))
            IconSelector(
                title = "副卡3",
                imageId = starDrawable(selection.sub3),
                entry = starMenu(selection.sub3, 16) { selection.selectSub3(it) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            // 三列宽度各不相同，等分会让窄列被 Box 的默认左上对齐推向左侧；
            // 交给 SpaceEvenly 均匀分布，视觉上就是居中且间距一致。
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconSelector(
                title = "四叶草",
                imageId = cloverDrawable(selection.clover),
                entry = cloverMenu(selection.clover) { selection.selectClover(it) },
            )
            ConsortiaSelector(selection = selection)
            IconSelector(
                title = "VIP等级",
                imageId = vipDrawable(selection.vip),
                entry = vipMenu(selection.vip) { selection.selectVip(it) },
            )
        }
    }
}

/**
 * 图标展示位 + 标题：整列作为下拉菜单的点击区，对应 Java 版的“标题+图片”整列。
 *
 * 点击本身不带视觉反馈（Java 版也没有），所以不用 IconButton 包裹：
 * 它的 indication 会出按压效果、圆角还会裁掉 55dp 展示图的四角，
 * 这里只保留无 indication 的 clickable 与 [SelectorPopup]。
 */
@Composable
private fun IconSelector(
    title: String,
    imageId: String,
    entry: DropdownEntry,
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box {
        Column(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { expanded = !expanded },
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = MiuixTheme.textStyles.subtitle.fontSize,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            PackImage(
                id = imageId,
                modifier = Modifier.size(55.dp),
                contentScale = ContentScale.Fit,
            )
        }
        SelectorPopup(
            entry = entry,
            show = expanded,
            onDismiss = { expanded = false },
        )
    }
}

/** 公会合成屋展示位：没有对应图片，用 45sp 粗体数字（未选中显示“无”）撑起 55dp 占位。 */
@Composable
private fun ConsortiaSelector(selection: CardOddsSelection) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box {
        Column(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { expanded = !expanded },
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "公会合成屋",
                fontSize = MiuixTheme.textStyles.subtitle.fontSize,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.size(55.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selection.consortia >= 0) "${selection.consortia + 1}" else NoneText,
                    fontSize = 45.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 20.sp,
                        maxFontSize = 45.sp,
                        stepSize = 1.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
        SelectorPopup(
            entry = consortiaMenu(selection.consortia) { selection.selectConsortia(it) },
            show = expanded,
            onDismiss = { expanded = false },
        )
    }
}

/**
 * 选择器共用的下拉弹层。
 *
 * 不能直接用 OverlayDropdownPopup：它内部固定走 ListPopupColumn，把宽度硬编码在 200dp~288dp，
 * 只有一张图的星级/VIP 菜单会被撑成长条。这里改用 [OverlayListPopup] 并放开 minWidth，
 * 宽度交给 [CompactPopupColumn] 按最宽条目决定。
 */
@Composable
private fun SelectorPopup(entry: DropdownEntry, show: Boolean, onDismiss: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    val dropdownColors = DropdownDefaults.dropdownColors()
    val items = entry.items
    val scrollState = rememberScrollState()
    // 每行在菜单内的纵向位置，用来把当前选中项滚到菜单顶部
    val rowOffsets = remember { mutableStateMapOf<Int, Int>() }
    val selectedIndex = items.indexOfFirst { it.selected }

    // 对应 Java 版 ListView 的 setSelection：每次弹出都把选中项对齐到菜单顶部，
    // 选中靠底部时受滚动上限限制，选中项仍然可见。
    LaunchedEffect(show, selectedIndex) {
        if (!show || selectedIndex < 0) return@LaunchedEffect
        val offset = snapshotFlow { rowOffsets[selectedIndex] }
            .filterNotNull()
            .first()
        scrollState.scrollTo(offset)
    }

    OverlayListPopup(
        show = show,
        alignment = PopupPositionProvider.Align.End,
        onDismissRequest = onDismiss,
        onDismissFinished = {},
        // 原生默认 200dp，这里放开让弹层自己收缩
        minWidth = 0.dp,
        renderInRootScaffold = true,
    ) {
        CompactPopupColumn(scrollState = scrollState) {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        rowOffsets[index] = coordinates.positionInParent().y.roundToInt()
                    },
                    // 把最小宽度约束继续传给行，整行背景与点击区才铺满弹层
                    propagateMinConstraints = true,
                ) {
                    DropdownImpl(
                        item = item,
                        optionSize = items.size,
                        isSelected = item.selected,
                        index = index,
                        dropdownColors = dropdownColors,
                        enabled = entry.enabled && item.enabled,
                        onSelectedIndexChange = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            item.onClick?.invoke()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/**
 * ListPopupColumn 的紧凑版：同样按最宽条目定宽，并把子项统一到同一宽度（点击区域整行一致），
 * 但去掉了 200dp 最小宽度下限，纯图片菜单的宽度只由图片本身决定。
 */
@Composable
private fun CompactPopupColumn(scrollState: ScrollState, content: @Composable () -> Unit) {
    val measurePolicy = remember {
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                var maxIntrinsic = 0
                measurables.forEach { measurable ->
                    val width = measurable.maxIntrinsicWidth(constraints.maxHeight)
                    if (width > maxIntrinsic) maxIntrinsic = width
                }
                val listWidth = maxIntrinsic.coerceIn(constraints.minWidth, constraints.maxWidth)
                val childConstraints = constraints.copy(minWidth = listWidth, maxWidth = listWidth, minHeight = 0)

                val placeables = ArrayList<Placeable>(measurables.size)
                var listHeight = 0
                measurables.forEach { measurable ->
                    val placeable = measurable.measure(childConstraints)
                    placeables.add(placeable)
                    listHeight += placeable.height
                }

                return layout(listWidth, listHeight) {
                    var currentY = 0
                    placeables.forEach { placeable ->
                        placeable.placeRelative(0, currentY)
                        currentY += placeable.height
                    }
                }
            }
        }
    }
    Layout(
        content = content,
        modifier = Modifier
            .focusGroup()
            .verticalScroll(state = scrollState),
        measurePolicy = measurePolicy,
    )
}

/** 星级菜单：首项“不选”，其余按 0..[maxStar] 配星级图（主卡 15、副卡 16）。 */
private fun starMenu(selected: Int, maxStar: Int, onSelect: (Int) -> Unit): DropdownEntry =
    DropdownEntry(
        items = List(maxStar + 2) { index ->
            val logic = index - 1
            DropdownItem(
                text = "",
                selected = logic == selected,
                onClick = { onSelect(logic) },
                icon = { iconModifier ->
                    PackImage(
                        id = starDrawable(logic),
                        modifier = iconModifier.size(40.dp),
                        contentDescription = if (logic < 0) NotSelect else "$logic",
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        },
    )

/** 四叶草菜单：首项“不选”，其余为图片 + 加成倍率文字（2.0 显示为“2倍”）。 */
private fun cloverMenu(selected: Int, onSelect: (Int) -> Unit): DropdownEntry =
    DropdownEntry(
        items = List(CardOddsData.CloverDrawables.size + 1) { index ->
            val logic = index - 1
            DropdownItem(
                text = if (logic < 0) NotSelect else cloverLabel(logic),
                selected = logic == selected,
                onClick = { onSelect(logic) },
                icon = { iconModifier ->
                    RemoteItemImage(
                        id = cloverDrawable(logic),
                        modifier = iconModifier.size(40.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        },
    )

/** VIP 菜单：首项“不选”，其余为 1~16 级的等级图。 */
private fun vipMenu(selected: Int, onSelect: (Int) -> Unit): DropdownEntry =
    DropdownEntry(
        items = List(CardOddsData.VipDrawables.size + 1) { index ->
            val logic = index - 1
            DropdownItem(
                text = "",
                selected = logic == selected,
                onClick = { onSelect(logic) },
                icon = { iconModifier ->
                    PackImage(
                        id = vipDrawable(logic),
                        modifier = iconModifier.size(40.dp),
                        contentDescription = if (logic < 0) NotSelect else "${logic + 1}",
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        },
    )

/** 公会合成屋菜单：纯文字，首项“不选”，其余为 1~6 级。 */
private fun consortiaMenu(selected: Int, onSelect: (Int) -> Unit): DropdownEntry =
    DropdownEntry(
        items = List(CardOddsData.Consortia.size + 1) { index ->
            val logic = index - 1
            DropdownItem(
                text = if (logic < 0) NotSelect else "${logic + 1}",
                selected = logic == selected,
                onClick = { onSelect(logic) },
            )
        },
    )

/** 四叶草菜单文字：去掉小数末尾的 0（2.0 → “2倍”，1.2 → “1.2倍”），与 Java 版一致。 */
private fun cloverLabel(logic: Int): String =
    BigDecimal.valueOf(CardOddsData.FourLeafClover[logic])
        .stripTrailingZeros()
        .toPlainString() + "倍"

/** 星级逻辑值 → 展示图；未选中或越界显示占位图。 */
private fun starDrawable(logic: Int): String =
    if (logic in CardOddsData.StarDrawables.indices) {
        CardOddsData.StarDrawables[logic]
    } else {
        CardOddsData.PLACEHOLDER
    }

/** 四叶草逻辑值 → 展示图；未选中或越界显示占位图。 */
private fun cloverDrawable(logic: Int): String =
    if (logic in CardOddsData.CloverDrawables.indices) {
        CardOddsData.CloverDrawables[logic]
    } else {
        CardOddsData.PLACEHOLDER
    }

/** VIP 逻辑值 → 展示图；未选中或越界显示占位图。 */
private fun vipDrawable(logic: Int): String =
    if (logic in CardOddsData.VipDrawables.indices) {
        CardOddsData.VipDrawables[logic]
    } else {
        CardOddsData.PLACEHOLDER
    }

/** 三段成功率：A + B = C，三张 Important 卡等宽。 */
@Composable
private fun PercentRow(result: CardOddsResult) {
    val colors = importantCardColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PercentCard(text = "${result.percentA}%", colors = colors, modifier = Modifier.weight(1f))
        Text(
            text = "+",
            modifier = Modifier.padding(horizontal = 6.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        PercentCard(text = "${result.percentB}%", colors = colors, modifier = Modifier.weight(1f))
        Text(
            text = "=",
            modifier = Modifier.padding(horizontal = 6.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        PercentCard(text = "${result.percentC}%", colors = colors, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PercentCard(text: String, colors: CardColors, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
        cornerRadius = 20.dp,
        colors = colors,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = colors.contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            autoSize = ValueAutoSize,
            maxLines = 1,
        )
    }
}

/** 金币 / 保险金 / 保底三张 General 卡等宽。 */
@Composable
private fun CostRow(result: CardOddsResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CostCard(title = "金币", value = result.coin.toString(), modifier = Modifier.weight(1f))
        CostCard(title = "保险金", value = result.insurance.toString(), modifier = Modifier.weight(1f))
        CostCard(title = "保底", value = result.guarantee, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CostCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
        cornerRadius = 20.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 2.dp),
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                autoSize = ValueAutoSize,
                maxLines = 1,
            )
        }
    }
}

/** 三张副卡的类型选择，放在同一张 General 卡里。 */
@Composable
private fun CategoryCard(selection: CardOddsSelection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        OverlayDropdownPreference(
            title = "副卡1",
            items = CategoryLabels,
            selectedIndex = categoryMenuIndex(selection.sub1Category),
            onSelectedIndexChange = { selection.selectSub1Category(categoryLogic(it)) },
        )
        OverlayDropdownPreference(
            title = "副卡2",
            items = CategoryLabels,
            selectedIndex = categoryMenuIndex(selection.sub2Category),
            onSelectedIndexChange = { selection.selectSub2Category(categoryLogic(it)) },
        )
        OverlayDropdownPreference(
            title = "副卡3",
            items = CategoryLabels,
            selectedIndex = categoryMenuIndex(selection.sub3Category),
            onSelectedIndexChange = { selection.selectSub3Category(categoryLogic(it)) },
        )
    }
}

/** 逻辑值 → 菜单下标：好卡 0 / 中卡 1 / 差卡 2。 */
private fun categoryMenuIndex(logic: Int): Int = (2 - logic).coerceIn(0, 2)

/** 菜单下标 → 逻辑值（概率表下标）。 */
private fun categoryLogic(menuIndex: Int): Int = (2 - menuIndex).coerceIn(0, 2)

/** 预设组合弹层：一行一条预设（标题 + 主卡/副卡1~3/四叶草预览图），点击即套用，不展示任何选中态。 */
@Composable
private fun PresetSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        title = "预设组合",
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(CardOddsData.Presets) { index, preset ->
                PresetCard(
                    preset = preset,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

/**
 * 一条预设：独立 Card 承载，可点击套用。
 *
 * 弹层背景是 `background`，与 Card 默认的 `surfaceContainer` 同色；
 * 按 miuix demo 的 BottomSheet 内容卡改回 `secondaryContainer`，卡片才能在弹层里分辨出来。
 */
@Composable
private fun PresetCard(preset: CardOddsData.Preset, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左半：概率值，右半：主卡/副卡1~3/四叶草预览图。
            // 中间用 weight 吃掉剩余宽度，两侧 20dp 边距才完全对等。
            Text(
                text = preset.title,
                fontSize = MiuixTheme.textStyles.body1.fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PresetPreview(starDrawable(preset.main))
                PresetPreview(starDrawable(preset.sub1))
                PresetPreview(starDrawable(preset.sub2))
                PresetPreview(starDrawable(preset.sub3))
                PresetPreview(cloverDrawable(preset.clover))
            }
        }
    }
}

@Composable
private fun PresetPreview(id: String) {
    PackImage(
        id = id,
        modifier = Modifier.size(32.dp),
        contentScale = ContentScale.Fit,
    )
}

/**
 * 按 Java 版的 recalculate 逐行移植：MAIN 定行、SUB 定列、副卡类型定表取出三路概率，
 * 合并成结果1（超过 100% 直接取 100%）→ 乘四叶草倍率并再次钳制 → 叠加 VIP 与公会加成得到结果2；
 * 三段显示值按字符串相加保证画面上的 A+B=C，保底按 120% 规则换算成次数。
 */
private fun computeCardOdds(selection: CardOddsSelection): CardOddsResult {
    val subSelected = intArrayOf(selection.sub1, selection.sub2, selection.sub3)
    val categories = intArrayOf(
        selection.sub1Category,
        selection.sub2Category,
        selection.sub3Category,
    )
    // 三路取数：任一依赖未选中时该路返回 NaN（不参与合并）
    val values = DoubleArray(subSelected.size) { index ->
        fetchOdds(selection.main, subSelected[index], categories[index])
    }

    var result1 = mergeValues(values, subSelected)
    if (result1 > 1.0) result1 = 1.0

    if (selection.clover in CardOddsData.FourLeafClover.indices) {
        result1 *= CardOddsData.FourLeafClover[selection.clover]
    }
    if (result1 > 1.0) result1 = 1.0

    var bonus = 0.0
    if (selection.vip in CardOddsData.Vip.indices) {
        bonus += CardOddsData.Vip[selection.vip]
    }
    if (selection.consortia in CardOddsData.Consortia.indices) {
        bonus += CardOddsData.Consortia[selection.consortia]
    }
    val result2 = result1 * bonus

    val percentA = formatPercent(result1)
    val percentB = formatPercent(result2)
    // 用 A、B 的显示值相加，保证画面上的 A+B 恒等于 C
    val percentC = BigDecimal(percentA).add(BigDecimal(percentB)).toPlainString()

    val hasMainCost = selection.main >= 0 &&
        selection.main < CardOddsData.Coin.size &&
        selection.main < CardOddsData.Insurance.size

    return CardOddsResult(
        percentA = percentA,
        percentB = percentB,
        percentC = percentC,
        guarantee = guaranteeText(percentA, percentC),
        coin = if (hasMainCost) CardOddsData.Coin[selection.main] else 0,
        insurance = if (hasMainCost) CardOddsData.Insurance[selection.main] else 0,
    )
}

/** 单路取数：主卡定行、副卡定列、类型定表；任一未选中或越界返回 NaN。 */
private fun fetchOdds(main: Int, sub: Int, category: Int): Double {
    if (main < 0 || sub < 0 || category < 0) return Double.NaN
    if (category >= CardOddsData.OddsTables.size) return Double.NaN
    val table = CardOddsData.OddsTables[category]
    if (main >= table.size || sub >= table[main].size) return Double.NaN
    return table[main][sub]
}

/**
 * 合并三路数据（NaN 为该路未取到，不参与）：
 * 只取到 1 个时它就是结果1；取到 2~3 个时，副卡下标最高且最先出现的那个保持原值，
 * 其余 ÷3 后求和；三路都没取到时为 0。
 */
private fun mergeValues(values: DoubleArray, subSelected: IntArray): Double {
    var firstMaxIndex = -1
    var maxSub = Int.MIN_VALUE
    values.forEachIndexed { index, value ->
        if (value.isNaN()) return@forEachIndexed
        // 仅严格更大才更新：并列最高时保留最先出现的那个
        if (firstMaxIndex < 0 || subSelected[index] > maxSub) {
            maxSub = subSelected[index]
            firstMaxIndex = index
        }
    }
    if (firstMaxIndex < 0) return 0.0

    var sum = 0.0
    values.forEachIndexed { index, value ->
        if (value.isNaN()) return@forEachIndexed
        sum += if (index == firstMaxIndex) value else value / 3
    }
    return sum
}

/** 百分比格式化：×100 后四舍五入保留两位小数（0.484 → 48.40）。 */
private fun formatPercent(value: Double): String =
    BigDecimal.valueOf(value)
        .multiply(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()

/**
 * 保底文案：成功率 A 达到 100% 时“本次必成”；合计为 0 时显示“—”；
 * 其余情况求首个严格超过 120% 的倍数 n，n+1 即保底次数。
 */
private fun guaranteeText(percentA: String, percentC: String): String {
    if (BigDecimal(percentA).compareTo(BigDecimal("100")) == 0) return "本次必成"
    val cValue = BigDecimal(percentC)
    if (cValue.compareTo(BigDecimal.ZERO) == 0) return "—"
    // 120 / percentC 向下取整 + 1 = 首个严格超过 120 的倍数（整除时 +1 保证 > 而非 =）
    val multiplier = BigDecimal.valueOf(120)
        .divide(cValue, 0, RoundingMode.DOWN)
        .add(BigDecimal.ONE)
        .toLong()
    return "${multiplier + 1}次"
}

/** 预设里的类型字段越界（含缺失的 -1）时回退“好卡”，与 Java 版 setLogic 的默认值一致。 */
private fun sanitizeCategory(value: Int): Int = if (value in 0..2) value else 2
