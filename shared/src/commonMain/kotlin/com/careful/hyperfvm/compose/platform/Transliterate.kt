package com.careful.hyperfvm.compose.platform

/**
 * 汉字 → 无声调拼音的转写器（`Han-Latin; Latin-ASCII`）。
 *
 * 两端都有 ICU，但不在同一个包下：Android 用系统内置的 `android.icu`（不增加 APK 体积），
 * 桌面用 Maven 上的 `com.ibm.icu`。返回 null 表示当前环境没有 ICU，调用方要能退化。
 *
 * 返回的函数**必须线程安全**：内部实现自己加锁。
 */
expect fun createHanTransliterator(): ((String) -> String)?
