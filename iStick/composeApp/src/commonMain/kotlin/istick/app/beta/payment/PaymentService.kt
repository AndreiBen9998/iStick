// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/payment/PaymentService.kt
package istick.app.beta.payment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Payment status enum representing the possible states of a payment
 */
enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}

/**
 * Payment method enum
 */
enum class PaymentMethodType {
    CREDIT_CARD,
    BANK_TRANSFER,
    PAYPAL,
    REVOLUT
}

/**
 * Data class representing a payment method
 */
data class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val title: String,
    val lastFour: String = "",
    val isDefault: Boolean = false
)

/**
 * Data class representing a payment transaction
 */
data class PaymentTransaction(
    val id: String,
    val campaignId: String,
    val carOwnerId: String,
    val brandId: String,
    val amount: Double,
    val currency: String,
    val status: PaymentStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val paymentMethod: PaymentMethod? = null,
    val notes: String = ""
)

/**
 * Interface for payment service
 */
interface PaymentService {
    val paymentMethods: StateFlow<List<PaymentMethod>>
    val pendingPayments: StateFlow<List<PaymentTransaction>>
    val completedPayments: StateFlow<List<PaymentTransaction>>
    
    // Get a list of saved payment methods
    suspend fun fetchPaymentMethods(userId: String): Result<List<PaymentMethod>>
    
    // Add a new payment method
    suspend fun addPaymentMethod(userId: String, paymentMethod: PaymentMethod): Result<PaymentMethod>
    
    // Remove a payment method
    suspend fun removePaymentMethod(userId: String, paymentMethodId: String): Result<Boolean>
    
    // Set a payment method as default
    suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String): Result<Boolean>
    
    // Process a payment from brand to car owner
    suspend fun processPayment(payment: PaymentTransaction): Result<PaymentTransaction>
    
    // Get payment history for a user (both sent and received)
    suspend fun fetchPaymentHistory(userId: String): Result<List<PaymentTransaction>>
    
    // Get payments for a specific campaign
    suspend fun fetchCampaignPayments(campaignId: String): Result<List<PaymentTransaction>>
    
    // Get payment details
    suspend fun fetchPaymentDetails(paymentId: String): Result<PaymentTransaction>
    
    // Request a refund
    suspend fun requestRefund(paymentId: String, reason: String): Result<PaymentTransaction>
    
    // Observe payment status changes
    fun observePaymentStatus(paymentId: String): Flow<PaymentStatus>
}
