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
            val authException = mapFirebaseException(