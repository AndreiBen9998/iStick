// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/FirebaseUserRepository.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.Brand
import istick.app.beta.model.CarOwner
import istick.app.beta.model.User
import istick.app.beta.model.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Firebase implementation of UserRepository
 * This is a placeholder implementation using MySQL under the hood
 */
class FirebaseUserRepository(
    private val authRepository: AuthRepository
) : UserRepository {
    // Forward to MySQL implementation
    private val mysqlRepo = MySqlUserRepository(authRepository)

    override val currentUser: StateFlow<User?> = mysqlRepo.currentUser

    override suspend fun createUser(email: String, name: String, userType: UserType): Result<User> {
        return mysqlRepo.createUser(email, name, userType)
    }

    override suspend fun updateUser(user: User): Result<User> {
        return mysqlRepo.updateUser(user)
    }

    override suspend fun getCurrentUser(): Result<User?> {
        return mysqlRepo.getCurrentUser()
    }

    override suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User> {
        return mysqlRepo.updateUserProfilePicture(userId, imageUrl)
    }
}