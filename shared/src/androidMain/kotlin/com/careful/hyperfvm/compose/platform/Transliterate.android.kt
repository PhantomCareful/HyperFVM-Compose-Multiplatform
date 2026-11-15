package com.careful.hyperfvm.compose.platform

import android.icu.text.Transliterator

/**
 * Android 侧用系统内置的 ICU（API 29+ 可用），不引入任何额外依赖。
 *
 * Transliterator 实例不是线程安全的，这里统一加锁；调用频率很低（启动时索引 1000 来个名字），
 * 锁竞争可以忽略。
 */
actual fun createHanTransliterator(): ((String) -> String)? {
    val transliterator = runCatching {
        Transliterator.getInstance("Han-Latin; Latin-ASCII")
    }.getOrNull() ?: return null
    val lock = Any()
    return { text -> synchronized(lock) { transliterator.transliterate(text) } }
}
