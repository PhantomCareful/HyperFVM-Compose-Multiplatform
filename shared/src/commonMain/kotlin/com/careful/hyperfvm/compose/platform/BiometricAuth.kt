package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable

/**
 * 设备生物识别 / 锁屏密码验证的统一入口。
 *
 * Android 上是系统 `BiometricPrompt`（指纹 + 锁屏密码），Windows 上是 `Windows Hello`
 * （指纹 / 人脸 / PIN），其它桌面平台没有等价能力：[available] 恒为 false，
 * [request] 直接放行，界面因此不需要额外的平台判断。
 */
interface BiometricGate {

    /** 设备是否具备可用的验证手段：有锁屏密码或已录入生物识别。 */
    val available: Boolean

    /**
     * 这套验证手段在文案里的叫法，界面用它拼「将通过 X 验证您的身份」。
     *
     * 两端叫法不同（Android 是「指纹或锁屏密码」，Windows 是「Windows Hello」），
     * 与其让界面按平台分支，不如让实现自己报名字。
     */
    val credentialName: String

    /**
     * 弹出系统验证面板。
     *
     * @param title 面板标题（如「确保那是你」）。
     * @param subtitle 面板副标题（说明为什么验证）。
     * @param onResult 验证通过为 true；用户取消或验证失败为 false。
     */
    fun request(title: String, subtitle: String, onResult: (Boolean) -> Unit)
}

@Composable
expect fun rememberBiometricGate(): BiometricGate
