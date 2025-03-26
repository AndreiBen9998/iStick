// Update iStick/composeApp/src/androidMain/kotlin/istick/app/beta/MyApplication.kt

package istick.app.beta

import android.app.Application
import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.repository.RepositoryFactory

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

        // Also inform the FirebaseInitializer
        FirebaseInitializer.initializeWithContext(this)

        // Set data source to MySQL for development testing
        // Comment this out to use the default Firebase/mock implementation
        // To use MySQL database, make sure the MySQL server is running and the database is imported
        // You might need to adjust the connection parameters in DatabaseHelper.kt
        RepositoryFactory.currentDataSource = RepositoryFactory.DataSource.MYSQL
    }

    override fun onTerminate() {
        super.onTerminate()

        // Clean up database connections when the application terminates
        if (RepositoryFactory.currentDataSource == RepositoryFactory.DataSource.MYSQL) {
            DatabaseHelper.closeAllConnections()
        }
    }
}