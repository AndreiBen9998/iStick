// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/network/MySqlApiClient.kt
package istick.app.beta.network

import android.util.Log
import istick.app.beta.auth.AuthRepository
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import istick.app.beta.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * MySQL implementation of ApiClient
 */
class MySqlApiClient(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ApiClient {
    private val TAG = "MySqlApiClient"

    // Cache for active campaigns - private but not overriding interface properties
    private val _activeCampaigns = MutableStateFlow<List<Campaign>>(emptyList())
    private val _userApplications = MutableStateFlow<List<CampaignApplication>>(emptyList())

    // Required implementation of abstract method from ApiClient
    override suspend fun getCampaigns(page: Int, pageSize: Int): NetworkResult<List<Campaign>> = withContext(Dispatchers.IO) {
        try {
            // Find an appropriate position marker in the SQL query
            val offset = (page - 1) * pageSize
            val params = ArrayList<Any>()
            params.add(pageSize)
            params.add(offset)

            val campaigns = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.status = 'ACTIVE' 
                ORDER BY o.created_at DESC
                LIMIT ? OFFSET ?
                """,
                params
            ) { resultSet ->
                val campaignsList = mutableListOf<Campaign>()
                while (resultSet.next()) {
                    campaignsList.add(resultSetToCampaign(resultSet))
                }
                campaignsList
            }

            _activeCampaigns.value = campaigns
            NetworkResult.Success(campaigns)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaigns", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Implementation of getUserProfile method (required by ApiClient interface)
    override suspend fun getUserProfile(userId: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            // Create explicit ArrayList<Any> for parameters
            val params = ArrayList<Any>()
            params.add(userId.toLong())

            // First, check if the user is a car owner in users_personal table
            var user: User? = DatabaseHelper.executeQuery(
                """
                SELECT id, email, full_name as name, profile_picture_url, city, daily_driving_distance
                FROM users_personal
                WHERE id = ?
                """,
                params
            ) { resultSet ->
                if (resultSet.next()) {
                    CarOwner(
                        id = resultSet.getLong("id").toString(),
                        email = resultSet.getString("email") ?: "",
                        name = resultSet.getString("name") ?: "",
                        profilePictureUrl = resultSet.getString("profile_picture_url") ?: "",
                        city = resultSet.getString("city") ?: "",
                        dailyDrivingDistance = resultSet.getInt("daily_driving_distance"),
                        type = UserType.CAR_OWNER
                    )
                } else {
                    null
                }
            }

            // If not found in users_personal, check in users_business
            if (user == null) {
                user = DatabaseHelper.executeQuery(
                    """
                    SELECT id, email, company_name as name, profile_picture_url, 
                           industry, website, description
                    FROM users_business
                    WHERE id = ?
                    """,
                    params
                ) { resultSet ->
                    if (resultSet.next()) {
                        Brand(
                            id = resultSet.getLong("id").toString(),
                            email = resultSet.getString("email") ?: "",
                            name = resultSet.getString("name") ?: "",
                            profilePictureUrl = resultSet.getString("profile_picture_url") ?: "",
                            companyDetails = CompanyDetails(
                                companyName = resultSet.getString("name") ?: "",
                                industry = resultSet.getString("industry") ?: "",
                                website = resultSet.getString("website") ?: "",
                                description = resultSet.getString("description") ?: ""
                            ),
                            type = UserType.BRAND
                        )
                    } else {
                        null
                    }
                }
            }

            if (user != null) {
                NetworkResult.Success(user)
            } else {
                NetworkResult.Error("User not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Implementation of updateUserProfile method (required by ApiClient interface)
    override suspend fun updateUserProfile(user: User): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            when (user) {
                is CarOwner -> {
                    // Create parameters list
                    val params = ArrayList<Any>()
                    params.add(user.email)
                    params.add(user.name)
                    params.add(user.profilePictureUrl ?: "")
                    params.add(user.city)
                    params.add(user.dailyDrivingDistance)
                    params.add(System.currentTimeMillis())
                    params.add(user.id.toLong())

                    // Update car owner in users_personal table
                    val rowsUpdated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_personal 
                        SET email = ?, full_name = ?, profile_picture_url = ?,
                            city = ?, daily_driving_distance = ?, updated_at = ?
                        WHERE id = ?
                        """,
                        params
                    )

                    if (rowsUpdated > 0) {
                        NetworkResult.Success(user)
                    } else {
                        Log.e(TAG, "No rows updated for car owner ID: ${user.id}")
                        NetworkResult.Error("Failed to update user profile")
                    }
                }

                is Brand -> {
                    // Create parameters list
                    val params = ArrayList<Any>()
                    params.add(user.email)
                    params.add(user.name)
                    params.add(user.profilePictureUrl ?: "")
                    params.add(user.companyDetails.industry)
                    params.add(user.companyDetails.website)
                    params.add(user.companyDetails.description)
                    params.add(System.currentTimeMillis())
                    params.add(user.id.toLong())

                    // Update brand in users_business table
                    val rowsUpdated = DatabaseHelper.executeUpdate(
                        """
                        UPDATE users_business 
                        SET email = ?, company_name = ?, profile_picture_url = ?,
                            industry = ?, website = ?, description = ?, updated_at = ?
                        WHERE id = ?
                        """,
                        params
                    )

                    if (rowsUpdated > 0) {
                        NetworkResult.Success(user)
                    } else {
                        Log.e(TAG, "No rows updated for brand ID: ${user.id}")
                        NetworkResult.Error("Failed to update brand profile")
                    }
                }

                else -> {
                    Log.e(TAG, "Unknown user type: ${user::class.java.simpleName}")
                    NetworkResult.Error("Unsupported user type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Implementation of uploadImage method (required by ApiClient interface)
    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            // Use the storage repository to upload the image
            val result = storageRepository.uploadImage(imageBytes, fileName)

            result.fold(
                onSuccess = { url ->
                    NetworkResult.Success(url)
                },
                onFailure = { e ->
                    Log.e(TAG, "Error uploading image", e)
                    NetworkResult.Error(e.message ?: "Unknown error uploading image")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadImage", e)
            NetworkResult.Error(e.message ?: "Unknown error in uploadImage")
        }
    }

    // Implementation of getCampaignDetails method with override keyword
    override suspend fun getCampaignDetails(campaignId: String): NetworkResult<Campaign> = withContext(Dispatchers.IO) {
        try {
            // Create a list with explicit Any type
            val params = ArrayList<Any>()
            params.add(campaignId.toLong())

            val campaign = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.id = ?
                """,
                params
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

                NetworkResult.Success(detailedCampaign)
            } else {
                NetworkResult.Error("Campaign not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaign details", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Implementation of getUserApplications method with override keyword
    override suspend fun getUserApplications(userId: String): NetworkResult<List<CampaignApplication>> = withContext(Dispatchers.IO) {
        try {
            val params = ArrayList<Any>()
            params.add(userId.toLong())

            val applications = DatabaseHelper.executeQuery(
                """
                SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model
                FROM applications a
                JOIN offers o ON a.offer_id = o.id
                JOIN cars c ON a.car_id = c.id
                WHERE a.user_id = ?
                ORDER BY a.created_at DESC
                """,
                params
            ) { resultSet ->
                val applicationsList = mutableListOf<CampaignApplication>()
                while (resultSet.next()) {
                    applicationsList.add(resultSetToApplication(resultSet))
                }
                applicationsList
            }

            _userApplications.value = applications
            NetworkResult.Success(applications)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user applications", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // This is the required method from ApiClient interface with correct return type
    override suspend fun applyCampaign(campaignId: String, carId: String): NetworkResult<CampaignApplication> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId()
                ?: return@withContext NetworkResult.Error("User not logged in")

            // Create parameters list for checking existing applications
            val checkParams = ArrayList<Any>()
            checkParams.add(campaignId.toLong())
            checkParams.add(carId.toLong())
            checkParams.add(userId.toLong())

            // FIXED: Changed to return Long instead of String? to avoid nullability issues
            val existingApplicationId = DatabaseHelper.executeQuery(
                """
                SELECT id FROM applications
                WHERE offer_id = ? AND car_id = ? AND user_id = ?
                """,
                checkParams
            ) { resultSet ->
                if (resultSet.next()) {
                    resultSet.getLong("id")  // Returns primitive long, not nullable
                } else {
                    0L  // Use 0L instead of null
                }
            }

            if (existingApplicationId > 0) {
                return@withContext NetworkResult.Error("You have already applied to this campaign with this car")
            }

            // Create parameters list for inserting new application
            val insertParams = ArrayList<Any>()
            insertParams.add(campaignId.toLong())
            insertParams.add(carId.toLong())
            insertParams.add(userId.toLong())

            // Insert new application
            val applicationId = DatabaseHelper.executeInsert(
                """
                INSERT INTO applications (
                    offer_id, car_id, user_id, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'PENDING', NOW(), NOW())
                """,
                insertParams
            )

            if (applicationId > 0) {
                // Create parameters for fetching new application
                val appParams = ArrayList<Any>()
                appParams.add(applicationId)

                // FIXED: Changed to return a non-nullable value or handle nullability differently
                val applicationFound = DatabaseHelper.executeQuery(
                    """
                    SELECT a.*, o.title as campaign_title, c.make as car_make, c.model as car_model
                    FROM applications a
                    JOIN offers o ON a.offer_id = o.id
                    JOIN cars c ON a.car_id = c.id
                    WHERE a.id = ?
                    """,
                    appParams
                ) { resultSet ->
                    val applications = mutableListOf<CampaignApplication>()
                    if (resultSet.next()) {
                        applications.add(resultSetToApplication(resultSet))
                    }
                    applications
                }

                if (applicationFound.isNotEmpty()) {
                    val application = applicationFound.first()
                    // Update the applications state flow with the new application
                    _userApplications.value = _userApplications.value + application
                    NetworkResult.Success(application)
                } else {
                    NetworkResult.Error("Failed to retrieve created application")
                }
            } else {
                NetworkResult.Error("Failed to create application")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying to campaign", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Helper method to build a query from endpoint and params
    private fun buildQueryFromEndpoint(endpoint: String, queryParams: Map<String, String>): String {
        // Determine base query from endpoint
        val baseQuery = when {
            endpoint.startsWith("/campaigns") -> "SELECT * FROM campaigns"
            endpoint.startsWith("/users") -> "SELECT * FROM users_personal"
            endpoint.startsWith("/cars") -> "SELECT * FROM cars"
            endpoint.startsWith("/applications") -> "SELECT * FROM applications"
            else -> "SELECT 1"  // Fallback query that will return a single row with a single column
        }

        // Add WHERE clauses for query params
        val whereClause = if (queryParams.isNotEmpty()) {
            "WHERE " + queryParams.entries.joinToString(" AND ") { "${it.key} = ?" }
        } else {
            ""
        }

        return "$baseQuery $whereClause LIMIT 1"
    }

    // Generic API call implementations using specific classes instead of reified type parameters
    suspend fun getStringData(endpoint: String, queryParams: Map<String, String>): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            // Convert query params to params list
            val params = ArrayList<Any>()
            queryParams.values.forEach { params.add(it) }

            // Construct query from endpoint and params using the helper method
            val query = buildQueryFromEndpoint(endpoint, queryParams)

            // Execute query and map result
            val result = DatabaseHelper.executeQuery(query, params) { resultSet ->
                if (resultSet.next()) {
                    // Get all column names
                    val metadata = resultSet.metaData
                    val columnCount = metadata.columnCount
                    val data = mutableMapOf<String, Any?>()

                    // Populate data map with column values
                    for (i in 1..columnCount) {
                        val columnName = metadata.getColumnName(i)
                        val value = resultSet.getObject(i)
                        data[columnName] = value
                    }

                    // Convert data map to JSON string
                    data.entries.joinToString(
                        prefix = "{",
                        postfix = "}",
                        separator = ","
                    ) { "\"${it.key}\":\"${it.value}\"" }
                } else {
                    // Return empty JSON if no results
                    "{}"
                }
            }

            NetworkResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error in API GET call to $endpoint", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Additional methods for other common return types
    suspend fun getIntData(endpoint: String, queryParams: Map<String, String>): NetworkResult<Int> = withContext(Dispatchers.IO) {
        try {
            // Convert query params to params list
            val params = ArrayList<Any>()
            queryParams.values.forEach { params.add(it) }

            // Implementation similar to getString but returns an Int
            val result = DatabaseHelper.executeQuery(
                buildQueryFromEndpoint(endpoint, queryParams),
                params
            ) { rs ->
                if (rs.next() && rs.getMetaData().getColumnCount() > 0) {
                    rs.getInt(1)
                } else {
                    0
                }
            }

            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Helper method to execute inserts
    private suspend fun executeInsert(table: String, data: Map<String, String>): Long = withContext(Dispatchers.IO) {
        if (data.isEmpty()) {
            return@withContext 0L
        }

        val columns = data.keys.joinToString(", ")
        val placeholders = data.keys.map { "?" }.joinToString(", ")
        val query = "INSERT INTO $table ($columns) VALUES ($placeholders)"

        val params = ArrayList<Any>()
        data.values.forEach { params.add(it) }

        try {
            DatabaseHelper.executeInsert(query, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing insert into $table", e)
            0L
        }
    }

    // Generic POST API call with specific implementations for different data types
    suspend fun postCampaign(campaign: Campaign): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val insertData = mapOf(
                "title" to campaign.title,
                "description" to campaign.description,
                "brand_id" to campaign.brandId,
                "status" to campaign.status.name,
                "created_at" to System.currentTimeMillis().toString()
            )

            val id = executeInsert("campaigns", insertData)

            if (id > 0) {
                NetworkResult.Success(id.toString())
            } else {
                NetworkResult.Error("Failed to create campaign")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting campaign", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Method for posting user data
    suspend fun postUser(user: User): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val insertData = mapOf(
                "email" to user.email,
                "full_name" to user.name,
                "created_at" to System.currentTimeMillis().toString()
            )

            val id = executeInsert("users_personal", insertData)

            if (id > 0) {
                NetworkResult.Success(id.toString())
            } else {
                NetworkResult.Error("Failed to create user")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting user", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Method for posting car data
    suspend fun postCar(car: Car): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val insertData = mapOf(
                "make" to car.make,
                "model" to car.model,
                "year" to car.year.toString(),
                "color" to car.color,
                "license_plate" to car.licensePlate,
                "current_mileage" to car.currentMileage.toString()
            )

            val id = executeInsert("cars", insertData)

            if (id > 0) {
                NetworkResult.Success(id.toString())
            } else {
                NetworkResult.Error("Failed to create car")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting car", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    // Helper functions
    private suspend fun fetchStickerPositions(campaignId: String): List<StickerPosition> = withContext(Dispatchers.IO) {
        try {
            val params = ArrayList<Any>()
            params.add(campaignId.toLong())

            DatabaseHelper.executeQuery(
                """
                SELECT position_name FROM offer_positions
                WHERE offer_id = ?
                """,
                params
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

    private suspend fun fetchTargetCities(campaignId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val params = ArrayList<Any>()
            params.add(campaignId.toLong())

            DatabaseHelper.executeQuery(
                """
                SELECT city_name FROM offer_cities
                WHERE offer_id = ?
                """,
                params
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

    private suspend fun fetchCarMakes(campaignId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val params = ArrayList<Any>()
            params.add(campaignId.toLong())

            DatabaseHelper.executeQuery(
                """
                SELECT make_name FROM offer_car_makes
                WHERE offer_id = ?
                """,
                params
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