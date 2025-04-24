// commonMain/kotlin/istick/app/beta/repository/OffersRepositoryInterface.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign

interface OffersRepositoryInterface {
    fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit)
    fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit)
    fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit)
    fun clearCache()
}