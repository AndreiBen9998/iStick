// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/CarRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.flow.StateFlow

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