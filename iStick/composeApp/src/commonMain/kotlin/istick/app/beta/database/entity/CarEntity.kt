package istick.app.beta.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cars",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerId")]
)
data class CarEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val make: String,
    val model: String,
    val year: Int,
    val color: String,
    val licensePlate: String,
    val currentMileage: Int,
    val createdAt: Long
)