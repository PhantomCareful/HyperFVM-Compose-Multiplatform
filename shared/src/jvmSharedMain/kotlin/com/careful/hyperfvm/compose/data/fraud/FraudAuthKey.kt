package com.careful.hyperfvm.compose.data.fraud

/**
 * 查黑接口的 `Authorization` KEY：不进仓库，由宿主在启动时注入。
 *
 * - Android：`app` 模块的 BuildConfig 从本机 `local.properties` 读入；
 * - 桌面：`desktop` 模块把同一个字段拼成 `-D` 参数写进 jvmArgs。
 *
 * 取不到时为空串，请求自动退回不需要鉴权的限流接口，功能不受影响。
 */
object FraudAuthKey {

    /** 系统属性名，Android 与桌面共用同一套注入约定。 */
    const val PropertyName = "hyperfvm.fraudAuthKey"

    @Volatile
    var value: String = System.getProperty(PropertyName).orEmpty()
}
