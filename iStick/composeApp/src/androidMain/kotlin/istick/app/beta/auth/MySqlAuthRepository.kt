// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/auth/MySqlAuthRepository.kt
package istick.app.beta.auth

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * MySQL implementation of Auth Repository
 */
class MySqlAuthRepository : AuthRepository {
    private val TAG = "MySqlAuthRepository"
    private val _authState = MutableStateFlow(false)
    private var currentUserId: String? = null
    
    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            // Hash the password (in a real implementation, use a proper password hashing library)
            val hashedPassword = password.hashCode().toString()
            
            // Insert new user into database
            val userId = DatabaseHelper.executeInsert(
                "INSERT INTO users (email, password) VALUES (?, ?)",
                listOf(email, hashedPassword)
            )
            
            if (userId > 0) {
                currentUserId = userId.toString()
                _authState.value = true
                Result.success(userId.toString())
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            // Hash the password the same way it was hashed during sign up
            val hashedPassword = password.hashCode().toString()
            
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
                
                Result.success(userId)
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in: ${e.message}", e)
            Result.failure(e)
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
}

// Define this as the default implementation to use
typealias DefaultAuthRepository = MySqlAuthRepository