// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/ui/screens/SplashScreen.kt
package istick.app.beta.ui.screens

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView

/**
 * Android-specific SplashScreen customization. This is optional and only needed
 * if you need Android-specific behavior like setting system UI visibility.
 */
@Composable
actual fun PlatformSplashScreen(onTimeout: () -> Unit, modifier: Modifier) {
    // Get the Android View
    val view = LocalView.current

    // Hide system UI for a more immersive splash screen experience
    DisposableEffect(Unit) {
        val systemUiVisibility = view.systemUiVisibility
        view.systemUiVisibility = systemUiVisibility or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        onDispose {
            // Restore original UI visibility when done
            view.systemUiVisibility = systemUiVisibility
        }
    }

    // Call the common SplashScreen implementation
    CommonSplashScreen(onTimeout, modifier)
}