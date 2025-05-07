// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
package istick.app.beta.repository

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignRequirements
import istick.app.beta.model.CampaignStatus
import istick.app.beta.model.DeliveryMethod
import istick.app.beta.model.PaymentDetails
import istick.app.beta.model.PaymentFrequency
import istick.app.beta.model.PaymentMethod
import istick.app.beta.model.StickerDetails
import istick.app.beta.model.StickerPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

actual class MySqlOffersRepository actual constructor() : OffersRepositoryInterface {
    private val TAG = "MySqlOffersRepository"

    // Cache for storing fetched offers
    private val cache = mutableMapOf<String, Campaign>()
    private var hasMore = true
    private var lastOffset = 0
    private val pageSize = 10

    actual override suspend fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Reset pagination
                lastOffset = 0
                hasMore = true

                // First try to get data from database
                try {
                    val offers = fetchOffersFromDatabase(0, pageSize)
                    if (offers.isNotEmpty()) {
                        // Update cache
                        offers.forEach { offer ->
                            cache[offer.id] = offer
                        }

                        onSuccess(offers)
                        return@withContext
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching offers from database", e)
                    onError(e)
                    return@withContext
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    actual override suspend fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                if (!hasMore) {
                    onSuccess(emptyList(), false)
                    return@withContext
                }

                lastOffset += pageSize

                // Try to get next page from database
                try {
                    val offers = fetchOffersFromDatabase(lastOffset, pageSize)
                    val hasMoreItems = offers.size >= pageSize
                    hasMore = hasMoreItems

                    if (offers.isNotEmpty()) {
                        // Update cache
                        offers.forEach { offer ->
                            cache[offer.id] = offer
                        }

                        onSuccess(offers, hasMoreItems)
                        return@withContext
                    } else {
                        onSuccess(emptyList(), false)
                        return@withContext
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching next page from database", e)
                    onError(e)
                    return@withContext
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    actual override suspend fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Check cache first
                cache[offerId]?.let {
                    onSuccess(it)
                    return@withContext
                }

                // Try to get details from database
                try {
                    val offer = fetchOfferDetailsFromDatabase(offerId)
                    if (offer != null) {
                        cache[offerId] = offer
                        onSuccess(offer)
                        return@withContext
                    } else {
                        onError(Exception("Offer not found"))
                        return@withContext
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching offer details from database", e)
                    onError(e)
                    return@withContext
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    actual override fun clearCache() {
        cache.clear()
        hasMore = true
        lastOffset = 0
    }

    // Private methods for database operations
    private suspend fun fetchOffersFromDatabase(offset: Int, limit: Int): List<Campaign> = withContext(Dispatchers.IO) {
        try {
            val campaignsList = mutableListOf<Campaign>()

            DatabaseHelper.executeQuery(
                """
                SELECT c.*, p.amount, p.currency, p.payment_frequency, p.payment_method
                FROM campaigns c
                LEFT JOIN campaign_payment p ON c.id = p.campaign_id
                WHERE c.status = 'ACTIVE'
                ORDER BY c.created_at DESC
                LIMIT ? OFFSET ?
                """,
                listOf(limit, offset)
            ) { rs ->
                while (rs.next()) {
                    val campaignId = rs.getString("id")

                    // Get sticker details and requirements synchronously
                    val stickerDetails = getStickerDetailsSync(campaignId)
                    val requirements = getRequirementsSync(campaignId)

                    // Get payment details
                    val paymentDetails = PaymentDetails(
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
                    )

                    val campaign = Campaign(
                        id = campaignId,
                        brandId = rs.getString("brand_id"),
                        title = rs.getString("title"),
                        description = rs.getString("description") ?: "",
                        stickerDetails = stickerDetails,
                        payment = paymentDetails,
                        requirements = requirements,
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

                    campaignsList.add(campaign)
                }
            }

            campaignsList
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchOffersFromDatabase", e)
            throw e
        }
    }

    private suspend fun fetchOfferDetailsFromDatabase(offerId: String): Campaign? = withContext(Dispatchers.IO) {
        try {
            var campaign: Campaign? = null

            DatabaseHelper.executeQuery(
                """
                SELECT c.*, p.amount, p.currency, p.payment_frequency, p.payment_method
                FROM campaigns c
                LEFT JOIN campaign_payment p ON c.id = p.campaign_id
                WHERE c.id = ?
                """,
                listOf(offerId)
            ) { rs ->
                if (rs.next()) {
                    val campaignId = rs.getString("id")

                    // Get sticker details and requirements synchronously
                    val stickerDetails = getStickerDetailsSync(campaignId)
                    val requirements = getRequirementsSync(campaignId)

                    // Get payment details
                    val paymentDetails = PaymentDetails(
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
                    )

                    campaign = Campaign(
                        id = campaignId,
                        brandId = rs.getString("brand_id"),
                        title = rs.getString("title"),
                        description = rs.getString("description") ?: "",
                        stickerDetails = stickerDetails,
                        payment = paymentDetails,
                        requirements = requirements,
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
                }
            }

            campaign
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchOfferDetailsFromDatabase", e)
            throw e
        }
    }

    // Using non-suspending versions to avoid the issue with suspension in lambdas
    private fun getStickerDetailsSync(campaignId: String): StickerDetails {
        try {
            var stickerDetails = StickerDetails()
            val positions = mutableListOf<StickerPosition>()

            DatabaseHelper.executeQuery(
                """
                SELECT s.*, p.position
                FROM campaign_sticker s
                LEFT JOIN campaign_sticker_positions p ON s.id = p.sticker_id
                WHERE s.campaign_id = ?
                """,
                listOf(campaignId)
            ) { rs ->
                while (rs.next()) {
                    if (stickerDetails.imageUrl.isEmpty()) {
                        // First row contains the sticker details
                        stickerDetails = StickerDetails(
                            imageUrl = rs.getString("image_url") ?: "",
                            width = rs.getInt("width"),
                            height = rs.getInt("height"),
                            deliveryMethod = try {
                                DeliveryMethod.valueOf(rs.getString("delivery_method") ?: "CENTER")
                            } catch (e: Exception) {
                                DeliveryMethod.CENTER
                            }
                        )
                    }

                    // Get position
                    val positionStr = rs.getString("position")
                    if (positionStr != null) {
                        try {
                            positions.add(StickerPosition.valueOf(positionStr))
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid position: $positionStr", e)
                        }
                    }
                }
            }

            // Default positions if none found
            if (positions.isEmpty()) {
                positions.add(StickerPosition.DOOR_LEFT)
            }

            return stickerDetails.copy(positions = positions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sticker details", e)
            return StickerDetails(
                positions = listOf(StickerPosition.DOOR_LEFT)
            )
        }
    }

    private fun getRequirementsSync(campaignId: String): CampaignRequirements {
        try {
            var requirements = CampaignRequirements()

            DatabaseHelper.executeQuery(
                "SELECT * FROM campaign_requirements WHERE campaign_id = ?",
                listOf(campaignId)
            ) { rs ->
                if (rs.next()) {
                    val cityStr = rs.getString("city")
                    val cities = if (cityStr != null) listOf(cityStr) else emptyList()

                    val carMakeStr = rs.getString("car_make")
                    val carMakes = if (carMakeStr != null) listOf(carMakeStr) else emptyList()

                    val carModelStr = rs.getString("car_model")
                    val carModels = if (carModelStr != null) listOf(carModelStr) else emptyList()

                    requirements = CampaignRequirements(
                        minDailyDistance = rs.getInt("min_daily_distance"),
                        cities = cities,
                        carMakes = carMakes,
                        carModels = carModels,
                        carYearMin = rs.getObject("car_year_min") as? Int,
                        carYearMax = rs.getObject("car_year_max") as? Int
                    )
                }
            }

            return requirements
        } catch (e: Exception) {
            Log.e(TAG, "Error getting requirements", e)
            return CampaignRequirements()
        }
    }
}