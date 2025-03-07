// În repository/OptimizedOffersRepository.kt
class OptimizedOffersRepository {
    private val db = Firebase.firestore
    private val cache = mutableMapOf<String, Offer>()
    private val _cachedOffers = MutableStateFlow<List<Offer>>(emptyList())
    val cachedOffers: StateFlow<List<Offer>> = _cachedOffers

    private var lastVisibleOffer: DocumentSnapshot? = null
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    // Stocăm timestamp-ul ultimei actualizări pentru a preveni actualizările frecvente
    private var lastRefreshTimestamp = 0L

    fun getOffers(onSuccess: (List<Offer>) -> Unit, onError: (Exception) -> Unit) {
        // Verifică mai întâi cache-ul - strategie Netflix: cache-first pentru încărcare rapidă
        if (_cachedOffers.value.isNotEmpty()) {
            onSuccess(_cachedOffers.value)

            // Actualizează în fundal pentru următoarea solicitare dacă au trecut cel puțin 5 minute
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRefreshTimestamp > 5 * 60 * 1000) {
                refreshOffersInBackground()
                lastRefreshTimestamp = currentTime
            }
            return
        }

        // Limitează câmpurile pentru lista principală
        db.collection("offers")
            .orderBy("price")
            .limit(20)
            .get()
            .addOnSuccessListener { result ->
                val offers = result.documents.mapNotNull { doc ->
                    doc.toObject(Offer::class.java)?.copy(id = doc.id)
                }
                // Populează cache-ul
                offers.forEach { offer ->
                    cache[offer.id] = offer
                }
                _cachedOffers.value = offers
                lastRefreshTimestamp = System.currentTimeMillis()
                onSuccess(offers)

                // Salvează ultimul document pentru paginare
                lastVisibleOffer = if (result.documents.isNotEmpty()) {
                    result.documents.last()
                } else {
                    null
                }

                _hasMorePages.value = result.documents.size >= 20
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    private fun refreshOffersInBackground() {
        db.collection("offers")
            .orderBy("price")
            .limit(20)
            .get()
            .addOnSuccessListener { result ->
                val offers = result.documents.mapNotNull { doc ->
                    doc.toObject(Offer::class.java)?.copy(id = doc.id)
                }
                offers.forEach { offer ->
                    cache[offer.id] = offer
                }
                _cachedOffers.value = offers

                lastVisibleOffer = if (result.documents.isNotEmpty()) {
                    result.documents.last()
                } else {
                    null
                }

                _hasMorePages.value = result.documents.size >= 20
            }
    }

    fun getNextOffersPage(onSuccess: (List<Offer>, Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (lastVisibleOffer == null || !_hasMorePages.value) {
            onSuccess(emptyList(), false)
            return
        }

        db.collection("offers")
            .orderBy("price")
            .startAfter(lastVisibleOffer)
            .limit(20)
            .get()
            .addOnSuccessListener { result ->
                val newOffers = result.documents.mapNotNull { doc ->
                    doc.toObject(Offer::class.java)?.copy(id = doc.id)
                }

                // Actualizează cache-ul
                newOffers.forEach { offer ->
                    cache[offer.id] = offer
                }

                // Actualizează lastVisibleOffer pentru următoarea pagină
                lastVisibleOffer = if (result.documents.isNotEmpty()) {
                    result.documents.last()
                } else {
                    null
                }

                val hasMore = result.documents.size >= 20
                _hasMorePages.value = hasMore

                onSuccess(newOffers, hasMore)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun getOfferDetails(offerId: String, onSuccess: (Offer) -> Unit, onError: (Exception) -> Unit) {
        // Verifică mai întâi cache-ul
        cache[offerId]?.let {
            onSuccess(it)

            // Actualizează în fundal pentru date proaspete
            refreshOfferInBackground(offerId)
            return
        }

        db.collection("offers").document(offerId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Offer::class.java)?.let {
                    val offer = it.copy(id = document.id)
                    cache[offerId] = offer
                    onSuccess(offer)
                } ?: onError(Exception("Offer not found"))
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    private fun refreshOfferInBackground(offerId: String) {
        db.collection("offers").document(offerId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Offer::class.java)?.let {
                    cache[offerId] = it.copy(id = document.id)
                }
            }
    }

    // Funcție pentru a șterge cache-ul în cazul unei erori sau la cerere
    fun clearCache() {
        cache.clear()
        _cachedOffers.value = emptyList()
        lastVisibleOffer = null
        _hasMorePages.value = true
    }
}