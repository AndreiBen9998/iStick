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
    
    /**
     * Check if migration is needed
     */
    suspend fun isMigrationNeeded(): Boolean = withContext(Dispatchers.Default) {
        // Check if we have a migration marker in preferences/local storage
        // For this example, we'll just return true
        true
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
                val userData = when (user) {
                    is CarOwner -> {
                        mapOf(
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
                    }
                    is Brand -> {
                        mapOf(
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
                    }
                    else -> continue // Skip unknown user types
                }
                
                // Create user document in Firestore
                // In a real implementation, you'd use Firestore API directly here
                // For this example, we'll just log the migration
                println("Migrating user: ${user.id}")
                
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
                // In a real implementation, you'd use Firestore API directly here
                println("Migrating car: ${car.id}")
                
                // Migrate verifications as a subcollection
                for (verification in car.verification) {
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
                // In a real implementation, you'd use Firestore API directly here
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