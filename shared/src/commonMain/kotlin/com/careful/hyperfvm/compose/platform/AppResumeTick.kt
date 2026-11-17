package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/**
 * App 回到前台的次数：Android 上每次 `Activity.onResume` 自增，
 * 界面拿它当「重新检查一次」的时机（对应 Java 版在 onResume 里做的事）。
 *
 * 桌面端没有前台 / 后台的切换，actual 恒为 0：那一端只在仪表盘重新
 * 进入组合、或从二级页返回时才检查。
 */
@Composable
expect fun rememberAppResumeTick(): Int
