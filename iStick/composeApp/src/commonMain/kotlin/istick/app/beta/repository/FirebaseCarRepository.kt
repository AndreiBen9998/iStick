// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/FirebaseCarRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.flow.StateFlow

/**
 * Firebase implementation of CarRepository
 * This is a placeholder implementation using MySQL under the hood
 */
class FirebaseCarRepository : CarRepository {
    // Forward to MySQL implementation
    private val mysqlRepo = MySqlCarRepository()

    override val userCars: StateFlow<List<Car>> = mysqlRepo.userCars

    override suspend fun fetchUserCars(userId: String): Result<List<Car>> {
        return mysqlRepo.fetchUserCars(userId)
    }

    override suspend fun addCar(car: Car): Result<Car> {
        return mysqlRepo.addCar(car)
    }

    override suspend fun updateCar(car: Car): Result<Car> {
        return mysqlRepo.updateCar(car)
    }

    override suspend fun deleteCar(carId: String): Result<Boolean> {
        return mysqlRepo.deleteCar(carId)
    }

    override suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification> {
        return mysqlRepo.addMileageVerification(carId, verification)
    }

    override suspend fun getCar(carId: String): Result<Car> {
        return mysqlRepo.getCar(carId)
    }
}