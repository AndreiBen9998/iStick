// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/navigation/AppNavigator.kt
package istick.app.beta.ui.navigation

import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.FirebaseAuthRepository
import istick.app.beta.repository.CarRepository
import istick.app.beta.repository.CampaignRepository
import istick.app.beta.repository.FirebaseCarRepository
import istick.app.beta.repository.FirebaseCampaignRepository
import istick.app.beta.repository.FirebaseUserRepository
import istick.app.beta.repository.UserRepository
import istick.app.beta.storage.FirebaseStorageRepository
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.CarManagementViewModel
import istick.app.beta.viewmodel.CampaignDetailViewModel
import istick.app.beta.viewmodel.HomeViewModel
import istick.app.beta.viewmodel.ProfileViewModel

/**
 * Navigation manager for the app
 */
class AppNavigator(
    val authRepository: AuthRepository = FirebaseAuthRepository(),
    val userRepository: UserRepository = FirebaseUserRepository(authRepository),
    val campaignRepository: CampaignRepository = FirebaseCampaignRepository(),
    val carRepository: CarRepository = FirebaseCarRepository(),
    val storageRepository: StorageRepository = FirebaseStorageRepository(),
    val performanceMonitor: PerformanceMonitor
) {
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