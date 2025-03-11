// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/RegistrationViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.Brand
import istick.app.beta.model.CarOwner
import istick.app.beta.model.CompanyDetails
import istick.app.beta.model.UserType
import istick.app.beta.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    // Registration form fields for both user types
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    // Car owner specific fields
    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    private val _dailyDrivingDistance = MutableStateFlow(0)
    val dailyDrivingDistance: StateFlow<Int> = _dailyDrivingDistance.asStateFlow()

    // Brand specific fields
    private val _companyName = MutableStateFlow("")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _industry = MutableStateFlow("")
    val industry: StateFlow<String> = _industry.asStateFlow()

    private val _website = MutableStateFlow("")
    val website: StateFlow<String> = _website.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    // Selected user type
    private val _userType = MutableStateFlow(UserType.CAR_OWNER)
    val userType: StateFlow<UserType> = _userType.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Registration step
    private val _registrationStep = MutableStateFlow(RegistrationStep.SELECT_TYPE)
    val registrationStep: StateFlow<RegistrationStep> = _registrationStep.asStateFlow()

    // Success state
    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    // Validation results
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    // Update form fields
    fun updateEmail(value: String) {
        _email.value = value
        validateEmail()
    }

    fun updatePassword(value: String) {
        _password.value = value
        validatePassword()
    }

    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
        validatePassword()
    }

    fun updateName(value: String) {
        _name.value = value
        _nameError.value = if (value.isBlank()) "Name is required" else null
    }

    fun updateCity(value: String) {
        _city.value = value
    }

    fun updateDailyDrivingDistance(value: Int) {
        _dailyDrivingDistance.value = value
    }

    fun updateCompanyName(value: String) {
        _companyName.value = value
    }

    fun updateIndustry(value: String) {
        _industry.value = value
    }

    fun updateWebsite(value: String) {
        _website.value = value
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun selectUserType(type: UserType) {
        _userType.value = type
        // Move to next step after selecting user type
        _registrationStep.value = RegistrationStep.ACCOUNT_DETAILS
    }

    fun moveToAccountDetails() {
        _registrationStep.value = RegistrationStep.ACCOUNT_DETAILS
    }

    fun moveToUserDetails() {
        // Validate account details before moving to user details
        if (validateAccountDetails()) {
            _registrationStep.value = RegistrationStep.USER_DETAILS
        }
    }

    // Validation functions
    private fun validateEmail(): Boolean {
        val emailValue = _email.value
        return when {
            emailValue.isBlank() -> {
                _emailError.value = "Email is required"
                false
            }
            !emailValue.contains('@') || !emailValue.contains('.') -> {
                _emailError.value = "Invalid email format"
                false
            }
            else -> {
                _emailError.value = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val passwordValue = _password.value
        val confirmValue = _confirmPassword.value

        return when {
            passwordValue.isBlank() -> {
                _passwordError.value = "Password is required"
                false
            }
            passwordValue.length < 6 -> {
                _passwordError.value = "Password must be at least 6 characters"
                false
            }
            passwordValue != confirmValue -> {
                _passwordError.value = "Passwords do not match"
                false
            }
            else -> {
                _passwordError.value = null
                true
            }
        }
    }

    private fun validateAccountDetails(): Boolean {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isNameValid = _name.value.isNotBlank().also {
            _nameError.value = if (!it) "Name is required" else null
        }

        return isEmailValid && isPasswordValid && isNameValid
    }

    private fun validateUserDetails(): Boolean {
        return when (_userType.value) {
            UserType.CAR_OWNER -> {
                // For car owners, city is required
                _city.value.isNotBlank()
            }
            UserType.BRAND -> {
                // For brands, company name and industry are required
                _companyName.value.isNotBlank() && _industry.value.isNotBlank()
            }
        }
    }

    // Register user
    fun register() {
        if (!validateAccountDetails() || !validateUserDetails()) {
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // 1. Create authentication account
                val signUpResult = authRepository.signUp(_email.value, _password.value)

                signUpResult.fold(
                    onSuccess = { userId ->
                        // 2. Create user profile based on user type
                        createUserProfile(userId).fold(
                            onSuccess = {
                                _registrationSuccess.value = true
                                _isLoading.value = false
                            },
                            onFailure = { exception ->
                                _error.value = "Error creating profile: ${exception.message}"
                                _isLoading.value = false
                            }
                        )
                    },
                    onFailure = { exception ->
                        _error.value = "Registration failed: ${exception.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Registration failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun createUserProfile(userId: String): Result<Unit> {
        return try {
            val result = when (_userType.value) {
                UserType.CAR_OWNER -> {
                    val carOwner = CarOwner(
                        id = userId,
                        email = _email.value,
                        name = _name.value,
                        city = _city.value,
                        dailyDrivingDistance = _dailyDrivingDistance.value
                    )
                    userRepository.createUser(_email.value, _name.value, UserType.CAR_OWNER)
                }
                UserType.BRAND -> {
                    val brand = Brand(
                        id = userId,
                        email = _email.value,
                        name = _name.value,
                        companyDetails = CompanyDetails(
                            companyName = _companyName.value,
                            industry = _industry.value,
                            website = _website.value,
                            description = _description.value
                        )
                    )
                    userRepository.createUser(_email.value, _name.value, UserType.BRAND)
                }
            }

            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetError() {
        _error.value = null
    }

    fun goBack() {
        _registrationStep.value = when (_registrationStep.value) {
            RegistrationStep.SELECT_TYPE -> RegistrationStep.SELECT_TYPE
            RegistrationStep.ACCOUNT_DETAILS -> RegistrationStep.SELECT_TYPE
            RegistrationStep.USER_DETAILS -> RegistrationStep.ACCOUNT_DETAILS
        }
    }

    // Registration steps
    enum class RegistrationStep {
        SELECT_TYPE,
        ACCOUNT_DETAILS,
        USER_DETAILS
    }
}