package com.careful.hyperfvm.compose.screen.meishi

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.ErrataCard
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.meishi.MeishiAccountCard
import com.careful.hyperfvm.compose.component.meishi.MeishiAccountSheet
import com.careful.hyperfvm.compose.component.meishi.MeishiAddLinkSheet
import com.careful.hyperfvm.compose.component.meishi.MeishiCardCornerRadius
import com.careful.hyperfvm.compose.component.meishi.MeishiCardGap
import com.careful.hyperfvm.compose.component.meishi.MeishiGiftRow
import com.careful.hyperfvm.compose.component.meishi.MeishiSidePadding
import com.careful.hyperfvm.compose.component.meishi.UnknownPlayerName
import com.careful.hyperfvm.compose.data.meishi.MeishiAccount
import com.careful.hyperfvm.compose.data.meishi.MeishiAccounts
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.isDesktopPlatform
import com.careful.hyperfvm.compose.platform.rememberBiometricGate
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.shared.*
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 礼包清单里的一项，文案与图标都取自 Java 版的 `activity_meishi_wechat`。 */
private class GiftItem(
    val title: String,
    val description: String,
    val icon: String,
)

private val GiftItems = listOf(
    GiftItem("道具双爆卡(2小时)×2", "装备后一定时间内，战斗中物品掉落率提高100%", "x13120210"),
    GiftItem("强化水晶×5", "满足一定数量可强化宝石(1–10级)", "x12700010"),
    GiftItem("上等香料×10", "集齐5个后在卡片制作中使用，可以直接制作出2星的卡片", "x12340020"),
    GiftItem("秘制香料×5", "集齐5个后在卡片制作中使用，可以直接制作出3星的卡片", "x12340080"),
    GiftItem("极品香料×5", "集齐5个后在卡片制作中使用，可以直接制作出4星的卡片", "x12340090"),
    GiftItem("1级四叶草×5", "提高强化成功率1.2倍", "x12320010"),
    GiftItem("2级四叶草×3", "提高强化成功率1.4倍", "x12320060"),
    GiftItem("3级四叶草×1", "提高强化成功率1.7倍", "x12320070"),
)

/** 使用说明，前两条同 Java 版 `content_meishi_wechat_rules_*`。
 *
 * 第三条按新交互重写：长按不再是删除入口（现在用于拖动排序），
 * 改角色 ID 与删除账号都收到点开的账号弹层里。
 */
private val UsageRules = listOf(
    "点击右上角的添加按钮，输入您从微信公众号-美食中心复制过来的链接。",
    "添加成功后，当前界面会出现一个卡片，里面会显示您的账号信息(所在区服、角色ID)，您可以检查信息是否有误。",
    "点击账号卡片可以修改角色ID或删除账号，长按卡片可以拖动调整顺序。",
    "您每次刷新仪表盘信息时都会自动执行一次领取操作。",
)

/** 两条提示卡，分别对应 Java 版的 Important 卡与 errorContainer 卡。
 *
 * 第一条只讲「打开受支持的链接」这一套流程，桌面端没有这个功能，那张卡整张不显示。
 */
private const val PermissionTips =
    "允许App打开受支持的链接后，您在微信公众号-美食中心内选择“在浏览器中打开”，即可自动跳转到App，" +
        "一步添加链接；您也可以在设备任意位置（如笔记、便签）长按选中链接，" +
        "系统也会弹出使用App打开的选项（一个链接符号或者直接是App图标）"

private const val OpenIdWarning = "请妥善保管好您的openid，不要泄露给他人！"

/**
 * 温馨礼包链接管理页：账号列表、添加链接、礼包清单与使用说明。
 *
 * 身份验证在进入本页之前就已经做过（见仪表盘的入口）；页内只有「删除账号」
 * 会再验一次：删掉一个账号就少一份自动领取，算不可逆操作。备份与还原暂未迁移。
 */
@Composable
fun MeishiWechatScreen(onBack: () -> Unit) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val haptic = LocalHapticFeedback.current
    val settings = LocalAppSettings.current
    val biometricGate = rememberBiometricGate()
    val store = remember(env) { MeishiAccounts.of(env) }
    val accounts = store.accounts
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    // 正在管理的账号：收起弹层时不清空，退出动画期间内容还得在。
    var managedAccount by remember { mutableStateOf<MeishiAccount?>(null) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    // 回调里每次重读 store.accounts：拖动过程中列表已经变过，不能用组合时的快照。
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val current = store.accounts
        val fromIndex = current.indexOfFirst { it.openId == from.key }
        val toIndex = current.indexOfFirst { it.openId == to.key }
        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
            store.move(fromIndex, toIndex)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * 删除账号：开了保护锁就再验一次身份，验过才动手。
     *
     * 删掉只让卡片消失，真正落盘等 Snackbar 超时或退出本页（见 MeishiStore.commitRemoval）；
     * 提示里点「撤销」就把卡片放回原位。
     */
    fun deleteAccount(account: MeishiAccount) {
        fun perform() {
            // 先收起弹层：卡片一消失，弹层里就只剩一个已经没了的账号。
            showAccountSheet = false
            if (!store.remove(account.openId)) return
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "账号【${account.playerName.ifBlank { UnknownPlayerName }}】已删除",
                    actionLabel = "撤销",
                    duration = SnackbarDuration.Long,
                )
                // 点了撤销就放回去；提示自己消失才算真删。
                if (result == SnackbarResult.ActionPerformed) {
                    store.undoRemoval(account.openId)
                } else {
                    store.commitRemoval(account.openId)
                }
            }
        }
        if (settings.contentBiometricAuth && biometricGate.available) {
            biometricGate.request(
                title = "确保那是你",
                subtitle = "验证您的身份以删除账号",
            ) { ok -> if (ok) perform() }
        } else {
            perform()
        }
    }

    // 离开本页时待定的删除立刻定案：Snackbar 跟着本页一起没了，撤销的机会也算用掉。
    DisposableEffect(store) {
        onDispose { store.commitRemoval() }
    }

    ScreenScaffold(
        title = "温馨礼包链接管理",
        // 二级页没有外层 Pager 的底栏，底部安全区由内层 Scaffold 自己吃。
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        actions = {
            AppTopBarIconButton(onClick = { showAddSheet = true }) {
                Icon(imageVector = MiuixIcons.Add, contentDescription = "添加链接")
            }
        },
        // Snackbar 宿主跟着本页的 Scaffold：删账号后的撤销提示从底部升起。
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 弹层必须在 Scaffold 子树里组合，才能挂到它的弹层宿主上。
        overlay = { _ ->
            MeishiAddLinkSheet(
                show = showAddSheet,
                onDismissRequest = { showAddSheet = false },
            )
            MeishiAccountSheet(
                account = managedAccount,
                show = showAccountSheet,
                onDismissRequest = { showAccountSheet = false },
                onDelete = { deleteAccount(it) },
            )
        },
    ) { padding ->
        LazyColumn(
            // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表。
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + MeishiCardGap,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            // 桌面端没有「默认打开」这个系统设置页，也没有受支持链接这套流程：
            // 入口卡与对应的说明卡都不显示，免得指点一个不存在的功能。
            if (!isDesktopPlatform) {
                item(key = "permission") {
                    LinksPermissionCard(
                        onClick = {
                            if (!env.openSupportedLinksSettings()) toaster.show("无法打开设置页")
                        },
                    )
                }
                item(key = "tips") { TipsCard(tips = PermissionTips) }
            }

            item(key = "warning") { ErrataCard(text = OpenIdWarning) }

            item(key = "accounts-title") {
                SmallTitle(text = "已保存 ${accounts.size} 个账号")
            }
            items(items = accounts, key = { it.openId }) { account ->
                ReorderableItem(state = reorderableState, key = account.openId) { _ ->
                    val interactionSource = remember { MutableInteractionSource() }
                    MeishiAccountCard(
                        account = account,
                        onClick = {
                            managedAccount = account
                            showAccountSheet = true
                        },
                        modifier = Modifier
                            .padding(horizontal = MeishiSidePadding)
                            .padding(bottom = MeishiCardGap)
                            // 整张卡都是拖动手柄：长按任意位置都能开始排序。
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                interactionSource = interactionSource,
                            ),
                    )
                }
            }

            // 两组说明各自合并成一张卡，都挂在「使用说明」下面，不再单独给领取时机开分组标题。
            item(key = "rules-title") { SmallTitle(text = "使用说明") }
            item(key = "rules") { RuleCard(text = UsageRules.joinToString("\n")) }

            item(key = "gift-title") { SmallTitle(text = "礼包内容") }
            items(items = GiftItems, key = { "gift/${it.title}" }) { gift ->
                MeishiGiftRow(
                    title = gift.title,
                    description = gift.description,
                    icon = gift.icon,
                    modifier = Modifier
                        .padding(horizontal = MeishiSidePadding)
                        .padding(bottom = MeishiCardGap),
                )
            }
        }
    }
}

/**
 * 顶部的「允许打开受支持的链接」入口卡：图标、标题、说明、右箭头，同 Java 版那一张。
 */
@Composable
private fun LinksPermissionCard(onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MeishiSidePadding)
            .padding(bottom = MeishiCardGap),
        cornerRadius = MeishiCardCornerRadius,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "允许打开受支持的链接",
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = "点击以快速配置",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

/**
 * 一组说明文字：几条规则用换行拼起来，整段放在一张卡里。
 *
 * 字号取 BasicComponent 的 summary（body2），颜色取它的 title 色：
 * 一整段读下来要跟「标题」一样清楚，不能再淡成变体色。
 */
@Composable
private fun RuleCard(text: String) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MeishiSidePadding)
            .padding(bottom = MeishiCardGap),
        cornerRadius = MeishiCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            // BasicComponentDefaults.titleColor() 默认就是 onBackground。
            color = MiuixTheme.colorScheme.onBackground,
        )
    }
}
