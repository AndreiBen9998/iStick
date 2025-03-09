// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/UserRepository.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.Brand
import istick.app.beta.model.CarOwner
import istick.app.beta.model.User
import istick.app.beta.model.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing user data
 */
interface UserRepository {
    val currentUser: StateFlow<User?>

    suspend fun createUser(email: String, name: String, userType: UserType): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User>
}

/**
 * Firebase implementation of the user repository
 */
class FirebaseUserRepository(
    private val authRepository: AuthRepository
) : UserRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    // Cache for user data
    private val userCache = mutableMapOf<String, User>()

    override suspend fun createUser(email: String, name: String, userType: UserType): Result<User> = withContext(Dispatchers.Default) {
        try {
            // In a real implementation, this would create a user document in Firebase Firestore
            // after authentication
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )

            val newUser = when (userType) {
                UserType.CAR_OWNER -> CarOwner(
                    id = userId,
                    email = email,
                    name = name
                )
                UserType.BRAND -> Brand(
                    id = userId,
                    email = email,
                    name = name
                )
            }

            // Update cache
            userCache[userId] = newUser
            _currentUser.value = newUser

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            // In a real implementation, this would update the user document in Firebase Firestore

            // Update cache
            userCache[user.id] = user

            // Update current user if it's the same
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.Default) {
        try {
            // Get current user ID from auth
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.success(null)

            // Check if user is in cache
            userCache[userId]?.let {
                _currentUser.value = it
                return@withContext Result.success(it)
            }

            // In a real implementation, fetch from Firebase if not in cache
            // For now, return mock data based on user type (randomly assign for demo)
            val mockUser = if (System.currentTimeMillis() % 2 == 0L) {
                CarOwner(
                    id = userId,
                    email = "user@example.com",
                    name = "John Doe",
                    city = "București",
                    dailyDrivingDistance = 60
                )
            } else {
                Brand(
                    id = userId,
                    email = "brand@example.com",
                    name = "Tech Corp",
                    companyDetails = istick.app.beta.model.CompanyDetails(
                        companyName = "Tech Corporation SRL",
                        industry = "Technology",
                        website = "https://techcorp.example.com"
                    )
                )
            }

            // Update cache
            userCache[userId] = mockUser
            _currentUser.value = mockUser

            Result.success(mockUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User> = withContext(Dispatchers.Default) {
        try {
            // Get existing user
            val existingUser = userCache[userId] ?: return@withContext Result.failure(
                Exception("User not found")
            )

            // Update profile picture
            val updatedUser = when (existingUser) {
                is CarOwner -> existingUser.copy(profilePictureUrl = imageUrl)
                is Brand -> existingUser.copy(profilePictureUrl = imageUrl)
                else -> return@withContext Result.failure(Exception("Unsupported user type"))
            }

            // Update cache
            userCache[userId] = updatedUser

            // Update current user if it's the same
            if (_currentUser.value?.id == userId) {
                _currentUser.value = updatedUser
            }

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}