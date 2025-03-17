// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/auth/EnhancedFirebaseAuthRepository.kt

package istick.app.beta.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Enhanced Firebase Auth Repository with additional features:
 * - Proper error handling with typed exceptions
 * - Token refresh management
 * - Session timeout handling
 * - Email verification
 * - Password reset
 */
class EnhancedFirebaseAuthRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AuthRepository {
    // Firebase Auth instance
    private val auth: FirebaseAuth = Firebase.auth

    // Auth state
    private val _authState = MutableStateFlow(AuthState.UNKNOWN)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Current user state
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)

    // Email verification state
    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    // Init block to set up state monitoring
    init {
        // Start monitoring auth state
        CoroutineScope(dispatcher).launch {
            try {
                auth.authStateChanged.collect { authResult ->
                    val user = auth.currentUser
                    _currentUser.value = user

                    if (user != null) {
                        _authState.value = AuthState.AUTHENTICATED
                        _isEmailVerified.value = user.isEmailVerified ?: false

                        // Refresh user data to ensure we have latest email verified status
                        try {
                            user.reload()
                            _isEmailVerified.value = user.isEmailVerified ?: false
                        } catch (e: Exception) {
                            // Ignore refresh errors
                        }
                    } else {
                        _authState.value = AuthState.UNAUTHENTICATED
                        _isEmailVerified.value = false
                    }
                }
            } catch (e: Exception) {
                // Handle initialization errors
                println("Error initializing auth state monitoring: ${e.message}")
                _authState.value = AuthState.UNAUTHENTICATED
            }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> =
        withContext(dispatcher) {
            try {
                // Validate email and password
                if (!isValidEmail(email)) {
                    return@withContext Result.failure(AuthException.InvalidEmail)
                }

                if (!isStrongPassword(password)) {
                    return@withContext Result.failure(AuthException.WeakPassword)
                }

                // Create user
                val result = auth.createUserWithEmailAndPassword(email, password)
                val user = result.user ?: return@withContext Result.failure(
                    AuthException.Unknown("User creation failed, but no error was returned")
                )

                // Send email verification
                try {
                    user.sendEmailVerification()
                } catch (e: Exception) {
                    // Log but don't fail if email verification fails
                    println("Failed to send verification email: ${e.message}")
                }

                Result.success(user.uid)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                // Map Firebase exceptions to our AuthException types
                val authException = mapFirebaseException(e)
                Result.failure(authException)
            }
        }

    override suspend fun signIn(email: String, password: String): Result<String> =
        withContext(dispatcher) {
            try {
                // Validate email
                if (!isValidEmail(email)) {
                    return@withContext Result.failure(AuthException.InvalidEmail)
                }

                // Sign in
                val result = auth.signInWithEmailAndPassword(email, password)
                val user = result.user ?: return@withContext Result.failure(
                    AuthException.Unknown("Sign in failed, but no error was returned")
                )

                // Check if email is verified (if verification is required)
                // Uncomment to enforce email verification
                /*
                if (!user.isEmailVerified) {
                    // Send a new verification email if needed
                    try {
                        user.sendEmailVerification()
                    } catch (e: Exception) {
                        // Ignore
                    }

                    return@withContext Result.failure(AuthException.EmailNotVerified)
                }
                */

                Result.success(user.uid)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                // Map Firebase exceptions to our AuthException types
                val authException = mapFirebaseException(e)
                Result.failure(authException)
            }
        }

    override suspend fun signOut() = withContext(dispatcher) {
        try {
            auth.signOut()
            _authState.value = AuthState.UNAUTHENTICATED
            _currentUser.value = null
            _isEmailVerified.value = false
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println("Error signing out: ${e.message}")
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun observeAuthState(): Flow<Boolean> = authState.map { state ->
        state == AuthState.AUTHENTICATED
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(dispatcher) {
        try {
            // Validate email
            if (!isValidEmail(email)) {
                return@withContext Result.failure(AuthException.InvalidEmail)
            }

            auth.sendPasswordResetEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            // Map Firebase exceptions to our AuthException types
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Update user email
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.UserNotAuthenticated
            )

            // Validate email
            if (!isValidEmail(newEmail)) {
                return@withContext Result.failure(AuthException.InvalidEmail)
            }

            user.updateEmail(newEmail)

            // Send new verification email
            try {
                user.sendEmailVerification()
            } catch (e: Exception) {
                // Log but don't fail if email verification fails
                println("Failed to send verification email: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            // Map Firebase exceptions to our AuthException types
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Update user password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.UserNotAuthenticated
            )

            // Validate password
            if (!isStrongPassword(newPassword)) {
                return@withContext Result.failure(AuthException.WeakPassword)
            }

            user.updatePassword(newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            // Map Firebase exceptions to our AuthException types
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.UserNotAuthenticated
            )

            user.delete()
            _authState.value = AuthState.UNAUTHENTICATED
            _currentUser.value = null
            _isEmailVerified.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            // Map Firebase exceptions to our AuthException types
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Send email verification
     */
    suspend fun sendEmailVerification(): Result<Unit> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.UserNotAuthenticated
            )

            user.sendEmailVerification()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            // Map Firebase exceptions to our AuthException types
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Check if email is verified
     */
    suspend fun checkEmailVerified(): Result<Boolean> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.UserNotAuthenticated
            )

            // Reload user to get latest verification status
            user.reload()
            val isVerified = user.isEmailVerified ?: false
            _isEmailVerified.value = isVerified
            Result.success(isVerified)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            // Map Firebase exceptions to our AuthException types
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Get ID token
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.UserNotAuthenticated
            )

            val tokenResult = user.getIdToken(forceRefresh)
            // The GitLive Firebase Auth wrapper directly returns the token string
            Result.success(tokenResult ?: "")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val authException = mapFirebaseException(e)
            Result.failure(authException)
        }
    }

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    /**
     * Validate password strength
     */
    private fun isStrongPassword(password: String): Boolean {
        // At least 6 characters (Firebase requirement)
        return password.length >= 6
    }

    /**
     * Map Firebase exception to AuthException
     */
    private fun mapFirebaseException(e: Exception): AuthException {
        // Map common Firebase exceptions to our typed exceptions
        return when {
            e.message?.contains("email address is badly formatted") == true ->
                AuthException.InvalidEmail
            e.message?.contains("password is invalid") == true ->
                AuthException.InvalidCredentials
            e.message?.contains("no user record") == true ->
                AuthException.UserNotFound
            e.message?.contains("email address is already in use") == true ->
                AuthException.EmailAlreadyInUse
            e.message?.contains("requires recent authentication") == true ->
                AuthException.RequiresRecentLogin
            e.message?.contains("network") == true ||
                    e.message?.contains("Network") == true ->
                AuthException.NetworkError
            else -> AuthException.Unknown(e.message ?: "Unknown error")
        }
    }
}

/**
 * Authentication state
 */
enum class AuthState {
    UNKNOWN,
    AUTHENTICATED,
    UNAUTHENTICATED
}

/**
 * Authentication exceptions
 */
sealed class AuthException(message: String) : Exception(message) {
    object InvalidEmail : AuthException("The email address is invalid")
    object InvalidCredentials : AuthException("The password is invalid")
    object UserNotFound : AuthException("No user found with this email")
    object EmailAlreadyInUse : AuthException("The email address is already in use")
    object WeakPassword : AuthException("The password is too weak")
    object UserNotAuthenticated : AuthException("User is not authenticated")
    object EmailNotVerified : AuthException("Email is not verified")
    object RequiresRecentLogin : AuthException("This operation requires recent authentication")
    object NetworkError : AuthException("A network error occurred")
    class Unknown(message: String) : AuthException(message)
}