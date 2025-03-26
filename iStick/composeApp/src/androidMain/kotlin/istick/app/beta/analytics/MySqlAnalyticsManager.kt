// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/analytics/MySqlAnalyticsManager.kt
package istick.app.beta.analytics

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Campaign
import istick.app.beta.model.Car
import istick.app.beta.model.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MySQL implementation of Analytics Manager
 * Stores analytics events in the database for later analysis
 */
class MySqlAnalyticsManager : AnalyticsManager {
    private val TAG = "MySqlAnalyticsManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private var userId: String? = null
    private var userType: UserType? = null
    private val userProperties = mutableMapOf<String, String>()
    
    override fun setUserProperties(userId: String, userType: UserType, properties: Map<String, String>) {
        this.userId = userId
        this.userType = userType
        userProperties.putAll(properties)
        
        // Record this information in the database
        scope.launch {
            try {
                // First check if user already has properties
                val count = DatabaseHelper.executeQuery(
                    "SELECT COUNT(*) FROM user_analytics WHERE user_id = ?",
                    listOf(userId.toLong())
                ) { resultSet ->
                    if (resultSet.next()) resultSet.getInt(1) else 0
                }
                
                if (count > 0) {
                    // Update existing record
                    DatabaseHelper.executeUpdate(
                        """
                        UPDATE user_analytics 
                        SET user_type = ?, properties = ?, updated_at = NOW()
                        WHERE user_id = ?
                        """,
                        listOf(
                            userType.name,
                            Json.encodeToString(properties),
                            userId.toLong()
                        )
                    )
                } else {
                    // Insert new record
                    DatabaseHelper.executeInsert(
                        """
                        INSERT INTO user_analytics 
                        (user_id, user_type, properties, created_at, updated_at)
                        VALUES (?, ?, ?, NOW(), NOW())
                        """,
                        listOf(
                            userId.toLong(),
                            userType.name,
                            Json.encodeToString(properties)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording user properties: ${e.message}", e)
            }
        }
    }
    
    override fun trackScreenView(screenName: String, screenClass: String) {
        logAnalyticsEvent(
            eventType = "screen_view",
            properties = mapOf(
                "screen_name" to screenName,
                "screen_class" to screenClass
            )
        )
    }
    
    override fun trackRegistration(userType: UserType, method: String) {
        logAnalyticsEvent(
            eventType = "registration",
            properties = mapOf(
                "user_type" to userType.name,
                "method" to method
            )
        )
    }
    
    override fun trackLogin(method: String) {
        logAnalyticsEvent(
            eventType = "login",
            properties = mapOf("method" to method)
        )
    }
    
    override fun trackCarAdded(car: Car) {
        logAnalyticsEvent(
            eventType = "car_added",
            properties = mapOf(
                "car_id" to car.id,
                "make" to car.make,
                "model" to car.model,
                "year" to car.year.toString()
            )
        )
    }
    
    override fun trackMileageVerification(car: Car, newMileage: Int, verificationMethod: String) {
        logAnalyticsEvent(
            eventType = "mileage_verification",
            properties = mapOf(
                "car_id" to car.id,
                "make" to car.make,
                "model" to car.model,
                "previous_mileage" to car.currentMileage.toString(),
                "new_mileage" to newMileage.toString(),
                "method" to verificationMethod
            )
        )
    }
    
    override fun trackCampaignView(campaign: Campaign) {
        logAnalyticsEvent(
            eventType = "campaign_view",
            properties = mapOf(
                "campaign_id" to campaign.id,
                "brand_id" to campaign.brandId,
                "title" to campaign.title
            )
        )
    }
    
    override fun trackCampaignApplication(campaign: Campaign, car: Car) {
        logAnalyticsEvent(
            eventType = "campaign_application",
            properties = mapOf(
                "campaign_id" to campaign.id,
                "brand_id" to campaign.brandId,
                "car_id" to car.id,
                "car_make" to car.make,
                "car_model" to car.model
            )
        )
    }
    
    override fun trackCampaignCreated(campaign: Campaign) {
        logAnalyticsEvent(
            eventType = "campaign_created",
            properties = mapOf(
                "campaign_id" to campaign.id,
                "brand_id" to campaign.brandId,
                "title" to campaign.title,
                "payment_amount" to campaign.payment.amount.toString(),
                "payment_currency" to campaign.payment.currency
            )
        )
    }
    
    override fun trackApplicationReview(campaignId: String, approved: Boolean) {
        logAnalyticsEvent(
            eventType = "application_review",
            properties = mapOf(
                "campaign_id" to campaignId,
                "approved" to approved.toString()
            )
        )
    }
    
    override fun trackAdRevenue(campaignId: String, amount: Double, currency: String) {
        logAnalyticsEvent(
            eventType = "ad_revenue",
            properties = mapOf(
                "campaign_id" to campaignId,
                "amount" to amount.toString(),
                "currency" to currency
            )
        )
    }
    
    override fun trackError(errorType: String, errorMessage: String, screenName: String?) {
        val properties = mutableMapOf(
            "error_type" to errorType,
            "error_message" to errorMessage
        )
        
        if (screenName != null) {
            properties["screen_name"] = screenName
        }
        
        logAnalyticsEvent(
            eventType = "error",
            properties = properties
        )
    }
    
    override fun trackFeatureUsed(featureName: String, parameters: Map<String, Any>) {
        val properties = mutableMapOf<String, String>(
            "feature_name" to featureName
        )
        
        // Convert all parameter values to strings
        parameters.forEach { (key, value) ->
            properties[key] = value.toString()
        }
        
        logAnalyticsEvent(
            eventType = "feature_used",
            properties = properties
        )
    }
    
    // Helper method to log analytics events to the database
    private fun logAnalyticsEvent(eventType: String, properties: Map<String, String>) {
        // Also log to logcat for debugging
        Log.d(TAG, "Analytics event: $eventType, properties: $properties")
        
        scope.launch {
            try {
                DatabaseHelper.executeInsert(
                    """
                    INSERT INTO analytics_events
                    (user_id, event_type, properties, event_time)
                    VALUES (?, ?, ?, NOW())
                    """,
                    listOf(
                        userId?.toLongOrNull() ?: 0,
                        eventType,
                        Json.encodeToString(properties)
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error logging analytics event: ${e.message}", e)
            }
        }
    }
}

/**
 * Factory function to create an AnalyticsManager
 */
actual fun createAnalyticsManager(): AnalyticsManager {
    return MySqlAnalyticsManager()
}