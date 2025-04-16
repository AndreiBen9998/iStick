// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/MySqlCarRepository.kt
package istick.app.beta.repository

import istick.app.beta.database.DatabaseHelper
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class MySqlCarRepository : CarRepository {
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    override val userCars: StateFlow<List<Car>> = _userCars

    override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(Dispatchers.IO) {
        try {
            // For now, we'll return mock data since there's no direct car table in the provided schema
            val mockCars = listOf(
                Car(
                    id = "$userId-car1",
                    make = "Dacia",
                    model = "Logan",
                    year = 2020,
                    color = "White",
                    licensePlate = "B123ABC",
                    currentMileage = 45000,
                    photos = listOf("https://example.com/car1.jpg"),
                    verification = listOf(
                        MileageVerification(
                            id = "v1",
                            carId = "$userId-car1",
                            timestamp = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000,
                            mileage = 40000,
                            photoUrl = "https://example.com/mileage1.jpg",
                            isVerified = true
                        )
                    )
                ),
                Car(
                    id = "$userId-car2",
                    make = "Volkswagen",
                    model = "Golf",
                    year = 2019,
                    color = "Black",
                    licensePlate = "B456DEF",
                    currentMileage = 60000,
                    photos = listOf("https://example.com/car2.jpg"),
                    verification = emptyList()
                )
            )
            
            _userCars.value = mockCars
            Result.success(mockCars)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, this would insert into a cars table
            // For now, we'll just add to our in-memory list
            val carWithId = if (car.id.isBlank()) {
                car.copy(id = "car-${System.currentTimeMillis()}")
            } else {
                car
            }
            
            _userCars.value = _userCars.value + carWithId
            Result.success(carWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCar(car: Car): Result<Car> = withContext(Dispatchers.IO) {
        try {
            // Update the car in our in-memory list
            val updated = _userCars.value.map {
                if (it.id == car.id) car else it
            }
            
            _userCars.value = updated
            Result.success(car)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCar(carId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentCars = _userCars.value
            val carToDelete = currentCars.find { it.id == carId }
            
            if (carToDelete == null) {
                return@withContext Result.failure(Exception("Car not found"))
            }
            
            _userCars.value = currentCars.filter { it.id != carId }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMileageVerification(
        carId: String, 
        verification: MileageVerification
    ): Result<MileageVerification> = withContext(Dispatchers.IO) {
        try {
            val currentCars = _userCars.value
            val carIndex = currentCars.indexOfFirst { it.id == carId }
            
            if (carIndex == -1) {
                return@withContext Result.failure(Exception("Car not found"))
            }
            
            val car = currentCars[carIndex]
            val verificationWithId = if (verification.id.isBlank()) {
                verification.copy(
                    id = "v-${System.currentTimeMillis()}",
                    carId = carId
                )
            } else {
                verification.copy(carId = carId)
            }
            
            val updatedCar = car.copy(
                verification = car.verification + verificationWithId,
                currentMileage = verification.mileage
            )
            
            val updatedCars = currentCars.toMutableList().apply {
                set(carIndex, updatedCar)
            }
            
            _userCars.value = updatedCars
            Result.success(verificationWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCar(carId: String): Result<Car> = withContext(Dispatchers.IO) {
        try {
            val car = _userCars.value.find { it.id == carId }
            
            if (car != null) {
                Result.success(car)
            } else {
                Result.failure(Exception("Car not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}