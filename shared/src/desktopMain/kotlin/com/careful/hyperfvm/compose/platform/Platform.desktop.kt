package com.careful.hyperfvm.compose.platform

actual val platformName: String =
    "Desktop ${System.getProperty("os.name")} ${System.getProperty("os.version")} / JVM ${System.getProperty("java.version")}"

actual val isDesktopPlatform: Boolean = true
