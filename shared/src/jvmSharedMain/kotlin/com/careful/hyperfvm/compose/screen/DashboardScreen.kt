package com.careful.hyperfvm.compose.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.dashboard.DashboardDetailSheet
import com.careful.hyperfvm.compose.component.dashboard.DashboardGiftPairRow
import com.careful.hyperfvm.compose.component.dashboard.DashboardStatusCard
import com.careful.hyperfvm.compose.component.dialog.openUrl
import com.careful.hyperfvm.compose.component.imageviewer.DataImageViewerHost
import com.careful.hyperfvm.compose.data.announcement.AnnouncementSource
import com.careful.hyperfvm.compose.data.dashboard.DashboardCardId
import com.careful.hyperfvm.compose.data.dashboard.DashboardEntry
import com.careful.hyperfvm.compose.data.dashboard.DashboardPreferences
import com.careful.hyperfvm.compose.data.dashboard.DashboardRepository
import com.careful.hyperfvm.compose.data.dashboard.DashboardSheet
import com.careful.hyperfvm.compose.data.dashboard.DashboardSlot
import com.careful.hyperfvm.compose.data.dashboard.DashboardStatus
import com.careful.hyperfvm.compose.data.dashboard.isMonthEnd
import com.careful.hyperfvm.compose.data.dashboard.isVisible
import com.careful.hyperfvm.compose.data.dashboard.token
import com.careful.hyperfvm.compose.data.notification.NotificationRepository
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.isDesktopPlatform
import com.careful.hyperfvm.compose.platform.rememberBiometricGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.RichTooltipBox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 使用提示，对应 Java 版仪表盘顶部那张 tips 卡片。
 *
 * 文案按 Compose 版的实际交互重写：点击卡片看详情。
 * 其余能做的三件事是刷新、长按拖动排序、右上角管理显隐；
 * 刷新这一项分平台：桌面端没有下拉手势，走顶栏右侧的刷新按钮。
 */
private val DashboardTips: String
    get() = if (isDesktopPlatform) {
        "点击每个卡片可以查看详细信息，长按卡片可以拖动排序，右边的两个按钮为刷新数据和卡片管理。"
    } else {
        "点击每个卡片可以查看详细信息，长按卡片可以拖动排序，下拉可以刷新数据，右边的按钮为卡片管理。"
    }

/** 卡片之间的竖向间距。 */
private val CardGap = 8.dp

/** 卡片与屏幕左右边缘的距离，与设置页、防御卡列表一致。 */
private val CardSidePadding = 12.dp

/** 卡片管理弹层里一个开关一张卡，卡间距与圆角同项目其它弹层内容卡。 */
private val SheetCardGap = 10.dp
private val SheetCardCornerRadius = 20.dp

/** 更新公告卡右下的提示：点开才去拉正文，卡片本身不请求网络。 */
private const val NoticeCardStatus = "点击查看"

/** 月末提醒的那一行：固定在首位，不参与长按拖动。 */
private val MonthEndSlot = DashboardSlot.Single(DashboardCardId.MonthEnd)

/** 下拉刷新时指示器的最短可见时长：数据往往回来得比动画快，不补足会看到它闪一下就收。 */
private const val MinRefreshVisibleMillis = 700L

/** 进度条的「喷出」：定长一喷到底，用减速曲线收尾，不走弹簧所以不会回弹过冲。 */
private val ProgressRevealSpec = tween<Float>(durationMillis = 450, easing = LinearOutSlowInEasing)

/**
 * 本次进程里首屏那次「自动刷新 + 进度条喷出」是否已经做过。
 * 底栏是 HorizontalPager，切到别的页面再切回来，DashboardScreen 会整页重建，
 * `remember` 里的状态带不过去，只能记在进程里：做过了就不再自动刷新。
 * 进度条也直接以满宽度起步，只有手动下拉才重播。
 */
private object LaunchRefresh {
    var done = false
}

/**
 * 仪表盘：一列可拖动排序的状态卡，下拉刷新（桌面端是顶栏右侧的刷新按钮），
 * 右上角两个按钮分别看提示和管显隐。
 *
 * 卡片右侧是状态文案（暂无 / 即将开启 / 12/21 等）；点开卡片弹通用详情
 * （时间行、返场卡片名单、场次与底部按钮），抢红包在今日开启时改为弹提示。
 */
@Composable
fun DashboardScreen(outerPadding: PaddingValues) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val haptic = LocalHapticFeedback.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val preferences = remember(env) { DashboardPreferences(env) }
    val orderState = remember(preferences) { CardOrderState { preferences.order } }
    val settings = LocalAppSettings.current
    val biometricGate = rememberBiometricGate()

    // 首屏先用上次的结果渲染；一次都没成功过时卡片显示「请等待…」。
    var entries by remember { mutableStateOf(DashboardRepository.cached().orEmpty()) }
    var loaded by remember { mutableStateOf(DashboardRepository.cached() != null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    // 进页面就刷一次数据（每个进程只刷首屏这一回），不展开下拉指示器。
    var refreshing by remember { mutableStateOf(false) }
    // 同一份回调喂两处：Android 端管触摸下拉，桌面端管顶栏上的刷新按钮。
    // 置 true 既让指示器/按钮进入刷新态，也让刷新协程知道这是手动刷新、要重播喷出动画。
    val requestRefresh: () -> Unit = {
        refreshing = true
        reloadToken += 1
    }
    // 进度条的展开系数。放在屏幕层：卡片被 LazyColumn 回收后重组合不会被重置。
    // 喷过了就直接从满值起步，页面重建时不会又从 0 长一遍。
    val progressReveal = remember { Animatable(if (LaunchRefresh.done) 1f else 0f) }
    val pullToRefreshState = rememberPullToRefreshState()

    var showCardSheet by remember { mutableStateOf(false) }
    val tipsState = rememberTooltipState(isPersistent = true)

    // 详情弹层的数据与显隐分开存：收起动画期间内容不能清空。
    var detailSheet by remember { mutableStateOf<DashboardSheet?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    LaunchedEffect(reloadToken) {
        // 进页面时的自动刷新也走这条协程，靠这个标记区分是不是手动下拉。
        val manual = refreshing
        // 收件箱的存档先读出来：顶栏角标靠它算条数，切回本页时也照样是现成的。
        NotificationRepository.ensureLoaded(env)
        // 只有两种刷新：本次进程的头一回，和用户手动下拉。切页重建不该再请求一次。
        if (!manual && LaunchRefresh.done) return@LaunchedEffect
        // 收件与仪表盘数据并行：收件自己兜底，慢或失败都不影响卡片。
        launch { NotificationRepository.refresh(env) }
        val startedAt = System.currentTimeMillis()
        val result = try {
            DashboardRepository.refresh(env)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 各数据源已各自兜底，走到这里说明出了意料之外的问题：保留旧数据，
            // 首次加载则让卡片落到「获取失败」。
            null
        }
        // 手动下拉时数据往往比指示器动画还快，补足最短时长，避免它闪一下就收；
        // 首屏那次没有指示器，不用等，数据到了直接上。
        if (manual) {
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MinRefreshVisibleMillis) delay(MinRefreshVisibleMillis - elapsed)
        }
        // 只有本次进程头一回或者手动下拉才喷；其余路径根本不刷新，自然不会到喷这一步。
        val spray = manual || !LaunchRefresh.done
        // 先把展开系数收回 0，再提交新数据：否则首屏那种「刷新前没有进度条」的情况。
        // 新卡会在第一帧就画出满进度，下一帧才被归零重播，看起来就是先闪一下再喷。
        if (spray) progressReveal.snapTo(0f)
        // 新数据、进度条起点、指示器收合放在同一帧：卡片不会先跳到新宽度再重播。
        if (result != null) entries = result
        loaded = true
        refreshing = false
        LaunchRefresh.done = true
        if (spray) {
            // 数据已经落位，接着从 0 喷到目标宽度。
            progressReveal.animateTo(targetValue = 1f, animationSpec = ProgressRevealSpec)
        }
    }

    val monthEnd = isMonthEnd()
    // 顺序以「行」为单位：成对的那一行永远显示，其余看有没有被关掉；
    // 月末提醒只在当月最后一天出现；数据源判定「没有就不展示」的卡片
    //（抢红包 / 走马观花 / 甜蜜岛 / 双爆）也在这一层滤掉。
    val visibleSlots = orderState.order.filter { slot ->
        slot.isVisible(preferences.hidden) &&
            (slot != MonthEndSlot || monthEnd) &&
            (slot !is DashboardSlot.Single || !entryOf(slot.id, entries, loaded).hidden)
    }
    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (orderState.move(from.key, to.key)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // 温馨礼包的账号页里能直接删账号，开门前先验一次（即设置里的「保护锁」）：
    // 开关关掉、或设备没有任何验证手段时直接进。
    val openMeishi: () -> Unit = {
        if (settings.contentBiometricAuth && biometricGate.available) {
            biometricGate.request(
                title = "确保那是你",
                subtitle = "验证您的身份以继续操作",
            ) { ok ->
                if (ok) navigator.push(Route.MeishiWechat)
            }
        } else {
            navigator.push(Route.MeishiWechat)
        }
    }

    ScreenScaffold(
        title = "仪表盘",
        outerPadding = outerPadding,
        scrollBehavior = scrollBehavior,
        // 左上角是收件箱入口：有新通知时套一层角标，数字是「上次看过之后又到了几条」。
        navigationIcon = {
            val newNotifications = NotificationRepository.newCount
            BadgedBox(
                badge = {
                    if (newNotifications > 0) {
                        Badge {
                            // Badge 已经把文字样式换成 footnote2 并供给内容色，这里不再覆盖。
                            Text(text = if (newNotifications > 99) "99+" else "$newNotifications")
                        }
                    }
                },
            ) {
                IconButton(onClick = { navigator.push(Route.NotificationInbox) }) {
                    Icon(imageVector = MiuixIcons.Email, contentDescription = "收件箱")
                }
            }
        },
        // 顶栏的滚动连接交给 PullToRefresh 统一编排，两边都挂会让折叠速度翻倍。
        attachScrollBehavior = false,
        // 桌面端没有下拉手势，刷新入口挂在顶栏右侧的按钮上。
        onRefresh = requestRefresh,
        refreshing = refreshing,
        actions = {
            RichTooltipBox(
                text = DashboardTips,
                state = tipsState,
                title = "使用提示",
                actionText = "知道了",
                onActionClick = {},
            ) {
                IconButton(onClick = { scope.launch { tipsState.show() } }) {
                    Icon(imageVector = MiuixIcons.Info, contentDescription = "使用提示")
                }
            }
            IconButton(onClick = { showCardSheet = true }) {
                Icon(imageVector = MiuixIcons.Tune, contentDescription = "卡片管理")
            }
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = requestRefresh,
            modifier = Modifier.fillMaxSize(),
            pullToRefreshState = pullToRefreshState,
            topAppBarScrollBehavior = scrollBehavior,
            // 指示器避开折叠顶栏：内容顶部内边距正好就是顶栏高度。
            contentPadding = PaddingValues(top = padding.calculateTopPadding()),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            refreshTexts = listOf(
                "下拉刷新仪表盘",
                "松开立即刷新",
                "正在刷新",
                "刷新完成",
            ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + CardGap,
                    bottom = padding.calculateBottomPadding() + 12.dp,
                ),
            ) {
                // 成对的行永远显示，所以列表不会空，不用再准备「全部隐藏」的占位。
                items(items = visibleSlots, key = { it.token }) { slot ->
                    ReorderableItem(state = reorderableState, key = slot.token) { _ ->
                        val interactionSource = remember { MutableInteractionSource() }
                        // 整行就是拖动手柄：长按任意位置都能开始排序，成对的两张卡一起走。
                        // 月末提醒是重要通知，固定在顶端，所以不给它手柄。
                        val dragHandle = if (slot == MonthEndSlot) {
                            Modifier
                        } else {
                            Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    preferences.saveOrder(orderState.commit())
                                },
                                interactionSource = interactionSource,
                            )
                        }
                        DashboardSlotRow(
                            slot = slot,
                            entries = entries,
                            loaded = loaded,
                            progressReveal = progressReveal,
                            // 公告卡直接进合集：倒序列表的第一项就是最新一期，历史上翻也在同一页里。
                            onNoticeClick = { navigator.push(Route.AnnouncementArchive) },
                            onGiftClick = openMeishi,
                            onOpenSheet = { sheet ->
                                detailSheet = sheet
                                showDetailSheet = true
                            },
                            modifier = Modifier
                                .padding(horizontal = CardSidePadding)
                                .padding(bottom = CardGap)
                                .then(dragHandle),
                        )
                    }
                }
            }
        }
    }

    OverlayBottomSheet(
        show = showCardSheet,
        title = "卡片管理",
        onDismissRequest = {
            // 弹层关掉才算敲定：这时才把关掉的卡片正式排到末尾（见 DashboardPreferences）。
            preferences.settleOrder()
            // 可见状态得自己复位：miuix 只把点击弹层外、返回手势算作 dismiss，
            // 不会替调用方改 show，漏掉这行弹层就永远关不上。
            showCardSheet = false
        },
    ) {
        CardManagementSheet(
            order = orderState.order,
            hidden = preferences.hidden,
            onVisibleChange = preferences::setVisible,
        )
    }

    DashboardDetailSheet(
        sheet = detailSheet,
        show = showDetailSheet,
        // 收起只把 show 置 false：detailSheet 留着给退出动画渲染，
        // 提前清空会让弹层在半空变成空白再滑走。
        onDismissRequest = { showDetailSheet = false },
        // 查看器挂在应用根层、盖在弹层之上，所以点图直接打开即可。
        // 弹层保持不动，关掉查看器后正好接着看这页的其它内容。
        onOpenImage = DataImageViewerHost::show,
        onLink = { link ->
            // 哔哩哔哩图文动态（巅峰对决的赛季规则 / 赛季奖励）在 App 内直接解析查看；
            // 其余外链（营地任务顺序表之类）照旧交给浏览器。
            val opusId = AnnouncementSource.opusId(link.url)
            if (opusId != null) {
                // 弹层不收起：文章页由导航宿主压在当前页之上，返回后弹。
                // 仍停在原来的位置。
                navigator.push(Route.BilibiliOpus(opusId = opusId, title = link.label))
            } else {
                openUrl(env, toaster, link.url)
            }
        },
    )
}

/** 一行卡片：成对的那一行排两张半宽卡，其余就是一张普通状态卡。 */
@Composable
private fun DashboardSlotRow(
    slot: DashboardSlot,
    entries: Map<DashboardCardId, DashboardEntry>,
    loaded: Boolean,
    progressReveal: Animatable<Float, AnimationVector1D>,
    onNoticeClick: () -> Unit,
    onGiftClick: () -> Unit,
    onOpenSheet: (DashboardSheet) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (slot) {
        DashboardSlot.GiftPair -> {
            val gift = entryOf(DashboardCardId.SweetGift, entries, loaded)
            DashboardGiftPairRow(
                giftStatus = gift.status,
                giftFailed = gift.error,
                onGiftClick = onGiftClick,
                noticeStatus = NoticeCardStatus,
                onNoticeClick = onNoticeClick,
                modifier = modifier,
            )
        }

        is DashboardSlot.Single -> {
            val id = slot.id
            val entry = entryOf(id, entries, loaded)
            val sheet = entry.sheet
            val todayTip = sheet?.todayTip
            if (todayTip != null) {
                // 抢红包今日开启：点卡片只弹这句提醒，不打开详情弹层。
                // 长按留给拖动排序，所以关掉 tooltip 自带的“长按显示”手势。
                val tooltipState = rememberTooltipState(isPersistent = true)
                val scope = rememberCoroutineScope()
                RichTooltipBox(
                    text = todayTip,
                    state = tooltipState,
                    title = "抢红包提示",
                    actionText = "好的",
                    onActionClick = {},
                    enabled = false,
                    modifier = modifier,
                ) {
                    DashboardStatusCard(
                        title = entry.title ?: id.title,
                        entry = entry,
                        progressReveal = progressReveal,
                        onClick = { scope.launch { tooltipState.show() } },
                    )
                }
            } else {
                DashboardStatusCard(
                    // 二转打折用数据里的动态标题覆盖枚举里的固定名。
                    title = entry.title ?: id.title,
                    entry = entry,
                    progressReveal = progressReveal,
                    modifier = modifier,
                    onClick = { if (sheet != null) onOpenSheet(sheet) },
                )
            }
        }
    }
}

/** 数据源没返回的卡片按加载状态兜底：没加载完就说「请等待」，加载完了就是「获取失败」。 */
private fun entryOf(
    id: DashboardCardId,
    entries: Map<DashboardCardId, DashboardEntry>,
    loaded: Boolean,
): DashboardEntry = entries[id] ?: DashboardEntry(
    if (loaded) DashboardStatus.Failed else DashboardStatus.Pending,
)

/**
 * 弹层里的显隐开关：一行一项，顺序与仪表盘保持一致。
 *
 * 拨开关只改显隐，顺序要等弹层关闭（[DashboardPreferences.settleOrder]）才重排。
 * 所以这个列表在弹层期间纹丝不动：关掉的卡片当场还能拨回来，回到原来的位置。
 * 成对的温馨礼包 / 更新公告固定并排、不能单独隐藏，这里不列它们。
 */
@Composable
private fun CardManagementSheet(
    order: List<DashboardSlot>,
    hidden: Set<DashboardCardId>,
    onVisibleChange: (DashboardCardId, Boolean) -> Unit,
) {
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val slots = order.filterIsInstance<DashboardSlot.Single>()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        // 左右边距由弹层自己的 insideMargin（4dp）负责，这里再加就会叠成双层。
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + navigationBarBottom),
    ) {
        items(items = slots, key = { it.token }) { slot ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // 拖到底部的重排在弹层关闭时才发生，这里仍留位移动画以防顺序变化。
                    .animateItem()
                    .padding(bottom = SheetCardGap),
                cornerRadius = SheetCardCornerRadius,
                // 同其它弹层：换成 secondaryContainer 才能在弹层里分辨出卡片边界。
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
            ) {
                SwitchPreference(
                    title = slot.id.title,
                    summary = if (slot.id == DashboardCardId.MonthEnd) "仅每月最后一天显示" else null,
                    checked = slot.id !in hidden,
                    onCheckedChange = { visible -> onVisibleChange(slot.id, visible) },
                )
            }
        }
    }
}

/**
 * 拖动排序期间的双层顺序。
 *
 * 手势回调是长驻的，只捕获这个稳定对象：`pending` 负责拖动中的即时顺序。
 * 松手时由 [commit] 交出去落盘，避免回调里读到手势开始前的旧列表。
 * 平时直接读 [persisted]：拨开关会重排那份顺序（关掉的沉到末尾），这里要跟着一起变。
 */
private class CardOrderState(private val persisted: () -> List<DashboardSlot>) {

    private var pending by mutableStateOf<List<DashboardSlot>?>(null)

    val order: List<DashboardSlot> get() = pending ?: persisted()

    /** 把 [fromKey] 挪到 [toKey] 的位置；两个 key 都在列表里且不相同才算移动成功。 */
    fun move(fromKey: Any, toKey: Any): Boolean {
        val current = order
        val from = current.indexOfFirst { it.token == fromKey }
        val to = current.indexOfFirst { it.token == toKey }
        if (from < 0 || to < 0) return false
        // 月末提醒固定在首位：它不能被拖走，也不能被别的卡挤到后面。
        if (current[from] == MonthEndSlot) return false
        // 拖到第一格时只能停在月末提醒后面；它被关掉时第一格是空出来的，照常占用。
        val floor = if (current.firstOrNull() == MonthEndSlot) 1 else 0
        val target = to.coerceAtLeast(floor)
        if (target == from) return false
        pending = current.toMutableList().apply { add(target, removeAt(from)) }
        return true
    }

    /** 结束一次拖动：把当前顺序交出去落盘，并回到直接读已保存顺序的状态。 */
    fun commit(): List<DashboardSlot> {
        val result = order
        pending = null
        return result
    }
}
