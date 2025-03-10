// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/storage/FirebaseStorageRepository.kt
package istick.app.beta.storage

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseStorageRepository : StorageRepository {
    private val storage by lazy { Firebase.storage }
    private val imagesRef by lazy { storage.reference.child("images") }

    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                val imageRef = imagesRef.child(fileName)

                // Fix potential blocking issue with await
                val uploadTask = imageRef.putBytes(imageBytes)
                uploadTask.await()

                val downloadUrl = imageRef.getDownloadUrl().await()
                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error uploading image: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun getImageUrl(path: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                val imageRef = storage.reference.child(path)
                val downloadUrl = imageRef.getDownloadUrl().await()

                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error getting image URL: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun getUserImages(userId: String): Result<List<String>> =
        withContext(Dispatchers.Default) {
            try {
                val userImagesRef = imagesRef.child("users").child(userId)

                // List all items in the user's images folder
                val result = userImagesRef.list(100).await()

                // Get download URLs for all items
                val urls = result.items.map { item ->
                    item.getDownloadUrl().await().toString()
                }

                Result.success(urls)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // If the directory doesn't exist yet, just return an empty list
                if (e.message?.contains("Object does not exist") == true) {
                    Result.success(emptyList())
                } else {
                    println("Error getting user images: ${e.message}")
                    Result.failure(e)
                }
            }
        }

    override suspend fun deleteImage(path: String): Result<Boolean> =
        withContext(Dispatchers.Default) {
            try {
                val imageRef = storage.reference.child(path)
                imageRef.delete().await()

                Result.success(true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("Error deleting image: ${e.message}")
                Result.failure(e)
            }
        }
}