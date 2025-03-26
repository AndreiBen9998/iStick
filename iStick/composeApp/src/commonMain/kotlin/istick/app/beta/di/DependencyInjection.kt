// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/di/DependencyInjection.kt
package istick.app.beta.di

import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.DefaultAuthRepository
import istick.app.beta.repository.*
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.network.NetworkMonitor
import istick.app.beta.analytics.AnalyticsManager
import istick.app.beta.ocr.OcrProcessor
import istick.app.beta.ui.navigation.AppNavigator

/**
 * Service locator pattern for dependency injection
 * This version is MySQL-focused without Firebase dependencies
 */
object DependencyInjection {
    // Platform-specific dependencies
    @Volatile
    private var platformContext: Any? = null

    private var networkMonitor: NetworkMonitor? = null
    private var analyticsManager: AnalyticsManager? = null
    private var ocrProcessor: OcrProcessor? = null
    private var performanceMonitor: PerformanceMonitor? = null
    private var storageRepository: StorageRepository? = null

    // Core repositories
    private val authRepository: AuthRepository by lazy {
        DefaultAuthRepository()
    }

    private val userRepository: UserRepository by lazy {
        DefaultUserRepository(authRepository)
    }

    private val carRepository: CarRepository by lazy {
        DefaultCarRepository()
    }

    private val campaignRepository: CampaignRepository by lazy {
        DefaultCampaignRepository(authRepository)
    }

    private val offersRepository: OptimizedOffersRepository by lazy {
        RepositoryFactory.getOffersRepository()
    }

    private val mySqlOffersRepository by lazy {
        RepositoryFactory.getMySqlOffersRepository()
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
            storageRepository = storageRepository ?: throw IllegalStateException("StorageRepository not initialized"),
            performanceMonitor = getPerformanceMonitor()
        )
    }

    /**
     * Set the platform-specific context
     * @param context Platform-specific context object
     */
    @Synchronized
    fun setPlatformContext(context: Any) {
        platformContext = context
    }

    /**
     * Get the platform-specific context
     * @return Platform-specific context or null
     */
    @Synchronized
    fun getPlatformContext(): Any? = platformContext

    /**
     * Initialize platform-specific dependencies
     */
    fun initPlatformDependencies(
        context: Any,
        networkMonitor: NetworkMonitor,
        analyticsManager: AnalyticsManager,
        ocrProcessor: OcrProcessor,
        performanceMonitor: PerformanceMonitor,
        storageRepository: StorageRepository
    ) {
        // Set platform context
        this.platformContext = context

        // Initialize other platform-specific dependencies
        this.networkMonitor = networkMonitor
        this.analyticsManager = analyticsManager
        this.ocrProcessor = ocrProcessor
        this.performanceMonitor = performanceMonitor
        this.storageRepository = storageRepository

        // Start network monitoring
        networkMonitor.startMonitoring()
    }

    /**
     * Initialize repositories
     */
    fun initRepositories() {
        // Trigger lazy initialization
        val auth = authRepository
        val user = userRepository
        val car = carRepository
        val campaign = campaignRepository
        val storage = storageRepository
    }

    // Getters for dependencies
    fun getAuthRepository(): AuthRepository = authRepository
    fun getUserRepository(): UserRepository = userRepository
    fun getCarRepository(): CarRepository = carRepository
    fun getCampaignRepository(): CampaignRepository = campaignRepository
    fun getStorageRepository(): StorageRepository = storageRepository
        ?: throw IllegalStateException("StorageRepository not initialized")
    fun getOffersRepository(): OptimizedOffersRepository = offersRepository

    // Get MySQL specific repositories when needed
    fun getMySqlOffersRepository() = mySqlOffersRepository

    fun getNetworkMonitor(): NetworkMonitor =
        networkMonitor ?: throw IllegalStateException("NetworkMonitor not initialized")

    fun getAnalyticsManager(): AnalyticsManager =
        analyticsManager ?: throw IllegalStateException("AnalyticsManager not initialized")

    fun getOcrProcessor(): OcrProcessor =
        ocrProcessor ?: throw IllegalStateException("OcrProcessor not initialized")

    fun getPerformanceMonitor(): PerformanceMonitor =
        performanceMonitor ?: throw IllegalStateException("PerformanceMonitor not initialized")

    fun getAppNavigator(): AppNavigator = appNavigator

    fun getOfflineRepositoryWrapper(): OfflineRepositoryWrapper =
        offlineRepositoryWrapper ?: throw IllegalStateException("NetworkMonitor not initialized")

    /**
     * Cleanup resources
     */
    fun cleanup() {
        networkMonitor?.stopMonitoring()
        // Add any additional cleanup logic for other dependencies
        platformContext = null
    }
}