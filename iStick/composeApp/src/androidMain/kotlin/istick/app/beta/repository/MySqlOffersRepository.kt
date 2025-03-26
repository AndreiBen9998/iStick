// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
package istick.app.beta.repository

import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignStatus
import istick.app.beta.model.PaymentDetails
import istick.app.beta.model.StickerDetails
import istick.app.beta.model.StickerPosition
import istick.app.beta.model.CampaignRequirements
import istick.app.beta.model.PaymentFrequency
import istick.app.beta.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.sql.ResultSet

/**
 * Implementation of OptimizedOffersRepository that uses MySQL database
 */
class MySqlOffersRepository : OptimizedOffersRepository() {
    private val _cachedOffers = MutableStateFlow<List<Campaign>>(emptyList())
    override val cachedOffers: StateFlow<List<Campaign>> = _cachedOffers

    private val _hasMorePages = MutableStateFlow(true)
    override val hasMorePages: StateFlow<Boolean> = _hasMorePages

    // Cache for storing fetched offers
    private val cache = mutableMapOf<String, Campaign>()

    // Timestamp of last refresh to limit frequent updates
    private var lastRefreshTimestamp = 0L
    
    // Current page for pagination
    private var currentPage = 0
    private val pageSize = 10

    /**
     * Get offers with pagination support from MySQL database
     */
    override fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) {
        // Return cached data if available and not expired
        if (_cachedOffers.value.isNotEmpty() && (System.currentTimeMillis() - lastRefreshTimestamp < 5 * 60 * 1000)) {
            onSuccess(_cachedOffers.value)
            return
        }

        // Reset pagination
        currentPage = 0
        
        try {
            // Use coroutines to fetch data from database
            kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch {
                try {
                    val sql = """
                        SELECT o.*, u.full_name as brand_name 
                        FROM offers o 
                        JOIN users_business u ON o.user_id = u.id 
                        WHERE o.status = 'verified' 
                        ORDER BY o.created_at DESC 
                        LIMIT ?, ?
                    """
                    
                    val offers = DatabaseHelper.executeQuery(sql, listOf(currentPage * pageSize, pageSize)) { resultSet ->
                        val offersList = mutableListOf<Campaign>()
                        while (resultSet.next()) {
                            offersList.add(resultSetToCampaign(resultSet))
                        }
                        offersList
                    }
                    
                    // Update cache
                    offers.forEach { offer ->
                        cache[offer.id] = offer
                    }
                    
                    _cachedOffers.value = offers
                    lastRefreshTimestamp = System.currentTimeMillis()
                    
                    // Check if there are more pages
                    val countSql = "SELECT COUNT(*) FROM offers WHERE status = 'verified'"
                    val totalCount = DatabaseHelper.executeQuery(countSql) { resultSet ->
                        if (resultSet.next()) resultSet.getInt(1) else 0
                    }
                    
                    _hasMorePages.value = (currentPage + 1) * pageSize < totalCount
                    currentPage++
                    
                    // Notify success on main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onSuccess(offers)
                    }
                } catch (e: Exception) {
                    // Notify error on main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Load the next page of offers
     */
    override fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (!_hasMorePages.value) {
            onSuccess(emptyList(), false)
            return
        }
        
        try {
            kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch {
                try {
                    val sql = """
                        SELECT o.*, u.full_name as brand_name 
                        FROM offers o 
                        JOIN users_business u ON o.user_id = u.id 
                        WHERE o.status = 'verified' 
                        ORDER BY o.created_at DESC 
                        LIMIT ?, ?
                    """
                    
                    val newOffers = DatabaseHelper.executeQuery(sql, listOf(currentPage * pageSize, pageSize)) { resultSet ->
                        val offersList = mutableListOf<Campaign>()
                        while (resultSet.next()) {
                            offersList.add(resultSetToCampaign(resultSet))
                        }
                        offersList
                    }
                    
                    // Update cache
                    newOffers.forEach { offer ->
                        cache[offer.id] = offer
                    }
                    
                    // Check if there are more pages
                    val countSql = "SELECT COUNT(*) FROM offers WHERE status = 'verified'"
                    val totalCount = DatabaseHelper.executeQuery(countSql) { resultSet ->
                        if (resultSet.next()) resultSet.getInt(1) else 0
                    }
                    
                    val hasMore = (currentPage + 1) * pageSize < totalCount
                    _hasMorePages.value = hasMore
                    currentPage++
                    
                    // Notify success on main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onSuccess(newOffers, hasMore)
                    }
                } catch (e: Exception) {
                    // Notify error on main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Get details for a specific offer
     */
    override fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        // Check cache first
        cache[offerId]?.let {
            onSuccess(it)
            return
        }
        
        try {
            kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch {
                try {
                    val sql = """
                        SELECT o.*, u.full_name as brand_name 
                        FROM offers o 
                        JOIN users_business u ON o.user_id = u.id 
                        WHERE o.id = ? AND o.status = 'verified'
                    """
                    
                    val offer = DatabaseHelper.executeQuery(sql, listOf(offerId.toInt())) { resultSet ->
                        if (resultSet.next()) {
                            resultSetToCampaign(resultSet)
                        } else {
                            null
                        }
                    }
                    
                    if (offer != null) {
                        // Get offer benefits
                        val benefitsSql = "SELECT benefit_text FROM offer_benefits WHERE offer_id = ?"
                        val benefits = DatabaseHelper.executeQuery(benefitsSql, listOf(offerId.toInt())) { resultSet ->
                            val benefitsList = mutableListOf<String>()
                            while (resultSet.next()) {
                                benefitsList.add(resultSet.getString("benefit_text"))
                            }
                            benefitsList
                        }
                        
                        // Update offer with benefits if any found
                        val updatedOffer = if (benefits.isNotEmpty()) {
                            offer.copy(
                                requirements = offer.requirements.copy(
                                    // We're repurposing cities to store benefits since there's no direct field
                                    cities = benefits 
                                )
                            )
                        } else {
                            offer
                        }
                        
                        // Update cache
                        cache[offerId] = updatedOffer
                        
                        // Notify success on main thread
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onSuccess(updatedOffer)
                        }
                    } else {
                        throw Exception("Offer not found")
                    }
                } catch (e: Exception) {
                    // Notify error on main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Clear the cache and fetch fresh data
     */
    override fun clearCache() {
        cache.clear()
        _cachedOffers.value = emptyList()
        _hasMorePages.value = true
        currentPage = 0
    }
    
    /**
     * Convert a database ResultSet row to a Campaign object
     */
    private fun resultSetToCampaign(resultSet: ResultSet): Campaign {
        val id = resultSet.getInt("id").toString()
        val brandId = resultSet.getInt("user_id").toString()
        val brandName = resultSet.getString("brand_name")
        val title = resultSet.getString("title")
        val description = resultSet.getString("description")
        val price = resultSet.getDouble("price")
        val currency = resultSet.getString("currency")
        val duration = resultSet.getInt("contract_duration")
        val location = resultSet.getString("location") ?: ""
        val maxDrivers = resultSet.getInt("max_drivers")
        val stickerWidth = resultSet.getDouble("sticker_width")
        val stickerHeight = resultSet.getDouble("sticker_height")
        val stickerPosition = resultSet.getString("sticker_position")
        val stickerImageUrl = resultSet.getString("sticker_image_url")
        val carRenderImageUrl = resultSet.getString("car_render_image_url")
        val createdAt = resultSet.getTimestamp("created_at").time
        val updatedAt = resultSet.getTimestamp("updated_at").time
        
        // Parse benefits from benefits JSON column if exists
        val benefitsJson = resultSet.getString("benefits")
        val benefits = if (benefitsJson != null && benefitsJson.isNotEmpty()) {
            try {
                // Simple JSON array parsing
                benefitsJson.trim('[', ']').split(",").map { 
                    it.trim('"', ' ') 
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // Map sticker position from database to enum
        val mappedPosition = when (stickerPosition) {
            "left-door-front" -> StickerPosition.DOOR_LEFT
            "right-door-front" -> StickerPosition.DOOR_RIGHT
            "hood" -> StickerPosition.HOOD
            "trunk" -> StickerPosition.TRUNK
            "rear-window" -> StickerPosition.REAR_WINDOW
            else -> StickerPosition.SIDE_PANEL
        }
        
        return Campaign(
            id = id,
            brandId = brandId,
            title = "$title by $brandName",
            description = description,
            stickerDetails = StickerDetails(
                imageUrl = stickerImageUrl,
                width = stickerWidth.toInt(),
                height = stickerHeight.toInt(),
                positions = listOf(mappedPosition)
            ),
            payment = PaymentDetails(
                amount = price,
                currency = currency,
                paymentFrequency = PaymentFrequency.MONTHLY
            ),
            requirements = CampaignRequirements(
                cities = if (location.isNotEmpty()) listOf(location) else benefits
            ),
            status = CampaignStatus.ACTIVE,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}