package istick.app.beta

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app with the Activity context
        AppInitializer.initialize(this) {
            // Set content after initialization to prevent crashes
            setContent {
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppInitializer.cleanup()
    }
}