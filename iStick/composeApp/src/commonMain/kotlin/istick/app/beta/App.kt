// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/App.kt
package istick.app.beta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import istick.app.beta.auth.FirebaseAuthRepository
import istick.app.beta.ui.navigation.AppNavigator
import istick.app.beta.ui.screens.LoginScreen
import istick.app.beta.ui.screens.MainScreen
import istick.app.beta.utils.PerformanceMonitor

@Composable
fun App() {
    // Initialize Firebase
    LaunchedEffect(Unit) {
        FirebaseInitializer.initialize()
    }

    // Initialize repositories and performance monitor
    val context = LocalContext.current
    val performanceMonitor = remember { PerformanceMonitor(context) }
    val authRepository = remember { FirebaseAuthRepository() }
    val appNavigator = remember { AppNavigator(authRepository = authRepository, performanceMonitor = performanceMonitor) }

    // Track if user is logged in
    var isLoggedIn by remember { mutableStateOf(false) }

    // Check if user is already logged in on app start
    LaunchedEffect(Unit) {
        isLoggedIn = authRepository.isUserLoggedIn()
        performanceMonitor.startTrace("app_startup")
    }

    // Material theme with dark colors
    MaterialTheme(
        colors = darkColors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F2030)
        ) {
            // Show login screen or main screen based on auth status
            AnimatedVisibility(
                visible = !isLoggedIn,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                    },
                    performanceMonitor = performanceMonitor
                )
            }

            AnimatedVisibility(
                visible = isLoggedIn,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MainScreen(
                    appNavigator = appNavigator,
                    performanceMonitor = performanceMonitor,
                    onLogout = {
                        isLoggedIn = false
                    }
                )
            }
        }
    }

    // Stop performance tracking when app exits
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("app_startup")
            performanceMonitor.monitorMemory()
        }
    }
}

// Custom dark color palette
private val darkColors = androidx.compose.material.darkColors(
    primary = Color(0xFF2962FF),
    primaryVariant = Color(0xFF0039CB),
    secondary = Color(0xFF00BFA5),
    background = Color(0xFF0F2030),
    surface = Color(0xFF1A3B66),
    error = Color(0xFFFF5252),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)