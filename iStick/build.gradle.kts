// iStick/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    // Use EXACTLY 1.9.20-1.0.13 since that's what's already on your classpath
    id("com.google.devtools.ksp") version "1.9.20-1.0.13" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}