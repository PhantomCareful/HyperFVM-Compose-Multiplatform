package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/**
 * 状态栏占用的高度，单位 px（**忽略可见性**：沉浸态下也要按真实高度留白）。
 *
 * `WindowInsets.statusBarsIgnoringVisibility` 只在 Android 侧有，桌面端没有状态栏，
 * actual 恒为 0。取 px 而不是 Dp，是因为调用点都在 `Modifier.layout` 的测距阶段。
 */
@Composable
expect fun rememberStatusBarHeightPx(): Int
