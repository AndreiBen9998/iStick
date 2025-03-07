// In androidMain/kotlin/istick.app.beta/FirebaseInitializer.kt
package istick.app.beta

import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    // No-op implementation since Firebase is initialized in MyApplication
    actual fun initialize() {
        // Intentionally empty - Firebase is already initialized in MyApplication
    }
}