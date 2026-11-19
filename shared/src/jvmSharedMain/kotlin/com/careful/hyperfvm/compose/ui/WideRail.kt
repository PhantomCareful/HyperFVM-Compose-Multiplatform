package com.careful.hyperfvm.compose.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 宽屏侧栏给页面内容让出的起始宽度。
 *
 * 由主界面骨架提供，只在布局阶段调用：玻璃侧栏折叠时它逐帧变化，读在布局里才不会
 * 把整页重组。窄屏与二级页面没有侧栏，默认 0。
 */
val LocalRailContentInset = staticCompositionLocalOf<() -> Dp> { { 0.dp } }

/**
 * 宽屏玻璃侧栏的展开进度（0 = 收起成顶部胶囊，1 = 展开成面板）。
 *
 * 页面顶栏的标题据此淡出：收起后胶囊就是顶栏本身，标题要让位（与 demo 一致）。
 * 只在绘制阶段读，逐帧不重组。
 */
val LocalRailTitleAlpha = staticCompositionLocalOf<() -> Float> { { 1f } }
