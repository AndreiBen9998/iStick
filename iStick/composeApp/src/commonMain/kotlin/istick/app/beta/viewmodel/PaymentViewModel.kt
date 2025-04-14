// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/viewmodel/PaymentViewModel.kt
package istick.app.beta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import istick.app.beta.auth.AuthRepository
import istick.app.beta.payment.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for payment functionality
 */
class PaymentViewModel(
    private val paymentService: PaymentService,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Payment method form fields
    private val _methodType = MutableStateFlow(PaymentMethodType.BANK_TRANSFER)
    val methodType: StateFlow<PaymentMethodType> = _methodType.asStateFlow()
    
    private val _methodTitle = MutableStateFlow("")
    val methodTitle: StateFlow<String> = _methodTitle.asStateFlow()
    
    private val _accountNumber = MutableStateFlow("")
    val accountNumber: StateFlow<String> = _accountNumber.asStateFlow()
    
    private val _makeDefault = MutableStateFlow(false)
    val makeDefault: StateFlow<Boolean> = _makeDefault.asStateFlow()
    
    // Payment history
    val paymentMethods = paymentService.paymentMethods
    val pendingPayments = paymentService.pendingPayments
    val completedPayments = paymentService.completedPayments
    
    // Currently selected payment
    private val _selectedPayment = MutableStateFlow<PaymentTransaction?>(null)
    val selectedPayment: StateFlow<PaymentTransaction?> = _selectedPayment.asStateFlow()
    
    // New payment details
    private val _paymentAmount = MutableStateFlow(0.0)
    val paymentAmount: StateFlow<Double> = _paymentAmount.asStateFlow()
    
    private val _paymentCurrency = MutableStateFlow("RON")
    val paymentCurrency: StateFlow<String> = _paymentCurrency.asStateFlow()
    
    private val _selectedMethodId = MutableStateFlow<String?>(null)
    val selectedMethodId: StateFlow<String?> = _selectedMethodId.asStateFlow()
    
    private val _paymentNotes = MutableStateFlow("")
    val paymentNotes: StateFlow<String> = _paymentNotes.asStateFlow()
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    /**
     * Initialize by loading payment methods and history
     */
    fun initialize() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            loadPaymentMethods(userId)
            loadPaymentHistory(userId)
        } else {
            _error.value = "User not logged in"
        }
    }
    
    /**
     * Load payment methods for the current user
     */
    fun loadPaymentMethods(userId: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            paymentService.fetchPaymentMethods(userId).fold(
                onSuccess = {
                    // If there are payment methods and none selected, select the default one
                    if (_selectedMethodId.value == null && it.isNotEmpty()) {
                        _selectedMethodId.value = it.find { method -> method.isDefault }?.id
                            ?: it.first().id
                    }
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to load payment methods: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Load payment history for the current user
     */
    fun loadPaymentHistory(userId: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            paymentService.fetchPaymentHistory(userId).fold(
                onSuccess = {
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to load payment history: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Add a new payment method
     */
    fun addPaymentMethod() {
        val userId = authRepository.getCurrentUserId() ?: run {
            _error.value = "User not logged in"
            return
        }
        
        if (_methodTitle.value.isBlank()) {
            _error.value = "Please enter a title for the payment method"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        // For bank transfers, we need the account number
        // For credit cards, this would be the last 4 digits
        val lastFour = when (_methodType.value) {
            PaymentMethodType.BANK_TRANSFER -> {
                if (_accountNumber.value.length < 4) {
                    _error.value = "Invalid account number"
                    _isLoading.value = false
                    return
                }
                _accountNumber.value.takeLast(4)
            }
            else -> ""
        }
        
        val newMethod = PaymentMethod(
            id = "", // Will be set by the server
            type = _methodType.value,
            title = _methodTitle.value,
            lastFour = lastFour,
            isDefault = _makeDefault.value
        )
        
        viewModelScope.launch {
            paymentService.addPaymentMethod(userId, newMethod).fold(
                onSuccess = { method ->
                    _successMessage.value = "Payment method added successfully"
                    
                    // Select this method if it's the first one or set as default
                    if (_selectedMethodId.value == null || method.isDefault) {
                        _selectedMethodId.value = method.id
                    }
                    
                    // Reset form
                    _methodTitle.value = ""
                    _accountNumber.value = ""
                    _makeDefault.value = false
                    
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to add payment method: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Remove a payment method
     */
    fun removePaymentMethod(methodId: String) {
        val userId = authRepository.getCurrentUserId() ?: run {
            _error.value = "User not logged in"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            paymentService.removePaymentMethod(userId, methodId).fold(
                onSuccess = { success ->
                    if (success) {
                        _successMessage.value = "Payment method removed successfully"
                        
                        // If this was the selected method, select another one
                        if (_selectedMethodId.value == methodId) {
                            _selectedMethodId.value = paymentMethods.value.firstOrNull { it.id != methodId }?.id
                        }
                    } else {
                        _error.value = "Failed to remove payment method"
                    }
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to remove payment method: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Set a payment method as default
     */
    fun setDefaultPaymentMethod(methodId: String) {
        val userId = authRepository.getCurrentUserId() ?: run {
            _error.value = "User not logged in"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            paymentService.setDefaultPaymentMethod(userId, methodId).fold(
                onSuccess = { success ->
                    if (success) {
                        _successMessage.value = "Default payment method updated successfully"
                    } else {
                        _error.value = "Failed to update default payment method"
                    }
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to update default payment method: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Process a payment
     */
    fun processPayment(campaignId: String, carOwnerId: String) {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _error.value = "User not logged in"
            return
        }
        
        if (_paymentAmount.value <= 0) {
            _error.value = "Please enter a valid payment amount"
            return
        }
        
        if (_selectedMethodId.value == null) {
            _error.value = "Please select a payment method"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        // Get the selected payment method
        val paymentMethod = paymentMethods.value.find { it.id == _selectedMethodId.value }
        
        if (paymentMethod == null) {
            _error.value = "Selected payment method not found"
            _isLoading.value = false
            return
        }
        
        // Create payment transaction
        val payment = PaymentTransaction(
            id = "", // Will be set by the server
            campaignId = campaignId,
            carOwnerId = carOwnerId,
            brandId = userId,
            amount = _paymentAmount.value,
            currency = _paymentCurrency.value,
            status = PaymentStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            paymentMethod = paymentMethod,
            notes = _paymentNotes.value
        )
        
        viewModelScope.launch {
            paymentService.processPayment(payment).fold(
                onSuccess = { completedPayment ->
                    _successMessage.value = "Payment processed successfully"
                    
                    // Reset form
                    _paymentAmount.value = 0.0
                    _paymentNotes.value = ""
                    
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to process payment: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Get payment details
     */
    fun loadPaymentDetails(paymentId: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            paymentService.fetchPaymentDetails(paymentId).fold(
                onSuccess = { payment ->
                    _selectedPayment.value = payment
                    _isLoading.value = false
                    
                    // Observe payment status changes
                    launch {
                        paymentService.observePaymentStatus(paymentId).collectLatest { status ->
                            _selectedPayment.value = _selectedPayment.value?.copy(status = status)
                        }
                    }
                },
                onFailure = { e ->
                    _error.value = "Failed to load payment details: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Request a refund
     */
    fun requestRefund(paymentId: String, reason: String) {
        if (reason.isBlank()) {
            _error.value = "Please provide a reason for the refund"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            paymentService.requestRefund(paymentId, reason).fold(
                onSuccess = { refundedPayment ->
                    _successMessage.value = "Refund requested successfully"
                    _selectedPayment.value = refundedPayment
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = "Failed to request refund: ${e.message}"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Update payment method type
     */
    fun updateMethodType(type: PaymentMethodType) {
        _methodType.value = type
    }
    
    /**
     * Update payment method title
     */
    fun updateMethodTitle(title: String) {
        _methodTitle.value = title
    }
    
    /**
     * Update account number
     */
    fun updateAccountNumber(accountNumber: String) {
        _accountNumber.value = accountNumber
    }
    
    /**
     * Update make default flag
     */
    fun updateMakeDefault(makeDefault: Boolean) {
        _makeDefault.value = makeDefault
    }
    
    /**
     * Update payment amount
     */
    fun updatePaymentAmount(amount: Double) {
        _paymentAmount.value = amount
    }
    
    /**
     * Update payment currency
     */
    fun updatePaymentCurrency(currency: String) {
        _paymentCurrency.value = currency
    }
    
    /**
     * Update selected payment method
     */
    fun updateSelectedMethodId(methodId: String?) {
        _selectedMethodId.value = methodId
    }
    
    /**
     * Update payment notes
     */
    fun updatePaymentNotes(notes: String) {
        _paymentNotes.value = notes
    }
}