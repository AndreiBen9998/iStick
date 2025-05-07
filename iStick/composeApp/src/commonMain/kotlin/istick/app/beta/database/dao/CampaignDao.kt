package istick.app.beta.database.dao

import androidx.room.*
import istick.app.beta.database.entity.CampaignEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = 'ACTIVE'")
    fun getActiveCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE brandId = :brandId")
    fun getCampaignsByBrand(brandId: String): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :campaignId")
    suspend fun getCampaignById(campaignId: String): CampaignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity)

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Query("UPDATE campaigns SET status = :status WHERE id = :campaignId")
    suspend fun updateCampaignStatus(campaignId: String, status: String)

    @Query("DELETE FROM campaigns WHERE id = :campaignId")
    suspend fun deleteCampaign(campaignId: String)
}