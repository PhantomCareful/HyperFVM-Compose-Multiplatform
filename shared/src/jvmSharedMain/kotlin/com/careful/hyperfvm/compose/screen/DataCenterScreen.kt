package com.careful.hyperfvm.compose.screen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.dialog.UrlTarget
import com.careful.hyperfvm.compose.component.dialog.VisitUrlDialog
import com.careful.hyperfvm.compose.component.image.PackImage
import com.careful.hyperfvm.compose.component.itemQuery.ItemQuerySheet
import com.careful.hyperfvm.compose.data.datacenter.DataCenterPreferences
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.screen.about.AboutContentCard
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.preference.ArrowPreference

/** 「美食数据站」的一张外链卡，对应 Java 版 fragment_data_center 里 card_* 的一条。 */
private data class DataStationEntry(
    val title: String,
    /** 作者署名，显示在标题下第一行。 */
    val author: String,
    /** 一句话简介，显示在作者下一行。 */
    val description: String,
    /** 左侧 50dp 数据图，按 [target] 的 circleIcon 决定是否裁成正圆。 */
    val image: String,
    /** 外链确认框的目标，弹窗大图按其 circleIcon 决定是否裁圆。 */
    val target: UrlTarget,
)

private val DataStationEntries = listOf(
    DataStationEntry(
        title = "提拉米鼠",
        author = "By 夏夜浅酌",
        description = "功能众多，简单易用的QQ机器人",
        image = "data_center_image_tiramisu",
        target = UrlTarget(
            title = "提拉米鼠使用指南",
            subtitle = null,
            url = "https://msdzls.cn/docs",
            imageId = "data_center_image_tiramisu",
            circleIcon = true,
        ),
    ),
    DataStationEntry(
        title = "综合数据表",
        author = "By So陌路の",
        description = "伟大无需多言",
        image = "data_center_image_molu",
        target = UrlTarget(
            title = "陌路の综合数据表",
            subtitle = null,
            url = "https://www.kdocs.cn/l/cmk63gHnvUdi",
            imageId = "data_center_image_molu",
            circleIcon = true,
        ),
    ),
    DataStationEntry(
        title = "FAA米苏物流",
        author = "By 夏夜浅酌",
        description = "FAA游戏数据统计分析平台",
        image = "data_center_image_faa",
        target = UrlTarget(
            title = "FAA米苏物流",
            subtitle = null,
            url = "https://faa.msdzls.cn",
            imageId = "data_center_image_faa",
            circleIcon = true,
        ),
    ),
    DataStationEntry(
        title = "轨道强卡统计",
        author = "By 夏夜浅酌",
        description = "由轨道执行器收集的强卡统计数据",
        image = "data_center_image_guidao",
        target = UrlTarget(
            title = "轨道强卡统计",
            subtitle = null,
            url = "https://card.mstrack.cn/",
            imageId = "data_center_image_guidao",
            circleIcon = false,
        ),
    ),
    DataStationEntry(
        title = "卡片鼠军对策表",
        author = "By 繁华洛尽、布鲁、陌路、式祈",
        description = "非常全面的游戏伤害机制详点说法",
        image = "data_center_image_strategy",
        target = UrlTarget(
            title = "卡片鼠军对策表",
            subtitle = null,
            url = "https://www.kdocs.cn/l/cku3hMogOycM",
            imageId = "data_center_image_strategy",
            circleIcon = false,
        ),
    ),
    DataStationEntry(
        title = "巅峰对决部分机制解析",
        author = "By 式祈",
        description = "段位数据、BOSS技能、BUFF机制",
        image = "dashboard_world_boss",
        target = UrlTarget(
            title = "巅峰对决部分机制解析",
            subtitle = null,
            url = "https://www.kdocs.cn/l/cll09ugqnrwG",
            imageId = "dashboard_world_boss",
            circleIcon = false,
        ),
    ),
    DataStationEntry(
        title = "强卡最优路径计算器",
        author = "By 夏夜浅酌",
        description = "为您提供相对更佳的上卡方案",
        image = "data_center_image_card_calculator",
        target = UrlTarget(
            title = "强卡最优路径计算器",
            subtitle = null,
            url = "https://app.msdzls.cn/card_op",
            imageId = "data_center_image_card_calculator",
            circleIcon = false,
        ),
    ),
    DataStationEntry(
        title = "宝石最优路径计算器",
        author = "By 夏夜浅酌",
        description = "为您提供相对更佳的宝石强化方案",
        image = "data_center_image_gem_calculator",
        target = UrlTarget(
            title = "宝石最优路径计算器",
            subtitle = null,
            url = "https://app.msdzls.cn/gem_co",
            imageId = "data_center_image_gem_calculator",
            circleIcon = false,
        ),
    ),
)

/** 数据中心「特色功能」的一张入口卡。 */
private data class FeatureEntry(
    /** 排序存储用的稳定 id；改名会让用户存过的顺序失效。 */
    val id: String,
    val title: String,
    val summary: String,
    /** 压栈目标；null 表示在页面内打开弹层（道具查询）。 */
    val route: Route?,
)

private val FeatureEntries = listOf(
    FeatureEntry("dataImages", "数据图合集", "目标是打造成最全面的数据图合集", Route.DataImages),
    FeatureEntry("cookery", "食神谱合集", "查询食神谱的详细信息", Route.CookeryIndex),
    FeatureEntry("odds", "概率公示合集", "展示官方已公示的宝箱与抽奖概率信息", Route.OddsDisclosure),
    FeatureEntry("clothing", "套装合集", "查看全部套装的预览图、体力与续费价格", Route.ClothingCollection),
    FeatureEntry("wing", "翅膀合集", "查看全部翅膀的预览图、体力与续费价格", Route.WingCollection),
    FeatureEntry("itemQuery", "道具查询", "查询游戏内道具的描述和作用", null),
)

/**
 * 数据中心一级页：「特色功能」与「美食数据站」两个版块，对应 Java 版
 * `fragment_data_center.xml` 的 `title_data_center_special_features` 与
 * `title_data_center_data_station` 分组，顺序也保持一致。
 *
 * 特色功能目前有数据图合集、食神谱合集、概率公示合集、套装合集、翅膀合集与道具查询六个入口，
 * 前五个点击直接压栈，道具查询是弹层；入口可长按拖动调整顺序，顺序存在本地。
 * 美食数据站都是外链，点击先弹 [VisitUrlDialog]，确认后才调起浏览器。
 *
 * 顶栏右侧是查黑系统的入口（Java 版在 Fragment 右上角的悬浮按钮），
 * 点击压栈到名单页 [Route.FraudList]；详情页与搜索都在那一页里完成。
 */
@Composable
fun DataCenterScreen(outerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val env = LocalAppEnv.current
    val haptic = LocalHapticFeedback.current
    // 弹窗状态提到列表外面，才能保证列表项被回收时弹窗不被一起销毁。
    var pendingUrl by remember { mutableStateOf<UrlTarget?>(null) }
    var showItemQuery by remember { mutableStateOf(false) }

    // 特色功能的顺序：存过的在前，代码里新增、还没存过的入口按默认顺序接在末尾。
    val preferences = remember(env) { DataCenterPreferences(env) }
    var featureOrder by remember {
        mutableStateOf(
            preferences.readOrder()
                .filter { id -> FeatureEntries.any { it.id == id } }
                .distinct()
                .let { saved -> saved + FeatureEntries.map { it.id }.filterNot { id -> id in saved } },
        )
    }
    val features = remember(featureOrder) {
        val byId = FeatureEntries.associateBy { it.id }
        featureOrder.mapNotNull { byId[it] }
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = (from.key as? String)?.let(featureOrder::indexOf) ?: -1
        val toIndex = (to.key as? String)?.let(featureOrder::indexOf) ?: -1
        // 拖到「美食数据站」、分节标题这类非入口项时忽略，卡片会自己回位。
        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
            featureOrder = featureOrder.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    ScreenScaffold(
        title = "数据中心",
        outerPadding = outerPadding,
        actions = {
            AppTopBarIconButton(onClick = { navigator.push(Route.FraudList) }) {
                Icon(imageVector = MiuixIcons.Blocklist, contentDescription = "查黑系统")
            }
        },
    ) { padding ->
        LazyColumn(
            // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，滚动与顶栏折叠才会响应。
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            item(key = "specialFeaturesTitle") {
                SmallTitle("特色功能")
            }
            // 入口支持长按拖动排序：整张卡都是手柄，松手后顺序落盘。
            items(items = features, key = { it.id }) { feature ->
                ReorderableItem(state = reorderableState, key = feature.id) { _ ->
                    val interactionSource = remember { MutableInteractionSource() }
                    AboutContentCard(
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = { preferences.saveOrder(featureOrder) },
                            interactionSource = interactionSource,
                        ),
                    ) {
                        ArrowPreference(
                            title = feature.title,
                            summary = feature.summary,
                            onClick = {
                                val route = feature.route
                                if (route != null) navigator.push(route) else showItemQuery = true
                            },
                        )
                    }
                }
            }
            item(key = "dataStation") {
                SmallTitle("美食数据站")
                // 与 Java 版一致：每个入口单独一张卡，卡间距由 AboutContentCard 的 bottom 12dp 提供。
                DataStationEntries.forEach { entry ->
                    AboutContentCard {
                        ArrowPreference(
                            title = entry.title,
                            summary = entry.author + "\n" + entry.description,
                            startAction = {
                                PackImage(
                                    id = entry.image,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(50.dp)
                                        .then(if (entry.target.circleIcon) Modifier.clip(CircleShape) else Modifier),
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onClick = { pendingUrl = entry.target },
                        )
                    }
                }
            }
        }

        VisitUrlDialog(
            target = pendingUrl,
            onDismiss = { pendingUrl = null },
        )

        ItemQuerySheet(
            show = showItemQuery,
            onDismissRequest = { showItemQuery = false },
        )
    }
}
