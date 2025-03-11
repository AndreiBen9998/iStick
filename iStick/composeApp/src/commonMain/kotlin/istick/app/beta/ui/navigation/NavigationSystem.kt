// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/navigation/NavigationSystem.kt
package istick.app.beta.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Photo
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

    try {
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
                icon = Icons.Default.DirectionsCar,
                label = "Cars"
            ),
            NavItem(
                route = Screen.Photos,
                icon = Icons.Default.Photo,
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
                                try {
                                    currentScreen = item.route
                                } catch (e: Exception) {
                                    navigationError = "Navigation error: ${e.message}"
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
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    try {
                        CampaignListScreen(
                            viewModel = homeViewModel,
                            performanceMonitor = appNavigator.performanceMonitor,
                            onCampaignClick = { campaign ->
                                // Navigate to campaign detail
                                try {
                                    currentScreen = Screen.CampaignDetail(campaign.id)
                                } catch (e: Exception) {
                                    navigationError = "Navigation error: ${e.message}"
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Text("Error loading Home screen: ${e.message}", color = Color.Red)
                    }
                }

                // Car management screen
                AnimatedVisibility(
                    visible = currentScreen == Screen.CarManagement,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    try {
                        CarManagementScreen(
                            viewModel = carManagementViewModel,
                            performanceMonitor = appNavigator.performanceMonitor,
                            userId = appNavigator.authRepository.getCurrentUserId() ?: "",
                            onBackClick = {
                                try {
                                    currentScreen = Screen.Home
                                } catch (e: Exception) {
                                    navigationError = "Navigation error: ${e.message}"
                                }
                            },
                            onAddCarClick = {
                                try {
                                    currentScreen = Screen.AddEditCar()
                                } catch (e: Exception) {
                                    navigationError = "Navigation error: ${e.message}"
                                }
                            },
                            onCarClick = { carId ->
                                try {
                                    currentScreen = Screen.AddEditCar(carId)
                                } catch (e: Exception) {
                                    navigationError = "Navigation error: ${e.message}"
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Text("Error loading Car Management screen: ${e.message}", color = Color.Red)
                    }
                }

                // Profile screen
                AnimatedVisibility(
                    visible = currentScreen == Screen.Profile,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    try {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            performanceMonitor = appNavigator.performanceMonitor,
                            onLogout = onLogout
                        )
                    } catch (e: Exception) {
                        Text("Error loading Profile screen: ${e.message}", color = Color.Red)
                    }
                }

                // Photos screen placeholder
                AnimatedVisibility(
                    visible = currentScreen == Screen.Photos,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    // Placeholder - to be implemented
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Photos Screen - Coming Soon", color = Color.White)
                    }
                }

                // Display any navigation errors
                if (navigationError != null) {
                    Text(navigationError ?: "", color = Color.Red)
                }
            }
        }
    } catch (e: Exception) {
        // Fallback for critical errors
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Critical navigation error: ${e.message}", color = Color.Red)
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