// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/FirebaseCampaignRepository.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignApplication
import istick.app.beta.model.CampaignStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Firebase implementation of CampaignRepository
 * This is a placeholder implementation using MySQL under the hood
 */
class FirebaseCampaignRepository(private val authRepository: AuthRepository) : CampaignRepository {
    // Forward to MySQL implementation
    private val mysqlRepo = MySqlCampaignRepository(authRepository)

    override val activeCampaigns: StateFlow<List<Campaign>> = mysqlRepo.activeCampaigns
    override val userApplications: StateFlow<List<CampaignApplication>> = mysqlRepo.userApplications

    override fun observeActiveCampaigns(): Flow<List<Campaign>> {
        return mysqlRepo.observeActiveCampaigns()
    }

    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> {
        return mysqlRepo.fetchActiveCampaigns()
    }

    override suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign> {
        return mysqlRepo.fetchCampaignDetails(campaignId)
    }

    override suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>> {
        return mysqlRepo.fetchUserApplications(userId)
    }

    override suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication> {
        return mysqlRepo.applyCampaign(campaignId, carId)
    }

    override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign> {
        return mysqlRepo.updateCampaignStatus(campaignId, status)
    }
}