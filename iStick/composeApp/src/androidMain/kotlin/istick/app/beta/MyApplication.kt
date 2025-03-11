// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/MyApplication.kt
package istick.app.beta

import android.app.Application
import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase for Android
        try {
            // First initialize Firebase Android SDK
            FirebaseApp.initializeApp(this)

            // Then initialize GitLive Firebase wrapper
            Firebase.initialize(context = this)

            println("Firebase initialized successfully in MyApplication")
        } catch (e: Exception) {
            println("Error initializing Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
}