plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// Add this to force consistent serialization versions
allprojects {
    configurations.all {
        resolutionStrategy {
            // Force consistent Kotlin version
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.22")

            // Force specific kotlinx.serialization versions
            force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
            force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.6.2")
            force("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.2")
        }
    }
}