// File: iStick/composeApp/src/iosMain/kotlin/istick/app/beta/FirebaseInitializer.kt
package istick.app.beta

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    actual fun initialize() {
        // Initialize Firebase for iOS
        Firebase.initialize()
    }
}