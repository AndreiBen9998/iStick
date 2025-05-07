package istick.app.beta.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import istick.app.beta.model.CampaignStatus

@Entity(
    tableName = "campaigns",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["brandId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("brandId")]
)
data class CampaignEntity(
    @PrimaryKey
    val id: String,
    val brandId: String,
    val title: String,
    val description: String,
    val status: String = CampaignStatus.DRAFT.name,
    val paymentAmount: Double,
    val currency: String,
    val paymentFrequency: String,
    val paymentMethod: String,
    val minDailyDistance: Int = 0,
    val stickerImageUrl: String? = null,
    val stickerWidth: Int = 0,
    val stickerHeight: Int = 0,
    val deliveryMethod: String,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)