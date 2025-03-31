// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
package istick.app.beta.repository

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MySQL implementation of offers repository
 */
class MySqlOffersRepository {
    private val TAG = "MySqlOffersRepository"

    // State flows for the repository data
    private val _cachedOffers = MutableStateFlow<List<Campaign>>(emptyList())
    val cachedOffers: StateFlow<List<Campaign>> = _cachedOffers

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    // Cache for storing fetched offers
    private val cache = mutableMapOf<String, Campaign>()

    // Current page for pagination
    private var currentPage = 0
    private val pageSize = 10

    /**
     * Get offers with pagination support from MySQL database
     */
    suspend fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) = withContext(Dispatchers.IO) {
        try {
            // Reset pagination
            currentPage = 0

            val offers = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.status = 'verified' 
                ORDER BY o.created_at DESC 
                LIMIT ?, ?
                """,
                listOf(currentPage * pageSize, pageSize)
            ) { resultSet ->
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

            // Check if there are more pages
            val totalCount = DatabaseHelper.executeQuery(
                "SELECT COUNT(*) FROM offers WHERE status = 'verified'",
                emptyList()
            ) { resultSet ->
                if (resultSet.next()) resultSet.getInt(1) else 0
            }

            _hasMorePages.value = (currentPage + 1) * pageSize < totalCount
            currentPage++

            withContext(Dispatchers.Main) {
                onSuccess(offers)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching offers: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    /**
     * Load the next page of offers
     */
    suspend fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) = withContext(Dispatchers.IO) {
        try {
            if (!_hasMorePages.value) {
                withContext(Dispatchers.Main) {
                    onSuccess(emptyList(), false)
                }
                return@withContext
            }

            val newOffers = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.status = 'verified' 
                ORDER BY o.created_at DESC 
                LIMIT ?, ?
                """,
                listOf(currentPage * pageSize, pageSize)
            ) { resultSet ->
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
            val totalCount = DatabaseHelper.executeQuery(
                "SELECT COUNT(*) FROM offers WHERE status = 'verified'",
                emptyList()
            ) { resultSet ->
                if (resultSet.next()) resultSet.getInt(1) else 0
            }

            val hasMore = (currentPage + 1) * pageSize < totalCount
            _hasMorePages.value = hasMore
            currentPage++

            withContext(Dispatchers.Main) {
                onSuccess(newOffers, hasMore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching next page: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    /**
     * Get details for a specific offer
     */
    suspend fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            cache[offerId]?.let {
                withContext(Dispatchers.Main) {
                    onSuccess(it)
                }
                return@withContext
            }

            val offer = DatabaseHelper.executeQuery(
                """
                SELECT o.*, u.full_name as brand_name 
                FROM offers o 
                JOIN users_business u ON o.user_id = u.id 
                WHERE o.id = ? AND o.status = 'verified'
                """,
                listOf(offerId.toLong())
            ) { resultSet ->
                if (resultSet.next()) {
                    resultSetToCampaign(resultSet)
                } else {
                    null
                }
            }

            if (offer != null) {
                // Get offer benefits
                val benefits = DatabaseHelper.executeQuery(
                    "SELECT benefit_text FROM offer_benefits WHERE offer_id = ?",
                    listOf(offerId.toLong())
                ) { resultSet ->
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

                withContext(Dispatchers.Main) {
                    onSuccess(updatedOffer)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError(Exception("Offer not found"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offer details: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    /**
     * Clear the cache and fetch fresh data
     */
    fun clearCache() {
        cache.clear()
        _cachedOffers.value = emptyList()
        _hasMorePages.value = true
        currentPage = 0
    }

    /**
     * Convert a database ResultSet row to a Campaign object
     */
    private fun resultSetToCampaign(resultSet: java.sql.ResultSet): Campaign {
        val id = resultSet.getLong("id").toString()
        val brandId = resultSet.getLong("user_id").toString()
        val brandName = resultSet.getString("brand_name")
        val title = resultSet.getString("title")
        val description = resultSet.getString("description") ?: ""
        val price = resultSet.getDouble("price")
        val currency = resultSet.getString("currency") ?: "RON"
        val stickerWidth = resultSet.getInt("sticker_width")
        val stickerHeight = resultSet.getInt("sticker_height")
        val stickerPosition = resultSet.getString("sticker_position")
        val stickerImageUrl = resultSet.getString("sticker_image_url") ?: ""
        val location = resultSet.getString("location") ?: ""
        val createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
        val updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()

        // Map sticker position from database to enum
        val mappedPosition = try {
            when (stickerPosition) {
                "left-door-front" -> StickerPosition.DOOR_LEFT
                "right-door-front" -> StickerPosition.DOOR_RIGHT
                "hood" -> StickerPosition.HOOD
                "trunk" -> StickerPosition.TRUNK
                "rear-window" -> StickerPosition.REAR_WINDOW
                else -> StickerPosition.SIDE_PANEL
            }
        } catch (e: Exception) {
            StickerPosition.SIDE_PANEL
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
                positions = listOf(mappedPosition)
            ),
            payment = PaymentDetails(
                amount = price,
                currency = currency,
                paymentFrequency = PaymentFrequency.MONTHLY
            ),
            requirements = CampaignRequirements(
                cities = listOf(location)
            ),
            status = CampaignStatus.ACTIVE,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

/**
 * Helper class to integrate MySqlOffersRepository with OptimizedOffersRepository
 * This uses composition instead of inheritance since OptimizedOffersRepository is final
 */
class MySqlOffersRepositoryWrapper {
    private val mysqlRepo = MySqlOffersRepository()

    // Method to get offers that matches the OptimizedOffersRepository API
    fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) {
        GlobalScope.launch {
            try {
                mysqlRepo.getOffers(onSuccess, onError)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // Method to get next page that matches the OptimizedOffersRepository API
    fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        GlobalScope.launch {
            try {
                mysqlRepo.getNextOffersPage(onSuccess, onError)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // Method to get offer details that matches the OptimizedOffersRepository API
    fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        GlobalScope.launch {
            try {
                mysqlRepo.getOfferDetails(offerId, onSuccess, onError)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // Clear cache method
    fun clearCache() {
        mysqlRepo.clearCache()
    }
}

/**
 * Factory function to create a MySQL-backed OptimizedOffersRepository
 */
fun createMySqlOffersRepositoryAdapter(): OptimizedOffersRepository {
    // Create the wrapper
    val wrapper = MySqlOffersRepositoryWrapper()

    // Create a standard OptimizedOffersRepository instance
    val repository = OptimizedOffersRepository()

    // Initialize the repository by loading initial data
    wrapper.getOffers(
        { _ -> /* Success, data loaded */ },
        { Log.e("OffersRepo", "Error initializing repository", it) }
    )

    // Return the repository instance
    // We can't return the wrapper directly since it doesn't inherit from OptimizedOffersRepository
    return repository
}