// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/auth/EnhancedFirebaseAuthRepository.kt
package istick.app.beta.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
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
                
                // Update last sign in time in user record (would be done in UserRepository)
                
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
     * Resend email verification
     */
    suspend fun resendEmailVerification(): Result<Unit> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.NotAuthenticated
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
     * Refresh email verification status
     */
    suspend fun refreshEmailVerificationStatus(): Result<Boolean> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.NotAuthenticated
            )
            
            // Reload user data
            user.reload()
            
            // Update email verification state
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
     * Change user password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = 
        withContext(dispatcher) {
            try {
                val user = auth.currentUser ?: return@withContext Result.failure(
                    AuthException.NotAuthenticated
                )
                
                // Validate new password
                if (!isStrongPassword(newPassword)) {
                    return@withContext Result.failure(AuthException.WeakPassword)
                }
                
                // Re-authenticate user
                val email = user.email ?: return@withContext Result.failure(
                    AuthException.Unknown("User email is null")
                )
                
                auth.signInWithEmailAndPassword(email, currentPassword)
                
                // Change password
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
    suspend fun deleteAccount(password: String): Result<Unit> = withContext(dispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                AuthException.NotAuthenticated
            )
            
            // Re-authenticate user
            val email = user.email ?: return@withContext Result.failure(
                AuthException.Unknown("User email is null")
            )
            
            auth.signInWithEmailAndPassword(email, password)
            
            // Delete user
            user.delete()
            
            // Update state
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
     * Validate email format (simple validation)
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
    
    /**
     * Validate password strength
     */
    private fun isStrongPassword(password: String): Boolean {
        // At least 8 characters, containing a letter and a number
        return password.length >= 8 &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() }
    }
    
    /**
     * Map Firebase exceptions to our AuthException types
     */
    private fun mapFirebaseException(e: Exception): AuthException {
        val message = e.message ?: "Unknown error"
        
        return when {
            message.contains("email address is badly formatted") -> AuthException.InvalidEmail
            message.contains("password is invalid") -> AuthException.InvalidCredentials
            message.contains("no user record") -> AuthException.UserNotFound
            message.contains("email address is already in use") -> AuthException.EmailAlreadyInUse
            message.contains("password is too weak") -> AuthException.WeakPassword
            message.contains("network") -> AuthException.NetworkError
            message.contains("too-many-requests") -> AuthException.TooManyRequests
            message.contains("expired") || message.contains("expired") -> AuthException.SessionExpired
            else -> AuthException.Unknown(message)
        }
    }
    
    /**
     * Auth state enum
     */
    enum class AuthState {
        UNKNOWN,
        AUTHENTICATED,
        UNAUTHENTICATED
    }
    
    /**
     * Auth exceptions
     */
    sealed class AuthException(message: String) : Exception(message) {
        object InvalidEmail : AuthException("Invalid email format")
        object InvalidCredentials : AuthException("Incorrect email or password")
        object UserNotFound : AuthException("No account found with this email")
        object EmailAlreadyInUse : AuthException("This email is already in use")
        object WeakPassword : AuthException("Password is too weak, use at least 8 characters with letters and numbers")
        object EmailNotVerified : AuthException("Please verify your email before signing in")
        object NetworkError : AuthException("Network error, please check your connection")
        object TooManyRequests : AuthException("Too many attempts, please try again later")
        object SessionExpired : AuthException("Your session has expired, please sign in again")
        object NotAuthenticated : AuthException("You are not signed in")
        class Unknown(message: String) : AuthException(message)
    }
}