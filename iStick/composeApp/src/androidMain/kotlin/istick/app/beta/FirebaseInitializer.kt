// In androidMain/kotlin/istick.app.beta/FirebaseInitializer.kt
package istick.app.beta

import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    // Firebase is already initialized in MyApplication class
    actual fun initialize() {
        // No need to initialize again as it's done in MyApplication.onCreate
        println("Firebase already initialized in MyApplication")
    }
}