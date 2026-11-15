package com.careful.hyperfvm.compose.platform

import android.app.Activity

/**
 * 系统入口（通知等）要跳回的 Activity。
 *
 * `:shared` 不能反向依赖 `:app`，所以这里只留一个由宿主注入的类引用；
 * Android 侧在 `MainActivity.onCreate` 里赋值一次，桌面端不涉及。
 */
object AppLaunchTarget {

    /** 「点通知打开数据图合集页」的 Intent extra 键，Service 与 Activity 共用同一个常量。 */
    const val ExtraOpenDataImages = "com.careful.hyperfvm.compose.extra.OPEN_DATA_IMAGES"

    @Volatile
    var activityClass: Class<out Activity>? = null
}
