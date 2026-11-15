package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/**
 * 查看器的返回手势/返回键拦截。
 *
 * Android 走 `OnBackPressedDispatcher`，桌面端没有这套系统手势（返回动作由标题栏返回按钮或
 * Esc 完成），actual 是空实现。默认参数只写在 expect 上，actual 不再重复。
 */
@Composable
expect fun ViewerBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * 查看器打开期间的系统栏配色。
 *
 * 查看器是黑底，状态栏图标平时按 App 主题取色（浅色界面用深色图标），
 * 不切到浅色就在黑条上看不清；退出时恢复原值。桌面端没有状态栏，actual 是空实现。
 */
@Composable
expect fun ViewerSystemBarsEffect()
