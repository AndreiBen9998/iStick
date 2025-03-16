// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/migration/DataMigrationManager.kt

package istick.app.beta.migration

import istick.app.beta.model.*
import istick.app.beta.repository.*
import istick.app.beta.storage.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Utility for migrating data from local/mock sources to Firebase
 */
class DataMigrationManager(
    private val userRepository: FirebaseUserRepository,
    private val carRepository: FirebaseCarRepository,
    private val campaignRepository: FirebaseCampaignRepository,
    private val storageRepository: StorageRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Migration state
    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.NotStarted)
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    // Migration progress tracking
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    // Migration preferences key
    private val MIGRATION_COMPLETED_KEY = "migration_completed"

    /**
     * Check if migration is needed
     */
    suspend fun isMigrationNeeded(): Boolean = withContext(Dispatchers.Default) {
        // Check if we have a migration marker in preferences/local storage
        // For this example, we'll just return false since we don't have a real implementation
        // In a real app, you'd check a preference or database flag
        false
    }

    /**
     * Start migration process
     */
    fun startMigration(mockUserData: List<User>, mockCars: List<Car>, mockCampaigns: List<Campaign>) {
        if (_migrationState.value is MigrationState.InProgress) {
            return // Migration already in progress
        }

        _migrationState.value = MigrationState.InProgress(0)
        _progress.value = 0

        coroutineScope.launch {
            try {
                // 1. Migrate users
                _migrationState.value = MigrationState.InProgress(10)
                migrateUsers(mockUserData)

                // 2. Migrate cars
                _migrationState.value = MigrationState.InProgress(40)
                migrateCars(mockCars)

                // 3. Migrate campaigns
                _migrationState.value = MigrationState.InProgress(70)
                migrateCampaigns(mockCampaigns)

                // 4. Finalize migration - mark as completed in preferences/local storage
                _migrationState.value = MigrationState.InProgress(95)
                finalizeMigration()

                // 5. Migration complete
                _migrationState.value = MigrationState.Completed
                _progress.value = 100
            } catch (e: Exception) {
                _migrationState.value = MigrationState.Failed(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Migrate user data
     */
    private suspend fun migrateUsers(mockUsers: List<User>) = withContext(Dispatchers.Default) {
        // Track progress
        val totalUsers = mockUsers.size
        var processedUsers = 0

        for (user in mockUsers) {
            try {
                when (user) {
                    is CarOwner -> {
                        // Create user in Firestore
                        val userData = mapOf(
                            "id" to user.id,
                            "email" to user.email,
                            "name" to user.name,
                            "profilePictureUrl" to user.profilePictureUrl,
                            "createdAt" to user.createdAt,
                            "lastLoginAt" to user.lastLoginAt,
                            "type" to user.type.name,
                            "rating" to user.rating,
                            "reviewCount" to user.reviewCount,
                            "city" to user.city,
                            "dailyDrivingDistance" to user.dailyDrivingDistance,
                            "adPreferences" to user.adPreferences
                        )

                        // In a real implementation, you'd use the Firebase SDK directly:
                        // FirebaseFirestore.getInstance().collection("users").document(user.id).set(userData)
                        println("Migrating car owner: ${user.id}")
                    }
                    is Brand -> {
                        // Create user in Firestore
                        val userData = mapOf(
                            "id" to user.id,
                            "email" to user.email,
                            "name" to user.name,
                            "profilePictureUrl" to user.profilePictureUrl,
                            "createdAt" to user.createdAt,
                            "lastLoginAt" to user.lastLoginAt,
                            "type" to user.type.name,
                            "rating" to user.rating,
                            "reviewCount" to user.reviewCount,
                            "companyDetails" to mapOf(
                                "companyName" to user.companyDetails.companyName,
                                "industry" to user.companyDetails.industry,
                                "website" to user.companyDetails.website,
                                "description" to user.companyDetails.description,
                                "logoUrl" to user.companyDetails.logoUrl
                            )
                        )

                        // In a real implementation, you'd use the Firebase SDK directly:
                        // FirebaseFirestore.getInstance().collection("users").document(user.id).set(userData)
                        println("Migrating brand: ${user.id}")
                    }
                }

                // Update progress
                processedUsers++
                val userProgress = (processedUsers.toFloat() / totalUsers * 30).toInt()
                _progress.value = 10 + userProgress

            } catch (e: Exception) {
                println("Error migrating user ${user.id}: ${e.message}")
                // Continue with next user
            }
        }
    }

    /**
     * Migrate car data
     */
    private suspend fun migrateCars(mockCars: List<Car>) = withContext(Dispatchers.Default) {
        // Track progress
        val totalCars = mockCars.size
        var processedCars = 0

        for (car in mockCars) {
            try {
                // Create car document in Firestore
                val carData = mapOf(
                    "id" to car.id,
                    "ownerId" to car.id.substringBefore("_"), // Extract owner ID from car ID format
                    "make" to car.make,
                    "model" to car.model,
                    "year" to car.year,
                    "color" to car.color,
                    "licensePlate" to car.licensePlate,
                    "photos" to car.photos,
                    "currentMileage" to car.currentMileage,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )

                // In a real implementation, you'd use the Firebase SDK directly:
                // val carRef = FirebaseFirestore.getInstance().collection("cars").document(car.id)
                // carRef.set(carData)
                println("Migrating car: ${car.id}")

                // Migrate verifications as a subcollection
                for (verification in car.verification) {
                    val verificationData = mapOf(
                        "timestamp" to verification.timestamp,
                        "mileage" to verification.mileage,
                        "photoUrl" to verification.photoUrl,
                        "verificationCode" to verification.verificationCode,
                        "isVerified" to verification.isVerified
                    )

                    // In a real implementation, you'd use the Firebase SDK directly:
                    // carRef.collection("verifications").document(verification.id).set(verificationData)
                    println("Migrating verification: ${verification.id} for car ${car.id}")
                }

                // Update progress
                processedCars++
                val carProgress = (processedCars.toFloat() / totalCars * 30).toInt()
                _progress.value = 40 + carProgress

            } catch (e: Exception) {
                println("Error migrating car ${car.id}: ${e.message}")
                // Continue with next car
            }
        }
    }

    /**
     * Migrate campaign data
     */
    private suspend fun migrateCampaigns(mockCampaigns: List<Campaign>) = withContext(Dispatchers.Default) {
        // Track progress
        val totalCampaigns = mockCampaigns.size
        var processedCampaigns = 0

        for (campaign in mockCampaigns) {
            try {
                // Create campaign document in Firestore
                val campaignData = mapOf(
                    "id" to campaign.id,
                    "brandId" to campaign.brandId,
                    "title" to campaign.title,
                    "description" to campaign.description,
                    "stickerDetails" to mapOf(
                        "imageUrl" to campaign.stickerDetails.imageUrl,
                        "width" to campaign.stickerDetails.width,
                        "height" to campaign.stickerDetails.height,
                        "positions" to campaign.stickerDetails.positions.map { it.name },
                        "deliveryMethod" to campaign.stickerDetails.deliveryMethod.name
                    ),
                    "payment" to mapOf(
                        "amount" to campaign.payment.amount,
                        "currency" to campaign.payment.currency,
                        "paymentFrequency" to campaign.payment.paymentFrequency.name,
                        "paymentMethod" to campaign.payment.paymentMethod.name
                    ),
                    "requirements" to mapOf(
                        "minDailyDistance" to campaign.requirements.minDailyDistance,
                        "cities" to campaign.requirements.cities,
                        "carMakes" to campaign.requirements.carMakes,
                        "carModels" to campaign.requirements.carModels,
                        "carYearMin" to campaign.requirements.carYearMin,
                        "carYearMax" to campaign.requirements.carYearMax
                    ),
                    "status" to campaign.status.name,
                    "startDate" to campaign.startDate,
                    "endDate" to campaign.endDate,
                    "createdAt" to campaign.createdAt,
                    "updatedAt" to campaign.updatedAt,
                    "applicants" to campaign.applicants,
                    "approvedApplicants" to campaign.approvedApplicants
                )

                // In a real implementation, you'd use the Firebase SDK directly:
                // FirebaseFirestore.getInstance().collection("campaigns").document(campaign.id).set(campaignData)
                println("Migrating campaign: ${campaign.id}")

                // Update progress
                processedCampaigns++
                val campaignProgress = (processedCampaigns.toFloat() / totalCampaigns * 25).toInt()
                _progress.value = 70 + campaignProgress

            } catch (e: Exception) {
                println("Error migrating campaign ${campaign.id}: ${e.message}")
                // Continue with next campaign
            }
        }
    }

    /**
     * Finalize migration by marking it as completed
     */
    private suspend fun finalizeMigration() = withContext(Dispatchers.Default) {
        // In a real implementation, you'd store a flag in preferences/local storage
        // For example, using Android SharedPreferences:
        // val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // prefs.edit().putBoolean(MIGRATION_COMPLETED_KEY, true).apply()

        println("Migration completed")
    }

    /**
     * Migration state
     */
    sealed class MigrationState {
        object NotStarted : MigrationState()
        data class InProgress(val percentComplete: Int) : MigrationState()
        object Completed : MigrationState()
        data class Failed(val error: String) : MigrationState()
    }
}