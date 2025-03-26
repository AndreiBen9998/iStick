// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/MyApplication.kt
package istick.app.beta

import android.app.Application
import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.repository.RepositoryFactory

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize app components
            AppInitializer.initialize(this)

            // Set data source to MySQL
            RepositoryFactory.currentDataSource = RepositoryFactory.DataSource.MYSQL

            Log.i(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application: ${e.message}", e)

            // Fallback to MOCK implementation if database connection fails
            RepositoryFactory.currentDataSource = RepositoryFactory.DataSource.MOCK
            Log.w(TAG, "Falling back to mock data implementation")
        }
    }

    override fun onTerminate() {
        // Clean up resources
        AppInitializer.cleanup()
        super.onTerminate()
    }
}