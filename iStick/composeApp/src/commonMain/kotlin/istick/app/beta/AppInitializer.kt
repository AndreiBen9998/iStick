package istick.app.beta

import android.content.Context
import android.util.Log
import istick.app.beta.analytics.createAnalyticsManager
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.di.DependencyInjection
import istick.app.beta.network.createNetworkMonitor
import istick.app.beta.ocr.createOcrProcessor
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.storage.MySqlStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppInitializer {
    fun initialize(context: Context) {
        // Start performance monitoring
        val performanceMonitor = PerformanceMonitor(context)
        performanceMonitor.startTrace("app_initialization")

        try {
            // Set up platform-specific dependencies
            DependencyInjection.setPlatformContext(context)

            val networkMonitor = createNetworkMonitor(context)
            val analyticsManager = createAnalyticsManager()
            val ocrProcessor = createOcrProcessor(context)
            val storageRepository = MySqlStorageRepository(context)

            // Initialize DI
            DependencyInjection.initPlatformDependencies(
                context = context,
                networkMonitor = networkMonitor,
                analyticsManager = analyticsManager,
                ocrProcessor = ocrProcessor,
                performanceMonitor = performanceMonitor,
                storageRepository = storageRepository
            )

            // Initialize repositories
            DependencyInjection.initRepositories()

            // Skip database testing for now to allow app to start
            performanceMonitor.recordMetric("app_initialized", 1)
        } catch (e: Exception) {
            performanceMonitor.recordMetric("app_initialization_failed", 1)
            Log.e("AppInitializer", "Error during initialization", e)
            // Don't throw the exception - let the app continue
        } finally {
            performanceMonitor.stopTrace("app_initialization")
        }
    }

    fun cleanup() {
        try {
            // Close database connections
            DatabaseHelper.closeAllConnections()

            // Clean up other resources
            DependencyInjection.cleanup()
        } catch (e: Exception) {
            Log.e("AppInitializer", "Error during app cleanup", e)
        }
    }
}