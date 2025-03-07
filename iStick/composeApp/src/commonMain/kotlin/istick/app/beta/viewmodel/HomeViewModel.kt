// În viewmodel/HomeViewModel.kt
class HomeViewModel(
    private val offersRepository: OptimizedOffersRepository
) : ViewModel() {
    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingNextPage = MutableStateFlow(false)
    val isLoadingNextPage: StateFlow<Boolean> = _isLoadingNextPage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    fun loadOffers() {
        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null

        offersRepository.getOffers(
            onSuccess = { offersList ->
                _offers.value = offersList
                _isLoading.value = false
            },
            onError = { e ->
                _error.value = e.message
                _isLoading.value = false
            }
        )
    }

    fun loadNextPage() {
        if (_isLoadingNextPage.value || !_hasMorePages.value) return

        _isLoadingNextPage.value = true

        offersRepository.getNextOffersPage(
            onSuccess = { newOffers, hasMore ->
                val currentList = _offers.value.toMutableList()
                currentList.addAll(newOffers)
                _offers.value = currentList
                _hasMorePages.value = hasMore
                _isLoadingNextPage.value = false
            },
            onError = { e ->
                _error.value = e.message
                _isLoadingNextPage.value = false
            }
        )
    }

    fun refreshOffers() {
        offersRepository.clearCache()
        loadOffers()
    }
}