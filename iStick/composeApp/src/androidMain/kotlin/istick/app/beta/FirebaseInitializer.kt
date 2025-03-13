// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/FirebaseInitializer.kt
package istick.app.beta

import android.content.Context
import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    private var initialized = false
    private var initializationAttempted = false

    actual fun initialize() {
        if (!initialized && !initializationAttempted) {
            initializationAttempted = true
            try {
                // For Android, check if Firebase is already initialized in MyApplication
                if (FirebaseApp.getInstance() != null) {
                    initialized = true
                    println("Firebase already initialized")
                }
            } catch (e: Exception) {
                println("Error checking Firebase initialization: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    actual fun isInitialized(): Boolean {
        return initialized || try {
            FirebaseApp.getInstance() != null
        } catch (e: Exception) {
            false
        }
    }

    // Helper for Android to initialize with context
    fun initializeWithContext(context: Context) {
        if (!initialized && !initializationAttempted) {
            initializationAttempted = true
            try {
                // First initialize Firebase Android SDK if not already initialized
                if (FirebaseApp.getInstance() == null) {
                    FirebaseApp.initializeApp(context)
                }

                // Then initialize GitLive Firebase wrapper
                Firebase.initialize(context = context)

                initialized = true
                println("Firebase initialized successfully with context")
            } catch (e: Exception) {
                println("Error initializing Firebase with context: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}