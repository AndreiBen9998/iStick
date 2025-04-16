// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/MySqlUserRepository.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class MySqlUserRepository(private val authRepository: AuthRepository) : UserRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override suspend fun createUser(email: String, name: String, userType: UserType): Result<User> = 
        withContext(Dispatchers.IO) {
            try {
                val userId = authRepository.getCurrentUserId() ?: 
                    return@withContext Result.failure(Exception("User not authenticated"))
                
                // The user is already created in the auth tables (users_personal or users_business)
                // We just need to retrieve it based on userType
                
                val user = when (userType) {
                    UserType.CAR_OWNER -> {
                        // Fetch from users_personal
                        DatabaseHelper.executeQuery(
                            """
                            SELECT * FROM users_personal 
                            WHERE id = ? OR email = ?
                            """,
                            listOf(userId.toLongOrNull() ?: 0, email)
                        ) { rs ->
                            if (rs.next()) {
                                CarOwner(
                                    id = rs.getString("id"),
                                    email = rs.getString("email"),
                                    name = rs.getString("full_name"),
                                    profilePictureUrl = rs.getString("profile_image"),
                                    createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                                    lastLoginAt = rs.getTimestamp("last_login")?.time ?: System.currentTimeMillis(),
                                    city = rs.getString("address") ?: "",
                                    dailyDrivingDistance = 0 // Not directly in DB, could be calculated
                                )
                            } else null
                        }
                    }
                    UserType.BRAND -> {
                        // Fetch from users_business
                        DatabaseHelper.executeQuery(
                            """
                            SELECT * FROM users_business 
                            WHERE id = ? OR email = ?
                            """,
                            listOf(userId.toLongOrNull() ?: 0, email)
                        ) { rs ->
                            if (rs.next()) {
                                Brand(
                                    id = rs.getString("id"),
                                    email = rs.getString("email"),
                                    name = rs.getString("full_name"),
                                    profilePictureUrl = null, // Add if available
                                    createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                                    lastLoginAt = rs.getTimestamp("last_login")?.time ?: System.currentTimeMillis(),
                                    companyDetails = CompanyDetails(
                                        companyName = rs.getString("company_name"),
                                        industry = "", // Not directly in DB
                                        website = rs.getString("company_website") ?: "",
                                        description = "" // Not directly in DB
                                    )
                                )
                            } else null
                        }
                    }
                }
                
                return@withContext if (user != null) {
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("User not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            when (user) {
                is CarOwner -> {
                    // Update in users_personal
                    val updated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_personal 
                        SET full_name = ?, 
                            address = ?,
                            updated_at = NOW()
                        WHERE id = ?
                        """,
                        listOf(
                            user.name,
                            user.city,
                            user.id.toLongOrNull() ?: 0
                        )
                    )
                    
                    if (updated > 0) {
                        _currentUser.value = user
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Failed to update user"))
                    }
                }
                is Brand -> {
                    // Update in users_business
                    val updated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_business 
                        SET full_name = ?, 
                            company_name = ?,
                            company_website = ?,
                            updated_at = NOW()
                        WHERE id = ?
                        """,
                        listOf(
                            user.name,
                            user.companyDetails.companyName,
                            user.companyDetails.website,
                            user.id.toLongOrNull() ?: 0
                        )
                    )
                    
                    if (updated > 0) {
                        _currentUser.value = user
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Failed to update user"))
                    }
                }
                else -> Result.failure(Exception("Unknown user type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId() ?: 
                return@withContext Result.success(null)
            
            // Try to fetch from users_personal first
            var user: User? = DatabaseHelper.executeQuery(
                "SELECT * FROM users_personal WHERE id = ?",
                listOf(userId.toLongOrNull() ?: 0)
            ) { rs ->
                if (rs.next()) {
                    CarOwner(
                        id = rs.getString("id"),
                        email = rs.getString("email"),
                        name = rs.getString("full_name"),
                        profilePictureUrl = rs.getString("profile_image"),
                        createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                        lastLoginAt = rs.getTimestamp("last_login")?.time ?: System.currentTimeMillis(),
                        city = rs.getString("address") ?: "",
                        dailyDrivingDistance = 0 // Not directly in DB
                    )
                } else null
            }
            
            // If not found, try users_business
            if (user == null) {
                user = DatabaseHelper.executeQuery(
                    "SELECT * FROM users_business WHERE id = ?",
                    listOf(userId.toLongOrNull() ?: 0)
                ) { rs ->
                    if (rs.next()) {
                        Brand(
                            id = rs.getString("id"),
                            email = rs.getString("email"),
                            name = rs.getString("full_name"),
                            profilePictureUrl = null,
                            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                            lastLoginAt = rs.getTimestamp("last_login")?.time ?: System.currentTimeMillis(),
                            companyDetails = CompanyDetails(
                                companyName = rs.getString("company_name"),
                                industry = "", // Not directly in DB
                                website = rs.getString("company_website") ?: "",
                                description = "" // Not directly in DB
                            )
                        )
                    } else null
                }
            }
            
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User> = 
        withContext(Dispatchers.IO) {
            try {
                // Try to update in users_personal first
                var updated = DatabaseHelper.executeUpdate(
                    """
                    UPDATE users_personal 
                    SET profile_image = ?,
                        updated_at = NOW()
                    WHERE id = ?
                    """,
                    listOf(imageUrl, userId.toLongOrNull() ?: 0)
                )
                
                if (updated == 0) {
                    // If not found, try users_business
                    updated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_business 
                        SET profile_image = ?,
                            updated_at = NOW()
                        WHERE id = ?
                        """,
                        listOf(imageUrl, userId.toLongOrNull() ?: 0)
                    )
                }
                
                if (updated > 0) {
                    // Refresh current user
                    return@withContext getCurrentUser()
                } else {
                    Result.failure(Exception("User not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}