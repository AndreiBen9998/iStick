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
import kotlinx.coroutines.*

object AppInitializer {
    private val TAG = "AppInitializer"
    private var initializationJob: Job? = null

    // Flag to track if initialization has completed
    private var isInitialized = false

    // Flag to track any crash during initialization
    private var hasCrashed = false

    // Callback for completion
    private var onInitializationComplete: (() -> Unit)? = null

    fun initialize(context: Context, onComplete: () -> Unit = {}) {
        // Store callback
        onInitializationComplete = onComplete

        // Start performance monitoring
        val performanceMonitor = PerformanceMonitor(context)
        performanceMonitor.startTrace("app_initialization")

        try {
            // Set up platform-specific dependencies immediately
            DependencyInjection.setPlatformContext(context)

            val networkMonitor = createNetworkMonitor()
            val analyticsManager = createAnalyticsManager()
            val ocrProcessor = createOcrProcessor()
            val storageRepository = MySqlStorageRepository(context)

            // Initialize DI with dependencies that don't require database
            DependencyInjection.initPlatformDependencies(
                context = context,
                networkMonitor = networkMonitor,
                analyticsManager = analyticsManager,
                ocrProcessor = ocrProcessor,
                performanceMonitor = performanceMonitor,
                storageRepository = storageRepository
            )

            // Start network monitoring
            networkMonitor.startMonitoring()

            // Initialize database in background
            initializationJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Test database connection (with timeout)
                    var dbConnected = false
                    withTimeoutOrNull(5000) { // 5 second timeout
                        dbConnected = DatabaseHelper.testConnectionAsync()
                        true
                    } ?: run {
                        // Timeout reached
                        Log.w(TAG, "Database connection timeout, continuing with fallback")
                    }

                    // Initialize repositories
                    DependencyInjection.initRepositories()

                    // Mark as initialized
                    withContext(Dispatchers.Main) {
                        isInitialized = true
                        performanceMonitor.recordMetric("app_initialized", 1)
                        performanceMonitor.recordMetric("db_connected", if (dbConnected) 1 else 0)

                        // Call completion callback
                        onInitializationComplete?.invoke()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        hasCrashed = true
                        performanceMonitor.recordMetric("app_initialization_failed", 1)
                        Log.e(TAG, "Error during initialization", e)

                        // We still mark as initialized so the app doesn't get stuck
                        isInitialized = true

                        // Call completion callback even on error
                        onInitializationComplete?.invoke()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        performanceMonitor.stopTrace("app_initialization")
                    }
                }
            }
        } catch (e: Exception) {
            hasCrashed = true
            performanceMonitor.recordMetric("app_initialization_failed", 1)
            Log.e(TAG, "Error during synchronous initialization", e)

            // Mark as initialized to avoid blocking the app
            isInitialized = true

            // Call completion callback even on error
            onInitializationComplete?.invoke()

            performanceMonitor.stopTrace("app_initialization")
        }
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun hasCrashed(): Boolean {
        return hasCrashed
    }

    fun cleanup() {
        try {
            // Cancel any pending initialization job
            initializationJob?.cancel()

            // Stop network monitoring
            try {
                DependencyInjection.getNetworkMonitor().stopMonitoring()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping network monitor", e)
            }

            // Close database connections
            try {
                DatabaseHelper.closeAllConnections()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing database connections", e)
            }

            // Clean up other resources
            DependencyInjection.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during app cleanup", e)
        }
    }
}