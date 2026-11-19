package com.careful.hyperfvm.compose.component.imageviewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.SaveResult
import com.careful.hyperfvm.compose.platform.ViewerBackHandler
import com.careful.hyperfvm.compose.platform.ViewerSystemBarsEffect
import com.careful.hyperfvm.compose.data.image.DataImageThumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import com.careful.hyperfvm.compose.ui.AppCard
import com.careful.hyperfvm.compose.ui.sheetCardColor
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import com.careful.hyperfvm.compose.ui.AppBottomSheet
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.rememberViewerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/** 查看器里可浏览的一张图：本地文件 + 标题（图片的无障碍描述）+ 文件名（查宽高比用）。 */
data class DataImageViewerItem(val file: File, val title: String, val name: String)

/**
 * 查看器的打开状态，由调用页面持有。
 *
 * @param items 可浏览的图片列表，顺序与页面上看到的一致。
 * @param startIndex 从哪一张开始看。
 * @param boundsByIndex 打开瞬间各张图卡片**预览区**在窗口中的矩形，键是 [items] 的下标。
 *   转场从它展开；关闭时飞回当前这张的矩形——翻过页了也能落回对应卡片。
 *   **可能超出屏幕**：卡片只露出一部分时给的是完整矩形（绘制时由窗口自己裁），
 *   不能用 `boundsInWindow` 那类被窗口裁剪过的结果。
 *   当前这张不在快照里（当时卡片没在屏幕上）时退化成淡入淡出。
 * @param ratios 图片宽高比（宽/高），转场要用它把图片 fit 后的真实矩形算准。
 * @param preview 打开那一张的缩略图（卡片当时正在显示的位图），
 *   转场期间的首帧内容与 Telephoto 的 preview 都用它；卡片还在解码占位时可以为 null。
 * @param fullPreview 打开那一张的「整图低清图」：卡片稳定后预热好的，
 *   转场全程以它为内容——窗口从卡片矩形扩展到全屏时，任何时刻露出的都是
 *   完整画面（包括卡片外被裁掉的部分）。没预热到时为 null，退用 [preview] 条带占位。
 */
class DataImageViewerState(
    val items: List<DataImageViewerItem>,
    val startIndex: Int,
    val boundsByIndex: Map<Int, Rect>,
    val ratios: Map<String, Float>,
    val preview: ImageBitmap? = null,
    val fullPreview: ImageBitmap? = null,
)

/** 打开与关闭转场的时长，两边对称，关闭时「飞回卡片」的感觉才连贯。 */
private const val ViewerTransitionMillis = 300

/**
 * 放大倍数上限，相对**原图像素**（见 [ZoomSpec.maximum]）。
 *
 * 退出转场要把整张图摆回它的原位，用的位图分辨率得跟着这个上限走：上限越高，
 * 放大后整图占的像素越多，整图位图就得解越大。
 */
private const val ViewerMaxZoomFactor = 1.5f

/**
 * 放大态关闭时，页面那块清晰画面在关闭进度的前四成里淡出：progress 从 1 落到
 * 这个值时已完全透明（见 HorizontalPager 的 graphicsLayer）。
 */
private const val ViewerPagerFadeEnd = 0.6f

/**
 * 全屏图片查看器：由页面叠上来的覆盖层，数据图合集与更新公告共用。
 *
 * 为什么从 mojito 迁过来：mojito 是 View 体系（ViewPager + SketchImageView），转场、
 * 手势、生命周期都与 Compose 割裂——「打开顿挫」「转场丢失」「返回手势被抢先」
 * 几个问题都源于这层边界。现在换 Telephoto：图片用它的区域解码
 * （sub-sampling-image 就是 Java 版查看器 subsampling 算法的 Compose 适配，放大后
 * 仍按瓦片解出原图像素），手势用 `Modifier.zoomable`，转场是普通的 Compose 属性
 * 动画，全部在同一棵组合树里，不再有跨体系的状态同步。
 *
 * 转场：图片内容按原图比例**等比**缩放，从头到尾与卡片预览区严丝合缝——起点按
 * 卡片实际使用的 Crop + TopCenter 还原（宽扁窗口裁掉图片上下、瘦高窗口裁掉
 * 左右），终点是全屏 fit 居中的完整图；中间用同一进度插值的裁剪窗口把超出部分
 * 裁掉，而不是把完整图硬塞进卡片矩形（那样会变形、位置也对不上）。
 *
 * 首帧内容：页面在打开瞬间把卡片正在显示的那张缩略图随状态快照一起传进来；
 * 若卡片稳定后预热过一张「整图低清图」，就以它为主（铺满层内 fit 区域）——
 * 窗口从卡片矩形向全屏扩展时，任何时刻露出的都是完整画面，竖长图被卡片裁掉的
 * 下半截也在其中，不会等动画结束才瞬间补上；条带缩略图只在预热缺失时兜底，
 * 完整图补解出来后与它交叉淡入。两者最终都等原图的 base 瓦片真正画出来再退场。
 *
 * 原图加载在转场跑到 1/4 时就放行：Telephoto 的瓦片解码在 IO 线程，不会把动画
 * 拖花；但它自己的 base 瓦片是按视图尺寸采样的整图，超长 PNG 要几百毫秒，
 * 不能指望它在转场期间就位（这正是以前「下半截等动画结束才出现」的来源），
 * 所以转场内容交给预热好的整图低清图。占位图一直盖到 base
 * 瓦片**真的画出来**（`isImageDisplayedInFullQuality`）才退场：`isImageDisplayed`
 * 只表示「可以显示了」，此时瓦片往往还在解码，按它撤占位会漏出一瞬间黑底；
 * 这个信号在 1x 缩放下恰好就是 base 瓦片已就绪。
 *
 * 关闭：单击图片、返回键/返回手势都走同一条路径——反向跑完转场再通知页面卸载。
 * 放大态下关闭时，转场让**整张图（包括屏幕外的那部分）的四角**一起收向缩略图的
 * 对应四角（见 closingZoom）。屏幕外的内容页内拿不到——Pager 会在页面坐标里
 * 把内容裁到屏幕大小（`clipScrollableContainer` 在主轴上硬裁），所以图片层下面
 * 额外垫一张整图位图，缩小时新露出的四周来自它。垫层画在图层变换**之内**，
 * 与 Pager 同一坐标系、跟着一起缩；画在变换之外会停在未缩放的位置不跟着收。
 *
 * 关键在于**整张图只能有一个内容源**（HyperOS 相册的做法：先把真实视图隐掉，
 * 再用一张整图快照根据矩阵飞回去）。清晰画面和垫层来自两张分辨率不同的图时，
 * 无论怎么对齐，交界处都是一道硬缝，看上去就是「缩小时的那张图不对」。所以：
 * 1. 放大后就后台预解一张「整图高清图」（分辨率按放大后的实际像素取，见
 *    [DataImageThumbnails.loadZoomPreview]），退出时整张图就只用它；
 * 2. 页面那块清晰画面在飞行的前四成里淡出，首帧仍是眼前的清晰画面，之后
 *    只剩同一张整图，分辨率变化被摊成一段时间而不是空间上的一圈；
 * 3. 页面边缘同步做 DstIn 羽化，淡出期间也不留矩形硬边（没预热到高清图时
 *    只剩这一层，垫层是低清的，不淡出——换过去只会更差）。
 *
 * 面板开着时的返回先关面板；面板的返回处理挂在 miuix 的 Compose 层，而这里注册
 * 的 [BackHandler] 更晚、优先级更高，所以裁决点只有一处，不会再互相抢。
 */
@Composable
internal fun DataImageViewer(
    state: DataImageViewerState,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val items = state.items
    val pagerState = rememberPagerState(initialPage = state.startIndex) { items.size }

    var showActions by remember { mutableStateOf(false) }
    // 当前页是否处于放大态：放大后要停掉翻页，横向拖拽全部用于平移图片，
    // 这也是 mojito（ViewPager）一直以来的行为。
    var zoomedIn by remember { mutableStateOf(false) }

    // 转场走完才算「内容就绪」：翻页、点击这些交互从这时开始放开。
    var contentActive by remember { mutableStateOf(false) }
    // 原图加载在转场跑到四分之一时就放行：Telephoto 的瓦片解码在 IO 线程，
    // 主线程只剩组合与状态更新，不会拖花动画。先让出首帧，避开覆盖层首次
    // 组合的大开销；尽早放行是为了让 base 瓦片（它很慢）尽早开始解码，
    // 转场一结束就能尽快从低清占位换成清晰图。
    var loadActive by remember { mutableStateOf(false) }
    // 相邻页的解码器再等一下：当前页先点亮，免得几路解码一起排队，
    // 把刚接手的画面顶掉一帧。
    var prefetchActive by remember { mutableStateOf(false) }
    // 哪些页的原图已经真的画出来了：缩略图占位靠它退场。用集合且只增不减——
    // isImageDisplayedInFullQuality 在缩放时（前景瓦片重新加载）会短暂回落，
    // 占位图不能因此重新冒出来闪一下。
    var displayedPages by remember { mutableStateOf(emptySet<Int>()) }

    // 转场占位用的整图低清图：命中预热缓存就是第一帧内容；没命中（刚进页面就点）
    // 则在转场期间补解一张，就绪后与条带占位交叉淡入。
    var fullPreview by remember { mutableStateOf(state.fullPreview) }
    LaunchedEffect(Unit) {
        if (fullPreview == null) {
            fullPreview = DataImageThumbnails.loadViewerPreview(items[state.startIndex].file)
        }
    }

    // 放大后预解一张「整图高清」：放大态退出时，屏幕外那部分只能靠一张整图的
    // 位图补上，而它的分辨率必须够到图片放大后实际占的像素——否则缩小时的
    // 图片就是一团被放大的低清图，和刚才看到的清晰画面完全对不上。放大后图片
    // 已稳定显示，这几百毫秒足够解完，退出时直接同步命中。
    //
    // 等一小会儿再解：整图采样解码是重 CPU 活，而缩放刚开始的这几百毫秒里，
    // 主线程和瓦片解码都在抢解码资源，插进去只会让双指拖动发滞。缩放中途退出
    // （效果变回 false）时协程被取消，不会白解。
    val zoomPreviewTargetWidth = with(LocalDensity.current) {
        // 窗口宽度已经是像素；首帧可能还是 0，夹一下免得传 0 进去。
        (LocalWindowInfo.current.containerSize.width * ViewerMaxZoomFactor).toInt().coerceAtLeast(1)
    }
    LaunchedEffect(zoomedIn, pagerState.currentPage) {
        if (!zoomedIn) return@LaunchedEffect
        val file = items.getOrNull(pagerState.currentPage)?.file ?: return@LaunchedEffect
        delay(350)
        DataImageThumbnails.loadZoomPreview(file, zoomPreviewTargetWidth)
    }

    // 0 = 卡片里的图片区，1 = 全屏。
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    // 关闭瞬间当前页整张图在屏幕上的落位（含屏幕外部分）：由 ZoomableState 的
    // offset 与 contentSize × scale 还原——这正是 SubSamplingImage 绘制瓦片用的
    // 那套参数（`region.bounds × scale + offset`），垫层照此摆放才不会错位。
    // 放大态退出时转场从它起飞，而不是从整张 fit 图起飞——见 [viewerTransition]
    // 的 from 参数。没放大过时为 null。
    var closingZoom by remember { mutableStateOf<Rect?>(null) }
    // 放大态退出的整图垫层（见 dismissViewer）。
    var closingPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    // 垫层是「放大后预热好的整图高清图」而不是低清兜底：只有高清时才让上层那段
    // 清晰画面淡出，转场全程才真正只剩一个内容源。
    var closingRich by remember { mutableStateOf(false) }
    // 每页的缩放状态：关闭时要读当前页的落位，由 ViewerPage 创建后上报。
    val pageStates = remember { mutableStateMapOf<Int, ZoomableState>() }

    // 卡片上报的矩形是窗口坐标，而转场计算都在覆盖层的本地坐标里做；
    // 这里记下覆盖层自己的窗口位置，换算时一起减掉。正常情况下就是 (0, 0)。
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        launch {
            progress.animateTo(1f, tween(ViewerTransitionMillis, easing = FastOutSlowInEasing))
            // 中途被关闭打断（animateTo 被取消）时保持 false：退出转场继续用缩略图，
            // 不会再拉起一轮解码。
            contentActive = true
        }
        // 转场跑到 1/4 就点亮加载（见 loadActive 注释）。关闭时进度回落，
        // 永远到不了这个门槛，不会白拉一轮解码。
        snapshotFlow { progress.value }.first { it >= 0.25f }
        loadActive = true
    }

    LaunchedEffect(contentActive) {
        if (contentActive) {
            delay(400)
            prefetchActive = true
        }
    }

    fun dismissViewer() {
        if (closing) return
        closing = true
        // 放大态退出：记下当前页的缩放落位，转场从这块局部起飞。没放大过（或页面
        // 状态还没就绪）时为 null，转场仍从 fit 矩形起飞，与以往完全一致。
        closingZoom = pageStates[pagerState.currentPage]?.let { zoomState ->
            val transformation = zoomState.contentTransformation
            if (!transformation.isSpecified) return@let null
            if (transformation.scaleMetadata.userZoom <= 1.001f) return@let null
            // 用瓦片绘制同一套参数还原图片矩形：图像素尺寸 × 绝对缩放 + 偏移。
            // 不拿 fit 矩形去乘倍数推——fit 是按接口/文件头比例算的，与真实像素
            // 尺寸差一点点，乘上几倍放大后就是肉眼可见的错位。
            @Suppress("DEPRECATION")
            val content = transformation.contentSize
            Rect(
                offset = transformation.offset,
                size = Size(
                    width = content.width * transformation.scale.scaleX,
                    height = content.height * transformation.scale.scaleY,
                ),
            )
        }
        // 放大态退出需要一张整图垫在图片层下面：Pager 会把页面内容裁到屏幕大小，
        // 屏幕外那部分在页内根本画不出来，只能靠垫层补上——四角的连线才看得见。
        //
        // 优先用放大后预热好的整图高清图（见上面的 LaunchedEffect）：它与卡片里的
        // 低清图是同一张图，但分辨率够到图「放大后实际占的像素」，退出的每一帧
        // 都对得上眼前的画面。没预热到才退回低清图，那时转场只能靠羽化掩盖接缝。
        val page = pagerState.currentPage
        val file = items.getOrNull(page)?.file
        val zoomPreview =
            if (closingZoom == null) null else file?.let { DataImageThumbnails.peekZoomPreview(it) }
        closingRich = zoomPreview != null
        closingPreview = when {
            zoomPreview != null -> zoomPreview
            closingZoom == null -> null
            // 没预热到高清图：退回低清整图，转场只能靠羽化掩盖接缝。
            else -> (if (page == state.startIndex) fullPreview else null)
                ?: file?.let { DataImageThumbnails.peekViewerPreview(it) }
        }
        scope.launch {
            progress.animateTo(0f, tween(ViewerTransitionMillis, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    ViewerBackHandler {
        if (showActions) showActions = false else dismissViewer()
    }

    // 查看器是黑底，状态栏图标要切到浅色；退出时恢复。桌面端没有状态栏，由平台层空实现。
    ViewerSystemBarsEffect()

    // 操作面板要用柔光玻璃：查看器铺在根层，LocalBlurBackdrop 到这里是 null，
    // 面板会退化成不透明实色。这里给它垫一层专用取样层，录「黑底 + 图片 + 页码」，
    // 面板通过下面的 CompositionLocalProvider 拿到它。
    val viewerBackdrop = rememberViewerBackdrop()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (viewerBackdrop != null) Modifier.layerBackdrop(viewerBackdrop) else Modifier),
        ) {
            // 背景不参与缩放：只有图片「从卡片里长出来」，遮罩是淡入的。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .background(Color.Black),
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val currentPage = pagerState.currentPage
                val currentItem = items.getOrNull(currentPage)
                // 当前这张卡片预览区的矩形，换算到覆盖层的本地坐标。
                val pageBounds = state.boundsByIndex[currentPage]
                    ?.translate(-overlayOrigin.x, -overlayOrigin.y)
                val pageRatio = currentItem?.let { state.ratios[it.name] }
                val fit = remember(containerSize, pageRatio) {
                    if (pageRatio != null) {
                        fitRect(containerSize, pageRatio)
                    } else {
                        Rect(Offset.Zero, containerSize)
                    }
                }

                // 放大态退出时图片实际占据的矩形：转场从整张图的当前位置起飞
                // （见 closingZoom）。在绘制阶段读它，关闭后的第一帧就能生效，
                // 不会先按 fit 裁一帧。
                fun fromRect(): Rect = closingZoom ?: fit

                // 当前进度下的转场几何；当前页没有卡片矩形（翻到快照外的图）时为
                // null，图片层退化成淡入淡出。
                fun transition(): ViewerTransition? {
                    if (pageBounds == null || pageRatio == null) return null
                    val from = fromRect()
                    // 裁剪窗口的起点：有整图垫层时从整张图起飞（四角可见）；没垫层
                    // 时只能从「屏幕上那块裁剪」起飞——否则窗口会超出页内画得出的
                    // 范围，缩放途中在缩小的窗口里露出空黑。
                    val windowFrom = if (closingPreview != null) {
                        from
                    } else {
                        val screen = Rect(Offset.Zero, containerSize)
                        Rect(
                            left = maxOf(from.left, screen.left),
                            top = maxOf(from.top, screen.top),
                            right = minOf(from.right, screen.right),
                            bottom = minOf(from.bottom, screen.bottom),
                        )
                    }
                    return viewerTransition(
                        bounds = pageBounds,
                        fit = fit,
                        progress = progress.value,
                        from = from,
                        windowFrom = windowFrom,
                    )
                }

                // 图片层：内容等比缩放，裁剪窗口从卡片矩形渐变到图片矩形。两者都随
                // progress 插值，首尾帧与卡片画面严丝合缝；翻到快照外的图（卡片当时
                // 不在屏幕上）没有矩形可用，退化成淡入淡出。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { overlayOrigin = it.positionInWindow() }
                        .drawWithContent {
                            val frame = transition()
                            if (frame != null && progress.value < 1f) {
                                val window = frame.window
                                clipRect(window.left, window.top, window.right, window.bottom) {
                                    this@drawWithContent.drawContent()
                                }
                            } else {
                                // 转场结束后不再裁剪：放大后的图片要能铺出 fit 矩形。
                                drawContent()
                            }
                        }
                        .graphicsLayer {
                            val frame = transition()
                            if (frame != null) {
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = frame.scale
                                scaleY = frame.scale
                                translationX = frame.translateX
                                translationY = frame.translateY
                                alpha = 1f
                            } else {
                                alpha = progress.value
                            }
                        }
                        // 放大态退出的整图垫层：Pager 把页面内容裁在屏幕内，屏幕外那部分
                        // 在页内画不出来。垫层必须铺在图片**当前的落位**上（而不是 fit 矩形）：
                        // fit 永远在屏幕那块裁剪内部，垫在那里会全程被清晰画面盖住；
                        // 铺到整图落位后，屏幕外那四边随图层缩小自然进入视野，
                        // 看上去就是整张图的四角一起收向缩略图。
                        //
                        // 必须是 graphicsLayer **内层**的绘制：写在外层 drawWithContent 里
                        // 会把垫层画在未缩放的坐标系里（实测整张图固定在放大位置不跟着收），
                        // 图层变换只对子内容生效。这里和 Pager 同一局部坐标系，一起被缩放。
                        .drawWithContent {
                            val exitPreview = closingPreview
                            if (exitPreview != null && pageBounds != null && pageRatio != null) {
                                val from = fromRect()
                                drawImage(
                                    image = exitPreview,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(exitPreview.width, exitPreview.height),
                                    dstOffset = IntOffset(
                                        from.left.roundToInt(),
                                        from.top.roundToInt(),
                                    ),
                                    dstSize = IntSize(
                                        from.width.roundToInt(),
                                        from.height.roundToInt(),
                                    ),
                                    filterQuality = FilterQuality.Medium,
                                )
                            }
                            drawContent()
                        }
                        .pointerInput(Unit) {
                            // 兜底消费拖动位移，必须放在 Main 阶段：PointerEventPass 的
                            // Main 是「叶 → 根」，子孙（zoomable/Pager）先处理，这里接住
                            // 没人消费的那部分；而 NavDisplay 的滑动返回在 Final 阶段才
                            // 检查 isConsumed，且要求「子节点连续消费」才会永久让路
                            // （NavSwipeArbitrator）——放在 Final 阶段消费时仲裁器早已
                            // 看过事件，翻到列表边缘、Pager 不再消费时它就会接管，误退
                            // 出页面。
                            //
                            // 但要等手指真的拖出 touchSlop 再开始消费：Telephoto 的
                            // zoomable 在 slop 之前会在 Final 阶段检查有没有人消费
                            // （transformable.kt 的 finallyCanceled），每帧都替它消费
                            // 会把它的手势识别提前打断。超过 slop 后仲裁器也正好到了
                            // 可以接管的条件，这时消费才既有意义又不干扰子孙手势。
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var dragging = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change =
                                            event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!dragging) {
                                            dragging =
                                                (change.position - down.position).getDistance() >
                                                    viewConfiguration.touchSlop
                                        }
                                        if (dragging && change.positionChanged()) change.consume()
                                        if (!change.pressed) break
                                    }
                                }
                            }
                        },
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            // 放大态关闭时，页面这块画面是缩在整图垫层里的，两者分辨率不同，
                            // 交界处天然是一道硬缝，看着像两张图拼在一起。两层配合着洗掉接缝：
                            //
                            // 1. 首帧到前四成：让页面这块清晰画面淡出（[ViewerPagerFadeEnd]）。
                            //    首帧仍是原图的清晰画面，退场时已经只剩同一张整图，中间的
                            //    分辨率变化被摊在一段时间里，看不出跳变。
                            // 2. 页面边缘同时做 DstIn 羽化，淡出过程中也不留一圈矩形硬边。
                            //
                            // 垫层不是高清（没预热到）时不做淡出：那层更糊，换过去只会更差。
                            .graphicsLayer {
                                if (closingZoom != null && progress.value < 1f) {
                                    // DstIn 与 alpha 都需要独立图层
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    if (closingRich) {
                                        alpha = ((progress.value - ViewerPagerFadeEnd) /
                                            (1f - ViewerPagerFadeEnd)).coerceIn(0f, 1f)
                                    }
                                }
                            }
                            .drawWithContent {
                                drawContent()
                                if (closingZoom != null && progress.value < 1f) {
                                    val feather = (64.dp.toPx() * (1f - progress.value))
                                        .coerceAtMost(size.minDimension / 2f - 1f)
                                    if (feather > 1f) {
                                        val fx = feather / size.width
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                fx to Color.White,
                                                1f - fx to Color.White,
                                                1f to Color.Transparent,
                                                startX = 0f,
                                                endX = size.width,
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                        val fy = feather / size.height
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                fy to Color.White,
                                                1f - fy to Color.White,
                                                1f to Color.Transparent,
                                                startY = 0f,
                                                endY = size.height,
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                }
                            },
                        // 转场期间不翻页：转场几何按当前页算，中途换页会跳变；
                        // 放大态也不翻页，横向拖拽全部用于平移图片。
                        userScrollEnabled = contentActive && !zoomedIn && !closing,
                        // 预取相邻页：翻页手势开始前就把新页组合好、解码器建好，
                        // 拖动时不会因为首次组合和解码器初始化卡住手势识别。
                        beyondViewportPageCount = 1,
                    ) { page ->
                        ViewerPage(
                            item = items[page],
                            page = page,
                            pagerState = pagerState,
                            // 加载在转场跑到 1/4 时就已放行；相邻页仍等 prefetchActive，
                            // 但用户已经开始翻页（拖动中）时不再等，直接组合。
                            active = loadActive && (prefetchActive ||
                                page == pagerState.currentPage || pagerState.isScrollInProgress),
                            onTap = { dismissViewer() },
                            onLongPress = {
                                // 拉起操作面板前给一下震动：长按手势靠触觉确认已识别。
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showActions = true
                            },
                            onZoomChanged = { zoomedIn = it },
                            onStateCreated = { createdPage, createdState ->
                                pageStates[createdPage] = createdState
                            },
                            onImageDisplayed = { shownPage, shown ->
                                if (shown && shownPage !in displayedPages) {
                                    displayedPages = displayedPages + shownPage
                                }
                            },
                        )
                    }

                    // 转场占位：两层叠放，谁就绪谁顶上。
                    //
                    // 主力是「整图低清图」：按层内 fit 区域铺满。它和卡片画面严格对齐
                    // （p=0 时可见窗口正好是卡片矩形，显示的就是同一块 cover 内容），
                    // 而窗口向全屏扩展时露出的是完整图的其余部分——竖长图被卡片裁掉的
                    // 下半截也在其中，不会等动画结束才补上。
                    //
                    // 兜底是「条带缩略图」：页面没预热到（刚进页面就点）时先盖住卡片
                    // 那块内容，等完整图补解出来后再交叉淡入退场。两者最终都等原图的
                    // base 瓦片真的画出来（displayedPages）后才一起淡出。
                    val showPlaceholder = !contentActive || currentPage == state.startIndex
                    if (showPlaceholder && currentItem != null && pageBounds != null &&
                        pageRatio != null && fit.width > 0f
                    ) {
                        val density = LocalDensity.current
                        val placeholderAlpha by animateFloatAsState(
                            targetValue = if (currentPage in displayedPages) 0f else 1f,
                            animationSpec = tween(200),
                        )
                        val full = if (currentPage == state.startIndex) fullPreview else null
                        // 完整图就绪时与条带交叉淡入：下半截是「渐显」而不是突然出现。
                        val fullAlpha by animateFloatAsState(
                            targetValue = if (full != null) 1f else 0f,
                            animationSpec = tween(200),
                        )
                        if (full != null && full.height > 0) {
                            Image(
                                bitmap = full,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                filterQuality = FilterQuality.Medium,
                                modifier = Modifier
                                    // 层内坐标系就是 fit 矩形所在的容器坐标，
                                    // 直接用 fit 的位置与尺寸，不用做逆变换。
                                    .offset {
                                        IntOffset(fit.left.roundToInt(), fit.top.roundToInt())
                                    }
                                    .requiredSize(
                                        width = with(density) { fit.width.toDp() },
                                        height = with(density) { fit.height.toDp() },
                                    )
                                    .graphicsLayer { alpha = placeholderAlpha * fullAlpha },
                            )
                        }
                        val stripe = remember(currentItem.file) {
                            // 打开那一页优先用调用方送来的缩略图（就是卡片眼前这张），
                            // 其余情况去最近缩略图缓存里碰运气。
                            (if (currentPage == state.startIndex) state.preview else null)
                                ?: DataImageThumbnails.peek(currentItem.file)
                        }
                        if (stripe != null && stripe.height > 0) {
                            val stripeRatio =
                                stripe.width.toFloat() / stripe.height.toFloat()
                            // 把「卡片里显示缩略图的那块屏幕区域」按 p=0 的变换逆映射回
                            // 图片层的本地坐标：宽图时它比覆盖层还大，靠后面的裁剪窗口
                            // 只露出该露的部分。
                            val initial = viewerTransition(pageBounds, fit, 0f)
                            val screenRect = coverRect(pageBounds, stripeRatio)
                            val left = (screenRect.left - initial.translateX) / initial.scale
                            val top = (screenRect.top - initial.translateY) / initial.scale
                            val width = screenRect.width / initial.scale
                            val height = screenRect.height / initial.scale
                            Image(
                                bitmap = stripe,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                filterQuality = FilterQuality.Medium,
                                modifier = Modifier
                                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                                    // requiredSize：层坐标里的尺寸可能超出覆盖层，
                                    // 别被父约束夹回去。
                                    .requiredSize(
                                        width = with(density) { width.toDp() },
                                        height = with(density) { height.toDp() },
                                    )
                                    // 完整图顶上后条带交叉退场；两者最终都随
                                    // placeholderAlpha 一起淡出。
                                    .graphicsLayer {
                                        alpha = placeholderAlpha * (1f - fullAlpha)
                                    },
                            )
                        }
                    }
                }
            }

            // 页码：单独一层，不跟着图片缩放。黑底白字是查看器的通用约定，
            // 这里不用主题色（页面主题是浅色/深色界面色，在黑底上都看不清）。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${items.size}",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        // 长按拉起的操作面板：Scaffold 会把 content 包进一个 Box，所以这里与画面是兄弟节点，
        // 叠在它上面但不在取样层里——不依赖弹层宿主自身的取样行为。
        CompositionLocalProvider(LocalBlurBackdrop provides viewerBackdrop) {
            DataImageViewerActionsSheet(
                item = items.getOrNull(pagerState.currentPage),
                show = showActions,
                onDismiss = { showActions = false },
            )
        }
    }
}

/**
 * 查看器里的一页：一张原图 + 手势缩放。
 *
 * [active] 为 false 时不组合任何东西；变为 true 后一次性点亮，此后不再回落。
 * 加载在转场跑到 1/4 时就放行（见文件头注释），画面由外层的缩略图占位盖着，
 * 所以点亮动作本身不会在屏幕上露出来。
 *
 * 图片加载走 `SubSamplingImageSource.contentUri`（file:// 会被解析成最快的文件描述符
 * 路径）＋区域解码：瓦片逐帧就绪，[onImageDisplayed] 把「这张已经画出来了」上报给
 * 查看器，由它决定什么时候撤掉缩略图占位。上报用 `isImageDisplayedInFullQuality`：
 * `isImageDisplayed` 在 base 瓦片开始解码时就为 true（painter 还是空的），提前撤占位
 * 会闪黑底；1x 缩放下前者等价于 base 瓦片已就绪。
 *
 * 缩放：上限 1.5 倍。`ZoomSpec.maximum` 是相对**原图像素**的倍数，不是相对眼前
 * fit 后的画面：数据图原宽接近 5000，fit 到窄屏只有约 0.19 倍，原先的 4 倍原图
 * 等于相对初始画面放大到 20 倍以上，到手就是一片马赛克。1.5 倍留了一点超采样
 * 余量，看清原图像素细节够用，双击也不会一下跳到无法定位的深放大。
 *
 * 关闭转场不在这里复位缩放：查看器会在关闭瞬间读当前页的缩放落位（closingZoom），
 * 让整张图（含屏幕外部分）的四角一起收向缩略图——放大状态下退出，看到的是
 * 整张图从当前位置缩回卡片，而不是先被拉回全图再缩回去。
 */
@Composable
private fun ViewerPage(
    item: DataImageViewerItem,
    page: Int,
    pagerState: PagerState,
    active: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    onStateCreated: (Int, ZoomableState) -> Unit,
    onImageDisplayed: (Int, Boolean) -> Unit,
) {
    val zoomSpec = remember { ZoomSpec(maxZoomFactor = ViewerMaxZoomFactor) }
    val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)

    val isCurrent = page == pagerState.currentPage

    // 离屏后回到全览：Pager 会恢复每页的缩放状态，翻回来时若还停在上次的放大
    // 位置会让人分不清方向，这里在页面离开时立刻复位。
    LaunchedEffect(isCurrent) {
        if (!isCurrent) zoomableState.resetZoom(SnapSpec())
    }

    // 只有当前页的缩放会影响翻页开关；离屏页不上报。
    val zoomed = (zoomableState.zoomFraction ?: 0f) > 0.01f
    LaunchedEffect(isCurrent, zoomed) {
        if (isCurrent) onZoomChanged(zoomed)
    }

    // 把缩放状态交给查看器：关闭瞬间要读当前页的缩放落位（见 closingZoom）。
    LaunchedEffect(zoomableState) { onStateCreated(page, zoomableState) }

    if (active) {
        ViewerSubSamplingImage(
            path = item.file.absolutePath,
            contentDescription = item.title,
            zoomableState = zoomableState,
            modifier = Modifier.fillMaxSize(),
            onClick = { onTap() },
            onLongClick = { onLongPress() },
            // 离屏页不上报，避免影响当前页的占位处理。
            onDisplayed = { displayed -> if (isCurrent) onImageDisplayed(page, displayed) },
        )
    }
}

/**
 * 转场的一帧几何：[scale] 是图片内容（[fit] 矩形）的等比缩放，[window] 是这一帧
 * 要裁剪到的可见区域（屏幕坐标）。
 */
private class ViewerTransition(
    val scale: Float,
    val translateX: Float,
    val translateY: Float,
    val window: Rect,
)

/**
 * 计算 progress 处的转场几何。
 *
 * 卡片预览是 Crop + TopCenter，所以起点要先按 cover 语义还原成「卡片窗口里实际显示
 * 的那块内容」：窗口比图宽扁时裁掉图片底部（宽铺满、向下溢出），窗口比图瘦高时
 * 左右各裁一刀（高铺满、横向居中）——宽图的预览正是后一种。内容保持原图比例等比
 * 缩放，越出窗口的部分由调用方裁掉；终点是全屏 fit 的完整图。
 *
 * [from] 是 p=1 时图片内容在屏幕上的实际矩形：平时就是 [fit]（整张图）；
 * 放大态退出时是整张图的当前落位（closingZoom）——四角连同屏幕外的部分
 * 一起收向缩略图的对应四角。p=0 的落点不变（卡片里的 cover 矩形），与卡片
 * 预览严丝合缝。
 *
 * [windowFrom] 是裁剪窗口的起点，默认与 [from] 相同（四角全程可见，与内容严丝）；
 * 只有拿不到整图垫层时才退成 [from] 与屏幕的交集，让窗口始终落在页面画得出的
 * 范围里。
 */
private fun viewerTransition(
    bounds: Rect,
    fit: Rect,
    progress: Float,
    from: Rect = fit,
    windowFrom: Rect = from,
): ViewerTransition {
    val fromWidth = from.width.coerceAtLeast(1f)
    val ratio = if (fit.height > 0f) fit.width / fit.height else 1f
    val start = coverRect(bounds, ratio)
    val scale = lerp(start.width / fromWidth, 1f, progress)
    val contentLeft = lerp(start.left, from.left, progress)
    val contentTop = lerp(start.top, from.top, progress)
    return ViewerTransition(
        scale = scale,
        // graphicsLayer 的缩放绕 (0,0)：把「缩放后的内容矩形」摆到目标位置的反推平移。
        translateX = contentLeft - scale * from.left,
        translateY = contentTop - scale * from.top,
        // 可见窗口从「关闭瞬间图片占据的范围」渐变到卡片矩形。
        window = Rect(
            left = lerp(bounds.left, windowFrom.left, progress),
            top = lerp(bounds.top, windowFrom.top, progress),
            right = lerp(bounds.right, windowFrom.right, progress),
            bottom = lerp(bounds.bottom, windowFrom.bottom, progress),
        ),
    )
}

/**
 * 把 [bounds] 按 ContentScale.Crop 的语义套进宽高比为 [ratio] 的图片：返回值是图片
 * 内容块的矩形，它覆盖整个窗口，溢出部分由调用方裁剪（与卡片预览同一套规则）。
 */
private fun coverRect(bounds: Rect, ratio: Float): Rect {
    if (!ratio.isFinite() || ratio <= 0f || bounds.height <= 0f) return bounds
    val boundsRatio = bounds.width / bounds.height
    return if (boundsRatio >= ratio) {
        // 窗口比图更宽扁：宽度铺满，图片向下溢出（TopCenter 从顶部开始裁）。
        Rect(bounds.left, bounds.top, bounds.right, bounds.top + bounds.width / ratio)
    } else {
        // 窗口比图更瘦高（宽图预览的常态）：高度铺满，左右居中各裁一点。
        val width = bounds.height * ratio
        val centerX = (bounds.left + bounds.right) / 2f
        Rect(centerX - width / 2f, bounds.top, centerX + width / 2f, bounds.bottom)
    }
}

/** 宽高比为 [ratio] 的图片在 [container] 中按 fit 居中后的矩形。 */
private fun fitRect(container: Size, ratio: Float): Rect {
    if (!ratio.isFinite() || ratio <= 0f) return Rect(Offset.Zero, container)
    val width = min(container.width, container.height * ratio)
    val height = width / ratio
    val left = (container.width - width) / 2f
    val top = (container.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

/**
 * 面板里的一行操作：独立 Card 承载，与预设组合的卡片同规格。
 *
 * 点击反馈只保留水波纹，不用 Sink：这两张卡在弹层里整卡下沉视觉上太抢，
 * 而弹层本身就说明「这是一组操作」，淡波纹足够确认点击。内层 [ArrowPreference]
 * 不传 onClick，否则它会再挂一层 clickable 把 Card 的反馈和点击都挡掉。
 */
@Composable
private fun ActionCard(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        // 弹层是浅色圆角面板，与 Card 默认的 surface 同色，
        // 换成弹层内容卡底色才能在弹层里分辨出卡片边界。
        colors = CardDefaults.defaultColors(color = sheetCardColor()),
        showIndication = true,
        onClick = onClick,
    ) {
        ArrowPreference(
            title = title,
            summary = summary,
        )
    }
}

/**
 * 保存 / 分享面板：查看器里长按图片拉起，公告正文里长按配图也用它——
 * 那里不先进查看器，只对这张图做事。
 *
 * [item] 为空（图还没落盘）时面板不会出现；关闭动作交给调用方收回 [show]。
 */
@Composable
internal fun DataImageViewerActionsSheet(
    item: DataImageViewerItem?,
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    // 面板最后一张卡下面要留出导航栏的高度：miuix 的 OverlayBottomSheet 只处理
    // IME insets（defaultWindowInsetsPadding → imePadding），内容会一直铺到手势条
    // 底下把分享卡压住，这里补齐。
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AppBottomSheet(
        show = show && item != null,
        onDismissRequest = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            contentPadding = PaddingValues(bottom = 16.dp + navigationBarBottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ActionCard(
                    title = if (env.capabilities.saveAsFile) "另存为图片" else "保存图片",
                    summary = if (env.capabilities.saveAsFile) {
                        "选择保存位置与文件名"
                    } else {
                        "图片将保存到设备的 Pictures 文件夹"
                    },
                    onClick = {
                        val target = item ?: return@ActionCard
                        onDismiss()
                        // 桌面的「另存为」要弹原生对话框，必须在界面线程调，不能下去 IO。
                        if (env.capabilities.saveAsFile) {
                            when (env.saveImageAs(target.file, mimeTypeOf(target.file))) {
                                SaveResult.Saved -> toaster.show("已保存")
                                SaveResult.Cancelled -> Unit
                                SaveResult.Failed -> toaster.show("保存失败，请稍后重试")
                            }
                        } else {
                            scope.launch {
                                val mimeType = mimeTypeOf(target.file)
                                val ok = withContext(Dispatchers.IO) {
                                    env.saveImageToGallery(target.file, mimeType)
                                }
                                toaster.show(
                                    if (ok) "已保存到 Pictures/HyperFVM" else "保存失败，请稍后重试",
                                )
                            }
                        }
                    },
                )
            }
            // 桌面端的「分享」只是资源管理器定位文件，入口已在需求里去掉。
            if (!env.capabilities.saveAsFile) {
                item {
                    ActionCard(
                        title = "分享图片",
                        summary = "拉起系统分享界面将图片传给他人",
                        onClick = {
                            val target = item ?: return@ActionCard
                            onDismiss()
                            if (!env.shareImage(target.file, mimeTypeOf(target.file))) {
                                toaster.show("分享失败，请稍后重试")
                            }
                        },
                    )
                }
            }
        }
    }
}

/** 文件后缀决定 MIME：数据图是 PNG，公告配图是 JPEG。 */
private fun mimeTypeOf(file: File): String =
    if (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true)) {
        "image/jpeg"
    } else {
        "image/png"
    }
