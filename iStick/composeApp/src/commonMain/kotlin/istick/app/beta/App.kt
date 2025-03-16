// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/App.kt

package istick.app.beta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import istick.app.beta.auth.FirebaseAuthRepository
import istick.app.beta.di.DependencyInjection
import istick.app.beta.migration.DataMigrationManager
import istick.app.beta.migration.DataMigrationManager.MigrationState
import istick.app.beta.repository.FirebaseCarRepository
import istick.app.beta.repository.FirebaseCampaignRepository
import istick.app.beta.repository.FirebaseUserRepository
import istick.app.beta.ui.navigation.AppNavigator
import istick.app.beta.ui.screens.*
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.utils.Preferences
import kotlinx.coroutines.launch

// Update AppState to include MIGRATION state
enum class AppState {
    MIGRATION,
    INTRO,
    LOGIN,
    REGISTRATION,
    MAIN
}

@Composable
fun App() {
    // Start Firebase initialization early
    LaunchedEffect(Unit) {
        FirebaseInitializer.initialize()
    }

    // Get context - platform-specific
    val context = LocalContext.current

    // Initialize performance monitor
    val performanceMonitor = remember { PerformanceMonitor(context) }

    // Initialize preferences
    val preferences = remember { Preferences() }

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // App state
    var appState by remember { mutableStateOf(AppState.INTRO) }

    // Migration state
    var migrationProgress by remember { mutableStateOf(0) }
    var migrationError by remember { mutableStateOf<String?>(null) }

    // Initialize repositories
    val authRepository = remember { DependencyInjection.getAuthRepository() }
    val userRepository = remember { DependencyInjection.getUserRepository() }
    val appNavigator = remember { DependencyInjection.getAppNavigator() }
    val networkMonitor = remember { DependencyInjection.getNetworkMonitor() }
    val analyticsManager = remember { DependencyInjection.getAnalyticsManager() }

    // Start initialization
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("app_startup")

        // Check if user has seen intro
        val hasSeenIntro = preferences.hasSeenIntro()

        // Initialize repositories
        scope.launch {
            try {
                // Check if migration is needed
                val migrationManager = DataMigrationManager(
                    userRepository = userRepository as FirebaseUserRepository,
                    carRepository = DependencyInjection.getCarRepository() as FirebaseCarRepository,
                    campaignRepository = DependencyInjection.getCampaignRepository() as FirebaseCampaignRepository,
                    storageRepository = DependencyInjection.getStorageRepository()
                )

                if (migrationManager.isMigrationNeeded()) {
                    // Show migration UI
                    appState = AppState.MIGRATION

                    // Start migration with mock data (or retrieve from local storage)
                    val mockUsers = getMockUsers()
                    val mockCars = getMockCars()
                    val mockCampaigns = getMockCampaigns()

                    migrationManager.startMigration(mockUsers, mockCars, mockCampaigns)

                    // Observe migration state
                    migrationManager.migrationState.collect { state ->
                        when (state) {
                            is MigrationState.InProgress -> migrationProgress = state.percentComplete
                            is MigrationState.Completed -> {
                                // Check login status and intro status
                                if (!hasSeenIntro) {
                                    appState = AppState.INTRO
                                } else if (authRepository.isUserLoggedIn()) {
                                    appState = AppState.MAIN

                                    // Track analytics
                                    analyticsManager.trackLogin("auto")
                                } else {
                                    appState = AppState.LOGIN
                                }
                            }
                            is MigrationState.Failed -> {
                                migrationError = state.error
                                // If migration fails, proceed to normal flow
                                if (!hasSeenIntro) {
                                    appState = AppState.INTRO
                                } else {
                                    appState = AppState.LOGIN
                                }
                            }
                            else -> {}
                        }
                    }
                } else {
                    // No migration needed, proceed with normal flow

                    // Check if user is already logged in
                    val isLoggedIn = authRepository.isUserLoggedIn()

                    // Set initial screen based on intro status and login status
                    appState = when {
                        !hasSeenIntro -> AppState.INTRO
                        isLoggedIn -> {
                            // Track auto-login in analytics
                            analyticsManager.trackLogin("auto")
                            AppState.MAIN
                        }
                        else -> AppState.LOGIN
                    }
                }
            } catch (e: Exception) {
                println("Error during initialization: ${e.message}")

                // Default to intro or login screen
                appState = if (!hasSeenIntro) AppState.INTRO else AppState.LOGIN

                // Track error
                analyticsManager.trackError(
                    errorType = "initialization_error",
                    errorMessage = e.message ?: "Unknown error"
                )
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
            // Migration screen
            AnimatedVisibility(
                visible = appState == AppState.MIGRATION,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MigrationScreen(
                    progress = migrationProgress,
                    error = migrationError
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
                        appState = if (authRepository.isUserLoggedIn()) AppState.MAIN else AppState.LOGIN
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
                        analyticsManager.trackLogin("email")

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
                networkMonitor.stopMonitoring()
            } catch (e: Exception) {
                println("Error stopping performance monitoring: ${e.message}")
            }
        }
    }
}

// A simple migration screen component
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

            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Error: $error",
                    color = Color.Red
                )
            }
        }
    }
}

// Helper function to get mock users - in a real app, you'd retrieve these from local storage
private fun getMockUsers(): List<User> {
    // Return mock user data
    return emptyList()
}

// Helper function to get mock cars - in a real app, you'd retrieve these from local storage
private fun getMockCars(): List<Car> {
    // Return mock car data
    return emptyList()
}

// Helper function to get mock campaigns - in a real app, you'd retrieve these from local storage
private fun getMockCampaigns(): List<Campaign> {
    // Return mock campaign data
    return emptyList()
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