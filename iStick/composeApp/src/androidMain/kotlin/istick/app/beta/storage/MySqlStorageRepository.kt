package istick.app.beta.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import istick.app.beta.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * MySQL implementation of StorageRepository
 * This implementation stores images in the local file system and
 * stores references in the database
 */
actual class MySqlStorageRepository actual constructor(private val context: Any) : StorageRepository {
    private val androidContext get() = context as Context
    private val TAG = "MySqlStorageRepository"
    private val STORAGE_DIR = "app_images"

    actual override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Create storage directory if it doesn't exist
                val storageDir = File(androidContext.filesDir, STORAGE_DIR)
                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }

                // Check if file with this name already exists
                val existingPath = runBlocking {
                    DatabaseHelper.executeQuery(
                        "SELECT file_path FROM images WHERE filename = ?",
                        listOf<Any>(fileName)
                    ) { rs ->
                        if (rs.next()) rs.getString("file_path") else null
                    }
                }

                if (existingPath != null) {
                    // File exists, update it
                    val file = File(existingPath)
                    FileOutputStream(file).use { fos ->
                        fos.write(imageBytes)
                        fos.flush()
                    }
                } else {
                    // Create new file
                    val file = File(storageDir, fileName)
                    FileOutputStream(file).use { fos ->
                        fos.write(imageBytes)
                        fos.flush()
                    }

                    // Save reference in database
                    DatabaseHelper.executeUpdate(
                        "INSERT INTO images (filename, file_path, upload_date, user_id) VALUES (?, ?, ?, ?)",
                        listOf<Any>(
                            fileName,
                            file.absolutePath,
                            System.currentTimeMillis(),
                            extractUserIdFromPath(fileName) ?: "anonymous"
                        )
                    )
                }

                Result.success("local://$fileName")
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image: ${e.message}", e)
                Result.failure(e)
            }
        }

    actual override suspend fun getImageUrl(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Extract filename from path if it's a local path
                val filename = if (path.startsWith("local://")) {
                    path.substring("local://".length)
                } else {
                    path
                }

                // Check if file exists in database
                val filePath = DatabaseHelper.executeQuery(
                    "SELECT file_path FROM images WHERE filename = ?",
                    listOf<Any>(filename)
                ) { rs ->
                    if (rs.next()) rs.getString("file_path") else null
                }

                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        Result.success("local://$filename")
                    } else {
                        Result.failure(Exception("Image file not found"))
                    }
                } else {
                    Result.failure(Exception("Image record not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting image URL: ${e.message}", e)
                Result.failure(e)
            }
        }

    actual override suspend fun getUserImages(userId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val imageList = mutableListOf<String>()

                val fileNames = DatabaseHelper.executeQuery(
                    "SELECT filename FROM images WHERE user_id = ? ORDER BY upload_date DESC",
                    listOf<Any>(userId)
                ) { rs ->
                    val result = mutableListOf<String>()
                    while (rs.next()) {
                        result.add(rs.getString("filename"))
                    }
                    result
                }

                fileNames.forEach { fileName ->
                    imageList.add("local://$fileName")
                }

                Result.success(imageList)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user images: ${e.message}", e)
                Result.failure(e)
            }
        }

    actual override suspend fun deleteImage(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // Extract filename from path
                val filename = if (path.startsWith("local://")) {
                    path.substring("local://".length)
                } else {
                    path
                }

                // Get file path from database
                val filePath = DatabaseHelper.executeQuery(
                    "SELECT file_path FROM images WHERE filename = ?",
                    listOf<Any>(filename)
                ) { rs ->
                    if (rs.next()) rs.getString("file_path") else null
                }

                if (filePath != null) {
                    // Delete the physical file
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }

                    // Delete the database record
                    val rowsAffected = DatabaseHelper.executeUpdate(
                        "DELETE FROM images WHERE filename = ?",
                        listOf<Any>(filename)
                    )

                    Result.success(rowsAffected > 0)
                } else {
                    Result.success(false) // Nothing to delete
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image: ${e.message}", e)
                Result.failure(e)
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
                    listOf<Any>(filename)
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
        // Example: user_123_image.jpg -> returns "123"
        val pattern = "user_(\\w+)_.*".toRegex()
        val matchResult = pattern.find(path)
        return matchResult?.groupValues?.getOrNull(1)
    }
}