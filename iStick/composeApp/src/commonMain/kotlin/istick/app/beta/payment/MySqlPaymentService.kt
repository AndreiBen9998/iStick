// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/payment/MySqlPaymentService.kt
package istick.app.beta.payment

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.database.DatabaseTransactionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MySQL implementation of PaymentService
 */
class MySqlPaymentService : PaymentService {
    private val TAG = "MySqlPaymentService"

    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    override val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods

    private val _pendingPayments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    override val pendingPayments: StateFlow<List<PaymentTransaction>> = _pendingPayments

    private val _completedPayments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    override val completedPayments: StateFlow<List<PaymentTransaction>> = _completedPayments

    // Map to store payment status observers
    private val paymentStatusFlows = mutableMapOf<String, MutableStateFlow<PaymentStatus>>()

    override suspend fun fetchPaymentMethods(userId: String): Result<List<PaymentMethod>> = withContext(Dispatchers.IO) {
        try {
            val methods = DatabaseHelper.executeQuery(
                """
                SELECT * FROM payment_methods 
                WHERE user_id = ?
                ORDER BY is_default DESC, created_at DESC
                """,
                listOf(userId.toLong())
            ) { resultSet ->
                val methodsList = mutableListOf<PaymentMethod>()
                while (resultSet.next()) {
                    methodsList.add(
                        PaymentMethod(
                            id = resultSet.getLong("id").toString(),
                            type = PaymentMethodType.valueOf(resultSet.getString("method_type") ?: "BANK_TRANSFER"),
                            title = resultSet.getString("title") ?: "",
                            lastFour = resultSet.getString("last_four") ?: "",
                            isDefault = resultSet.getBoolean("is_default")
                        )
                    )
                }
                methodsList
            }

            _paymentMethods.value = methods
            Result.success(methods)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payment methods: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addPaymentMethod(userId: String, paymentMethod: PaymentMethod): Result<PaymentMethod> = withContext(Dispatchers.IO) {
        val connection = DatabaseTransactionHelper.beginTransaction()
        try {
            // If this is the first payment method for the user, make it default
            val isFirstMethod = DatabaseTransactionHelper.executeQueryWithConnection(
                connection,
                "SELECT COUNT(*) FROM payment_methods WHERE user_id = ?",
                listOf(userId.toLong())
            ) { rs ->
                if (rs.next()) rs.getInt(1) == 0 else true
            }

            // Insert the payment method
            val methodId = DatabaseTransactionHelper.executeInsertWithConnection(
                connection,
                """
                INSERT INTO payment_methods (
                    user_id, method_type, title, last_four, is_default, created_at
                ) VALUES (?, ?, ?, ?, ?, NOW())
                """,
                listOf(
                    userId.toLong(),
                    paymentMethod.type.name,
                    paymentMethod.title,
                    paymentMethod.lastFour,
                    isFirstMethod || paymentMethod.isDefault
                )
            )

            // If this is set as default, update other methods to non-default
            if (paymentMethod.isDefault || isFirstMethod) {
                DatabaseTransactionHelper.executeUpdateWithConnection(
                    connection,
                    """
                    UPDATE payment_methods 
                    SET is_default = FALSE
                    WHERE user_id = ? AND id != ?
                    """,
                    listOf(userId.toLong(), methodId)
                )
            }

            DatabaseTransactionHelper.commitTransaction(connection)

            // Create the new payment method with generated ID
            val newMethod = paymentMethod.copy(
                id = methodId.toString(),
                isDefault = isFirstMethod || paymentMethod.isDefault
            )

            // Update state
            _paymentMethods.value = _paymentMethods.value + newMethod

            Result.success(newMethod)
        } catch (e: Exception) {
            DatabaseTransactionHelper.rollbackTransaction(connection)
            Log.e(TAG, "Error adding payment method: ${e.message}", e)
            Result.failure(e)
        } finally {
            DatabaseTransactionHelper.closeConnection(connection)
        }
    }

    override suspend fun removePaymentMethod(userId: String, paymentMethodId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val connection = DatabaseTransactionHelper.beginTransaction()
        try {
            // Check if method exists and belongs to user
            val exists = DatabaseTransactionHelper.executeQueryWithConnection(
                connection,
                "SELECT id FROM payment_methods WHERE id = ? AND user_id = ?",
                listOf(paymentMethodId.toLong(), userId.toLong())
            ) { rs -> rs.next() }

            if (!exists) {
                return@withContext Result.failure(Exception("Payment method not found or doesn't belong to user"))
            }

            // Check if it's the default method
            val isDefault = DatabaseTransactionHelper.executeQueryWithConnection(
                connection,
                "SELECT is_default FROM payment_methods WHERE id = ?",
                listOf(paymentMethodId.toLong())
            ) { rs -> if (rs.next()) rs.getBoolean("is_default") else false }

            // Delete the payment method
            DatabaseTransactionHelper.executeUpdateWithConnection(
                connection,
                "DELETE FROM payment_methods WHERE id = ?",
                listOf(paymentMethodId.toLong())
            )

            // If it was the default method, set a new default
            if (isDefault) {
                DatabaseTransactionHelper.executeUpdateWithConnection(
                    connection,
                    """
                    UPDATE payment_methods
                    SET is_default = TRUE
                    WHERE user_id = ?
                    ORDER BY created_at DESC
                    LIMIT 1
                    """,
                    listOf(userId.toLong())
                )
            }

            DatabaseTransactionHelper.commitTransaction(connection)

            // Update state
            _paymentMethods.value = _paymentMethods.value.filter { it.id != paymentMethodId }

            Result.success(true)
        } catch (e: Exception) {
            DatabaseTransactionHelper.rollbackTransaction(connection)
            Log.e(TAG, "Error removing payment method: ${e.message}", e)
            Result.failure(e)
        } finally {
            DatabaseTransactionHelper.closeConnection(connection)
        }
    }

    override suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val connection = DatabaseTransactionHelper.beginTransaction()
        try {
            // Check if method exists and belongs to user
            val exists = DatabaseTransactionHelper.executeQueryWithConnection(
                connection,
                "SELECT id FROM payment_methods WHERE id = ? AND user_id = ?",
                listOf(paymentMethodId.toLong(), userId.toLong())
            ) { rs -> rs.next() }

            if (!exists) {
                return@withContext Result.failure(Exception("Payment method not found or doesn't belong to user"))
            }

            // Set all methods to non-default
            DatabaseTransactionHelper.executeUpdateWithConnection(
                connection,
                "UPDATE payment_methods SET is_default = FALSE WHERE user_id = ?",
                listOf(userId.toLong())
            )

            // Set the selected method as default
            DatabaseTransactionHelper.executeUpdateWithConnection(
                connection,
                "UPDATE payment_methods SET is_default = TRUE WHERE id = ?",
                listOf(paymentMethodId.toLong())
            )

            DatabaseTransactionHelper.commitTransaction(connection)

            // Update state
            _paymentMethods.value = _paymentMethods.value.map {
                it.copy(isDefault = it.id == paymentMethodId)
            }

            Result.success(true)
        } catch (e: Exception) {
            DatabaseTransactionHelper.rollbackTransaction(connection)
            Log.e(TAG, "Error setting default payment method: ${e.message}", e)
            Result.failure(e)
        } finally {
            DatabaseTransactionHelper.closeConnection(connection)
        }
    }

    override suspend fun processPayment(payment: PaymentTransaction): Result<PaymentTransaction> = withContext(Dispatchers.IO) {
        val connection = DatabaseTransactionHelper.beginTransaction()
        try {
            // Insert payment record with PROCESSING status
            val processingPayment = payment.copy(status = PaymentStatus.PROCESSING)

            val paymentId = DatabaseTransactionHelper.executeInsertWithConnection(
                connection,
                """
                INSERT INTO payments (
                    campaign_id, car_owner_id, brand_id, amount, currency,
                    status, created_at, updated_at, payment_method_id, notes
                ) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?)
                """,
                listOf(
                    processingPayment.campaignId.toLong(),
                    processingPayment.carOwnerId.toLong(),
                    processingPayment.brandId.toLong(),
                    processingPayment.amount,
                    processingPayment.currency,
                    processingPayment.status.name,
                    processingPayment.paymentMethod?.id?.toLongOrNull() ?: 0,
                    processingPayment.notes
                )
            )

            // In a real implementation, there would be integration with a payment gateway here
            // For now, we'll simulate payment processing and mark it as completed

            // Update payment status to COMPLETED
            DatabaseTransactionHelper.executeUpdateWithConnection(
                connection,
                """
                UPDATE payments
                SET status = ?, updated_at = NOW()
                WHERE id = ?
                """,
                listOf(PaymentStatus.COMPLETED.name, paymentId)
            )

            DatabaseTransactionHelper.commitTransaction(connection)

            // Create completed payment object
            val completedPayment = payment.copy(
                id = paymentId.toString(),
                status = PaymentStatus.COMPLETED,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Update payment status flow if observed
            paymentStatusFlows[paymentId.toString()]?.value = PaymentStatus.COMPLETED

            // Update state
            _completedPayments.value = _completedPayments.value + completedPayment

            Result.success(completedPayment)
        } catch (e: Exception) {
            DatabaseTransactionHelper.rollbackTransaction(connection)
            Log.e(TAG, "Error processing payment: ${e.message}", e)
            Result.failure(e)
        } finally {
            DatabaseTransactionHelper.closeConnection(connection)
        }
    }

    override suspend fun fetchPaymentHistory(userId: String): Result<List<PaymentTransaction>> = withContext(Dispatchers.IO) {
        try {
            val payments = DatabaseHelper.executeQuery(
                """
                SELECT p.*, pm.method_type, pm.title, pm.last_four
                FROM payments p
                LEFT JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.car_owner_id = ? OR p.brand_id = ?
                ORDER BY p.created_at DESC
                """,
                listOf(userId.toLong(), userId.toLong())
            ) { resultSet ->
                val paymentsList = mutableListOf<PaymentTransaction>()
                while (resultSet.next()) {
                    paymentsList.add(resultSetToPaymentTransaction(resultSet))
                }
                paymentsList
            }

            // Update state flows
            val pendingPayments = payments.filter {
                it.status == PaymentStatus.PENDING || it.status == PaymentStatus.PROCESSING
            }
            val completedPayments = payments.filter {
                it.status == PaymentStatus.COMPLETED || it.status == PaymentStatus.REFUNDED
            }

            _pendingPayments.value = pendingPayments
            _completedPayments.value = completedPayments

            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payment history: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchCampaignPayments(campaignId: String): Result<List<PaymentTransaction>> = withContext(Dispatchers.IO) {
        try {
            val payments = DatabaseHelper.executeQuery(
                """
                SELECT p.*, pm.method_type, pm.title, pm.last_four
                FROM payments p
                LEFT JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.campaign_id = ?
                ORDER BY p.created_at DESC
                """,
                listOf(campaignId.toLong())
            ) { resultSet ->
                val paymentsList = mutableListOf<PaymentTransaction>()
                while (resultSet.next()) {
                    paymentsList.add(resultSetToPaymentTransaction(resultSet))
                }
                paymentsList
            }

            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaign payments: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchPaymentDetails(paymentId: String): Result<PaymentTransaction> = withContext(Dispatchers.IO) {
        try {
            val payment = DatabaseHelper.executeQuery(
                """
                SELECT p.*, pm.method_type, pm.title, pm.last_four
                FROM payments p
                LEFT JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.id = ?
                """,
                listOf(paymentId.toLong())
            ) { resultSet ->
                if (resultSet.next()) {
                    resultSetToPaymentTransaction(resultSet)
                } else {
                    null
                }
            }

            if (payment != null) {
                Result.success(payment)
            } else {
                Result.failure(Exception("Payment not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payment details: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun requestRefund(paymentId: String, reason: String): Result<PaymentTransaction> = withContext(Dispatchers.IO) {
        val connection = DatabaseTransactionHelper.beginTransaction()
        try {
            // Check if payment exists and is completed
            val payment = DatabaseTransactionHelper.executeQueryWithConnection(
                connection,
                """
                SELECT p.*, pm.method_type, pm.title, pm.last_four
                FROM payments p
                LEFT JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.id = ? AND p.status = ?
                """,
                listOf(paymentId.toLong(), PaymentStatus.COMPLETED.name)
            ) { resultSet ->
                if (resultSet.next()) {
                    resultSetToPaymentTransaction(resultSet)
                } else {
                    null
                }
            }

            if (payment == null) {
                return@withContext Result.failure(Exception("Payment not found or not eligible for refund"))
            }

            // Update payment status to REFUNDED
            DatabaseTransactionHelper.executeUpdateWithConnection(
                connection,
                """
                UPDATE payments
                SET status = ?, notes = CONCAT(notes, '\nRefund reason: ', ?), updated_at = NOW()
                WHERE id = ?
                """,
                listOf(PaymentStatus.REFUNDED.name, reason, paymentId.toLong())
            )

            DatabaseTransactionHelper.commitTransaction(connection)

            // Create refunded payment object
            val refundedPayment = payment.copy(
                status = PaymentStatus.REFUNDED,
                notes = payment.notes + "\nRefund reason: $reason",
                updatedAt = System.currentTimeMillis()
            )

            // Update payment status flow if observed
            paymentStatusFlows[paymentId]?.value = PaymentStatus.REFUNDED

            // Update state
            _completedPayments.value = _completedPayments.value.map {
                if (it.id == paymentId) refundedPayment else it
            }

            Result.success(refundedPayment)
        } catch (e: Exception) {
            DatabaseTransactionHelper.rollbackTransaction(connection)
            Log.e(TAG, "Error requesting refund: ${e.message}", e)
            Result.failure(e)
        } finally {
            DatabaseTransactionHelper.closeConnection(connection)
        }
    }

    override fun observePaymentStatus(paymentId: String): Flow<PaymentStatus> {
        return paymentStatusFlows.getOrPut(paymentId) {
            MutableStateFlow(PaymentStatus.PENDING).also { flow ->
                // Fetch current status in background
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val status = DatabaseHelper.executeQuery(
                            "SELECT status FROM payments WHERE id = ?",
                            listOf(paymentId.toLong())
                        ) { rs ->
                            if (rs.next()) {
                                PaymentStatus.valueOf(rs.getString("status") ?: PaymentStatus.PENDING.name)
                            } else {
                                PaymentStatus.PENDING
                            }
                        }
                        flow.value = status
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching payment status: ${e.message}", e)
                    }
                }
            }
        }
    }

    /**
     * Helper method to convert ResultSet to PaymentTransaction
     */
    private fun resultSetToPaymentTransaction(resultSet: java.sql.ResultSet): PaymentTransaction {
        val paymentId = resultSet.getLong("id").toString()
        val campaignId = resultSet.getLong("campaign_id").toString()
        val carOwnerId = resultSet.getLong("car_owner_id").toString()
        val brandId = resultSet.getLong("brand_id").toString()
        val amount = resultSet.getDouble("amount")
        val currency = resultSet.getString("currency") ?: "RON"
        val statusStr = resultSet.getString("status") ?: PaymentStatus.PENDING.name
        val createdAt = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
        val updatedAt = resultSet.getTimestamp("updated_at")?.time ?: System.currentTimeMillis()
        val notes = resultSet.getString("notes") ?: ""

        // Payment method details
        val paymentMethodId = resultSet.getLong("payment_method_id")
        val methodType = resultSet.getString("method_type")
        val methodTitle = resultSet.getString("title")
        val lastFour = resultSet.getString("last_four")

        val paymentMethod = if (methodType != null && methodTitle != null) {
            PaymentMethod(
                id = paymentMethodId.toString(),
                type = try { PaymentMethodType.valueOf(methodType) } catch (e: Exception) { PaymentMethodType.BANK_TRANSFER },
                title = methodTitle,
                lastFour = lastFour ?: "",
                isDefault = false // Not relevant in this context
            )
        } else {
            null
        }

        return PaymentTransaction(
            id = paymentId,
            campaignId = campaignId,
            carOwnerId = carOwnerId,
            brandId = brandId,
            amount = amount,
            currency = currency,
            status = try { PaymentStatus.valueOf(statusStr) } catch (e: Exception) { PaymentStatus.PENDING },
            createdAt = createdAt,
            updatedAt = updatedAt,
            paymentMethod = paymentMethod,
            notes = notes
        )
    }
}