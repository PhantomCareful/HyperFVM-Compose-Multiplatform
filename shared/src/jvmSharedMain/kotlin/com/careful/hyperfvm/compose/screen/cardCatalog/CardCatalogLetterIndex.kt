package com.careful.hyperfvm.compose.screen.cardCatalog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

// —— 几何参数：以 Java 版 LetterIndexBarView 为基础，按实机观感调整 ——
private val BarWidth = 16.dp
private val BubbleGap = 6.dp
// 气泡比 Java 版的 40dp 再大一圈，拖拽时更易看清当前分节。
private val BubbleSize = 48.dp
private val BubbleTextSize = 22.dp
// 上下各留 48dp：字母间距收紧一些，索引条也不会贴到底栏边缘。
private val BarVerticalPadding = 48.dp

/** 绘制区总宽 = 气泡 + 间隙 + 胶囊；气泡因此在自身 bounds 内，不需要父容器关闭裁剪。 */
private val IndexWidth = BubbleSize + BubbleGap + BarWidth

private const val InnerVerticalPaddingRatio = 0.5f
private const val TextSizeCellRatio = 0.6f
private val TextSizeMin = 6.dp
private val TextSizeMax = 11.5.dp

private const val BubbleHideDelayMs = 400L
private const val BubbleFadeMs = 150

/**
 * 右侧字母索引条，对应 Java 版 `LetterIndexBarView`。
 *
 * 竖排胶囊里逐格画分节标签（只画实际存在的 `0 / A-Z / #`），按住上下滑动时
 * 按触点所在格命中最接近的分节，格位变化才回调并给一次轻震动；命中期间在胶囊
 * 左侧显示跟手的圆形气泡，松手停留 400ms 后淡出（同 Slider tooltip 的节奏）。
 *
 * 只有最右侧 [BarWidth] 宽的区域接收触摸，左侧留给气泡的空间不参与命中，
 * 列表在该区域的滚动手势不会被吞掉。
 *
 * @param labels 分节标签，顺序与列表分节一一对应。
 * @param onLabelPicked 命中的分节下标变化时回调。
 */
@Composable
internal fun CardCatalogLetterIndex(
    labels: List<String>,
    onLabelPicked: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val containerColor = MiuixTheme.colorScheme.surfaceContainer
    val contentColor = MiuixTheme.colorScheme.onSurfaceContainerVariant
    val bubbleColor = MiuixTheme.colorScheme.primary
    val onBubbleColor = MiuixTheme.colorScheme.onPrimary

    var bubbleLabel by remember { mutableStateOf("") }
    var bubbleCenterY by remember { mutableFloatStateOf(0f) }
    val bubbleAlpha = remember { Animatable(0f) }
    var bubbleJob by remember { mutableStateOf<Job?>(null) }

    // 文字用 TextMeasurer 画：Android 与桌面同一套代码，不再直接操作平台 Canvas。
    val measurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .width(IndexWidth)
            .fillMaxHeight()
            .padding(vertical = BarVerticalPadding),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = BarWidth.toPx()
            val barLeft = size.width - barWidth
            if (barLeft <= 0f || size.height <= 0f) return@Canvas

            // 1) 胶囊底：宽度一半做圆角，两端自然收成半圆。
            drawRoundRect(
                color = containerColor,
                topLeft = Offset(barLeft, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )

            // 2) 内容区再收一圈，首末字母落进圆头之外。
            val innerPadding = barWidth * InnerVerticalPaddingRatio
            val cellHeight = (size.height - innerPadding * 2f) / labels.size
            if (cellHeight <= 0f) return@Canvas
            val cellTextSize = (cellHeight * TextSizeCellRatio)
                .coerceIn(TextSizeMin.toPx(), TextSizeMax.toPx())

            // 2.5) 逐格写分节字母。尺寸按 px 算完再换回 sp，字号随格子高度自适应。
            val barCenterX = barLeft + barWidth / 2f
            val letterStyle = TextStyle(
                fontSize = cellTextSize.toSp(),
                color = contentColor,
            )
            labels.forEachIndexed { index, label ->
                val layout = measurer.measure(text = label, style = letterStyle)
                val centerY = innerPadding + cellHeight * (index + 0.5f)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        barCenterX - layout.size.width / 2f,
                        centerY - layout.size.height / 2f,
                    ),
                )
            }

            // 3) 跟手气泡：画在胶囊左侧，透明度由淡入淡出动画驱动。
            val alpha = bubbleAlpha.value
            if (alpha <= 0f || bubbleLabel.isEmpty()) return@Canvas
            val bubbleHeight = BubbleSize.toPx()
            val bubbleStyle = TextStyle(
                fontSize = BubbleTextSize.toPx().toSp(),
                color = onBubbleColor.copy(alpha = alpha),
            )
            val bubbleText = measurer.measure(text = bubbleLabel, style = bubbleStyle)
            val bubbleWidth = maxOf(bubbleHeight, bubbleText.size.width + 20.dp.toPx())
            val bubbleRight = barLeft - BubbleGap.toPx()
            val bubbleTop = (bubbleCenterY - bubbleHeight / 2f)
                .coerceIn(0f, size.height - bubbleHeight)

            // 投影：平台原生 setShadowLayer 不可移植，用一层半透明圆角矩形模拟。
            val bubbleShapeSize = Size(bubbleWidth, bubbleHeight)
            drawRoundRect(
                color = bubbleColor.copy(alpha = 0.25f * alpha),
                topLeft = Offset(bubbleRight - bubbleWidth, bubbleTop + 1.5.dp.toPx()),
                size = bubbleShapeSize,
                cornerRadius = CornerRadius(bubbleHeight / 2f),
            )
            drawRoundRect(
                color = bubbleColor.copy(alpha = alpha),
                topLeft = Offset(bubbleRight - bubbleWidth, bubbleTop),
                size = bubbleShapeSize,
                cornerRadius = CornerRadius(bubbleHeight / 2f),
            )
            drawText(
                textLayoutResult = bubbleText,
                topLeft = Offset(
                    bubbleRight - bubbleWidth / 2f - bubbleText.size.width / 2f,
                    bubbleTop + bubbleHeight / 2f - bubbleText.size.height / 2f,
                ),
            )
        }

        // 触摸区只覆盖胶囊本身；按在气泡那一侧不会抢列表的滚动手势。
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(BarWidth)
                .fillMaxHeight()
                .pointerInput(labels) {
                    // 格位变化才回调 + 震动，拖动时天然节流。
                    var lastPicked = -1
                    fun hit(y: Float) {
                        val innerPadding = BarWidth.toPx() * InnerVerticalPaddingRatio
                        val contentHeight = size.height - innerPadding * 2f
                        if (contentHeight <= 0f) return
                        val index = ((y - innerPadding) /
                            (contentHeight / labels.size)).toInt().coerceIn(0, labels.lastIndex)
                        val half = BubbleSize.toPx() / 2f
                        bubbleCenterY = y.coerceIn(half, (size.height - half).coerceAtLeast(half))
                        if (index == lastPicked) return
                        lastPicked = index
                        bubbleLabel = labels[index]
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onLabelPicked(index)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 拦住外层 Pager 的横滑与列表的纵滑。
                        down.consume()
                        bubbleJob?.cancel()
                        bubbleJob = scope.launch {
                            bubbleAlpha.animateTo(1f, tween(BubbleFadeMs))
                        }
                        hit(down.position.y)

                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                pressed = false
                            } else {
                                change.consume()
                                pressed = change.pressed
                                if (pressed) hit(change.position.y)
                            }
                        }

                        // 松手：气泡停留片刻后淡出。
                        bubbleJob?.cancel()
                        bubbleJob = scope.launch {
                            delay(BubbleHideDelayMs)
                            bubbleAlpha.animateTo(0f, tween(BubbleFadeMs))
                        }
                    }
                },
        )
    }
}
