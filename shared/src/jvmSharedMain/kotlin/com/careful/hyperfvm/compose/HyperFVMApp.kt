package com.careful.hyperfvm.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerHostOverlay
import com.careful.hyperfvm.compose.component.liquid.IosLiquidGlassNavigationBar
import com.careful.hyperfvm.compose.data.datapack.DataPackRepository
import com.careful.hyperfvm.compose.data.datapack.DataPackState
import com.careful.hyperfvm.compose.screen.datapack.DataPackSetupScreen
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.Toaster
import com.careful.hyperfvm.compose.data.cardCatalog.CardCatalog
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.LocalUpdateSettings
import com.careful.hyperfvm.compose.data.settings.SettingsStore
import com.careful.hyperfvm.compose.data.settings.isBouncyGlassBar
import com.careful.hyperfvm.compose.data.settings.isLiquidGlassBar
import com.careful.hyperfvm.compose.navigation.CrossActivityTransition
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.MainTab
import com.careful.hyperfvm.compose.navigation.NavBackStackSaver
import com.careful.hyperfvm.compose.navigation.Navigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.screen.about.AboutScreen
import com.careful.hyperfvm.compose.screen.about.CoContributorTeamScreen
import com.careful.hyperfvm.compose.screen.about.ThanksAppScreen
import com.careful.hyperfvm.compose.screen.about.ThanksGameScreen
import com.careful.hyperfvm.compose.screen.announcement.AnnouncementArchiveScreen
import com.careful.hyperfvm.compose.screen.announcement.AnnouncementScreen
import com.careful.hyperfvm.compose.screen.DashboardScreen
import com.careful.hyperfvm.compose.screen.DataCenterScreen
import com.careful.hyperfvm.compose.screen.DefenseCardScreen
import com.careful.hyperfvm.compose.screen.settings.PermissionScreen
import com.careful.hyperfvm.compose.screen.SettingsScreen
import com.careful.hyperfvm.compose.screen.ToolsScreen
import com.careful.hyperfvm.compose.screen.tools.CardOddsCalculatorScreen
import com.careful.hyperfvm.compose.screen.tools.ResourceCalculatorScreen
import com.careful.hyperfvm.compose.screen.tools.TodayLuckyScreen
import com.careful.hyperfvm.compose.screen.dataCenter.dataImages.DataImagesScreen
import com.careful.hyperfvm.compose.screen.dataCenter.clothing.ClothingCollectionScreen
import com.careful.hyperfvm.compose.screen.dataCenter.wings.WingCollectionScreen
import com.careful.hyperfvm.compose.screen.dataCenter.cookery.CookeryIndexScreen
import com.careful.hyperfvm.compose.screen.dataCenter.fraud.FraudListScreen
import com.careful.hyperfvm.compose.screen.dataCenter.fraud.IcuFraudScreen
import com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure.LotteryOddsInfoScreen
import com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure.OddsDisclosureIndexScreen
import com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure.OddsDisclosureInfoScreen
import com.careful.hyperfvm.compose.screen.meishi.MeishiWechatScreen
import com.careful.hyperfvm.compose.screen.notification.NotificationInboxScreen
import com.careful.hyperfvm.compose.ui.isWideLayout
import com.careful.hyperfvm.compose.ui.rememberAppBlurBackdrop
import com.careful.hyperfvm.compose.ui.shouldExpandNavigationRail
import com.careful.hyperfvm.compose.ui.theme.HyperFVMTheme
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import com.careful.hyperfvm.compose.ui.theme.keyColorFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.navBackStackOf
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** 悬浮导航栏「玻璃」样式使用的圆角，比默认胶囊更圆润。 */
private val GlassBarCornerRadius = 32.dp

/**
 * 应用主题与根容器。
 *
 * [pendingRoute] 是外部入口（目前是下载通知）发起的导航请求：不为空时直接把对应页面
 * 压上返回栈，随后置回 null 消费掉。
 *
 * 提示统一走 [LocalToaster]：桌面端是 miuix Snackbar，Android 端直接弹 Toast。
 */
@Composable
fun HyperFVMApp(env: AppEnv, store: SettingsStore, pendingRoute: MutableStateFlow<Route?>) {
    val settings = store.settings
    // 界面缩放：只乘在 density.density 上，fontScale 保持系统的值。
    // 这样 dp 和 sp 一起缩放，而系统「字体大小」设置依旧叠加在上面。
    val systemDensity = LocalDensity.current
    val scaledDensity = remember(systemDensity, settings.uiScale) {
        Density(systemDensity.density * settings.uiScale, systemDensity.fontScale)
    }
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toaster = remember(env, snackbarState, scope) {
        if (env.capabilities.snackbarHints) {
            // 桌面端没有 Toast，用悬浮在底部的 Snackbar 代替。
            Toaster { message -> scope.launch { snackbarState.showSnackbar(message) } }
        } else {
            Toaster { message -> env.showToast(message) }
        }
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        HyperFVMTheme(
            colorMode = settings.colorMode,
            keyColor = keyColorFor(settings.keyColorIndex),
            paletteStyle = settings.paletteStyle,
            colorSpec = settings.colorSpec,
        ) {
            CompositionLocalProvider(
                LocalAppSettings provides settings,
                LocalUpdateSettings provides store::update,
                LocalAppEnv provides env,
                LocalToaster provides toaster,
                // 超椭圆轮廓固定启用，不提供开关。运行时着色器不支持时 miuix 会自己回退到普通圆角。
                LocalSquircleEnabled provides true,
            ) {
                // 所有图片与概率公示数据都在数据包里。正式启动前先同步摸底：本地已装包
                // 就是 Ready，主界面首帧直接出，不会先闪一下「正在检查」；没装才是 Missing，
                // 由引导页接管整个界面。下载与索引建立继续交给 check 在后台跑。
                remember(env) { DataPackRepository.prime(env) }
                val packState by DataPackRepository.state.collectAsState()
                LaunchedEffect(Unit) { DataPackRepository.check(env) }
                // 只有本地缺包时才停在引导页；数据包更新走主界面里的二级页（Route.DataPackUpdate），
                // 下载期间的 Downloading / Preparing 状态不会把整页门禁重新拉起来。
                // 首次安装后停在「一切就绪」，用户点按钮才进主界面并播整页转场。
                val gateInitially = remember(env) { !DataPackRepository.isInstalled(env) }
                var gateActive by remember { mutableStateOf(gateInitially) }
                PackGate(
                    state = packState,
                    showMain = !gateActive,
                    animateEntry = gateInitially,
                    onDownload = { scope.launch { DataPackRepository.download(env) } },
                    onEnter = { gateActive = false },
                ) { modifier ->
                    Box(modifier) {
                        MainFrame(pendingRoute)
                        // Snackbar 宿主放在导航宿主之上：二级页面与图查看器展开时提示依旧可见。
                        // 底部留出一级页面的悬浮底栏高度，避免提示被遮住。
                        SnackbarHost(
                            state = snackbarState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(bottom = if (env.capabilities.snackbarHints) 92.dp else 0.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 数据包引导页 ⇄ 主界面。
 *
 * 用户点「进入」时的整页转场直接借 [NavDisplay] 的那套 [NavTransition]（设置里选的
 * miuix 滑动 / 跨活动两档），只是自己驱动一条 0→1 的栈顶深度：引导页是被覆盖的一层
 * （深度 top），主界面是从右侧压上来的一层（深度 top - 1），几何公式与二级页面 push
 * 完全一致，手感不会有出入。
 *
 * 日常启动（本次会话没出现过引导页）不播转场，直接换内容。
 */
@Composable
private fun PackGate(
    state: DataPackState,
    showMain: Boolean,
    animateEntry: Boolean,
    onDownload: () -> Unit,
    onEnter: () -> Unit,
    main: @Composable (Modifier) -> Unit,
) {
    val settings = LocalAppSettings.current
    val navTransition = if (settings.navTransitionStyle == 1) CrossActivityTransition else NavTransitions.MiuixDefault
    // 时长与曲线跟着这套转场自己的 motion 走（miuix 是短补间，跨活动是 450ms 的曲线）。
    val motion = remember(navTransition) { navTransition.motion.programmatic.toAnimationSpec() }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val layoutSize = LocalWindowInfo.current.containerSize

    // 0 = 引导页在栈顶，1 = 主界面在栈顶，与 NavDisplay 内部的 relativeDepth 同一口径。
    val top = remember { Animatable(0f) }
    var entryDone by remember { mutableStateOf(false) }
    LaunchedEffect(showMain) {
        if (!showMain || !animateEntry) return@LaunchedEffect
        top.animateTo(1f, motion)
        entryDone = true
    }

    // 引导页：被覆盖的一层，转场结束才撤掉，免得它的动态背景在后台继续跑。
    val setupScope = remember(navTransition, layoutDirection, density, layoutSize) {
        PackGateScope(
            depth = { top.value },
            roleOf = { d -> if (d > 0f) NavRole.Covered else NavRole.Top },
            layoutSize = layoutSize,
            layoutDirection = layoutDirection,
            density = density,
        )
    }
    // 主界面：压上来的一层。
    val mainScope = remember(navTransition, layoutDirection, density, layoutSize) {
        PackGateScope(
            depth = { top.value - 1f },
            roleOf = { d -> if (d < 0f) NavRole.Incoming else NavRole.Top },
            layoutSize = layoutSize,
            layoutDirection = layoutDirection,
            density = density,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 先画引导页，主界面后画：转场时压上来的一层盖在它上面。
        // 只有真的需要引导页（门禁未解除）或「点进入」的转场还没播完时才继续组合它，
        // 日常启动不经过引导页，不会在背后白跑一层动态背景。
        val setupVisible = !showMain || (animateEntry && !entryDone)
        if (setupVisible) {
            Box(modifier = with(navTransition) { Modifier.fillMaxSize().transformEntry(setupScope) }) {
                DataPackSetupScreen(
                    state = state,
                    onDownload = onDownload,
                    onEnter = onEnter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (showMain) {
            val modifier = if (animateEntry) {
                with(navTransition) { Modifier.fillMaxSize().transformEntry(mainScope) }
            } else {
                Modifier.fillMaxSize()
            }
            main(modifier)
        }
    }
}

/**
 * 把 [NavTransition] 从 NavDisplay 里借到普通布局中用：它只读 scope 上的深度和手势上下文，
 * 这里给一条由动画驱动的深度，其余按「一次到顶的 push」填默认值。
 */
private class PackGateScope(
    private val depth: () -> Float,
    private val roleOf: (Float) -> NavRole,
    override val layoutSize: IntSize,
    override val layoutDirection: LayoutDirection,
    override val density: Density,
) : NavTransitionScope {
    override val relativeDepth: Float get() = depth()
    override val role: NavRole get() = roleOf(relativeDepth)
    override val change: NavChange get() = NavChange.Push
    override val gesture: NavGesture? get() = null
}

/** NavMotion 给 settle 用的参数和 Compose 的 AnimationSpec 是同一套语义，直接换算。 */
private fun NavSettleSpec.toAnimationSpec(): AnimationSpec<Float> = when (this) {
    is NavSettleSpec.Spring -> spring(dampingRatio = dampingRatio, stiffness = stiffness)
    is NavSettleSpec.Tween -> tween(durationMillis = durationMillis, easing = easing)
}

/**
 * 应用根：只负责导航宿主。五个一级界面在 [Route.Main] 里由 Pager 承载，
 * 二级页面（如 [Route.About]）压在它上面，转场样式由设置决定。
 */
@Composable
private fun MainFrame(pendingRoute: MutableStateFlow<Route?>) {
    val settings = LocalAppSettings.current
    val env = LocalAppEnv.current
    val scope = rememberCoroutineScope()
    // 下载 / 准备期间锁住更新页的返回。用去重后的布尔流，不跟着进度字节重组整棵树；
    // 页面内部的进度状态由更新页自己收集。
    val updateLocked by DataPackRepository.running.collectAsState(initial = false)

    // 防御卡目录的拼音分节是静态数据：启动时先在后台算好，首次切到
    // 「防御卡」Tab 时就不用再等 ICU 转写（373 张卡约一千个名字）。
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) { CardCatalog.sections }
    }
    // 用 rememberSaveable 而不是 remember：Activity 因配置变更重建（旋转、字号、语言）
    // 或进程被系统回收后，返回栈能从 Bundle 恢复，不会莫名掉回主界面。
    val backStack = rememberSaveable(saver = NavBackStackSaver) { navBackStackOf(Route.Main) }
    val navigator = remember(backStack) { Navigator(backStack) }
    // 外部入口指定的页面：入栈后立刻消费掉，重组或旋转都不会让它再跳一次。
    val requestedRoute by pendingRoute.collectAsState()
    LaunchedEffect(requestedRoute) {
        val route = requestedRoute ?: return@LaunchedEffect
        navigator.push(route)
        pendingRoute.value = null
    }
    // 从受支持的链接进来：由独立的透明中转页（MeishiLinkReceiverActivity）接手，
    // 弹层直接浮在来源应用上，不经过本界面。
    val navCornerRadius = rememberNavSystemCornerRadius()
    val backdropColor = MiuixTheme.colorScheme.surface
    val isDark = isInDarkTheme()
    val isCrossActivity = settings.navTransitionStyle == 1

    // 角标裁剪与背景变暗固定开启，不提供开关；两种转场只是参数取值不同。
    // 转场期间的输入拦截保持 demo 默认（关闭）。
    val effects = remember(navCornerRadius, isDark, isCrossActivity, backdropColor) {
        NavDisplayEffects(
            enableCornerClip = true,
            // AOSP 卡片会把整页缩小，四角都露出背景，所以半径兼底 32dp。
            cornerClipRadius = if (isCrossActivity && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
            cornerClipMode = if (isCrossActivity) NavCornerClipMode.All else NavCornerClipMode.Leading,
            dimAmount = if (isCrossActivity) {
                if (isDark) 0.8f else 0.2f
            } else {
                0.5f
            },
            blockInputDuringTransition = false,
            backdropColor = backdropColor,
        )
    }
    val transition = if (isCrossActivity) CrossActivityTransition else NavTransitions.MiuixDefault
    // 滑动返回固定开启，不提供开关。方向是物理方向（不做 RTL 镜像），
    // 所以 LTR 下左到右、RTL 下右到左。栈底页不可回退，手势自然不会触发。
    val swipeBackDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        NavSwipeDirection.RightToLeft
    } else {
        NavSwipeDirection.LeftToRight
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        // 数据图查看器要盖在一级页面的悬浮底栏之上，所以它与导航宿主同放一个 Box，
        // 并排在 NavDisplay 之后：后绘制的在上层。
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = navigator.backStack,
                // 数据包更新下载中不允许退回仪表盘。失败（Error）后回到重试界面，
                // 这时放行返回：网络一直不通也不至于把人困死在更新页。
                onBack = {
                    val lockedHere = navigator.backStack.lastOrNull() == Route.DataPackUpdate && updateLocked
                    if (!lockedHere) navigator.pop()
                },
                transition = transition,
                effects = effects,
            ) {
                entry<Route.Main>(swipeDismiss = swipeBackDirection) { MainShell() }
                entry<Route.MeishiWechat>(swipeDismiss = swipeBackDirection) {
                    MeishiWechatScreen(onBack = { navigator.pop() })
                }
                entry<Route.NotificationInbox>(swipeDismiss = swipeBackDirection) {
                    NotificationInboxScreen(onBack = { navigator.pop() })
                }
                entry<Route.About>(swipeDismiss = swipeBackDirection) { AboutScreen(onBack = { navigator.pop() }) }
                entry<Route.Announcement>(swipeDismiss = swipeBackDirection) {
                    AnnouncementScreen(onBack = { navigator.pop() })
                }
                entry<Route.AnnouncementArchive>(swipeDismiss = swipeBackDirection) {
                    AnnouncementArchiveScreen(onBack = { navigator.pop() })
                }
                entry<Route.AnnouncementArticle>(swipeDismiss = swipeBackDirection) { route ->
                    AnnouncementScreen(
                        articleCvId = route.cvId,
                        onBack = { navigator.pop() },
                    )
                }
                // 任意一条 B 站图文动态（巅峰对决的赛季规则 / 赛季奖励），
                // 与公告页共用同一套解析与阅读界面。
                entry<Route.BilibiliOpus>(swipeDismiss = swipeBackDirection) { route ->
                    AnnouncementScreen(
                        opusId = route.opusId,
                        title = route.title,
                        onBack = { navigator.pop() },
                    )
                }
                entry<Route.Permission>(swipeDismiss = swipeBackDirection) {
                    PermissionScreen(onBack = { navigator.pop() })
                }
                entry<Route.CoContributorTeam>(swipeDismiss = swipeBackDirection) {
                    CoContributorTeamScreen(onBack = { navigator.pop() })
                }
                entry<Route.ThanksGame>(swipeDismiss = swipeBackDirection) {
                    ThanksGameScreen(onBack = { navigator.pop() })
                }
                entry<Route.ThanksApp>(swipeDismiss = swipeBackDirection) {
                    ThanksAppScreen(onBack = { navigator.pop() })
                }
                entry<Route.TodayLucky>(swipeDismiss = swipeBackDirection) {
                    TodayLuckyScreen(onBack = { navigator.pop() })
                }
                entry<Route.ResourceCalculator>(swipeDismiss = swipeBackDirection) {
                    ResourceCalculatorScreen(onBack = { navigator.pop() })
                }
                entry<Route.CardOddsCalculator>(swipeDismiss = swipeBackDirection) {
                    CardOddsCalculatorScreen(onBack = { navigator.pop() })
                }
                entry<Route.OddsDisclosure>(swipeDismiss = swipeBackDirection) {
                    OddsDisclosureIndexScreen(onBack = { navigator.pop() })
                }
                entry<Route.OddsDisclosureInfo>(swipeDismiss = swipeBackDirection) { route ->
                    OddsDisclosureInfoScreen(
                        categoryIndex = route.categoryIndex,
                        boxIndex = route.boxIndex,
                        onBack = { navigator.pop() },
                    )
                }
                entry<Route.LotteryOddsInfo>(swipeDismiss = swipeBackDirection) { route ->
                    LotteryOddsInfoScreen(
                        categoryIndex = route.categoryIndex,
                        drawIndex = route.drawIndex,
                        onBack = { navigator.pop() },
                    )
                }
                entry<Route.DataImages>(swipeDismiss = swipeBackDirection) {
                    DataImagesScreen(onBack = { navigator.pop() })
                }
                entry<Route.DataPackUpdate>(
                    // 下载 / 准备期间把侧滑关掉（None），点下载前照常可侧滑返回。
                    swipeDismiss = if (updateLocked) NavSwipeDirection.None else swipeBackDirection,
                ) {
                    // 进度状态在本页内部收集：只有这层跟着进度重组，仪表盘与其它页面不受影响。
                    val packState by DataPackRepository.state.collectAsState()
                    DataPackSetupScreen(
                        state = packState,
                        update = true,
                        onDownload = { scope.launch { DataPackRepository.download(env) } },
                        onEnter = { navigator.pop() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                entry<Route.ClothingCollection>(swipeDismiss = swipeBackDirection) {
                    ClothingCollectionScreen(onBack = { navigator.pop() })
                }
                entry<Route.WingCollection>(swipeDismiss = swipeBackDirection) {
                    WingCollectionScreen(onBack = { navigator.pop() })
                }
                entry<Route.CookeryIndex>(swipeDismiss = swipeBackDirection) {
                    CookeryIndexScreen(onBack = { navigator.pop() })
                }
                entry<Route.FraudList>(swipeDismiss = swipeBackDirection) {
                    FraudListScreen(onBack = { navigator.pop() })
                }
                entry<Route.IcuFraud>(swipeDismiss = swipeBackDirection) { route ->
                    IcuFraudScreen(qq = route.qq, onBack = { navigator.pop() })
                }
            }

            // 仪表盘详情里点开的「米鼠的图」由它统一展示，盖住底栏与所有二级页。
            DataImageViewerHostOverlay()
        }
    }
}

/** 一级界面骨架：宽屏留左侧导航栏，窄屏留底部导航栏。两者都固定显示。 */
@Composable
private fun MainShell() {
    val isWide = isWideLayout()
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })

    Row(modifier = Modifier.fillMaxSize()) {
        if (isWide) {
            MainNavigationRail(pagerState)
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            MainPager(pagerState = pagerState, showBottomBar = !isWide)
        }
    }
}

@Composable
private fun MainPager(
    pagerState: PagerState,
    showBottomBar: Boolean,
) {
    val settings = LocalAppSettings.current
    // 跨轴拦截模式下，页面竖向滚动时横向滑动照样能翻页；它接管了全部触摸拖动，
    // 所以原生翻页手势（userScrollEnabled）必须一起关掉，否则两边会抢同一串拖动。
    val crossAxis = settings.pagerInterceptionMode == PagerInterceptionMode.CrossAxisInterceptor.ordinal
    val nativeConnection = PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Horizontal)
    // 底栏的模糊取样层必须挂在 Scaffold 的 content 槽里，与 bottomBar 平行。
    // 一旦把它套在 Scaffold 外面，底栏就成了被采样内容的一部分：记录中的图层会引用自己，
    // RenderThread 在 prepareTree 时无限递归，最终栈溢出崩溃。
    val pagerBackdrop = rememberAppBlurBackdrop()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(pagerState = pagerState, backdrop = pagerBackdrop)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showBottomBar && pagerBackdrop != null) Modifier.layerBackdrop(pagerBackdrop) else Modifier,
                ),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pagerGestureOverride(
                        pagerState = pagerState,
                        mode = PagerInterceptionMode.entries[settings.pagerInterceptionMode],
                        enabled = settings.pageUserScroll,
                    ),
                userScrollEnabled = settings.pageUserScroll && !crossAxis,
                pageNestedScrollConnection = if (crossAxis) PagerGestureNestedScrollConnection else nativeConnection,
                verticalAlignment = Alignment.Top,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = PagerNavigationSpringSpec,
                ),
            ) { page ->
                val contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                when (MainTab.entries[page]) {
                    MainTab.Dashboard -> DashboardScreen(contentPadding)
                    MainTab.DefenseCard -> DefenseCardScreen(contentPadding)
                    MainTab.DataCenter -> DataCenterScreen(contentPadding)
                    MainTab.Tools -> ToolsScreen(contentPadding)
                    MainTab.Settings -> SettingsScreen(contentPadding)
                }
            }
        }
    }
}

@Composable
private fun MainNavigationRail(pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    val expanded = shouldExpandNavigationRail()
    val current = pagerState.targetPage

    NavigationRail(expanded = expanded) {
        MainTab.entries.forEachIndexed { index, tab ->
            NavigationRailItem(
                selected = current == index,
                onClick = { scope.launch { pagerState.springAnimateToPage(index) } },
                icon = tab.icon,
                label = tab.label,
            )
        }
    }
}

/** 一级导航的导航项，顺序与 [MainTab] 一致；枚举不变，所以只求值一次。 */
private val NavigationItems: List<NavigationItem> =
    MainTab.entries.map { NavigationItem(label = it.label, icon = it.icon) }

@Composable
private fun MainBottomBar(pagerState: PagerState, backdrop: LayerBackdrop?) {
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val current = pagerState.targetPage
    val blurActive = settings.enableBlur && backdrop != null
    val onSelect: (Int) -> Unit = { index -> scope.launch { pagerState.springAnimateToPage(index) } }

    if (settings.useFloatingNavigationBar) {
        if (settings.isLiquidGlassBar) {
            // 液态玻璃路线：模糊开是液态玻璃，模糊关退成「滑块」（同一套胶囊几何的非玻璃形态）。
            // 它自带底部安全区与居中胶囊，不再套外层 Box。
            IosLiquidGlassNavigationBar(
                items = NavigationItems,
                selectedIndex = current,
                onItemClick = onSelect,
                backdrop = backdrop,
                isBlurActive = blurActive,
                // 只有模糊真的生效时才有「Q弹」这一档；模糊关时一律退回滑块的常规手感。
                bouncy = blurActive && settings.isBouncyGlassBar,
            )
        } else {
            FloatingBottomBar(
                backdrop = backdrop,
                blurActive = blurActive,
                current = current,
                onSelect = onSelect,
            )
        }
        return
    }

    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    NavigationBar(
        modifier = if (blurActive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
        color = barColor,
        mode = NavigationBarDisplayMode.entries[settings.navigationBarMode],
    ) {
        MainTab.entries.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = current == index,
                onClick = { onSelect(index) },
                icon = tab.icon,
                label = tab.label,
            )
        }
    }
}

@Composable
private fun FloatingBottomBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean,
    current: Int,
    onSelect: (Int) -> Unit,
) {
    // 模糊开启时固定用高光描边：更圆的角、描边高光、底色更透；关闭时退回 miuix 的实色默认栏。
    val cornerRadius = if (blurActive) GlassBarCornerRadius else FloatingToolbarDefaults.CornerRadius
    val shape = RoundedCornerShape(cornerRadius)
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
    val highlight = if (blurActive) {
        if (isInDarkTheme()) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    } else {
        null
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        FloatingNavigationBar(
            modifier = if (blurActive) {
                Modifier.textureBlur(
                    backdrop = backdrop!!,
                    shape = shape,
                    blurRadius = 25f,
                    colors = BlurDefaults.blurColors(
                        blendColors = listOf(
                            BlendColorEntry(
                                color = MiuixTheme.colorScheme.surfaceContainer.copy(0.45f),
                            ),
                        ),
                    ),
                    highlight = highlight,
                )
            } else {
                Modifier
            },
            color = barColor,
            cornerRadius = cornerRadius,
            // 悬浮位置固定居中，不再提供菜单。
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MainTab.entries.forEachIndexed { index, tab ->
                FloatingNavigationBarItem(
                    selected = current == index,
                    onClick = { onSelect(index) },
                    icon = tab.icon,
                    label = tab.label,
                )
            }
        }
    }
}

