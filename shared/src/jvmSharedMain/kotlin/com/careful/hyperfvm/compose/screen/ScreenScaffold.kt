package com.careful.hyperfvm.compose.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.platform.isDesktopPlatform
import com.careful.hyperfvm.compose.platform.rememberStatusBarHeightPx
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.isWideLayout
import com.careful.hyperfvm.compose.ui.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

/** 顶栏模糊在滚动多少像素后完全显现。 */
private val BlurRevealDistance = 48.dp

/**
 * 渐进模糊静止时的保底可见度：内容不满一屏时顶栏永远收不起来（contentOffset 恒 0），
 * 没有保底的话短页面上渐进模糊会整个透明；滚动到位后仍会拉满到 1。
 */
private const val ProgressiveBaseAlpha = 1.0f

/**
 * 每个一级页面的通用脚手架：顶栏（带模糊）+ 内容区。
 *
 * [outerPadding] 由外层 Pager 传入，只取它的 bottom（底部导航占用的高度）；
 * 顶栏与状态栏的安全区由内层 Scaffold 自己算。
 *
 * [onBack] 非 null 时顶栏左侧渲染返回按钮（二级页面用）。
 *
 * [navigationIcon] 是返回按钮之外的自定义左上角槽位（如仪表盘的收件箱入口）；
 * 两者都给时返回按钮优先——它承载的是「退出当前页」，不该被别的东西顶掉。
 *
 * [compactTitle] 为 true 时顶栏固定用小标题样式：内容不滚动的页面用，
 * 大标题永远不会折叠成小标题，直接用收起后的形态。
 *
 * [scrollBehavior] 默认由脚手架自己创建；需要程序化滚动时（如目录菜单跳转）
 * 可由调用方传入同一实例，手动同步顶栏的折叠状态。
 *
 * [attachScrollBehavior] 为 false 时脚手架不再自己挂顶栏的 nested scroll 连接，
 * 改由内容里的 PullToRefresh 接管：它需要在 post 阶段先展开顶栏、再把剩余的下拉量
 * 交给刷新指示器。两边都挂同一个 behavior 会让折叠速度翻倍。
 *
 * [onRefresh] 与 [refreshing] 只在桌面端生效：miuix 的手势会话只认触摸，鼠标拖不出
 * 下拉刷新，所以桌面端把刷新入口挂到顶栏右侧的按钮上。刷新中按钮换成细环并禁用。
 *
 * [bottomContent] 挂在顶栏标题下方、属于顶栏的一部分：它会跟着大标题一起折叠，
 * 并且同样落在顶栏的模糊层里（搜索框这类需要悬浮感又不失去模糊的控件放这里）。
 * 顶栏高度（含 bottomContent）会算进内容区的 top padding。渐进模糊下，传了
 * [bottomContent] 的顶栏会把模糊带向下外延盖过它；只有标题的顶栏用原版高度。
 *
 * [overlay] 是内容区上方的悬浮层，但**不参与越界回弹位移**：overScrollVertical
 * 会用 placeWithLayer 平移内容，需要绝对固定位置的控件（如目录页的字母索引条）
 * 必须放在这里，否则回弹时会被一起拖动。
 *
 * [snackbarHost] 挂在 Scaffold 底部的提示宿主：带 Action 的 Snackbar 走这里，
 * miuix 会把它抬到底栏 / 导航栏安全区之上。
 *
 * [floatingActionButton] 是右下角的悬浮按钮槽位：由 miuix Scaffold 摆放，
 * 自带底边距与导航栏安全区避让，位置不随越界回弹移动。
 */
@Composable
fun ScreenScaffold(
    title: String,
    outerPadding: PaddingValues,
    subtitle: String = "",
    compactTitle: Boolean = false,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    scrollBehavior: ScrollBehavior = MiuixScrollBehavior(),
    attachScrollBehavior: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    bottomContent: (@Composable () -> Unit)? = null,
    overlay: @Composable BoxScope.(PaddingValues) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val settings = LocalAppSettings.current
    // 顶栏的模糊取样层只包内容区；顶栏本身留在 topBar 槽里，与它是兄弟关系。
    // 两者一旦变成父子，记录中的图层就会引用自己，RenderThread 栈溢出。
    val backdrop = rememberAppBlurBackdrop()

    CompositionLocalProvider(LocalBlurBackdrop provides backdrop) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                BlurredTopAppBar(
                    title = title,
                    subtitle = subtitle,
                    compactTitle = compactTitle,
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                    leadingIcon = navigationIcon,
                    actions = actions,
                    onRefresh = onRefresh,
                    refreshing = refreshing,
                    bottomContent = bottomContent,
                )
            },
            snackbarHost = snackbarHost,
            floatingActionButton = { floatingActionButton?.invoke() },
        ) { innerPadding ->
            val padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                // 一级页由外层 Pager 报底栏高度；二级页全屏，自己吃 Scaffold 的底部安全区。
                bottom = maxOf(
                    innerPadding.calculateBottomPadding(),
                    outerPadding.calculateBottomPadding(),
                ),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 取样层放在回弹位移的外层：overScrollVertical 用 placeWithLayer 平移内容，
                        // 记录层要拍到位移后的画面，顶栏采样才不会与内容错位。
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                        // 下面三个连接点的顺序对齐 demo 的 pageScrollModifiers：
                        // 震动 → 越界回弹 → 顶栏折叠。
                        // 震动靠 onPostFling 判定边界，必须能收到子级没消费掉的剩余速度。
                        .then(if (settings.scrollEndHaptic) Modifier.scrollEndHaptic(HapticFeedbackType.TextHandleMove) else Modifier)
                        // overScrollVertical 是内容不满一屏时唯一的回弹来源：Compose 的 scrollable 在
                        // canScrollForward/Backward 都为 false 时会跳过 overscrollEffect.applyToScroll，
                        // 但 nested scroll 仍照常派发，普通 NestedScrollConnection 能接住剩余 delta。
                        .overScrollVertical()
                        .then(
                            if (attachScrollBehavior) {
                                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    content(padding)
                }
                // 回弹位移之外：这里的控件位置绝对固定，不会被 overScrollVertical 拖动。
                overlay(padding)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlurredTopAppBar(
    title: String,
    subtitle: String,
    compactTitle: Boolean,
    scrollBehavior: ScrollBehavior,
    onBack: (() -> Unit)?,
    leadingIcon: (@Composable () -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    onRefresh: (() -> Unit)?,
    refreshing: Boolean,
    bottomContent: (@Composable () -> Unit)?,
) {
    val settings = LocalAppSettings.current
    val backdrop = LocalBlurBackdrop.current
    val isWide = isWideLayout()
    val blurActive = settings.enableBlur && backdrop != null
    val progressive = settings.blurStyle == 1
    val hasBottomContent = bottomContent != null
    // 状态栏真实高度（含沉浸态）；桌面端恒为 0。测距阶段要用 px，所以提前取好。
    val statusBarHeightPx = rememberStatusBarHeightPx()

    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                )
            }
        } else if (leadingIcon != null) {
            leadingIcon()
        }
    }

    // 桌面端补一个刷新入口：miuix 的下拉手势只认触摸，鼠标拖不出刷新。
    // 刷新中换成细环并禁用，避免重复触发。
    val refresh = onRefresh
    val barActions: @Composable RowScope.() -> Unit = {
        actions()
        if (isDesktopPlatform && refresh != null) {
            IconButton(onClick = refresh, enabled = !refreshing) {
                if (refreshing) {
                    // 取和旁边图标同一个内容色，深浅色主题下才不会突然变成一块灰。
                    InfiniteProgressIndicator(color = LocalContentColor.current)
                } else {
                    Icon(imageVector = MiuixIcons.Refresh, contentDescription = "刷新")
                }
            }
        }
    }

    val bar: @Composable (Color) -> Unit = { color ->
        if (isWide || compactTitle) {
            SmallTopAppBar(
                title = title,
                subtitle = subtitle,
                color = color,
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = false,
                navigationIcon = navigationIcon,
                actions = barActions,
                bottomContent = bottomContent ?: {},
            )
        } else {
            TopAppBar(
                title = title,
                subtitle = subtitle,
                color = color,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = barActions,
                bottomContent = bottomContent ?: {},
            )
        }
    }

    if (!blurActive) {
        bar(MiuixTheme.colorScheme.surface)
        return
    }

    val surface = MiuixTheme.colorScheme.surface
    Box(
        modifier = if (progressive) {
            Modifier
        } else {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(BlendColorEntry(color = surface.copy(0.8f))),
                ),
            )
        },
    ) {
        if (progressive) {
            // 渐进模糊：曲线 2.2、radius 10f 都是原版 miuix 参数（官方 example 同款）——
            // 顶部强、往下保持得远、末端才收尾；radius 的降采样档低，无果粒（Melox 的
            // 80f 会落到 1/8~1/16 档，画面放大回来就是大果粒）。
            // 带高分两档：只有标题的顶栏用原版高度（matchParentSize 即 bar 全高）；挂了
            // bottomContent（TabRow、搜索框）时向下外延 fadeReach，把带拉高后组件位置的
            // raw 变小、仍处在衰减途中，模糊范围就从标题延伸到了组件，组件因此只做
            // 透明底、不自己垫 textureBlur。
            val bandHeight =
                if (hasBottomContent) {
                    Modifier.layout { measurable, constraints ->
                        val collapsedHeight = statusBarHeightPx + TopAppBarDefaults.CollapsedHeight.toPx()
                        val fadeReach = ((collapsedHeight + 20.dp.toPx()) * 1.2f - collapsedHeight).roundToInt()
                        val height = (constraints.maxHeight + fadeReach).coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        // 外层尺寸仍报 bar 高度，placeable 向 bar 底部外延绘制。
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                } else {
                    Modifier
                }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(bandHeight)
                    .graphicsLayer {
                        val revealed = (-scrollBehavior.state.contentOffset / BlurRevealDistance.toPx())
                            .coerceIn(0f, 1f)
                        alpha = ProgressiveBaseAlpha + (1f - ProgressiveBaseAlpha) * revealed
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(BlendColorEntry(color = surface.copy(0.3f))),
                        ),
                    ),
            )
        }
        bar(Color.Transparent)
    }
}

/**
 * 空界面占位内容。等各页面接入真实内容后逐个替换。
 */
@Composable
fun PlaceholderContent(
    padding: PaddingValues,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            textAlign = TextAlign.Center,
        )
    }
}
