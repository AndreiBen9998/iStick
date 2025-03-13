// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/App.kt
package istick.app.beta

// At the top of App.kt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
// Add other icon imports as needed
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
import kotlinx.coroutines.launch


@Composable
fun App() {
    // Get context
    val context = LocalContext.current

    // Initialize performance monitor
    val performanceMonitor = remember { PerformanceMonitor(context) }

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // Start tracking app startup
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("app_startup")

        // Initialize Firebase
        scope.launch {
            try {
                FirebaseInitializer.initialize()
            } catch (e: Exception) {
                println("Error initializing Firebase: ${e.message}")
            }
        }
    }

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

    // Check if user is already logged in on app start
    LaunchedEffect(Unit) {
        try {
            isLoggedIn = authRepository.isUserLoggedIn()
        } catch (e: Exception) {
            println("Error checking login status: ${e.message}")
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
            color = Color(0xFF0F2030)
        ) {
            // Splash screen
            AnimatedVisibility(
                visible = appState == AppState.SPLASH,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SplashScreen(
                    onTimeout = {
                        appState = if (isLoggedIn) AppState.MAIN else AppState.LOGIN
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