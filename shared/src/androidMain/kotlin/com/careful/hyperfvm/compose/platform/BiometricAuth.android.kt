package com.careful.hyperfvm.compose.platform

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android 侧的验证入口：直接用平台的 [BiometricPrompt]，不额外引入 androidx.biometric。
 *
 * minSdk 31 已经覆盖 `setAllowedAuthenticators`（API 30）与 `DEVICE_CREDENTIAL`，
 * 所以允许「任意强度生物识别 + 锁屏密码」这一组合：设备没有指纹时，
 * 用户可以退回输入锁屏密码，不至于把管理界面锁死。
 */
@Composable
actual fun rememberBiometricGate(): BiometricGate {
    val context = LocalContext.current
    return remember(context) { AndroidBiometricGate(context) }
}

private class AndroidBiometricGate(context: Context) : BiometricGate {

    /** 平台 BiometricPrompt 要求能启动系统界面的 Context，applicationContext 会直接抛异常。 */
    private val activity: Activity? = context.findActivity()

    /** 系统面板里可选的两条路，文案也照这个说。 */
    override val credentialName: String = "指纹或锁屏密码"

    /**
     * 有锁屏密码就当作可用。
     *
     * Android 要求先设锁屏密码才能录指纹 / 面容，所以 `isDeviceSecure` 为 true
     * 时至少能走 [BiometricManager.Authenticators.DEVICE_CREDENTIAL] 这一条路。
     */
    override val available: Boolean
        get() {
            val host = activity ?: return false
            return runCatching {
                host.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
            }.getOrDefault(false)
        }

    override fun request(title: String, subtitle: String, onResult: (Boolean) -> Unit) {
        val host = activity
        if (host == null) {
            // 理论上不会发生（入口只在界面里用）；退化成放行比把用户挡在外面更合理。
            onResult(true)
            return
        }
        val prompt = try {
            BiometricPrompt.Builder(host)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build()
        } catch (_: Exception) {
            onResult(true)
            return
        }
        // 结果只回调一次：取消、报错、成功都走这里，界面据此决定是否继续。
        prompt.authenticate(
            CancellationSignal(),
            host.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onResult(false)
                }

                // 单次指纹不匹配不算失败：面板会留在原地等用户重试，
                // 真正的结论由 onAuthenticationSucceeded / onAuthenticationError 给出。
                override fun onAuthenticationFailed() = Unit
            },
        )
    }
}

/** 从可能被包装的 Context 里取出 Activity（LocalContext 常见是 ContextThemeWrapper）。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
