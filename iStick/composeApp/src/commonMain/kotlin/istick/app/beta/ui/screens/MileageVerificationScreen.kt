import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import istick.app.beta.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MileageVerificationState(
    val isLoading: Boolean = false,
    val imageUri: String? = null,
    val detectedMileage: Int? = null,
    val isVerified: Boolean = false,
    val error: String? = null
)

class MileageVerificationViewModel(
    private val carRepository: CarRepository
) : ViewModel() {
    // State for the verification process
    private val _state = MutableStateFlow(MileageVerificationState())
    val state: StateFlow<MileageVerificationState> = _state.asStateFlow()

    // Current car being verified
    private val _car = MutableStateFlow<Car?>(null)
    val car: StateFlow<Car?> = _car.asStateFlow()

    // Verification code (would be generated on server in real app)
    private val _verificationCode = MutableStateFlow("")
    val verificationCode: StateFlow<String> = _verificationCode.asStateFlow()

    // Load car details
    fun loadCarDetails(carId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            carRepository.getCar(carId).fold(
                onSuccess = { car ->
                    _car.value = car
                    _state.value = _state.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Error loading car: ${error.message}"
                    )
                }
            )
        }
    }

    // Generate verification code
    fun generateVerificationCode() {
        // In a real app, this would be generated on the server
        val code = (100000 + Random.nextInt(900000)).toString()
        _verificationCode.value = code
    }

    // Process captured image
    fun processImage(imageBytes: ByteArray) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                // In a real app, this would use OCR to detect the mileage
                // For demo purposes, let's simulate OCR with a random increase in mileage
                val currentMileage = _car.value?.currentMileage ?: 0
                val detectedMileage = currentMileage + Random.nextInt(100, 500)

                // Create a simulated image URI
                val imageUri = "file://simulated/verification_${System.currentTimeMillis()}.jpg"

                _state.value = _state.value.copy(
                    isLoading = false,
                    imageUri = imageUri,
                    detectedMileage = detectedMileage
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error processing image: ${e.message}"
                )
            }
        }
    }

    // Reset captured image
    fun resetCapturedImage() {
        _state.value = _state.value.copy(
            imageUri = null,
            detectedMileage = null
        )
    }

    // Submit verification
    fun submitVerification(carId: String) {
        viewModelScope.launch {
            val detectedMileage = _state.value.detectedMileage ?: return@launch
            val imageUri = _state.value.imageUri ?: return@launch

            _state.value = _state.value.copy(isLoading = true)

            val verification = MileageVerification(
                timestamp = System.currentTimeMillis(),
                mileage = detectedMileage,
                photoUrl = imageUri,
                verificationCode = _verificationCode.value,
                isVerified = true  // In a real app, this would be verified by the server
            )

            carRepository.addMileageVerification(carId, verification).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isVerified = true
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Error submitting verification: ${error.message}"
                    )
                }
            )
        }
    }
}