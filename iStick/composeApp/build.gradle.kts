import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

kotlin {
    // Explicitly define Android target
    androidTarget {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.runtime)
            implementation("androidx.compose.ui:ui-graphics:1.5.4") // Use your current version
            implementation("androidx.compose.ui:ui-util:1.5.4")


            // Use explicit material icons import
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.material3)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Firebase dependencies
            implementation("dev.gitlive:firebase-common:1.11.0")
            implementation("dev.gitlive:firebase-auth:1.11.0")
            implementation("dev.gitlive:firebase-storage:1.11.0")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Firebase dependencies
            implementation(platform("com.google.firebase:firebase-bom:32.4.0"))
            implementation("com.google.firebase:firebase-auth-ktx")
            implementation("com.google.firebase:firebase-storage-ktx")
            implementation("com.google.firebase:firebase-analytics-ktx")

            // Room dependencies
            implementation("androidx.room:room-runtime:2.6.1")
            implementation("androidx.room:room-ktx:2.6.1")
        }

        androidMain {
            kotlin.srcDir("build/generated/ksp/android/androidDebug/kotlin")
        }
    }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")

    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.firebase.perf.ktx)
    debugImplementation(compose.uiTooling)
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("dev.gitlive:firebase-auth:1.10.0")
    implementation("dev.gitlive:firebase-common:1.10.0")
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.animation)
    implementation(compose.components.resources)
}