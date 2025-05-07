package istick.app.beta

import android.content.Context
import android.util.Log
import istick.app.beta.database.AppDatabase
import istick.app.beta.di.DependencyInjection
import istick.app.beta.network.createNetworkMonitor
import istick.app.beta.ocr.createOcrProcessor
import istick.app.beta.storage.RoomStorageRepository
import istick.app.beta.util.CoroutineConfig
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.analytics.createAnalyticsManager
import kotlinx.coroutines.*

object AppInitializer {
    private val TAG = "AppInitializer"
    private var initializationJob: Job? = null
    private var isInitialized = false
    private var hasCrashed = false
    private var onInitializationComplete: (() -> Unit)? = null

    fun initialize(context: Context, onComplete: () -> Unit = {}) {
        onInitializationComplete = onComplete

        // Start performance monitoring
        val performanceMonitor = PerformanceMonitor(context)
        performanceMonitor.startTrace("app_initialization")

        try {
            // Set up platform-specific dependencies immediately
            DependencyInjection.setPlatformContext(context)

            // Initialize database
            val database = AppDatabase.getDatabase(context)

            // Initialize network monitor with context
            val networkMonitor = createNetworkMonitor(context)

            // Initialize OCR with proper context
            val ocrProcessor = createOcrProcessor(context)

            // Initialize storage repository with context and database
            val storageRepository = RoomStorageRepository(context, database)

            // Initialize analytics
            val analyticsManager = createAnalyticsManager(context)

            // Initialize DI with all dependencies
            DependencyInjection.initPlatformDependencies(
                context = context,
                database = database,
                networkMonitor = networkMonitor,
                analyticsManager = analyticsManager,
                ocrProcessor = ocrProcessor,
                performanceMonitor = performanceMonitor,
                storageRepository = storageRepository
            )

            // Start network monitoring
            networkMonitor.startMonitoring()

            // Initialize repositories
            DependencyInjection.initRepositories()

            // Mark as initialized
            isInitialized = true
            performanceMonitor.recordMetric("app_initialized", 1)

            // Call completion callback
            onInitializationComplete?.invoke()

            performanceMonitor.stopTrace("app_initialization")
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

            // Clean up other resources
            DependencyInjection.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during app cleanup", e)
        }
    }
}