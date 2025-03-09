
// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/ProfileViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.User
import istick.app.beta.model.Car
import istick.app.beta.repository.CarRepository
import istick.app.beta.repository.UserRepository
import istick.app.beta.storage.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val carRepository: CarRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {
    // Current user
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user
    
    // User's cars (if car owner)
    private val _cars = MutableStateFlow<List<Car>>(emptyList())
    val cars: StateFlow<List<Car>> = _cars
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadProfile()
    }
    
    // Function to load profile
    fun loadProfile() {
        _isLoading.value = true
        _error.value = null
        
        userRepository.getCurrentUser().fold(
            onSuccess = { user ->
                _user.value = user
                
                // If car owner, load cars
                if (user?.type == istick.app.beta.model.UserType.CAR_OWNER) {
                    loadCars(user.id)
                }
                
                _isLoading.value = false
            },
            onFailure = { error ->
                _error.value = "Failed to load profile: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to load cars
    private fun loadCars(userId: String) {
        carRepository.fetchUserCars(userId).fold(
            onSuccess = { carsList ->
                _cars.value = carsList
            },
            onFailure = { error ->
                _error.value = "Failed to load cars: ${error.message}"
            }
        )
    }
    
    // Function to upload profile picture
    fun uploadProfilePicture(imageBytes: ByteArray) {
        _isLoading.value = true
        _error.value = null
        
        val userId = _user.value?.id ?: return
        val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
        
        storageRepository.uploadImage(imageBytes, "profiles/$fileName").fold(
            onSuccess = { imageUrl ->
                userRepository.updateUserProfilePicture(userId, imageUrl).fold(
                    onSuccess = { updatedUser ->
                        _user.value = updatedUser
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = "Failed to update profile picture: ${error.message}"
                        _isLoading.value = false
                    }
                )
            },
            onFailure = { error ->
                _error.value = "Failed to upload image: ${error.message}"
                _isLoading.value = false
            }
        )
    }
    
    // Function to sign out
    fun signOut(onComplete: () -> Unit) {
        _isLoading.value = true
        
        try {
            authRepository.signOut()
            onComplete()
        } catch (e: Exception) {
            _error.value = "Failed to sign out: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
}