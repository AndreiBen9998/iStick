// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/OptimizedOffersRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign
import istick.app.beta.network.ApiClient
import istick.app.beta.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An optimized repository implementation for campaigns/offers
 * This implementation communicates with the real backend API
 */
class OptimizedOffersRepository(
    private val apiClient: ApiClient
) : OffersRepositoryInterface {
    // Cache for offers data
    private val offersCache = mutableListOf<Campaign>()
    private var hasMoreOffers = true
    private var currentPage = 0
    private val offersPerPage = 10

    // Cached offer details
    private val offerDetailsCache = mutableMapOf<String, Campaign>()

    /**
     * Get offers with pagination
     */
    override suspend fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit) {
        // Reset pagination when getting offers from the beginning
        currentPage = 0
        hasMoreOffers = true
        offersCache.clear()

        withContext(Dispatchers.IO) {
            try {
                // Make API call to get campaigns
                when (val result = apiClient.getCampaigns(page = 0, pageSize = offersPerPage)) {
                    is NetworkResult.Success -> {
                        val campaigns = result.data

                        // Cache results
                        offersCache.addAll(campaigns)

                        // Update pagination state
                        hasMoreOffers = campaigns.size >= offersPerPage
                        currentPage++

                        onSuccess(campaigns)
                    }
                    is NetworkResult.Error -> {
                        onError(Exception(result.message))
                    }
                    is NetworkResult.Loading -> {
                        // Do nothing, wait for the result
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Get next page of offers
     */
    override suspend fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (!hasMoreOffers) {
            onSuccess(emptyList(), false)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                // Make API call to get next page of campaigns
                when (val result = apiClient.getCampaigns(page = currentPage, pageSize = offersPerPage)) {
                    is NetworkResult.Success -> {
                        val campaigns = result.data

                        // Cache results
                        offersCache.addAll(campaigns)

                        // Update pagination state
                        hasMoreOffers = campaigns.size >= offersPerPage
                        currentPage++

                        onSuccess(campaigns, hasMoreOffers)
                    }
                    is NetworkResult.Error -> {
                        onError(Exception(result.message))
                    }
                    is NetworkResult.Loading -> {
                        // Do nothing, wait for the result
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Get details for a specific offer
     */
    override suspend fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit) {
        // Check cache first
        offerDetailsCache[offerId]?.let {
            onSuccess(it)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                // Make API call to get campaign details
                when (val result = apiClient.getCampaignDetails(campaignId = offerId)) {
                    is NetworkResult.Success -> {
                        val campaign = result.data

                        // Cache the result
                        offerDetailsCache[offerId] = campaign

                        onSuccess(campaign)
                    }
                    is NetworkResult.Error -> {
                        onError(Exception(result.message))
                    }
                    is NetworkResult.Loading -> {
                        // Do nothing, wait for the result
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Clear caches
     */
    override fun clearCache() {
        offersCache.clear()
        offerDetailsCache.clear()
        currentPage = 0
        hasMoreOffers = true
    }
}