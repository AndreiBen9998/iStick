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
import kotlinx.coroutines.runBlocking
import java.sql.Date

class MySqlCarRepository : CarRepository {
    private val TAG = "MySqlCarRepository"

    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    override val userCars: StateFlow<List<Car>> = _userCars

    override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(Dispatchers.IO) {
        try {
            val cars = DatabaseHelper.executeQuery(
                "SELECT * FROM cars WHERE user_id = ?",
                listOf(userId.toLong())
            ) { resultSet ->
                val carsList = mutableListOf<Car>()
                while (resultSet.next()) {
                    val carId = resultSet.getLong("id").toString()
                    carsList.add(
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
                    )
                }
                carsList
            }

            // Update state flow
            _userCars.value = cars

            return@withContext Result.success(cars)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user cars: ${e.message}", e)
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

    override suspend fun addCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID
            val userId = "1" // In a real app, get this from AuthRepository

            // Insert car record
            val carId = DatabaseHelper.executeInsert(
                """
                INSERT INTO cars (
                    make, model, year, color, license_plate, current_mileage, user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                listOf<Any>(
                    car.make,
                    car.model,
                    car.year,
                    car.color,
                    car.licensePlate,
                    car.currentMileage,
                    userId.toLong()
                )
            )

            if (carId > 0) {
                // Create new car with generated ID
                val newCar = car.copy(id = carId.toString())

                // Update cache
                _userCars.value = _userCars.value + newCar

                return@withContext Result.success(newCar)
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
            // Update car record
            val rowsUpdated = DatabaseHelper.executeUpdate(
                """
                UPDATE cars SET 
                    make = ?, 
                    model = ?, 
                    year = ?, 
                    color = ?, 
                    license_plate = ?, 
                    current_mileage = ?
                WHERE id = ?
                """,
                listOf<Any>(
                    car.make,
                    car.model,
                    car.year,
                    car.color,
                    car.licensePlate,
                    car.currentMileage,
                    car.id.toLong()
                )
            )

            if (rowsUpdated > 0) {
                // Update cache
                _userCars.value = _userCars.value.map {
                    if (it.id == car.id) car else it
                }

                return@withContext Result.success(car)
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
            // Delete car record
            val rowsDeleted = DatabaseHelper.executeUpdate(
                "DELETE FROM cars WHERE id = ?",
                listOf<Any>(carId.toLong())
            )

            if (rowsDeleted > 0) {
                // Update cache
                _userCars.value = _userCars.value.filter { it.id != carId }

                return@withContext Result.success(true)
            } else {
                return@withContext Result.failure(Exception("Failed to delete car"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting car: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification> = withContext(Dispatchers.IO) {
        try {
            // Insert verification record
            val verificationId = DatabaseHelper.executeInsert(
                """
                INSERT INTO car_verifications (
                    car_id, mileage, photo_url, verification_code, 
                    is_verified, verification_date, verification_notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                listOf<Any>(
                    carId.toLong(),
                    verification.mileage,
                    verification.photoUrl,
                    verification.verificationCode,
                    verification.isVerified,
                    Date(verification.date.time),
                    verification.notes ?: ""
                )
            )

            if (verificationId > 0) {
                // Create new verification with generated ID
                val newVerification = verification.copy(
                    id = verificationId.toString(),
                    carId = carId
                )

                // Update car with new verification
                val car = getCar(carId).getOrNull()
                if (car != null) {
                    val updatedCar = car.copy(
                        verification = car.verification + newVerification,
                        currentMileage = verification.mileage
                    )

                    // Update car in database with new mileage
                    updateCar(updatedCar)

                    // Update userCars cache
                    _userCars.value = _userCars.value.map {
                        if (it.id == carId) updatedCar else it
                    }
                }

                return@withContext Result.success(newVerification)
            } else {
                return@withContext Result.failure(Exception("Failed to add verification"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding mileage verification: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    // Helper method to get car photos
    private fun getCarPhotos(carId: String): List<String> {
        return try {
            runBlocking(Dispatchers.IO) {
                DatabaseHelper.executeQuery(
                    "SELECT photo_url FROM car_photos WHERE car_id = ?",
                    listOf<Any>(carId.toLong())
                ) { resultSet ->
                    val photos = mutableListOf<String>()
                    while (resultSet.next()) {
                        photos.add(resultSet.getString("photo_url"))
                    }
                    photos
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting car photos: ${e.message}", e)
            emptyList()
        }
    }

    // Helper method to get car verifications
    private fun getCarVerifications(carId: String): List<MileageVerification> {
        return try {
            runBlocking(Dispatchers.IO) {
                DatabaseHelper.executeQuery(
                    "SELECT * FROM car_verifications WHERE car_id = ?",
                    listOf<Any>(carId.toLong())
                ) { resultSet ->
                    val verifications = mutableListOf<MileageVerification>()
                    while (resultSet.next()) {
                        val verificationId = resultSet.getLong("id").toString()
                        val timestamp = resultSet.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
                        val verificationDate = resultSet.getDate("verification_date")

                        verifications.add(
                            MileageVerification(
                                id = verificationId,
                                carId = carId,
                                timestamp = timestamp,
                                mileage = resultSet.getInt("mileage"),
                                photoUrl = resultSet.getString("photo_url") ?: "",
                                verificationCode = resultSet.getString("verification_code") ?: "",
                                isVerified = resultSet.getBoolean("is_verified"),
                                date = verificationDate ?: java.util.Date(),
                                notes = resultSet.getString("verification_notes")
                            )
                        )
                    }
                    verifications
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting car verifications: ${e.message}", e)
            emptyList()
        }
    }
}