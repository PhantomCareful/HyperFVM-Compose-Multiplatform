package com.careful.hyperfvm.compose.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune

/**
 * 主界面的一级导航。顺序即 Pager 的页序，也是 NavigationBar / NavigationRail 的排列顺序。
 */
enum class MainTab(val label: String, val icon: ImageVector) {
    Dashboard("仪表盘", MiuixIcons.Home),
    DefenseCard("防御卡", MiuixIcons.Notes),
    DataCenter("数据中心", MiuixIcons.GridView),
    Tools("实用工具", MiuixIcons.Tune),
    Settings("设置", MiuixIcons.Settings),
}
