package istick.app.beta.database.dao

import androidx.room.*
import istick.app.beta.database.entity.CarEntity
import istick.app.beta.database.entity.CarPhotoEntity
import istick.app.beta.database.entity.MileageVerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE id = :carId")
    suspend fun getCarById(carId: String): CarEntity?

    @Query("SELECT * FROM cars WHERE ownerId = :ownerId")
    fun getCarsByOwner(ownerId: String): Flow<List<CarEntity>>

    @Query("SELECT * FROM car_photos WHERE carId = :carId")
    suspend fun getCarPhotos(carId: String): List<CarPhotoEntity>

    @Query("SELECT * FROM mileage_verifications WHERE carId = :carId ORDER BY timestamp DESC")
    suspend fun getCarVerifications(carId: String): List<MileageVerificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarPhoto(photo: CarPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMileageVerification(verification: MileageVerificationEntity)

    @Update
    suspend fun updateCar(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :carId")
    suspend fun deleteCar(carId: String)

    @Query("DELETE FROM car_photos WHERE carId = :carId")
    suspend fun deleteCarPhotos(carId: String)

    @Transaction
    suspend fun insertCarWithPhotos(car: CarEntity, photos: List<CarPhotoEntity>) {
        insertCar(car)
        photos.forEach { insertCarPhoto(it) }
    }
}