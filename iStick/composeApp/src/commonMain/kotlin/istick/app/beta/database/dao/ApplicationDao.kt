package istick.app.beta.database.dao

import androidx.room.*
import istick.app.beta.database.entity.ApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE id = :applicationId")
    suspend fun getApplicationById(applicationId: String): ApplicationEntity?

    @Query("SELECT * FROM applications WHERE carOwnerId = :carOwnerId")
    fun getApplicationsByCarOwner(carOwnerId: String): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE campaignId = :campaignId")
    fun getApplicationsByCampaign(campaignId: String): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE carOwnerId = :carOwnerId AND campaignId = :campaignId")
    suspend fun getApplicationByOwnerAndCampaign(carOwnerId: String, campaignId: String): ApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: ApplicationEntity)

    @Update
    suspend fun updateApplication(application: ApplicationEntity)

    @Query("UPDATE applications SET status = :status, updatedAt = :timestamp WHERE id = :applicationId")
    suspend fun updateApplicationStatus(applicationId: String, status: String, timestamp: Long)

    @Query("DELETE FROM applications WHERE id = :applicationId")
    suspend fun deleteApplication(applicationId: String)
}