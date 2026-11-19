package com.careful.hyperfvm.compose.platform

/** 桌面端没有 Android 那套线程优先级机制，整表解析本来就只有一百多毫秒，直接不管。 */
actual fun lowerCurrentThreadPriority() = Unit

/** 桌面端无需还原。 */
actual fun restoreThreadPriority() = Unit
