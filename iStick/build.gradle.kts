// iStick/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    // Update this line to use the compatible version with Kotlin 1.9.23
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// Add these repositories to ensure proper dependency resolution
allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.20")
        }
    }
}