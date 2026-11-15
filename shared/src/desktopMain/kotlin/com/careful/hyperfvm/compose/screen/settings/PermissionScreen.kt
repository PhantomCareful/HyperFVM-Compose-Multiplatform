package com.careful.hyperfvm.compose.screen.settings

import androidx.compose.runtime.Composable

/**
 * 桌面端没有这些系统授权页，设置页也不会给出入口。
 * 万一被导航到这里，直接退回去，而不是显示一屏无法操作的空列表。
 */
@Composable
actual fun PermissionScreen(onBack: () -> Unit) {
    onBack()
}
