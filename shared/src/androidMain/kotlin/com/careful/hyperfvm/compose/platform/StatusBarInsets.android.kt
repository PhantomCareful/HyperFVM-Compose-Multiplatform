package com.careful.hyperfvm.compose.platform

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun rememberStatusBarHeightPx(): Int {
    // getter 本身是 @Composable，必须在可组合作用域里读好；调用点是测距阶段，
    // 所以这里直接把 Dp 换算成 px 交出去。
    val insets = WindowInsets.statusBarsIgnoringVisibility
    val density = LocalDensity.current
    return insets.getTop(density)
}
