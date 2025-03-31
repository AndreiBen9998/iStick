package istick.app.beta.repository

import android.util.Log
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.sql.Connection

class MySqlCarRepository : CarRepository {
    private val TAG = "MySqlCarRepository"

    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    override val userCars: StateFlow<List<Car>> = _userCars

    // Method to get a database connection
    private fun getDatabaseConnection(): Connection? {
        return try {
            DatabaseHelper.getConnection() // Ensure this is public in DatabaseHelper
        } catch (e: Exception) {
            Log.e(TAG, "Error getting database connection: ${e.message}", e)
            null
        }
    }

    override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(Dispatchers.IO) {
        try {
            val connection = getDatabaseConnection() ?: return@withContext Result.failure(Exception("Database connection failed."))
            val query = "SELECT * FROM cars WHERE user_id = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { resultSet ->
                    val cars = mutableListOf<Car>()
                    while (resultSet.next()) {
                        cars.add(
                            Car(
                                id = resultSet.getString("id"),
                                make = resultSet.getString("make"),
                                model = resultSet.getString("model"),
                                year = resultSet.getInt("year"),
                                color = resultSet.getString("color"),
                                licensePlate = resultSet.getString("license_plate"),
                                currentMileage = resultSet.getInt("current_mileage"),
                                photos = getCarPhotosFromDatabase(connection, resultSet.getString("id")),
                                verification = getCarVerificationsFromDatabase(connection, resultSet.getString("id"))
                            )
                        )
                    }
                    Result.success(cars)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user cars: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getCar(carId: String): Result<Car> = withContext(Dispatchers.IO) {
        try {
            val connection = getDatabaseConnection() ?: return@withContext Result.failure(Exception("Database connection failed."))
            val query = "SELECT * FROM cars WHERE id = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, carId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        val car = Car(
                            id = resultSet.getString("id"),
                            make = resultSet.getString("make"),
                            model = resultSet.getString("model"),
                            year = resultSet.getInt("year"),
                            color = resultSet.getString("color"),
                            licensePlate = resultSet.getString("license_plate"),
                            currentMileage = resultSet.getInt("current_mileage"),
                            photos = getCarPhotosFromDatabase(connection, carId),
                            verification = getCarVerificationsFromDatabase(connection, carId)
                        )
                        return@withContext Result.success(car)
                    } else {
                        return@withContext Result.failure(Exception("Car with ID $carId not found."))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching car with ID: $carId | ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            val connection = getDatabaseConnection() ?: return@withContext Result.failure(Exception("Database connection failed."))
            val query = "INSERT INTO cars (id, make, model, year, color, license_plate, current_mileage, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, car.id)
                statement.setString(2, car.make)
                statement.setString(3, car.model)
                statement.setInt(4, car.year)
                statement.setString(5, car.color)
                statement.setString(6, car.licensePlate)
                statement.setInt(7, car.currentMileage ?: 0)
                statement.setString(8, "user_id_placeholder") // Replace with actual user ID
                statement.executeUpdate()
            }
            Result.success(car)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding car: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            val connection = getDatabaseConnection() ?: return@withContext Result.failure(Exception("Database connection failed."))
            val query =
                "UPDATE cars SET make = ?, model = ?, year = ?, color = ?, license_plate = ?, current_mileage = ? WHERE id = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, car.make)
                statement.setString(2, car.model)
                statement.setInt(3, car.year)
                statement.setString(4, car.color)
                statement.setString(5, car.licensePlate)
                statement.setInt(6, car.currentMileage ?: 0)
                statement.setString(7, car.id)
                statement.executeUpdate()
            }
            Result.success(car)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating car: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteCar(carId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val connection = getDatabaseConnection() ?: return@withContext Result.failure(Exception("Database connection failed."))
            val query = "DELETE FROM cars WHERE id = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, carId)
                val rowsDeleted = statement.executeUpdate()
                if (rowsDeleted > 0) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Car not found."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting car: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addMileageVerification(
        carId: String,
        verification: MileageVerification
    ): Result<MileageVerification> = withContext(Dispatchers.IO) {
        try {
            val connection = getDatabaseConnection() ?: return@withContext Result.failure(Exception("Database connection failed."))
            val query = "INSERT INTO car_verifications (id, car_id, mileage, verification_date, verification_notes) VALUES (?, ?, ?, ?, ?)"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, verification.id)
                statement.setString(2, carId) // Use correct parameter name
                statement.setInt(3, verification.mileage)
                statement.setDate(4, java.sql.Date(verification.date.time)) // Correct parameter mapping
                statement.setString(5, verification.notes ?: "") // Avoid null issues
                statement.executeUpdate()
            }
            Result.success(verification)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding mileage verification: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getCarPhotosFromDatabase(connection: Connection, carId: String): List<String> {
        val photos = mutableListOf<String>()
        val query = "SELECT photo_url FROM car_photos WHERE car_id = ?"
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, carId)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    photos.add(resultSet.getString("photo_url"))
                }
            }
        }
        return photos
    }

    private fun getCarVerificationsFromDatabase(connection: Connection, carId: String): List<MileageVerification> {
        val verifications = mutableListOf<MileageVerification>()
        val query = "SELECT * FROM car_verifications WHERE car_id = ?"
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, carId) // Bind the correct car ID
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    verifications.add(
                        MileageVerification(
                            id = resultSet.getString("id"),
                            carId = resultSet.getString("car_id"),
                            mileage = resultSet.getInt("mileage"),
                            date = resultSet.getDate("verification_date"), // Corrected column name
                            notes = resultSet.getString("verification_notes") // Corrected column name
                        )
                    )
                }
            }
        }
        return verifications
    }
}