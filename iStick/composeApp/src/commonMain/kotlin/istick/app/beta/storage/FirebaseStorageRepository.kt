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
 * Firebase Storage implementation for handling file storage
 */
expect fun FirebaseStorageRepository.compressPlatformImage(imageBytes: ByteArray, quality: Int): ByteArray
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
                    compressImage(imageBytes)
                } else {
                    imageBytes
                }

                // Create a reference to the file location
                val fileRef = storageRef.child(fileName)

                // Upload the file
                val uploadTask = fileRef.putBytes(compressedBytes)

                // Wait for the upload to complete
                uploadTask.await()

                // Get the download URL
                val downloadUrl = fileRef.getDownloadUrl().await()

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
            val downloadUrl = fileRef.getDownloadUrl().await()
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
                item.getDownloadUrl().await().toString()
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
     * Upload an image with progress tracking
     * @param imageBytes The image data as ByteArray
     * @param fileName The name to use for the file (should include path)
     * @return Flow emitting upload progress (0-100) and final URL
     */
    fun uploadImageWithProgress(imageBytes: ByteArray, fileName: String): Flow<UploadProgress> = flow {
        try {
            // Compress the image if it's too large (>1MB)
            val compressedBytes = if (imageBytes.size > 1024 * 1024) {
                compressImage(imageBytes)
            } else {
                imageBytes
            }

            // Create a reference to the file location
            val fileRef = storageRef.child(fileName)

            // Start with 0% progress
            emit(UploadProgress.Progress(0))

            // Upload the file and track progress
            val uploadTask = fileRef.putBytes(compressedBytes)

            // Monitor progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                emit(UploadProgress.Progress(progress))
            }

            // Wait for completion
            uploadTask.await()

            // Get download URL
            val downloadUrl = fileRef.getDownloadUrl().await().toString()

            // Emit success with URL
            emit(UploadProgress.Success(downloadUrl))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(UploadProgress.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Compress image to reduce size
     * This is a simple implementation - in a real app, you'd use platform-specific
     * image compression libraries for better results
     */
    private fun compressImage(imageBytes: ByteArray): ByteArray {
        // Simple compression by reducing quality
        // In a real implementation, you'd use platform-specific image compression
        // For this example, we'll just return the original bytes
        // A real implementation would use something like:
        // - Android: Bitmap.compress with JPEG and reduced quality
        // - iOS: UIImage compression with UIImageJPEGRepresentation
        return imageBytes
    }

    /**
     * Helper class for tracking upload progress
     */
    sealed class UploadProgress {
        /**
         * Progress update during upload (0-100)
         */
        data class Progress(val percent: Int) : UploadProgress()

        /**
         * Upload completed successfully
         */
        data class Success(val downloadUrl: String) : UploadProgress()

        /**
         * Upload failed with error
         */
        data class Error(val message: String) : UploadProgress()
    }

    /**
     * Platform-specific image compression (to be implemented in platform-specific code)
     */
    expect fun compressPlatformImage(imageBytes: ByteArray, quality: Int): ByteArray
}

