package istick.app.beta.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import istick.app.beta.model.UserType

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val password: String,
    val fullName: String,
    val profilePictureUrl: String? = null,
    val userType: String = UserType.CAR_OWNER.name,
    val city: String? = null,
    val dailyDrivingDistance: Int = 0,
    val companyName: String? = null,
    val industry: String? = null,
    val website: String? = null,
    val description: String? = null,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val createdAt: Long,
    val lastLoginAt: Long
)