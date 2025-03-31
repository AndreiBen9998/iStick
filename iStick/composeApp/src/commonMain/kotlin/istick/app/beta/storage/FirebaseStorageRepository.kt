// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/storage/FirebaseStorageRepository.kt
package istick.app.beta.storage

import istick.app.beta.model.User
import android.content.Context

/**
 * Firebase implementation of StorageRepository
 * This is a placeholder implementation that delegates to MySqlStorageRepository
 */
class FirebaseStorageRepository(
    private val context: Context
) : StorageRepository {
    // Actually use MySQL implementation
    private val mysqlRepo = MySqlStorageRepository(context)

    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> {
        return mysqlRepo.uploadImage(imageBytes, fileName)
    }

    override suspend fun getImageUrl(path: String): Result<String> {
        return mysqlRepo.getImageUrl(path)
    }

    override suspend fun getUserImages(userId: String): Result<List<String>> {
        return mysqlRepo.getUserImages(userId)
    }

    override suspend fun deleteImage(path: String): Result<Boolean> {
        return mysqlRepo.deleteImage(path)
    }
}