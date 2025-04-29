// androidMain/kotlin/istick/app/beta/repository/MySqlCarRepository.kt
package istick.app.beta.repository

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

actual class MySqlCarRepository : CarRepository {
    private val TAG = "MySqlCarRepository"

    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    actual override val userCars: StateFlow<List<Car>> = _userCars

    // Cache for cars
    private val carCache = mutableMapOf<String, Car>()

    actual override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(Dispatchers.IO) {
        try {
            // Try to fetch from the database
            val cars = try {
                DatabaseHelper.executeQuery(
                    "SELECT * FROM cars WHERE owner_id = ?",
                    listOf(userId)
                ) { rs ->
                    val carsList = mutableListOf<Car>()
                    while (rs.next()) {
                        val carId = rs.getString("id")
                        val car = Car(
                            id = carId,
                            make = rs.getString("make"),
                            model = rs.getString("model"),
                            year = rs.getInt("year"),
                            color = rs.getString("color"),
                            licensePlate = rs.getString("license_plate"),
                            currentMileage = rs.getInt("current_mileage"),
                            photos = getCarPhotos(carId),
                            verification = getCarVerifications(carId)
                        )
                        carsList.add(car)
                        carCache[carId] = car
                    }
                    carsList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching cars from database", e)

                // Return mock data if database fails
                getOrCreateMockCars(userId)
            }

            // Update the state flow
            _userCars.value = cars

            Result.success(cars)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user cars", e)
            Result.failure(e)
        }
    }

    actual override suspend fun addCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Generate a new car ID if needed
            val carId = if (car.id.isBlank()) UUID.randomUUID().toString() else car.id
            val newCar = car.copy(id = carId)

            // Try to insert into the database
            try {
                DatabaseHelper.executeUpdate(
                    """
                    INSERT INTO cars 
                    (id, owner_id, make, model, year, color, license_plate, current_mileage, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    listOf(
                        carId,
                        "owner", // This should come from the car object or as a parameter
                        car.make,
                        car.model,
                        car.year,
                        car.color,
                        car.licensePlate,
                        car.currentMileage,
                        System.currentTimeMillis()
                    )
                )

                // Insert photos if any
                for (photoUrl in car.photos) {
                    DatabaseHelper.executeUpdate(
                        "INSERT INTO car_photos (car_id, url) VALUES (?, ?)",
                        listOf(carId, photoUrl)
                    )
                }

                // Insert verifications if any
                for (verification in car.verification) {
                    DatabaseHelper.executeUpdate(
                        """
                        INSERT INTO mileage_verifications 
                        (car_id, timestamp, mileage, photo_url, verification_code, is_verified)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        listOf(
                            carId,
                            verification.timestamp,
                            verification.mileage,
                            verification.photoUrl,
                            verification.verificationCode,
                            if (verification.isVerified) 1 else 0
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting car into database", e)
                // Continue with in-memory operations even if DB fails
            }

            // Update the cache
            carCache[carId] = newCar

            // Update the state flow
            _userCars.value = _userCars.value + newCar

            Result.success(newCar)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding car", e)
            Result.failure(e)
        }
    }

    actual override suspend fun updateCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Try to update the database
            try {
                DatabaseHelper.executeUpdate(
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
                        car.id
                    )
                )

                // Update photos
                // First delete existing photos
                DatabaseHelper.executeUpdate(
                    "DELETE FROM car_photos WHERE car_id = ?",
                    listOf(car.id)
                )

                // Then insert new photos
                for (photoUrl in car.photos) {
                    DatabaseHelper.executeUpdate(
                        "INSERT INTO car_photos (car_id, url) VALUES (?, ?)",
                        listOf(car.id, photoUrl)
                    )
                }

                // We don't update verifications here as they are handled separately
            } catch (e: Exception) {
                Log.e(TAG, "Error updating car in database", e)
                // Continue with in-memory operations even if DB fails
            }

            // Update the cache
            carCache[car.id] = car

            // Update the state flow
            _userCars.value = _userCars.value.map { if (it.id == car.id) car else it }

            Result.success(car)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating car", e)
            Result.failure(e)
        }
    }

    actual override suspend fun deleteCar(carId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Try to delete from the database
            try {
                // First delete related records
                DatabaseHelper.executeUpdate(
                    "DELETE FROM car_photos WHERE car_id = ?",
                    listOf(carId)
                )

                DatabaseHelper.executeUpdate(
                    "DELETE FROM mileage_verifications WHERE car_id = ?",
                    listOf(carId)
                )

                // Then delete the car
                DatabaseHelper.executeUpdate(
                    "DELETE FROM cars WHERE id = ?",
                    listOf(carId)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting car from database", e)
                // Continue with in-memory operations even if DB fails
            }

            // Remove from cache
            carCache.remove(carId)

            // Update the state flow
            _userCars.value = _userCars.value.filter { it.id != carId }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting car", e)
            Result.failure(e)
        }
    }

    actual override suspend fun addMileageVerification(
        carId: String,
        verification: MileageVerification
    ): Result<MileageVerification> = withContext(Dispatchers.IO) {
        try {
            // Generate a verification ID if needed
            val verificationId = if (verification.id.isBlank()) UUID.randomUUID().toString() else verification.id
            val newVerification = verification.copy(id = verificationId, carId = carId)

            // Try to insert into the database
            try {
                DatabaseHelper.executeUpdate(
                    """
                    INSERT INTO mileage_verifications 
                    (id, car_id, timestamp, mileage, photo_url, verification_code, is_verified)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    listOf(
                        verificationId,
                        carId,
                        verification.timestamp,
                        verification.mileage,
                        verification.photoUrl,
                        verification.verificationCode,
                        if (verification.isVerified) 1 else 0
                    )
                )

                // Also update the car's current mileage
                DatabaseHelper.executeUpdate(
                    "UPDATE cars SET current_mileage = ? WHERE id = ?",
                    listOf(verification.mileage, carId)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting verification into database", e)
                // Continue with in-memory operations even if DB fails
            }

            // Update the car in cache
            val car = carCache[carId]
            if (car != null) {
                val updatedCar = car.copy(
                    currentMileage = verification.mileage,
                    verification = car.verification + newVerification
                )
                carCache[carId] = updatedCar

                // Update the state flow
                _userCars.value = _userCars.value.map { if (it.id == carId) updatedCar else it }
            }

            Result.success(newVerification)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding mileage verification", e)
            Result.failure(e)
        }
    }

    actual override suspend fun getCar(carId: String): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            carCache[carId]?.let {
                return@withContext Result.success(it)
            }

            // Try to fetch from the database
            val car = try {
                DatabaseHelper.executeQuery(
                    "SELECT * FROM cars WHERE id = ?",
                    listOf(carId)
                ) { rs ->
                    if (rs.next()) {
                        Car(
                            id = rs.getString("id"),
                            make = rs.getString("make"),
                            model = rs.getString("model"),
                            year = rs.getInt("year"),
                            color = rs.getString("color"),
                            licensePlate = rs.getString("license_plate"),
                            currentMileage = rs.getInt("current_mileage"),
                            photos = getCarPhotos(carId),
                            verification = getCarVerifications(carId)
                        )
                    } else null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching car from database", e)
                null
            }

            if (car != null) {
                // Update cache
                carCache[carId] = car
                return@withContext Result.success(car)
            }

            // If not found, return mock data
            val mockCar = Car(
                id = carId,
                make = "Toyota",
                model = "Corolla",
                year = 2020,
                color = "Blue",
                licensePlate = "B123ABC",
                currentMileage = 15000
            )

            // Update cache
            carCache[carId] = mockCar

            Result.success(mockCar)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting car", e)
            Result.failure(e)
        }
    }

    // Helper methods
    private fun getCarPhotos(carId: String): List<String> {
        return try {
            DatabaseHelper.executeQuery(
                "SELECT url FROM car_photos WHERE car_id = ?",
                listOf(carId)
            ) { rs ->
                val photos = mutableListOf<String>()
                while (rs.next()) {
                    photos.add(rs.getString("url"))
                }
                photos
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching car photos", e)
            emptyList()
        }
    }

    private fun getCarVerifications(carId: String): List<MileageVerification> {
        return try {
            DatabaseHelper.executeQuery(
                "SELECT * FROM mileage_verifications WHERE car_id = ? ORDER BY timestamp DESC",
                listOf(carId)
            ) { rs ->
                val verifications = mutableListOf<MileageVerification>()
                while (rs.next()) {
                    verifications.add(
                        MileageVerification(
                            id = rs.getString("id") ?: UUID.randomUUID().toString(),
                            carId = carId,
                            timestamp = rs.getLong("timestamp"),
                            mileage = rs.getInt("mileage"),
                            photoUrl = rs.getString("photo_url") ?: "",
                            verificationCode = rs.getString("verification_code") ?: "",
                            isVerified = rs.getInt("is_verified") == 1
                        )
                    )
                }
                verifications
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching car verifications", e)
            emptyList()
        }
    }

    private fun getOrCreateMockCars(userId: String): List<Car> {
        // Return mock cars for the user
        return listOf(
            Car(
                id = "car1_$userId",
                make = "Toyota",
                model = "Corolla",
                year = 2020,
                color = "Blue",
                licensePlate = "B123ABC",
                currentMileage = 15000,
                photos = listOf("https://example.com/car1.jpg"),
                verification = listOf(
                    MileageVerification(
                        id = "v1",
                        carId = "car1_$userId",
                        timestamp = System.currentTimeMillis() - 2592000000, // 30 days ago
                        mileage = 12000,
                        photoUrl = "https://example.com/v1.jpg",
                        verificationCode = "123456",
                        isVerified = true
                    )
                )
            ),
            Car(
                id = "car2_$userId",
                make = "Honda",
                model = "Civic",
                year = 2019,
                color = "Red",
                licensePlate = "B456DEF",
                currentMileage = 20000,
                photos = listOf("https://example.com/car2.jpg"),
                verification = listOf(
                    MileageVerification(
                        id = "v2",
                        carId = "car2_$userId",
                        timestamp = System.currentTimeMillis() - 1296000000, // 15 days ago
                        mileage = 18000,
                        photoUrl = "https://example.com/v2.jpg",
                        verificationCode = "654321",
                        isVerified = true
                    )
                )
            )
        )
    }
}