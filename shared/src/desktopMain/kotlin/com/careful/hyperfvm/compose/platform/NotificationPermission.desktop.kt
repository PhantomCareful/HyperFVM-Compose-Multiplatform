package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/** 桌面端没有通知权限概念，申请动作直接忽略。 */
@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = {}
