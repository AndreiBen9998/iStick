// androidMain/kotlin/istick/app/beta/repository/MySqlUserRepository.kt
package istick.app.beta.repository

import android.util.Log
import istick.app.beta.auth.AuthRepository
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Brand
import istick.app.beta.model.CarOwner
import istick.app.beta.model.CompanyDetails
import istick.app.beta.model.User
import istick.app.beta.model.UserType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

actual class MySqlUserRepository actual constructor(private val authRepository: AuthRepository) : UserRepository {
    private val TAG = "MySqlUserRepository"

    private val _currentUser = MutableStateFlow<User?>(null)
    actual override val currentUser: StateFlow<User?> = _currentUser

    // Cache for user data
    private val userCache = mutableMapOf<String, User>()

    actual override suspend fun createUser(
        email: String,
        name: String,
        userType: UserType
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, this would create a user document in the database
            // after authentication
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )

            // Check if user already exists
            val existingUser = getUserById(userId, userType)
            if (existingUser != null) {
                return@withContext Result.success(existingUser)
            }

            // Create new user object
            val newUser = when (userType) {
                UserType.CAR_OWNER -> {
                    try {
                        // Try to insert into database
                        DatabaseHelper.executeUpdate(
                            """
                            UPDATE users_personal 
                            SET full_name = ?, type = ? 
                            WHERE id = ?
                            """,
                            listOf(name, userType.name, userId)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating user", e)
                        // Continue with in-memory object creation even if DB fails
                    }

                    CarOwner(
                        id = userId,
                        email = email,
                        name = name
                    )
                }
                UserType.BRAND -> {
                    try {
                        // Try to insert into database
                        DatabaseHelper.executeUpdate(
                            """
                            INSERT INTO users_business 
                            (id, email, company_name, password, created_at, type) 
                            VALUES (?, ?, ?, '', ?, ?)
                            ON CONFLICT (id) DO UPDATE SET
                            company_name = excluded.company_name,
                            type = excluded.type
                            """,
                            listOf(userId, email, name, System.currentTimeMillis(), userType.name)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inserting brand user", e)
                        // Continue with in-memory object creation even if DB fails
                    }

                    Brand(
                        id = userId,
                        email = email,
                        name = name,
                        companyDetails = CompanyDetails(
                            companyName = name
                        )
                    )
                }
            }

            // Update cache
            userCache[userId] = newUser
            _currentUser.value = newUser

            Result.success(newUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            Result.failure(e)
        }
    }

    actual override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, this would update the user document in the database
            when (user) {
                is CarOwner -> {
                    try {
                        DatabaseHelper.executeUpdate(
                            """
                            UPDATE users_personal 
                            SET full_name = ?, city = ?, daily_driving_distance = ?
                            WHERE id = ?
                            """,
                            listOf(
                                user.name,
                                user.city,
                                user.dailyDrivingDistance,
                                user.id
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating car owner", e)
                        // Continue with in-memory update even if DB fails
                    }
                }
                is Brand -> {
                    try {
                        DatabaseHelper.executeUpdate(
                            """
                            UPDATE users_business 
                            SET company_name = ?, industry = ?, website = ?, description = ?
                            WHERE id = ?
                            """,
                            listOf(
                                user.companyDetails.companyName,
                                user.companyDetails.industry,
                                user.companyDetails.website,
                                user.companyDetails.description,
                                user.id
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating brand", e)
                        // Continue with in-memory update even if DB fails
                    }
                }
                else -> {
                    return@withContext Result.failure(Exception("Unknown user type"))
                }
            }

            // Update cache
            userCache[user.id] = user

            // Update current user if it's the same
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(e)
        }
    }

    actual override suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID from auth
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.success(null)

            // Check if user is in cache
            userCache[userId]?.let {
                _currentUser.value = it
                return@withContext Result.success(it)
            }

            // Try to fetch from database
            var userType: UserType? = null

            // First check in personal users
            try {
                val personalUser = DatabaseHelper.executeQuery(
                    "SELECT type FROM users_personal WHERE id = ?",
                    listOf(userId)
                ) { rs ->
                    if (rs.next()) {
                        try {
                            val typeStr = rs.getString("type")
                            UserType.valueOf(typeStr)
                        } catch (e: Exception) {
                            UserType.CAR_OWNER // Default
                        }
                    } else null
                }

                if (personalUser != null) {
                    userType = personalUser
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking personal users", e)
                // Continue to check business users
            }

            // Then check business users if not found
            if (userType == null) {
                try {
                    val businessUser = DatabaseHelper.executeQuery(
                        "SELECT type FROM users_business WHERE id = ?",
                        listOf(userId)
                    ) { rs ->
                        if (rs.next()) {
                            try {
                                val typeStr = rs.getString("type")
                                UserType.valueOf(typeStr)
                            } catch (e: Exception) {
                                UserType.BRAND // Default
                            }
                        } else null
                    }

                    if (businessUser != null) {
                        userType = businessUser
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking business users", e)
                }
            }

            // Create user object based on the type
            val user = if (userType == UserType.BRAND) {
                createMockBrand(userId)
            } else {
                createMockCarOwner(userId)
            }

            // Update cache
            userCache[userId] = user
            _currentUser.value = user

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            Result.failure(e)
        }
    }

    actual override suspend fun updateUserProfilePicture(
        userId: String,
        imageUrl: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Get existing user
            val existingUser = userCache[userId] ?: return@withContext Result.failure(
                Exception("User not found")
            )

            // Update profile picture
            val updatedUser = when (existingUser) {
                is CarOwner -> {
                    try {
                        DatabaseHelper.executeUpdate(
                            "UPDATE users_personal SET profile_picture_url = ? WHERE id = ?",
                            listOf(imageUrl, userId)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating profile picture", e)
                        // Continue with in-memory update even if DB fails
                    }

                    existingUser.copy(profilePictureUrl = imageUrl)
                }
                is Brand -> {
                    try {
                        DatabaseHelper.executeUpdate(
                            "UPDATE users_business SET profile_picture_url = ? WHERE id = ?",
                            listOf(imageUrl, userId)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating profile picture", e)
                        // Continue with in-memory update even if DB fails
                    }

                    existingUser.copy(profilePictureUrl = imageUrl)
                }
                else -> {
                    return@withContext Result.failure(Exception("Unsupported user type"))
                }
            }

            // Update cache
            userCache[userId] = updatedUser

            // Update current user if it's the same
            if (_currentUser.value?.id == userId) {
                _currentUser.value = updatedUser
            }

            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile picture", e)
            Result.failure(e)
        }
    }

    // Helper methods
    private suspend fun getUserById(userId: String, userType: UserType): User? {
        return try {
            when (userType) {
                UserType.CAR_OWNER -> {
                    DatabaseHelper.executeQuery(
                        """
                        SELECT email, full_name, city, daily_driving_distance, profile_picture_url
                        FROM users_personal
                        WHERE id = ?
                        """,
                        listOf(userId)
                    ) { rs ->
                        if (rs.next()) {
                            CarOwner(
                                id = userId,
                                email = rs.getString("email"),
                                name = rs.getString("full_name"),
                                city = rs.getString("city") ?: "",
                                dailyDrivingDistance = rs.getInt("daily_driving_distance"),
                                profilePictureUrl = rs.getString("profile_picture_url")
                            )
                        } else null
                    }
                }
                UserType.BRAND -> {
                    DatabaseHelper.executeQuery(
                        """
                        SELECT email, company_name, industry, website, description, profile_picture_url
                        FROM users_business
                        WHERE id = ?
                        """,
                        listOf(userId)
                    ) { rs ->
                        if (rs.next()) {
                            Brand(
                                id = userId,
                                email = rs.getString("email"),
                                name = rs.getString("company_name"),
                                profilePictureUrl = rs.getString("profile_picture_url"),
                                companyDetails = CompanyDetails(
                                    companyName = rs.getString("company_name"),
                                    industry = rs.getString("industry") ?: "",
                                    website = rs.getString("website") ?: "",
                                    description = rs.getString("description") ?: ""
                                )
                            )
                        } else null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by id", e)
            null
        }
    }

    private fun createMockCarOwner(userId: String): CarOwner {
        return CarOwner(
            id = userId,
            email = "user@example.com",
            name = "Default User",
            city = "București",
            dailyDrivingDistance = 50
        )
    }

    private fun createMockBrand(userId: String): Brand {
        return Brand(
            id = userId,
            email = "brand@example.com",
            name = "Default Brand",
            companyDetails = CompanyDetails(
                companyName = "Default Brand",
                industry = "Technology",
                website = "https://example.com"
            )
        )
    }
}