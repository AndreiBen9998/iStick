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

// Improved Composable function with proper permission handling
@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current

    // Add permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Only launch camera if permission is granted
            cameraLauncher.launch(null)
        }
    }

    // Camera launcher
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

    return remember(cameraLauncher, permissionLauncher) {
        {
            // Check permission before launching camera
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, launch camera
                    cameraLauncher.launch(null)
                }
                else -> {
                    // Request the permission
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}