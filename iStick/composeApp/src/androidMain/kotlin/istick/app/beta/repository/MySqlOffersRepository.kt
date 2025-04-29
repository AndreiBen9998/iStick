// androidMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
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
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching offers from database", e)
            // Fallback to mock data
        }

        // Fallback to mock data
        val mockOffers = createMockOffers()
        mockOffers.forEach { offer ->
            cache[offer.id] = offer
        }

        onSuccess(mockOffers)
    }

    actual override suspend fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (!hasMore) {
            onSuccess(emptyList(), false)
            return
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
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching next page from database", e)
            // Fallback to mock data
        }

        // Fallback to mock data for the next page
        val mockOffers = createNextPageMockOffers()
        hasMore = false

        mockOffers.forEach { offer ->
            cache[offer.id] = offer
        }

        onSuccess(mockOffers, false)
    }

    actual override suspend fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        // Check cache first
        cache[offerId]?.let {
            onSuccess(it)
            return
        }

        // Try to get details from database
        try {
            val offer = fetchOfferDetailsFromDatabase(offerId)
            if (offer != null) {
                cache[offerId] = offer
                onSuccess(offer)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching offer details from database", e)
            // Fallback to mock data
        }

        // Fallback to mock data
        val mockOffer = createMockOffer(offerId)
        cache[offerId] = mockOffer
        onSuccess(mockOffer)
    }

    actual override fun clearCache() {
        cache.clear()
        hasMore = true
        lastOffset = 0
    }

    // Private methods for database operations
    private suspend fun fetchOffersFromDatabase(offset: Int, limit: Int): List<Campaign> = withContext(Dispatchers.IO) {
        try {
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
                val campaigns = mutableListOf<Campaign>()
                while (rs.next()) {
                    val campaignId = rs.getString("id")

                    // Get sticker details
                    val stickerDetails = getStickerDetails(campaignId)

                    // Get requirements
                    val requirements = getRequirements(campaignId)

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

                    campaigns.add(campaign)
                }
                campaigns
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchOffersFromDatabase", e)
            emptyList()
        }
    }

    private suspend fun fetchOfferDetailsFromDatabase(offerId: String): Campaign? = withContext(Dispatchers.IO) {
        try {
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

                    // Get sticker details
                    val stickerDetails = getStickerDetails(campaignId)

                    // Get requirements
                    val requirements = getRequirements(campaignId)

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

                    Campaign(
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
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchOfferDetailsFromDatabase", e)
            null
        }
    }

    private suspend fun getStickerDetails(campaignId: String): StickerDetails = withContext(Dispatchers.IO) {
        try {
            DatabaseHelper.executeQuery(
                """
                SELECT s.*, p.position
                FROM campaign_sticker s
                LEFT JOIN campaign_sticker_positions p ON s.id = p.sticker_id
                WHERE s.campaign_id = ?
                """,
                listOf(campaignId)
            ) { rs ->
                var stickerDetails = StickerDetails()
                val positions = mutableListOf<StickerPosition>()

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

                // Default positions if none found
                if (positions.isEmpty()) {
                    positions.add(StickerPosition.DOOR_LEFT)
                }

                stickerDetails.copy(positions = positions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sticker details", e)
            StickerDetails(
                positions = listOf(StickerPosition.DOOR_LEFT)
            )
        }
    }

    private suspend fun getRequirements(campaignId: String): CampaignRequirements = withContext(Dispatchers.IO) {
        try {
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

                    CampaignRequirements(
                        minDailyDistance = rs.getInt("min_daily_distance"),
                        cities = cities,
                        carMakes = carMakes,
                        carModels = carModels,
                        carYearMin = rs.getObject("car_year_min") as Int?,
                        carYearMax = rs.getObject("car_year_max") as Int?
                    )
                } else {
                    CampaignRequirements()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting requirements", e)
            CampaignRequirements()
        }
    }

    private fun createMockOffers(): List<Campaign> {
        return listOf(
            Campaign(
                id = "offer1",
                brandId = "brand1",
                title = "TechCorp Promotional Campaign",
                description = "Promote our tech products on your car",
                status = CampaignStatus.ACTIVE,
                payment = PaymentDetails(
                    amount = 500.0,
                    currency = "RON"
                )
            ),
            Campaign(
                id = "offer2",
                brandId = "brand2",
                title = "EcoFriendly Campaign",
                description = "Promote eco-friendly products",
                status = CampaignStatus.ACTIVE,
                payment = PaymentDetails(
                    amount = 450.0,
                    currency = "RON"
                )
            ),
            Campaign(
                id = "offer3",
                brandId = "brand3",
                title = "Local Business Promotion",
                description = "Support local businesses with your car",
                status = CampaignStatus.ACTIVE,
                payment = PaymentDetails(
                    amount = 400.0,
                    currency = "RON"
                )
            )
        )
    }

    private fun createNextPageMockOffers(): List<Campaign> {
        return listOf(
            Campaign(
                id = "offer4",
                brandId = "brand4",
                title = "Fitness Promotion",
                description = "Promote fitness products with your car",
                status = CampaignStatus.ACTIVE,
                payment = PaymentDetails(
                    amount = 550.0,
                    currency = "RON"
                )
            ),
            Campaign(
                id = "offer5",
                brandId = "brand5",
                title = "Coffee Shop Ads",
                description = "Promote local coffee shops",
                status = CampaignStatus.ACTIVE,
                payment = PaymentDetails(
                    amount = 350.0,
                    currency = "RON"
                )
            )
        )
    }

    private fun createMockOffer(offerId: String): Campaign {
        return Campaign(
            id = offerId,
            brandId = "brand1",
            title = "Special Campaign",
            description = "This is a detailed description of the campaign with all the information you need to know about promoting our products on your car.",
            status = CampaignStatus.ACTIVE,
            stickerDetails = StickerDetails(
                imageUrl = "https://example.com/sticker.jpg",
                width = 30,
                height = 20,
                positions = listOf(StickerPosition.DOOR_LEFT, StickerPosition.DOOR_RIGHT),
                deliveryMethod = DeliveryMethod.HOME_KIT
            ),
            payment = PaymentDetails(
                amount = 500.0,
                currency = "RON",
                paymentFrequency = PaymentFrequency.MONTHLY,
                paymentMethod = PaymentMethod.BANK_TRANSFER
            ),
            requirements = CampaignRequirements(
                minDailyDistance = 30,
                cities = listOf("București", "Cluj"),
                carMakes = listOf("Toyota", "Honda"),
                carYearMin = 2015
            ),
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + 2592000000, // 30 days from now
            createdAt = System.currentTimeMillis() - 259200000, // 3 days ago
            updatedAt = System.currentTimeMillis()
        )
    }
}