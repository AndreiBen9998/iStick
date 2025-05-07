// commonMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/MySqlOffersRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Campaign

expect class MySqlOffersRepository() : OffersRepositoryInterface {
    override suspend fun getOffers(onSuccess: (List<Campaign>) -> Unit, onError: (Exception) -> Unit)
    override suspend fun getNextOffersPage(onSuccess: (List<Campaign>, Boolean) -> Unit, onError: (Exception) -> Unit)
    override suspend fun getOfferDetails(offerId: String, onSuccess: (Campaign) -> Unit, onError: (Exception) -> Unit)
    override fun clearCache()
}