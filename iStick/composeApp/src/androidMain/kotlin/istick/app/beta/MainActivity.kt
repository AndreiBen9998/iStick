// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/MainActivity.kt

package istick.app.beta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import istick.app.beta.database.DatabaseHelper
import istick.app.beta.repository.RepositoryFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database
        try {
            DatabaseHelper.initialize()
        } catch (e: Exception) {
            // Handle initialization error
            e.printStackTrace()
        }

        // Initialize application with MySQL
        AppInitializer.initialize(applicationContext)

        // Set data source to MySQL
        RepositoryFactory.currentDataSource = RepositoryFactory.DataSource.MYSQL

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        AppInitializer.cleanup()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}