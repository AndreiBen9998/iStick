package istick.app.beta.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import istick.app.beta.model.ApplicationStatus

@Entity(
    tableName = "applications",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["carOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("campaignId"),
        Index("carOwnerId"),
        Index("carId")
    ]
)
data class ApplicationEntity(
    @PrimaryKey
    val id: String,
    val campaignId: String,
    val carOwnerId: String,
    val carId: String,
    val status: String = ApplicationStatus.PENDING.name,
    val appliedAt: Long,
    val updatedAt: Long,
    val notes: String? = null
)