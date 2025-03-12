// File: iStick/composeApp/src/iosMain/kotlin/istick/app/beta/FirebaseInitializer.kt
package istick.app.beta

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    private var initialized = false
    private var initializationAttempted = false

    actual fun initialize() {
        if (!initialized && !initializationAttempted) {
            initializationAttempted = true
            try {
                // Initialize Firebase for iOS
                Firebase.initialize()
                initialized = true
                println("Firebase initialized for iOS")
            } catch (e: Exception) {
                println("Error initializing Firebase for iOS: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    actual fun isInitialized(): Boolean {
        return initialized
    }
}