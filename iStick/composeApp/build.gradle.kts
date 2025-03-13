// iStick/composeApp/build.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

kotlin {
    // Properly configure Android target
    androidTarget()

    // Configure Java version for Android
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    // Remove or comment out iOS targets
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "ComposeApp"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Updated Firebase dependencies to latest compatible version
            implementation("dev.gitlive:firebase-common:1.10.0")
            implementation("dev.gitlive:firebase-auth:1.10.0")
            implementation("dev.gitlive:firebase-storage:1.10.0")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Android-specific Firebase dependencies
            implementation("com.google.firebase:firebase-auth:22.3.1")
            implementation("com.google.firebase:firebase-storage:20.3.0")

            // Room dependencies
            implementation("androidx.room:room-runtime:2.6.1")
            implementation("androidx.room:room-ktx:2.6.1")
        }

        iosMain.dependencies {
            // No platform-specific Firebase dependencies required for iOS
        }
    }
}

// Configure KSP properly
dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")

    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.firebase.perf.ktx)
    debugImplementation(compose.uiTooling)
    implementation("androidx.activity:activity-compose:1.8.0")
}

android {
    namespace = "istick.app.beta"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "istick.app.beta"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Apply Google Services plugin after Android configuration
    apply(plugin = "com.google.gms.google-services")
}