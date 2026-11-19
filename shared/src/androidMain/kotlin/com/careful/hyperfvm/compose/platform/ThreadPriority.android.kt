package com.careful.hyperfvm.compose.platform

import android.os.Process

/**
 * Android 上把线程 nice 值降到 [Process.THREAD_PRIORITY_BACKGROUND]，
 * 界面线程（THREAD_PRIORITY_DISPLAY）就不会被后台解析拖慢。
 */
actual fun lowerCurrentThreadPriority() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
}

/** 还原成默认优先级；解析线程来自协程池，用完必须还回去，别把池里的线程降级。 */
actual fun restoreThreadPriority() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
}
