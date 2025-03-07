package istick.app.beta.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.cancellation.CancellationException

class FirebaseAuthRepository : AuthRepository {
    private val auth = Firebase.auth

    override suspend fun signUp(email: String, password: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password)
                println("Sign up successful: ${result.user?.uid}")
                Result.success(result.user?.uid ?: "")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("Sign up error: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun signIn(email: String, password: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                println("Attempting sign in for email: $email")
                val result = auth.signInWithEmailAndPassword(email, password)
                println("Sign in successful: ${result.user?.uid}")
                Result.success(result.user?.uid ?: "")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("Sign in error: ${e.javaClass.simpleName} - ${e.message}")

                // Create more user-friendly error messages
                val errorMsg = when {
                    e.message?.contains("password is invalid") == true ->
                        "Parola este incorectă."
                    e.message?.contains("no user record") == true ->
                        "Nu există niciun cont cu acest email."
                    e.message?.contains("blocked") == true || e.message?.contains("disabled") == true ->
                        "Acest cont a fost dezactivat."
                    e.message?.contains("network") == true ->
                        "Eroare de rețea. Verificați conexiunea."
                    else -> e.message ?: "Eroare de autentificare necunoscută."
                }

                Result.failure(RuntimeException(errorMsg, e))
            }
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}