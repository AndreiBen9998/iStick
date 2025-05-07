package istick.app.beta.di

import android.content.Context
import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.RoomAuthRepository
import istick.app.beta.database.AppDatabase
import istick.app.beta.network.NetworkMonitor
import istick.app.beta.ocr.OcrProcessor
import istick.app.beta.repository.*
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.analytics.AnalyticsManager
import istick.app.beta.payment.PaymentService
import istick.app.beta.payment.DefaultPaymentService
import istick.app.beta.ui.navigation.AppNavigator

object DependencyInjection {
    // Platform-specific dependencies
    @Volatile
    private var platformContext: Context? = null

    private var database: AppDatabase? = null
    private var networkMonitor: NetworkMonitor? = null
    private var analyticsManager: AnalyticsManager? = null
    private var ocrProcessor: OcrProcessor? = null
    private var performanceMonitor: PerformanceMonitor? = null
    private var storageRepository: StorageRepository? = null

    // Core repositories
    private val _authRepository: AuthRepository by lazy {
        RoomAuthRepository(getDatabase())
    }

    private val _userRepository: UserRepository by lazy {
        RoomUserRepository(_authRepository, getDatabase())
    }

    private val _carRepository: CarRepository by lazy {
        RoomCarRepository(getDatabase())
    }

    private val _campaignRepository: CampaignRepository by lazy {
        RoomCampaignRepository(_authRepository, getDatabase())
    }

    private val _paymentService: PaymentService by lazy {
        DefaultPaymentService()
    }

    private val _appNavigator: AppNavigator by lazy {
        AppNavigator(
            authRepository = _authRepository,
            userRepository = _userRepository,
            campaignRepository = _campaignRepository,
            carRepository = _carRepository,
            storageRepository = getStorageRepository(),
            performanceMonitor = getPerformanceMonitor()
        )
    }

    @Synchronized
    fun setPlatformContext(context: Context) {
        platformContext = context.applicationContext
    }

    @Synchronized
    fun getPlatformContext(): Context {
        return platformContext ?: throw IllegalStateException("Platform context not initialized")
    }

    fun initPlatformDependencies(
        context: Context,
        database: AppDatabase,
        networkMonitor: NetworkMonitor,
        analyticsManager: AnalyticsManager,
        ocrProcessor: OcrProcessor,
        performanceMonitor: PerformanceMonitor,
        storageRepository: StorageRepository
    ) {
        setPlatformContext(context)
        this.database = database
        this.networkMonitor = networkMonitor
        this.analyticsManager = analyticsManager
        this.ocrProcessor = ocrProcessor
        this.performanceMonitor = performanceMonitor
        this.storageRepository = storageRepository

        // Initialize the repository factory as well
        RepositoryFactory.initialize(_authRepository, storageRepository, database)
    }

    fun initRepositories() {
        // Any additional repository initialization can go here
    }

    fun getDatabase(): AppDatabase {
        return database ?: throw IllegalStateException("Database not initialized")
    }

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

    fun cleanup() {
        networkMonitor?.stopMonitoring()
        networkMonitor = null
        analyticsManager = null
        ocrProcessor = null
        performanceMonitor = null
        storageRepository = null
        database = null
        platformContext = null
        RepositoryFactory.reset()
    }

    fun getAuthRepository(): AuthRepository = _authRepository
    fun getUserRepository(): UserRepository = _userRepository
    fun getAppNavigator(): AppNavigator = _appNavigator
    fun getCarRepository(): CarRepository = _carRepository
    fun getCampaignRepository(): CampaignRepository = _campaignRepository
    fun getPaymentService(): PaymentService = _paymentService
}