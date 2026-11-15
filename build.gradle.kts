// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 所有插件都在根工程声明版本：子工程重复声明会让 Gradle 报「已在 classpath 且版本未知」。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}