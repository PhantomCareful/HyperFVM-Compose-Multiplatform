package com.careful.hyperfvm.compose.component.meishi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.meishi.MeishiAccount
import com.careful.hyperfvm.compose.data.meishi.MeishiAccounts
import com.careful.hyperfvm.compose.data.meishi.MeishiFetchResult
import com.careful.hyperfvm.compose.data.meishi.MeishiNetwork
import com.careful.hyperfvm.compose.data.meishi.MeishiStore
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import com.careful.hyperfvm.compose.ui.AppBottomSheet
import com.careful.hyperfvm.compose.ui.AppDialog
import com.careful.hyperfvm.compose.ui.AppDialogButton
import com.careful.hyperfvm.compose.ui.AppDialogButtonRole
import com.careful.hyperfvm.compose.ui.sheetCardColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 弹层里字段标题的内边距：与仪表盘详情弹层的分组标题一致，左端对齐卡片里的正文。 */
private val SheetLabelMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** 弹层内容默认的底部留白（导航栏安全区之外），与仪表盘详情弹层一致。 */
private val SheetBottomPadding = 16.dp

/** 弹层里按钮之间的间距，与仪表盘详情弹层的按钮间隔一致。 */
private val SheetButtonGap = 8.dp

/** 解析结果卡的显隐动画：展开时数据已经在了，只需一段短促的推挤。 */
private const val RevealDurationMillis = 220
private const val HideDurationMillis = 160

/** 「正在解析」的固定显示时长：网络再快也让这一屏留满，免得弹层一闪而过。 */
private const val ParsingRevealMillis = 1000L

/** 链接输入框的提示文案，同 Java 版 `dialog_tips_meishi_wechat_add_url`，作为输入框的内嵌占位文字。 */
private const val AddLinkHint = "请输入链接，形如：\nhttp://meishi.wechat.123u.com/meishi/index?openid=XXXXX"

/** 从相册 / 微信跳进来但解析失败时的统一说法。 */
private const val LinkNotRecognized = "您提供的链接可能有误，没有识别到相关绑定信息"

/**
 * 入库并提示：openid 已在表里就只刷新角色名，提示随之换成「已更新」。
 *
 * 添加链接弹层与链接中转弹层共用这一份，两种提示的说法才不会两处各写一遍。
 */
private fun MeishiStore.saveAccount(openId: String, info: MeishiFetchResult.Success, toaster: Toaster) {
    val updated = upsert(openId, info.serverName, info.playerName)
    toaster.show(if (updated) "已更新账号信息" else "添加成功")
}

/**
 * 添加链接弹层：链接输入框 + 测试 / 添加 / 关闭三个按钮。
 *
 * 测试成功后在输入框下方展开两张卡（角色 ID、所在区服），失败则收起。
 * 与 Java 版一致：只有点「添加链接」才会入库，点「测试链接」只做展示。
 */
@Composable
internal fun MeishiAddLinkSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val store = remember(env) { MeishiAccounts.of(env) }
    val scope = rememberCoroutineScope()
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    // 最近一次解析出的结果与它对应的 openid：两样一起记，点「添加链接」时才能确认结果没过期。
    // 隐藏时只清 openid：结果本身留到弹层收起，否则收起动画还没播完，卡里就没内容可画了。
    var shownResult by remember { mutableStateOf<MeishiFetchResult.Success?>(null) }
    var shownOpenId by remember { mutableStateOf<String?>(null) }

    /** 入库并提示，然后收起弹层。 */
    fun save(openId: String, info: MeishiFetchResult.Success) {
        store.saveAccount(openId, info, toaster)
        onDismissRequest()
    }

    /** 收起解析结果：链接不合法或为空时，上一轮的两张卡已经和这次输入无关了。 */
    fun clearTested() {
        shownOpenId = null
    }

    fun submit(testOnly: Boolean) {
        if (busy) return
        val text = input.trim()
        if (text.isEmpty()) {
            clearTested()
            toaster.show("链接不能为空")
            return
        }
        val openId = MeishiNetwork.parseOpenId(text)
        if (openId == null) {
            clearTested()
            toaster.show("链接格式不正确")
            return
        }
        // 刚测过同一条链接：直接复用结果，少一次请求。
        val cached = shownResult
        if (!testOnly && cached != null && shownOpenId == openId) {
            save(openId, cached)
            return
        }
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { MeishiNetwork.fetchPlayerInfo(openId) }
            busy = false
            when (result) {
                is MeishiFetchResult.Success -> {
                    shownOpenId = openId
                    shownResult = result
                    // 测试只把结果显示到下面两张卡里，不再额外弹一句「欢迎您」。
                    if (!testOnly) save(openId, result)
                }
                // 失败一律收起两张卡：上一轮的解析结果和这次输入已经没关系了。
                MeishiFetchResult.NotParsed -> {
                    clearTested()
                    toaster.show("解析失败\n未找到区服和角色信息")
                }
                MeishiFetchResult.ServerError -> {
                    clearTested()
                    toaster.show("获取信息失败\n服务器无响应")
                }
                MeishiFetchResult.NetworkError -> {
                    clearTested()
                    toaster.show("网络错误\n无法连接服务器")
                }
            }
        }
    }

    AppBottomSheet(
        show = show,
        title = "添加链接",
        onDismissRequest = onDismissRequest,
        onDismissFinished = {
            // 弹层彻底收起后才清空：收起动画期间内容还得在。
            input = ""
            shownResult = null
            shownOpenId = null
            busy = false
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SheetBottomPadding + navigationBarBottom),
        ) {
            // 提示直接当输入框里的占位文字：一有输入就让位，不再单独占一行高度。
            // 占位色跟之前贴在框外时一样是变体色，免得看起来像已经填好了内容。
            // 请求期间用 readOnly 而不是 enabled=false：两个都会拦住输入，但后者会让输入框
            // 掉焦点、收起键盘，结果回来再重新点回去。
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                // 弹层里的输入框与其余内容卡同一种底色：轻透磨砂下是灰调 secondaryContainer，
                // 柔光玻璃下换成不透明的 surfaceContainer，才不会跟着玻璃面板一起发透。
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = sheetCardColor(),
                    labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                ),
                label = AddLinkHint,
                useLabelAsPlaceholder = true,
                readOnly = busy,
                minLines = 3,
                maxLines = 5,
            )
            AnimatedVisibility(
                visible = shownOpenId != null,
                enter = expandVertically(animationSpec = tween(RevealDurationMillis)) +
                    fadeIn(animationSpec = tween(RevealDurationMillis)),
                exit = shrinkVertically(animationSpec = tween(HideDurationMillis)) +
                    fadeOut(animationSpec = tween(HideDurationMillis)),
            ) {
                // 收起动画期间显示的仍是上一轮的结果。
                val shown = shownResult
                if (shown != null) {
                    Column {
                        Spacer(modifier = Modifier.height(MeishiCardGap))
                        MeishiFieldCard(label = "角色ID", value = shown.playerName)
                        Spacer(modifier = Modifier.height(MeishiCardGap))
                        MeishiFieldCard(label = "所在区服", value = shown.serverName)
                    }
                }
            }
            // 输入框 / 解析结果卡到按钮组的间距，与按钮之间保持一致。
            Spacer(modifier = Modifier.height(SheetButtonGap))
            // 两个按钮在请求期间不再置灰：禁用的瞬间会刷成灰底灰字，结果回来再刷回主色，
            // 看起来就像按钮在闪。重复点击由 submit 里的 busy 守卫拦下。
            AppDialogButton(
                text = "测试链接",
                onClick = { submit(testOnly = true) },
                modifier = Modifier.fillMaxWidth(),
                role = AppDialogButtonRole.Primary,
            )
            Spacer(modifier = Modifier.height(SheetButtonGap))
            AppDialogButton(
                text = "添加链接",
                onClick = { submit(testOnly = false) },
                modifier = Modifier.fillMaxWidth(),
                role = AppDialogButtonRole.Primary,
            )
            Spacer(modifier = Modifier.height(SheetButtonGap))
            AppDialogButton(
                text = "关闭窗口",
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 单个账号的管理弹层：链接、角色 ID、所在区服，加上保存 / 关闭 / 删除三个按钮。
 *
 * 只有角色 ID 可以改——玩家改名之后，角色页返回的仍然是旧名字。
 * 链接与区服都由 openid 决定，改不了。
 *
 * 删除不在弹层里自己做：[onDelete] 交给页面，因为开了保护锁要先验一次身份，
 * 删完还要弹一个可撤销的 Snackbar，那两件事都归页面管。
 */
@Composable
internal fun MeishiAccountSheet(
    account: MeishiAccount?,
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: (MeishiAccount) -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val store = remember(env) { MeishiAccounts.of(env) }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 退出动画期间 account 仍然有值（调用方只翻 show），这里不用另存一份。
    val current = account ?: return

    var playerName by remember(current.openId) { mutableStateOf(current.playerName) }

    AppBottomSheet(
        show = show,
        title = "账号信息",
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SheetBottomPadding + navigationBarBottom),
        ) {
            SmallTitle(text = "链接", insideMargin = SheetLabelMargin)
            MeishiTextCard(
                text = current.link,
                // 链接没法手打，想挪到别处只能靠复制。
                onCopy = {
                    if (env.copyToClipboard(current.link)) {
                        toaster.show("已复制链接")
                    } else {
                        toaster.show("复制失败")
                    }
                },
            )
            SmallTitle(text = "角色ID", insideMargin = SheetLabelMargin)
            TextField(
                value = playerName,
                onValueChange = { playerName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // 与添加链接弹层的输入框同款：弹层里统一用内容卡底色。
                colors = TextFieldDefaults.textFieldColors(backgroundColor = sheetCardColor()),
            )
            SmallTitle(text = "所在区服", insideMargin = SheetLabelMargin)
            MeishiTextCard(text = current.serverName.ifBlank { UnknownServerName })
            // 与添加链接弹层保持一致：最后一组内容到按钮组的间距，等于按钮之间的间距。
            Spacer(modifier = Modifier.height(SheetButtonGap))
            AppDialogButton(
                text = "保存更改",
                onClick = {
                    store.rename(current.openId, playerName.trim())
                    toaster.show("已保存更改")
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
                role = AppDialogButtonRole.Primary,
            )
            Spacer(modifier = Modifier.height(SheetButtonGap))
            AppDialogButton(
                text = "关闭窗口",
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(SheetButtonGap))
            AppDialogButton(
                text = "删除账号",
                onClick = { onDelete(current) },
                modifier = Modifier.fillMaxWidth(),
                // 删除不可逆，用玻璃弹层的破坏性语气（勘误红文字）而不是普通正文色。
                role = AppDialogButtonRole.Destructive,
            )
        }
    }
}

/**
 * 从受支持的链接跳进来时的确认弹层。
 *
 * 收到链接就直接展开：先显示一段「正在解析」（至少 [ParsingRevealMillis] 毫秒，不论请求多快），
 * 解析成功后标题换成「请确认信息是否正确」并展开两张卡（角色 ID、所在区服），确认即入库；
 * 解析不出来就收起弹层、改弹一个一般弹窗说明链接可能有误。
 *
 * [onFinished] 给中转页用：弹层彻底收起（失败时还要等失败弹窗也关掉）之后才调，宿主再结束自己。
 */
@Composable
internal fun MeishiLinkReceiverSheet(
    link: String?,
    onDismissRequest: () -> Unit,
    onFinished: () -> Unit = {},
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val store = remember(env) { MeishiAccounts.of(env) }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 首帧就按「正在解析」展开：链接已经在手上了，没有先弹一个空壳的理由。
    var parsing by remember { mutableStateOf(link != null) }
    var ready by remember { mutableStateOf<Pair<String, MeishiFetchResult.Success>?>(null) }
    // 失败分两步走：先把弹层收起来，收起动画播完（onDismissFinished）才亮失败弹窗。
    var failurePending by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    /** 解析失败弹窗的收尾：关掉弹窗、清掉待处理链接；窗口等退出动画播完再关。 */
    fun closeFailure() {
        failed = false
        onDismissRequest()
    }

    LaunchedEffect(link) {
        // 弹层关闭后 link 置空：等收起动画结束、onDismissFinished 再清状态。
        if (link == null) return@LaunchedEffect
        parsing = true
        ready = null
        failurePending = false
        failed = false

        val openId = MeishiNetwork.parseOpenId(link)
        // 请求和「正在解析」并行跑：网络再快也把指示器留满 1 秒，免得弹层一闪而过。
        val request = openId?.let { id ->
            async(Dispatchers.IO) { MeishiNetwork.fetchPlayerInfo(id) }
        }
        delay(ParsingRevealMillis)
        val result = request?.await()
        parsing = false
        if (openId != null && result is MeishiFetchResult.Success) {
            ready = openId to result
        } else {
            failurePending = true
        }
    }

    AppBottomSheet(
        show = link != null && (parsing || ready != null),
        // 结果出来之后标题换成一句问句：这时候用户要做的正是核对信息。
        title = if (ready != null) "请确认信息是否正确" else "添加链接",
        onDismissRequest = onDismissRequest,
        onDismissFinished = {
            // 到这一步收起动画已经播完，结果卡可以清了（退出期间它们还得在）。
            ready = null
            if (failurePending) {
                // 弹层已经收好了，接着亮失败弹窗；收尾交给弹窗自己。
                failurePending = false
                failed = true
            } else {
                parsing = false
                // 中转页靠这一下收尾：收起动画播完才关窗口，否则退出动画会被砍断。
                onFinished()
            }
        },
    ) {
        AnimatedContent(
            targetState = ready,
            // 只有「转圈 ⇄ 结果卡」这两类内容之间才做过渡。
            contentKey = { it != null },
            transitionSpec = {
                // 交叉淡化 + 容器高度跟着一起长高：弹层贴底边往上长，不会先窜高一截再补内容。
                fadeIn(tween(RevealDurationMillis)) togetherWith
                    fadeOut(tween(HideDurationMillis)) using SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ -> tween(RevealDurationMillis) },
                )
            },
            label = "meishiReceiverContent",
        ) { parsed ->
            if (parsed == null) {
                // 解析中：与骗子名单、公告页的加载态同款——默认尺寸的细环，说明文字在下方。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = SheetBottomPadding + navigationBarBottom),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InfiniteProgressIndicator()
                    Text(
                        text = "正在解析",
                        modifier = Modifier.padding(top = 12.dp),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }
            } else {
                // 解构一次：下面的两张卡与「添加」都要用。
                val (openId, info) = parsed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SheetBottomPadding + navigationBarBottom),
                ) {
                    MeishiFieldCard(label = "角色ID", value = info.playerName)
                    Spacer(modifier = Modifier.height(MeishiCardGap))
                    MeishiFieldCard(label = "所在区服", value = info.serverName)
                    Spacer(modifier = Modifier.height(SheetBottomPadding))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AppDialogButton(
                            text = "取消",
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        AppDialogButton(
                            text = "添加",
                            onClick = {
                                store.saveAccount(openId, info, toaster)
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            role = AppDialogButtonRole.Primary,
                        )
                    }
                }
            }
        }
    }

    AppDialog(
        show = failed,
        title = "添加链接",
        onDismissRequest = { closeFailure() },
        onDismissFinished = {
            // 失败提示也走退出动画，播完再把窗口关掉。
            onFinished()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = LinkNotRecognized,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            AppDialogButton(
                text = "好的",
                onClick = { closeFailure() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                role = AppDialogButtonRole.Primary,
            )
        }
    }
}
