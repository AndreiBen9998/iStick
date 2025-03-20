// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/storage/FirebaseStorageRepositoryExt.kt

package istick.app.beta.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

actual fun FirebaseStorageRepository.compressPlatformImage(imageBytes: ByteArray, quality: Int): ByteArray {
    // Decode the image
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // Compress to JPEG with reduced quality
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

    return outputStream.toByteArray()
}
