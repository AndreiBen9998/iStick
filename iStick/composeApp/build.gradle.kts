plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
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
            implementation("androidx.compose.ui:ui-graphics:1.6.0")
            implementation("androidx.compose.ui:ui-util:1.6.0")

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.material3)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation("androidx.compose.material:material-icons-extended:1.6.0")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Room Database
            implementation("androidx.room:room-runtime:2.6.1")
            implementation("androidx.room:room-ktx:2.6.1")

            // ML Kit for OCR
            implementation("com.google.mlkit:text-recognition:16.0.0")

            // GSON for type conversions
            implementation("com.google.code.gson:gson:2.10.1")
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
            excludes += "**/META-INF/*.kotlin_module"
        }
    }

    buildTypes {
        named("release") {
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
    debugImplementation(compose.uiTooling)
    implementation("androidx.activity:activity-compose:1.8.0")

    // Remove JDBC dependencies
    // implementation("mysql:mysql-connector-java:8.0.33")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
    }
}