package istick.app.beta.repository

import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class OptimizedOffersRepository : OffersRepositoryInterface {
    private val _cachedOffers = MutableStateFlow<List<Campaign>>(emptyList())
    val cachedOffers: StateFlow<List<Campaign>> = _cachedOffers

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    // Cache for storing fetched offers
    protected val cache = mutableMapOf<String, Campaign>()

    // Timestamp of last refresh to limit frequent updates
    private var lastRefreshTimestamp = 0L

    /**
     * Get offers with pagination support.
     * First loads from cache if available, then updates from the backend.
     */
    override fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) {
        // For demo purpose, return mock data
        if (_cachedOffers.value.isNotEmpty()) {
            onSuccess(_cachedOffers.value)
            return
        }

        try {
            // Mock data for now
            val mockOffers = listOf(
                Campaign(
                    id = "offer1",
                    brandId = "brand1",
                    title = "TechCorp Promotional Campaign",
                    description = "Promote our tech products on your car",
                    status = CampaignStatus.ACTIVE,
                    payment = istick.app.beta.model.PaymentDetails(
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
                    payment = istick.app.beta.model.PaymentDetails(
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
                    payment = istick.app.beta.model.PaymentDetails(
                        amount = 400.0,
                        currency = "RON"
                    )
                )
            )

            // Update cache
            mockOffers.forEach { offer ->
                cache[offer.id] = offer
            }

            _cachedOffers.value = mockOffers
            lastRefreshTimestamp = System.currentTimeMillis()

            onSuccess(mockOffers)
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Load the next page of offers.
     */
    override fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (!_hasMorePages.value) {
            onSuccess(emptyList(), false)
            return
        }

        try {
            // Mock next page data
            val newOffers = listOf(
                Campaign(
                    id = "offer4",
                    brandId = "brand4",
                    title = "Fitness Promotion",
                    description = "Promote fitness products with your car",
                    status = CampaignStatus.ACTIVE,
                    payment = istick.app.beta.model.PaymentDetails(
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
                    payment = istick.app.beta.model.PaymentDetails(
                        amount = 350.0,
                        currency = "RON"
                    )
                )
            )

            // Update cache
            newOffers.forEach { offer ->
                cache[offer.id] = offer
            }

            // No more pages after this demonstration
            val hasMore = false
            _hasMorePages.value = hasMore

            onSuccess(newOffers, hasMore)
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Get details for a specific offer.
     */
    override fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        // Check cache first
        cache[offerId]?.let {
            onSuccess(it)
            return
        }

        try {
            // Mock data for a specific offer
            val offer = Campaign(
                id = offerId,
                brandId = "brand1",
                title = "Special Campaign",
                description = "This is a detailed description of the campaign",
                status = CampaignStatus.ACTIVE,
                payment = istick.app.beta.model.PaymentDetails(
                    amount = 500.0,
                    currency = "RON"
                )
            )

            // Update cache
            cache[offerId] = offer

            onSuccess(offer)
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Refresh the cache in the background.
     */
    private fun refreshOffersInBackground() {
        // This would normally fetch fresh data from the backend
        // For now, we'll just skip implementation
    }

    /**
     * Clear the cache and fetch fresh data.
     */
    override fun clearCache() {
        cache.clear()
        _cachedOffers.value = emptyList()
        _hasMorePages.value = true
    }
}