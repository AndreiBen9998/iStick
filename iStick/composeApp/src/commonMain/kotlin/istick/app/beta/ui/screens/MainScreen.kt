// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/MainScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import istick.app.beta.ui.navigation.AppNavigator
import istick.app.beta.utils.PerformanceMonitor

@Composable
fun MainScreen(
    appNavigator: AppNavigator,
    performanceMonitor: PerformanceMonitor,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("main_screen")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "iStick Main Screen",
                color = Color.White
            )

            Button(
                onClick = { onLogout() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Logout")
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("main_screen")
        }
    }
}