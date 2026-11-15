package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/**
 * 「通知」权限的申请入口（Android 13+ 的 POST_NOTIFICATIONS）。
 *
 * 权限只影响进度能否显示在通知栏，拒绝也照常下载，所以不需要返回值。
 * 桌面端没有这个权限概念，actual 是空实现。
 */
@Composable
expect fun rememberNotificationPermissionRequester(): () -> Unit
