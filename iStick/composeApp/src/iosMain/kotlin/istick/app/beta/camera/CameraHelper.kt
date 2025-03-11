// File: iStick/composeApp/src/iosMain/kotlin/istick/app/beta/camera/CameraHelper.kt
package istick.app.beta.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSLog
import platform.UIKit.UIApplication
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

/**
 * iOS implementation of CameraHelper
 */
actual class CameraHelper {
    actual fun checkCameraPermission(): Boolean {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return status == AVAuthorizationStatusAuthorized
    }

    actual fun requestCameraPermission() {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            if (granted) {
                NSLog("Camera permission granted")
            } else {
                NSLog("Camera permission denied")
            }
        }
    }

    // Helper function to get current status as string
    fun getCameraAuthStatusString(): String {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> "Authorized"
            AVAuthorizationStatusDenied -> "Denied"
            AVAuthorizationStatusRestricted -> "Restricted"
            AVAuthorizationStatusNotDetermined -> "Not Determined"
            else -> "Unknown"
        }
    }
}

/**
 * iOS implementation of camera launcher
 * Note: This is still a basic implementation. In a real app,
 * you'd implement UIImagePickerController.
 */
@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (ByteArray) -> Unit): () -> Unit {
    val helper = remember { CameraHelper() }

    return remember {
        {
            NSLog("Camera launcher: Current status: ${helper.getCameraAuthStatusString()}")

            if (helper.checkCameraPermission()) {
                NSLog("Camera permission already granted, would launch camera")
                // In a real implementation, you would launch UIImagePickerController here
                // and handle the photo result
            } else {
                NSLog("Requesting camera permission")
                helper.requestCameraPermission()
            }
        }
    }
}