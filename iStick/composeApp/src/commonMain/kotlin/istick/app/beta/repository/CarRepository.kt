// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/CarRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing car data and verifications
 */
interface CarRepository {
    val userCars: StateFlow<List<Car>>

    suspend fun fetchUserCars(userId: String): Result<List<Car>>
    suspend fun addCar(car: Car): Result<Car>
    suspend fun updateCar(car: Car): Result<Car>
    suspend fun deleteCar(carId: String): Result<Boolean>
    suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification>
    suspend fun getCar(carId: String): Result<Car>
}

/**
 * Firebase implementation of the car repository
 */
class FirebaseCarRepository : CarRepository {
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    override val userCars: StateFlow<List<Car>> = _userCars

    // Cache to reduce network calls
    private val carCache = mutableMapOf<String, Car>()

    override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(Dispatchers.Default) {
        try {
            // For now, return mock data
            // In a real implementation, this would fetch from Firebase Firestore
            val mockCars = listOf(
                Car(
                    id = "car1",
                    make = "Dacia",
                    model = "Logan",
                    year = 2019,
                    color = "White",
                    licensePlate = "B123ABC",
                    photos = listOf("https://example.com/cars/logan1.jpg", "https://example.com/cars/logan2.jpg"),
                    currentMileage = 45000,
                    verification = listOf(
                        MileageVerification(
                            id = "ver1",
                            timestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 7 days ago
                            mileage = 44800,
                            photoUrl = "https://example.com/verifications/ver1.jpg",
                            verificationCode = "ABC123",
                            isVerified = true
                        )
                    )
                ),
                Car(
                    id = "car2",
                    make = "Volkswagen",
                    model = "Golf",
                    year = 2020,
                    color = "Blue",
                    licensePlate = "B456DEF",
                    photos = listOf("https://example.com/cars/golf1.jpg"),
                    currentMileage = 32000,
                    verification = listOf(
                        MileageVerification(
                            id = "ver2",
                            timestamp = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000), // 14 days ago
                            mileage = 31500,
                            photoUrl = "https://example.com/verifications/ver2.jpg",
                            verificationCode = "DEF456",
                            isVerified = true
                        )
                    )
                )
            )

            // Update cache
            mockCars.forEach { car ->
                carCache[car.id] = car
            }

            _userCars.value = mockCars
            Result.success(mockCars)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCar(car: Car): Result<Car> = withContext(Dispatchers.Default) {
        try {
            // In a real implementation, this would create a document in Firebase
            val newCar = car.copy(id = "car_${System.currentTimeMillis()}")

            // Update local cache and state
            carCache[newCar.id] = newCar
            _userCars.value = _userCars.value + newCar

            Result.success(newCar)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCar(car: Car): Result<Car> = withContext(Dispatchers.Default) {
        try {
            // Update cache
            carCache[car.id] = car

            // Update state
            _userCars.value = _userCars.value.map {
                if (it.id == car.id) car else it
            }

            Result.success(car)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCar(carId: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Remove from cache
            carCache.remove(carId)

            // Update state
            _userCars.value = _userCars.value.filter { it.id != carId }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification> = withContext(Dispatchers.Default) {
        try {
            // Get existing car
            val existingCar = carCache[carId] ?: return@withContext Result.failure(
                Exception("Car not found")
            )

            // Add verification
            val newVerification = verification.copy(
                id = "ver_${System.currentTimeMillis()}"
            )

            val updatedCar = existingCar.copy(
                verification = existingCar.verification + newVerification,
                currentMileage = verification.mileage
            )

            // Update cache
            carCache[carId] = updatedCar

            // Update state
            _userCars.value = _userCars.value.map {
                if (it.id == carId) updatedCar else it
            }

            Result.success(newVerification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCar(carId: String): Result<Car> = withContext(Dispatchers.Default) {
        try {
            // Try to get from cache first
            carCache[carId]?.let {
                return@withContext Result.success(it)
            }

            // In a real implementation, fetch from Firebase if not in cache
            Result.failure(Exception("Car not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}