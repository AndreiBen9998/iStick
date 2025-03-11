// File: iStick/settings.gradle.kts
rootProject.name = "iStick"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // Add specific repository for GitLive Firebase dependencies
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

include(":composeApp")