// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/repository/MySqlCampaignRepository.kt
package istick.app.beta.repository

import android.util.Log
import istick.app.beta.auth.AuthRepository
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * MySQL implementation of CampaignRepository
 */
class MySqlCampaignRepository(private val authRepository: AuthRepository) : CampaignRepository {
    private val TAG = "MySqlCampaignRepository"

    private val _activeCampaigns = MutableStateFlow<List<Campaign>>(emptyList())
    override val activeCampaigns: StateFlow<List<Campaign>> = _activeCampaigns

    private val _userApplications = MutableStateFlow<List<CampaignApplication>>(emptyList())
    override val userApplications: StateFlow<List<CampaignApplication>> = _userApplications

    override fun observeActiveCampaigns(): Flow<List<Campaign>> {
        return activeCampaigns
    }

    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> = withContext(Dispatchers.IO) {
        try {
            val campaigns = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.status = 'verified' 
                ORDER BY o.created_at DESC
                """,
                emptyList()
            ) { resultSet ->
                val offersList = mutableListOf<Campaign>()

                while (resultSet.next()) {
                    // Convert from database schema to Campaign model
                    val campaignId = resultSet.getLong("id").toString()
                    val campaign = Campaign(
                        id = campaignId,
                        brandId = resultSet.getLong("user_id").toString(),
                        title = "${resultSet.getString("title")} by ${resultSet.getString("brand_name")}",
                        description = resultSet.getString("description") ?: "",
                        stickerDetails = StickerDetails(
                            imageUrl = resultSet.getString("sticker_image_url") ?: "",
                            width = resultSet.getInt("sticker_width"),
                            height = resultSet.getInt("sticker_height"),
                            positions = parsePositions(resultSet.getString("sticker_position") ?: ""),
                            deliveryMethod = DeliveryMethod.CENTER // Default or from DB
                        ),
                        payment = PaymentDetails(
                            amount = resultSet.getDouble("price"),
                            currency = resultSet.getString("currency") ?: "RON"
                        ),
                        requirements = CampaignRequirements(
                            minDailyDistance = resultSet.getInt("min_daily_distance"),
                            cities = listOf(resultSet.getString("location") ?: "")
                        ),
                        status = CampaignStatus.ACTIVE, // Default for "verified" in DB
                        createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                        updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()
                    )

                    offersList.add(campaign)
                }

                offersList
            }

            _activeCampaigns.value = campaigns
            return@withContext Result.success(campaigns)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active campaigns: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign> = withContext(Dispatchers.IO) {
        try {
            val campaign = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.id = ?
                """,
                listOf(campaignId.toLong())
            ) { resultSet ->
                if (resultSet.next()) {
                    // Get campaign benefits - execute this query in the same coroutine context
                    val benefits = DatabaseHelper.executeQuery(
                        "SELECT benefit_text FROM offer_benefits WHERE offer_id = ?",
                        listOf(campaignId.toLong())
                    ) { benefitResultSet ->
                        val benefitsList = mutableListOf<String>()
                        while (benefitResultSet.next()) {
                            benefitsList.add(benefitResultSet.getString("benefit_text"))
                        }
                        benefitsList
                    }

                    // Convert from database schema to Campaign model
                    Campaign(
                        id = campaignId,
                        brandId = resultSet.getLong("user_id").toString(),
                        title = "${resultSet.getString("title")} by ${resultSet.getString("brand_name")}",
                        description = resultSet.getString("description") ?: "",
                        stickerDetails = StickerDetails(
                            imageUrl = resultSet.getString("sticker_image_url") ?: "",
                            width = resultSet.getInt("sticker_width"),
                            height = resultSet.getInt("sticker_height"),
                            positions = parsePositions(resultSet.getString("sticker_position") ?: ""),
                            deliveryMethod = DeliveryMethod.CENTER // Default or from DB
                        ),
                        payment = PaymentDetails(
                            amount = resultSet.getDouble("price"),
                            currency = resultSet.getString("currency") ?: "RON"
                        ),
                        requirements = CampaignRequirements(
                            minDailyDistance = resultSet.getInt("min_daily_distance"),
                            cities = benefits.ifEmpty { listOf(resultSet.getString("location") ?: "") }
                        ),
                        status = CampaignStatus.ACTIVE, // Default for "verified" in DB
                        createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                        updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
                        applicants = getApplicantsForCampaign(campaignId),
                        approvedApplicants = getApprovedApplicantsForCampaign(campaignId)
                    )
                } else {
                    null
                }
            }

            if (campaign != null) {
                return@withContext Result.success(campaign)
            } else {
                return@withContext Result.failure(Exception("Campaign not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaign details: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>> = withContext(Dispatchers.IO) {
        try {
            val applications = DatabaseHelper.executeQuery(
                """
                SELECT * FROM driver_applications
                WHERE driver_id = ?
                ORDER BY created_at DESC
                """,
                listOf(userId.toLong())
            ) { resultSet ->
                val appList = mutableListOf<CampaignApplication>()

                while (resultSet.next()) {
                    appList.add(
                        CampaignApplication(
                            id = resultSet.getLong("id").toString(),
                            campaignId = resultSet.getLong("offer_id").toString(),
                            carOwnerId = userId,
                            carId = resultSet.getLong("car_id").toString(),
                            status = parseApplicationStatus(resultSet.getString("status") ?: "pending"),
                            appliedAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                            updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
                            notes = resultSet.getString("notes") ?: ""
                        )
                    )
                }

                appList
            }

            _userApplications.value = applications
            return@withContext Result.success(applications)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user applications: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Check if application already exists
            val existingApp = DatabaseHelper.executeQuery(
                """
                SELECT id FROM driver_applications
                WHERE driver_id = ? AND offer_id = ?
                """,
                listOf(userId.toLong(), campaignId.toLong())
            ) { resultSet ->
                if (resultSet.next()) resultSet.getLong("id").toString() else null
            }

            if (existingApp != null) {
                return@withContext Result.failure(Exception("You have already applied to this campaign"))
            }

            // Insert application
            val applicationId = DatabaseHelper.executeInsert(
                """
                INSERT INTO driver_applications
                (driver_id, offer_id, car_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'pending', NOW(), NOW())
                """,
                listOf(userId.toLong(), campaignId.toLong(), carId.toLong())
            )

            if (applicationId > 0) {
                // Return the new application
                val application = CampaignApplication(
                    id = applicationId.toString(),
                    campaignId = campaignId,
                    carOwnerId = userId,
                    carId = carId,
                    status = ApplicationStatus.PENDING,
                    appliedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                return@withContext Result.success(application)
            } else {
                return@withContext Result.failure(Exception("Failed to apply to campaign"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying to campaign: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign> = withContext(Dispatchers.IO) {
        try {
            // Map our status to the database status
            val dbStatus = when(status) {
                CampaignStatus.ACTIVE -> "verified"
                CampaignStatus.PAUSED -> "paused"
                CampaignStatus.COMPLETED -> "completed"
                CampaignStatus.CANCELLED -> "cancelled"
                else -> "draft"
            }

            // Update status
            val result = DatabaseHelper.executeUpdate(
                """
                UPDATE offers
                SET status = ?, updated_at = NOW()
                WHERE id = ?
                """,
                listOf(dbStatus, campaignId.toLong())
            )

            if (result > 0) {
                // Fetch updated campaign
                return@withContext fetchCampaignDetails(campaignId)
            } else {
                return@withContext Result.failure(Exception("Failed to update campaign status"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating campaign status: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

// Update the helper methods in MySqlCampaignRepository.kt

    // Helper method to get campaign applicants using runBlocking
    private fun getApplicantsForCampaign(campaignId: String): List<String> {
        return runBlocking(Dispatchers.IO) {
            getApplicantsForCampaignSuspend(campaignId)
        }
    }

    // Suspend version of the helper method
    private suspend fun getApplicantsForCampaignSuspend(campaignId: String): List<String> {
        return DatabaseHelper.executeQuery(
            "SELECT DISTINCT driver_id FROM driver_applications WHERE offer_id = ?",
            listOf(campaignId.toLong())
        ) { resultSet ->
            val applicants = mutableListOf<String>()
            while (resultSet.next()) {
                applicants.add(resultSet.getLong("driver_id").toString())
            }
            applicants
        }
    }

    // Helper method to get approved applicants using runBlocking
    private fun getApprovedApplicantsForCampaign(campaignId: String): List<String> {
        return runBlocking(Dispatchers.IO) {
            getApprovedApplicantsForCampaignSuspend(campaignId)
        }
    }

    // Suspend version of the helper method
    private suspend fun getApprovedApplicantsForCampaignSuspend(campaignId: String): List<String> {
        return DatabaseHelper.executeQuery(
            """
        SELECT DISTINCT driver_id 
        FROM driver_applications 
        WHERE offer_id = ? AND status = 'approved'
        """,
            listOf(campaignId.toLong())
        ) { resultSet ->
            val approved = mutableListOf<String>()
            while (resultSet.next()) {
                approved.add(resultSet.getLong("driver_id").toString())
            }
            approved
        }
    }

    // Helper to parse sticker positions from database
    private fun parsePositions(positionString: String): List<StickerPosition> {
        return when(positionString.lowercase()) {
            "left-door-front" -> listOf(StickerPosition.DOOR_LEFT)
            "right-door-front" -> listOf(StickerPosition.DOOR_RIGHT)
            "hood" -> listOf(StickerPosition.HOOD)
            "trunk" -> listOf(StickerPosition.TRUNK)
            "rear-window" -> listOf(StickerPosition.REAR_WINDOW)
            "side-panel" -> listOf(StickerPosition.SIDE_PANEL)
            // Include other mappings as needed
            else -> listOf(StickerPosition.SIDE_PANEL) // Default
        }
    }

    // Helper to parse application status from database
    private fun parseApplicationStatus(statusString: String): ApplicationStatus {
        return when(statusString.lowercase()) {
            "approved" -> ApplicationStatus.APPROVED
            "rejected" -> ApplicationStatus.REJECTED
            "completed" -> ApplicationStatus.COMPLETED
            else -> ApplicationStatus.PENDING
        }
    }
}