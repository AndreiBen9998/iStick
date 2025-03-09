// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/camera/CameraHelper.kt
package istick.app.beta.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

actual class CameraHelper(private val activity: Activity) {
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    actual fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    fun getBytesFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    // Launch camera method
    fun launchCamera(onResult: (Bitmap?) -> Unit) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        activity.startActivityForResult(intent, CAMERA_PERMISSION_CODE)
    }
}

// This is the composable function that your app is already using
@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current

    // Create a camera launcher using the new Activity Result API
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // If bitmap is not null, convert it to ByteArray and call the callback
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            onPhotoTaken(stream.toByteArray())
        }
    }

    return remember(cameraLauncher) {
        {
            // Check permission before launching camera
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraLauncher.launch(null)
            } else {
                // For a production app, you should request permission here
                // This is simplified for your existing code
                when (context) {
                    is Activity -> {
                        ActivityCompat.requestPermissions(
                            context,
                            arrayOf(Manifest.permission.CAMERA),
                            100
                        )
                    }
                }
            }
        }
    }
}