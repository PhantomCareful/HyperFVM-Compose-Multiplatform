package com.careful.hyperfvm.compose.data.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.KeyValueStore
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_DARK
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_LIGHT
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_MONET_DARK
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_MONET_LIGHT

/**
 * 全量用户设置。
 *
 * 字段按设置页的分组排列；新增字段时必须同步补上 [SettingsStore] 里的读写映射，
 * 否则重启后会静默回到默认值。
 */
data class AppSettings(
    // 窗口（仅桌面端生效）
    /** 点窗口关闭按钮时的行为，取值见 [CloseAction]；Android 端不使用。 */
    val closeAction: Int = CloseAction.MinimizeToTray,
    // 外观
    val colorMode: Int = 0,
    val keyColorIndex: Int = 0,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 0,
    val enableBlur: Boolean = true,
    val blurStyle: Int = 0,
    /** 全局界面缩放：只缩放 dp 的密度，不影响系统的字体缩放。 */
    val uiScale: Float = 1f,
    // 交互
    val scrollEndHaptic: Boolean = true,
    val pageUserScroll: Boolean = true,
    /** 左右滑动的接管方式：0 = 默认，1 = 跨轴拦截，2 = iOS 风格。 */
    val pagerInterceptionMode: Int = 1,
    /** 页面转场样式：0 = Miuix 默认滑动，1 = AOSP 跨应用卡片。角标裁剪与变暗固定开启。 */
    val navTransitionStyle: Int = 0,
    // 底部导航
    val navigationBarMode: Int = 0,
    val useFloatingNavigationBar: Boolean = false,
    /** 选项见 [FloatingBarStyle]：0 默认、2 液态玻璃、4 液态玻璃（Q弹）；外观和名字都由全局「模糊」开关决定。 */
    val floatingNavigationBarStyle: Int = 0,
)

/**
 * 桌面端点窗口关闭按钮时的行为。
 *
 * 默认收回托盘：桌面端没有系统管家托住进程，误点关闭不该把后台任务一起丢掉；
 * 想要「点叉即退出」的用户可以在设置里切成 [Exit]。
 */
object CloseAction {
    const val MinimizeToTray = 0
    const val Exit = 1
}

/**
 * 悬浮底栏样式的持久化编号。
 *
 * 下拉框的项数随全局模糊开关变化，外观和名字一并跟着变：
 * - [Default]：模糊关 → 「默认」实色栏，模糊开 → 「高光描边」栏
 * - [LiquidGlass]：模糊关 → 「滑块」，模糊开 → 「液态玻璃」栏
 * - [BouncyGlass]：模糊关 → 同样退成「滑块」，模糊开 → 「液态玻璃（Q弹）」栏
 *
 * [Highlight] 与 [Slider] 曾是独立选项，编号保留下来兼容旧设置，
 * 新写入只会落 [Default]、[LiquidGlass] 或 [BouncyGlass]。
 */
object FloatingBarStyle {
    const val Default = 0
    const val Highlight = 1
    const val LiquidGlass = 2
    const val Slider = 3
    const val BouncyGlass = 4
}

/** 底栏是否走液态玻璃路线（含历史滑块编号与 Q弹）。 */
val AppSettings.isLiquidGlassBar: Boolean
    get() = floatingNavigationBarStyle >= FloatingBarStyle.LiquidGlass

/** 是否选的是「液态玻璃（Q弹）」；模糊关时会退成滑块，这个判断只在模糊开时有效。 */
val AppSettings.isBouncyGlassBar: Boolean
    get() = floatingNavigationBarStyle == FloatingBarStyle.BouncyGlass

val LocalAppSettings = staticCompositionLocalOf<AppSettings> {
    error("No AppSettings provided!")
}

val LocalUpdateSettings = staticCompositionLocalOf<((AppSettings) -> AppSettings) -> Unit> {
    error("No AppSettings updater provided!")
}

/**
 * 设置仓库。落盘走 [KeyValueStore]：Android 是 SharedPreferences，桌面是 Properties 文件。
 *
 * [settings] 是可观察的 Compose 状态，读它会自动订阅；写入只走 [update]，
 * 这样内存状态和磁盘状态不会分叉。
 */
class SettingsStore(env: AppEnv) {

    private val prefs: KeyValueStore = env.prefs(PREFS_NAME)

    var settings: AppSettings by mutableStateOf(read())
        private set

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings)
        if (next == settings) return
        settings = next
        write(next)
    }

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            closeAction = prefs.getInt(KEY_CLOSE_ACTION, d.closeAction),
            colorMode = prefs.getInt(KEY_COLOR_MODE, d.colorMode),
            keyColorIndex = prefs.getInt(KEY_KEY_COLOR, d.keyColorIndex),
            paletteStyle = prefs.getInt(KEY_PALETTE_STYLE, d.paletteStyle),
            colorSpec = prefs.getInt(KEY_COLOR_SPEC, d.colorSpec),
            enableBlur = prefs.getBoolean(KEY_BLUR, d.enableBlur),
            blurStyle = prefs.getInt(KEY_BLUR_STYLE, d.blurStyle),
            uiScale = prefs.getFloat(KEY_UI_SCALE, d.uiScale),
            scrollEndHaptic = prefs.getBoolean(KEY_SCROLL_HAPTIC, d.scrollEndHaptic),
            pageUserScroll = prefs.getBoolean(KEY_PAGE_SCROLL, d.pageUserScroll),
            pagerInterceptionMode = prefs.getInt(KEY_PAGER_MODE, d.pagerInterceptionMode),
            navTransitionStyle = prefs.getInt(KEY_NAV_TRANSITION, d.navTransitionStyle),
            navigationBarMode = prefs.getInt(KEY_NAV_MODE, d.navigationBarMode),
            useFloatingNavigationBar = prefs.getBoolean(KEY_NAV_FLOATING, d.useFloatingNavigationBar),
            floatingNavigationBarStyle = prefs.getInt(KEY_NAV_FLOATING_STYLE, d.floatingNavigationBarStyle),
        )
    }

    private fun write(s: AppSettings) {
        prefs.putInt(KEY_CLOSE_ACTION, s.closeAction)
        prefs.putInt(KEY_COLOR_MODE, s.colorMode)
        prefs.putInt(KEY_KEY_COLOR, s.keyColorIndex)
        prefs.putInt(KEY_PALETTE_STYLE, s.paletteStyle)
        prefs.putInt(KEY_COLOR_SPEC, s.colorSpec)
        prefs.putBoolean(KEY_BLUR, s.enableBlur)
        prefs.putInt(KEY_BLUR_STYLE, s.blurStyle)
        prefs.putFloat(KEY_UI_SCALE, s.uiScale)
        prefs.putBoolean(KEY_SCROLL_HAPTIC, s.scrollEndHaptic)
        prefs.putBoolean(KEY_PAGE_SCROLL, s.pageUserScroll)
        prefs.putInt(KEY_PAGER_MODE, s.pagerInterceptionMode)
        prefs.putInt(KEY_NAV_TRANSITION, s.navTransitionStyle)
        prefs.putInt(KEY_NAV_MODE, s.navigationBarMode)
        prefs.putBoolean(KEY_NAV_FLOATING, s.useFloatingNavigationBar)
        prefs.putInt(KEY_NAV_FLOATING_STYLE, s.floatingNavigationBarStyle)
        prefs.commit()
    }

    companion object {
        private const val PREFS_NAME = "hyperfvm_settings"

        private const val KEY_CLOSE_ACTION = "closeAction"
        private const val KEY_COLOR_MODE = "colorMode"
        private const val KEY_KEY_COLOR = "keyColorIndex"
        private const val KEY_PALETTE_STYLE = "paletteStyle"
        private const val KEY_COLOR_SPEC = "colorSpec"
        private const val KEY_BLUR = "enableBlur"
        private const val KEY_BLUR_STYLE = "blurStyle"
        private const val KEY_UI_SCALE = "uiScale"
        private const val KEY_SCROLL_HAPTIC = "scrollEndHaptic"
        private const val KEY_PAGE_SCROLL = "pageUserScroll"
        private const val KEY_PAGER_MODE = "pagerInterceptionMode"
        private const val KEY_NAV_TRANSITION = "navTransitionStyle"
        private const val KEY_NAV_MODE = "navigationBarMode"
        private const val KEY_NAV_FLOATING = "useFloatingNavigationBar"
        private const val KEY_NAV_FLOATING_STYLE = "floatingNavigationBarStyle"

        /**
         * 只读一次配色模式并换算成「是否深色」，供非 Compose 环境（如查看器的系统栏）使用。
         *
         * 与 `MainActivity` 里推导系统栏样式的逻辑一致：固定浅/深色直接返回，
         * 「跟随系统」时才去读系统的夜间模式。
         */
        fun isDarkMode(env: AppEnv): Boolean {
            val prefs = env.prefs(PREFS_NAME)
            return when (prefs.getInt(KEY_COLOR_MODE, AppSettings().colorMode)) {
                COLOR_MODE_LIGHT, COLOR_MODE_MONET_LIGHT -> false
                COLOR_MODE_DARK, COLOR_MODE_MONET_DARK -> true
                else -> env.isSystemDark()
            }
        }
    }
}
