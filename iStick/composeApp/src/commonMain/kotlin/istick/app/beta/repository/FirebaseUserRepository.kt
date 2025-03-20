// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/FirebaseUserRepository.kt
package istick.app.beta.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firebase implementation of the user repository
 */
class FirebaseUserRepository(
    private val authRepository: AuthRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : UserRepository {
    // Firebase instances
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")

    // In-memory cache for current user
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // In-memory cache for other users
    private val userCache = mutableMapOf<String, User>()

    init {
        // Start observing auth state to update current user
        observeAuthState()
    }

    /**
     * Observe authentication state changes and update current user accordingly
     */
    private fun observeAuthState() {
        GlobalScope.launch(dispatcher) {
            authRepository.observeAuthState().collect { isLoggedIn ->
                if (!isLoggedIn) {
                    // Clear current user when logged out
                    _currentUser.value = null
                    userCache.clear()
                } else {
                    // Reload current user when logged in
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null && _currentUser.value == null) {
                        getCurrentUser()
                    }
                }
            }
        }
    }

    override suspend fun createUser(email: String, name: String, userType: UserType): Result<User> =
        withContext(dispatcher) {
            try {
                // Get current user ID from auth
                val userId = authRepository.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                // Create the user object based on type
                val newUser = when (userType) {
                    UserType.CAR_OWNER -> CarOwner(
                        id = userId,
                        email = email,
                        name = name
                    )
                    UserType.BRAND -> Brand(
                        id = userId,
                        email = email,
                        name = name
                    )
                }

                // Prepare user data for Firestore
                val userData = when (userType) {
                    UserType.CAR_OWNER -> mapOf(
                        "id" to userId,
                        "email" to email,
                        "name" to name,
                        "profilePictureUrl" to null,
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis(),
                        "type" to userType.name,
                        "rating" to 0f,
                        "reviewCount" to 0,
                        "city" to "",
                        "dailyDrivingDistance" to 0,
                        "adPreferences" to emptyList<String>()
                    )
                    UserType.BRAND -> mapOf(
                        "id" to userId,
                        "email" to email,
                        "name" to name,
                        "profilePictureUrl" to null,
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis(),
                        "type" to userType.name,
                        "rating" to 0f,
                        "reviewCount" to 0,
                        "companyDetails" to mapOf(
                            "companyName" to "",
                            "industry" to "",
                            "website" to "",
                            "description" to "",
                            "logoUrl" to ""
                        )
                    )
                }

                // Create the user document in Firestore
                usersCollection.document(userId).set(userData).await()

                // Update cache
                userCache[userId] = newUser
                _currentUser.value = newUser

                Result.success(newUser)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to create user: ${e.message}", e))
            }
        }

    override suspend fun updateUser(user: User): Result<User> = withContext(dispatcher) {
        try {
            // Prepare the update data based on user type
            val updateData = when (user) {
                is CarOwner -> mapOf(
                    "name" to user.name,
                    "profilePictureUrl" to user.profilePictureUrl,
                    "city" to user.city,
                    "dailyDrivingDistance" to user.dailyDrivingDistance,
                    "adPreferences" to user.adPreferences,
                    "lastLoginAt" to System.currentTimeMillis()
                )
                is Brand -> mapOf(
                    "name" to user.name,
                    "profilePictureUrl" to user.profilePictureUrl,
                    "lastLoginAt" to System.currentTimeMillis(),
                    "companyDetails" to mapOf(
                        "companyName" to user.companyDetails.companyName,
                        "industry" to user.companyDetails.industry,
                        "website" to user.companyDetails.website,
                        "description" to user.companyDetails.description,
                        "logoUrl" to user.companyDetails.logoUrl
                    )
                )
                else -> return@withContext Result.failure(
                    Exception("Unsupported user type")
                )
            }

            // Update the user document in Firestore
            usersCollection.document(user.id).update(updateData).await()

            // Update cache
            userCache[user.id] = user
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }

            Result.success(user)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to update user: ${e.message}", e))
        }
    }

    override suspend fun getCurrentUser(): Result<User?> = withContext(dispatcher) {
        try {
            // Get current user ID from auth
            val userId = authRepository.getCurrentUserId() ?: return@withContext Result.success(null)

            // Check if user is in cache
            userCache[userId]?.let {
                _currentUser.value = it
                return@withContext Result.success(it)
            }

            // Fetch from Firestore if not in cache
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists) {
                return@withContext Result.success(null)
            }

            // Parse user type
            val userTypeStr = userDoc.getString("type")
            val userType = try {
                if (userTypeStr != null) UserType.valueOf(userTypeStr)
                else return@withContext Result.failure(Exception("User type not found"))
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Invalid user type: $userTypeStr"))
            }

            // Create user object based on type
            val user = when (userType) {
                UserType.CAR_OWNER -> {
                    val carOwner = CarOwner(
                        id = userId,
                        email = userDoc.getString("email") ?: "",
                        name = userDoc.getString("name") ?: "",
                        profilePictureUrl = userDoc.getString("profilePictureUrl"),
                        createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                        lastLoginAt = userDoc.getLong("lastLoginAt") ?: System.currentTimeMillis(),
                        rating = userDoc.getDouble("rating")?.toFloat() ?: 0f,
                        reviewCount = userDoc.getLong("reviewCount")?.toInt() ?: 0,
                        city = userDoc.getString("city") ?: "",
                        dailyDrivingDistance = userDoc.getLong("dailyDrivingDistance")?.toInt() ?: 0
                    )
                    carOwner
                }
                UserType.BRAND -> {
                    val companyDetailsMap = userDoc.get("companyDetails") as? Map<String, Any> ?: emptyMap()
                    val companyDetails = CompanyDetails(
                        companyName = companyDetailsMap["companyName"] as? String ?: "",
                        industry = companyDetailsMap["industry"] as? String ?: "",
                        website = companyDetailsMap["website"] as? String ?: "",
                        description = companyDetailsMap["description"] as? String ?: "",
                        logoUrl = companyDetailsMap["logoUrl"] as? String ?: ""
                    )

                    Brand(
                        id = userId,
                        email = userDoc.getString("email") ?: "",
                        name = userDoc.getString("name") ?: "",
                        profilePictureUrl = userDoc.getString("profilePictureUrl"),
                        createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                        lastLoginAt = userDoc.getLong("lastLoginAt") ?: System.currentTimeMillis(),
                        rating = userDoc.getDouble("rating")?.toFloat() ?: 0f,
                        reviewCount = userDoc.getLong("reviewCount")?.toInt() ?: 0,
                        companyDetails = companyDetails
                    )
                }
            }

            // Update cache and current user
            userCache[userId] = user
            _currentUser.value = user

            // Update last login time in background
            firestore.runTransaction { transaction ->
                transaction.update(
                    usersCollection.document(userId),
                    mapOf("lastLoginAt" to System.currentTimeMillis())
                )
            }

            Result.success(user)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get current user: ${e.message}", e))
        }
    }

    override suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<User> =
        withContext(dispatcher) {
            try {
                // Get existing user
                val existingUser = userCache[userId] ?: run {
                    // If not in cache, update the profile picture URL directly in Firestore
                    usersCollection.document(userId).update(
                        mapOf("profilePictureUrl" to imageUrl)
                    ).await()

                    // Fetch the updated user (or null if it fails)
                    val userResult = getUserById(userId)
                    return@withContext userResult
                }

                // Update profile picture for the existing user in cache
                val updatedUser = when (existingUser) {
                    is CarOwner -> existingUser.copy(profilePictureUrl = imageUrl)
                    is Brand -> existingUser.copy(profilePictureUrl = imageUrl)
                    else -> return@withContext Result.failure(Exception("Unsupported user type"))
                }

                // Update Firestore
                usersCollection.document(userId).update(
                    mapOf("profilePictureUrl" to imageUrl)
                ).await()

                // Update cache
                userCache[userId] = updatedUser
                if (_currentUser.value?.id == userId) {
                    _currentUser.value = updatedUser
                }

                Result.success(updatedUser)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to update profile picture: ${e.message}", e))
            }
        }

    /**
     * Get user by ID - useful for fetching other users' profiles
     */
    suspend fun getUserById(userId: String): Result<User> = withContext(dispatcher) {
        try {
            // Check cache first
            userCache[userId]?.let {
                return@withContext Result.success(it)
            }

            // Fetch from Firestore
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists) {
                return@withContext Result.failure(Exception("User not found"))
            }

            // Parse user type
            val userTypeStr = userDoc.getString("type")
            val userType = try {
                if (userTypeStr != null) UserType.valueOf(userTypeStr)
                else return@withContext Result.failure(Exception("User type not found"))
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Invalid user type: $userTypeStr"))
            }

            // Create user object based on type
            val user = when (userType) {
                UserType.CAR_OWNER -> {
                    CarOwner(
                        id = userId,
                        email = userDoc.getString("email") ?: "",
                        name = userDoc.getString("name") ?: "",
                        profilePictureUrl = userDoc.getString("profilePictureUrl"),
                        createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                        lastLoginAt = userDoc.getLong("lastLoginAt") ?: System.currentTimeMillis(),
                        rating = userDoc.getDouble("rating")?.toFloat() ?: 0f,
                        reviewCount = userDoc.getLong("reviewCount")?.toInt() ?: 0,
                        city = userDoc.getString("city") ?: "",
                        dailyDrivingDistance = userDoc.getLong("dailyDrivingDistance")?.toInt() ?: 0
                    )
                }
                UserType.BRAND -> {
                    val companyDetailsMap = userDoc.get("companyDetails") as? Map<String, Any> ?: emptyMap()
                    val companyDetails = CompanyDetails(
                        companyName = companyDetailsMap["companyName"] as? String ?: "",
                        industry = companyDetailsMap["industry"] as? String ?: "",
                        website = companyDetailsMap["website"] as? String ?: "",
                        description = companyDetailsMap["description"] as? String ?: "",
                        logoUrl = companyDetailsMap["logoUrl"] as? String ?: ""
                    )

                    Brand(
                        id = userId,
                        email = userDoc.getString("email") ?: "",
                        name = userDoc.getString("name") ?: "",
                        profilePictureUrl = userDoc.getString("profilePictureUrl"),
                        createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                        lastLoginAt = userDoc.getLong("lastLoginAt") ?: System.currentTimeMillis(),
                        rating = userDoc.getDouble("rating")?.toFloat() ?: 0f,
                        reviewCount = userDoc.getLong("reviewCount")?.toInt() ?: 0,
                        companyDetails = companyDetails
                    )
                }
            }

            // Update cache
            userCache[userId] = user

            Result.success(user)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get user: ${e.message}", e))
        }
    }

    /**
     * Search users by name
     */
    suspend fun searchUsersByName(query: String, maxResults: Int = 20): Result<List<User>> =
        withContext(dispatcher) {
            try {
                if (query.length < 2) {
                    return@withContext Result.success(emptyList())
                }

                val querySnapshot = usersCollection
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .limit(maxResults.toLong())
                    .get()
                    .await()

                val users = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val userId = doc.id
                        val userTypeStr = doc.getString("type")
                        val userType = try {
                            if (userTypeStr != null) UserType.valueOf(userTypeStr) else return@mapNotNull null
                        } catch (e: Exception) {
                            return@mapNotNull null
                        }

                        val user = when (userType) {
                            UserType.CAR_OWNER -> {
                                CarOwner(
                                    id = userId,
                                    email = doc.getString("email") ?: "",
                                    name = doc.getString("name") ?: "",
                                    profilePictureUrl = doc.getString("profilePictureUrl"),
                                    rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                                    city = doc.getString("city") ?: ""
                                )
                            }
                            UserType.BRAND -> {
                                val companyDetailsMap = doc.get("companyDetails") as? Map<String, Any> ?: emptyMap()

                                Brand(
                                    id = userId,
                                    email = doc.getString("email") ?: "",
                                    name = doc.getString("name") ?: "",
                                    profilePictureUrl = doc.getString("profilePictureUrl"),
                                    rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                                    companyDetails = CompanyDetails(
                                        companyName = companyDetailsMap["companyName"] as? String ?: "",
                                        industry = companyDetailsMap["industry"] as? String ?: ""
                                    )
                                )
                            }
                        }

                        // Update cache
                        userCache[userId] = user

                        user
                    } catch (e: Exception) {
                        null
                    }
                }

                Result.success(users)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to search users: ${e.message}", e))
            }
        }

    /**
     * Clear cache
     */
    fun clearCache() {
        userCache.clear()
        _currentUser.value = null
    }
}
