package istick.app.beta

import android.app.Application
import android.util.Log

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize app components with application context
            AppInitializer.initialize(this)
            Log.i(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application: ${e.message}", e)

            // Set default fallback data source
            try {
                RepositoryFactory.currentDataSource = RepositoryFactory.DataSource.MOCK
            } catch (e: Exception) {
                Log.e(TAG, "Could not set fallback data source", e)
            }
        }
    }

    override fun onTerminate() {
        // Clean up resources
        AppInitializer.cleanup()
        super.onTerminate()
    }
}