// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/camera/CameraHelper.kt
package istick.app.beta.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

actual class CameraHelper {
    private var activity: Activity? = null

    // This is now a method, not a constructor
    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    actual fun checkCameraPermission(): Boolean {
        val ctx = activity ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual fun requestCameraPermission() {
        val act = activity ?: return
        ActivityCompat.requestPermissions(
            act,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun getBytesFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
}

@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current

    // Camera launcher - defined first to be referenced later
    val cameraPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // If bitmap is not null, convert it to ByteArray and call the callback
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            onPhotoTaken(stream.toByteArray())
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Only launch camera if permission is granted
            cameraPictureLauncher.launch(null)
        }
    }

    return remember(cameraPictureLauncher, permissionLauncher) {
        {
            // Check permission before launching camera
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, launch camera
                    cameraPictureLauncher.launch(null)
                }
                else -> {
                    // Request the permission
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}