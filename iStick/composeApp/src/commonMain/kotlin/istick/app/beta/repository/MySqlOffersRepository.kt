// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
package istick.app.beta.repository

import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class MySqlOffersRepository : OptimizedOffersRepository() {
    // Keep the existing implementation but override the data fetching methods

    override fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) {
        try {
            // Execute query on a background thread
            kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch(Dispatchers.IO) {
                try {
                    val campaigns = DatabaseHelper.executeQuery(
                        """
                        SELECT o.*, u.full_name as brand_name, u.company_name
                        FROM offers o
                        JOIN users_business u ON o.user_id = u.id
                        WHERE o.status = 'verified'
                        ORDER BY o.created_at DESC
                        """,
                        emptyList()
                    ) { rs ->
                        val results = mutableListOf<Campaign>()
                        while (rs.next()) {
                            results.add(
                                Campaign(
                                    id = rs.getInt("id").toString(),
                                    brandId = rs.getInt("user_id").toString(),
                                    title = rs.getString("title"),
                                    description = rs.getString("description"),
                                    status = CampaignStatus.ACTIVE, // Only verified ones are returned
                                    payment = PaymentDetails(
                                        amount = rs.getDouble("price"),
                                        currency = rs.getString("currency")
                                    ),
                                    stickerDetails = StickerDetails(
                                        imageUrl = rs.getString("sticker_image_url"),
                                        width = rs.getInt("sticker_width"),
                                        height = rs.getInt("sticker_height"),
                                        positions = listOf(mapPosition(rs.getString("sticker_position"))),
                                        deliveryMethod = DeliveryMethod.CENTER // Default
                                    ),
                                    requirements = CampaignRequirements(
                                        minDailyDistance = 0, // Not directly in DB
                                        cities = listOf(rs.getString("location") ?: "")
                                            .filter { it.isNotBlank() }
                                    ),
                                    startDate = rs.getTimestamp("created_at")?.time,
                                    endDate = null, // Calculate from contract_duration if needed
                                    createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                                    updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()
                                )
                            )
                        }
                        results
                    }
                    
                    // Update cache
                    _cachedOffers.value = campaigns
                    
                    // Notify with the result
                    withContext(Dispatchers.Main) {
                        onSuccess(campaigns)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        // For simplicity, we'll just return an empty list since pagination
        // would require more complex implementation with the actual database
        onSuccess(emptyList(), false)
    }

    override fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        try {
            // Check cache first
            cache[offerId]?.let {
                onSuccess(it)
                return
            }

            // Execute query on a background thread
            kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch(Dispatchers.IO) {
                try {
                    val campaign = DatabaseHelper.executeQuery(
                        """
                        SELECT o.*, u.full_name as brand_name, u.company_name
                        FROM offers o
                        JOIN users_business u ON o.user_id = u.id
                        WHERE o.id = ?
                        """,
                        listOf(offerId.toIntOrNull() ?: 0)
                    ) { rs ->
                        if (rs.next()) {
                            Campaign(
                                id = rs.getInt("id").toString(),
                                brandId = rs.getInt("user_id").toString(),
                                title = rs.getString("title"),
                                description = rs.getString("description"),
                                status = when (rs.getString("status")) {
                                    "verified" -> CampaignStatus.ACTIVE
                                    "pending" -> CampaignStatus.DRAFT
                                    "rejected" -> CampaignStatus.CANCELLED
                                    else -> CampaignStatus.DRAFT
                                },
                                payment = PaymentDetails(
                                    amount = rs.getDouble("price"),
                                    currency = rs.getString("currency")
                                ),
                                stickerDetails = StickerDetails(
                                    imageUrl = rs.getString("sticker_image_url"),
                                    width = rs.getInt("sticker_width"),
                                    height = rs.getInt("sticker_height"),
                                    positions = listOf(mapPosition(rs.getString("sticker_position"))),
                                    deliveryMethod = DeliveryMethod.CENTER // Default
                                ),
                                requirements = CampaignRequirements(
                                    minDailyDistance = 0, // Not directly in DB
                                    cities = listOf(rs.getString("location") ?: "")
                                        .filter { it.isNotBlank() }
                                ),
                                startDate = rs.getTimestamp("created_at")?.time,
                                endDate = null, // Calculate from contract_duration if needed
                                createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
                                updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()
                            )
                        } else null
                    }
                    
                    if (campaign != null) {
                        // Update cache
                        cache[offerId] = campaign
                        
                        // Notify with the result
                        withContext(Dispatchers.Main) {
                            onSuccess(campaign)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError(Exception("Offer not found"))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    private fun mapPosition(position: String): StickerPosition {
        return when (position.toLowerCase()) {
            "left-door-front" -> StickerPosition.DOOR_LEFT
            "right-door-front" -> StickerPosition.DOOR_RIGHT
            "hood" -> StickerPosition.HOOD
            "trunk" -> StickerPosition.TRUNK
            "rear-window" -> StickerPosition.REAR_WINDOW
            else -> StickerPosition.SIDE_PANEL
        }
    }
}