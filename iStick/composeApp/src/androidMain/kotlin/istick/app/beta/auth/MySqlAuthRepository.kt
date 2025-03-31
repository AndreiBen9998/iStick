// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/auth/MySqlAuthRepository.kt
package istick.app.beta.auth

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * MySQL implementation of Auth Repository
 */
class MySqlAuthRepository : AuthRepository {
    private val TAG = "MySqlAuthRepository"
    private val _authState = MutableStateFlow(false)
    private var currentUserId: String? = null

    override suspend fun signUp(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if user already exists
            val existingUser = DatabaseHelper.executeQuery(
                "SELECT id FROM users WHERE email = ?",
                listOf(email)
            ) { resultSet ->
                if (resultSet.next()) resultSet.getLong("id").toString() else null
            }

            if (existingUser != null) {
                return@withContext Result.failure(Exception("User with this email already exists"))
            }

            // Hash the password for security
            val hashedPassword = hashPassword(password)

            // Insert new user into database
            val userId = DatabaseHelper.executeInsert(
                "INSERT INTO users (email, password, created_at, last_login_at) VALUES (?, ?, NOW(), NOW())",
                listOf(email, hashedPassword)
            )

            if (userId > 0) {
                currentUserId = userId.toString()
                _authState.value = true
                return@withContext Result.success(userId.toString())
            } else {
                return@withContext Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Hash the password for comparison
            val hashedPassword = hashPassword(password)

            // Query database for user
            val userId = DatabaseHelper.executeQuery(
                "SELECT id FROM users WHERE email = ? AND password = ?",
                listOf(email, hashedPassword)
            ) { resultSet ->
                if (resultSet.next()) resultSet.getLong("id").toString() else null
            }

            if (userId != null) {
                currentUserId = userId
                _authState.value = true

                // Update last login timestamp
                DatabaseHelper.executeUpdate(
                    "UPDATE users SET last_login_at = NOW() WHERE id = ?",
                    listOf(userId.toLong())
                )

                return@withContext Result.success(userId)
            } else {
                return@withContext Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun signOut() {
        currentUserId = null
        _authState.value = false
    }

    override fun getCurrentUserId(): String? {
        return currentUserId
    }

    override fun isUserLoggedIn(): Boolean {
        return _authState.value
    }

    override fun observeAuthState(): Flow<Boolean> {
        return _authState
    }

    // Helper method to hash passwords
    private fun hashPassword(password: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing password: ${e.message}", e)
            // Fallback to simple hashing if secure hashing fails
            return password.hashCode().toString()
        }
    }
}