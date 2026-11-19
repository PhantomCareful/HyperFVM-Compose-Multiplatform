package com.careful.hyperfvm.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu

/**
 * 页面根宿主：柔光玻璃的弹出菜单都渲染在这里。
 *
 * 玻璃面板要取样页面背板（[LocalBlurBackdrop]），而取样层录的是内容区子树；
 * 面板本身必须是取样层的兄弟节点，落在内容区里就会录到自己，RenderThread 会栈溢出。
 * [com.careful.hyperfvm.compose.screen.ScreenScaffold] 把宿主挂在 Scaffold 之外
 * （与整块 Scaffold 平级，后绘制），菜单通过 [GlassMenuPanelHosted] 登记到这里：
 * 既与内容取样层是兄弟，也不会被顶栏盖住。
 */
@Stable
class GlassMenuHostState {
    internal val panels = mutableStateListOf<GlassMenuPanel>()
}

/** 一块登记的玻璃面板；内容由登记方每次重组时刷新。 */
internal class GlassMenuPanel {
    internal var content: (@Composable BoxScope.() -> Unit)? by mutableStateOf(null)
}

val LocalGlassMenuHost = staticCompositionLocalOf<GlassMenuHostState?> { null }

/** 由 [com.careful.hyperfvm.compose.screen.ScreenScaffold] 调用，把登记的玻璃面板摆到页面根上。 */
@Composable
fun BoxScope.GlassMenuHost(state: GlassMenuHostState) {
    state.panels.forEach { panel -> panel.content?.invoke(this) }
}

/**
 * 把一块玻璃面板登记到页面根宿主，返回是否登记成功。
 *
 * 返回 false 表示当前不在 [com.careful.hyperfvm.compose.screen.ScreenScaffold] 内容区里，
 * 调用方应退回非玻璃实现——没有宿主时面板会落在取样层内部，既会被裁切也可能闪退。
 */
@Composable
fun GlassMenuPanelHosted(content: @Composable BoxScope.() -> Unit): Boolean {
    val host = LocalGlassMenuHost.current ?: return false
    val panel = remember { GlassMenuPanel() }
    DisposableEffect(host) {
        host.panels.add(panel)
        onDispose { host.panels.remove(panel) }
    }
    SideEffect { panel.content = content }
    return true
}

/**
 * 顶栏的图标下拉菜单。
 *
 * 柔光玻璃下按钮是 OS4 玻璃圆钮，菜单从它展开（[GlassIconDropdownMenu] 自带展开形变），
 * 轻透磨砂下保持原生 [OverlayIconDropdownMenu]。
 * 参数取两个实现共有的交集，调用方换名字即可。
 */
@Composable
fun AppIconDropdownMenu(
    entries: List<DropdownEntry>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    collapseOnSelection: Boolean = entries.size <= 1,
    content: @Composable () -> Unit,
) {
    val fallback: @Composable () -> Unit = {
        OverlayIconDropdownMenu(
            entries = entries,
            modifier = modifier,
            enabled = enabled,
            collapseOnSelection = collapseOnSelection,
            content = content,
        )
    }
    if (!LocalAppSettings.current.isGlassStyle) {
        fallback()
        return
    }

    val actualEnabled = enabled && entries.any { it.items.isNotEmpty() }
    var show by remember { mutableStateOf(false) }
    val anchor = rememberGlassPopupAnchor()
    val backdrop = overlayBackdrop()
    val hosted = GlassMenuPanelHosted {
        GlassIconDropdownMenu(
            show = show,
            onDismissRequest = { show = false },
            anchor = anchor,
            anchorContent = content,
            entries = entries,
            backdrop = backdrop,
            collapseOnSelection = collapseOnSelection,
        )
    }
    if (!hosted) {
        fallback()
        return
    }
    GlassIconButton(
        onClick = { if (actualEnabled) show = !show },
        modifier = modifier
            .glassPopupAnchor(
                anchor = anchor,
                cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
            )
            .alpha(if (actualEnabled) 1f else 0.4f),
    ) {
        Box(modifier = Modifier.glassPopupAnchorContent(anchor)) {
            content()
        }
    }
}
