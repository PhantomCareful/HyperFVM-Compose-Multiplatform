package com.careful.hyperfvm.compose.screen.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.careful.hyperfvm.compose.component.card.errataContentColor
import com.careful.hyperfvm.compose.component.notification.NotificationDetailSheet
import com.careful.hyperfvm.compose.data.notification.NotificationRepository
import com.careful.hyperfvm.compose.data.notification.StoredNotification
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.screen.PlaceholderContent
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Sort
import com.careful.hyperfvm.compose.ui.AppDialog
import com.careful.hyperfvm.compose.ui.AppDialogButton
import com.careful.hyperfvm.compose.ui.AppDialogButtonRole
import com.careful.hyperfvm.compose.ui.AppIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 排序菜单：索引 0 是倒序（最新在前，默认），索引 1 是顺序（最早在前）。同更新公告合集页。 */
private val SortOptions = listOf("倒序查看（最新在前）", "顺序查看（最早在前）")

/** 通知卡片的圆角与外边距，与项目里其它二级页的卡片一致。 */
private val CardCornerRadius = 20.dp
private val CardSidePadding = 12.dp
private val CardGap = 8.dp

/** 未读通知标题前的标记：用文字代替圆点，行首不再留空白。 */
private const val UnreadLabel = "【未读】"

/** 多选槽位宽度：复选框 26dp + 与文字的 8dp 间距，伸缩时作为一个整体动画。 */
private val SelectionSlotWidth = 26.dp + 8.dp

/**
 * 收件箱：进页面时先把本地存档读出来，并清掉仪表盘上的新通知角标。
 *
 * 未读有两层提示：
 * - 通知标题前加红色的「【未读】」前缀（取勘误卡的红），点开详情弹层才清；
 * - 仪表盘入口上的角标表示「上次看过收件箱之后又到了几条」，进一次本页就归零。
 *
 * 多选靠长按进入：平时顶栏只有排序与一键已读；长按任一通知后，左上角返回换成叉号
 * （退出多选），右上角只剩垃圾桶。删除与一键已读都先弹确认；删除确认按钮沿用温馨礼包
 * 「删除账号」的勘误红，删完再跟一条可撤销的 Snackbar。
 */
@Composable
fun NotificationInboxScreen(onBack: () -> Unit) {
    val env = LocalAppEnv.current
    val scrollBehavior = MiuixScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        NotificationRepository.ensureLoaded(env)
        NotificationRepository.markInboxSeen(env)
    }

    // 默认最新在前：进来多数人只看最近一条。
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // 一键已读会一次性抹掉所有未读提示，也和删除一样先确认一下。
    var showMarkAllReadConfirm by remember { mutableStateOf(false) }
    // 详情弹层的内容与显隐分开存：收起动画期间内容不能清空。
    var detail by remember { mutableStateOf<StoredNotification?>(null) }
    var showDetail by remember { mutableStateOf(false) }

    val items = NotificationRepository.items
    val unread = items.count { !it.read }
    val ordered = remember(items, newestFirst) {
        if (newestFirst) items.asReversed().toList() else items
    }

    // 点开详情才算读过：未读前缀只在这一刻清掉（refresh 里的状态会一起落盘）。
    val openDetail: (StoredNotification) -> Unit = { item ->
        NotificationRepository.markRead(env, item.id)
        detail = item.copy(read = true)
        showDetail = true
    }

    // 退出多选：勾选一起清掉，顶栏恢复成普通的返回 + 排序/一键已读。
    val exitSelection: () -> Unit = {
        selecting = false
        selected = emptySet()
    }

    /**
     * 删除选中的通知：先让列表少掉这几条，再弹可撤销的 Snackbar。
     *
     * 与温馨礼包的删账号同一套机制：真正落盘推迟到 Snackbar 自己消失（或退出本页），
     * 点「撤销」则按 id 顺序放回去（见 NotificationRepository.remove / undoRemove）。
     */
    val deleteSelected: () -> Unit = {
        val ids = selected
        showDeleteConfirm = false
        val token = NotificationRepository.remove(env, ids)
        if (token != null) {
            exitSelection()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "已删除 ${ids.size} 条通知",
                    actionLabel = "撤销",
                    duration = SnackbarDuration.Long,
                )
                // 点了撤销就放回去；提示自己消失才算真删（token 防止后一批被前一条提示定案）。
                if (result == SnackbarResult.ActionPerformed) {
                    NotificationRepository.undoRemove(env, token)
                } else {
                    NotificationRepository.commitRemove(env, token)
                }
            }
        }
    }

    // 离开收件箱时待定的删除立刻定案：Snackbar 跟着本页一起没了，撤销的机会也算用掉。
    DisposableEffect(Unit) {
        onDispose { NotificationRepository.commitRemove(env) }
    }

    // 多选时左上角换成叉号。单独存成变量：lambda 在这种条件赋值里要显式给出类型才稳。
    val selectionIcon: (@Composable () -> Unit)? = if (selecting) {
        {
            AppTopBarIconButton(onClick = exitSelection) {
                Icon(imageVector = MiuixIcons.Close, contentDescription = "退出选择")
            }
        }
    } else {
        null
    }

    // 多选时系统返回先退多选，而不是直接离开本页（与左上角叉号一致）。
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = selecting,
        onBackCompleted = exitSelection,
    )

    val orderMenu = remember(newestFirst) {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = SortOptions[0],
                        selected = newestFirst,
                        onClick = { newestFirst = true },
                    ),
                    DropdownItem(
                        text = SortOptions[1],
                        selected = !newestFirst,
                        onClick = { newestFirst = false },
                    ),
                ),
            ),
        )
    }

    ScreenScaffold(
        title = if (selecting) "通知管理" else "收件箱",
        subtitle = when {
            // 多选时标题栏直接报已选条数，比让人在列表里回溯更直观。
            selecting -> if (selected.isEmpty()) "未选择通知" else "已选择 ${selected.size} 条"
            items.isEmpty() -> ""
            unread > 0 -> "共 ${items.size} 条 · $unread 条未读"
            else -> "共 ${items.size} 条"
        },
        outerPadding = PaddingValues(0.dp),
        // 列表本身不滚动顶栏（内容区是独立的 LazyColumn），固定小标题即可。
        compactTitle = true,
        // 多选时不显示返回按钮：左上角让给「退出多选」的叉号（返回按钮优先级更高）。
        onBack = if (selecting) null else onBack,
        navigationIcon = selectionIcon,
        scrollBehavior = scrollBehavior,
        attachScrollBehavior = false,
        // Snackbar 宿主跟着本页的 Scaffold：删通知后的撤销提示从底部升起。
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 弹层必须在 Scaffold 子树里组合，才能挂到它的弹层宿主上（二级页没有外层 Scaffold）。
        overlay = { _ ->
            AppDialog(
                show = showDeleteConfirm,
                title = "删除通知",
                onDismissRequest = { showDeleteConfirm = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "将删除选中的 ${selected.size} 条通知，删除后可在弹出的提示条中撤销。",
                        modifier = Modifier.fillMaxWidth(),
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppDialogButton(
                            text = "咱手滑了",
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(20.dp))
                        AppDialogButton(
                            text = "确认删除",
                            onClick = deleteSelected,
                            modifier = Modifier.weight(1f),
                            // 破坏性操作走弹层按钮的 Destructive 语气（勘误红），不用主色。
                            role = AppDialogButtonRole.Destructive,
                        )
                    }
                }
            }

            AppDialog(
                show = showMarkAllReadConfirm,
                title = "一键已读",
                onDismissRequest = { showMarkAllReadConfirm = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "将清除 $unread 条通知的未读提示。",
                        modifier = Modifier.fillMaxWidth(),
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppDialogButton(
                            text = "咱手滑了",
                            onClick = { showMarkAllReadConfirm = false },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(20.dp))
                        AppDialogButton(
                            text = "确认已读",
                            onClick = {
                                showMarkAllReadConfirm = false
                                NotificationRepository.markAllRead(env)
                            },
                            modifier = Modifier.weight(1f),
                            // 只清未读标记，不是破坏性操作，用主色而不是勘误红。
                            role = AppDialogButtonRole.Primary,
                        )
                    }
                }
            }

            NotificationDetailSheet(
                notification = detail,
                show = showDetail,
                onDismissRequest = { showDetail = false },
            )
        },
        actions = {
            if (selecting) {
                // 多选期间只留垃圾桶：此时排序与一键已读都用不上。
                AppTopBarIconButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = selected.isNotEmpty(),
                ) {
                    Icon(imageVector = MiuixIcons.Delete, contentDescription = "删除选中")
                }
            } else {
                AppIconDropdownMenu(entries = orderMenu) {
                    Icon(imageVector = MiuixIcons.Sort, contentDescription = "排序")
                }
                AppTopBarIconButton(
                    onClick = { showMarkAllReadConfirm = true },
                    enabled = NotificationRepository.hasUnread,
                ) {
                    Icon(imageVector = MiuixIcons.Show, contentDescription = "一键已读")
                }
            }
        },
    ) { padding ->
        // 列表常驻、空状态叠在它上面：删掉最后一条时先让卡片淡出，占位内容再浮上来。
        // 若按 items.isEmpty() 直接把列表换成占位，列表会被整体拆掉，退场动画就没得播了。
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + CardGap,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                items(items = ordered, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        // 删除 / 撤销删除：这条自己淡出淡入，其余卡片顺势挪到新位置。
                        modifier = Modifier.animateItem(),
                        selecting = selecting,
                        checked = item.id in selected,
                        onClick = {
                            if (selecting) {
                                selected = if (item.id in selected) {
                                    selected - item.id
                                } else {
                                    selected + item.id
                                }
                            } else {
                                openDetail(item)
                            }
                        },
                        onLongPress = {
                            // 长按任一通知进入多选，并把按住的这条直接勾上。
                            selecting = true
                            selected = selected + item.id
                        },
                    )
                }
            }
            AnimatedVisibility(
                visible = ordered.isEmpty(),
                // 稍等一下让退场的那张卡片让开，再淡入占位，两段内容不至于叠在一起。
                enter = fadeIn(animationSpec = tween(durationMillis = 160, delayMillis = 120)),
                exit = fadeOut(animationSpec = tween(durationMillis = 120)),
            ) {
                PlaceholderContent(
                    padding = padding,
                    icon = MiuixIcons.Email,
                    title = "收件箱是空的",
                    description = "云端有新通知时会自动出现在这里。",
                )
            }
        }
    }
}

/**
 * 一条通知：整卡可点，行内是 ArrowPreference 的排布。
 *
 * 没有直接用 `ArrowPreference`：它的 summary 只收一个
 * String、按单行渲染，而需求要正文在卡片上只显示两行再省略。行内按它的实现
 * （BasicComponent）复刻：16dp 内边距、56dp 最小高度、headline1 标题 + body2 摘要、
 * 行尾箭头。也没有直接复用 BasicComponent 的槽位：它在槽位宽度归零时才撤掉 8dp 间距，
 * 多选收起动画的最后一帧会多跳一下——这里把间距并进槽位宽度一起做动画。
 *
 * 点击反馈挂在卡片上（`pressFeedbackType` + `showIndication`），行内就不再叠一层
 * clickable：同一个点击区域只留一层高光，选中态与详情入口也共用这一次点击。
 *
 * 未读提示是标题前的红色前缀，读过之后宽度收起，标题顺势滑回最左。
 *
 * 列表里的增删动画由调用方传进来的 `animateItem()` 负责，这里只留布局本身。
 */
@Composable
private fun NotificationCard(
    item: StoredNotification,
    modifier: Modifier = Modifier,
    selecting: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = CardSidePadding, end = CardSidePadding, bottom = CardGap),
        cornerRadius = CardCornerRadius,
        colors = CardDefaults.defaultColors(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // showIndication = true 同时要给 onClick，卡片才会画出点击高光。
        onClick = onClick,
        onLongPress = onLongPress,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                // 与 BasicComponent 的内边距保持一致。
                .padding(BasicComponentDefaults.InsideMargin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 复选框与它后面的 8dp 间距作为一个整体伸缩：宽度归零时不会剩下一小截间隙。
            val slotWidth by animateDpAsState(
                targetValue = if (selecting) SelectionSlotWidth else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "selectionSlot",
            )
            if (slotWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .clipToBounds(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Checkbox(
                        state = if (checked) ToggleableState.On else ToggleableState.Off,
                        // 点击由整张卡片接管，复选框只负责显示。
                        onClick = null,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 未读标记用勘误卡的红。读过之后宽度收起，标题顺势滑回最左。
                    AnimatedVisibility(
                        visible = !item.read,
                        enter = expandHorizontally(expandFrom = Alignment.Start),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
                    ) {
                        Text(
                            text = UnreadLabel,
                            fontSize = MiuixTheme.textStyles.headline1.fontSize,
                            fontWeight = FontWeight.Medium,
                            color = errataContentColor(),
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = item.title,
                        // 不撑满：标题短时标记紧跟在它后面，标题长时左对齐省略。
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = MiuixTheme.textStyles.headline1.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = item.content,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    // 卡片上只露两行，完整内容点开详情弹层看。
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 与 BasicComponent 一样，正文与行尾箭头之间留 8dp。
            Spacer(Modifier.width(8.dp))
            Image(
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
            )
        }
    }
}
