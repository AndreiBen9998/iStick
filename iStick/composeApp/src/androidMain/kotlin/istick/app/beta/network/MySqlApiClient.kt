// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/network/MySqlApiClient.kt
package istick.app.beta.network

import android.util.Log
import istick.app.beta.auth.AuthRepository
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import istick.app.beta.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of ApiClient that uses MySQL database
 */
class MySqlApiClient(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ApiClient {
    private val TAG = "MySqlApiClient"

    override suspend fun getCampaigns(page: Int, pageSize: Int): NetworkResult<List<Campaign>> = withContext(Dispatchers.IO) {
        try {
            val offset = page * pageSize

            val campaigns = DatabaseHelper.executeQuery(
                """
                SELECT c.*, p.amount, p.currency, p.payment_frequency, p.payment_method,
                       u.company_name as brand_name
                FROM campaigns c
                LEFT JOIN campaign_payment p ON c.id = p.campaign_id
                LEFT JOIN users_business u ON c.brand_id = u.id
                WHERE c.status = 'ACTIVE'
                ORDER BY c.created_at DESC
                LIMIT ? OFFSET ?
                """,
                listOf<Any>(pageSize, offset)
            ) { rs ->
                val campaignList = mutableListOf<Campaign>()

                while (rs.next()) {
                    val campaignId = rs.getString("id")
                    val brandName = rs.getString("brand_name") ?: "Unknown Brand"

                    val campaign = Campaign(
                        id = campaignId,
                        brandId = rs.getString("brand_id"),
                        title = "${rs.getString("title")} by $brandName",
                        description = rs.getString("description") ?: "",
                        stickerDetails = StickerDetails(
                            imageUrl = rs.getString("sticker_image_url") ?: "",
                            width = rs.getInt("sticker_width"),
                            height = rs.getInt("sticker_height")
                        ),
                        payment = PaymentDetails(
                            amount = rs.getDouble("amount"),
                            currency = rs.getString("currency") ?: "RON",
                            paymentFrequency = try {
                                PaymentFrequency.valueOf(rs.getString("payment_frequency") ?: "MONTHLY")
                            } catch (e: Exception) {
                                PaymentFrequency.MONTHLY
                            },
                            paymentMethod = try {
                                PaymentMethod.valueOf(rs.getString("payment_method") ?: "BANK_TRANSFER")
                            } catch (e: Exception) {
                                PaymentMethod.BANK_TRANSFER
                            }
                        ),
                        requirements = CampaignRequirements(
                            minDailyDistance = rs.getInt("min_daily_distance")
                        ),
                        status = try {
                            CampaignStatus.valueOf(rs.getString("status"))
                        } catch (e: Exception) {
                            CampaignStatus.ACTIVE
                        },
                        startDate = rs.getLong("start_date"),
                        endDate = rs.getLong("end_date"),
                        createdAt = rs.getLong("created_at"),
                        updatedAt = rs.getLong("updated_at")
                    )

                    campaignList.add(campaign)
                }

                campaignList
            }

            NetworkResult.Success(campaigns)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaigns", e)
            NetworkResult.Error("Failed to fetch campaigns: ${e.message}")
        }
    }

    override suspend fun getCampaignDetails(campaignId: String): NetworkResult<Campaign> = withContext(Dispatchers.IO) {
        try {
            val campaign = DatabaseHelper.executeQuery(
                """
                SELECT c.*, p.amount, p.currency, p.payment_frequency, p.payment_method,
                       u.company_name as brand_name
                FROM campaigns c
                LEFT JOIN campaign_payment p ON c.id = p.campaign_id
                LEFT JOIN users_business u ON c.brand_id = u.id
                WHERE c.id = ?
                """,
                listOf<Any>(campaignId)
            ) { rs ->
                if (rs.next()) {
                    val brandName = rs.getString("brand_name") ?: "Unknown Brand"
                    val cId = rs.getString("id")

                    // Fetch the required data first and then create the Campaign object
                    val stickerPositions = getStickerPositionsSync(cId)
                    val cities = getTargetCitiesSync(cId)
                    val carMakes = getTargetCarMakesSync(cId)

                    Campaign(
                        id = cId,
                        brandId = rs.getString("brand_id"),
                        title = "${rs.getString("title")} by $brandName",
                        description = rs.getString("description") ?: "",
                        stickerDetails = StickerDetails(
                            imageUrl = rs.getString("sticker_image_url") ?: "",
                            width = rs.getInt("sticker_width"),
                            height = rs.getInt("sticker_height"),
                            positions = stickerPositions,
                            deliveryMethod = try {
                                DeliveryMethod.valueOf(rs.getString("delivery_method") ?: "CENTER")
                            } catch (e: Exception) {
                                DeliveryMethod.CENTER
                            }
                        ),
                        payment = PaymentDetails(
                            amount = rs.getDouble("amount"),
                            currency = rs.getString("currency") ?: "RON",
                            paymentFrequency = try {
                                PaymentFrequency.valueOf(rs.getString("payment_frequency") ?: "MONTHLY")
                            } catch (e: Exception) {
                                PaymentFrequency.MONTHLY
                            },
                            paymentMethod = try {
                                PaymentMethod.valueOf(rs.getString("payment_method") ?: "BANK_TRANSFER")
                            } catch (e: Exception) {
                                PaymentMethod.BANK_TRANSFER
                            }
                        ),
                        requirements = CampaignRequirements(
                            minDailyDistance = rs.getInt("min_daily_distance"),
                            cities = cities,
                            carMakes = carMakes,
                            carYearMin = rs.getObject("car_year_min") as? Int,
                            carYearMax = rs.getObject("car_year_max") as? Int
                        ),
                        status = try {
                            CampaignStatus.valueOf(rs.getString("status"))
                        } catch (e: Exception) {
                            CampaignStatus.ACTIVE
                        },
                        startDate = rs.getLong("start_date"),
                        endDate = rs.getLong("end_date"),
                        createdAt = rs.getLong("created_at"),
                        updatedAt = rs.getLong("updated_at")
                    )
                } else null
            } ?: throw Exception("Campaign not found")

            NetworkResult.Success(campaign)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaign details", e)
            NetworkResult.Error("Failed to fetch campaign details: ${e.message}")
        }
    }

    override suspend fun applyCampaign(campaignId: String, carId: String): NetworkResult<CampaignApplication> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId() ?: throw Exception("User not logged in")

            // Check if already applied
            val existingApplication = DatabaseHelper.executeQuery(
                "SELECT id FROM applications WHERE user_id = ? AND offer_id = ? AND car_id = ?",
                listOf<Any>(userId, campaignId, carId)
            ) { rs ->
                if (rs.next()) rs.getLong("id") else null
            }

            if (existingApplication != null) {
                return@withContext NetworkResult.Error("You have already applied to this campaign with this car")
            }

            // Create application
            val applicationId = DatabaseHelper.executeInsert(
                """
                INSERT INTO applications (
                    user_id, offer_id, car_id, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'PENDING', NOW(), NOW())
                """,
                listOf<Any>(userId, campaignId, carId)
            )

            if (applicationId <= 0) {
                throw Exception("Failed to create application")
            }

            // Get the application details
            val application = DatabaseHelper.executeQuery(
                """
                SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model
                FROM applications a
                JOIN offers o ON a.offer_id = o.id
                JOIN cars c ON a.car_id = c.id
                WHERE a.id = ?
                """,
                listOf<Any>(applicationId)
            ) { rs ->
                if (rs.next()) {
                    CampaignApplication(
                        id = rs.getString("id"),
                        campaignId = rs.getString("offer_id"),
                        carId = rs.getString("car_id"),
                        carOwnerId = rs.getString("user_id"),
                        status = try {
                            ApplicationStatus.valueOf(rs.getString("status"))
                        } catch (e: Exception) {
                            ApplicationStatus.PENDING
                        },
                        appliedAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                        updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
                        notes = "Applied for ${rs.getString("campaign_title")} with ${rs.getString("car_make")} ${rs.getString("car_model")}"
                    )
                } else null
            } ?: throw Exception("Failed to retrieve application details")

            NetworkResult.Success(application)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying to campaign", e)
            NetworkResult.Error("Failed to apply to campaign: ${e.message}")
        }
    }

    override suspend fun getUserProfile(userId: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            // Check if user is a car owner
            val carOwner = DatabaseHelper.executeQuery(
                """
                SELECT * FROM users_personal WHERE id = ?
                """,
                listOf<Any>(userId)
            ) { rs ->
                if (rs.next()) {
                    CarOwner(
                        id = rs.getString("id"),
                        email = rs.getString("email"),
                        name = rs.getString("full_name"),
                        profilePictureUrl = rs.getString("profile_picture_url") ?: "",
                        city = rs.getString("city") ?: "",
                        dailyDrivingDistance = rs.getInt("daily_driving_distance"),
                        createdAt = rs.getTimestamp("created_at")?.time ?: 0,
                        lastLoginAt = rs.getTimestamp("last_login")?.time ?: 0
                    )
                } else null
            }

            if (carOwner != null) {
                return@withContext NetworkResult.Success(carOwner)
            }

            // Check if user is a brand
            val brand = DatabaseHelper.executeQuery(
                """
                SELECT * FROM users_business WHERE id = ?
                """,
                listOf<Any>(userId)
            ) { rs ->
                if (rs.next()) {
                    Brand(
                        id = rs.getString("id"),
                        email = rs.getString("email"),
                        name = rs.getString("company_name"),
                        profilePictureUrl = rs.getString("profile_picture_url") ?: "",
                        createdAt = rs.getTimestamp("created_at")?.time ?: 0,
                        lastLoginAt = rs.getTimestamp("last_login")?.time ?: 0,
                        companyDetails = CompanyDetails(
                            companyName = rs.getString("company_name"),
                            industry = rs.getString("industry") ?: "",
                            website = rs.getString("website") ?: "",
                            description = rs.getString("description") ?: "",
                            logoUrl = rs.getString("logo_url") ?: ""
                        )
                    )
                } else null
            }

            if (brand != null) {
                return@withContext NetworkResult.Success(brand)
            }

            throw Exception("User not found")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            NetworkResult.Error("Failed to fetch user profile: ${e.message}")
        }
    }

    override suspend fun updateUserProfile(user: User): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            when (user) {
                is CarOwner -> {
                    val rowsUpdated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_personal
                        SET full_name = ?, city = ?, daily_driving_distance = ?, 
                            profile_picture_url = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                        listOf<Any>(
                            user.name,
                            user.city,
                            user.dailyDrivingDistance,
                            user.profilePictureUrl,
                            user.id
                        )
                    )

                    if (rowsUpdated <= 0) {
                        throw Exception("Failed to update user profile")
                    }
                }
                is Brand -> {
                    val rowsUpdated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_business
                        SET company_name = ?, industry = ?, website = ?, 
                            description = ?, profile_picture_url = ?, 
                            logo_url = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                        listOf<Any>(
                            user.companyDetails.companyName,
                            user.companyDetails.industry,
                            user.companyDetails.website,
                            user.companyDetails.description,
                            user.profilePictureUrl,
                            user.companyDetails.logoUrl,
                            user.id
                        )
                    )

                    if (rowsUpdated <= 0) {
                        throw Exception("Failed to update brand profile")
                    }
                }
                else -> throw Exception("Unsupported user type")
            }

            NetworkResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            NetworkResult.Error("Failed to update user profile: ${e.message}")
        }
    }

    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val result = storageRepository.uploadImage(imageBytes, fileName)

            if (result.isSuccess) {
                NetworkResult.Success(result.getOrThrow())
            } else {
                NetworkResult.Error("Failed to upload image: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            NetworkResult.Error("Failed to upload image: ${e.message}")
        }
    }

    override suspend fun getUserApplications(userId: String): NetworkResult<List<CampaignApplication>> = withContext(Dispatchers.IO) {
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
                listOf<Any>(userId)
            ) { rs ->
                val applicationList = mutableListOf<CampaignApplication>()

                while (rs.next()) {
                    applicationList.add(
                        CampaignApplication(
                            id = rs.getString("id"),
                            campaignId = rs.getString("offer_id"),
                            carId = rs.getString("car_id"),
                            carOwnerId = rs.getString("user_id"),
                            status = try {
                                ApplicationStatus.valueOf(rs.getString("status"))
                            } catch (e: Exception) {
                                ApplicationStatus.PENDING
                            },
                            appliedAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
                            notes = "Applied for ${rs.getString("campaign_title")} with ${rs.getString("car_make")} ${rs.getString("car_model")}"
                        )
                    )
                }

                applicationList
            }

            NetworkResult.Success(applications)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user applications", e)
            NetworkResult.Error("Failed to fetch user applications: ${e.message}")
        }
    }

    // Non-suspending helper methods
    private fun getStickerPositionsSync(campaignId: String): List<StickerPosition> {
        try {
            return DatabaseHelper.executeQuery(
                """
                SELECT position_name FROM offer_positions
                WHERE offer_id = ?
                """,
                listOf<Any>(campaignId)
            ) { rs ->
                val positions = mutableListOf<StickerPosition>()

                while (rs.next()) {
                    try {
                        val positionName = rs.getString("position_name")
                        if (positionName != null) {
                            positions.add(StickerPosition.valueOf(positionName))
                        }
                    } catch (e: Exception) {
                        // Skip invalid positions
                    }
                }

                if (positions.isEmpty()) {
                    positions.add(StickerPosition.DOOR_LEFT)
                }

                positions
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sticker positions", e)
            return listOf(StickerPosition.DOOR_LEFT)
        }
    }

    private fun getTargetCitiesSync(campaignId: String): List<String> {
        try {
            val result: List<String> = DatabaseHelper.executeQuery<List<String>>(
                """
                SELECT city_name FROM offer_cities
                WHERE offer_id = ?
                """,
                listOf<Any>(campaignId)
            ) { rs ->
                val cities = mutableListOf<String>()

                while (rs.next()) {
                    // Using safe call with let to only add non-null values
                    rs.getString("city_name")?.let { cities.add(it) }
                }

                cities // Return the list
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching target cities", e)
            return emptyList()
        }
    }

    private fun getTargetCarMakesSync(campaignId: String): List<String> {
        try {
            val result: List<String> = DatabaseHelper.executeQuery<List<String>>(
                """
                SELECT make_name FROM offer_car_makes
                WHERE offer_id = ?
                """,
                listOf<Any>(campaignId)
            ) { rs ->
                val makes = mutableListOf<String>()

                while (rs.next()) {
                    // Using safe call with let to only add non-null values
                    rs.getString("make_name")?.let { makes.add(it) }
                }

                makes // Return the list
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching target car makes", e)
            return emptyList()
        }
    }
}