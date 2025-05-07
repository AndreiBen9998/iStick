package istick.app.beta.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import istick.app.beta.database.AppDatabase
import istick.app.beta.util.CoroutineConfig
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class RoomStorageRepository(
    private val context: Context,
    private val database: AppDatabase
) : StorageRepository {
    private val TAG = "RoomStorageRepository"

    // Directory for saving images locally
    private val imagesDir: File by lazy {
        File(context.filesDir, "images").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> =
        withContext(CoroutineConfig.IO) {
            try {
                // Compress the image to save space
                val compressedBytes = compressImage(imageBytes, 80)

                // Generate a unique ID for the image
                val imageId = UUID.randomUUID().toString()

                // Create a file name with the unique ID
                val uniqueFileName = "${imageId}_$fileName"

                // Save the image to local storage
                val imageFile = File(imagesDir, uniqueFileName)
                FileOutputStream(imageFile).use { outputStream ->
                    outputStream.write(compressedBytes)
                }

                // Get the local URL (file path)
                val localUrl = "file://${imageFile.absolutePath}"

                Result.success(localUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                Result.failure(e)
            }
        }

    override suspend fun getImageUrl(path: String): Result<String> =
        withContext(CoroutineConfig.IO) {
            try {
                // If path is already a URL or file path, return it
                if (path.startsWith("http") || path.startsWith("file")) {
                    return@withContext Result.success(path)
                }

                // Look for the file in local storage
                val file = File(imagesDir, path)
                if (file.exists()) {
                    return@withContext Result.success("file://${file.absolutePath}")
                }

                Result.failure(Exception("Image not found: $path"))
            } catch (e: Exception) {
                Log.e(TAG, "Error getting image URL", e)
                Result.failure(e)
            }
        }

    override suspend fun getUserImages(userId: String): Result<List<String>> =
        withContext(CoroutineConfig.IO) {
            try {
                // For simplicity, just return all images in the directory
                val files = imagesDir.listFiles()
                val urls = files?.map { "file://${it.absolutePath}" } ?: emptyList()
                
                Result.success(urls)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user images", e)
                Result.failure(e)
            }
        }

    override suspend fun deleteImage(path: String): Result<Boolean> =
        withContext(CoroutineConfig.IO) {
            try {
                // Extract file name from path
                val fileName = path.substringAfterLast("/")

                // Delete local file
                if (path.startsWith("file://")) {
                    val filePath = path.removePrefix("file://")
                    val file = File(filePath)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted) {
                            return@withContext Result.success(true)
                        }
                    }
                } else {
                    // Look for the file in the images directory
                    val file = File(imagesDir, fileName)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted) {
                            return@withContext Result.success(true)
                        }
                    }
                }

                Result.success(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image", e)
                Result.failure(e)
            }
        }
}

// Helper function to compress an image
fun compressImage(imageBytes: ByteArray, quality: Int): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}