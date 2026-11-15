package com.careful.hyperfvm.compose.desktop

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.SystemTray
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * 托盘图标：只负责图标本身与点击回调。
 *
 * 右键菜单不在这里画，交给 [TrayMenuWindow]（独立的无边框 Compose 窗口）——
 * AWT/Swing 的弹出菜单要么是 Java 自绘的 owner-draw 菜单（WinXP 观感、中文还容易出问题），
 * 要么没法做 miuix 的展开收起动画。
 */
internal class AppTray(
    /** 左键单击：召回主窗口。 */
    private val onShowWindow: () -> Unit,
    /** 右键：请求把菜单弹到鼠标位置（AWT 逻辑坐标）。 */
    private val onMenuRequest: (x: Int, y: Int) -> Unit,
) {
    private var trayIcon: NativeTrayIcon? = null

    fun install() {
        // 图标按**设备像素**出图：托盘位是 16 逻辑单位，200% 缩放下要 32 物理像素。
        // 走 [NativeTrayIcon] 而不是 AWT 的 TrayIcon：后者固定按 16px 生成 HICON，
        // 再由系统拉大一倍，图标永远是糊的。
        val logical = SystemTray.getSystemTray().trayIconSize.width
        val pixels = (logical * uiScale()).roundToInt().coerceAtLeast(logical)
        trayIcon = NativeTrayIcon(
            tooltip = "HyperFVM",
            onLeftClick = onShowWindow,
            onRightClick = {
                MouseInfo.getPointerInfo()?.location?.let { onMenuRequest(it.x, it.y) }
            },
        ).also { it.install(AppIcon.toAwtImage(pixels, pixels)) }
        println("[tray] icon=${logical}px logical, bitmap=${pixels}px")
    }

    fun uninstall() {
        trayIcon?.remove()
        trayIcon = null
    }
}

/** 系统 UI 缩放比例（200% 缩放下返回 2.0）。 */
internal fun uiScale(): Double =
    GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX

/** 把矢量图标渲染成 AWT 位图（按给定像素尺寸出图，不做额外缩放）。 */
private fun Painter.toAwtImage(width: Int, height: Int): BufferedImage {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    val size = Size(width.toFloat(), height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        with(this@toAwtImage) { draw(size) }
    }
    return bitmap.toAwtImage()
}
