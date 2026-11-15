package com.careful.hyperfvm.compose.screen.settings

import androidx.compose.runtime.Composable

/**
 * 授权检查页：通知、安装、受支持链接、剪贴板四项系统授权的跳转入口。
 *
 * 这一整套只在 Android 上有意义（[com.careful.hyperfvm.compose.platform.PlatformCapabilities.systemPermissions]
 * 为 true 时设置页才会出现入口），所以实现整个放在 androidMain。
 */
@Composable
expect fun PermissionScreen(onBack: () -> Unit)
