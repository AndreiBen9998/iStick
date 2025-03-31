// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/model/Models.kt
package istick.app.beta.model

import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Represents a verification of a car's mileage
 */
@Serializable
data class MileageVerification(
    val id: String = "",
    val carId: String = "",  // Added for MySqlCarRepository
    val timestamp: Long = System.currentTimeMillis(),
    val mileage: Int = 0,
    val photoUrl: String = "",
    val verificationCode: String = "",
    val isVerified: Boolean = false,
    val date: Date = Date(), // Added for MySqlCarRepository
    val notes: String? = null // Added for MySqlCarRepository
)

/**
 * Base user interface containing common properties
 */
interface User {
    val id: String
    val email: String
    val name: String
    val profilePictureUrl: String?
    val createdAt: Long
    val lastLoginAt: Long
    val type: UserType
    val rating: Float
    val reviewCount: Int
}

/**
 * Represents a car owner in the system
 */
@Serializable
data class CarOwner(
    override val id: String = "",
    override val email: String = "",
    override val name: String = "",
    override val profilePictureUrl: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val lastLoginAt: Long = System.currentTimeMillis(),
    override val rating: Float = 0f,
    override val reviewCount: Int = 0,
    override val type: UserType = UserType.CAR_OWNER,
    val cars: List<Car> = emptyList(),
    val city: String = "",
    val dailyDrivingDistance: Int = 0, // In kilometers
    val adPreferences: List<String> = emptyList()
) : User

/**
 * Represents a brand/company in the system
 */
@Serializable
data class Brand(
    override val id: String = "",
    override val email: String = "",
    override val name: String = "",
    override val profilePictureUrl: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val lastLoginAt: Long = System.currentTimeMillis(),
    override val rating: Float = 0f,
    override val reviewCount: Int = 0,
    override val type: UserType = UserType.BRAND,
    val companyDetails: CompanyDetails = CompanyDetails(),
    val campaigns: List<String> = emptyList() // List of campaign IDs
) : User

/**
 * Represents company details for a brand
 */
@Serializable
data class CompanyDetails(
    val companyName: String = "",
    val industry: String = "",
    val website: String = "",
    val description: String = "",
    val logoUrl: String = ""
)

/**
 * Represents a user's car
 */
@Serializable
data class Car(
    val id: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val color: String = "",
    val licensePlate: String = "",
    val photos: List<String> = emptyList(),
    val currentMileage: Int = 0,
    val verification: List<MileageVerification> = emptyList()
)

/**
 * Represents a verification of a car's mileage
 */
@Serializable
data class MileageVerification(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val mileage: Int = 0,
    val photoUrl: String = "",
    val verificationCode: String = "",
    val isVerified: Boolean = false
)

/**
 * Represents an advertising campaign created by a brand
 */
@Serializable
data class Campaign(
    val id: String = "",
    val brandId: String = "",
    val title: String = "",
    val description: String = "",
    val stickerDetails: StickerDetails = StickerDetails(),
    val payment: PaymentDetails = PaymentDetails(),
    val requirements: CampaignRequirements = CampaignRequirements(),
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val applicants: List<String> = emptyList(), // List of car owner IDs who applied
    val approvedApplicants: List<String> = emptyList() // List of car owner IDs who were approved
)

/**
 * Details about the sticker to be applied
 */
@Serializable
data class StickerDetails(
    val imageUrl: String = "",
    val width: Int = 0, // In centimeters
    val height: Int = 0, // In centimeters
    val positions: List<StickerPosition> = emptyList(),
    val deliveryMethod: DeliveryMethod = DeliveryMethod.CENTER
)

/**
 * Positions where stickers can be applied
 */
@Serializable
enum class StickerPosition {
    DOOR_LEFT,
    DOOR_RIGHT,
    HOOD,
    TRUNK,
    REAR_WINDOW,
    SIDE_PANEL
}

/**
 * Methods of sticker delivery/application
 */
@Serializable
enum class DeliveryMethod {
    CENTER, // Application at authorized center
    HOME_KIT // Kit sent to user's home
}

/**
 * Payment details for a campaign
 */
@Serializable
data class PaymentDetails(
    val amount: Double = 0.0,
    val currency: String = "RON",
    val paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER
)

/**
 * Frequency of payments to car owners
 */
@Serializable
enum class PaymentFrequency {
    WEEKLY,
    MONTHLY,
    END_OF_CAMPAIGN
}

/**
 * Payment methods
 */
@Serializable
enum class PaymentMethod {
    BANK_TRANSFER,
    REVOLUT,
    PAYPAL
}

/**
 * Requirements for car owners to participate in a campaign
 */
@Serializable
data class CampaignRequirements(
    val minDailyDistance: Int = 0, // Minimum daily driving distance in kilometers
    val cities: List<String> = emptyList(), // Cities where the campaign is available
    val carMakes: List<String> = emptyList(), // Preferred car makes, empty means any
    val carModels: List<String> = emptyList(), // Preferred car models, empty means any
    val carYearMin: Int? = null, // Minimum car year, null means no minimum
    val carYearMax: Int? = null // Maximum car year, null means no maximum
)

/**
 * Status of a campaign
 */
@Serializable
enum class CampaignStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * Review of a user or brand
 */
@Serializable
data class Review(
    val id: String = "",
    val authorId: String = "",
    val receiverId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * User type enum
 */
@Serializable
enum class UserType {
    CAR_OWNER,
    BRAND
}

/**
 * Application of a car owner to a campaign
 */
@Serializable
data class CampaignApplication(
    val id: String = "",
    val campaignId: String = "",
    val carOwnerId: String = "",
    val carId: String = "",
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Status of a campaign application
 */
@Serializable
enum class ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED
}