// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/payment/MySqlPaymentService.kt
package istick.app.beta.payment

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class MySqlPaymentService : PaymentService {
    private val TAG = "MySqlPaymentService"

    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    override val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods

    private val _pendingPayments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    override val pendingPayments: StateFlow<List<PaymentTransaction>> = _pendingPayments

    private val _completedPayments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    override val completedPayments: StateFlow<List<PaymentTransaction>> = _completedPayments

    override suspend fun fetchPaymentMethods(userId: String): Result<List<PaymentMethod>> =
        withContext(Dispatchers.IO) {
            try {
                // In a real implementation, this would fetch from a payment_methods table
                // For now, we'll return a mock list
                val mockMethods = listOf(
                    PaymentMethod(
                        id = "1",
                        type = PaymentMethodType.BANK_TRANSFER,
                        title = "My Bank Account",
                        lastFour = "1234",
                        isDefault = true
                    ),
                    PaymentMethod(
                        id = "2",
                        type = PaymentMethodType.CREDIT_CARD,
                        title = "Visa Card",
                        lastFour = "5678",
                        isDefault = false
                    )
                )

                _paymentMethods.value = mockMethods
                Result.success(mockMethods)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun addPaymentMethod(userId: String, paymentMethod: PaymentMethod): Result<PaymentMethod> =
        withContext(Dispatchers.IO) {
            try {
                // In a real implementation, this would insert into a payment_methods table
                // For now, we'll simply add to our in-memory list
                val newId = (_paymentMethods.value.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
                val newMethod = paymentMethod.copy(id = newId.toString())

                val updatedMethods = if (newMethod.isDefault) {
                    // If this is default, update other methods
                    _paymentMethods.value.map { it.copy(isDefault = false) } + newMethod
                } else {
                    // Otherwise just add it
                    _paymentMethods.value + newMethod
                }

                _paymentMethods.value = updatedMethods
                Result.success(newMethod)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun removePaymentMethod(userId: String, paymentMethodId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val currentMethods = _paymentMethods.value
                val methodToRemove = currentMethods.find { it.id == paymentMethodId }

                if (methodToRemove == null) {
                    return@withContext Result.failure(Exception("Payment method not found"))
                }

                val wasDefault = methodToRemove.isDefault
                val updatedMethods = currentMethods.filter { it.id != paymentMethodId }

                // If we removed the default method, set a new default
                if (wasDefault && updatedMethods.isNotEmpty()) {
                    val withNewDefault = updatedMethods.mapIndexed { index, method ->
                        if (index == 0) method.copy(isDefault = true) else method
                    }
                    _paymentMethods.value = withNewDefault
                } else {
                    _paymentMethods.value = updatedMethods
                }

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val currentMethods = _paymentMethods.value
                if (currentMethods.none { it.id == paymentMethodId }) {
                    return@withContext Result.failure(Exception("Payment method not found"))
                }

                val updatedMethods = currentMethods.map { method ->
                    method.copy(isDefault = method.id == paymentMethodId)
                }

                _paymentMethods.value = updatedMethods
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun processPayment(payment: PaymentTransaction): Result<PaymentTransaction> =
        withContext(Dispatchers.IO) {
            try {
                // In a real implementation, this would insert into a payments table
                // For now, we'll simulate a successful payment
                val processingPayment = payment.copy(
                    id = System.currentTimeMillis().toString(),
                    status = PaymentStatus.PROCESSING
                )

                // Simulate processing delay
                kotlinx.coroutines.delay(1000)

                // Complete the payment
                val completedPayment = processingPayment.copy(
                    status = PaymentStatus.COMPLETED,
                    updatedAt = System.currentTimeMillis()
                )

                // Update state
                _completedPayments.value = _completedPayments.value + completedPayment

                Result.success(completedPayment)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // Implement other methods similarly
    override suspend fun fetchPaymentHistory(userId: String): Result<List<PaymentTransaction>> =
        withContext(Dispatchers.IO) {
            try {
                // Mock implementation
                val pendingList = listOf(
                    PaymentTransaction(
                        id = "p1",
                        campaignId = "1",
                        carOwnerId = "101",
                        brandId = userId,
                        amount = 100.0,
                        currency = "RON",
                        status = PaymentStatus.PENDING,
                        createdAt = System.currentTimeMillis() - 86400000,
                        updatedAt = System.currentTimeMillis() - 86400000,
                        paymentMethod = _paymentMethods.value.firstOrNull()
                    )
                )

                val completedList = listOf(
                    PaymentTransaction(
                        id = "c1",
                        campaignId = "2",
                        carOwnerId = "102",
                        brandId = userId,
                        amount = 200.0,
                        currency = "RON",
                        status = PaymentStatus.COMPLETED,
                        createdAt = System.currentTimeMillis() - 172800000,
                        updatedAt = System.currentTimeMillis() - 172700000,
                        paymentMethod = _paymentMethods.value.firstOrNull()
                    )
                )

                _pendingPayments.value = pendingList
                _completedPayments.value = completedList

                Result.success(pendingList + completedList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun fetchCampaignPayments(campaignId: String): Result<List<PaymentTransaction>> =
        withContext(Dispatchers.IO) {
            try {
                // Filter payments by campaign ID
                val allPayments = _pendingPayments.value + _completedPayments.value
                val campaignPayments = allPayments.filter { it.campaignId == campaignId }
                Result.success(campaignPayments)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun fetchPaymentDetails(paymentId: String): Result<PaymentTransaction> =
        withContext(Dispatchers.IO) {
            try {
                val allPayments = _pendingPayments.value + _completedPayments.value
                val payment = allPayments.find { it.id == paymentId }

                if (payment != null) {
                    Result.success(payment)
                } else {
                    Result.failure(Exception("Payment not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun requestRefund(paymentId: String, reason: String): Result<PaymentTransaction> =
        withContext(Dispatchers.IO) {
            try {
                val allPayments = _pendingPayments.value + _completedPayments.value
                val payment = allPayments.find { it.id == paymentId }

                if (payment == null) {
                    return@withContext Result.failure(Exception("Payment not found"))
                }

                if (payment.status != PaymentStatus.COMPLETED) {
                    return@withContext Result.failure(Exception("Only completed payments can be refunded"))
                }

                // Process refund
                val refundedPayment = payment.copy(
                    status = PaymentStatus.REFUNDED,
                    notes = payment.notes + "\nRefund reason: $reason",
                    updatedAt = System.currentTimeMillis()
                )

                // Update state
                _completedPayments.value = _completedPayments.value.map {
                    if (it.id == paymentId) refundedPayment else it
                }

                Result.success(refundedPayment)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observePaymentStatus(paymentId: String): Flow<PaymentStatus> {
        val flow = MutableStateFlow(PaymentStatus.PENDING)

        // Find current status
        val allPayments = _pendingPayments.value + _completedPayments.value
        val payment = allPayments.find { it.id == paymentId }

        if (payment != null) {
            flow.value = payment.status
        }

        return flow
    }
}