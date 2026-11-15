package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/**
 * 桌面端没有系统返回手势，查看器靠标题栏的返回按钮关闭；
 * 这里不注册任何东西，也不改变系统栏配色。
 */
@Composable
actual fun ViewerBackHandler(enabled: Boolean, onBack: () -> Unit) {
}

@Composable
actual fun ViewerSystemBarsEffect() {
}
