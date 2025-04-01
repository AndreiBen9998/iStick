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

    // Core repositories - Changed from private to public
    private val _authRepository: AuthRepository by lazy {
        DefaultAuthRepository()
    }


    private val _userRepository: UserRepository by lazy {
        DefaultUserRepository(_authRepository)
    }


    private val _carRepository: CarRepository by lazy {
        DefaultCarRepository()
    }

    private val _campaignRepository: CampaignRepository by lazy {
        DefaultCampaignRepository(_authRepository)
    }


    val offersRepository: OptimizedOffersRepository by lazy {
        RepositoryFactory.getOffersRepository()
    }

    val mySqlOffersRepository: MySqlOffersRepository by lazy {
        RepositoryFactory.getMySqlOffersRepository()
    }

    val offlineRepositoryWrapper: OfflineRepositoryWrapper? by lazy {
        networkMonitor?.let { OfflineRepositoryWrapper(it) }
    }

    private val _appNavigator: AppNavigator by lazy {
        AppNavigator(
            authRepository = _authRepository,
            userRepository = _userRepository,
            campaignRepository = _campaignRepository,
            carRepository = _carRepository,
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
        // Implement your platform dependencies initialization here
        setPlatformContext(context)
        this.networkMonitor = networkMonitor
        this.analyticsManager = analyticsManager
        this.ocrProcessor = ocrProcessor
        this.performanceMonitor = performanceMonitor
        this.storageRepository = storageRepository
    }

    /**
     * Initialize repositories
     */
    fun initRepositories() {
        // Implement your repositories initialization logic here
        // This would typically be called after platform dependencies are set up
    }

    // The following getter methods are removed to avoid platform declaration clashes
    // Instead, use the public properties directly

    // The other getter methods that weren't in the clash errors can remain
    fun getStorageRepository(): StorageRepository {
        return storageRepository ?: throw IllegalStateException("StorageRepository not initialized")
    }

    fun getNetworkMonitor(): NetworkMonitor {
        return networkMonitor ?: throw IllegalStateException("NetworkMonitor not initialized")
    }

    fun getAnalyticsManager(): AnalyticsManager {
        return analyticsManager ?: throw IllegalStateException("AnalyticsManager not initialized")
    }

    fun getOcrProcessor(): OcrProcessor {
        return ocrProcessor ?: throw IllegalStateException("OcrProcessor not initialized")
    }

    fun getPerformanceMonitor(): PerformanceMonitor {
        return performanceMonitor ?: throw IllegalStateException("PerformanceMonitor not initialized")
    }

    /**
     * Clean up resources when the app is shutting down
     */
    fun cleanup() {
        // Release resources
        networkMonitor = null
        analyticsManager = null
        ocrProcessor = null
        performanceMonitor = null
        storageRepository = null
        platformContext = null
    }

    // Repository getter methods
   fun getAuthRepository(): AuthRepository = _authRepository

   fun getUserRepository(): UserRepository = _userRepository

   fun getAppNavigator(): AppNavigator = _appNavigator

   fun getCarRepository(): CarRepository = _carRepository

   fun getCampaignRepository(): CampaignRepository = _campaignRepository
}