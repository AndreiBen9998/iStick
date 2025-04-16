// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/auth/DefaultAuthRepository.kt
package istick.app.beta.auth

import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class DefaultAuthRepository : AuthRepository {
    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _isLoggedIn = MutableStateFlow(false)

    override suspend fun signUp(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Check if user exists in either table
                val existsPersonal = DatabaseHelper.executeQuery(
                    "SELECT id FROM users_personal WHERE email = ?",
                    listOf(email)
                ) { rs -> rs.next() }

                val existsBusiness = DatabaseHelper.executeQuery(
                    "SELECT id FROM users_business WHERE email = ?",
                    listOf(email)
                ) { rs -> rs.next() }

                if (existsPersonal || existsBusiness) {
                    return@withContext Result.failure(Exception("Email already exists"))
                }

                // Hash the password
                val hashedPassword = hashPassword(password)

                // For simplicity, we'll insert into users_personal
                // In a real app, you'd determine the type and insert accordingly
                val userId = DatabaseHelper.executeInsert(
                    """
                    INSERT INTO users_personal 
                    (full_name, email, password, phone, birth_date, car_type, address, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                    """,
                    listOf(
                        "New User", // Default name
                        email,
                        hashedPassword,
                        "", // Default phone
                        java.sql.Date(System.currentTimeMillis()), // Current date
                        "", // Default car type
                        "" // Default address
                    )
                )

                if (userId > 0) {
                    _currentUserId.value = userId.toString()
                    _isLoggedIn.value = true
                    Result.success(userId.toString())
                } else {
                    Result.failure(Exception("Failed to create user"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun signIn(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Check in users_personal
                var userId: String? = null
                var isValid = false

                val personalUser = DatabaseHelper.executeQuery(
                    "SELECT id, password FROM users_personal WHERE email = ?",
                    listOf(email)
                ) { rs ->
                    if (rs.next()) {
                        val id = rs.getString("id")
                        val storedPassword = rs.getString("password")
                        id to storedPassword
                    } else null
                }

                if (personalUser != null) {
                    val (id, storedPassword) = personalUser
                    isValid = verifyPassword(password, storedPassword)
                    if (isValid) {
                        userId = id
                    }
                }

                // If not found or invalid, check in users_business
                if (userId == null) {
                    val businessUser = DatabaseHelper.executeQuery(
                        "SELECT id, password FROM users_business WHERE email = ?",
                        listOf(email)
                    ) { rs ->
                        if (rs.next()) {
                            val id = rs.getString("id")
                            val storedPassword = rs.getString("password")
                            id to storedPassword
                        } else null
                    }

                    if (businessUser != null) {
                        val (id, storedPassword) = businessUser
                        isValid = verifyPassword(password, storedPassword)
                        if (isValid) {
                            userId = id
                        }
                    }
                }

                if (userId != null && isValid) {
                    _currentUserId.value = userId
                    _isLoggedIn.value = true

                    // Update last login time
                    // This would normally include updating both tables

                    Result.success(userId)
                } else {
                    Result.failure(Exception("Invalid email or password"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun signOut() {
        _currentUserId.value = null
        _isLoggedIn.value = false
    }

    override fun getCurrentUserId(): String? {
        return _currentUserId.value
    }

    override fun isUserLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    override fun observeAuthState(): Flow<Boolean> {
        return _isLoggedIn.asStateFlow()
    }

    private fun hashPassword(password: String): String {
        // In a real app, use a proper password hashing library
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(password: String, hashedPassword: String): Boolean {
        // For simplicity, we'll use a direct comparison
        // In a real app, you'd use a proper password verification method
        return hashPassword(password) == hashedPassword
    }
}