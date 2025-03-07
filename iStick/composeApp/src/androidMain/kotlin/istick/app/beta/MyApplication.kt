package istick.app.beta

import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase here
        Firebase.initialize(this)
    }
}