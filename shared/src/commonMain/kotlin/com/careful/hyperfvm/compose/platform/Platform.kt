package com.careful.hyperfvm.compose.platform

/**
 * 平台名，用于启动日志与设置页「关于」信息。
 *
 * 这是 KMP 骨架的探针：确认 commonMain 里的 expect 能分别落到 androidMain / desktopMain。
 */
expect val platformName: String

/**
 * 是否为桌面端。
 *
 * 用来分流鼠标专属的交互（如拖拽滚动）：Compose Desktop 的可滚动容器只认滚轮，
 * 按住鼠标拖动不会动，需要自己包一层。
 */
expect val isDesktopPlatform: Boolean
