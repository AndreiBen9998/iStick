// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/CampaignRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignApplication
import istick.app.beta.model.CampaignStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

fun observeActiveCampaigns(): Flow<List<Campaign>>
/**
 * Repository interface for managing campaigns
 */
interface CampaignRepository {
    val activeCampaigns: StateFlow<List<Campaign>>
    val userApplications: StateFlow<List<CampaignApplication>>
    
    suspend fun fetchActiveCampaigns(): Result<List<Campaign>>
    suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign>
    suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>>
    suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication>
    suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign>
}

/**
 * Firebase implementation of the campaign repository
 */
class FirebaseCampaignRepository : CampaignRepository {
    private val _activeCampaigns = MutableStateFlow<List<Campaign>>(emptyList())
    override val activeCampaigns: StateFlow<List<Campaign>> = _activeCampaigns
    
    private val _userApplications = MutableStateFlow<List<CampaignApplication>>(emptyList())
    override val userApplications: StateFlow<List<CampaignApplication>> = _userApplications
    
    // Cache to reduce network calls
    private val campaignCache = mutableMapOf<String, Campaign>()
    private val applicationCache = mutableMapOf<String, CampaignApplication>()
    
    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> = withContext(Dispatchers.Default) {
        try {
            // For now, return mock data
            // In a real implementation, this would fetch from Firebase Firestore
            val mockCampaigns = listOf(
                Campaign(
                    id = "camp1",
                    brandId = "brand1",
                    title = "TechCorp Promotional Campaign",
                    description = "Promote our new tech product line with car stickers across the city",
                    stickerDetails = istick.app.beta.model.StickerDetails(
                        imageUrl = "https://example.com/stickers/techcorp.png",
                        width = 30,
                        height = 20,
                        positions = listOf(
                            istick.app.beta.model.StickerPosition.DOOR_LEFT,
                            istick.app.beta.model.StickerPosition.DOOR_RIGHT
                        )
                    ),
                    payment = istick.app.beta.model.PaymentDetails(
                        amount = 500.0,
                        currency = "RON",
                        paymentFrequency = istick.app.beta.model.PaymentFrequency.MONTHLY
                    ),
                    requirements = istick.app.beta.model.CampaignRequirements(
                        minDailyDistance = 50,
                        cities = listOf("București", "Cluj", "Iași")
                    ),
                    status = CampaignStatus.ACTIVE,
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
                ),
                Campaign(
                    id = "camp2",
                    brandId = "brand2",
                    title = "EcoDrive Campaign",
                    description = "Promote eco-friendly driving habits with our branded stickers",
                    stickerDetails = istick.app.beta.model.StickerDetails(
                        imageUrl = "https://example.com/stickers/ecodrive.png",
                        width = 25,
                        height = 15,
                        positions = listOf(
                            istick.app.beta.model.StickerPosition.REAR_WINDOW
                        )
                    ),
                    payment = istick.app.beta.model.PaymentDetails(
                        amount = 400.0,
                        currency = "RON",
                        paymentFrequency = istick.app.beta.model.PaymentFrequency.MONTHLY
                    ),
                    requirements = istick.app.beta.model.CampaignRequirements(
                        minDailyDistance = 30,
                        cities = listOf("București", "Timișoara", "Constanța")
                    ),
                    status = CampaignStatus.ACTIVE,
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000) // 60 days
                ),
                Campaign(
                    id = "camp3",
                    brandId = "brand3",
                    title = "LocalBiz Network",
                    description = "Support local businesses by promoting our network on your car",
                    stickerDetails = istick.app.beta.model.StickerDetails(
                        imageUrl = "https://example.com/stickers/localbiz.png",
                        width = 40,
                        height = 25,
                        positions = listOf(
                            istick.app.beta.model.StickerPosition.HOOD,
                            istick.app.beta.model.StickerPosition.SIDE_PANEL
                        )
                    ),
                    payment = istick.app.beta.model.PaymentDetails(
                        amount = 600.0,
                        currency = "RON",
                        paymentFrequency = istick.app.beta.model.PaymentFrequency.MONTHLY
                    ),
                    requirements = istick.app.beta.model.CampaignRequirements(
                        minDailyDistance = 100,
                        cities = listOf("București"),
                        carYearMin = 2015
                    ),
                    status = CampaignStatus.ACTIVE,
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000) // 90 days
                )
            )
            
            // Update cache
            mockCampaigns.forEach { campaign ->
                campaignCache[campaign.id] = campaign
            }
            
            _activeCampaigns.value = mockCampaigns
            Result.success(mockCampaigns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign> = withContext(Dispatchers.Default) {
        try {
            // Try to get from cache first
            campaignCache[campaignId]?.let {
                return@withContext Result.success(it)
            }
            
            // In a real implementation, fetch from Firebase if not in cache
            // For now, return mock data
            val mockCampaign = Campaign(
                id = campaignId,
                brandId = "brand1",
                title = "Campaign Details",
                description = "Detailed description of the campaign",
                // Other fields...
                status = CampaignStatus.ACTIVE
            )
            
            // Update cache
            campaignCache[campaignId] = mockCampaign
            
            Result.success(mockCampaign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>> = withContext(Dispatchers.Default) {
        try {
            // For now, return mock data
            val mockApplications = listOf(
                CampaignApplication(
                    id = "app1",
                    campaignId = "camp1",
                    carOwnerId = userId,
                    carId = "car1",
                    status = istick.app.beta.model.ApplicationStatus.PENDING
                ),
                CampaignApplication(
                    id = "app2",
                    campaignId = "camp2",
                    carOwnerId = userId,
                    carId = "car1",
                    status = istick.app.beta.model.ApplicationStatus.APPROVED
                )
            )
            
            // Update cache
            mockApplications.forEach { application ->
                applicationCache[application.id] = application
            }
            
            _userApplications.value = mockApplications
            Result.success(mockApplications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication> = withContext(Dispatchers.Default) {
        try {
            // In a real implementation, this would create a document in Firebase
            val newApplication = CampaignApplication(
                id = "app_${System.currentTimeMillis()}",
                campaignId = campaignId,
                carOwnerId = "current_user_id", // In a real implementation, get from auth
                carId = carId,
                status = istick.app.beta.model.ApplicationStatus.PENDING,
                appliedAt = System.currentTimeMillis()
            )
            
            // Update local cache and state
            applicationCache[newApplication.id] = newApplication
            _userApplications.value = _userApplications.value + newApplication
            
            Result.success(newApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign> = withContext(Dispatchers.Default) {
        try {
            // Get existing campaign
            val existingCampaign = campaignCache[campaignId] ?: return@withContext Result.failure(
                Exception("Campaign not found")
            )
            
            // Update status
            val updatedCampaign = existingCampaign.copy(
                status = status,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update cache
            campaignCache[campaignId] = updatedCampaign
            
            // Update state
            _activeCampaigns.value = _activeCampaigns.value.map {
                if (it.id == campaignId) updatedCampaign else it
            }
            
            Result.success(updatedCampaign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override fun observeActiveCampaigns(): Flow<List<Campaign>> = callbackFlow {
        try {
            // In a real implementation, this would use Firestore listeners
            // For now, we'll simulate with periodic updates
            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    fetchActiveCampaigns().onSuccess { campaigns ->
                        trySend(campaigns)
                    }
                }
            }, 0, 60000) // Every minute

            awaitClose { timer.cancel() }
        } catch (e: Exception) {
            // Log and emit empty list on error
            println("Error setting up campaign observer: ${e.message}")
            trySend(emptyList())
        }
    }
}