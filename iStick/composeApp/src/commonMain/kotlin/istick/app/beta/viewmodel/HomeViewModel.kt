// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/HomeViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.model.Campaign
import istick.app.beta.model.User
import istick.app.beta.model.UserType
import istick.app.beta.repository.OptimizedOffersRepository
import istick.app.beta.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val offersRepository: OptimizedOffersRepository = OptimizedOffersRepository(),
    private val userRepository: UserRepository
) : ViewModel() {
    // UI state for campaigns
    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadUser()
        loadCampaigns()
    }

    /**
     * Load the current user
     */
    private fun loadUser() {
        viewModelScope.launch {
            try {
                val result = userRepository.getCurrentUser()
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                    },
                    onFailure = { error ->
                        _error.value = "Failed to load user: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error loading user: ${e.message}"
            }
        }
    }

    /**
     * Load active campaigns
     */
    fun loadCampaigns() {
        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null

        // Check user type to determine what to load
        val userType = _currentUser.value?.type

        offersRepository.getOffers(
            onSuccess = { campaignList ->
                if (userType == UserType.BRAND) {
                    // Brands see their own campaigns
                    _campaigns.value = campaignList.filter { it.brandId == _currentUser.value?.id }
                } else {
                    // Car owners see available campaigns
                    _campaigns.value = campaignList
                }
                _isLoading.value = false
            },
            onError = { error ->
                _error.value = "Failed to load campaigns: ${error.message}"
                _isLoading.value = false
            }
        )
    }

    /**
     * Load the next page of campaigns
     */
    fun loadNextPage() {
        if (_isLoading.value) return

        _isLoading.value = true

        offersRepository.getNextOffersPage(
            onSuccess = { newCampaigns, hasMore ->
                val userType = _currentUser.value?.type
                if (userType == UserType.BRAND) {
                    // Filter for brand's own campaigns
                    val filteredNewCampaigns = newCampaigns.filter { it.brandId == _currentUser.value?.id }
                    _campaigns.value = _campaigns.value + filteredNewCampaigns
                } else {
                    // Show all campaigns to car owners
                    _campaigns.value = _campaigns.value + newCampaigns
                }
                _isLoading.value = false
            },
            onError = { error ->
                _error.value = "Failed to load more campaigns: ${error.message}"
                _isLoading.value = false
            }
        )
    }

    /**
     * Refresh campaigns
     */
    fun refresh() {
        offersRepository.clearCache()
        loadCampaigns()
    }
}