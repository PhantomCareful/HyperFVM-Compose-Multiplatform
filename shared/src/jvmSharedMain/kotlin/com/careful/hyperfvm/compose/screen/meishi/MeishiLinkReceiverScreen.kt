package com.careful.hyperfvm.compose.screen.meishi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.careful.hyperfvm.compose.component.meishi.MeishiLinkReceiverSheet
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.LocalUpdateSettings
import com.careful.hyperfvm.compose.data.settings.SettingsStore
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.Toaster
import com.careful.hyperfvm.compose.ui.LocalBlurBackdrop
import com.careful.hyperfvm.compose.ui.rememberWindowBackdrop
import com.careful.hyperfvm.compose.ui.theme.HyperFVMTheme
import com.careful.hyperfvm.compose.ui.theme.keyColorFor
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled

/**
 * 链接中转窗口的根：一层透明的 Scaffold 只当弹层宿主，背后就是用户点链接的那个应用。
 *
 * 与主界面那套 [com.careful.hyperfvm.compose.HyperFVMApp] 的区别只在两处：
 * 背景必须透明（不能把来源应用盖住），并且弹层收尾后要把整个窗口关掉。
 * 主题、缩放、提示这些东西照旧，弹层在两处的大小和配色才一致。
 */
@Composable
fun MeishiLinkReceiverScreen(
    env: AppEnv,
    store: SettingsStore,
    link: String,
    onFinished: () -> Unit,
) {
    val settings = store.settings
    // 界面缩放跟主界面一致：同一个弹层在两处不能一大一小。
    val systemDensity = LocalDensity.current
    val scaledDensity = remember(systemDensity, settings.uiScale) {
        Density(systemDensity.density * settings.uiScale, systemDensity.fontScale)
    }
    val toaster = remember(env) { Toaster { env.showToast(it) } }
    // 弹层收起要先播完退出动画：点取消 / 添加时只把链接置空，动画结束后再关窗口。
    var pending by remember { mutableStateOf<String?>(link) }

    HyperFVMTheme(
        colorMode = settings.colorMode,
        keyColor = keyColorFor(settings.keyColorIndex),
        paletteStyle = settings.paletteStyle,
        colorSpec = settings.colorSpec,
    ) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalAppSettings provides settings,
            LocalUpdateSettings provides store::update,
            LocalAppEnv provides env,
            LocalToaster provides toaster,
            LocalSquircleEnabled provides true,
        ) {
            // 透明窗口背后是来源应用，没有页面可采样，玻璃弹层会退化成不透明实色。
            // 垫一层专用取样层：它只把内容录进离屏层（见 rememberWindowBackdrop），
            // 屏幕上仍然透明，弹层取样到的就是这层「假背板」。
            val receiverBackdrop = rememberWindowBackdrop()
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (receiverBackdrop != null) {
                                Modifier.layerBackdrop(receiverBackdrop)
                            } else {
                                Modifier
                            },
                        ),
                )
                // 弹层与取样层是兄弟节点：玻璃面板才能安全地取样背板。
                CompositionLocalProvider(LocalBlurBackdrop provides receiverBackdrop) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        // 窗口本身是透明的，这里再刷一层主题色就会把来源应用整块盖住。
                        containerColor = Color.Transparent,
                    ) { _ ->
                        MeishiLinkReceiverSheet(
                            link = pending,
                            onDismissRequest = { pending = null },
                            onFinished = onFinished,
                        )
                    }
                }
            }
        }
    }
}
