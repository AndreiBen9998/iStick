// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/analytics/AnalyticsManager.kt
package istick.app.beta.analytics

import istick.app.beta.model.Campaign
import istick.app.beta.model.Car
import istick.app.beta.model.UserType

/**
 * Analytics Manager interface for tracking events
 */
interface AnalyticsManager {
    /**
     * Set user properties for segmentation
     */
    fun setUserProperties(userId: String, userType: UserType, properties: Map<String, String>)
    
    /**
     * Track screen view
     */
    fun trackScreenView(screenName: String, screenClass: String)
    
    /**
     * Track user registration
     */
    fun trackRegistration(userType: UserType, method: String = "email")
    
    /**
     * Track user login
     */
    fun trackLogin(method: String = "email")
    
    /**
     * Track car added
     */
    fun trackCarAdded(car: Car)
    
    /**
     * Track mileage verification
     */
    fun trackMileageVerification(car: Car, newMileage: Int, verificationMethod: String = "photo")
    
    /**
     * Track campaign view
     */
    fun trackCampaignView(campaign: Campaign)
    
    /**
     * Track campaign application
     */
    fun trackCampaignApplication(campaign: Campaign, car: Car)
    
    /**
     * Track campaign created (for brands)
     */
    fun trackCampaignCreated(campaign: Campaign)
    
    /**
     * Track application review (for brands)
     */
    fun trackApplicationReview(campaignId: String, approved: Boolean)
    
    /**
     * Track ad revenue (for payout tracking)
     */
    fun trackAdRevenue(campaignId: String, amount: Double, currency: String)
    
    /**
     * Track error event
     */
    fun trackError(errorType: String, errorMessage: String, screenName: String? = null)
    
    /**
     * Track feature usage
     */
    fun trackFeatureUsed(featureName: String, parameters: Map<String, Any> = emptyMap())
}

/**
 * Platform-specific analytics manager factory
 */
expect fun createAnalyticsManager(): AnalyticsManager