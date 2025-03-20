package istick.app.beta.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import istick.app.beta.model.Car
import istick.app.beta.model.MileageVerification
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class FirebaseCarRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CarRepository {
    // Firebase Firestore instance
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val carsCollection = firestore.collection("cars")

    // In-memory cache for user's cars
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    override val userCars: StateFlow<List<Car>> = _userCars.asStateFlow()

    // Individual car cache
    private val carCache = mutableMapOf<String, Car>()

    // Flag to track if initial load has happened
    private var initialLoadDone = false

    override suspend fun fetchUserCars(userId: String): Result<List<Car>> = withContext(dispatcher) {
        try {
            // Query Firestore for cars owned by this user
            val querySnapshot = carsCollection
                .whereEqualTo("ownerId", userId)
                .get()
                .await()

            // Parse cars from documents
            val cars = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val carId = doc.id

                    // Get verification subcollection
                    val verificationsSnapshot = carsCollection
                        .document(carId)
                        .collection("verifications")
                        .orderBy("timestamp", Direction.DESCENDING)
                        .get()
                        .await()

                    val verifications = verificationsSnapshot.documents.mapNotNull { verDoc ->
                        try {
                            MileageVerification(
                                id = verDoc.id,
                                timestamp = verDoc.getLong("timestamp") ?: 0L,
                                mileage = verDoc.getLong("mileage")?.toInt() ?: 0,
                                photoUrl = verDoc.getString("photoUrl") ?: "",
                                verificationCode = verDoc.getString("verificationCode") ?: "",
                                isVerified = verDoc.getBoolean("isVerified") ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Create Car object
                    val car = Car(
                        id = carId,
                        make = doc.getString("make") ?: "",
                        model = doc.getString("model") ?: "",
                        year = doc.getLong("year")?.toInt() ?: 0,
                        color = doc.getString("color") ?: "",
                        licensePlate = doc.getString("licensePlate") ?: "",
                        photos = doc.get("photos") as? List<String> ?: emptyList(),
                        currentMileage = doc.getLong("currentMileage")?.toInt() ?: 0,
                        verification = verifications
                    )

                    // Update car cache
                    carCache[carId] = car

                    car
                } catch (e: Exception) {
                    null
                }
            }

            // Update state flow
            _userCars.value = cars
            initialLoadDone = true

            Result.success(cars)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to fetch user cars: ${e.message}", e))
        }
    }

    override suspend fun addCar(car: Car): Result<Car> = withContext(dispatcher) {
        try {
            // Prepare car data for Firestore
            val carData = mapOf(
                "ownerId" to car.id.substringBefore("_"), // Extract user ID from car ID format
                "make" to car.make,
                "model" to car.model,
                "year" to car.year,
                "color" to car.color,
                "licensePlate" to car.licensePlate,
                "photos" to car.photos,
                "currentMileage" to car.currentMileage,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            // Add car to Firestore and get the new document ID
            val docRef = carsCollection.add(carData).await()

            // Create a new car object with the generated ID
            val newCar = car.copy(id = docRef.id)

            // Add initial verifications if any
            if (car.verification.isNotEmpty()) {
                for (verification in car.verification) {
                    val verificationData = mapOf(
                        "timestamp" to verification.timestamp,
                        "mileage" to verification.mileage,
                        "photoUrl" to verification.photoUrl,
                        "verificationCode" to verification.verificationCode,
                        "isVerified" to verification.isVerified
                    )

                    docRef.collection("verifications").add(verificationData).await()
                }
            }

            // Update cache
            carCache[newCar.id] = newCar
            _userCars.value = _userCars.value + newCar

            Result.success(newCar)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to add car: ${e.message}", e))
        }
    }

    override suspend fun updateCar(car: Car): Result<Car> = withContext(dispatcher) {
        try {
            // Prepare car data for update
            val carData = mapOf(
                "make" to car.make,
                "model" to car.model,
                "year" to car.year,
                "color" to car.color,
                "licensePlate" to car.licensePlate,
                "photos" to car.photos,
                "currentMileage" to car.currentMileage,
                "updatedAt" to System.currentTimeMillis()
            )

            // Update car in Firestore
            carsCollection.document(car.id).update(carData).await()

            // Update cache
            carCache[car.id] = car
            _userCars.value = _userCars.value.map { if (it.id == car.id) car else it }

            Result.success(car)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to update car: ${e.message}", e))
        }
    }

    override suspend fun deleteCar(carId: String): Result<Boolean> = withContext(dispatcher) {
        try {
            // Delete all verifications first
            val verificationsSnapshot = carsCollection
                .document(carId)
                .collection("verifications")
                .get()
                .await()

            // Delete each verification document
            for (doc in verificationsSnapshot.documents) {
                carsCollection
                    .document(carId)
                    .collection("verifications")
                    .document(doc.id)
                    .delete()
                    .await()
            }

            // Delete the car document
            carsCollection.document(carId).delete().await()

            // Update cache
            carCache.remove(carId)
            _userCars.value = _userCars.value.filter { it.id != carId }

            Result.success(true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to delete car: ${e.message}", e))
        }
    }

    override suspend fun addMileageVerification(carId: String, verification: MileageVerification): Result<MileageVerification> =
        withContext(dispatcher) {
            try {
                // Get existing car
                val existingCar = carCache[carId] ?: run {
                    // Fetch car if not in cache
                    val carDoc = carsCollection.document(carId).get().await()
                    if (!carDoc.exists) {
                        return@withContext Result.failure(Exception("Car not found"))
                    }

                    // Just proceed with fetching verification details
                    null
                }

                // Prepare verification data
                val verificationData = mapOf(
                    "timestamp" to verification.timestamp,
                    "mileage" to verification.mileage,
                    "photoUrl" to verification.photoUrl,
                    "verificationCode" to verification.verificationCode,
                    "isVerified" to verification.isVerified
                )

                // Add verification to subcollection
                val verificationRef = carsCollection
                    .document(carId)
                    .collection("verifications")
                    .add(verificationData)
                    .await()

                // Create new verification object with ID
                val newVerification = verification.copy(id = verificationRef.id)

                // Update car's current mileage
                carsCollection.document(carId).update(
                    mapOf(
                        "currentMileage" to verification.mileage,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                // Update cache if car exists
                existingCar?.let {
                    val updatedCar = it.copy(
                        currentMileage = verification.mileage,
                        verification = it.verification + newVerification
                    )

                    carCache[carId] = updatedCar
                    _userCars.value = _userCars.value.map { car ->
                        if (car.id == carId) updatedCar else car
                    }
                }

                Result.success(newVerification)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to add mileage verification: ${e.message}", e))
            }
        }

    override suspend fun getCar(carId: String): Result<Car> = withContext(dispatcher) {
        try {
            // Check cache first
            carCache[carId]?.let {
                return@withContext Result.success(it)
            }

            // Fetch car from Firestore
            val carDoc = carsCollection.document(carId).get().await()
            if (!carDoc.exists) {
                return@withContext Result.failure(Exception("Car not found"))
            }

            // Fetch verifications
            val verificationsSnapshot = carsCollection
                .document(carId)
                .collection("verifications")
                .orderBy("timestamp", Direction.DESCENDING)
                .get()
                .await()

            val verifications = verificationsSnapshot.documents.mapNotNull { doc ->
                try {
                    MileageVerification(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        mileage = doc.getLong("mileage")?.toInt() ?: 0,
                        photoUrl = doc.getString("photoUrl") ?: "",
                        verificationCode = doc.getString("verificationCode") ?: "",
                        isVerified = doc.getBoolean("isVerified") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Create car object
            val car = Car(
                id = carId,
                make = carDoc.getString("make") ?: "",
                model = carDoc.getString("model") ?: "",
                year = carDoc.getLong("year")?.toInt() ?: 0,
                color = carDoc.getString("color") ?: "",
                licensePlate = carDoc.getString("licensePlate") ?: "",
                photos = carDoc.get("photos") as? List<String> ?: emptyList(),
                currentMileage = carDoc.getLong("currentMileage")?.toInt() ?: 0,
                verification = verifications
            )

            // Update cache
            carCache[carId] = car

            Result.success(car)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get car: ${e.message}", e))
        }
    }
}
