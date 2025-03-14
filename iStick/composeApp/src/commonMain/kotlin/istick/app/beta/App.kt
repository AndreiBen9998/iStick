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
import istick.app.beta.repository.FirebaseUserRepository
import istick.app.beta.ui.navigation.AppNavigator
import istick.app.beta.ui.screens.LoginScreen
import istick.app.beta.ui.screens.MainScreen
import istick.app.beta.ui.screens.RegistrationScreen
import istick.app.beta.ui.screens.SplashScreen
import istick.app.beta.utils.PerformanceMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    // Get context
    val context = LocalContext.current

    // Initialize performance monitor
    val performanceMonitor = remember { PerformanceMonitor(context) }

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // Initialize repositories
    val authRepository = remember { FirebaseAuthRepository() }
    val userRepository = remember { FirebaseUserRepository(authRepository) }

    val appNavigator = remember {
        AppNavigator(
            authRepository = authRepository,
            userRepository = userRepository,
            performanceMonitor = performanceMonitor
        )
    }

    // Track if user is logged in
    var isLoggedIn by remember { mutableStateOf(false) }

    // Track app state with splash screen
    var appState by remember { mutableStateOf(AppState.SPLASH) }

    // Start tracking app startup and handle initialization
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("app_startup")

        // Initialize Firebase
        scope.launch {
            try {
                FirebaseInitializer.initialize()

                // Ensure we stay in splash screen for minimum time for better UX
                delay(2000) // Minimum 2 seconds splash screen

                // Check login status
                isLoggedIn = authRepository.isUserLoggedIn()

                // Move to next screen based on login status
                if (appState == AppState.SPLASH) {
                    appState = if (isLoggedIn) AppState.MAIN else AppState.LOGIN
                }
            } catch (e: Exception) {
                println("Error initializing Firebase: ${e.message}")

                // Still move to login screen even if there was an error
                delay(2000)
                if (appState == AppState.SPLASH) {
                    appState = AppState.LOGIN
                }
            }
        }
    }

    // Material theme with dark colors
    MaterialTheme(
        colors = darkColors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A1929)
        ) {
            // Splash screen
            AnimatedVisibility(
                visible = appState == AppState.SPLASH,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SplashScreen(
                    onTimeout = {
                        // We handle the transition in the LaunchedEffect above
                    }
                )
            }

            // Show login screen or main screen based on auth status
            AnimatedVisibility(
                visible = appState == AppState.LOGIN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        appState = AppState.MAIN
                    },
                    onNavigateToRegister = {
                        appState = AppState.REGISTRATION
                    },
                    performanceMonitor = performanceMonitor
                )
            }

            // Registration screen
            AnimatedVisibility(
                visible = appState == AppState.REGISTRATION,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                RegistrationScreen(
                    onRegistrationSuccess = {
                        appState = AppState.MAIN
                    },
                    onBackToLogin = {
                        appState = AppState.LOGIN
                    },
                    performanceMonitor = performanceMonitor
                )
            }

            AnimatedVisibility(
                visible = appState == AppState.MAIN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MainScreen(
                    appNavigator = appNavigator,
                    performanceMonitor = performanceMonitor,
                    onLogout = {
                        appState = AppState.LOGIN
                    }
                )
            }
        }
    }

    // Stop performance tracking when app exits
    DisposableEffect(Unit) {
        onDispose {
            try {
                performanceMonitor.stopTrace("app_startup")
                performanceMonitor.monitorMemory()
            } catch (e: Exception) {
                println("Error stopping performance monitoring: ${e.message}")
            }
        }
    }
}

// App states with added SPLASH state
enum class AppState {
    SPLASH,
    LOGIN,
    REGISTRATION,
    MAIN
}

// Custom dark color palette with iStick branding
private val darkColors = androidx.compose.material.darkColors(
    primary = Color(0xFF1FBDFF),       // iStick blue from logo
    primaryVariant = Color(0xFF0084BE), // Darker blue variant
    secondary = Color(0xFF29ABE2),     // Secondary blue from logo
    background = Color(0xFF0A1929),    // Dark navy background
    surface = Color(0xFF1A3B66),       // Slightly lighter navy for cards
    error = Color(0xFFFF5252),         // Error red
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)