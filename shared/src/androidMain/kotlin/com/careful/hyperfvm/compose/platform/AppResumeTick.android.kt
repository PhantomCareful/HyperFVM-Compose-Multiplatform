package com.careful.hyperfvm.compose.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Android 侧实现：监听承载窗口的 Activity 的 ON_RESUME。
 *
 * `addObserver` 会按 Activity 的当前状态补发一遍事件：组合时本来就在前台，
 * 那次 ON_RESUME 不算「回到前台」，跳过它，否则进页面就会检查两遍。
 */
@Composable
actual fun rememberAppResumeTick(): Int {
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val owner = lifecycleOwner ?: return@DisposableEffect onDispose {}
        var replaying = true
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            if (replaying) {
                replaying = false
                return@LifecycleEventObserver
            }
            tick += 1
        }
        owner.lifecycle.addObserver(observer)
        replaying = false
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    return tick
}

/** 一路往上找 ContextWrapper，拿到真正承载窗口的 Activity。 */
private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}
