// File: iStick/composeApp/src/iosMain/kotlin/istick/app/beta/FirebaseInitializer.kt
package istick.app.beta

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    actual fun initialize() {
        try {
            // Initialize Firebase for iOS
            Firebase.initialize()
            println("Firebase initialized for iOS")
        } catch (e: Exception) {
            println("Error initializing Firebase for iOS: ${e.message}")
        }
    }
}