package com.careful.hyperfvm.compose.component.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用级的数据图查看器宿主。
 *
 * 数据图合集页把查看器做成页面内的覆盖层就够了——它就盖在自己的瀑布流上。
 * 但仪表盘详情是从底部弹层里点开的：弹层由根 Scaffold 的弹层宿主绘制，
 * 一级页面又在带悬浮底栏的 Scaffold 里，页面内的覆盖层会被底栏压住。
 * 所以查看器统一挂在导航宿主之上（见 `HyperFVMApp` 的 `MainFrame`），
 * 由这个对象保存「当前在看的图」。
 *
 * 打开时机由调用方决定：仪表盘会等弹层完全收起后再 [show]，
 * 这样从卡片矩形展开的转场不会被正在滑走的弹层盖住。
 */
object DataImageViewerHost {

    private var current by mutableStateOf<DataImageViewerState?>(null)

    val state: DataImageViewerState?
        get() = current

    fun show(state: DataImageViewerState) {
        current = state
    }

    fun dismiss() {
        current = null
    }
}

/** 渲染 [DataImageViewerHost] 里的查看器；没有在看的图时什么都不画。 */
@Composable
internal fun DataImageViewerHostOverlay() {
    val state = DataImageViewerHost.state
    if (state != null) {
        DataImageViewer(state = state, onDismiss = DataImageViewerHost::dismiss)
    }
}
