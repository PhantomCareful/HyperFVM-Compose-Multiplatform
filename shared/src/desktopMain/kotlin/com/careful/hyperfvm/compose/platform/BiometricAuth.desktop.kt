package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 桌面端的验证入口：没有系统生物识别面板，直接放行。
 *
 * 与 [PlatformCapabilities.addToCalendar] 的处理方式一致——能力缺失时让入口
 * 自然退化成「没有这一层」，而不是把管理界面锁死。
 */
@Composable
actual fun rememberBiometricGate(): BiometricGate = remember { NoOpBiometricGate }

private object NoOpBiometricGate : BiometricGate {

    override val available: Boolean = false

    override fun request(title: String, subtitle: String, onResult: (Boolean) -> Unit) {
        onResult(true)
    }
}
