// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/network/ApiClient.kt
package istick.app.beta.network

import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignApplication
import istick.app.beta.model.User

/**
 * API client interface for network operations
 */
interface ApiClient {
    /**
     * Get campaigns with pagination
     */
    suspend fun getCampaigns(page: Int, pageSize: Int): NetworkResult<List<Campaign>>
    
    /**
     * Get detailed information about a specific campaign
     */
    suspend fun getCampaignDetails(campaignId: String): NetworkResult<Campaign>
    
    /**
     * Apply for a campaign
     */
    suspend fun applyCampaign(campaignId: String, carId: String): NetworkResult<CampaignApplication>
    
    /**
     * Get user profile
     */
    suspend fun getUserProfile(userId: String): NetworkResult<User>
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(user: User): NetworkResult<User>
    
    /**
     * Upload image and get URL
     */
    suspend fun uploadImage(imageBytes: ByteArray, fileName: String): NetworkResult<String>
    
    /**
     * Get applications for a user
     */
    suspend fun getUserApplications(userId: String): NetworkResult<List<CampaignApplication>>
}