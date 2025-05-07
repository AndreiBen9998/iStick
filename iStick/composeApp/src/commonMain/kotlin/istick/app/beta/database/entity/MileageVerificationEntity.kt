package istick.app.beta.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mileage_verifications",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class MileageVerificationEntity(
    @PrimaryKey
    val id: String,
    val carId: String,
    val timestamp: Long,
    val mileage: Int,
    val photoUrl: String?,
    val verificationCode: String,
    val isVerified: Boolean
)