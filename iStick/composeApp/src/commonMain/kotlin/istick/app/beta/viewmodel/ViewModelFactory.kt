// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/ViewModelFactory.kt
package istick.app.beta.viewmodel

import istick.app.beta.repository.RepositoryFactory
import istick.app.beta.repository.UserRepository
import istick.app.beta.repository.CampaignRepository
import istick.app.beta.repository.CarRepository
import istick.app.beta.auth.AuthRepository
import istick.app.beta.storage.StorageRepository
import istick.app.beta.utils.PerformanceMonitor

/**
 * Factory for creating ViewModel instances
 */
object ViewModelFactory {
    /**
     * Create a HomeViewModel instance with the configured data source
     */
    fun createHomeViewModel(userRepository: UserRepository): HomeViewModel {
        return HomeViewModel(
            offersRepository = RepositoryFactory.getOffersRepository(),
            userRepository = userRepository
        )
    }

    /**
     * Create a ProfileViewModel instance
     */
    fun createProfileViewModel(
        authRepository: AuthRepository,
        userRepository: UserRepository,
        carRepository: CarRepository,
        storageRepository: StorageRepository
    ): ProfileViewModel {
        return ProfileViewModel(
            authRepository = authRepository,
            userRepository = userRepository,
            carRepository = carRepository,
            storageRepository = storageRepository
        )
    }

    /**
     * Create a CarManagementViewModel instance
     */
    fun createCarManagementViewModel(
        carRepository: CarRepository,
        storageRepository: StorageRepository
    ): CarManagementViewModel {
        return CarManagementViewModel(
            carRepository = carRepository,
            storageRepository = storageRepository
        )
    }

    /**
     * Create a CampaignDetailViewModel instance
     */
    fun createCampaignDetailViewModel(
        campaignRepository: CampaignRepository,
        carRepository: CarRepository
    ): CampaignDetailViewModel {
        return CampaignDetailViewModel(
            campaignRepository = campaignRepository,
            carRepository = carRepository
        )
    }

    /**
     * Create a MileageVerificationViewModel instance
     */
    fun createMileageVerificationViewModel(
        carRepository: CarRepository
    ): MileageVerificationViewModel {
        return MileageVerificationViewModel(
            carRepository = carRepository
        )
    }

    /**
     * Create a RegistrationViewModel instance
     */
    fun createRegistrationViewModel(
        authRepository: AuthRepository,
        userRepository: UserRepository
    ): RegistrationViewModel {
        return RegistrationViewModel(
            authRepository = authRepository,
            userRepository = userRepository
        )
    }
    /**
     * Create a CampaignAnalyticsViewModel instance
     */
    fun createCampaignAnalyticsViewModel(
        campaignRepository: CampaignRepository,
        userRepository: UserRepository
    ): CampaignAnalyticsViewModel {
        return CampaignAnalyticsViewModel(
            campaignRepository = campaignRepository,
            userRepository = userRepository
        )
    }
}