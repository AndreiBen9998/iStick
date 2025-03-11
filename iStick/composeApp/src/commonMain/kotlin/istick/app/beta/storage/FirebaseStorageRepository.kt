// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/storage/FirebaseStorageRepository.kt
package istick.app.beta.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Temporary dummy implementation of FirebaseStorageRepository
 * This allows the app to compile and run while Firebase implementation is being fixed
 */
class FirebaseStorageRepository : StorageRepository {
    // Dummy storage reference - not actually used but prevents compiler errors
    private val mockStorage = MockFirebaseStorage()

    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                // Log the action for debugging
                println("MOCK: Uploading image $fileName, size: ${imageBytes.size} bytes")

                // Return a fake download URL
                val fakeDownloadUrl = "https://example.com/storage/images/$fileName"
                Result.success(fakeDownloadUrl)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error in mock upload: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun getImageUrl(path: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                // Log the action for debugging
                println("MOCK: Getting image URL for $path")

                // Return a fake download URL
                val fakeDownloadUrl = "https://example.com/storage/$path"
                Result.success(fakeDownloadUrl)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error in mock getImageUrl: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun getUserImages(userId: String): Result<List<String>> =
        withContext(Dispatchers.Default) {
            try {
                // Log the action for debugging
                println("MOCK: Getting images for user $userId")

                // Return fake image URLs
                val fakeImages = listOf(
                    "https://example.com/storage/users/$userId/image1.jpg",
                    "https://example.com/storage/users/$userId/image2.jpg"
                )
                Result.success(fakeImages)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error in mock getUserImages: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun deleteImage(path: String): Result<Boolean> =
        withContext(Dispatchers.Default) {
            try {
                // Log the action for debugging
                println("MOCK: Deleting image $path")

                // Pretend deletion was successful
                Result.success(true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error in mock deleteImage: ${e.message}")
                Result.failure(e)
            }
        }
}

/**
 * Mock class to simulate Firebase Storage functionality
 * This is just to prevent compiler errors
 */
private class MockFirebaseStorage {
    fun reference() = MockStorageReference()
}

private class MockStorageReference {
    fun child(path: String) = MockStorageReference()
}