package istick.app.beta.storage

interface StorageRepository {
    suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String>
    suspend fun getImageUrl(path: String): Result<String>
    suspend fun getUserImages(userId: String): Result<List<String>>
    suspend fun deleteImage(path: String): Result<Boolean>
}