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
import java.io.File

/**
 * Windows 桌面入口：窗口 + 托盘常驻。
 *
 * 生命周期约定（与 Android 不同，桌面端没有「系统管家」来托住进程）：
 * - 点窗口右上角的关闭按钮**不退出**，只是把窗口收回托盘，进程继续跑；
 * - 托盘图标左键单击 / 右键菜单第一项 = 重新显示窗口；
 * - 托盘右键菜单的「退出」才是真正的结束；
 * - 重复双击启动不会开第二个窗口：后启动的进程把激活请求递给已在跑的实例后立刻退出
 *   （见 [SingleInstance]），已有窗口则从托盘 / 最小化状态回到前台。
 *
 * 界面与数据全部来自 :shared，这里只做窗口与托盘的壳。
 */
fun main() {
    // 环境与设置在窗口出现之前就建好：首帧直接是用户上次选择的配色。
    val env = DesktopAppEnv()

    // 单实例：已经有一个在跑时，这一次启动只负责把它的窗口唤到前台，自己不再开窗口。
    // dev 版（不是 Gradle 注入版本号的那种）单独用一格实例目录，免得 IDE 里调试时
    // 把用户装好的正式版顶掉。
    val instanceDir = File(env.filesDir, if (env.appVersionName == "dev") "instance-dev" else "instance")
    val instance = SingleInstance.acquire(instanceDir)
    if (instance == null) {
        val delivered = SingleInstance.requestActivate(instanceDir)
        println("[single-instance] 已有实例在运行，激活请求送达=$delivered")
        return
    }
    instance.start()

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

            // 第二次双击启动时，另一个进程会通过单实例通道把请求送到这里：
            // 走和托盘左键单击同一条路径把窗口拉回来。状态写入要等下一帧才落到 AWT
            // 窗口上，所以这里直接把窗口置为可见再要焦点，否则 toFront 可能作用在
            // 还隐藏着的窗口上、顶不到最前面。
            LaunchedEffect(Unit) {
                while (true) {
                    instance.activateRequests.receive()
                    showWindow()
                    window.isVisible = true
                    // 第二个进程已经把前台权限让过来了（见 SingleInstance）；这里再「临时置顶」兜一次底，
                    // 保证窗口真的冒到最前面，而不是只闪一下任务栏。
                    window.isAlwaysOnTop = true
                    window.isAlwaysOnTop = false
                    window.toFront()
                    window.requestFocus()
                }
            }
            HyperFVMApp(env = env, store = store, pendingRoute = pendingRoute)
        }
    }

    // 正常退出时把锁和端口文件一起清掉；被强杀的情况由操作系统释放锁。
    instance.close()
}
