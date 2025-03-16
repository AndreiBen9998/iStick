// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/analytics/FirebaseAnalyticsManager.kt
package istick.app.beta.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import istick.app.beta.model.Campaign
import istick.app.beta.model.Car
import istick.app.beta.model.UserType

/**
 * Firebase Analytics implementation for Android
 */
class FirebaseAnalyticsManager(
    private val context: Context
) : AnalyticsManager {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
    
    override fun setUserProperties(userId: String, userType: UserType, properties: Map<String, String>) {
        // Set user ID
        firebaseAnalytics.setUserId(userId)
        
        // Set user type
        firebaseAnalytics.setUserProperty("user_type", userType.name)
        
        // Set additional properties
        properties.forEach { (key, value) ->
            firebaseAnalytics.setUserProperty(key, value)
        }
    }
    
    override fun trackScreenView(screenName: String, screenClass: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }
    
    override fun trackRegistration(userType: UserType, method: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
            param("user_type", userType.name)
        }
    }
    
    override fun trackLogin(method: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }
    
    override fun trackCarAdded(car: Car) {
        firebaseAnalytics.logEvent("car_added") {
            param("car_make", car.make)
            param("car_model", car.model)
            param("car_year", car.year.toLong())
            param("car_color", car.color)
        }
    }
    
    override fun trackMileageVerification(car: Car, newMileage: Int, verificationMethod: String) {
        firebaseAnalytics.logEvent("mileage_verification") {
            param("car_id", car.id)
            param("car_make", car.make)
            param("car_model", car.model)
            param("previous_mileage", car.currentMileage.toLong())
            param("new_mileage", newMileage.toLong())
            param("verification_method", verificationMethod)
        }
    }
    
    override fun trackCampaignView(campaign: Campaign) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, campaign.id)
            param(FirebaseAnalytics.Param.ITEM_NAME, campaign.title)
            param(FirebaseAnalytics.Param.ITEM_CATEGORY, "campaign")
            param("brand_id", campaign.brandId)
            param("campaign_status", campaign.status.name)
            param("payment_amount", campaign.payment.amount)
            param("payment_currency", campaign.payment.currency)
        }
    }
    
    override fun trackCampaignApplication(campaign: Campaign, car: Car) {
        firebaseAnalytics.logEvent("campaign_application") {
            param("campaign_id", campaign.id)
            param("campaign_title", campaign.title)
            param("brand_id", campaign.brandId)
            param("car_id", car.id)
            param("car_make", car.make)
            param("car_model", car.model)
            param("car_year", car.year.toLong())
        }
    }
    
    override fun trackCampaignCreated(campaign: Campaign) {
        firebaseAnalytics.logEvent("campaign_created") {
            param("campaign_id", campaign.id)
            param("campaign_title", campaign.title)
            param("brand_id", campaign.brandId)
            param("payment_amount", campaign.payment.amount)
            param("payment_currency", campaign.payment.currency)
            param("payment_frequency", campaign.payment.paymentFrequency.name)
            param("delivery_method", campaign.stickerDetails.deliveryMethod.name)
            param("min_daily_distance", campaign.requirements.minDailyDistance.toLong())
            param("cities_count", campaign.requirements.cities.size.toLong())
        }
    }
    
    override fun trackApplicationReview(campaignId: String, approved: Boolean) {
        firebaseAnalytics.logEvent("application_review") {
            param("campaign_id", campaignId)
            param("approved", approved)
        }
    }
    
    override fun trackAdRevenue(campaignId: String, amount: Double, currency: String) {
        firebaseAnalytics.logEvent("ad_revenue") {
            param("campaign_id", campaignId)
            param(FirebaseAnalytics.Param.VALUE, amount)
            param(FirebaseAnalytics.Param.CURRENCY, currency)
        }
    }
    
    override fun trackError(errorType: String, errorMessage: String, screenName: String?) {
        firebaseAnalytics.logEvent("app_error") {
            param("error_type", errorType)
            param("error_message", errorMessage)
            if (screenName != null) {
                param("screen_name", screenName)
            }
        }
    }
    
    override fun trackFeatureUsed(featureName: String, parameters: Map<String, Any>) {
        firebaseAnalytics.logEvent("feature_used") {
            param("feature_name", featureName)
            
            // Add additional parameters
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Boolean -> param(key, value)
                    else -> param(key, value.toString())
                }
            }
        }
    }
}

actual fun createAnalyticsManager(): AnalyticsManager {
    // This is just a stub - in real implementation, we need context
    throw IllegalStateException("Context must be provided for Android AnalyticsManager")
}

/**
 * Create analytics manager with Android context
 */
fun createAnalyticsManager(context: Context): AnalyticsManager {
    return FirebaseAnalyticsManager(context)
}