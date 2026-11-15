package com.careful.hyperfvm.compose.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android 侧实现：13 岁以上才需要动态申请 POST_NOTIFICATIONS，更早的系统装完即有。
 * 已经授权时直接跳过，不弹第二次。
 */
@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    return remember(context, launcher) {
        {
            val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
