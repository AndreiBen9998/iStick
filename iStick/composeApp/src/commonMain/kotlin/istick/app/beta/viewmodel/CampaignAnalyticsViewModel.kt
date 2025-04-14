// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/CampaignAnalyticsViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.model.Campaign
import istick.app.beta.model.User
import istick.app.beta.repository.CampaignRepository
import istick.app.beta.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Campaign Analytics
 */
class CampaignAnalyticsViewModel(
    private val campaignRepository: CampaignRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    // UI state for current user
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // UI state for campaigns
    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    // Analytics data state flows
    private val _totalApplications = MutableStateFlow(0)
    val totalApplications: StateFlow<Int> = _totalApplications.asStateFlow()

    private val _approvedCars = MutableStateFlow(0)
    val approvedCars: StateFlow<Int> = _approvedCars.asStateFlow()

    private val _totalMileage = MutableStateFlow(0)
    val totalMileage: StateFlow<Int> = _totalMileage.asStateFlow()

    private val _estimatedViews = MutableStateFlow(0)
    val estimatedViews: StateFlow<Int> = _estimatedViews.asStateFlow()

    // UI state for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI state for errors
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Time range for analytics
    private val _timeRange = MutableStateFlow("week")
    val timeRange: StateFlow<String> = _timeRange.asStateFlow()

    /**
     * Load user and campaign data
     */
    fun loadAnalytics() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Load current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.fold(
                    onSuccess = { user ->
                        _user.value = user

                        // If user is a brand, load their campaigns
                        if (user != null && user.type == istick.app.beta.model.UserType.BRAND) {
                            loadBrandCampaigns(user.id)
                        } else {
                            // Not a brand user
                            _isLoading.value = false
                        }
                    },
                    onFailure = { error ->
                        _error.value = "Failed to load user: ${error.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error loading data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load campaigns for a specific brand
     */
    private suspend fun loadBrandCampaigns(brandId: String) {
        try {
            val campaignsResult = campaignRepository.fetchBrandCampaigns(brandId)
            campaignsResult.fold(
                onSuccess = { campaigns ->
                    _campaigns.value = campaigns
                    calculateAnalytics(campaigns)
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _error.value = "Failed to load campaigns: ${error.message}"
                    _isLoading.value = false
                }
            )
        } catch (e: Exception) {
            _error.value = "Error loading campaigns: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Calculate analytics from campaign data
     */
    private fun calculateAnalytics(campaigns: List<Campaign>) {
        val activeCampaigns = campaigns.filter { it.status == istick.app.beta.model.CampaignStatus.ACTIVE }
        
        // For each campaign, sum up the applications and approved applicants
        val totalApps = activeCampaigns.sumOf { it.applicants.size }
        val totalApproved = activeCampaigns.sumOf { it.approvedApplicants.size }
        
        _totalApplications.value = totalApps
        _approvedCars.value = totalApproved
        
        // For now, we're using mock data for mileage and views
        // In a real implementation, these would come from the backend
        _totalMileage.value = totalApproved * 500 // Assume 500km per car on average
        _estimatedViews.value = _totalMileage.value * 10 // Assume 10 views per km
    }

    /**
     * Set the time range for analytics
     */
    fun setTimeRange(range: String) {
        _timeRange.value = range
        // In a real implementation, this would trigger a reload of analytics with the new time range
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}