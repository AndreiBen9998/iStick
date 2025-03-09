// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/CampaignDetailViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.model.Campaign
import istick.app.beta.model.Car
import istick.app.beta.model.CampaignApplication
import istick.app.beta.repository.CarRepository
import istick.app.beta.repository.CampaignRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CampaignDetailViewModel(
    private val campaignRepository: CampaignRepository,
    private val carRepository: CarRepository
) : ViewModel() {
    // Campaign details
    private val _campaign = MutableStateFlow<Campaign?>(null)
    val campaign: StateFlow<Campaign?> = _campaign

    // User's application for this campaign
    private val _application = MutableStateFlow<CampaignApplication?>(null)
    val application: StateFlow<CampaignApplication?> = _application

    // User's cars
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    val userCars: StateFlow<List<Car>> = _userCars

    // Selected car for application
    private val _selectedCarId = MutableStateFlow<String?>(null)
    val selectedCarId: StateFlow<String?> = _selectedCarId

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Function to load campaign details
    fun loadCampaignDetails(campaignId: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = campaignRepository.fetchCampaignDetails(campaignId)
                result.fold(
                    onSuccess = { fetchedCampaign ->
                        _campaign.value = fetchedCampaign
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        _error.value = "Error loading campaign: ${exception.toString()}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error loading campaign: ${e.toString()}"
                _isLoading.value = false
            }
        }
    }

    // Function to load user's cars
    fun loadUserCars(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = carRepository.fetchUserCars(userId)
                result.fold(
                    onSuccess = { cars ->
                        _userCars.value = cars
                        if (cars.isNotEmpty() && _selectedCarId.value == null) {
                            _selectedCarId.value = cars.first().id
                        }
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        _error.value = "Error loading cars: ${exception.toString()}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error loading cars: ${e.toString()}"
                _isLoading.value = false
            }
        }
    }

    // Function to select a car
    fun selectCar(carId: String) {
        _selectedCarId.value = carId
    }

    // Function to apply to campaign
    fun applyToCampaign() {
        _isLoading.value = true
        _error.value = null

        val campaignId = _campaign.value?.id
        val carId = _selectedCarId.value

        if (campaignId == null || carId == null) {
            _error.value = "Missing campaign or car information"
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                val result = campaignRepository.applyCampaign(campaignId, carId)
                result.fold(
                    onSuccess = { application ->
                        _application.value = application
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        _error.value = "Failed to apply to campaign: ${exception.toString()}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error applying to campaign: ${e.toString()}"
                _isLoading.value = false
            }
        }
    }
}