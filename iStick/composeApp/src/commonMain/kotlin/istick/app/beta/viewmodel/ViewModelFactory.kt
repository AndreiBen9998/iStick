// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/ViewModelFactory.kt
package istick.app.beta.viewmodel

import istick.app.beta.repository.RepositoryFactory
import istick.app.beta.repository.UserRepository

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
}