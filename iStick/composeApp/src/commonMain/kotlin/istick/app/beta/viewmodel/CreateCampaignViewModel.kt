// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/CampaignCreationViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.model.*
import istick.app.beta.repository.CampaignRepository
import istick.app.beta.storage.StorageRepository
import istick.app.beta.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing campaign creation workflow
 */
class CampaignCreationViewModel(
    private val campaignRepository: CampaignRepository,
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    // Step 1: Basic Information
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow(0.0)
    val amount: StateFlow<Double> = _amount.asStateFlow()

    private val _currency = MutableStateFlow("RON")
    val currency: StateFlow<String> = _currency.asStateFlow()

    // Step 2: Sticker Details
    private val _stickerImageUrl = MutableStateFlow("")
    val stickerImageUrl: StateFlow<String> = _stickerImageUrl.asStateFlow()

    private val _stickerWidth = MutableStateFlow(0)
    val stickerWidth: StateFlow<Int> = _stickerWidth.asStateFlow()

    private val _stickerHeight = MutableStateFlow(0)
    val stickerHeight: StateFlow<Int> = _stickerHeight.asStateFlow()

    private val _stickerPositions = MutableStateFlow<List<StickerPosition>>(listOf())
    val stickerPositions: StateFlow<List<StickerPosition>> = _stickerPositions.asStateFlow()

    private val _deliveryMethod = MutableStateFlow(DeliveryMethod.CENTER)
    val deliveryMethod: StateFlow<DeliveryMethod> = _deliveryMethod.asStateFlow()

    // Step 3: Targeting
    private val _minDailyDistance = MutableStateFlow(0)
    val minDailyDistance: StateFlow<Int> = _minDailyDistance.asStateFlow()

    private val _cities = MutableStateFlow<List<String>>(listOf())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()

    private val _carMakes = MutableStateFlow<List<String>>(listOf())
    val carMakes: StateFlow<List<String>> = _carMakes.asStateFlow()

    private val _carModels = MutableStateFlow<List<String>>(listOf())
    val carModels: StateFlow<List<String>> = _carModels.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentStep = MutableStateFlow(Step.BASIC_INFO)
    val currentStep: StateFlow<Step> = _currentStep.asStateFlow()

    // Step enum
    enum class Step {
        BASIC_INFO,
        STICKER_DETAILS,
        TARGETING,
        REVIEW
    }

    // Initialize with default values
    init {
        _stickerWidth = MutableStateFlow(20)
        _stickerHeight = MutableStateFlow(10)
        _minDailyDistance = MutableStateFlow(20)
        _stickerPositions.value = listOf(StickerPosition.DOOR_LEFT)
    }

    // Step 1: Basic Information Methods
    fun updateTitle(title: String) {
        _title.value = title
    }

    fun updateDescription(description: String) {
        _description.value = description
    }

    fun updateAmount(amount: Double) {
        _amount.value = amount
    }

    fun updateCurrency(currency: String) {
        _currency.value = currency
    }

    // Step 2: Sticker Details Methods
    fun updateStickerWidth(width: Int) {
        _stickerWidth.value = width
    }

    fun updateStickerHeight(height: Int) {
        _stickerHeight.value = height
    }

    fun toggleStickerPosition(position: StickerPosition) {
        val positions = _stickerPositions.value.toMutableList()
        if (positions.contains(position)) {
            positions.remove(position)
        } else {
            positions.add(position)
        }

        // Ensure at least one position is selected
        if (positions.isNotEmpty()) {
            _stickerPositions.value = positions
        }
    }

    fun updateDeliveryMethod(method: DeliveryMethod) {
        _deliveryMethod.value = method
    }

    // Step 3: Targeting Methods
    fun updateMinDailyDistance(distance: Int) {
        _minDailyDistance.value = distance
    }

    fun addCity(city: String) {
        if (city.isNotBlank() && !_cities.value.contains(city)) {
            _cities.value = _cities.value + city
        }
    }

    fun removeCity(city: String) {
        _cities.value = _cities.value.filter { it != city }
    }

    fun addCarMake(make: String) {
        if (make.isNotBlank() && !_carMakes.value.contains(make)) {
            _carMakes.value = _carMakes.value + make
        }
    }

    fun removeCarMake(make: String) {
        _carMakes.value = _carMakes.value.filter { it != make }
    }

    fun addCarModel(model: String) {
        if (model.isNotBlank() && !_carModels.value.contains(model)) {
            _carModels.value = _carModels.value + model
        }
    }

    fun removeCarModel(model: String) {
        _carModels.value = _carModels.value.filter { it != model }
    }

    // Navigation Methods
    fun nextStep() {
        if (validateCurrentStep()) {
            _currentStep.value = when (_currentStep.value) {
                Step.BASIC_INFO -> Step.STICKER_DETAILS
                Step.STICKER_DETAILS -> Step.TARGETING
                Step.TARGETING -> Step.REVIEW
                Step.REVIEW -> Step.REVIEW // No next step
            }
        }
    }

    fun previousStep() {
        _currentStep.value = when (_currentStep.value) {
            Step.BASIC_INFO -> Step.BASIC_INFO // No previous step
            Step.STICKER_DETAILS -> Step.BASIC_INFO
            Step.TARGETING -> Step.STICKER_DETAILS
            Step.REVIEW -> Step.TARGETING
        }
    }

    // Validation Methods
    private fun validateCurrentStep(): Boolean {
        return when (_currentStep.value) {
            Step.BASIC_INFO -> validateBasicInfo()
            Step.STICKER_DETAILS -> validateStickerDetails()
            Step.TARGETING -> validateTargeting()
            Step.REVIEW -> true // Nothing to validate on review
        }
    }

    private fun validateBasicInfo(): Boolean {
        if (_title.value.isBlank()) {
            _error.value = "Please enter a campaign title"
            return false
        }

        if (_description.value.isBlank()) {
            _error.value = "Please enter a campaign description"
            return false
        }

        if (_amount.value <= 0) {
            _error.value = "Please enter a valid payment amount"
            return false
        }

        return true
    }

    private fun validateStickerDetails(): Boolean {
        if (_stickerWidth.value <= 0 || _stickerHeight.value <= 0) {
            _error.value = "Please enter valid sticker dimensions"
            return false
        }

        if (_stickerPositions.value.isEmpty()) {
            _error.value = "Please select at least one sticker position"
            return false
        }

        return true
    }

    private fun validateTargeting(): Boolean {
        if (_cities.value.isEmpty()) {
            _error.value = "Please add at least one target city"
            return false
        }

        return true
    }

    // Image Upload Methods
    fun uploadStickerImage(imageBytes: ByteArray) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val fileName = "sticker_${System.currentTimeMillis()}.jpg"
                val result = storageRepository.uploadImage(imageBytes, fileName)

                result.fold(
                    onSuccess = { url ->
                        _stickerImageUrl.value = url
                        _isLoading.value = false
                    },
                    onFailure = { e ->
                        _error.value = "Failed to upload image: ${e.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error uploading image: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Campaign Creation Method
    fun createCampaign(onSuccess: (String) -> Unit) {
        if (!validateAll()) {
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Get brand ID from current user
                val brandId = authRepository.getCurrentUserId() ?: run {
                    _error.value = "You must be logged in to create a campaign"
                    _isLoading.value = false
                    return@launch
                }

                // Create campaign object
                val campaign = Campaign(
                    // ID will be generated by the backend
                    brandId = brandId,
                    title = _title.value,
                    description = _description.value,
                    stickerDetails = StickerDetails(
                        imageUrl = _stickerImageUrl.value,
                        width = _stickerWidth.value,
                        height = _stickerHeight.value,
                        positions = _stickerPositions.value,
                        deliveryMethod = _deliveryMethod.value
                    ),
                    payment = PaymentDetails(
                        amount = _amount.value,
                        currency = _currency.value,
                        paymentFrequency = PaymentFrequency.MONTHLY, // Hard-coded for now
                        paymentMethod = PaymentMethod.BANK_TRANSFER // Hard-coded for now
                    ),
                    requirements = CampaignRequirements(
                        minDailyDistance = _minDailyDistance.value,
                        cities = _cities.value,
                        carMakes = _carMakes.value,
                        carModels = _carModels.value
                    ),
                    status = CampaignStatus.ACTIVE
                )

                // Use mock call for now, need to extend the repository with createCampaign method
                // This would typically call the backend API
                val result = mockCreateCampaign(campaign)

                result.fold(
                    onSuccess = { createdCampaign ->
                        _isLoading.value = false
                        onSuccess(createdCampaign.id)
                    },
                    onFailure = { e ->
                        _error.value = "Failed to create campaign: ${e.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error creating campaign: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Mock method for campaign creation
    // In a real implementation, this would call the repository
    private suspend fun mockCreateCampaign(campaign: Campaign): Result<Campaign> {
        return try {
            // In a real implementation, we'd call the repository here
            // Simulate network delay
            kotlinx.coroutines.delay(1000)

            // Create a copy with a generated ID
            val createdCampaign = campaign.copy(
                id = System.currentTimeMillis().toString()
            )

            Result.success(createdCampaign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Validate all fields before final submission
    private fun validateAll(): Boolean {
        if (!validateBasicInfo()) {
            _currentStep.value = Step.BASIC_INFO
            return false
        }

        if (!validateStickerDetails()) {
            _currentStep.value = Step.STICKER_DETAILS
            return false
        }

        if (!validateTargeting()) {
            _currentStep.value = Step.TARGETING
            return false
        }

        return true
    }

    // Clear error message
    fun clearError() {
        _error.value = null
    }
}