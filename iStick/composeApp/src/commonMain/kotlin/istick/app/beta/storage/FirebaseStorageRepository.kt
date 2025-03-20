// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/storage/FirebaseStorageRepository.kt

package istick.app.beta.storage

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

/**
 * Platform-specific implementation for compressing images
 */
expect fun FirebaseStorageRepository.compressPlatformImage(imageBytes: ByteArray, quality: Int): ByteArray
/**
 * Firebase Storage implementation for handling file storage
 */
class FirebaseStorageRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : StorageRepository {

    // Firebase Storage reference
    private val storage = Firebase.storage
    private val storageRef = storage.reference

    // Set storage bucket - this would normally be configured in firebase.json
    private val storageBucket = "istickapp.firebasestorage.app"

    /**
     * Upload an image to Firebase Storage
     * @param imageBytes The image data as ByteArray
     * @param fileName The name to use for the file (should include path)
     * @return Result with the download URL on success, or an error on failure
     */
    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> =
        withContext(dispatcher) {
            try {
                // Compress the image if it's too large (>1MB)
                val compressedBytes = if (imageBytes.size > 1024 * 1024) {
                    compressPlatformImage(imageBytes, 85)
                } else {
                    imageBytes
                }

                // Create a reference to the file location
                val fileRef = storageRef.child(fileName)

                // Upload the file
                val uploadTask = fileRef.putBytes(compressedBytes)
                uploadTask.await()

                // Get the download URL
                val downloadUrl = fileRef.downloadUrl.await()

                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to upload image: ${e.message}", e))
            }
        }

    /**
     * Get a download URL for a file in Firebase Storage
     * @param path The path to the file in Storage
     * @return Result with the download URL on success, or an error on failure
     */
    override suspend fun getImageUrl(path: String): Result<String> = withContext(dispatcher) {
        try {
            val fileRef = storageRef.child(path)
            val downloadUrl = fileRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get image URL: ${e.message}", e))
        }
    }

    /**
     * Get all images for a user
     * @param userId The user ID
     * @return Result with a list of image URLs on success, or an error on failure
     */
    override suspend fun getUserImages(userId: String): Result<List<String>> = withContext(dispatcher) {
        try {
            // Reference to the user's images folder
            val userImagesRef = storageRef.child("users/$userId")

            // List all items in the folder
            val listResult = userImagesRef.list(100).await()

            // Get download URLs for all items
            val urls = listResult.items.map { item ->
                item.downloadUrl.await().toString()
            }

            Result.success(urls)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get user images: ${e.message}", e))
        }
    }

    /**
     * Delete an image from Firebase Storage
     * @param path The path to the file in Storage
     * @return Result with true on success, or an error on failure
     */
    override suspend fun deleteImage(path: String): Result<Boolean> = withContext(dispatcher) {
        try {
            val fileRef = storageRef.child(path)
            fileRef.delete().await()
            Result.success(true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to delete image: ${e.message}", e))
        }
    }

    /**
     * Platform-specific image compression (to be implemented in platform-specific code)
     */
    expect fun compressPlatformImage(imageBytes: ByteArray, quality: Int): ByteArray
}
