// commonMain/kotlin/istick/app/beta/repository/MySqlUserRepository.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.User
import kotlinx.coroutines.flow.StateFlow

expect class MySqlUserRepository(authRepository: AuthRepository) : UserRepository {
    override val currentUser: StateFlow<User?>

    override suspend fun createUser(email: String, name: String, userType: istick.app.beta.model.UserType): Result<User>
    override suspend fun updateUser(user: User): Result<User>
    override suspend fun getCurrentUser(): Result<User?>
    override suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User>
}