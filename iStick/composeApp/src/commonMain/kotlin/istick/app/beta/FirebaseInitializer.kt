// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/FirebaseInitializer.kt
package istick.app.beta

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

expect object FirebaseInitializer {
    fun initialize()

    // Add a way to check initialization status
    fun isInitialized(): Boolean
}

// Helper function to safely initialize Firebase
fun safeInitializeFirebase(onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
    try {
        // Only initialize if not already initialized
        if (!FirebaseInitializer.isInitialized()) {
            FirebaseInitializer.initialize()
        }
        onSuccess()
    } catch (e: Exception) {
        println("Error initializing Firebase: ${e.message}")
        onError(e)
    }
}

// Helper to initialize in a coroutine
fun initializeFirebaseAsync(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    scope.launch {
        safeInitializeFirebase(onSuccess, onError)
    }
}