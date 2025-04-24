// commonMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign

expect class MySqlOffersRepository() : OffersRepositoryInterface {
    override fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit)
    override fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit)
    override fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit)
    override fun clearCache()
}