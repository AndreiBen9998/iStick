// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/navigation/NavigationSystem.kt
package istick.app.beta.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import istick.app.beta.ui.navigation.AppNavigator.Screen
import istick.app.beta.ui.screens.CampaignListScreen
import istick.app.beta.ui.screens.CarManagementScreen
import istick.app.beta.ui.screens.MileageVerificationScreen
import istick.app.beta.ui.screens.ProfileScreen
import istick.app.beta.utils.PerformanceMonitor

/**
 * Main navigation system for the app. Handles the bottom navigation
 * and screen switching.
 */
@Composable
fun NavigationSystem(
    appNavigator: AppNavigator,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for current screen
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    // Error state
    var navigationError by remember { mutableStateOf<String?>(null) }

    // Create view models
    val homeViewModel = remember { appNavigator.createHomeViewModel() }
    val profileViewModel = remember { appNavigator.createProfileViewModel() }
    val carManagementViewModel = remember { appNavigator.createCarManagementViewModel() }

    // Navigation items
    val navItems = listOf(
        NavItem(
            route = Screen.Home,
            icon = Icons.Default.Home,
            label = "Home"
        ),
        NavItem(
            route = Screen.CarManagement,
            icon = Icons.Default.AccountCircle, // Replacement for DirectionsCar
            label = "Cars"
        ),
        NavItem(
            route = Screen.Photos,
            icon = Icons.Default.AccountCircle, // Replacement for Photo
            label = "Photos"
        ),
        NavItem(
            route = Screen.Profile,
            icon = Icons.Default.AccountCircle,
            label = "Profile"
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color(0xFF0A1929),
                contentColor = Color.White
            ) {
                navItems.forEach { item ->
                    val selected = currentScreen == item.route

                    BottomNavigationItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            currentScreen = item.route
                            if (navigationError != null) {
                                navigationError = null
                            }
                        },
                        selectedContentColor = Color(0xFF2962FF),
                        unselectedContentColor = Color.Gray
                    )
                }
            }
        }
    ) { paddingValues ->
        // Content area
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Home screen
            AnimatedVisibility(
                visible = currentScreen == Screen.Home,
                enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(),
                exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
            ) {
                CampaignListScreen(
                    viewModel = homeViewModel,
                    performanceMonitor = appNavigator.performanceMonitor,
                    onCampaignClick = { campaign ->
                        // Navigate to campaign detail
                        currentScreen = Screen.CampaignDetail(campaign.id)
                    }
                )
            }

            // Car management screen
            AnimatedVisibility(
                visible = currentScreen == Screen.CarManagement,  // FIXED: This was incorrectly set to Home
                enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(),
                exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
            ) {
                CarManagementScreen(
                    viewModel = carManagementViewModel,
                    performanceMonitor = appNavigator.performanceMonitor,
                    userId = appNavigator.authRepository.getCurrentUserId() ?: "",
                    onBackClick = {
                        currentScreen = Screen.Home
                    },
                    onAddCarClick = {
                        currentScreen = Screen.AddEditCar()
                    },
                    onCarClick = { carId ->
                        currentScreen = Screen.AddEditCar(carId)
                    },
                    onVerifyMileageClick = { carId ->
                        // New navigation to mileage verification
                        currentScreen = Screen.MileageVerification(carId)
                    }
                )
            }

            // Profile screen
            AnimatedVisibility(
                visible = currentScreen == Screen.Profile,
                enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(),
                exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
            ) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    performanceMonitor = appNavigator.performanceMonitor,
                    onLogout = onLogout
                )
            }

            // Photos screen placeholder
            AnimatedVisibility(
                visible = currentScreen == Screen.Photos,
                enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(),
                exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
            ) {
                // Placeholder - to be implemented
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Photos Screen - Coming Soon", color = Color.White)
                }
            }

            // Mileage Verification screen
            AnimatedVisibility(
                visible = currentScreen is Screen.MileageVerification,
                enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(),
                exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
            ) {
                val carId = (currentScreen as? Screen.MileageVerification)?.carId ?: ""
                val viewModel = remember { appNavigator.createMileageVerificationViewModel() }

                MileageVerificationScreen(
                    viewModel = viewModel,
                    carId = carId,
                    onSuccess = {
                        // Navigate back to car management
                        currentScreen = Screen.CarManagement
                    },
                    onCancel = {
                        // Go back
                        currentScreen = Screen.CarManagement
                    },
                    performanceMonitor = appNavigator.performanceMonitor
                )
            }

            // Display any navigation errors
            if (navigationError != null) {
                Text(navigationError ?: "", color = Color.Red)
            }
        }
    }
}

/**
 * Data class for bottom navigation items
 */
data class NavItem(
    val route: Screen,
    val icon: ImageVector,
    val label: String
)