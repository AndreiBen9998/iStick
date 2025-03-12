// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/FirebaseInitializer.kt
package istick.app.beta

import android.content.Context
import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    private var initialized = false
    private var initializationAttempted = false

    // Firebase is already initialized in MyApplication class
    actual fun initialize() {
        if (!initialized && !initializationAttempted) {
            initializationAttempted = true
            try {
                // The actual initialization happens in MyApplication.onCreate
                // This is just to note that initialization was successful
                if (FirebaseApp.getInstance() != null) {
                    initialized = true
                    println("Firebase confirmed as initialized")
                }
            } catch (e: Exception) {
                println("Error confirming Firebase initialization: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    actual fun isInitialized(): Boolean {
        return initialized
    }

    // Additional helper for Android to initialize with context if needed
    fun initializeWithContext(context: Context) {
        if (!initialized && !initializationAttempted) {
            initializationAttempted = true
            try {
                // First initialize Firebase Android SDK
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