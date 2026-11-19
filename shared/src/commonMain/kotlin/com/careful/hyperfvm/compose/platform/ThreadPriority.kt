package com.careful.hyperfvm.compose.platform

/**
 * 把当前线程降到后台优先级，配合 [restoreThreadPriority] 成对使用。
 *
 * 整表解析这类纯 CPU 的活即使跑在后台线程上，也会和界面线程抢 CPU 时间片，
 * 启动阶段尤其明显；Android 上真正能压住的是线程的 nice 值，桌面端没有对应机制。
 */
expect fun lowerCurrentThreadPriority()

/** 把当前线程还原成默认优先级。只在 [lowerCurrentThreadPriority] 之后调用。 */
expect fun restoreThreadPriority()
