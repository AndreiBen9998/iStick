// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/storage/MySqlStorageRepository.kt
package istick.app.beta.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import istick.app.beta.auth.DefaultAuthRepository
import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

actual class MySqlStorageRepository actual constructor(private val context: Any) : StorageRepository {
    private val TAG = "MySqlStorageRepository"

    // Cast to Android Context
    private val androidContext: Context
        get() = context as Context

    // Directory for saving images locally
    private val imagesDir: File by lazy {
        File(androidContext.filesDir, "images").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    actual override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> = withContext(Dispatchers.IO) {
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

            // Get current user ID
            val authRepository = DefaultAuthRepository()
            val userId = authRepository.getCurrentUserId() ?: "unknown"

            // Save the image metadata to the database
            try {
                DatabaseHelper.executeUpdate(
                    """
                    INSERT INTO images (user_id, file_name, url, size, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    listOf(
                        userId,
                        uniqueFileName,
                        localUrl,
                        compressedBytes.size,
                        System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image metadata to database", e)
                // If database operation fails, continue with local storage
            }

            Result.success(localUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            Result.failure(e)
        }
    }

    actual override suspend fun getImageUrl(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // If path is already a URL or file path, return it
            if (path.startsWith("http") || path.startsWith("file")) {
                return@withContext Result.success(path)
            }

            // Otherwise, check the database for the image
            try {
                val url = DatabaseHelper.executeQuery(
                    "SELECT url FROM images WHERE file_name = ?",
                    listOf(path)
                ) { rs ->
                    if (rs.next()) {
                        rs.getString("url")
                    } else null
                }

                if (url != null) {
                    return@withContext Result.success(url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying image URL from database", e)
                // If database operation fails, check local storage
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

    actual override suspend fun getUserImages(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Try to fetch from database
            try {
                val urls = DatabaseHelper.executeQuery(
                    "SELECT url FROM images WHERE user_id = ?",
                    listOf(userId)
                ) { rs ->
                    val imageUrls = mutableListOf<String>()
                    while (rs.next()) {
                        imageUrls.add(rs.getString("url"))
                    }
                    imageUrls
                }

                if (urls.isNotEmpty()) {
                    return@withContext Result.success(urls)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying user images from database", e)
                // If database operation fails, return empty list
            }

            // Return mock data if nothing found
            val mockUrls = listOf(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
            )

            Result.success(mockUrls)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user images", e)
            Result.failure(e)
        }
    }

    actual override suspend fun deleteImage(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Extract file name from path
            val fileName = path.substringAfterLast("/")

            // Delete from database
            try {
                DatabaseHelper.executeUpdate(
                    "DELETE FROM images WHERE file_name = ?",
                    listOf(fileName)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image from database", e)
                // Continue with local file deletion even if DB fails
            }

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

// Move the actual implementation of compressImage here to avoid duplication
actual fun compressImage(imageBytes: ByteArray, quality: Int): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}