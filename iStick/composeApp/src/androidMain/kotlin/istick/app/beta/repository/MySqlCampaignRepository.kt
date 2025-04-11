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

    /**
     * Observe active campaigns as a Flow
     */
    override fun observeActiveCampaigns(): Flow<List<Campaign>> {
        return activeCampaigns
    }

    /**
     * Fetch active campaigns, ensuring it runs within a coroutine context (Dispatchers.IO for database/network calls)
     */
    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> =
        withContext(Dispatchers.IO) {
            try {
                val campaigns = DatabaseHelper.executeQuery(
                    """
                    SELECT o.*, u.full_name as brand_name 
                    FROM offers o 
                    JOIN users_business u ON o.user_id = u.id 
                    WHERE o.status = 'ACTIVE' 
                    ORDER BY o.created_at DESC
                    """,
                    emptyList()
                ) { resultSet ->
                    val campaignsList = mutableListOf<Campaign>()
                    while (resultSet.next()) {
                        campaignsList.add(resultSetToCampaign(resultSet))
                    }
                    campaignsList
                }

                _activeCampaigns.value = campaigns
                Result.success(campaigns)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching campaigns", e)
                Result.failure(e)
            }
        }

    /**
     * Fetch campaign details
     */
    override suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign> =
        withContext(Dispatchers.IO) {
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
                        resultSetToCampaign(resultSet)
                    } else {
                        null
                    }
                }

                if (campaign != null) {
                    // Fetch additional details like sticker positions, etc.
                    val positions = fetchStickerPositions(campaignId)
                    val cities = fetchTargetCities(campaignId)
                    val carMakes = fetchCarMakes(campaignId)

                    // Create a new campaign with all the details
                    val detailedCampaign = campaign.copy(
                        stickerDetails = campaign.stickerDetails.copy(
                            positions = positions
                        ),
                        requirements = campaign.requirements.copy(
                            cities = cities,
                            carMakes = carMakes
                        )
                    )

                    Result.success(detailedCampaign)
                } else {
                    Result.failure(Exception("Campaign not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching campaign details", e)
                Result.failure(e)
            }
        }

    /**
     * Fetch user applications
     */
    override suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>> =
        withContext(Dispatchers.IO) {
            try {
                val applications = DatabaseHelper.executeQuery(
                    """
                    SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model
                    FROM applications a
                    JOIN offers o ON a.offer_id = o.id
                    JOIN cars c ON a.car_id = c.id
                    WHERE a.user_id = ?
                    ORDER BY a.created_at DESC
                    """,
                    listOf(userId.toLong())
                ) { resultSet ->
                    val applicationsList = mutableListOf<CampaignApplication>()
                    while (resultSet.next()) {
                        applicationsList.add(resultSetToApplication(resultSet))
                    }
                    applicationsList
                }

                _userApplications.value = applications
                Result.success(applications)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user applications", e)
                Result.failure(e)
            }
        }

    /**
     * Apply to a campaign
     */
    override suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication> =
        withContext(Dispatchers.IO) {
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not logged in"))

                // Check if user has already applied to this campaign with this car
                val existingApplication = DatabaseHelper.executeQuery(
                    """
                    SELECT id FROM applications
                    WHERE offer_id = ? AND car_id = ? AND user_id = ?
                    """,
                    listOf(campaignId.toLong(), carId.toLong(), userId.toLong())
                ) { resultSet ->
                    if (resultSet.next()) resultSet.getLong("id").toString() else null
                }

                if (existingApplication != null) {
                    return@withContext Result.failure(Exception("You have already applied to this campaign with this car"))
                }

                // Insert new application
                val applicationId = DatabaseHelper.executeInsert(
                    """
                    INSERT INTO applications (
                        offer_id, car_id, user_id, status, created_at, updated_at
                    ) VALUES (?, ?, ?, 'PENDING', NOW(), NOW())
                    """,
                    listOf(campaignId.toLong(), carId.toLong(), userId.toLong())
                )

                if (applicationId > 0) {
                    // Fetch the new application details
                    val application = DatabaseHelper.executeQuery(
                        """
                        SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model
                        FROM applications a
                        JOIN offers o ON a.offer_id = o.id
                        JOIN cars c ON a.car_id = c.id
                        WHERE a.id = ?
                        """,
                        listOf(applicationId)
                    ) { resultSet ->
                        if (resultSet.next()) {
                            resultSetToApplication(resultSet)
                        } else {
                            null
                        }
                    }

                    if (application != null) {
                        // Update the applications state flow with the new application
                        _userApplications.value = _userApplications.value + application
                        Result.success(application)
                    } else {
                        Result.failure(Exception("Failed to retrieve created application"))
                    }
                } else {
                    Result.failure(Exception("Failed to create application"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying to campaign", e)
                Result.failure(e)
            }
        }

    /**
     * Update campaign status
     */
    override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign> =
        withContext(Dispatchers.IO) {
            try {
                val rowsUpdated = DatabaseHelper.executeUpdate(
                    """
                    UPDATE offers
                    SET status = ?, updated_at = NOW()
                    WHERE id = ?
                    """,
                    listOf(status.name, campaignId.toLong())
                )

                if (rowsUpdated > 0) {
                    // Fetch the updated campaign
                    fetchCampaignDetails(campaignId)
                } else {
                    Result.failure(Exception("Failed to update campaign status"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating campaign status", e)
                Result.failure(e)
            }
        }

    /**
     * Create a new campaign
     */
    override suspend fun createCampaign(campaign: Campaign): Result<Campaign> =
        withContext(Dispatchers.IO) {
            try {
                val brandId = authRepository.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not logged in"))

                // Start a transaction
                val connection = DatabaseHelper.beginTransaction()
                try {
                    // Insert campaign record
                    val campaignId = DatabaseHelper.executeInsertWithConnection(
                        connection,
                        """
                        INSERT INTO offers (
                            user_id, title, description, price, currency, 
                            sticker_width, sticker_height, sticker_position, sticker_image_url,
                            delivery_method, min_daily_distance, status, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                        listOf(
                            brandId.toLong(),
                            campaign.title,
                            campaign.description,
                            campaign.payment.amount,
                            campaign.payment.currency,
                            campaign.stickerDetails.width,
                            campaign.stickerDetails.height,
                            if (campaign.stickerDetails.positions.isNotEmpty())
                                campaign.stickerDetails.positions.first().name else "DOOR_LEFT",
                            campaign.stickerDetails.imageUrl,
                            campaign.stickerDetails.deliveryMethod.name,
                            campaign.requirements.minDailyDistance,
                            campaign.status.name
                        )
                    )

                    if (campaignId <= 0) {
                        DatabaseHelper.rollbackTransaction(connection)
                        return@withContext Result.failure(Exception("Failed to create campaign"))
                    }

                    // Insert sticker positions
                    campaign.stickerDetails.positions.forEach { position ->
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            INSERT INTO offer_positions (
                                offer_id, position_name
                            ) VALUES (?, ?)
                            """,
                            listOf(campaignId, position.name)
                        )
                    }

                    // Insert target cities
                    campaign.requirements.cities.forEach { city ->
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            INSERT INTO offer_cities (
                                offer_id, city_name
                            ) VALUES (?, ?)
                            """,
                            listOf(campaignId, city)
                        )
                    }

                    // Insert car makes requirements
                    campaign.requirements.carMakes.forEach { make ->
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            INSERT INTO offer_car_makes (
                                offer_id, make_name
                            ) VALUES (?, ?)
                            """,
                            listOf(campaignId, make)
                        )
                    }

                    DatabaseHelper.commitTransaction(connection)

                    // Fetch the created campaign details
                    val createdCampaign = fetchCampaignDetails(campaignId.toString()).getOrNull()
                        ?: return@withContext Result.failure(Exception("Failed to retrieve created campaign"))

                    Result.success(createdCampaign)
                } catch (e: Exception) {
                    DatabaseHelper.rollbackTransaction(connection)
                    throw e
                } finally {
                    DatabaseHelper.closeConnection(connection)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating campaign", e)
                Result.failure(e)
            }
        }

    /**
     * Update an existing campaign
     */
    override suspend fun updateCampaign(campaign: Campaign): Result<Campaign> =
        withContext(Dispatchers.IO) {
            try {
                // Start a transaction
                val connection = DatabaseHelper.beginTransaction()
                try {
                    // Update main campaign record
                    val rowsUpdated = DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        """
                        UPDATE offers
                        SET title = ?, description = ?, price = ?, currency = ?,
                            sticker_width = ?, sticker_height = ?, sticker_image_url = ?,
                            delivery_method = ?, min_daily_distance = ?, status = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                        listOf(
                            campaign.title,
                            campaign.description,
                            campaign.payment.amount,
                            campaign.payment.currency,
                            campaign.stickerDetails.width,
                            campaign.stickerDetails.height,
                            campaign.stickerDetails.imageUrl,
                            campaign.stickerDetails.deliveryMethod.name,
                            campaign.requirements.minDailyDistance,
                            campaign.status.name,
                            campaign.id.toLong()
                        )
                    )

                    if (rowsUpdated <= 0) {
                        DatabaseHelper.rollbackTransaction(connection)
                        return@withContext Result.failure(Exception("Failed to update campaign"))
                    }

                    // Clear existing related data
                    DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        "DELETE FROM offer_positions WHERE offer_id = ?",
                        listOf(campaign.id.toLong())
                    )

                    DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        "DELETE FROM offer_cities WHERE offer_id = ?",
                        listOf(campaign.id.toLong())
                    )

                    DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        "DELETE FROM offer_car_makes WHERE offer_id = ?",
                        listOf(campaign.id.toLong())
                    )

                    // Insert updated sticker positions
                    campaign.stickerDetails.positions.forEach { position ->
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            INSERT INTO offer_positions (
                                offer_id, position_name
                            ) VALUES (?, ?)
                            """,
                            listOf(campaign.id.toLong(), position.name)
                        )
                    }

                    // Insert updated target cities
                    campaign.requirements.cities.forEach { city ->
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            INSERT INTO offer_cities (
                                offer_id, city_name
                            ) VALUES (?, ?)
                            """,
                            listOf(campaign.id.toLong(), city)
                        )
                    }

                    // Insert updated car makes requirements
                    campaign.requirements.carMakes.forEach { make ->
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            INSERT INTO offer_car_makes (
                                offer_id, make_name
                            ) VALUES (?, ?)
                            """,
                            listOf(campaign.id.toLong(), make)
                        )
                    }

                    DatabaseHelper.commitTransaction(connection)

                    // Fetch the updated campaign details
                    val updatedCampaign = fetchCampaignDetails(campaign.id).getOrNull()
                        ?: return@withContext Result.failure(Exception("Failed to retrieve updated campaign"))

                    Result.success(updatedCampaign)
                } catch (e: Exception) {
                    DatabaseHelper.rollbackTransaction(connection)
                    throw e
                } finally {
                    DatabaseHelper.closeConnection(connection)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating campaign", e)
                Result.failure(e)
            }
        }

    /**
     * Delete a campaign
     */
    override suspend fun deleteCampaign(campaignId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // Start a transaction
                val connection = DatabaseHelper.beginTransaction()
                try {
                    // Delete related records first
                    DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        "DELETE FROM offer_positions WHERE offer_id = ?",
                        listOf(campaignId.toLong())
                    )

                    DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        "DELETE FROM offer_cities WHERE offer_id = ?",
                        listOf(campaignId.toLong())
                    )

                    DatabaseHelper.executeUpdateWithConnection(
                        connection,
                        "DELETE FROM offer_car_makes WHERE offer_id = ?",
                        listOf(campaignId.toLong())
                    )

                    // Check for applications
                    val applicationCount = DatabaseHelper.executeQueryWithConnection(
                        connection,
                        "SELECT COUNT(*) FROM applications WHERE offer_id = ?",
                        listOf(campaignId.toLong())
                    ) { resultSet ->
                        if (resultSet.next()) resultSet.getInt(1) else 0
                    }

                    if (applicationCount > 0) {
                        // If there are applications, just mark as deleted
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            """
                            UPDATE offers
                            SET status = 'CANCELLED', updated_at = NOW()
                            WHERE id = ?
                            """,
                            listOf(campaignId.toLong())
                        )
                    } else {
                        // Otherwise, fully delete the campaign
                        DatabaseHelper.executeUpdateWithConnection(
                            connection,
                            "DELETE FROM offers WHERE id = ?",
                            listOf(campaignId.toLong())
                        )
                    }

                    DatabaseHelper.commitTransaction(connection)
                    Result.success(true)
                } catch (e: Exception) {
                    DatabaseHelper.rollbackTransaction(connection)
                    throw e
                } finally {
                    DatabaseHelper.closeConnection(connection)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting campaign", e)
                Result.failure(e)
            }
        }

    /**
     * Fetch campaigns for a specific brand
     */
    override suspend fun fetchBrandCampaigns(brandId: String): Result<List<Campaign>> =
        withContext(Dispatchers.IO) {
            try {
                val campaigns = DatabaseHelper.executeQuery(
                    """
                    SELECT o.*, u.full_name as brand_name 
                    FROM offers o 
                    JOIN users_business u ON o.user_id = u.id 
                    WHERE o.user_id = ? 
                    ORDER BY o.created_at DESC
                    """,
                    listOf(brandId.toLong())
                ) { resultSet ->
                    val campaignsList = mutableListOf<Campaign>()
                    while (resultSet.next()) {
                        campaignsList.add(resultSetToCampaign(resultSet))
                    }
                    campaignsList
                }

                Result.success(campaigns)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching brand campaigns", e)
                Result.failure(e)
            }
        }

    /**
     * Fetch applications for a specific campaign
     */
    override suspend fun fetchCampaignApplications(campaignId: String): Result<List<CampaignApplication>> =
        withContext(Dispatchers.IO) {
            try {
                val applications = DatabaseHelper.executeQuery(
                    """
                    SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model,
                           u.full_name as user_name
                    FROM applications a
                    JOIN offers o ON a.offer_id = o.id
                    JOIN cars c ON a.car_id = c.id
                    JOIN users_drivers u ON a.user_id = u.user_id
                    WHERE a.offer_id = ?
                    ORDER BY a.created_at DESC
                    """,
                    listOf(campaignId.toLong())
                ) { resultSet ->
                    val applicationsList = mutableListOf<CampaignApplication>()
                    while (resultSet.next()) {
                        applicationsList.add(resultSetToApplication(resultSet))
                    }
                    applicationsList
                }

                Result.success(applications)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching campaign applications", e)
                Result.failure(e)
            }
        }

    /**
     * Approve an application
     */
    override suspend fun approveApplication(applicationId: String): Result<CampaignApplication> =
        withContext(Dispatchers.IO) {
            try {
                val rowsUpdated = DatabaseHelper.executeUpdate(
                    """
                    UPDATE applications
                    SET status = 'APPROVED', updated_at = NOW()
                    WHERE id = ?
                    """,
                    listOf(applicationId.toLong())
                )

                if (rowsUpdated > 0) {
                    // Fetch the updated application
                    val application = DatabaseHelper.executeQuery(
                        """
                        SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model,
                               u.full_name as user_name
                        FROM applications a
                        JOIN offers o ON a.offer_id = o.id
                        JOIN cars c ON a.car_id = c.id
                        JOIN users_drivers u ON a.user_id = u.user_id
                        WHERE a.id = ?
                        """,
                        listOf(applicationId.toLong())
                    ) { resultSet ->
                        if (resultSet.next()) {
                            resultSetToApplication(resultSet)
                        } else {
                            null
                        }
                    }

                    if (application != null) {
                        Result.success(application)
                    } else {
                        Result.failure(Exception("Failed to retrieve updated application"))
                    }
                } else {
                    Result.failure(Exception("Failed to approve application"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error approving application", e)
                Result.failure(e)
            }
        }

    /**
     * Reject an application
     */
    override suspend fun rejectApplication(applicationId: String): Result<CampaignApplication> =
        withContext(Dispatchers.IO) {
            try {
                val rowsUpdated = DatabaseHelper.executeUpdate(
                    """
                    UPDATE applications
                    SET status = 'REJECTED', updated_at = NOW()
                    WHERE id = ?
                    """,
                    listOf(applicationId.toLong())
                )

                if (rowsUpdated > 0) {
                    // Fetch the updated application
                    val application = DatabaseHelper.executeQuery(
                        """
                        SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model,
                               u.full_name as user_name
                        FROM applications a
                        JOIN offers o ON a.offer_id = o.id
                        JOIN cars c ON a.car_id = c.id
                        JOIN users_drivers u ON a.user_id = u.user_id
                        WHERE a.id = ?
                        """,
                        listOf(applicationId.toLong())
                    ) { resultSet ->
                        if (resultSet.next()) {
                            resultSetToApplication(resultSet)
                        } else {
                            null
                        }
                    }

                    if (application != null) {
                        Result.success(application)
                    } else {
                        Result.failure(Exception("Failed to retrieve updated application"))
                    }
                } else {
                    Result.failure(Exception("Failed to reject application"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting application", e)
                Result.failure(e)
            }
        }

    /**
     * Fetch sticker positions for a campaign
     */
    private suspend fun fetchStickerPositions(campaignId: String): List<StickerPosition> =
        withContext(Dispatchers.IO) {
            try {
                DatabaseHelper.executeQuery(
                    """
                    SELECT position_name FROM offer_positions
                    WHERE offer_id = ?
                    """,
                    listOf(campaignId.toLong())
                ) { resultSet ->
                    val positions = mutableListOf<StickerPosition>()
                    while (resultSet.next()) {
                        val positionName = resultSet.getString("position_name")
                        try {
                            positions.add(StickerPosition.valueOf(positionName))
                        } catch (e: IllegalArgumentException) {
                            // Ignore invalid position names
                        }
                    }
                    positions
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sticker positions", e)
                emptyList()
            }
        }

    /**
     * Fetch target cities for a campaign
     */
    private suspend fun fetchTargetCities(campaignId: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                DatabaseHelper.executeQuery(
                    """
                    SELECT city_name FROM offer_cities
                    WHERE offer_id = ?
                    """,
                    listOf(campaignId.toLong())
                ) { resultSet ->
                    val cities = mutableListOf<String>()
                    while (resultSet.next()) {
                        cities.add(resultSet.getString("city_name"))
                    }
                    cities
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching target cities", e)
                emptyList()
            }
        }

    /**
     * Fetch car makes for a campaign
     */
    private suspend fun fetchCarMakes(campaignId: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                DatabaseHelper.executeQuery(
                    """
                    SELECT make_name FROM offer_car_makes
                    WHERE offer_id = ?
                    """,
                    listOf(campaignId.toLong())
                ) { resultSet ->
                    val makes = mutableListOf<String>()
                    while (resultSet.next()) {
                        makes.add(resultSet.getString("make_name"))
                    }
                    makes
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching car makes", e)
                emptyList()
            }
        }

    /**
     * Convert a ResultSet row to a Campaign object
     */
    private fun resultSetToCampaign(resultSet: java.sql.ResultSet): Campaign {
        val id = resultSet.getLong("id").toString()
        val brandId = resultSet.getLong("user_id").toString()
        val brandName = resultSet.getString("brand_name") ?: ""
        val title = resultSet.getString("title") ?: ""
        val description = resultSet.getString("description") ?: ""
        val price = resultSet.getDouble("price")
        val currency = resultSet.getString("currency") ?: "RON"
        val stickerWidth = resultSet.getInt("sticker_width")
        val stickerHeight = resultSet.getInt("sticker_height")
        val stickerPositionStr = resultSet.getString("sticker_position") ?: "DOOR_LEFT"
        val stickerImageUrl = resultSet.getString("sticker_image_url") ?: ""
        val deliveryMethodStr = resultSet.getString("delivery_method") ?: "CENTER"
        val minDailyDistance = resultSet.getInt("min_daily_distance")
        val statusStr = resultSet.getString("status") ?: "ACTIVE"
        val createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
        val updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()

        // Parse enums
        val deliveryMethod = try {
            DeliveryMethod.valueOf(deliveryMethodStr)
        } catch (e: IllegalArgumentException) {
            DeliveryMethod.CENTER
        }

        val status = try {
            CampaignStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
            CampaignStatus.ACTIVE
        }

        // For basic listing, we include the default position from the main table
        // The detailed view will override this with the full list from the positions table
        val position = try {
            StickerPosition.valueOf(stickerPositionStr)
        } catch (e: IllegalArgumentException) {
            StickerPosition.DOOR_LEFT
        }

        return Campaign(
            id = id,
            brandId = brandId,
            title = "$title by $brandName",
            description = description,
            stickerDetails = StickerDetails(
                imageUrl = stickerImageUrl,
                width = stickerWidth,
                height = stickerHeight,
                positions = listOf(position),
                deliveryMethod = deliveryMethod
            ),
            payment = PaymentDetails(
                amount = price,
                currency = currency,
                paymentFrequency = PaymentFrequency.MONTHLY
            ),
            requirements = CampaignRequirements(
                minDailyDistance = minDailyDistance
            ),
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Convert a ResultSet row to a CampaignApplication object
     */
    private fun resultSetToApplication(resultSet: java.sql.ResultSet): CampaignApplication {
        val id = resultSet.getLong("id").toString()
        val campaignId = resultSet.getLong("offer_id").toString()
        val carOwnerId = resultSet.getLong("user_id").toString()
        val carId = resultSet.getLong("car_id").toString()
        val statusStr = resultSet.getString("status") ?: "PENDING"
        val appliedAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
        val updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()
        val notes = resultSet.getString("notes") ?: ""

        // Additional info from joins
        val campaignTitle = resultSet.getString("campaign_title") ?: ""
        val carMake = resultSet.getString("car_make") ?: ""
        val carModel = resultSet.getString("car_model") ?: ""

        // Parse enum
        val status = try {
            ApplicationStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
            ApplicationStatus.PENDING
        }

        return CampaignApplication(
            id = id,
            campaignId = campaignId,
            carOwnerId = carOwnerId,
            carId = carId,
            status = status,
            appliedAt = appliedAt,
            updatedAt = updatedAt,
            notes = "$campaignTitle - $carMake $carModel\n$notes"
        )
    }
}