// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/LoginScreen.kt
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
import istick.app.beta.utils.PerformanceMonitor

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("login_screen")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A1929)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "iStick Login",
                color = Color.White
            )

            Button(
                onClick = { onLoginSuccess() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Login")
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("login_screen")
        }
    }
}