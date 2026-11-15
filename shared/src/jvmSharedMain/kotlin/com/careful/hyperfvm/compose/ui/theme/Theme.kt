package com.careful.hyperfvm.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/** 当前配色模式的下标，0..5 分别对应 System / Light / Dark / MonetSystem / MonetLight / MonetDark。 */
val LocalColorMode = compositionLocalOf { COLOR_MODE_SYSTEM }

const val COLOR_MODE_SYSTEM = 0
const val COLOR_MODE_LIGHT = 1
const val COLOR_MODE_DARK = 2
const val COLOR_MODE_MONET_SYSTEM = 3
const val COLOR_MODE_MONET_LIGHT = 4
const val COLOR_MODE_MONET_DARK = 5

/** 主题色的可选值。下标 0 没有颜色，代表交给平台动态取色。 */
val KeyColors: List<Pair<String, Color>> = listOf(
    "蓝" to Color(0xFF3482FF),
    "绿" to Color(0xFF36D167),
    "紫" to Color(0xFF7C4DFF),
    "黄" to Color(0xFFFFB21D),
    "橙" to Color(0xFFFF5722),
    "粉" to Color(0xFFE91E63),
    "青" to Color(0xFF00BCD4),
)

/** 设置页「主题色」下拉的选项文案，下标 0 表示不使用种子色。 */
val KeyColorNames: List<String> = listOf("默认") + KeyColors.map { it.first }

fun keyColorFor(index: Int): Color? = if (index <= 0) null else KeyColors.getOrNull(index - 1)?.second

/**
 * HyperFVM 主题。
 *
 * [colorMode] 为 0..2 时使用 miuix 自带的静态色板（主色 #3482FF），
 * 为 3..5 时用 [keyColor] 通过 MaterialKolor 动态生成；[keyColor] 为 null 则退回平台壁纸取色。
 */
@Composable
fun HyperFVMTheme(
    colorMode: Int = COLOR_MODE_SYSTEM,
    keyColor: Color? = null,
    paletteStyle: Int = 0,
    colorSpec: Int = 0,
    content: @Composable () -> Unit,
) {
    val spec = ThemeColorSpec.entries.getOrNull(colorSpec) ?: ThemeColorSpec.Spec2021
    val style = ThemePaletteStyle.entries.getOrNull(paletteStyle) ?: ThemePaletteStyle.Content
    val controller = remember(colorMode, keyColor, spec, style) {
        when (colorMode) {
            COLOR_MODE_LIGHT -> ThemeController(ColorSchemeMode.Light)
            COLOR_MODE_DARK -> ThemeController(ColorSchemeMode.Dark)
            COLOR_MODE_MONET_SYSTEM ->
                ThemeController(ColorSchemeMode.MonetSystem, keyColor = keyColor, colorSpec = spec, paletteStyle = style)
            COLOR_MODE_MONET_LIGHT ->
                ThemeController(ColorSchemeMode.MonetLight, keyColor = keyColor, colorSpec = spec, paletteStyle = style)
            COLOR_MODE_MONET_DARK ->
                ThemeController(ColorSchemeMode.MonetDark, keyColor = keyColor, colorSpec = spec, paletteStyle = style)
            else -> ThemeController(ColorSchemeMode.System)
        }
    }
    CompositionLocalProvider(LocalColorMode provides colorMode) {
        MiuixTheme(controller = controller, content = content)
    }
}

/** 按配色模式（而不是只看系统）判断当前是否为深色，用于系统栏图标与玻璃描边。 */
@Composable
fun isInDarkTheme(): Boolean = when (LocalColorMode.current) {
    COLOR_MODE_LIGHT, COLOR_MODE_MONET_LIGHT -> false
    COLOR_MODE_DARK, COLOR_MODE_MONET_DARK -> true
    else -> isSystemInDarkTheme()
}
