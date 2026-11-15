package com.careful.hyperfvm.compose.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.careful.hyperfvm.compose.HyperFVMApp
import com.careful.hyperfvm.compose.data.settings.CloseAction
import com.careful.hyperfvm.compose.data.settings.SettingsStore
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.DesktopAppEnv
import com.careful.hyperfvm.compose.ui.theme.keyColorFor
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

/**
 * Windows 桌面入口：窗口 + 托盘常驻。
 *
 * 生命周期约定（与 Android 不同，桌面端没有「系统管家」来托住进程）：
 * - 点窗口右上角的关闭按钮**不退出**，只是把窗口收回托盘，进程继续跑；
 * - 托盘图标左键单击 / 右键菜单第一项 = 重新显示窗口；
 * - 托盘右键菜单的「退出」才是真正的结束。
 *
 * 界面与数据全部来自 :shared，这里只做窗口与托盘的壳。
 */
fun main() {
    // 环境与设置在窗口出现之前就建好：首帧直接是用户上次选择的配色。
    val env = DesktopAppEnv()
    val store = SettingsStore(env)
    // 桌面端没有下载通知这类外部入口，仍保留同一个参数以复用 Android 的根组合函数。
    val pendingRoute = MutableStateFlow<Route?>(null)

    application {
        var windowVisible by remember { mutableStateOf(true) }
        // 首次尺寸先给个占位，真正的尺寸在窗口里按「屏幕高度 80% + 16:9」校正
        // （见下面的 LaunchedEffect）：高 DPI 下自己去算 dp 容易差一截，直接读 AWT 的
        // 屏幕尺寸最准——AWT 的用户单位跟 Compose 的 dp 在这里是同一套。
        val windowState = rememberWindowState(
            size = DpSize(405.dp, 720.dp),
            position = WindowPosition(Alignment.Center),
        )

        // 托盘与菜单共用：最小化状态也一并复位，否则「显示」之后窗口还是收起的。
        fun showWindow() {
            windowState.isMinimized = false
            windowVisible = true
        }

        // 托盘的图标建在 AWT/Win32 侧（见 AppTray），右键菜单是独立的 Compose 窗口
        // （见 TrayMenuWindow），这里只把两边接起来。
        val menuHost = remember { TrayMenuHost() }
        DisposableEffect(Unit) {
            val tray = AppTray(
                onShowWindow = ::showWindow,
                onMenuRequest = menuHost::showAt,
            )
            tray.install()
            onDispose { tray.uninstall() }
        }

        // 菜单的配色跟随设置：单独一个窗口不共享主窗口的 MiuixTheme，所以参数要显式传。
        val settings = store.settings
        TrayMenuWindow(
            host = menuHost,
            colorMode = settings.colorMode,
            keyColor = keyColorFor(settings.keyColorIndex),
            paletteStyle = settings.paletteStyle,
            colorSpec = settings.colorSpec,
            onShowWindow = ::showWindow,
            onExit = ::exitApplication,
        )

        Window(
            // 关闭按钮的行为由设置决定：默认收回托盘继续跑（桌面端没有系统管家托住进程），
            // 选「直接退出」才结束进程。托盘菜单里的「退出」始终直接退出，不受这个设置影响。
            onCloseRequest = {
                if (store.settings.closeAction == CloseAction.Exit) {
                    exitApplication()
                } else {
                    windowVisible = false
                }
            },
            state = windowState,
            visible = windowVisible,
            title = "HyperFVM",
            icon = AppIcon,
        ) {
            LaunchedEffect(Unit) {
                // 「屏幕高度 80%，长宽比 16:9」；如果 80% 会盖到任务栏，就退到可用高度。
                val screen = Toolkit.getDefaultToolkit().screenSize
                val work = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                val height = minOf((screen.height * 0.8f).toInt(), work.height)
                val width = height * 9 / 16
                window.setSize(width, height)
                // 交给 AWT 居中：多显示器/虚拟显示器下自己算坐标容易偏。
                window.setLocationRelativeTo(null)
                println("[window] screen=${screen.width}x${screen.height} work=${work.width}x${work.height} -> ${width}x$height")
            }
            HyperFVMApp(env = env, store = store, pendingRoute = pendingRoute)
        }
    }
}
