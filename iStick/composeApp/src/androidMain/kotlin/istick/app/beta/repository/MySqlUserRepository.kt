// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/repository/MySqlUserRepository.kt

package istick.app.beta.repository

import android.util.Log
import istick.app.beta.auth.AuthRepository
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * MySQL implementation of UserRepository
 */
actual class MySqlUserRepository actual constructor(private val authRepository: AuthRepository) : UserRepository {
    private val TAG = "MySqlUserRepository"
    private val _currentUser = MutableStateFlow<User?>(null)
    actual override val currentUser: StateFlow<User?> = _currentUser

    actual override suspend fun createUser(email: String, name: String, userType: UserType): Result<User> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Table differs based on user type
            val result = if (userType == UserType.CAR_OWNER) {
                // Insert into car owners table
                DatabaseHelper.executeUpdate(
                    "INSERT INTO users_drivers (user_id, full_name) VALUES (?, ?)",
                    listOf<Any>(userId.toLong(), name)
                )
            } else {
                // Insert into brands table
                DatabaseHelper.executeUpdate(
                    "INSERT INTO users_business (user_id, full_name) VALUES (?, ?)",
                    listOf<Any>(userId.toLong(), name)
                )

                // Also create company details record
                DatabaseHelper.executeUpdate(
                    "INSERT INTO company_details (user_id) VALUES (?)",
                    listOf<Any>(userId.toLong())
                )
            }

            if (result > 0) {
                // Query the newly created user
                val user = getUserByIdAndType(userId, userType)
                if (user != null) {
                    _currentUser.value = user
                    return@withContext Result.success(user)
                } else {
                    return@withContext Result.failure(Exception("Failed to retrieve created user"))
                }
            } else {
                return@withContext Result.failure(Exception("Failed to create user profile"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    actual override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            val result = when (user) {
                is CarOwner -> {
                    // Update car owner profile
                    DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_drivers 
                        SET full_name = ?, city = ?, daily_driving_distance = ?, profile_picture_url = ?
                        WHERE user_id = ?
                        """,
                        listOf<Any>(
                            user.name,
                            user.city,
                            user.dailyDrivingDistance,
                            user.profilePictureUrl ?: "",
                            user.id.toLong()
                        )
                    )
                }
                is Brand -> {
                    // Update brand profile
                    val brandResult = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_business 
                        SET full_name = ?, profile_picture_url = ?
                        WHERE user_id = ?
                        """,
                        listOf<Any>(
                            user.name,
                            user.profilePictureUrl ?: "",
                            user.id.toLong()
                        )
                    )

                    // Also update company details
                    val companyResult = DatabaseHelper.executeUpdate(
                        """
                        UPDATE company_details 
                        SET company_name = ?, industry = ?, website = ?, description = ?, logo_url = ?
                        WHERE user_id = ?
                        """,
                        listOf<Any>(
                            user.companyDetails.companyName,
                            user.companyDetails.industry,
                            user.companyDetails.website,
                            user.companyDetails.description,
                            user.companyDetails.logoUrl,
                            user.id.toLong()
                        )
                    )

                    brandResult + companyResult
                }
                else -> 0
            }

            if (result > 0) {
                // If this is the current user, update the state
                if (_currentUser.value?.id == user.id) {
                    _currentUser.value = user
                }

                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("Failed to update user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    actual override suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.success(null)

            // First determine the user type
            val userType = getUserType(userId)
                ?: return@withContext Result.failure(Exception("User type not found"))

            // Now get the full user profile based on type
            val user = getUserByIdAndType(userId, userType)

            if (user != null) {
                _currentUser.value = user
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    actual override suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // First determine the user type
            val userType = getUserType(userId)
                ?: return@withContext Result.failure(Exception("User type not found"))

            // Table name based on user type
            val tableName = if (userType == UserType.CAR_OWNER) "users_drivers" else "users_business"

            // Update profile picture
            val result = DatabaseHelper.executeUpdate(
                "UPDATE $tableName SET profile_picture_url = ? WHERE user_id = ?",
                listOf<Any>(imageUrl, userId.toLong())
            )

            if (result > 0) {
                // Get updated user
                val user = getUserByIdAndType(userId, userType)

                if (user != null) {
                    // Update current user if this is the one
                    if (_currentUser.value?.id == userId) {
                        _currentUser.value = user
                    }

                    return@withContext Result.success(user)
                } else {
                    return@withContext Result.failure(Exception("Failed to retrieve updated user"))
                }
            } else {
                return@withContext Result.failure(Exception("Failed to update profile picture"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile picture: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    // Helper method to determine user type
    private suspend fun getUserType(userId: String): UserType? {
        return try {
            DatabaseHelper.executeQuery(
                """
                SELECT 
                    CASE 
                        WHEN EXISTS(SELECT 1 FROM users_drivers WHERE user_id = ?) THEN 'CAR_OWNER'
                        WHEN EXISTS(SELECT 1 FROM users_business WHERE user_id = ?) THEN 'BRAND'
                        ELSE NULL
                    END as user_type
                """,
                listOf<Any>(userId.toLong(), userId.toLong())
            ) { resultSet ->
                if (resultSet.next()) {
                    val typeStr = resultSet.getString("user_type")
                    when (typeStr) {
                        "CAR_OWNER" -> UserType.CAR_OWNER
                        "BRAND" -> UserType.BRAND
                        else -> null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining user type: ${e.message}", e)
            null
        }
    }

    // Helper method to get a user by ID and type
    private suspend fun getUserByIdAndType(userId: String, userType: UserType): User? {
        return try {
            when (userType) {
                UserType.CAR_OWNER -> {
                    // Query car owner details
                    DatabaseHelper.executeQuery(
                        """
                        SELECT d.*, u.email, u.created_at, u.last_login_at,
                               COALESCE(AVG(r.rating), 0) as avg_rating,
                               COUNT(r.id) as review_count
                        FROM users_drivers d
                        JOIN users u ON d.user_id = u.id
                        LEFT JOIN reviews r ON r.receiver_id = d.user_id
                        WHERE d.user_id = ?
                        GROUP BY d.user_id
                        """,
                        listOf<Any>(userId.toLong())
                    ) { resultSet ->
                        if (resultSet.next()) {
                            CarOwner(
                                id = userId,
                                email = resultSet.getString("email") ?: "",
                                name = resultSet.getString("full_name") ?: "",
                                profilePictureUrl = resultSet.getString("profile_picture_url"),
                                createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                                lastLoginAt = resultSet.getTimestamp("last_login_at")?.time ?: System.currentTimeMillis(),
                                rating = resultSet.getFloat("avg_rating"),
                                reviewCount = resultSet.getInt("review_count"),
                                type = UserType.CAR_OWNER,
                                city = resultSet.getString("city") ?: "",
                                dailyDrivingDistance = resultSet.getInt("daily_driving_distance")
                            )
                        } else {
                            null
                        }
                    }
                }
                UserType.BRAND -> {
                    // Query brand details
                    DatabaseHelper.executeQuery(
                        """
                        SELECT b.*, u.email, u.created_at, u.last_login_at, cd.*,
                               COALESCE(AVG(r.rating), 0) as avg_rating,
                               COUNT(r.id) as review_count
                        FROM users_business b
                        JOIN users u ON b.user_id = u.id
                        LEFT JOIN company_details cd ON cd.user_id = b.user_id
                        LEFT JOIN reviews r ON r.receiver_id = b.user_id
                        WHERE b.user_id = ?
                        GROUP BY b.user_id
                        """,
                        listOf<Any>(userId.toLong())
                    ) { resultSet ->
                        if (resultSet.next()) {
                            Brand(
                                id = userId,
                                email = resultSet.getString("email") ?: "",
                                name = resultSet.getString("full_name") ?: "",
                                profilePictureUrl = resultSet.getString("profile_picture_url"),
                                createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                                lastLoginAt = resultSet.getTimestamp("last_login_at")?.time ?: System.currentTimeMillis(),
                                rating = resultSet.getFloat("avg_rating"),
                                reviewCount = resultSet.getInt("review_count"),
                                type = UserType.BRAND,
                                companyDetails = CompanyDetails(
                                    companyName = resultSet.getString("company_name") ?: "",
                                    industry = resultSet.getString("industry") ?: "",
                                    website = resultSet.getString("website") ?: "",
                                    description = resultSet.getString("description") ?: "",
                                    logoUrl = resultSet.getString("logo_url") ?: ""
                                )
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID and type: ${e.message}", e)
            null
        }
    }
}