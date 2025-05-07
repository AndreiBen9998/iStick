// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/OffersRepositoryInterface.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign

interface OffersRepositoryInterface {
    suspend fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit)
    suspend fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit)
    suspend fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit)
    fun clearCache()
}