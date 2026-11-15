package com.careful.hyperfvm.compose.screen.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careful.hyperfvm.compose.Res
import com.careful.hyperfvm.compose.platform.createDataImageDecoder
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.ui.isTabletLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 4 个动图各自动图速率（1f 为素材原始速度），对应 Java 版 `PLAY_SPEEDS`。 */
private val LuckyPlaySpeeds = listOf(0.5f, 1f, 2f, 4f)

/** 帧序列所在的资源目录（f00.webp ... 与 meta.txt），由 shared 的 composeResources 分发到双端。 */
private const val LuckyFrameAssetsDir = "today_lucky_frames"

/** 资源路径前缀：Compose Resources 的 files 区。 */
private const val LuckyFrameResourcePrefix = "files/$LuckyFrameAssetsDir"

/** 每一帧对应的分数（下标 = 帧号 0 ~ 27，与素材帧数保持一致）。 */
private val LuckyFrameScores = intArrayOf(
    5, 7, 14, 8, 6, // 帧 0 ~ 4
    9, 7, 12, 10, 6, // 帧 5 ~ 9
    9, 6, 8, 5, 11, // 帧 10 ~ 14
    16, 13, 5, 8, 7, // 帧 15 ~ 19
    15, 10, 5, 7, 6, // 帧 20 ~ 24
    11, 8, 12, // 帧 25 ~ 27
)

/** 得分达标时的描边宽度与描边距动图边缘的间距（描边位于动图外侧）。 */
private val LuckyStrokeWidth = 5.dp
private val LuckyStrokeGap = 5.dp

/** 动图圆角半径，对应 Java 版 `CORNER_RADIUS_DP`。 */
private val LuckyCornerRadius = 25.dp

/** 单次回调最多推进的素材时间：设备卡顿后避免一次跳跃过多帧。 */
private const val LuckyMaxTickDeltaMs = 100L

/** 暂停时展示的等待表情。 */
private const val LuckyWaitingText = "⏳"

/** meta.txt 缺失时的默认帧时长（毫秒）。 */
private const val LuckyDefaultFrameDurationMs = 20

/**
 * 帧序列：按素材时间映射到帧，帧位图带 LRU 缓存。
 * 与 Java 版 `FrameSequence` 一致，所有方法仅应在主线程调用。
 */
private class LuckyFrameSequence(
    private val frameBytes: List<ByteArray>,
    private val frameStartTimesMs: LongArray,
    private val totalDurationMs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
    firstFrame: ImageBitmap,
) {
    private val decoder = createDataImageDecoder()

    // 全量缓存 28 帧（约 12MB）：帧数多于缓存上限时每个循环都要重新解码，4 路播放会持续卡顿
    private val frameCache = LinkedHashMap<Int, ImageBitmap>().apply { put(0, firstFrame) }

    /** 将素材时间（毫秒，任意非负值）映射为对应的帧下标。 */
    fun frameIndexAt(timeMs: Long): Int {
        val t = timeMs.mod(totalDurationMs)
        for (i in frameStartTimesMs.indices.reversed()) {
            if (t >= frameStartTimesMs[i]) return i
        }
        return 0
    }

    /** 获取指定帧的位图（带缓存），解码失败时返回 null。 */
    fun frameAt(index: Int): ImageBitmap? {
        frameCache[index]?.let { return it }
        val bitmap = decoder.decodeBytes(frameBytes[index]) ?: return null
        frameCache[index] = bitmap
        return bitmap
    }
}

/** 从资源加载帧序列（IO 线程）；目录为空或首帧不可读时返回 null。 */
private suspend fun loadLuckyFrameSequence(): LuckyFrameSequence? =
    withContext(Dispatchers.IO) {
        // 资源没有目录枚举接口：按 f00、f01 … 的顺序一直读到第一份缺失为止。
        val frames = ArrayList<ByteArray>()
        var index = 0
        while (true) {
            // 素材文件名是 f00.webp ~ f27.webp：f 前缀 + 两位帧号。
            val name = "f" + index.toString().padStart(2, '0') + ".webp"
            val bytes = runCatching {
                Res.readBytes("$LuckyFrameResourcePrefix/$name")
            }.getOrNull() ?: break
            frames += bytes
            index += 1
        }
        if (frames.isEmpty()) return@withContext null

        // 解码首帧拿到尺寸（帧尺寸全一致），同时把它先放进缓存。
        val first = createDataImageDecoder().decodeBytes(frames.first()) ?: return@withContext null
        if (first.width <= 0 || first.height <= 0) return@withContext null

        val durations = readLuckyFrameDurations(frames.size)
        val startTimes = LongArray(frames.size)
        var total = 0L
        for (i in frames.indices) {
            startTimes[i] = total
            total += durations[i]
        }
        LuckyFrameSequence(frames, startTimes, total, first.width, first.height, first)
    }

/** 读取 meta.txt 中的帧时长（首个非空行为帧数）；缺失或损坏时按顺序回退到默认值。 */
private suspend fun readLuckyFrameDurations(frameCount: Int): IntArray {
    val durations = ArrayList<Int>()
    runCatching {
        val text = Res.readBytes("$LuckyFrameResourcePrefix/meta.txt").decodeToString()
        var firstLine = true
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (firstLine) {
                firstLine = false // 首行为帧数，跳过
                continue
            }
            line.toIntOrNull()?.let(durations::add)
        }
    }

    val result = IntArray(frameCount)
    var last = LuckyDefaultFrameDurationMs
    for (i in 0 until frameCount) {
        if (i < durations.size) last = maxOf(1, durations[i])
        result[i] = last
    }
    return result
}

/** 安全读取某帧的分数（帧号越界时按 0 分处理）。 */
private fun scoreOfLuckyFrame(frameIndex: Int): Int =
    if (frameIndex in LuckyFrameScores.indices) LuckyFrameScores[frameIndex] else 0

/** 4 个动图的定格分数是否完全相同。 */
private fun allLuckyScoresSame(scores: IntArray): Boolean = scores.all { it == scores[0] }

/** 根据总分与 4 个单分数判断结果表情，对应 Java 版 `judgeResultText`。 */
private fun judgeLuckyResult(scores: IntArray, totalScore: Int): String = when {
    totalScore == 20 -> "🤡"
    totalScore == 64 -> "🤑"
    allLuckyScoresSame(scores) -> "🃏"
    scores.any { it == 16 } -> "🤩"
    scores.any { it == 14 || it == 15 } -> "😄"
    scores.any { it == 13 } -> "👍"
    scores.any { it == 12 } -> "🫣"
    else -> "😭" // 9 ~ 11 与兜底同为一个表情
}

/**
 * 今日运势页（实用小工具）：4 个海星动图以不同速率播放，
 * 点暂停时按 4 个定格帧算分并展示运势表情，对应 Java 版 `TodayLuckyActivity`。
 *
 * 播放速率 0.5f / 1f / 2f / 4f 让 4 张图停在同一素材时间点之外的帧上；
 * 素材时间与播放状态在界面重建（深色切换等）后恢复。
 */
@Composable
fun TodayLuckyScreen(onBack: () -> Unit) {
    var sequence by remember { mutableStateOf<LuckyFrameSequence?>(null) }
    LaunchedEffect(Unit) {
        sequence = loadLuckyFrameSequence()
    }

    // 4 个动图各自的素材时间（毫秒）：暂停保留、继续播放从原位置接着走，重建后可恢复
    val frameTimes = List(LuckyPlaySpeeds.size) { rememberSaveable { mutableLongStateOf(0L) } }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    // 暂停时定格的帧号（显示与算分共用这一份数据）；null 表示还在播放（分数显示 ⏳、无描边）
    var frozenFrames by remember { mutableStateOf<IntArray?>(null) }

    val seq = sequence
    // 重建后处于暂停态：用恢复出的素材时间重新定格（Java 版恢复暂停态时同样重算）
    LaunchedEffect(seq, isPlaying) {
        if (seq != null && !isPlaying && frozenFrames == null) {
            frozenFrames = IntArray(frameTimes.size) { i ->
                seq.frameIndexAt(frameTimes[i].longValue)
            }
        }
    }

    // 播放中推进 4 路素材时间：各自按自己的速率前进，再由时间映射到帧
    LaunchedEffect(isPlaying, seq) {
        if (!isPlaying || seq == null) return@LaunchedEffect
        var lastTickMs = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val now = frameTimeNanos / 1_000_000
                if (lastTickMs != 0L) {
                    val delta = minOf(now - lastTickMs, LuckyMaxTickDeltaMs)
                    frameTimes.forEachIndexed { index, time ->
                        time.longValue += (delta * LuckyPlaySpeeds[index]).toLong()
                    }
                }
                lastTickMs = now
            }
        }
    }

    val scores = frozenFrames?.let { frames ->
        IntArray(frames.size) { i -> scoreOfLuckyFrame(frames[i]) }
    }
    val totalScore = scores?.sum()
    val allSame = scores != null && allLuckyScoresSame(scores)
    // 得分 >= 13 的动图描边；4 个得分完全相同时全部描边（播放中一律不描边）
    val highlighted = BooleanArray(frameTimes.size)
    if (scores != null) {
        for (i in scores.indices) {
            highlighted[i] = allSame || scores[i] >= 13
        }
    }
    val resultText = if (scores == null || totalScore == null) {
        LuckyWaitingText
    } else {
        judgeLuckyResult(scores, totalScore)
    }
    val strokeColor = MiuixTheme.colorScheme.primary
    // PAD 判定对齐 Java 版 layout-sw600dp：动图从 2×2 变为一行四列
    val isTablet = isTabletLayout()

    ScreenScaffold(
        title = "选取您的运势",
        // 二级页没有外层 Pager 的底栏。
        outerPadding = PaddingValues(0.dp),
        // 界面本身不滑动，顶栏固定用小标题形态，不做折叠大标题。
        compactTitle = true,
        onBack = onBack,
    ) { padding ->
        // 只吃顶部（状态栏 + 顶栏）：底部导航栏的抬高会让按钮底边距显得更宽，
        // 这里不叠加它，左右下三边距才能视觉等宽。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            // 内容区（分数行 + 4 张动图）在「顶栏之下、按钮之上」的剩余空间里垂直居中
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 容器左右边距与 Java 版一致：PAD 20dp、手机 10dp
                        .padding(horizontal = if (isTablet) 20.dp else 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 结果行：左右两侧表情夹着总分，播放中显示 ⏳
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = resultText,
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = 40.sp,
                        )
                        Text(
                            text = totalScore?.toString() ?: LuckyWaitingText,
                            modifier = Modifier.weight(1f),
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = resultText,
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = 40.sp,
                        )
                    }

                    // 动图排布：PAD 一行四列（对齐 Java 版 layout-sw600dp），手机两行两列
                    val frameRows = if (isTablet) {
                        listOf(listOf(0, 1, 2, 3))
                    } else {
                        listOf(listOf(0, 1), listOf(2, 3))
                    }
                    frameRows.forEachIndexed { rowIndex, rowFrames ->
                        if (rowIndex > 0) {
                            Spacer(Modifier.height(30.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(0.9f)) {
                            rowFrames.forEachIndexed { columnIndex, frameIndex ->
                                if (columnIndex > 0) {
                                    Spacer(Modifier.width(30.dp))
                                }
                                LuckyFrameImage(
                                    sequence = seq,
                                    materialTime = frameTimes[frameIndex],
                                    frozenFrameIndex = frozenFrames?.getOrNull(frameIndex),
                                    highlighted = highlighted[frameIndex],
                                    strokeColor = strokeColor,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (seq != null) {
                        if (isPlaying) {
                            // 暂停：先把 4 个定格帧抓出来，显示和算分共用同一份数据。
                            // 不能只冻结素材时间——播放循环要等重组后才取消，
                            // 期间可能再推进一两次，画面会与分数错开。
                            frozenFrames = IntArray(frameTimes.size) { i ->
                                seq.frameIndexAt(frameTimes[i].longValue)
                            }
                            isPlaying = false
                        } else {
                            // 恢复播放：清空定格帧，描边与 ⏳ 一起回到播放态
                            frozenFrames = null
                            isPlaying = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    // 左右下三边距等宽：底边不再叠加底部导航栏的抬高
                    .padding(32.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = if (isPlaying) "求求好运" else "再来一次",
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

/**
 * 单个海星动图：按素材时间取帧显示，圆角 25dp；
 * 得分达标时在图片外侧画一圈主题色描边（宽 5dp、距边缘 5dp）。
 */
@Composable
private fun LuckyFrameImage(
    sequence: LuckyFrameSequence?,
    materialTime: MutableState<Long>,
    frozenFrameIndex: Int?,
    highlighted: Boolean,
    strokeColor: Color,
    modifier: Modifier = Modifier,
) {
    // 素材时间每帧都在变，但只有换帧才需要重组：用 derivedStateOf 收缩到帧号变化。
    // 暂停时以 frozenFrameIndex 为准，保证画面与算分帧严格一致。
    val liveFrameIndex by remember(sequence, materialTime) {
        derivedStateOf { sequence?.frameIndexAt(materialTime.value) ?: -1 }
    }
    val frameIndex = frozenFrameIndex ?: liveFrameIndex
    val frame = remember(sequence, frameIndex) {
        if (frameIndex >= 0) sequence?.frameAt(frameIndex) else null
    }
    val frameRatio = if (sequence != null && sequence.frameHeight > 0) {
        sequence.frameWidth.toFloat() / sequence.frameHeight
    } else {
        1f
    }

    Box(
        modifier = modifier
            .aspectRatio(frameRatio)
            .drawBehind {
                if (highlighted) {
                    // 描边整体位于图片外侧：中心在「间距 + 半个线宽」的外扩偏移处，圆角半径同步外扩保持同心
                    val outward = LuckyStrokeGap.toPx() + LuckyStrokeWidth.toPx() / 2f
                    val radius = LuckyCornerRadius.toPx() + outward
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(-outward, -outward),
                        size = Size(size.width + outward * 2f, size.height + outward * 2f),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = LuckyStrokeWidth.toPx()),
                    )
                }
            }
            .clip(RoundedCornerShape(LuckyCornerRadius)),
    ) {
        if (frame != null) {
            Image(
                bitmap = frame,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }
}
