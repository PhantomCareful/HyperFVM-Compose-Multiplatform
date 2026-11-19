package com.careful.hyperfvm.compose.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import com.careful.hyperfvm.compose.platform.isDesktopPlatform
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import com.careful.hyperfvm.compose.ui.GlassMenuHost
import com.careful.hyperfvm.compose.ui.GlassMenuHostState
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.LocalGlassMenuHost
import com.careful.hyperfvm.compose.ui.LocalRailTitleAlpha
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
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
 * 顶栏高度（含 bottomContent）会算进内容区的 top padding。柔光玻璃下由
 * [GlassTopAppBar] 自己把渐进模糊带向下外延盖过它。
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
    // 柔光玻璃弹出菜单的根宿主：它摆在取样层外面，面板才敢取样页面背板。
    val glassMenuHost = remember { GlassMenuHostState() }
    // 回弹位移写给顶栏状态：越界回弹把内容推得碰进顶栏时，模糊带与按钮浮起也要跟着出现，
    // 而 GlassTopAppBar 的弹性材质也读这个字段。
    // 回调必须在组合之间保持同一实例：overScrollVertical 检测到引用变化会先清零点再回放，
    // 每次重组换新 lambda 会让顶栏状态反复抖动。
    val onOverscroll = remember(scrollBehavior) {
        { offset: Float -> scrollBehavior.state.overscrollOffset = offset }
    }

    CompositionLocalProvider(
        LocalBlurBackdrop provides backdrop,
        LocalGlassMenuHost provides glassMenuHost,
    ) {
        // 玻璃菜单宿主包在 Scaffold **外面**：miuix 的 Scaffold 把 topBar 画在 content
        // 之上，宿主若落在 content 里，面板会被标题、搜索框盖住。这里让面板与整块
        // Scaffold 平级、后绘制，才能浮在顶栏之上。
        Box(modifier = Modifier.fillMaxSize()) {
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
                            // 位移同时存进 scrollBehavior.state.overscrollOffset：顶栏要知道内容是否
                            // 被回弹推进了顶栏区域。
                            .overScrollVertical(onOffsetChanged = onOverscroll)
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
            // 玻璃菜单宿主与内容取样层是兄弟节点：面板取样页面背板不会录到自己。
            GlassMenuHost(glassMenuHost)
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
    // 主题风格决定顶栏形态，不再允许混搭：柔光玻璃 = OS4 Glass 渐进模糊顶栏，
    // 轻透磨砂 = 高斯模糊顶栏（模糊关掉时退化为实色）。
    val glass = settings.isGlassStyle
    // 模糊生效的前提：设置里开着，且取样层可用（RuntimeShader 设备）。
    // 集中成这一个可空值，下面两个分支直接用，不再各自拼条件。
    val blurBackdrop = backdrop?.takeIf { settings.enableBlur }

    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            if (glass) {
                GlassIconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                    )
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                    )
                }
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
            AppTopBarIconButton(
                onClick = { if (!refreshing) refresh() },
                enabled = !refreshing,
            ) {
                if (refreshing) {
                    // 取和旁边图标同一个内容色，深浅色主题下才不会突然变成一块灰。
                    InfiniteProgressIndicator(color = LocalContentColor.current)
                } else {
                    Icon(imageVector = MiuixIcons.Refresh, contentDescription = "刷新")
                }
            }
        }
    }
    // 柔光玻璃的按钮是带圆形背板的玻璃圆钮，紧贴会连成一条胶囊；
    // 按 demo 的做法在相邻两个按钮之间留一小段空隙。
    val spacedActions: @Composable RowScope.() -> Unit = if (glass) {
        {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GlassActionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                barActions()
            }
        }
    } else {
        barActions
    }

    // 主题风格切换时顶栏整体淡入淡出，不要瞬间换形。
    Crossfade(
        targetState = glass,
        animationSpec = tween(ThemeStyleFadeMillis),
        label = "topBarStyle",
    ) { glassStyle ->
        if (glassStyle) {
            // 柔光玻璃顶栏自带三层：滚动后浮现的渐进模糊带、浮起后向玻璃胶囊过渡的按钮、
            // 以及折叠时标题的模糊过渡。backdrop 为 null（模糊关闭 / 设备不支持 RuntimeShader）
            // 时它自己退到半透明实色渐变带，不需要我们再垫分支。
            //
            // 外面再垫一层常驻渐进模糊（demo BlurredBar 的做法，几何与内建带一致）：
            // 模糊区域比顶栏高出一截，渐变尾巴垂进内容区，顶栏底边不会再切出一条硬线。
            // 它不接滚动状态，内容不足一屏的页面顶栏也有材质；alpha 与内建带的进展互补，
            // 未滚动时它负责全部材质，内容进顶栏后它淡出、内建带淡入，交叉淡化不叠加。
            // 这一层只管模糊带：按钮的胶囊与阴影仍由 GlassTopAppBar 按真实滚动/回弹位移驱动，
            // 与列表长短无关——所有页面走同一条路径，短页面不会因为它而提前浮起。
            val glassBar: @Composable () -> Unit = {
                GlassTopAppBar(
                    title = title,
                    subtitle = subtitle,
                    expandable = !(isWide || compactTitle),
                    progressiveBlur = true,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    defaultWindowInsetsPadding = !isWide,
                    // 宽屏玻璃侧栏收起成胶囊后，胶囊就落在顶栏标题的位置上，
                    // 标题跟着展开进度淡出（默认 1f，窄屏与二级页不受影响）。
                    titleAlpha = LocalRailTitleAlpha.current,
                    navigationIcon = navigationIcon,
                    actions = spacedActions,
                    bottomContent = bottomContent ?: {},
                )
            }
            if (blurBackdrop != null) {
                GlassBarBackdropBlur(backdrop = blurBackdrop, scrollBehavior = scrollBehavior) {
                    glassBar()
                }
            } else {
                glassBar()
            }
        } else {
            FrostedTopAppBar(
                title = title,
                subtitle = subtitle,
                compactTitle = compactTitle,
                isWide = isWide,
                backdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = spacedActions,
                bottomContent = bottomContent,
            )
        }
    }
}

/**
 * 轻透磨砂的顶栏：大标题 [TopAppBar] / 小标题 [SmallTopAppBar]，模糊开启时垫一层高斯模糊。
 *
 * 抽成独立 composable 是因为要跟着主题风格淡入淡出：分支里不能再提前 return。
 */
@Composable
private fun FrostedTopAppBar(
    title: String,
    subtitle: String,
    compactTitle: Boolean,
    isWide: Boolean,
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    bottomContent: (@Composable () -> Unit)?,
) {
    val bar: @Composable (Color) -> Unit = { color ->
        if (isWide || compactTitle) {
            SmallTopAppBar(
                title = title,
                subtitle = subtitle,
                color = color,
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = false,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent ?: {},
            )
        } else {
            TopAppBar(
                title = title,
                subtitle = subtitle,
                color = color,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent ?: {},
            )
        }
    }

    if (backdrop == null) {
        bar(MiuixTheme.colorScheme.surface)
        return
    }

    // 轻透磨砂固定高斯模糊：整条顶栏（含 bottomContent）垫一层 25f 的 textureBlur。
    val surface = MiuixTheme.colorScheme.surface
    Box(
        modifier = Modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 25f,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(BlendColorEntry(color = surface.copy(0.8f))),
            ),
        ),
    ) {
        bar(Color.Transparent)
    }
}

/**
 * 主题风格切换时的淡入淡出时长。
 *
 * 顶栏、底栏、侧栏与顶栏下方的 TabRow 共用同一节奏：同一帧里换材质，整页才像一起过渡。
 */
internal const val ThemeStyleFadeMillis = 220

/** 柔光玻璃顶栏里相邻两个图标按钮的间距。 */
private val GlassActionSpacing = 8.dp

/**
 * 玻璃顶栏常驻渐进模糊的参数，取 [GlassTopAppBar] 内建带同款：
 * 80dp 半径与 0.5 的 curve（强端集中、长软尾），渐变到底部自然归零。
 */
private const val GlassBarBandBlurRadius = 80f

/**
 * 常驻渐进模糊带比顶栏多画出的高度：让渐变尾巴落到内容区上再收尾，顶栏底边没有硬线。
 *
 * 内建带的尾巴约 20% + 20dp（折叠后约 40dp），这里取同一量级。
 */
private val GlassBarBandOverhang = 40.dp

/**
 * 玻璃顶栏的常驻渐进模糊层，几何与参数对齐 [GlassTopAppBar] 内建的渐进带：
 * 模糊区域比顶栏高出一截（[GlassBarBandOverhang]），渐变尾巴垂进内容区自然消失，
 * 不会在顶栏底边切出硬线；强度（curve 0.5、半径 80dp、surface 0.3）也与内建带一致。
 *
 * alpha 与内建带的进展互补（1 - ramp）：未滚动时由这一层负责顶栏材质，内容进顶栏后
 * 这层淡出、内建带淡入，两者交叉淡化，不叠加成一片更浓的模糊。
 */
@Composable
private fun GlassBarBackdropBlur(
    backdrop: LayerBackdrop,
    scrollBehavior: ScrollBehavior,
    content: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val ramp = (-scrollBehavior.state.visualContentOffset /
                            GlassTopAppBarDefaults.ProgressiveBlurRampDistance.toPx()).coerceIn(0f, 1f)
                        alpha = 1f - ramp
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                        blurRadius = GlassBarBandBlurRadius,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.3f)),
                            ),
                        ),
                    ),
            )
            content()
        },
    ) { measurables, constraints ->
        val foreground = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        // 顶栏是全宽布局，测量结果已经满足父约束，直接用它自己的尺寸。
        val width = foreground.width
        val height = foreground.height
        // 尾巴伸到内容区上：内建带同样用超出顶栏高度的区域把渐变收尾。
        val background = measurables[0].measure(
            Constraints.fixed(width, height + GlassBarBandOverhang.roundToPx()),
        )
        layout(width, height) {
            background.placeRelative(0, 0)
            foreground.placeRelative(0, 0)
        }
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
