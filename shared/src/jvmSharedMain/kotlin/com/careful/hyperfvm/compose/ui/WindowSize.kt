package com.careful.hyperfvm.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * 是否使用宽屏布局（侧边 NavigationRail 替代底部 NavigationBar）。
 *
 * 断点与 miuix 官方 demo 一致：窗口宽度 >= 840dp，或宽度 >= 600dp 且接近横屏比例。
 */
@Composable
fun isWideLayout(): Boolean {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    return with(density) {
        val width = size.width.toDp()
        val height = size.height.toDp()
        width >= 840.dp || (width >= 600.dp && height / width < 1.2f)
    }
}

/** NavigationRail 是否展开为带文字标签的宽态。 */
@Composable
fun shouldExpandNavigationRail(): Boolean {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    return with(density) { size.width.toDp() >= 1200.dp }
}

/**
 * 是否按平板（PAD）布局：断点对齐 Java 版的 `layout-sw600dp` 资源限定符，
 * 窗口最短边 >= 600dp 即为 true，竖屏平板也包含在内。
 * 与 [isWideLayout]（是否换侧边导航栏）是两个独立概念，不要互相替代。
 */
@Composable
fun isTabletLayout(): Boolean {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    return with(density) {
        minOf(size.width, size.height).toDp() >= 600.dp
    }
}
