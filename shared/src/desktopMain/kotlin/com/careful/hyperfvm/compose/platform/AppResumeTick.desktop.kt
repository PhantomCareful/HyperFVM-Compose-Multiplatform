package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/** 桌面端没有前台 / 后台的切换：恒为 0，页面只在进入组合时检查一次。 */
@Composable
actual fun rememberAppResumeTick(): Int = 0
