plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.15" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// Update the force resolution to use Kotlin 2.0.0
allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.0")
        }
    }
}