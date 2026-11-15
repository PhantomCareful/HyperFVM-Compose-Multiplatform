package com.careful.hyperfvm.compose.platform

/**
 * 跨端日志出口。
 *
 * 公共代码不直接引用 android.util.Log，而是把日志送进一个可替换的 sink：
 * Android 侧在 [AndroidAppEnv] 里接到 Logcat，桌面侧直接打 stdout。
 */
object Logs {

    /** 默认输出：桌面端 / 单元测试直接用标准输出。 */
    var sink: (tag: String, message: String, error: Throwable?) -> Unit =
        { tag, message, error ->
            if (error == null) println("[$tag] $message") else println("[$tag] $message: $error")
        }

    fun w(tag: String, message: String, error: Throwable? = null) {
        sink(tag, message, error)
    }

    /** 恢复默认输出，供测试使用。 */
    fun reset() {
        sink = { tag, message, error ->
            if (error == null) println("[$tag] $message") else println("[$tag] $message: $error")
        }
    }
}
