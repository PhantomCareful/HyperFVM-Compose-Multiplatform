import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * 双端共用的 UI + 数据层。
 *
 * 源集划分：
 * - jvmSharedMain：**绝大部分代码放这里**。Android 与桌面都是 JVM，所以可以直接用 java.io.File、
 *   java.util.concurrent 等 JDK 类型，不必为了 KMP 把文件 API 全部抽象一遍。
 * - androidMain / desktopMain：只放真正的平台实现（Context vs java.awt），由 expect/actual
 *   或接口 + 启动时注入来打通。
 * - commonMain：暂时留空（两端都是 JVM，不需要再往上抽一层）。
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    // AGP 9.2 起 KMP 的 android 目标用 android { }（旧名 androidLibrary 已废弃）。
    // namespace 必须与 :app 不同（同名会在 manifest 合并时直接被判非法）：
    // 它只影响本库生成的 R/BuildConfig 与合并后的 manifest 包名，Kotlin 源码包名不受影响。
    android {
        namespace = "com.careful.hyperfvm.compose.shared"
        compileSdk = 37
        minSdk = 31

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        // 自定义 JVM 中间源集：androidMain 与 desktopMain 都依赖它。
        val jvmSharedMain by creating {
            dependsOn(commonMain.get())
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.miuix.ui)
            implementation(libs.miuix.icons)
            implementation(libs.miuix.preference)
            implementation(libs.miuix.blur)
            implementation(libs.miuix.squircle)
            implementation(libs.miuix.nav)

            implementation(libs.telephoto.subsampling)
            implementation(libs.telephoto.zoomable)

            implementation(libs.reorderable)
            // LruCache 的双端实现（android.util.LruCache 只在 Android 上有）。
            implementation(libs.androidx.collection)
            // miuix-nav 内部依赖它，公共代码也会直接用到 NavigationEventInfo；
            // 自 1.1 起官方也发布了 desktop 变体，两端同一坐标。
            implementation("androidx.navigationevent:navigationevent-compose:1.1.2")
        }

        androidMain.get().dependsOn(jvmSharedMain)
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            // 申请通知权限要用 rememberLauncherForActivityResult。
            implementation(libs.androidx.activity.compose)
        }

        val desktopMain by getting {
            dependsOn(jvmSharedMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                // 桌面侧没有 android.icu，用 ICU4J 补上同一个 Transliterator。
                implementation(libs.icu4j)
                // org.json 在 Android 上是系统自带的，桌面需要从 Maven 补。
                implementation(libs.org.json)
                // Dispatchers.Main 在桌面端要靠 Swing 调度器实现：共享层里有
                // withContext(Dispatchers.Main.immediate) 回主线程发布状态，缺了会直接抛
                // 「Module with the Main dispatcher had failed to initialize」。
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

/**
 * 资源全部收在 shared 里，Android 与桌面共用同一套 drawable。
 * 生成的 `Res` 类放在包根下，公共代码直接 `Res.drawable.xxx`。
 */
compose.resources {
    publicResClass = true
    packageOfResClass = "com.careful.hyperfvm.compose"
    generateResClass =
        org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
}
