package istick.app.beta.data.local

import androidx.room.*
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignStatus
import istick.app.beta.model.PaymentDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first


@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: String,
    val brandId: String,
    val title: String,
    val description: String,
    val status: String,
    val paymentAmount: Double,
    val paymentCurrency: String,
    // Add other necessary fields
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns WHERE status = 'ACTIVE'")
    fun observeActiveCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaigns(campaigns: List<CampaignEntity>)

    @Query("DELETE FROM campaigns")
    suspend fun clearAllCampaigns()

    @Transaction
    suspend fun clearAndInsert(campaigns: List<CampaignEntity>) {
        clearAllCampaigns()
        insertCampaigns(campaigns)
    }
}

@Database(
    entities = [CampaignEntity::class],
    version = 1,
    exportSchema = false  // Add this line
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun campaignDao(): CampaignDao
}

// Create a singleton instance
object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getDatabase(context: android.content.Context): AppDatabase {
        return instance ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "istick-database"
            ).build().also { instance = it }
        }
    }
}