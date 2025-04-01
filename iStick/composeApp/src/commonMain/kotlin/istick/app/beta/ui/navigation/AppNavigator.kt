package istick.app.beta.ui.navigation

import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.DefaultAuthRepository
import istick.app.beta.repository.CarRepository
import istick.app.beta.repository.CampaignRepository
import istick.app.beta.repository.DefaultCarRepository
import istick.app.beta.repository.DefaultCampaignRepository
import istick.app.beta.repository.DefaultUserRepository
import istick.app.beta.repository.UserRepository
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.CarManagementViewModel
import istick.app.beta.viewmodel.CampaignDetailViewModel
import istick.app.beta.viewmodel.HomeViewModel
import istick.app.beta.viewmodel.MileageVerificationViewModel
import istick.app.beta.viewmodel.ProfileViewModel
import istick.app.beta.di.DependencyInjection

/**
 * Navigation manager for the app
 */
class AppNavigator(
    val authRepository: AuthRepository,
    val userRepository: UserRepository,
    val campaignRepository: CampaignRepository,
    val carRepository: CarRepository,
    val storageRepository: StorageRepository,
    val performanceMonitor: PerformanceMonitor
) {
    // Alternative constructor that gets dependencies from DI
    constructor(performanceMonitor: PerformanceMonitor) : this(
        authRepository = DependencyInjection.getAuthRepository(),
        userRepository = DependencyInjection.getUserRepository(),
        campaignRepository = DependencyInjection.getCampaignRepository(),
        carRepository = DependencyInjection.getCarRepository(),
        storageRepository = DependencyInjection.getStorageRepository(),
        performanceMonitor = performanceMonitor
    )

    // Screens definition
    sealed class Screen {
        // Auth screens
        object Login : Screen()
        object Registration : Screen()

        // Main navigation tabs
        object Home : Screen()
        object Map : Screen()
        object Photos : Screen()
        object Profile : Screen()

        // Detail screens
        data class CampaignDetail(val campaignId: String) : Screen()
        object CarManagement : Screen()
        data class AddEditCar(val carId: String? = null) : Screen()
        data class MileageVerification(val carId: String) : Screen()
        object VerificationHistory : Screen()

        // Brand-specific screens
        object CreateCampaign : Screen()
        object ManageCampaigns : Screen()
        data class ManageApplications(val campaignId: String) : Screen()
    }

    fun createMileageVerificationViewModel(): MileageVerificationViewModel {
        return MileageVerificationViewModel(
            carRepository = carRepository
        )
    }

    // ViewModel factory methods
    fun createHomeViewModel(): HomeViewModel {
        return HomeViewModel(userRepository = userRepository)
    }

    fun createCampaignDetailViewModel(): CampaignDetailViewModel {
        return CampaignDetailViewModel(
            campaignRepository = campaignRepository,
            carRepository = carRepository
        )
    }

    fun createCarManagementViewModel(): CarManagementViewModel {
        return CarManagementViewModel(
            carRepository = carRepository,
            storageRepository = storageRepository
        )
    }

    fun createProfileViewModel(): ProfileViewModel {
        return ProfileViewModel(
            authRepository = authRepository,
            userRepository = userRepository,
            carRepository = carRepository,
            storageRepository = storageRepository
        )
    }
}