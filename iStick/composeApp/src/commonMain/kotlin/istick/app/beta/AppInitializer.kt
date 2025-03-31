// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/AppInitializer.kt
package istick.app.beta

import android.content.Context
import istick.app.beta.analytics.createAnalyticsManager
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.di.DependencyInjection
import istick.app.beta.network.createNetworkMonitor
import istick.app.beta.ocr.createOcrProcessor
import istick.app.beta.storage.MySqlStorageRepository  // Use direct import instead of alias
import istick.app.beta.utils.PerformanceMonitor

/**
 * Initialize the application with required components
 */
object AppInitializer {
    /**
     * Initialize all app components
     */
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

            // Initialize database connection
            performanceMonitor.startTrace("database_initialization")
            try {
                // Test database connection
                DatabaseHelper.testConnection()
                performanceMonitor.recordMetric("database_connected", 1)
            } catch (e: Exception) {
                performanceMonitor.recordMetric("database_connection_failed", 1)
                throw e
            } finally {
                performanceMonitor.stopTrace("database_initialization")
            }

            performanceMonitor.recordMetric("app_initialized", 1)
        } catch (e: Exception) {
            performanceMonitor.recordMetric("app_initialization_failed", 1)
            throw e
        } finally {
            performanceMonitor.stopTrace("app_initialization")
        }
    }

    /**
     * Clean up resources when the app is shutting down
     */
    fun cleanup() {
        try {
            // Close database connections
            DatabaseHelper.closeAllConnections()

            // Clean up other resources
            DependencyInjection.cleanup()
        } catch (e: Exception) {
            // Log but don't crash on cleanup errors
            println("Error during app cleanup: ${e.message}")
        }
    }
}