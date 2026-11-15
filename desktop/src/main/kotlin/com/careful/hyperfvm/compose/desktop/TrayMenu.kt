package com.careful.hyperfvm.compose.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.careful.hyperfvm.compose.ui.theme.HyperFVMTheme
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.roundToInt

/** 菜单卡片的尺寸：两行 36dp，上下各 6dp 内边距——桌面右键菜单的尺度，比设置页紧凑。 */
private val MenuWidth = 168.dp
private val MenuItemHeight = 36.dp
private val MenuVerticalPadding = 6.dp
private val MenuHeight = MenuItemHeight * 2 + MenuVerticalPadding * 2

/** 圆角与 Miuix 的列表弹层同源，卡片小时略收一档。 */
private val MenuCornerRadius = 14.dp

private val MenuHorizontalPadding = 16.dp
private val MenuIconSize = 18.dp
private val MenuIconEndPadding = 10.dp

/**
 * 托盘右键菜单的宿主：管「在哪弹、什么时候收」。
 *
 * 菜单是一个独立的无边框窗口（[TrayMenuWindow]），不挂在主窗口里——主窗口可能已经收进
 * 托盘，弹层必须能出现在屏幕任意位置，而且不应该被主窗口裁掉。
 *
 * 收起依旧交给 [MenuDismissHook]（全局低层鼠标钩子）：点任务栏、桌面、别的程序都能收到，
 * 且不吞掉那一次点击。
 */
internal class TrayMenuHost {

    /** 窗口是否可见（AWT 层）。 */
    var visible by mutableStateOf(false)
        private set

    /** 内容是否展开：true 播展开动画，false 播收起动画，动画结束后才把窗口藏起来。 */
    var expanded by mutableStateOf(false)
        private set

    /** 缩放原点：鼠标在卡片里的相对位置，做出「从托盘图标长出来」的观感。 */
    var transformOrigin by mutableStateOf(TransformOrigin(1f, 1f))
        private set

    private var window: Component? = null

    /** 卡片的尺寸，Compose 布局像素（物理像素，200% 缩放下是逻辑像素的两倍）。 */
    private var sizePx = IntSize.Zero

    /** 菜单的屏幕矩形（AWT 逻辑坐标）：钩子线程要读。 */
    @Volatile
    private var bounds: Rectangle? = null

    private val dismissHook = MenuDismissHook(
        isInsideMenu = { x, y ->
            val rect = bounds
            // 钩子的 pt 是**物理**像素，AWT 的 locationOnScreen 是**逻辑**像素，两种都认。
            rect != null && (rect.contains(x, y) ||
                rect.contains((x / uiScale()).roundToInt(), (y / uiScale()).roundToInt()))
        },
        onOutsideClick = { SwingUtilities.invokeLater { dismiss() } },
    )

    /** 兜底：钩子没装上或者漏事件时，别让菜单一直挂在最上层。 */
    private val safetyTimer = Timer(30_000) { dismiss() }

    /** 由窗口组合时回填：AWT 窗口句柄 + 卡片像素尺寸。 */
    fun attach(component: Component, size: IntSize) {
        window = component
        sizePx = size
    }

    /**
     * 在鼠标位置弹出。
     *
     * 先把窗口挪到位再显示：直接切可见性会让窗口先在前一帧的位置闪一下。
     */
    fun showAt(anchorX: Int, anchorY: Int) {
        val component = window ?: return
        if (sizePx == IntSize.Zero) return

        // Compose 的布局像素是**物理**像素，AWT 的 setBounds 收**逻辑**像素，200% 缩放下差一倍。
        // 不换算的话窗口会被系统再放大一次，卡片就会缩在左上角、下方空出一大截。
        val scale = uiScale()
        val width = (sizePx.width / scale).roundToInt()
        val height = (sizePx.height / scale).roundToInt()

        // 托盘贴着屏幕边缘，越界就贴边；动画原点取鼠标在卡片里的位置。
        val work = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        val left = anchorX.coerceIn(work.x, (work.x + work.width - width).coerceAtLeast(work.x))
        val top = anchorY.coerceIn(work.y, (work.y + work.height - height).coerceAtLeast(work.y))

        component.setBounds(left, top, width, height)
        bounds = Rectangle(left, top, width, height)
        transformOrigin = TransformOrigin(
            pivotFractionX = ((anchorX - left).toFloat() / width).coerceIn(0f, 1f),
            pivotFractionY = ((anchorY - top).toFloat() / height).coerceIn(0f, 1f),
        )

        visible = true
        expanded = true
        dismissHook.start()
        safetyTimer.restart()
    }

    /** 触发收起动画；窗口等动画播完由 [onCollapsed] 隐藏。 */
    fun dismiss() {
        if (!visible) return
        dismissHook.stop()
        safetyTimer.stop()
        expanded = false
    }

    /** 收起动画结束（窗口侧回调）：这时候才真正把窗口藏起来。 */
    fun onCollapsed() {
        visible = false
        bounds = null
    }
}

/**
 * 菜单窗口本体：一个无边框、透明、不进任务栏、不抢焦点的对话框窗口。
 *
 * 注意别换成 [androidx.compose.ui.window.Window]：那是一个 JFrame，会在任务栏多出一个
 * 条目；对话框窗口天生没有任务栏按钮。
 */
@Composable
internal fun TrayMenuWindow(
    host: TrayMenuHost,
    colorMode: Int,
    keyColor: Color?,
    paletteStyle: Int,
    colorSpec: Int,
    onShowWindow: () -> Unit,
    onExit: () -> Unit,
) {
    val dialogState = rememberDialogState(size = DpSize(MenuWidth, MenuHeight))
    // 展开进度：0 = 完全收起，1 = 完全展开。
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val expanded = host.expanded

    DialogWindow(
        onCloseRequest = { host.dismiss() },
        state = dialogState,
        visible = host.visible,
        title = "HyperFVM",
        undecorated = true,
        transparent = true,
        resizable = false,
        // 不抢焦点：托盘菜单不应该把当前前台窗口变灰。
        focusable = false,
        alwaysOnTop = true,
    ) {
        LaunchedEffect(window, density) {
            host.attach(
                component = window,
                size = with(density) { IntSize(MenuWidth.roundToPx(), MenuHeight.roundToPx()) },
            )
        }
        // 展开用 miuix 弹层那套弹簧（带一点回弹），收起用短促的线性收尾。
        LaunchedEffect(expanded) {
            if (expanded) {
                progress.animateTo(1f, ListPopupDefaults.FractionAnimationSpec)
            } else {
                if (progress.value > 0f) progress.animateTo(0f, tween(durationMillis = 130))
                // 收起中途又被叫起来时 expanded 已经变回 true，这时不能隐藏窗口。
                if (!host.expanded) host.onCollapsed()
            }
        }
        HyperFVMTheme(
            colorMode = colorMode,
            keyColor = keyColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val fraction = progress.value
                        scaleX = 0.6f + 0.4f * fraction
                        scaleY = 0.6f + 0.4f * fraction
                        alpha = fraction
                        transformOrigin = host.transformOrigin
                    },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(MenuCornerRadius),
                    color = MiuixTheme.colorScheme.surfaceContainer,
                ) {
                    // 每行的点击区域都顶到卡片边缘，水波纹才能铺满圆角内的整块区域。
                    Column {
                        MenuRow(
                            text = "显示主界面",
                            icon = MiuixIcons.Home,
                            paddingTop = MenuVerticalPadding,
                            onClick = {
                                host.dismiss()
                                onShowWindow()
                            },
                        )
                        MenuRow(
                            text = "退出",
                            icon = MiuixIcons.Close,
                            paddingBottom = MenuVerticalPadding,
                            onClick = {
                                host.dismiss()
                                onExit()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单行菜单项：与 miuix 下拉菜单的行一致（左图标 + 中等字重标题），按桌面尺度收紧。
 *
 * [paddingTop] / [paddingBottom] 是首尾行多出来的留白，必须叠在 clickable **之后**——
 * miuix 的 `BasicComponent` 也是 `.clickable().padding()` 的顺序，这样水波纹的遮罩范围
 * 把这段留白算进去，不会在行和圆角之间露出一条没被盖住的缝。
 */
@Composable
private fun MenuRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(
                start = MenuHorizontalPadding,
                end = MenuHorizontalPadding,
                top = paddingTop,
                bottom = paddingBottom,
            )
            .height(MenuItemHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
            modifier = Modifier
                .padding(end = MenuIconEndPadding)
                .size(MenuIconSize),
        )
        Text(
            text = text,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}
