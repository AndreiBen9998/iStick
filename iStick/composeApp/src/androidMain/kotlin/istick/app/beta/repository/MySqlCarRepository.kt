// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/repository/MySqlCarRepository.kt
package istick.app.beta.repository

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * MySQL implementation of CarRepository
 */
class MySqlCarRepository : CarRepository {
    private val TAG = "MySqlCarRepository"
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    override val userCars: StateFlow<List<Car>> = _userCars
    
    override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(Dispatchers.IO) {
        try {
            val cars = DatabaseHelper.executeQuery(
                """
                SELECT * FROM cars
                WHERE user_id = ?
                ORDER BY make, model
                """,
                listOf(userId.toLong())
            ) { resultSet ->
                val carsList = mutableListOf<Car>()
                
                while (resultSet.next()) {
                    val carId = resultSet.getLong("id").toString()
                    
                    // Basic car details
                    val car = Car(
                        id = carId,
                        make = resultSet.getString("make"),
                        model = resultSet.getString("model"),
                        year = resultSet.getInt("year"),
                        color = resultSet.getString("color"),
                        licensePlate = resultSet.getString("license_plate"),
                        currentMileage = resultSet.getInt("current_mileage"),
                        photos = emptyList(), // We'll fill these in separately
                        verification = emptyList() // We'll fill these in separately
                    )
                    
                    carsList.add(car)
                }
                
                // For each car, get photos and verifications
                carsList.map { car ->
                    car.copy(
                        photos = getCarPhotos(car.id),
                        verification = getCarVerifications(car.id)
                    )
                }
            }
            
            _userCars.value = cars
            return@withContext Result.success(cars)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user cars: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun addCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Extract user ID from car owner ID format
            val userId = car.id.substringBefore("_") // Assuming car.id is in format "userId_car_index"
            
            // Insert car
            val carId = DatabaseHelper.executeInsert(
                """
                INSERT INTO cars (user_id, make, model, year, color, license_plate, current_mileage)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                listOf(
                    userId.toLong(), 
                    car.make, 
                    car.model, 
                    car.year, 
                    car.color, 
                    car.licensePlate, 
                    car.currentMileage
                )
            )
            
            if (carId > 0) {
                // Insert photos if any
                car.photos.forEach { photoUrl ->
                    DatabaseHelper.executeInsert(
                        "INSERT INTO car_photos (car_id, photo_url) VALUES (?, ?)",
                        listOf(carId, photoUrl)
                    )
                }
                
                // Get the newly created car
                return@withContext getCar(carId.toString())
            } else {
                return@withContext Result.failure(Exception("Failed to add car"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding car: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun updateCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Update car details
            val result = DatabaseHelper.executeUpdate(
                """
                UPDATE cars 
                SET make = ?, model = ?, year = ?, color = ?, license_plate = ?, current_mileage = ?
                WHERE id = ?
                """,
                listOf(
                    car.make, 
                    car.model, 
                    car.year, 
                    car.color, 
                    car.licensePlate, 
                    car.currentMileage,
                    car.id.toLong()
                )
            )
            
            if (result > 0) {
                // Clear existing photos and add new ones
                DatabaseHelper.executeUpdate(
                    "DELETE FROM car_photos WHERE car_id = ?",
                    listOf(car.id.toLong())
                )
                
                // Insert new photos
                car.photos.forEach { photoUrl ->
                    DatabaseHelper.executeInsert(
                        "INSERT INTO car_photos (car_id, photo_url) VALUES (?, ?)",
                        listOf(car.id.toLong(), photoUrl)
                    )
                }
                
                // Return updated car
                return@withContext getCar(car.id)
            } else {
                return@withContext Result.failure(Exception("Failed to update car"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating car: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun deleteCar(carId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // First delete related records (photos, verifications)
            DatabaseHelper.executeUpdate(
                "DELETE FROM car_photos WHERE car_id = ?",
                listOf(carId.toLong())
            )
            
            DatabaseHelper.executeUpdate(
                "DELETE FROM mileage_verifications WHERE car_id = ?",
                listOf(carId.toLong())
            )
            
            // Then delete the car itself
            val result = DatabaseHelper.executeUpdate(
                "DELETE FROM cars WHERE id = ?",
                listOf(carId.toLong())
            )
            
            return@withContext Result.success(result > 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting car: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification> = withContext(Dispatchers.IO) {
        try {
            // Insert verification
            val verificationId = DatabaseHelper.executeInsert(
                """
                INSERT INTO mileage_verifications 
                (car_id, timestamp, mileage, photo_url, verification_code, is_verified)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                listOf(
                    carId.toLong(),
                    verification.timestamp,
                    verification.mileage,
                    verification.photoUrl,
                    verification.verificationCode,
                    if (verification.isVerified) 1 else 0
                )
            )
            
            if (verificationId > 0) {
                // Update car's current mileage
                DatabaseHelper.executeUpdate(
                    "UPDATE cars SET current_mileage = ? WHERE id = ?",
                    listOf(verification.mileage, carId.toLong())
                )
                
                // Return the verification with its new ID
                val newVerification = verification.copy(id = verificationId.toString())
                return@withContext Result.success(newVerification)
            } else {
                return@withContext Result.failure(Exception("Failed to add verification"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding mileage verification: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun getCar(carId: String): Result<Car> = withContext(Dispatchers.IO) {
        try {
            val car = DatabaseHelper.executeQuery(
                "SELECT * FROM cars WHERE id = ?",
                listOf(carId.toLong())
            ) { resultSet ->
                if (resultSet.next()) {
                    Car(
                        id = carId,
                        make = resultSet.getString("make"),
                        model = resultSet.getString("model"),
                        year = resultSet.getInt("year"),
                        color = resultSet.getString("color"),
                        licensePlate = resultSet.getString("license_plate"),
                        currentMileage = resultSet.getInt("current_mileage"),
                        photos = getCarPhotos(carId),
                        verification = getCarVerifications(carId)
                    )
                } else {
                    null
                }
            }
            
            if (car != null) {
                return@withContext Result.success(car)
            } else {
                return@withContext Result.failure(Exception("Car not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting car: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    // Helper method to get car photos
    private suspend fun getCarPhotos(carId: String): List<String> {
        return DatabaseHelper.executeQuery(
            "SELECT photo_url FROM car_photos WHERE car_id = ? ORDER BY id",
            listOf(carId.toLong())
        ) { resultSet ->
            val photos = mutableListOf<String>()
            while (resultSet.next()) {
                resultSet.getString("photo_url")?.let { photos.add(it) }
            }
            photos
        }
    }
    
    // Helper method to get car verifications
    private suspend fun getCarVerifications(carId: String): List<MileageVerification> {
        return DatabaseHelper.executeQuery(
            """
            SELECT * FROM mileage_verifications 
            WHERE car_id = ? 
            ORDER BY timestamp DESC
            """,
            listOf(carId.toLong())
        ) { resultSet ->
            val verifications = mutableListOf<MileageVerification>()
            while (resultSet.next()) {
                verifications.add(
                    MileageVerification(
                        id = resultSet.getLong("id").toString(),
                        timestamp = resultSet.getTimestamp("timestamp").time,
                        mileage = resultSet.getInt("mileage"),
                        photoUrl = resultSet.getString("photo_url") ?: "",
                        verificationCode = resultSet.getString("verification_code") ?: "",
                        isVerified = resultSet.getBoolean("is_verified")
                    )
                )
            }
            verifications
        }
    }
}

// Define this as the default implementation to use
typealias DefaultCarRepository = MySqlCarRepository