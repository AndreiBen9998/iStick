package istick.app.beta.auth

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signOut()
    fun getCurrentUserId(): String?
    fun isUserLoggedIn(): Boolean
}