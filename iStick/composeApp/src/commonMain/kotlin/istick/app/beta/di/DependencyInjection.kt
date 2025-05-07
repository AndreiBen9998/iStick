// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/di/DependencyInjection.kt
package istick.app.beta.di

import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.DefaultAuthRepository
import istick.app.beta.network.ApiClient
import istick.app.beta.network.MySqlApiClient
import istick.app.beta.repository.*
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.network.NetworkMonitor
import istick.app.beta.analytics.AnalyticsManager
import istick.app.beta.ocr.OcrProcessor
import istick.app.beta.payment.PaymentService
import istick.app.beta.payment.MySqlPaymentService
import istick.app.beta.ui.navigation.AppNavigator
import istick.app.beta.viewmodel.PaymentViewModel
import istick.app.beta.viewmodel.ViewModelFactory

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
    private val _authRepository: AuthRepository by lazy {
        DefaultAuthRepository()
    }

    private val _userRepository: UserRepository by lazy {
        MySqlUserRepository(_authRepository)
    }

    private val _carRepository: CarRepository by lazy {
        MySqlCarRepository()
    }

    private val _apiClient: ApiClient by lazy {
        MySqlApiClient(_authRepository, getStorageRepository())
    }

    private val _campaignRepository: CampaignRepository by lazy {
        DefaultCampaignRepository(_authRepository)
    }

    val offersRepository: OffersRepositoryInterface by lazy {
        OptimizedOffersRepository(_apiClient)
    }

    val mySqlOffersRepository: MySqlOffersRepository by lazy {
        MySqlOffersRepository()
    }

    private val _paymentService: PaymentService by lazy {
        MySqlPaymentService()
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

    @Synchronized
    fun setPlatformContext(context: Any) {
        platformContext = context
    }

    @Synchronized
    fun getPlatformContext(): Any? = platformContext

    fun initPlatformDependencies(
        context: Any,
        networkMonitor: NetworkMonitor,
        analyticsManager: AnalyticsManager,
        ocrProcessor: OcrProcessor,
        performanceMonitor: PerformanceMonitor,
        storageRepository: StorageRepository
    ) {
        setPlatformContext(context)
        this.networkMonitor = networkMonitor
        this.analyticsManager = analyticsManager
        this.ocrProcessor = ocrProcessor
        this.performanceMonitor = performanceMonitor
        this.storageRepository = storageRepository

        // Initialize the repository factory as well
        RepositoryFactory.initialize(_authRepository, storageRepository)
    }

    fun initRepositories() {
        // Repository initialization
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
        networkMonitor = null
        analyticsManager = null
        ocrProcessor = null
        performanceMonitor = null
        storageRepository = null
        platformContext = null
        RepositoryFactory.reset()
    }

    fun getAuthRepository(): AuthRepository = _authRepository

    fun getUserRepository(): UserRepository = _userRepository

    fun getAppNavigator(): AppNavigator = _appNavigator

    fun getCarRepository(): CarRepository = _carRepository

    fun getCampaignRepository(): CampaignRepository = _campaignRepository

    fun getApiClient(): ApiClient = _apiClient

    fun getPaymentService(): PaymentService = _paymentService

    fun createPaymentViewModel(): PaymentViewModel {
        return ViewModelFactory.createPaymentViewModel(
            authRepository = getAuthRepository(),
            paymentService = getPaymentService()
        )
    }
}