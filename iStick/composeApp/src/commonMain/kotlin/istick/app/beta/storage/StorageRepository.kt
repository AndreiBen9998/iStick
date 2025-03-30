// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/storage/StorageRepository.kt
package istick.app.beta.storage

/**
 * Interface for storage operations
 */
interface StorageRepository {
    /**
     * Upload an image and return the URL
     */
    suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String>

    /**
     * Get the URL for an image
     */
    suspend fun getImageUrl(path: String): Result<String>

    /**
     * Get all images for a user
     */
    suspend fun getUserImages(userId: String): Result<List<String>>

    /**
     * Delete an image
     */
    suspend fun deleteImage(path: String): Result<Boolean>
}