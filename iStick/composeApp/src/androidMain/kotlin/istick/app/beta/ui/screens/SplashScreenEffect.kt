// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/ui/screens/SplashScreenEffect.android.kt

package istick.app.beta.ui.screens

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
actual fun PlatformSplashScreenEffect() {
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
}