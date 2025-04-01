package istick.app.beta.storage

/**
 * MySqlStorageRepository provides storage functionalities using platform-specific details.
 *
 * This is an `expect` class, with its implementation provided in platform-specific modules.
 */
expect class MySqlStorageRepository(context: Any) : StorageRepository {
    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String>
    override suspend fun getImageUrl(path: String): Result<String>
    override suspend fun getUserImages(userId: String): Result<List<String>>
    override suspend fun deleteImage(path: String): Result<Boolean>
}