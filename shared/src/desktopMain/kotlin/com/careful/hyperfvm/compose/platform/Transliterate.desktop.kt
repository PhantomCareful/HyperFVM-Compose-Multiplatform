package com.careful.hyperfvm.compose.platform

import com.ibm.icu.text.Transliterator

/**
 * 桌面侧用 Maven 上的 ICU4J，API 与 Android 自带的那份同名同形。
 *
 * ICU4J 只有十几 MB 且只在桌面包里出现，不影响 APK。同样加锁：ICU4J 的实例也非线程安全。
 */
actual fun createHanTransliterator(): ((String) -> String)? {
    val transliterator = runCatching {
        Transliterator.getInstance("Han-Latin; Latin-ASCII")
    }.getOrNull() ?: return null
    val lock = Any()
    return { text -> synchronized(lock) { transliterator.transliterate(text) } }
}
