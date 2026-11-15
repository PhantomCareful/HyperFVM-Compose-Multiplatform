package com.careful.hyperfvm.compose.platform

import android.os.Build

actual val platformName: String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

actual val isDesktopPlatform: Boolean = false
