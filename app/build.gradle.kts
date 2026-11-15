// android.newDsl=false（见 gradle.properties）：miuix 0.9.4 需要 Kotlin 2.4，而 AGP 内置 Kotlin 仍为 2.2.10，
// 外部 KGP 只能搭配旧 DSL，所以 android { } 的 BaseAppModuleExtension 入口被标记为 deprecated。
// 这是有意的临时停用，等 AGP 内置 Kotlin >= 2.4 迁移到新 DSL 后连同本行一起删除。
@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 查黑系统接口的 Authorization KEY 不进仓库：从本机 local.properties 读取（该文件已被 .gitignore 排除），
// 开源仓库里没有这个字段，构建出来的包也就不带 KEY，只是会受接口限流。
val fraudAuthKey = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}.getProperty("fraud.authKey").orEmpty()

// 版本号唯一来源在根目录的 gradle.properties，桌面端的安装包版本读的是同一组值。
val appVersionName = providers.gradleProperty("app.versionName").get()
val appVersionCode = providers.gradleProperty("app.versionCode").get().toInt()

android {
    namespace = "com.careful.hyperfvm.compose"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.careful.hyperfvm.compose"
        minSdk = 31
        targetSdk = 37
        // 日期式版本号：YYYYMMDD + 当日构建序号，实际值见根目录 gradle.properties。
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 通过 BuildConfig 注入，R8 会把常量直接内联到调用点。
        buildConfigField("String", "FRAUD_AUTH_KEY", "\"$fraudAuthKey\"")
    }

    buildTypes {
        release {
            // 资源缩减只作用在 Android 的 res/ 上：res/drawable 是指向
            // shared/src/commonMain/composeResources/drawable 的目录联接（junction），
            // 只为让 Android 视图和 Resource Manager 能浏览图片，代码里没有任何 R 引用，
            // 所以 release 会被收缩器全部裁掉，不会把这 5 MB 重复打进安装包。
            // Compose 资源真正生效靠 assets（见文件末尾的复制任务）。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        // 日期式版本号按天递增，距离 Google Play 上限 2100000000 还有约 7390 万空间（可用至 2099 年底）
        disable += "HighAppVersionCode"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        // demo 同样排除了这两个只在 Compose 内部用的 native 库；本应用不用图形路径 API。
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

/**
 * 把共享层的 Compose 资源铺成 Android 的 assets。
 *
 * Compose Multiplatform 在 Android 上不用 R 资源，而是把
 * composeResources/<包名>/drawable|files/... 当普通文件读（见 components-resources 的
 * DefaultAndroidResourceReader：assets 读不到就 ClassLoader 读，再读不到就抛异常），
 * 所以文件必须原样躺在 assets 里，路径里还要带 Res 类所在包名。
 *
 * 本来这活是 Compose 插件干的（copy<Variant>ComposeResourcesToAndroidAssets），
 * 但 1.12.0 在 AGP 9.2 的 KMP Library 上没接上：它注册的 copy 任务 outputDirectory
 * 没有值、也不在构建图里，结果 resources 一张都没进包，运行时启动即崩
 * 「Missing resource with path: composeResources/...」。这里在 app 侧自己补上。
 */
abstract class CopyComposeResourcesToAssetsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourcesDir: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun action() {
        fileSystem.copy {
            includeEmptyDirs = false
            from(resourcesDir)
            // 前缀就是 shared 里 packageOfResClass 的值，运行时按它拼路径。
            into(outputDirectory.dir("composeResources/com.careful.hyperfvm.compose"))
        }
    }
}

val copyComposeResourcesToAndroidAssets = tasks.register<CopyComposeResourcesToAssetsTask>(
    "copyComposeResourcesToAndroidAssets"
) {
    resourcesDir.from(rootProject.layout.projectDirectory.dir("shared/src/commonMain/composeResources"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            copyComposeResourcesToAndroidAssets,
            CopyComposeResourcesToAssetsTask::outputDirectory
        )
    }
}

dependencies {
    // 界面与数据层全部在 :shared（KMP 模块）：app 只剩 Application / Activity / Service 三个壳。
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // miuix (Compose Multiplatform UI library)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.squircle)
    implementation(libs.miuix.nav)

    // Compose 原生大图查看：区域解码 + 手势缩放，数据图查看器直接使用。
    implementation(libs.telephoto.subsampling)
    implementation(libs.telephoto.zoomable)

    // LazyColumn 长按拖动排序，仪表盘卡片排序用。
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}