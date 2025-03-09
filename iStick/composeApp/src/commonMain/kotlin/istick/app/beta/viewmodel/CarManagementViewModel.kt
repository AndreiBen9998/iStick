// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/CarManagementViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import istick.app.beta.repository.CarRepository
import istick.app.beta.storage.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CarManagementViewModel(
    private val carRepository: CarRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {
    // Current car being viewed/edited
    private val _currentCar = MutableStateFlow<Car?>(null)
    val currentCar: StateFlow<Car?> = _currentCar
    
    // All user cars
    private val _cars = MutableStateFlow<List<Car>>(emptyList())
    val cars: StateFlow<List<Car>> = _cars
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage
    
    // Function to load user's cars
    fun loadCars(userId: String) {
        _isLoading.value = true
        _error.value = null
        
        carRepository.fetchUserCars(userId).fold(
            onSuccess = { carsList ->
                _cars.value = carsList
                _isLoading.value = false
            },
            onFailure = { error ->
                _error.value = "Error loading cars: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to load specific car details
    fun loadCar(carId: String) {
        _isLoading.value = true
        _error.value = null
        
        carRepository.getCar(carId).fold(
            onSuccess = { car ->
                _currentCar.value = car
                _isLoading.value = false
            },
            onFailure = { error ->
                _error.value = "Error loading car details: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to add a new car
    fun addCar(
        make: String,
        model: String,
        year: Int,
        color: String,
        licensePlate: String,
        currentMileage: Int
    ) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null
        
        val newCar = Car(
            make = make,
            model = model,
            year = year,
            color = color,
            licensePlate = licensePlate,
            currentMileage = currentMileage
        )
        
        carRepository.addCar(newCar).fold(
            onSuccess = { car ->
                _currentCar.value = car
                _cars.value = _cars.value + car
                _successMessage.value = "Car added successfully"
                _isLoading.value = false
            },
            onFailure = { error ->
                _error.value = "Failed to add car: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to update an existing car
    fun updateCar(
        carId: String,
        make: String,
        model: String,
        year: Int,
        color: String,
        licensePlate: String,
        currentMileage: Int
    ) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null
        
        val existingCar = _currentCar.value ?: return
        
        val updatedCar = existingCar.copy(
            make = make,
            model = model,
            year = year,
            color = color,
            licensePlate = licensePlate,
            currentMileage = currentMileage
        )
        
        carRepository.updateCar(updatedCar).fold(
            onSuccess = { car ->
                _currentCar.value = car
                _cars.value = _cars.value.map { if (it.id == carId) car else it }
                _successMessage.value = "Car updated successfully"
                _isLoading.value = false
            },
            onFailure = { error ->
                _error.value = "Failed to update car: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to delete a car
    fun deleteCar(carId: String) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null
        
        carRepository.deleteCar(carId).fold(
            onSuccess = { success ->
                if (success) {
                    _cars.value = _cars.value.filter { it.id != carId }
                    if (_currentCar.value?.id == carId) {
                        _currentCar.value = null
                    }
                    _successMessage.value = "Car deleted successfully"
                } else {
                    _error.value = "Failed to delete car"
                }
                _isLoading.value = false
            },
            onFailure = { error ->
                _error.value = "Failed to delete car: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to upload a car photo
    fun uploadCarPhoto(carId: String, imageBytes: ByteArray) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null
        
        val fileName = "car_${carId}_${System.currentTimeMillis()}.jpg"
        
        storageRepository.uploadImage(imageBytes, "cars/$fileName").fold(
            onSuccess = { imageUrl ->
                // Get current car
                val currentCar = _currentCar.value ?: return@fold
                
                // Add photo to car
                val updatedCar = currentCar.copy(
                    photos = currentCar.photos + imageUrl
                )
                
                // Update car
                carRepository.updateCar(updatedCar).fold(
                    onSuccess = { car ->
                        _currentCar.value = car
                        _cars.value = _cars.value.map { if (it.id == carId) car else it }
                        _successMessage.value = "Photo uploaded successfully"
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = "Failed to update car with new photo: ${error.message}"
                        _isLoading.value = false
                    }
                )
            },
            onFailure = { error ->
                _error.value = "Failed to upload photo: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to clear error and success messages
    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}