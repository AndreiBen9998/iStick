package istick.app.beta

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app
        AppInitializer.initialize(this)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppInitializer.cleanup()
    }
}