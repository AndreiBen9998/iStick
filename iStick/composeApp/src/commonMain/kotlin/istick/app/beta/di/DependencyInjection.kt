// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/di/DependencyInjection.kt
package istick.app.beta.di

import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.EnhancedFirebaseAuthRepository
import istick.app.beta.auth.FirebaseAuthRepository
import istick.app.beta.repository.*
import istick.app.beta.storage.FirebaseStorageRepository
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.network.NetworkMonitor
import istick.app.beta.analytics.AnalyticsManager
import istick.app.beta.analytics.createAnalyticsManager
import istick.app.beta.ocr.OcrProcessor
import istick.app.beta.ocr.createOcrProcessor
import istick.app.beta.ui.navigation.AppNavigator

/**
 * Service locator pattern for dependency injection
 * Note: In a production app, you might want to use a proper DI framework like Koin or Dagger
 */
object DependencyInjection {
    // Platform-specific dependencies (initialized in platform-specific code)
    private var platformContext: Any? = null
    private var networkMonitor: NetworkMonitor? = null
    private var analyticsManager: AnalyticsManager? = null
    private var ocrProcessor: OcrProcessor? = null
    private var performanceMonitor: PerformanceMonitor? = null
    
    // Core repositories
    private val authRepository: AuthRepository by lazy {
        EnhancedFirebaseAuthRepository()
    }
    
    private val userRepository: UserRepository by lazy {
        FirebaseUserRepository(authRepository)
    }
    
    private val carRepository: CarRepository by lazy {
        FirebaseCarRepository()
    }
    
    private val campaignRepository: CampaignRepository by lazy {
        FirebaseCampaignRepository(authRepository)
    }
    
    private val storageRepository: StorageRepository by lazy {
        FirebaseStorageRepository()
    }
    
    private val offlineRepositoryWrapper: OfflineRepositoryWrapper? by lazy {
        networkMonitor?.let { OfflineRepositoryWrapper(it) }
    }
    
    // Navigator
    private val appNavigator by lazy {
        AppNavigator(
            authRepository = authRepository,
            userRepository = userRepository,
            campaignRepository = campaignRepository,
            carRepository = carRepository,
            storageRepository = storageRepository,
            performanceMonitor = getPerformanceMonitor()
        )
    }
    
    // Initialize platform-specific dependencies
    fun initPlatformDependencies(
        context: Any,
        networkMonitor: NetworkMonitor,
        analyticsManager: AnalyticsManager,
        ocrProcessor: OcrProcessor,
        performanceMonitor: PerformanceMonitor
    ) {
        this.platformContext = context
        this.networkMonitor = networkMonitor
        this.analyticsManager = analyticsManager
        this.ocrProcessor = ocrProcessor
        this.performanceMonitor = performanceMonitor
        
        // Start network monitoring
        networkMonitor.startMonitoring()
    }
    
    // Getters for dependencies
    fun getAuthRepository(): AuthRepository = authRepository
    fun getUserRepository(): UserRepository = userRepository
    fun getCarRepository(): CarRepository = carRepository
    fun getCampaignRepository(): CampaignRepository = campaignRepository
    fun getStorageRepository(): StorageRepository = storageRepository
    fun getNetworkMonitor(): NetworkMonitor = networkMonitor ?: throw IllegalStateException("NetworkMonitor not initialized")
    fun getAnalyticsManager(): AnalyticsManager = analyticsManager ?: throw IllegalStateException("AnalyticsManager not initialized")
    fun getOcrProcessor(): OcrProcessor = ocrProcessor ?: throw IllegalStateException("OcrProcessor not initialized")
    fun getPerformanceMonitor(): PerformanceMonitor = performanceMonitor ?: throw IllegalStateException("PerformanceMonitor not initialized")
    fun getAppNavigator(): AppNavigator = appNavigator
    fun getOfflineRepositoryWrapper(): OfflineRepositoryWrapper = offlineRepositoryWrapper ?: throw IllegalStateException("NetworkMonitor not initialized")
    
    // Cleanup resources
    fun cleanup() {
        networkMonitor?.stopMonitoring()
    }
}