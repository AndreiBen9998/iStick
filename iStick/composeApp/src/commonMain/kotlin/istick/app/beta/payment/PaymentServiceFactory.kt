// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/payment/PaymentServiceFactory.kt
package istick.app.beta.payment

/**
 * Factory for creating PaymentService instances
 */
object PaymentServiceFactory {
    /**
     * Create a new PaymentService instance
     */
    fun createPaymentService(): PaymentService {
        return MySqlPaymentService()
    }
}