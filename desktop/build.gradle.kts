import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

/**
 * 查黑接口的 Authorization KEY 与 Android 侧同一来源（本机 local.properties，不入库）；
 * 开源仓库里没有该字段，拼出来就是空串，请求自动退回限流接口。
 */
val fraudAuthKey = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}.getProperty("fraud.authKey").orEmpty()

/**
 * 双端版本号的唯一来源是根目录 gradle.properties 的 app.versionName / app.versionCode：
 * Android 的 versionName、桌面端的安装包版本和两端的「关于」页显示都读这一组值。
 */
val desktopAppVersion = providers.gradleProperty("app.versionName").get()
val desktopVersionCode = providers.gradleProperty("app.versionCode").get()

/**
 * Windows 桌面入口。只负责「窗口 / 托盘 / 生命周期」，界面与数据全部来自 :shared。
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    // Dispatchers.Main 的桌面实现：缺了在首次 withContext(Dispatchers.Main) 时就抛异常。
    implementation(libs.kotlinx.coroutines.swing)
    // 托盘右键菜单用 Miuix 的 Surface/Text/主题色画（:shared 用的是 implementation 依赖，不对外暴露）。
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.glass)
    implementation(libs.miuix.material)
    // 托盘菜单的全局鼠标钩子：JDK 没有跨进程的鼠标事件，只能自己调 user32.dll。
    implementation(libs.jna)
}

compose.desktop {
    application {
        mainClass = "com.careful.hyperfvm.compose.desktop.MainKt"
        // 通过系统属性注入：「关于」页的版本号与查黑 KEY 都从系统属性读取。
        jvmArgs += listOf(
            "-Dhyperfvm.fraudAuthKey=$fraudAuthKey",
            "-Dhyperfvm.versionName=$desktopAppVersion",
            "-Dhyperfvm.versionCode=$desktopVersionCode",
        )
        // jpackage 只存在于完整 JDK：指向 Gradle 自动供给的 Zulu JDK 21。
        javaHome = "D:/Warehouse/Android/AndroidStudio/.gradle/jdks/azul_systems__inc_-21-amd64-windows.2"
        nativeDistributions {
            // 安装包里也要带上同一组属性：KEY 缺了只能走限流接口，版本号缺了「关于」页显示 dev。
            jvmArgs += listOf(
                "-Dhyperfvm.fraudAuthKey=$fraudAuthKey",
                "-Dhyperfvm.versionName=$desktopAppVersion",
                "-Dhyperfvm.versionCode=$desktopVersionCode",
            )
            // 不声明 targetFormats：Windows 安装包改由 Inno Setup 出（见下文 packageInnoSetup），
            // jpackage 只负责 createDistributable 那个 app-image，packageMsi 任务因此不再注册。
            packageName = "HyperFVM"
            packageVersion = desktopAppVersion
            description = "HyperFVM Desktop"
            vendor = "HyperFVM"
            windows {
                // 由 tool/make_desktop_icon.ps1 从 Android 自适应图标生成，安装包也复用这一张。
                iconFile.set(project.file("installer/HyperFVM.ico"))
                menuGroup = "HyperFVM"
                shortcut = true
                // 托盘常驻的应用不能一关窗口就退进程，但安装包仍按 GUI 程序发布（不带控制台）。
                console = false
            }
        }
    }
}

/**
 * Inno Setup 编译入口（Windows 包的唯一入口）。
 *
 * jpackage 出的 MSI 用的是 Windows Installer 自带的老式对话框：界面停在 Win95 风格、
 * 没有「创建桌面快捷方式」选项、装完不能直接启动、卸载也没有向导，而且同版本号无法
 * 覆盖安装。这里改用 Inno Setup 重新打一个 EXE 安装包，一并解决（脚本见 desktop/installer）。
 *
 * 需要先装 Inno Setup 6（https://jrsoftware.org/isdl.php）。ISCC.exe 的查找顺序：
 * local.properties 里的 innosetup.path → Inno 的两个默认安装目录。
 */
val innoSetupPath: String? = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}.getProperty("innosetup.path")

fun locateIscc(): String {
    innoSetupPath?.takeIf { it.isNotBlank() }?.let { configured ->
        val file = File(configured)
        if (file.isFile) return file.absolutePath
        throw GradleException("local.properties 里的 innosetup.path 指向的文件不存在：$configured")
    }
    // winget 装的是「仅当前用户」版，落在 LOCALAPPDATA\Programs 下；官网下载器默认装 Program Files。
    val candidates = buildList {
        System.getenv("LOCALAPPDATA")?.let { add("$it/Programs/Inno Setup 6/ISCC.exe") }
        add("C:/Program Files (x86)/Inno Setup 6/ISCC.exe")
        add("C:/Program Files/Inno Setup 6/ISCC.exe")
    }
    return candidates.firstOrNull { File(it).isFile } ?: throw GradleException(
        "没找到 Inno Setup 6 的编译器 ISCC.exe。请先装 Inno Setup 6（https://jrsoftware.org/isdl.php）；" +
            "如果装在别处，在 local.properties 里加一行 innosetup.path=...</ISCC.exe 的完整路径>",
    )
}

tasks.register<Exec>("packageInnoSetup") {
    group = "compose desktop"
    description = "用 Inno Setup 生成带向导的 Windows 安装包（内部先跑 createDistributable）"
    dependsOn("createDistributable")
    workingDir = layout.projectDirectory.dir("installer").asFile
    args("HyperFVM.iss", "/DAppVersion=$desktopAppVersion")
    doFirst { executable = locateIscc() }
}

// 不声明 targetFormats 后，jpackage 剩下那几个打包任务已经无格式可打，
// 逐个关掉：Gradle 面板里只留 createDistributable 和 packageInnoSetup，避免误点。
tasks.matching {
    it.name in setOf(
        "package",
        "packageDistributionForCurrentOS",
        "packageReleaseDistributionForCurrentOS",
        "packageUberJarForCurrentOS",
        "packageReleaseUberJarForCurrentOS",
    )
}.configureEach { enabled = false }
