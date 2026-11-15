package com.careful.hyperfvm.compose.desktop

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathParser

/**
 * 托盘与任务栏用的应用图标：品牌色圆角方块 + 白色字标。
 *
 * 直接画出来而不是加载图片：托盘图标要交给 AWT，而 shared 的 composeResources 里
 * 只有 Android 用的 24dp 单色矢量（白字标在浅色任务栏上会看不见）。这里补一个
 * 带底色的版本，顺带把 `gradlew run` 时的咖啡杯图标换掉。
 *
 * 字标路径与 `app/src/main/res/drawable/app_icon.xml` 保持一致（24 视口）。
 */
internal object AppIcon : Painter() {

    /** 与 `@color/ic_launcher_background` 同一个值。 */
    private val Background = Color(0xFF3482FF)

    /** 图标外框相对边长的比例：0.22 接近 Android 自适应图标的圆角观感。 */
    private const val CornerPercent = 0.22f

    /** 字标在 24 视口里只占约 20.25，留一点内边距避免贴边。 */
    private const val GlyphScale = 0.86f

    private val glyph: Path = PathParser().parsePathString(GlyphPathData).toPath()

    override val intrinsicSize: Size = Size(64f, 64f)

    override fun DrawScope.onDraw() {
        val side = size.minDimension
        drawRoundRect(
            color = Background,
            size = Size(side, side),
            cornerRadius = CornerRadius(side * CornerPercent),
        )
        // 先按边长把 24 视口缩放到整块画布，再乘 GlyphScale 留出内边距，最后居中。
        val scale = side / Viewport * GlyphScale
        val offset = (side - Viewport * scale) / 2f
        withTransform({
            translate(offset, offset)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path = glyph, color = Color.White)
        }
    }

    private const val Viewport = 24f

    private const val GlyphPathData =
        "M17.73,12L21.71,8.04C22.1,7.65 22.1,7 21.71,6.63L17.37,2.29C17,1.9 16.35,1.9 " +
            "15.96,2.29L12,6.27L8,2.29C7.8,2.1 7.55,2 7.29,2C7.04,2 6.78,2.1 6.59,2.29L2.25," +
            "6.63C1.86,7 1.86,7.65 2.25,8.04L6.23,12L2.25,16C1.86,16.39 1.86,17 2.25,17.41L" +
            "6.59,21.75C7,22.14 7.61,22.14 8,21.75L12,17.77L15.96,21.75C16.16,21.95 16.41," +
            "22.04 16.67,22.04C16.93,22.04 17.18,21.94 17.38,21.75L21.72,17.41C22.11,17 " +
            "22.11,16.39 21.72,16L17.73,12M12,9A1,1 0 0,1 13,10A1,1 0 0,1 12,11A1,1 0 0,1 " +
            "11,10A1,1 0 0,1 12,9M7.29,10.96L3.66,7.34L7.29,3.71L10.91,7.33L7.29,10.96M10," +
            "13A1,1 0 0,1 9,12A1,1 0 0,1 10,11A1,1 0 0,1 11,12A1,1 0 0,1 10,13M12,15A1,1 0 " +
            "0,1 11,14A1,1 0 0,1 12,13A1,1 0 0,1 13,14A1,1 0 0,1 12,15M14,11A1,1 0 0,1 15," +
            "12A1,1 0 0,1 14,13A1,1 0 0,1 13,12A1,1 0 0,1 14,11M16.66,20.34L13.03,16.72L16." +
            "66,13.09L20.28,16.71L16.66,20.34Z"
}
