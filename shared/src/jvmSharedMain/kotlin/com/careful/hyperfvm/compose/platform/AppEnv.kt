package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File

/**
 * 键值存储。
 *
 * Android 侧是 SharedPreferences，桌面侧是 Properties 文件；两边的语义差别只有「何时落盘」，
 * 统一用 [commit] 表达。
 */
interface KeyValueStore {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String?)
    fun remove(key: String)
    fun contains(key: String): Boolean

    /** 批量写入后统一落盘（Android 的 apply / 桌面的写文件）。 */
    fun commit()
}

/**
 * 平台能力开关。
 *
 * 界面上凡是「只有某个平台才有意义」的入口，都先问这里，而不是到处散落 `isAndroid` 判断。
 */
data class PlatformCapabilities(
    /** 「添加到日历」：桌面端没有等价系统能力，置 false 后入口不显示。 */
    val addToCalendar: Boolean = true,
    /** 悬浮窗/通知等系统权限页：只有 Android 有。 */
    val systemPermissions: Boolean = true,
    /** 保存到系统相册：只有 Android 有。 */
    val saveToGallery: Boolean = true,
    /** 保存图片时弹系统「另存为」对话框：桌面有，Android 走相册。 */
    val saveAsFile: Boolean = false,
    /** 提示是否走 miuix Snackbar（桌面）而非系统 Toast（Android）。 */
    val snackbarHints: Boolean = false,
    /** 手势返回（边缘滑动、返回键）是否可用。 */
    val systemBack: Boolean = true,
)

/**
 * 平台环境。
 *
 * 所有原本散在各处的 `Context` 用法收敛到这里：目录、键值存储、打开链接、剪贴板、提示、日志。
 * 界面层通过 [LocalAppEnv] 取用，数据层通过参数传入。
 */
interface AppEnv {
    val filesDir: File
    val cacheDir: File
    val capabilities: PlatformCapabilities

    /** 启动日志与「关于」页展示用的平台标识。 */
    val platformName: String

    /** 「关于」页展示的版本名（Android 取 PackageInfo，桌面会在打包时注入）。 */
    val appVersionName: String

    /** 「关于」页展示的版本号。 */
    val appVersionCode: Long

    fun prefs(name: String): KeyValueStore

    /** 用系统默认程序打开链接，失败返回 false。 */
    fun openUrl(url: String): Boolean

    fun copyToClipboard(text: String): Boolean

    /** 系统级短提示（Android Toast）。桌面端提示请走 [LocalToaster]。 */
    fun showToast(message: String)

    fun log(tag: String, message: String)

    /**
     * 把一场活动预填进系统日历，由用户在日历里确认保存（不需要日历权限）。
     *
     * 桌面端没有等价能力（[PlatformCapabilities.addToCalendar] 为 false，界面不给入口），
     * 默认返回 false。
     */
    fun addToCalendar(title: String, description: String, beginMillis: Long, endMillis: Long): Boolean = false

    /**
     * 把一张图片保存进系统图库（Android 落 `Pictures/HyperFVM`，桌面落「图片\HyperFVM」）。
     *
     * @return 是否保存成功
     */
    fun saveImageToGallery(file: File, mimeType: String): Boolean = false

    /**
     * 把一张图片交给系统的分享入口。
     *
     * Android 拉起分享面板；桌面端没有等价能力，降级为在资源管理器里定位该文件。
     */
    fun shareImage(file: File, mimeType: String): Boolean = false

    /**
     * 弹出系统「另存为」对话框，把图片写到用户选定的位置（桌面端）。
     *
     * 只有 [PlatformCapabilities.saveAsFile] 为 true 的平台会走到这里；
     * 取消与失败要分开，界面才能决定要不要提示。
     */
    fun saveImageAs(file: File, mimeType: String): SaveResult = SaveResult.Failed

    /**
     * 撤下后台任务留在通知栏的进度/结果通知。
     *
     * 桌面端没有通知栏，默认空实现；Android 由前台服务发出，需要页面主动收起。
     */
    fun dismissBackgroundNotification() {}

    /** 跟随系统的深色模式；固定浅/深色时由调用方优先处理，不会走到这里。 */
    fun isSystemDark(): Boolean
}

val LocalAppEnv = staticCompositionLocalOf<AppEnv> {
    error("No AppEnv provided!")
}

/** [AppEnv.saveImageAs] 的结果：取消不该弹「保存失败」。 */
enum class SaveResult {
    Saved,
    Cancelled,
    Failed,
}

/**
 * 统一提示入口。
 *
 * 桌面端实现为 miuix Snackbar，Android 端实现为 Toast，界面代码只调 [show]。
 */
fun interface Toaster {
    fun show(message: String)
}

val LocalToaster = staticCompositionLocalOf<Toaster> {
    Toaster { }
}
