package com.careful.hyperfvm.compose.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun ViewerBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun ViewerSystemBarsEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = view.context.findActivity()?.window?.let {
            WindowInsetsControllerCompat(it, view)
        }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            // 退出时恢复成原值，不靠调用方重建。
            if (previousLightStatusBars != null) {
                controller.isAppearanceLightStatusBars = previousLightStatusBars
            }
            if (previousLightNavigationBars != null) {
                controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }
}

/** 一路往上找 ContextWrapper，拿到真正承载窗口的 Activity。 */
private fun Context.findActivity(): ComponentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}
