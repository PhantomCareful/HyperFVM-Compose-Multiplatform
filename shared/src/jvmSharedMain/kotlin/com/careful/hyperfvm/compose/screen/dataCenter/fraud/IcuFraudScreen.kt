package com.careful.hyperfvm.compose.screen.dataCenter.fraud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.card.importantCardColors
import com.careful.hyperfvm.compose.component.card.statusCardAccentColor
import com.careful.hyperfvm.compose.component.card.statusCardBackgroundColor
import com.careful.hyperfvm.compose.data.fraud.FraudAccount
import com.careful.hyperfvm.compose.data.fraud.FraudRepository
import com.careful.hyperfvm.compose.data.fraud.FraudVictim
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.Toaster
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.ui.isTabletLayout
import kotlinx.coroutines.CancellationException
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import com.careful.hyperfvm.compose.ui.AppTopBarIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 详情页的四种状态：查询中、已拿到数据、接口明确回答没有记录、查询失败。 */
private sealed interface IcuFraudState {
    data object Loading : IcuFraudState
    data class Ready(val account: FraudAccount) : IcuFraudState
    data object NotFound : IcuFraudState
    data class Failed(val message: String) : IcuFraudState
}

/**
 * 查黑系统的骗子详情页，对应 Java 版 `IcuFraudActivity`。
 *
 * 标题用「骗子详情」，下面拆成两个板块：
 * - 骗子信息：QQ 号用一张 Important 卡承载六个字段（与 Java 版同配色）；
 * - 行骗记录：每条记录一张卡，受害者、金额、区服、行骗类型、日期与备注分层排布；
 *   没有记录时整个板块隐藏，与 Java 版一致。
 *
 * 路由只带 QQ 号：优先读查询缓存，进程重建后缓存为空则自己重查。
 * 请求在转场期间就发出（IO 线程，不占主线程），但整页内容要等转场结束后的下一帧
 * 才渲染；转场动画期间只显示查询中的指示器占位。
 */
@Composable
fun IcuFraudScreen(qq: String, onBack: () -> Unit) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    var reloadToken by remember(qq) { mutableIntStateOf(0) }
    var state by remember(qq) { mutableStateOf<IcuFraudState>(IcuFraudState.Loading) }

    // 网络请求在转场期间就发出，它跑在 IO 线程，不会和转场动画抢主线程；
    // 但整页内容的渲染要等转场结束后的下一帧，避免列表重组与转场动画同帧抢帧。
    // ready 一旦置 true 就不再回退：返回转场时详情内容不会突然消失。
    val transitionRunning = LocalNavTransitionScope.current.isRunning
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(transitionRunning) {
        if (transitionRunning || contentReady) return@LaunchedEffect
        // 多等一帧：转场收尾那一帧仍在播旧内容，同帧重组会抢帧。
        withFrameNanos { }
        contentReady = true
    }

    LaunchedEffect(qq, reloadToken) {
        val cached = FraudRepository.cachedAccount(qq)
        if (cached != null) {
            state = IcuFraudState.Ready(cached)
            return@LaunchedEffect
        }
        state = IcuFraudState.Loading
        state = try {
            val account = FraudRepository.query(qq)
            if (account == null) {
                IcuFraudState.NotFound
            } else {
                IcuFraudState.Ready(account)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            IcuFraudState.Failed(e.message ?: "查询异常，请稍后重试。")
        }
    }

    // 转场未结束前一律按查询中渲染：请求可能已经回来，但整页内容要等页面静止再上场。
    val visibleState = if (contentReady) state else IcuFraudState.Loading

    ScreenScaffold(
        title = "骗子详情",
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        actions = {
            // 复制按钮挂在顶栏右侧；加载 / 无记录 / 失败时没有可复制的内容，不显示。
            val ready = visibleState as? IcuFraudState.Ready
            if (ready != null) {
                AppTopBarIconButton(onClick = { copyFraudInfo(env, toaster, ready.account) }) {
                    Icon(
                        imageVector = MiuixIcons.Copy,
                        contentDescription = "复制骗子信息",
                    )
                }
            }
        },
    ) { padding ->
        when (visibleState) {
            IcuFraudState.Loading -> FraudLoading(padding)
            IcuFraudState.NotFound -> FraudNotFound(padding)
            is IcuFraudState.Failed -> FraudFailed(
                padding = padding,
                message = visibleState.message,
                onRetry = { reloadToken += 1 },
            )

            is IcuFraudState.Ready -> FraudDetail(padding = padding, account = visibleState.account)
        }
    }
}

/**
 * 查询中：转场占位与真实查询全程只用这一种形态，不再在加载中途切换指示器或文字。
 */
@Composable
private fun FraudLoading(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 样式同 miuix demo 里圆形指示器的最后一个：全部用默认值（20dp 细环 + 轨道点）。
        InfiniteProgressIndicator()
        Text(
            text = "查询中",
            modifier = Modifier.padding(top = 12.dp),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

/** 接口明确回答「没有记录」：只给提示卡；重查也不会有结果，所以不给重试按钮。 */
@Composable
private fun FraudNotFound(padding: PaddingValues) {
    // 配色对齐防御卡概览大卡：背景取状态卡绿，文字用那卡图标的绿色。
    val colors = CardDefaults.defaultColors(
        color = statusCardBackgroundColor(),
        contentColor = statusCardAccentColor(),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        TipsCard(
            tips = "该 QQ 号当前没有行骗记录。",
            outerPadding = PaddingValues(0.dp),
            colors = colors,
        )
    }
}

/** 查询失败（网络、证书、解析等不定因素）：给一张提示卡和重试入口，而不是停在空白页。 */
@Composable
private fun FraudFailed(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        TipsCard(tips = message, outerPadding = PaddingValues(0.dp))
        TextButton(
            text = "重新查询",
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}

@Composable
private fun FraudDetail(padding: PaddingValues, account: FraudAccount) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "infoTitle") { SmallTitle("骗子信息") }
        item(key = "info") { FraudInfoCard(account) }
        if (account.victims.isNotEmpty()) {
            item(key = "victimsTitle") {
                SmallTitle("行骗记录（共 ${account.victims.size} 条）")
            }
            itemsIndexed(
                items = account.victims,
                key = { index, _ -> "victim-$index" },
            ) { _, victim -> VictimCard(victim) }
        }
    }
}

/**
 * 骗子信息卡：Important 配色与 Java 版一致。
 *
 * 第一行用大字展示 QQ 号与行骗总金额；两个时间在平板上一行两列，
 * 手机上拆成上下两行（标题都在内容上方），保证精确到秒的时间串不被截断；
 * 两个次数并排一行。整张卡带 miuix 原生的 showIndication 按压阴影。
 */
@Composable
private fun FraudInfoCard(account: FraudAccount) {
    val colors = importantCardColors()
    val contentColor = colors.contentColor
    val tablet = isTabletLayout()

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        cornerRadius = 20.dp,
        colors = colors,
        showIndication = true,
        // 与 Java 版一致：卡片可点但没有业务动作，只负责按压阴影。
        onClick = {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FraudFieldColumn(
                label = "QQ号",
                value = account.qq,
                contentColor = contentColor,
                modifier = Modifier.weight(1f),
                large = true,
            )
            FraudFieldColumn(
                label = "行骗总金额",
                value = account.fraudAmount,
                contentColor = contentColor,
                modifier = Modifier.weight(1f),
                large = true,
                alignEnd = true,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = contentColor.copy(alpha = 0.16f),
        )
        if (tablet) {
            // 平板宽度足够：「录入时间」和「最后行骗时间」一行两列。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                FraudFieldColumn(
                    label = "录入时间",
                    value = account.recordTime,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                )
                FraudFieldColumn(
                    label = "最后行骗时间",
                    value = account.lastFraudTime,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                    alignEnd = true,
                )
            }
        } else {
            // 手机放不下两列完整时间串：上下两行，标题统一放在内容上方。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            ) {
                FraudFieldColumn(
                    label = "录入时间",
                    value = account.recordTime,
                    contentColor = contentColor,
                    modifier = Modifier.fillMaxWidth(),
                )
                FraudFieldColumn(
                    label = "最后行骗时间",
                    value = account.lastFraudTime,
                    contentColor = contentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (tablet) 0.dp else 12.dp,
                    bottom = 14.dp,
                ),
        ) {
            FraudFieldColumn(
                label = "行骗次数",
                value = "${account.fraudCount}次",
                contentColor = contentColor,
                modifier = Modifier.weight(1f),
            )
            FraudFieldColumn(
                label = "不确定金额的行骗次数",
                value = "${account.uncertainAmountCount}次",
                contentColor = contentColor,
                modifier = Modifier.weight(1f),
                alignEnd = true,
            )
        }
    }
}

/** 卡片内的「小标签 + 数值」列；[large] 用于 QQ 号 / 金额这类首行大字。 */
@Composable
private fun FraudFieldColumn(
    label: String,
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    alignEnd: Boolean = false,
) {
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            color = contentColor.copy(alpha = 0.6f),
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            textAlign = textAlign,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 2.dp),
            color = contentColor,
            fontSize = if (large) {
                MiuixTheme.textStyles.title2.fontSize
            } else {
                MiuixTheme.textStyles.headline1.fontSize
            },
            fontWeight = FontWeight.Medium,
            // 长 QQ 号/金额宁可自动缩字号，也不要被省略号截断。
            autoSize = if (large) LargeValueAutoSize else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
    }
}

/** 首行大字：17sp～24sp 之间自动收缩。 */
private val LargeValueAutoSize = TextAutoSize.StepBased(
    minFontSize = 17.sp,
    maxFontSize = 24.sp,
    stepSize = 1.sp,
)

/** 复制整张骗子信息卡：格式沿用「一行一个字段，末行为数据来源」。 */
private fun copyFraudInfo(env: AppEnv, toaster: Toaster, account: FraudAccount) {
    env.copyToClipboard(account.toCopyText())
    toaster.show("骗子信息已复制")
}

/** 复制用的纯文本：一行一个字段，最后一行为数据来源。 */
private fun FraudAccount.toCopyText(): String = listOf(
    "QQ号：$qq",
    "行骗次数：${fraudCount}次",
    "行骗总金额：$fraudAmount",
    "不确定金额的行骗次数：${uncertainAmountCount}次",
    "录入时间：$recordTime",
    "最后行骗时间：$lastFraudTime",
    "数据来自【石头卫士-骗子查询】",
).joinToString("\n")

/**
 * 一条行骗记录：受害者与金额做头部强调，区服（平台+区服拼接）/ 行骗类型 / 日期是次级信息，
 * 备注非空时再单独一块（Java 版空白备注也会占一行，这里直接隐藏）。
 */
@Composable
private fun VictimCard(victim: FraudVictim) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(),
        showIndication = true,
        // 同上：只有按压阴影，没有点击动作。
        onClick = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FraudField(
                    label = "受害者QQ",
                    value = victim.victimQq,
                    modifier = Modifier.weight(1f),
                    emphasize = true,
                )
                FraudField(
                    label = "被骗金额",
                    value = victim.amount,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    alignEnd = true,
                    emphasize = true,
                    // 金额未知时没有强调的必要，退回普通色。
                    valueColor = if (victim.amount == "未知") {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    } else {
                        MiuixTheme.colorScheme.error
                    },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MiuixTheme.colorScheme.dividerLine,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                FraudField(
                    // 平台与区服已在数据层拼好，形如「4399-75」。
                    label = "区服",
                    value = victim.serverArea,
                    modifier = Modifier.weight(1f),
                )
                FraudField(
                    label = "行骗类型",
                    value = victim.fraudType,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                )
            }
            FraudField(
                label = "录入时间",
                value = victim.fraudTime,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (victim.remark.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MiuixTheme.colorScheme.dividerLine,
                )
                FraudField(label = "备注", value = victim.remark)
            }
        }
    }
}

/** 详情页的「小标签 + 值」字段：标签用摘要色，值用正文色。 */
@Composable
private fun FraudField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    emphasize: Boolean = false,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 2.dp),
            color = valueColor,
            // 次级字段与骗子信息卡统一用 headline1，两处时间/字段的字号看起来才一致。
            fontSize = if (emphasize) {
                MiuixTheme.textStyles.title2.fontSize
            } else {
                MiuixTheme.textStyles.headline1.fontSize
            },
            fontWeight = FontWeight.Medium,
            // 头部大字（受害者QQ/被骗金额）过长时自动缩字号；备注等正文仍可换行。
            autoSize = if (emphasize) LargeValueAutoSize else null,
            maxLines = if (emphasize) 1 else Int.MAX_VALUE,
            overflow = if (emphasize) TextOverflow.Ellipsis else TextOverflow.Clip,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        )
    }
}
