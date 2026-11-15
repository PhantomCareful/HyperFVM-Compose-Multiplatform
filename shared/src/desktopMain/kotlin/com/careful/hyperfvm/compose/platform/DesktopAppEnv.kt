package com.careful.hyperfvm.compose.platform

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.util.Properties
import javax.swing.filechooser.FileSystemView

/**
 * 桌面侧的 [KeyValueStore]：一个 Properties 文件。
 *
 * 写入只改内存副本，[commit] 时才落盘——对应 Android SharedPreferences 的 apply/commit 语义，
 * 这样上层代码不必判断当前在哪个平台。
 */
class DesktopKeyValueStore(private val file: File) : KeyValueStore {

    private val props: Properties = Properties().also { p ->
        if (file.isFile) {
            runCatching { file.inputStream().use(p::load) }
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        props.getProperty(key)?.toIntOrNull() ?: defaultValue

    override fun putInt(key: String, value: Int) {
        props.setProperty(key, value.toString())
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        props.getProperty(key)?.toLongOrNull() ?: defaultValue

    override fun putLong(key: String, value: Long) {
        props.setProperty(key, value.toString())
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        props.getProperty(key)?.toFloatOrNull() ?: defaultValue

    override fun putFloat(key: String, value: Float) {
        props.setProperty(key, value.toString())
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        props.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        props.setProperty(key, value.toString())
    }

    override fun getString(key: String, defaultValue: String?): String? =
        props.getProperty(key) ?: defaultValue

    override fun putString(key: String, value: String?) {
        if (value == null) props.remove(key) else props.setProperty(key, value)
    }

    override fun remove(key: String) {
        props.remove(key)
    }

    override fun contains(key: String): Boolean = props.containsKey(key)

    override fun commit() {
        runCatching {
            file.parentFile?.mkdirs()
            file.outputStream().use { props.store(it, null) }
        }
    }
}

/**
 * Windows 侧的 [AppEnv]。
 *
 * 目录遵循 Windows 习惯：数据放 %APPDATA%，缓存放 %LOCALAPPDATA%。
 */
class DesktopAppEnv : AppEnv {

    private val appDataDir: File by lazy {
        val appData = System.getenv("APPDATA")
        if (!appData.isNullOrBlank()) File(appData, "HyperFVM")
        else File(System.getProperty("user.home"), ".hyperfvm")
    }

    override val filesDir: File get() = appDataDir

    override val cacheDir: File by lazy {
        val local = System.getenv("LOCALAPPDATA")
        if (!local.isNullOrBlank()) File(local, "HyperFVM\\cache")
        else File(appDataDir, "cache")
    }

    override val capabilities: PlatformCapabilities = PlatformCapabilities(
        addToCalendar = false,
        systemPermissions = false,
        saveToGallery = false,
        saveAsFile = true,
        snackbarHints = true,
        systemBack = false,
    )

    override val platformName: String =
        "Windows ${System.getProperty("os.version")} / JVM ${System.getProperty("java.version")}"

    // 桌面端没有 PackageManager：版本号由 Gradle 从根目录 gradle.properties 读出后经 -D 注入
    // （见 desktop/build.gradle.kts）。脱离 Gradle 直接跑（例如 IDE 里单跑 Main）才会落到 dev。
    override val appVersionName: String =
        System.getProperty("hyperfvm.versionName") ?: "dev"

    override val appVersionCode: Long =
        System.getProperty("hyperfvm.versionCode")?.toLongOrNull() ?: 0L

    override fun prefs(name: String): KeyValueStore =
        DesktopKeyValueStore(File(filesDir, "$name.properties"))

    override fun openUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri)
            true
        } else {
            // 没有桌面环境时退回到系统 shell。
            ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
            true
        }
    }.getOrDefault(false)

    override fun copyToClipboard(text: String): Boolean = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    }.getOrDefault(false)

    /** Windows 没有系统图库，落到用户的「图片\HyperFVM」目录，语义上等价于 Android 的相册。 */
    override fun saveImageToGallery(file: File, mimeType: String): Boolean = runCatching {
        val dir = File(File(System.getProperty("user.home"), "Pictures"), "HyperFVM").apply { mkdirs() }
        file.copyTo(File(dir, file.name), overwrite = true)
    }.isSuccess

    /**
     * 桌面端没有系统分享面板，降级为在资源管理器里选中这个文件，用户可以直接拖到聊天窗口。
     *
     * `explorer /select,` 会把请求转给已存在的 explorer 进程并立刻返回，退出码不可信，
     * 所以不检查 exitValue。
     */
    override fun shareImage(file: File, mimeType: String): Boolean = runCatching {
        if (!file.isFile) return false
        ProcessBuilder("explorer.exe", "/select,${file.absolutePath}").start()
        true
    }.getOrDefault(false)

    /**
     * 「另存为」：弹系统文件对话框，默认目录是系统的「文档」，文件名沿用图片自己那个。
     *
     * 调用方都在界面协程里（就是 AWT 事件线程），直接 show 就行：
     * 原生对话框自身会起一副事件循环，不会把窗口卡死。
     */
    override fun saveImageAs(file: File, mimeType: String): SaveResult {
        if (!file.isFile) return SaveResult.Failed
        val dialog = FileDialog(null as Frame?, "另存为", FileDialog.SAVE).apply {
            this.file = file.name
            directory = FileSystemView.getFileSystemView().defaultDirectory?.absolutePath
        }
        dialog.isVisible = true
        val name = dialog.file ?: return SaveResult.Cancelled
        return runCatching {
            // 用户把后缀删了就补上，否则存出来的文件双击打不开。
            val fileName = if (name.substringAfterLast('.', "").isEmpty()) "$name.${file.extension}" else name
            val target = File(dialog.directory ?: file.parentFile.absolutePath, fileName)
            if (target.absolutePath != file.absolutePath) file.copyTo(target, overwrite = true)
            SaveResult.Saved
        }.getOrDefault(SaveResult.Failed)
    }

    /** 桌面端没有系统 Toast；界面上真正可见的提示走 [LocalToaster]（miuix Snackbar）。 */
    override fun showToast(message: String) {
        println("[hint] $message")
    }

    override fun log(tag: String, message: String) {
        println("[$tag] $message")
    }

    override fun isSystemDark(): Boolean = systemDark

    private val systemDark: Boolean by lazy {
        // Windows 11 把「应用」明暗记在 HKCU 的这个键下，值 0 = 深色模式。
        runCatching {
            val process = ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme",
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("0x0")
        }.getOrDefault(false)
    }
}
