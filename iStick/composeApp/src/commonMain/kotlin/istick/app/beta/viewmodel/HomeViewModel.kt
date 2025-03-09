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
            userRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    _currentUser.value = user
                },
                onFailure = { error ->
                    _error.value = "Failed to load user: ${error.message}"
                }
            )
        }
    }

    /**
     * Load active campaigns
     */
    fun loadCampaigns() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Check user type to determine what to load
            val userType = _currentUser.value?.type

            if (userType == UserType.CAR_OWNER || userType == null) {
                // Car owners see available campaigns
                offersRepository.getOffers(
                    onSuccess = { campaignList ->
                        _campaigns.value = campaignList
                        _isLoading.value = false
                    },
                    onError = { error ->
                        _error.value = "Failed to load campaigns: ${error.message}"
                        _isLoading.value = false
                    }
                )
            } else if (userType == UserType.BRAND) {
                // Brands see their own campaigns
                offersRepository.getOffers(
                    onSuccess = { campaignList ->
                        _campaigns.value = campaignList.filter { it.brandId == _currentUser.value?.id }
                        _isLoading.value = false
                    },
                    onError = { error ->
                        _error.value = "Failed to load your campaigns: ${error.message}"
                        _isLoading.value = false
                    }
                )
            }
        }
    }

    /**
     * Load the next page of campaigns
     */
    fun loadNextPage() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true

            offersRepository.getNextOffersPage(
                onSuccess = { newCampaigns, hasMore ->
                    _campaigns.value = _campaigns.value + newCampaigns
                    _isLoading.value = false
                },
                onError = { error ->
                    _error.value = "Failed to load more campaigns: ${error.message}"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Refresh campaigns
     */
    fun refresh() {
        offersRepository.clearCache()
        loadCampaigns()
    }
}