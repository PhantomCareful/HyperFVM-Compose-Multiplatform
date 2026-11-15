package com.careful.hyperfvm.compose.screen.about

import androidx.compose.foundation.lazy.LazyColumn
import com.careful.hyperfvm.compose.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.effect.BgEffectBackground
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.component.dialog.UrlConfirmDialog
import com.careful.hyperfvm.compose.component.dialog.UrlTarget
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.rememberAppBlurBackdrop
import com.careful.hyperfvm.compose.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode

/** 顶部 logo 方块边长与圆角，对齐 demo 的 About。 */
private val LogoSize = 100.dp
private val LogoCornerRadius = 24.dp

/** 标题区在 LazyColumn 里的占位项 key，滚动进度要靠它算。 */
private const val LogoSpacerKey = "logoSpacer"

/**
 * ColorBlendToken 里的两条：卡片玻璃的混合参数色。
 * 这些颜色大多不是最终显示色 —— Lab / Luminosity / Overlay 都拿通道值当参数用。
 */
private val CardBlendLight = listOf(
    BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
    BlendColorEntry(Color(0xB3FFFFFF), BlurBlendMode.HardLight),
)
private val CardBlendDark = listOf(
    BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
    BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker),
)

/**
 * 关于页。结构照搬 miuix demo 的 AboutPage：
 *
 * - 整页铺一层背景效果（OS3 变体、动态配色、全屏），滚动时按进度淡出，但**不跟着滚动位移**；
 * - logo 区（图标 / 标题 / 版本号）是固定层，随滚动渐隐缩小，不参与列表滚动；
 * - 卡片在 LazyColumn 里，通过一个与 logo 等高的透明占位项从下方滑上来；
 * - 顶栏初始透明，滚到底才出模糊与标题。
 *
 * 背景效果和玻璃质感都依赖 RuntimeShader（Android 13+）。低版本由 [BgEffectBackground]
 * 自己回退成纯底色，图标退回纯色底盘，卡片退回不透明底色。
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val env = LocalAppEnv.current
    val versionName = env.appVersionName
    val versionCode = env.appVersionCode

    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    // 顶部占位项被滚过多少。驱动三件事：logo 渐隐、顶栏标题渐显、背景效果淡出。
    // 用 derivedStateOf 把逐帧的滚动读取挡在重组之外。
    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == LogoSpacerKey }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    // 顶栏的取样层录的是整块内容区；顶栏在 topBar 槽里，与它是兄弟节点。
    val backdrop = rememberAppBlurBackdrop()
    val collapsed by remember { derivedStateOf { scrollProgress == 1f } }
    val blurActive by remember(backdrop) {
        derivedStateOf { backdrop != null && scrollProgress == 1f }
    }

    CompositionLocalProvider(LocalBlurBackdrop provides backdrop) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AboutTopBar(
                    scrollBehavior = scrollBehavior,
                    scrollProgress = { scrollProgress },
                    blurActive = blurActive,
                    collapsed = collapsed,
                    onBack = onBack,
                )
            },
        ) { innerPadding ->
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                AboutContent(
                    innerPadding = innerPadding,
                    topAppBarScrollBehavior = scrollBehavior,
                    lazyListState = lazyListState,
                    scrollProgressProvider = { scrollProgress },
                    versionName = versionName,
                    versionCode = versionCode,
                )
            }
        }
    }
}

/**
 * 顶栏：初始整条透明，只留返回键；滚到底才有底色与模糊，标题同时淡入。
 * 与 demo 的 BlurredBar + SmallTopAppBar 一致。
 */
@Composable
private fun AboutTopBar(
    scrollBehavior: ScrollBehavior,
    scrollProgress: () -> Float,
    blurActive: Boolean,
    collapsed: Boolean,
    onBack: () -> Unit,
) {
    val settings = LocalAppSettings.current
    val surface = MiuixTheme.colorScheme.surface
    val backdrop = LocalBlurBackdrop.current
    val progressive = settings.blurStyle == 1
    val titleColor = MiuixTheme.colorScheme.onSurface.copy(
        alpha = ((scrollProgress() - 0.35f) / 0.65f).coerceIn(0f, 1f),
    )

    Box(
        modifier = if (blurActive && !progressive && backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(BlendColorEntry(color = surface.copy(0.8f))),
                ),
            )
        } else {
            Modifier
        },
    ) {
        if (blurActive && progressive && backdrop != null) {
            // 渐进模糊静止时几乎不可见，随顶栏收起逐渐显现。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // 小顶栏会把 ScrollBehavior 钉住，contentOffset 永远是 0；
                        // 这里改用整页滚动进度驱动，渐进模糊才会真的显现。
                        alpha = ((scrollProgress() - 0.5f) * 2f).coerceIn(0f, 1f)
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
        SmallTopAppBar(
            title = "关于",
            scrollBehavior = scrollBehavior,
            color = if (blurActive) Color.Transparent else if (collapsed) surface else Color.Transparent,
            titleColor = titleColor,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                    )
                }
            },
        )
    }
}

@Composable
private fun AboutContent(
    innerPadding: PaddingValues,
    topAppBarScrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgressProvider: () -> Float,
    versionName: String,
    versionCode: Long,
) {
    val settings = LocalAppSettings.current
    val isInDark = isInDarkTheme()

    // 内容侧的取样层：只录背景效果那一层渐变。logo 与卡片是它的兄弟，
    // 若复用顶栏那个 backdrop（录的是整块内容区），模糊节点会落在被录制的子树里，
    // 图层引用自己，RenderThread 栈溢出。
    val backdrop = rememberAppBlurBackdrop()

    val logoBlend = remember(isInDark) {
        if (isInDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
            )
        }
    }
    val cardBlend = if (isInDark) CardBlendDark else CardBlendLight

    val topInset = innerPadding.calculateTopPadding()
    val bottomInset = innerPadding.calculateBottomPadding()
    // 先给一个接近真值的初值，避免首帧占位太短、列表高度闪一下。
    var logoHeight by remember { mutableStateOf(300.dp) }

    val navigator = LocalNavigator.current
    // 外链确认框的状态提到列表外面，避免列表项被回收时弹窗一起销毁。
    var pendingUrl by remember { mutableStateOf<UrlTarget?>(null) }

    BgEffectBackground(
        // Effect Variant 固定 OS3，Dynamic Background 与 Full Screen Background 固定开启。
        dynamicBackground = true,
        isOs3Effect = true,
        isFullSize = true,
        modifier = Modifier.fillMaxSize(),
        bgModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
        // 与 demo 一致：滚得越深背景越淡，最后只剩 Scaffold 的底色。
        alpha = { (1f - scrollProgressProvider()).coerceIn(0f, 1f) },
    ) {
        AboutLogo(
            backdrop = backdrop,
            logoBlend = logoBlend,
            scrollProgressProvider = scrollProgressProvider,
            versionName = versionName,
            versionCode = versionCode,
            topPadding = topInset + 40.dp + 52.dp,
            onHeightChanged = { logoHeight = it },
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                // 顺序对齐 demo 的 pageScrollModifiers：震动 → 越界回弹 → 顶栏折叠。
                // 这些连接点挂在列表上而不是页面容器上，背景效果才不会跟着一起位移。
                .then(if (settings.scrollEndHaptic) Modifier.scrollEndHaptic(HapticFeedbackType.TextHandleMove) else Modifier)
                .overScrollVertical()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = topInset, bottom = bottomInset),
        ) {
            // 与 logo 区等高的透明占位：让卡片从 logo 下方滑上来。
            // demo 的公式：logo 高度 + 52（图标上方留白）+ 40（额外顶部）+ 126（标题下留白）。
            item(key = LogoSpacerKey) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(logoHeight + 52.dp + 40.dp + 126.dp),
                )
            }

            item(key = "about") {
                Box {
                    // 撑满一屏，保证短内容也能滚到底，顶栏的模糊与标题才有机会出现。
                    Spacer(Modifier.fillParentMaxHeight())
                    // 三节卡片共用同一套玻璃质感与配色。
                    val cardModifier = Modifier
                        .padding(horizontal = 12.dp)
                        .then(
                            if (backdrop != null) {
                                Modifier.textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(16.dp),
                                    blurRadius = 60f,
                                    colors = BlurDefaults.blurColors(blendColors = cardBlend),
                                )
                            } else {
                                Modifier
                            },
                        )
                    val cardColors = CardDefaults.defaultColors(
                        color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Column(modifier = Modifier.padding(bottom = bottomInset)) {
                        // 第一节：开发者、共建团。
                        Card(modifier = cardModifier, colors = cardColors) {
                            ArrowPreference(
                                title = "Phantom_Careful",
                                summary = "QQ空间1-8服·喵帕斯赛高",
                                startAction = {
                                    Image(
                                        modifier = Modifier.padding(end = 12.dp).size(50.dp).clip(CircleShape),
                                        painter = painterResource(Res.drawable.phantom_careful),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                    )
                                },
                                onClick = {
                                    pendingUrl = UrlTarget(
                                        title = "Github",
                                        subtitle = "Phantom_Careful",
                                        url = "https://github.com/PhantomCareful",
                                        iconRes = Res.drawable.ic_github,
                                    )
                                },
                            )
                            ArrowPreference(
                                title = "共建团",
                                summary = "HyperFVM也是这些同学们的作品",
                                onClick = { navigator.push(Route.CoContributorTeam) },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 第二节：致谢（游戏相关、App相关）。
                        Card(modifier = cardModifier, colors = cardColors) {
                            ArrowPreference(
                                title = "致谢-游戏相关",
                                summary = "HyperFVM使用了这些老师们的作品",
                                onClick = { navigator.push(Route.ThanksGame) },
                            )
                            ArrowPreference(
                                title = "致谢-App相关",
                                summary = "HyperFVM使用了这些开源项目",
                                onClick = { navigator.push(Route.ThanksApp) },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 第三节：更多（历史更新日志按需求不放进来）。
                        Card(modifier = cardModifier, colors = cardColors) {
                            ArrowPreference(
                                title = "项目地址",
                                summary = "本项目基于Apache-2.0协议开源",
                                onClick = {
                                    pendingUrl = UrlTarget(
                                        title = "Github",
                                        subtitle = "HyperFVM-Java",
                                        url = "https://github.com/PhantomCareful/HyperFVM-Compose-Multiplatform",
                                        iconRes = Res.drawable.ic_github,
                                    )
                                },
                            )
                            ArrowPreference(
                                title = "哔哩哔哩",
                                summary = "获取App最新开发动态",
                                onClick = {
                                    pendingUrl = UrlTarget(
                                        title = "哔哩哔哩",
                                        subtitle = "Phantom_Careful",
                                        url = "https://space.bilibili.com/360191288",
                                        iconRes = Res.drawable.ic_bilibili,
                                    )
                                },
                            )
                            ArrowPreference(
                                title = "腾讯频道",
                                summary = "获取相关通知、提交反馈或建议",
                                onClick = {
                                    pendingUrl = UrlTarget(
                                        title = "腾讯频道",
                                        subtitle = "HyperFVM交流社区",
                                        url = "https://pd.qq.com/s/el8bp2qra",
                                        iconRes = Res.drawable.ic_qq,
                                    )
                                },
                            )
                            ArrowPreference(
                                title = "QQ群组",
                                summary = "参与内测、直接和开发者对话、闲聊",
                                onClick = {
                                    pendingUrl = UrlTarget(
                                        title = "QQ群组",
                                        subtitle = "HyperFVM聊天小窝",
                                        url = "https://qm.qq.com/q/bJDJZnBg7C",
                                        iconRes = Res.drawable.ic_qq,
                                    )
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        UrlConfirmDialog(
            target = pendingUrl,
            onDismiss = { pendingUrl = null },
        )
    }
}

/**
 * 固定的 logo 区：图标、标题、版本号。
 *
 * 三者都在 [BgEffectBackground] 的 Box 里、LazyColumn 之外，所以不随列表滚动；
 * 只有滚动进度驱动的渐隐与轻微缩小。
 */
@Composable
private fun AboutLogo(
    backdrop: LayerBackdrop?,
    logoBlend: List<BlendColorEntry>,
    scrollProgressProvider: () -> Float,
    versionName: String,
    versionCode: Long,
    topPadding: Dp,
    onHeightChanged: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val logoShape = RoundedCornerShape(LogoCornerRadius)
        if (backdrop != null) {
            // 不加任何底盘：效果直接用 DstIn 打在图形上，图形内是玻璃、图形外完全透明，
            // 和标题同一套配方，两者看起来是同一种材质。
            Image(
                modifier = Modifier
                    .size(LogoSize)
                    .graphicsLayer { applyLogoFade(scrollProgressProvider, startAt = 0.35f) }
                    .textureBlur(
                        backdrop = backdrop,
                        shape = logoShape,
                        blurRadius = 150f,
                        colors = BlurDefaults.blurColors(blendColors = logoBlend),
                        contentBlendMode = ComposeBlendMode.DstIn,
                    ),
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.primary),
            )
        } else {
            // Android 12（minSdk 31）没有 RuntimeShader：退回纯色底盘 + 内层图标。
            Box(
                modifier = Modifier
                    .size(LogoSize)
                    .graphicsLayer { applyLogoFade(scrollProgressProvider, startAt = 0.35f) }
                    .clip(logoShape)
                    .background(MiuixTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.size(LogoSize / 2),
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.primary),
                )
            }
        }

        Text(
            text = "HyperFVM Compose",
            modifier = Modifier
                .padding(top = 12.dp, bottom = 5.dp)
                .graphicsLayer { applyLogoFade(scrollProgressProvider, startAt = 0.20f) }
                .then(
                    if (backdrop != null) {
                        // 与 demo 的标题一致：先模糊背景效果，再用文字 alpha 当遮罩（DstIn），
                        // 于是字身是玻璃、字外保持透明。
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(16.dp),
                            blurRadius = 150f,
                            colors = BlurDefaults.blurColors(blendColors = logoBlend),
                            contentBlendMode = ComposeBlendMode.DstIn,
                        )
                    } else {
                        Modifier
                    },
                ),
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = MiuixTheme.textStyles.title1.fontSize,
        )
        Text(
            text = "v$versionName ($versionCode)",
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { applyLogoFade(scrollProgressProvider, startAt = 0.05f) },
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * demo 的渐隐曲线：进度到 [startAt] 开始，在 0.15 的区间内淡到透明并缩到 95%。
 * 写在 graphicsLayer 里逐帧读进度，不触发重组。
 */
private fun GraphicsLayerScope.applyLogoFade(scrollProgress: () -> Float, startAt: Float) {
    val progress = ((scrollProgress() - startAt) / 0.15f).coerceIn(0f, 1f)
    alpha = 1f - progress
    scaleX = 1f - progress * 0.05f
    scaleY = 1f - progress * 0.05f
}
