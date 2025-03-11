// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/camera/CameraHelper.common.kt
package istick.app.beta.camera

import androidx.compose.runtime.Composable

/**
 * Common interface for camera functionality across platforms
 */
expect class CameraHelper {
    // No constructor parameters in the expect declaration
    fun checkCameraPermission(): Boolean
    fun requestCameraPermission()
}

@Composable
expect fun rememberCameraLauncher(onPhotoTaken: (ByteArray) -> Unit): () -> Unit