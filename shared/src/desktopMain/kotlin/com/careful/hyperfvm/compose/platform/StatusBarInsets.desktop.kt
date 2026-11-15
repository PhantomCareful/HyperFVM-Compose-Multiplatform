package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/** 桌面端没有状态栏，顶栏按 0 留白。 */
@Composable
actual fun rememberStatusBarHeightPx(): Int = 0
