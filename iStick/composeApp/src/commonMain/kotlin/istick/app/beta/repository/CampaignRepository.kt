// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/CampaignRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignApplication
import istick.app.beta.model.CampaignStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing campaigns
 */
interface CampaignRepository {
    // Observable state flows
    val activeCampaigns: StateFlow<List<Campaign>>
    val userApplications: StateFlow<List<CampaignApplication>>

    // Methods for data operations
    fun observeActiveCampaigns(): Flow<List<Campaign>>
    suspend fun fetchActiveCampaigns(): Result<List<Campaign>>
    suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign>
    suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>>
    suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication>
    suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign>

    // New methods for campaign management
    suspend fun createCampaign(campaign: Campaign): Result<Campaign>
    suspend fun updateCampaign(campaign: Campaign): Result<Campaign>
    suspend fun deleteCampaign(campaignId: String): Result<Boolean>
    suspend fun fetchBrandCampaigns(brandId: String): Result<List<Campaign>>
    suspend fun fetchCampaignApplications(campaignId: String): Result<List<CampaignApplication>>
    suspend fun approveApplication(applicationId: String): Result<CampaignApplication>
    suspend fun rejectApplication(applicationId: String): Result<CampaignApplication>
}