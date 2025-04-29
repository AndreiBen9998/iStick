// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/App.kt

package istick.app.beta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.di.DependencyInjection
import istick.app.beta.model.Campaign
import istick.app.beta.model.Car
import istick.app.beta.model.User
import istick.app.beta.ui.navigation.AppNavigator
import istick.app.beta.ui.screens.*
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.utils.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Update AppState to include INITIALIZATION and ERROR states
enum class AppState {
    INITIALIZATION,
    ERROR,
    MIGRATION,
    INTRO,
    LOGIN,
    REGISTRATION,
    MAIN
}

@Composable
fun App() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // App state
    var appState by remember { mutableStateOf(AppState.INITIALIZATION) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialization state
    var initAttempts by remember { mutableStateOf(0) }
    var isRetrying by remember { mutableStateOf(false) }

    // Platform-specific context handling
    val platformContext = getPlatformContextComposable()

    // Initialize performance monitor
    val performanceMonitor = remember {
        PerformanceMonitor(platformContext ?: context)
    }

    // Initialize preferences
    val preferences = remember { Preferences() }

    // Start initialization if needed
    LaunchedEffect(Unit) {
        if (appState == AppState.INITIALIZATION) {
            performanceMonitor.startTrace("app_startup")

            // Initialize the app
            initAttempts++
            AppInitializer.initialize(context as android.content.Context) {
                if (!AppInitializer.hasCrashed()) {
                    // Initialization succeeded
                    try {
                        // Check if user has seen intro
                        val hasSeenIntro = preferences.hasSeenIntro()

                        // Check if user is already logged in
                        val authRepository = DependencyInjection.getAuthRepository()
                        val isLoggedIn = authRepository.isUserLoggedIn()

                        appState = when {
                            !hasSeenIntro -> AppState.INTRO
                            isLoggedIn -> {
                                // If logged in, track auto-login in analytics
                                try {
                                    val analyticsManager = DependencyInjection.getAnalyticsManager()
                                    analyticsManager.trackLogin("auto")
                                } catch (e: Exception) {
                                    // Log but don't fail if analytics fails
                                }
                                AppState.MAIN
                            }
                            else -> AppState.LOGIN
                        }
                    } catch (e: Exception) {
                        // If determining the next state fails, default to intro or login
                        val hasSeenIntro = try {
                            preferences.hasSeenIntro()
                        } catch (e: Exception) {
                            false
                        }

                        appState = if (!hasSeenIntro) AppState.INTRO else AppState.LOGIN
                    }
                } else {
                    // Initialization failed, but we'll continue anyway
                    // with default state
                    val hasSeenIntro = try {
                        preferences.hasSeenIntro()
                    } catch (e: Exception) {
                        false
                    }

                    appState = if (!hasSeenIntro) AppState.INTRO else AppState.LOGIN
                }
            }
        }
    }

    // Listen for network changes
    LaunchedEffect(Unit) {
        try {
            val networkMonitor = DependencyInjection.getNetworkMonitor()
            networkMonitor.isOnline.collectLatest { isOnline ->
                if (!isOnline) {
                    // We're offline, show a snackbar or some indication
                    // This doesn't change the app state
                }
            }
        } catch (e: Exception) {
            // Failed to get network monitor, just continue
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
            // Initialization screen
            AnimatedVisibility(
                visible = appState == AppState.INITIALIZATION,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InitializationScreen(
                    isRetrying = isRetrying,
                    onRetry = {
                        if (initAttempts < 3) {
                            // Try initialization again
                            isRetrying = true
                            coroutineScope.launch {
                                // Cleanup first
                                AppInitializer.cleanup()

                                // Short delay
                                delay(1000)

                                // Reinitialize
                                initAttempts++
                                AppInitializer.initialize(context as android.content.Context) {
                                    isRetrying = false
                                    if (!AppInitializer.hasCrashed()) {
                                        // Initialization succeeded on retry
                                        // Check if user has seen intro
                                        val hasSeenIntro = try {
                                            preferences.hasSeenIntro()
                                        } catch (e: Exception) {
                                            false
                                        }

                                        // Move to appropriate state
                                        appState = if (!hasSeenIntro) AppState.INTRO else AppState.LOGIN
                                    } else {
                                        // Still failed, show error
                                        appState = AppState.ERROR
                                        errorMessage = "Could not initialize app. Please restart."
                                    }
                                }
                            }
                        } else {
                            // Too many attempts, show error
                            appState = AppState.ERROR
                            errorMessage = "Could not initialize app after multiple attempts. Please restart."
                        }
                    }
                )
            }

            // Error screen
            AnimatedVisibility(
                visible = appState == AppState.ERROR,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ErrorScreen(
                    errorMessage = errorMessage ?: "Unknown error occurred",
                    onRetry = {
                        // Go back to initialization
                        appState = AppState.INITIALIZATION
                        errorMessage = null
                    }
                )
            }

            // Migration screen
            AnimatedVisibility(
                visible = appState == AppState.MIGRATION,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MigrationScreen(
                    progress = 0,
                    error = null
                )
            }

            // Introduction screens
            AnimatedVisibility(
                visible = appState == AppState.INTRO,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IntroScreen(
                    onFinishIntro = {
                        // Save that user has seen intro
                        preferences.setIntroSeen()

                        // Navigate to login or main based on login status
                        appState = try {
                            val authRepository = DependencyInjection.getAuthRepository()
                            if (authRepository.isUserLoggedIn()) AppState.MAIN else AppState.LOGIN
                        } catch (e: Exception) {
                            // If checking login status fails, default to login
                            AppState.LOGIN
                        }
                    },
                    performanceMonitor = performanceMonitor
                )
            }

            // Login screen
            AnimatedVisibility(
                visible = appState == AppState.LOGIN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        // Track login in analytics
                        try {
                            val analyticsManager = DependencyInjection.getAnalyticsManager()
                            analyticsManager.trackLogin("email")
                        } catch (e: Exception) {
                            // Log but don't fail if analytics fails
                        }

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

            // Main screen
            AnimatedVisibility(
                visible = appState == AppState.MAIN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MainScreen(
                    appNavigator = try {
                        DependencyInjection.getAppNavigator()
                    } catch (e: Exception) {
                        // If getting the navigator fails, create a new one
                        AppNavigator(performanceMonitor)
                    },
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

                // Clean up resources
                try {
                    val networkMonitor = DependencyInjection.getNetworkMonitor()
                    networkMonitor.stopMonitoring()
                } catch (e: Exception) {
                    // Failed to stop network monitor, just continue
                }

                // Final cleanup
                AppInitializer.cleanup()
            } catch (e: Exception) {
                // Log but continue
            }
        }
    }
}

// Initialization screen
@Composable
fun InitializationScreen(
    isRetrying: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Logo or app name
            Text(
                text = "iStick",
                color = Color(0xFF2962FF),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading indicator
            CircularProgressIndicator(
                color = Color(0xFF2962FF),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isRetrying) "Restarting..." else "Initializing...",
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Retry button (only show if not already retrying)
            if (!isRetrying) {
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2962FF)
                    ),
                    border = ButtonDefaults.outlinedBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2962FF))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF2962FF)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Retry")
                }
            }
        }
    }
}

// Error screen
@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Error",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2962FF)
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

// Migration screen - existing code
@Composable
fun MigrationScreen(
    progress: Int,
    error: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1929)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Migrating Data",
                style = MaterialTheme.typography.h5,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2962FF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$progress%",
                color = Color.Gray
            )

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Error: $it",
                    color = Color.Red
                )
            }
        }
    }
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