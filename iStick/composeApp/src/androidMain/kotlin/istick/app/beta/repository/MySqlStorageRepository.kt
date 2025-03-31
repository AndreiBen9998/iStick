// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/storage/MySqlStorageRepository.kt
package istick.app.beta.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import istick.app.beta.storage.StorageRepository
/**
 * MySQL implementation of StorageRepository
 * This implementation stores images in the local file system and
 * stores references in the database
 */

// Add this function to the MySqlStorageRepository class
private fun compressImage(imageBytes: ByteArray, quality: Int): ByteArray {
    // Decode the image
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // Compress to JPEG with reduced quality
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

    return outputStream.toByteArray()
}
class MySqlStorageRepository(private val context: Context) : StorageRepository {
    private val TAG = "MySqlStorageRepository"
    private val STORAGE_DIR = "app_images"

    override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // First save the image to local storage
            val storageDir = File(context.filesDir, STORAGE_DIR).apply {
                if (!exists()) mkdirs()
            }

            // Create file
            val imageFile = File(storageDir, fileName)

            // Compress the image before saving
            val compressedBytes = compressImage(imageBytes, 80)

            // Write to file - Fix the ambiguity by specifying parameter types
            FileOutputStream(imageFile).use { output ->
                output.write(compressedBytes, 0, compressedBytes.size)
            }
            
            // Create record in database
            val imageId = DatabaseHelper.executeInsert(
                "INSERT INTO images (filename, file_path, created_at) VALUES (?, ?, NOW())",
                listOf(fileName, imageFile.absolutePath)
            )
            
            if (imageId > 0) {
                // Return path that can be used to retrieve the image
                val imageUrl = "local://$fileName"
                
                // Extract user ID if present in filename
                val userId = extractUserIdFromPath(fileName)
                if (userId != null) {
                    // Associate with user
                    DatabaseHelper.executeUpdate(
                        "INSERT INTO user_images (user_id, image_id) VALUES (?, ?)",
                        listOf(userId.toLong(), imageId)
                    )
                }
                
                return@withContext Result.success(imageUrl)
            } else {
                return@withContext Result.failure(Exception("Failed to record image in database"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun getImageUrl(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if this is already a local:// URL
            if (path.startsWith("local://")) {
                return@withContext Result.success(path)
            }
            
            // Look up in database
            val imageUrl = DatabaseHelper.executeQuery(
                "SELECT filename FROM images WHERE file_path = ?",
                listOf(path)
            ) { resultSet ->
                if (resultSet.next()) {
                    "local://${resultSet.getString("filename")}"
                } else {
                    null
                }
            }
            
            if (imageUrl != null) {
                return@withContext Result.success(imageUrl)
            } else {
                return@withContext Result.failure(Exception("Image not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image URL: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun getUserImages(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val images = DatabaseHelper.executeQuery(
                """
                SELECT i.filename
                FROM images i
                JOIN user_images ui ON i.id = ui.image_id
                WHERE ui.user_id = ?
                ORDER BY i.created_at DESC
                """,
                listOf(userId.toLong())
            ) { resultSet ->
                val imageUrls = mutableListOf<String>()
                while (resultSet.next()) {
                    val filename = resultSet.getString("filename")
                    imageUrls.add("local://$filename")
                }
                imageUrls
            }
            
            return@withContext Result.success(images)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user images: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun deleteImage(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Extract filename from path
            val filename = if (path.startsWith("local://")) {
                path.substring("local://".length)
            } else {
                path
            }
            
            // Find image in database
            val imageRecord = DatabaseHelper.executeQuery(
                "SELECT id, file_path FROM images WHERE filename = ?",
                listOf(filename)
            ) { resultSet ->
                if (resultSet.next()) {
                    Pair(
                        resultSet.getLong("id"),
                        resultSet.getString("file_path")
                    )
                } else {
                    null
                }
            }
            
            if (imageRecord != null) {
                val (imageId, filePath) = imageRecord
                
                // Delete from user_images
                DatabaseHelper.executeUpdate(
                    "DELETE FROM user_images WHERE image_id = ?",
                    listOf(imageId)
                )
                
                // Delete from images table
                DatabaseHelper.executeUpdate(
                    "DELETE FROM images WHERE id = ?",
                    listOf(imageId)
                )
                
                // Delete file from disk
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                return@withContext Result.success(true)
            } else {
                return@withContext Result.failure(Exception("Image not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Helper method to load an image from the local file system
     */
    fun loadImage(path: String): Bitmap? {
        try {
            // Extract filename from path
            val filename = if (path.startsWith("local://")) {
                path.substring("local://".length)
            } else {
                path
            }
            
            // Look up the file path
            val filePath = runBlocking {
                DatabaseHelper.executeQuery(
                    "SELECT file_path FROM images WHERE filename = ?",
                    listOf(filename)
                ) { resultSet ->
                    if (resultSet.next()) resultSet.getString("file_path") else null
                }
            }
            
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    return BitmapFactory.decodeFile(file.absolutePath)
                }
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}", e)
            return null
        }
    }
    
    private fun extractUserIdFromPath(path: String): String? {
        // Expected patterns:
        // profiles/profile_userId_timestamp.jpg
        // cars/car_userId_timestamp.jpg
        
        return when {
            path.startsWith("profiles/profile_") -> {
                val parts = path.substringAfter("profiles/profile_").split("_")
                if (parts.size >= 2) parts[0] else null
            }
            path.startsWith("cars/car_") -> {
                val parts = path.substringAfter("cars/car_").split("_")
                if (parts.size >= 2) parts[0] else null
            }
            else -> null
        }
    }
    
    // Helper to use coroutines in non-suspending functions
    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) { block() }
    }
}

