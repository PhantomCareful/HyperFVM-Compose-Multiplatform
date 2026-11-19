package com.careful.hyperfvm.compose.screen.datapack

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.effect.BgEffectBackground
import com.careful.hyperfvm.compose.data.datapack.DataPackState
import com.careful.hyperfvm.compose.ui.rememberAppBlurBackdrop
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode

/** 按钮离左右下三边的距离，三边等距。 */
private val SidePadding = 24.dp

/** 主标题字号：在 title1（32sp）的基础上再放大一档；副标题沿用 body1。 */
private const val TitleScale = 1.8f

/** 标题玻璃文字的混合参数，与关于页标题同一套配方（Lab 通道参与混色）。 */
private val TitleBlendLight = listOf(
    BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
    BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
    BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
)
private val TitleBlendDark = listOf(
    BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
    BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
    BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
)

/**
 * 首次启动 / 数据包缺失时的引导页。
 *
 * 应用的所有图片与概率公示数据都在数据包里，没有它主界面只会是空白，
 * 所以这一页在数据包就绪前替代整个主界面。背景与会话页同款：整页铺一层
 * 背景效果，标题用前景模糊取玻璃质感；按钮贴底、左右下三边等距。
 *
 * 下载与准备全部完成后不自动进主界面：停在「一切就绪」，由用户点按钮进入
 * （[onEnter]），下载中途失败则给「重试」回到 [onDownload]。
 *
 * [update] 为 true 时是已装包用户的「数据包更新」页：从仪表盘提示卡进入，
 * 初始不是 [DataPackState.Missing] 而是 [DataPackState.Ready]（本地有旧包），
 * 所以开头的提示阶段单独算一档（「又见面了」）；点下载后与首次更新完全一致。
 * 下载 / 准备期间再按返回也没用：本页靠 [NavigationBackHandler] 吞掉返回事件，
 * 入口路由也会把侧滑关掉。
 */
@Composable
fun DataPackSetupScreen(
    state: DataPackState,
    onDownload: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
    update: Boolean = false,
) {
    val backdrop = rememberAppBlurBackdrop()
    val isInDark = isInDarkTheme()
    val titleBlend = remember(isInDark) {
        if (isInDark) TitleBlendDark else TitleBlendLight
    }

    // 本次会话是否已经点过下载：更新模式下靠它区分「又见面了」和在下载后回到的 Ready。
    var started by remember { mutableStateOf(false) }
    // 点过下载、但仓库状态还没离开 Ready 的极短空窗：界面继续按「下载中」显示，
    // 免得先闪一下「一切就绪」；状态一旦进入下载 / 准备就清掉这个标记。
    var clickPending by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (clickPending && state !is DataPackState.Ready) clickPending = false
    }
    val phase = phaseOf(state, update, started, clickPending)
    val beginDownload = {
        started = true
        clickPending = true
        onDownload()
    }

    // 下载 / 准备期间禁止返回：本页在导航栈顶时，这里的处理器晚于 NavDisplay 注册、
    // 优先级更高，系统返回与预测性手势都会被它吞掉；没点下载之前则不拦，照常返回。
    val locked = update && (phase == PackPhase.Downloading || phase == PackPhase.Preparing)
    if (update) {
        val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = locked,
            onBackCompleted = {},
        )
    }

    BgEffectBackground(
        dynamicBackground = true,
        isOs3Effect = true,
        isFullSize = true,
        modifier = modifier.fillMaxSize(),
        // 取样层录的是背景效果那一层渐变；标题的模糊节点是它的兄弟，不能落进它的子树。
        bgModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = SidePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // 标题不参与阶段过渡：模糊标题每帧跟着缩放、位移会让整层重新渲染，
            // 切阶段时就是那一下卡顿；这里固定成一个节点直接换字，只让下面的内容动。
            DataPackTitle(text = textOf(state, phase).title, backdrop = backdrop, blend = titleBlend)
            // 副标题与状态圈单独做淡入淡出：新内容几乎立刻开始出现、旧内容快速让位，
            // 中间不留空档；高度变化仍由 SizeTransform 补间，标题不会被拽着跳。
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (
                        fadeIn(tween(durationMillis = 200, delayMillis = 40)) +
                            slideInVertically(tween(durationMillis = 200, delayMillis = 40)) { it / 10 }
                        ).togetherWith(
                        fadeOut(tween(durationMillis = 90)),
                    ).using(
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(durationMillis = 220) },
                        ),
                    )
                },
                contentAlignment = Alignment.Center,
                label = "datapackPhase",
            ) { phase ->
                val text = textOf(state, phase)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 副标题为空串时这一行整个不出现，也不会在标题和进度之间留下空隙。
                    if (text.subtitle.isNotEmpty()) {
                        Text(
                            text = text.subtitle,
                            modifier = Modifier.padding(top = 12.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = MiuixTheme.textStyles.body1.fontSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (phase.showsStatus) {
                        Spacer(Modifier.height(28.dp))
                        StatusOf(state = state, phase = phase)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            ActionOf(phase = phase, onDownload = beginDownload, onEnter = onEnter)
            Spacer(Modifier.height(SidePadding))
        }
    }
}

/** 标题：有模糊取样层时用前景模糊当字身（DstIn 取文字 alpha 当遮罩），否则退回纯色。 */
@Composable
private fun DataPackTitle(text: String, backdrop: LayerBackdrop?, blend: List<BlendColorEntry>) {
    Text(
        text = text,
        modifier = if (backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RoundedCornerShape(16.dp),
                blurRadius = 150f,
                colors = BlurDefaults.blurColors(blendColors = blend),
                contentBlendMode = ComposeBlendMode.DstIn,
            )
        } else {
            Modifier
        },
        color = MiuixTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = MiuixTheme.textStyles.title1.fontSize * TitleScale,
        textAlign = TextAlign.Center,
    )
}

/** 引导页的展示阶段：下载进度每 64 KB 刷一次，只有这个枚举变化才播过渡动画。 */
private enum class PackPhase { Checking, Missing, Update, Downloading, Preparing, Ready, Failed }

/** 有进度指示的阶段；其余阶段标题下面什么都不放。 */
private val PackPhase.showsStatus: Boolean
    get() = this == PackPhase.Checking || this == PackPhase.Downloading || this == PackPhase.Preparing

private fun phaseOf(
    state: DataPackState,
    update: Boolean,
    started: Boolean,
    clickPending: Boolean,
): PackPhase {
    if (update) {
        // 还没点下载：本地是旧包，停在前置提示。
        if (!started && state is DataPackState.Ready) return PackPhase.Update
        // 点过下载、状态还没切到 Downloading 的空窗，按下载中显示。
        if (clickPending && state is DataPackState.Ready) return PackPhase.Downloading
    }
    return when (state) {
        DataPackState.Checking -> PackPhase.Checking
        DataPackState.Missing -> PackPhase.Missing
        is DataPackState.Downloading -> PackPhase.Downloading
        DataPackState.Preparing -> PackPhase.Preparing
        DataPackState.Ready -> PackPhase.Ready
        is DataPackState.Error -> PackPhase.Failed
    }
}

/**
 * 引导页文案：主标题一行大字，副标题小字补充说明，两者都可以带换行。
 * 副标题是空串时整行不显示、不占位；改文案只需要动 [textOf] 里的字符串。
 */
private data class PhaseText(val title: String, val subtitle: String)

private fun textOf(state: DataPackState, phase: PackPhase): PhaseText = when (phase) {
    PackPhase.Checking -> PhaseText(title = "正在检查", subtitle = "")
    PackPhase.Missing -> PhaseText(
        title = "初次见面",
        subtitle = "请先下载数据包，App才能正常运作",
    )
    // 已装包用户的更新入口：标题与副标题换成「更新」的语气，流程其余部分不变。
    PackPhase.Update -> PhaseText(
        title = "又见面了",
        subtitle = "点击下面按钮，开始更新数据包",
    )
    PackPhase.Downloading -> PhaseText(title = "下载数据包", subtitle = "请勿关闭App")
    PackPhase.Preparing -> PhaseText(title = "数据准备中", subtitle = "很快就好")
    PackPhase.Ready -> PhaseText(title = "一切就绪~", subtitle = "开始体验激动人心的HyperFVM吧")
    // 失败原因由仓库给出（网络、校验、空间不足……），放进副标题。
    PackPhase.Failed -> PhaseText(
        title = "下载失败",
        subtitle = (state as? DataPackState.Error)?.message.orEmpty(),
    )
}

@Composable
private fun StatusOf(state: DataPackState, phase: PackPhase) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (phase) {
            PackPhase.Checking -> InfiniteProgressIndicator(color = Color.Black)
            PackPhase.Downloading -> DownloadStatus(state as? DataPackState.Downloading)
            PackPhase.Preparing -> PrepareStatus()
            else -> Unit
        }
    }
}

/** 下载进度：条子按字节块刷新，用补间动画让它平滑前进而不是一跳一跳。 */
@Composable
private fun DownloadStatus(state: DataPackState.Downloading?) {
    val progressColors = ProgressIndicatorDefaults.progressIndicatorColors(
        foregroundColor = Color.Black,
        backgroundColor = Color.Black.copy(alpha = 0.15f),
    )
    val target = if (state != null && state.total > 0) {
        (state.received.toFloat() / state.total).coerceIn(0f, 1f)
    } else {
        null
    }
    if (state == null || target == null) {
        InfiniteProgressIndicator(color = Color.Black)
        if (state != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatBytes(state.received),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 320, easing = LinearEasing),
        label = "datapackProgress",
    )
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = animated,
        colors = progressColors,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "${formatBytes(state.received)} / ${formatBytes(state.total)}（${(animated * 100).toInt()}%）",
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        textAlign = TextAlign.Center,
    )
}

/** 准备阶段只转圈：两个合集并发准备，完成数量对用户没有意义。 */
@Composable
private fun PrepareStatus() {
    InfiniteProgressIndicator(color = Color.Black)
}

/** 底部按钮的四种操作；null 表示当前阶段没有可点的东西（保留占位高度）。 */
private enum class PackAction(val label: String) {
    Download("立即下载"),
    Retry("点击重试"),
    Enter("开启新世界"),
}

/** 底部按钮：黑色半透明底、白字；出现与消失都带淡入上浮，内容不因按钮变化而跳动。 */
@Composable
private fun ActionOf(phase: PackPhase, onDownload: () -> Unit, onEnter: () -> Unit) {
    val action = when (phase) {
        PackPhase.Missing, PackPhase.Update -> PackAction.Download
        PackPhase.Failed -> PackAction.Retry
        PackPhase.Ready -> PackAction.Enter
        else -> null
    }
    Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = action,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 240, delayMillis = 80)) +
                    slideInVertically(tween(durationMillis = 240, delayMillis = 80)) { it / 3 })
                    .togetherWith(
                        fadeOut(tween(durationMillis = 140)) +
                            slideOutVertically(tween(durationMillis = 140)) { it / 3 },
                    )
            },
            contentAlignment = Alignment.Center,
            label = "datapackAction",
        ) { target ->
            when (target) {
                null -> Spacer(Modifier.fillMaxWidth().height(56.dp))
                PackAction.Enter -> ActionButton(action = target, onClick = onEnter)
                else -> ActionButton(action = target, onClick = onDownload)
            }
        }
    }
}

@Composable
private fun ActionButton(action: PackAction, onClick: () -> Unit) {
    TextButton(
        text = action.label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            color = Color.Black.copy(alpha = 0.55f),
            disabledColor = Color.Black.copy(alpha = 0.3f),
            textColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.6f),
        ),
    )
}

/** 字节数的简短展示：只保留一位小数，超过 1 GB 时换成 GB。 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format(java.util.Locale.CHINA, "%.1f GB", mb / 1024)
    } else {
        String.format(java.util.Locale.CHINA, "%.1f MB", mb)
    }
}
