// commonMain/kotlin/istick/app/beta/repository/MySqlCarRepository.kt
package istick.app.beta.repository

import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.flow.StateFlow

expect class MySqlCarRepository() : CarRepository {
    override val userCars: StateFlow<List<Car>>

    override suspend fun fetchUserCars(userId: String): Result<List<Car>>
    override suspend fun addCar(car: Car): Result<Car>
    override suspend fun updateCar(car: Car): Result<Car>
    override suspend fun deleteCar(carId: String): Result<Boolean>
    override suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification>
    override suspend fun getCar(carId: String): Result<Car>
}