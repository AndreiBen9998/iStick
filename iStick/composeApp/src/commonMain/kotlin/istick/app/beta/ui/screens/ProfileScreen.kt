// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/ProfileScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Brand
import istick.app.beta.model.CarOwner
import istick.app.beta.model.User
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    performanceMonitor: PerformanceMonitor,
    onLogout: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},  // Add this parameter
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("profile_screen")
        viewModel.loadProfile()
    }

    // Get state from view model
    val user by viewModel.user.collectAsState()
    val cars by viewModel.cars.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030))
    ) {
        if (isLoading && user == null) {
            // Show loading indicator
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF2962FF)
            )
        } else if (error != null) {
            // Show error message
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = error ?: "Unknown error",
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.loadProfile() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2962FF)
                    )
                ) {
                    Text("Retry")
                }
            }
        } else {
            // Show profile content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Profile header
                ProfileHeader(
                    user = user,
                    onLogout = {
                        viewModel.signOut {
                            onLogout()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Profile details based on user type
                user?.let { currentUser ->
                    when (currentUser) {
                        is CarOwner -> CarOwnerDetails(carOwner = currentUser, cars = cars)
                        is Brand -> BrandDetails(
                            brand = currentUser,
                            onNavigateToAnalytics = onNavigateToAnalytics
                        )
                        else -> {
                            // Fallback for unknown user type
                            Text(
                                text = "Unknown user type",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("profile_screen")
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile picture
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A3B66)),
            contentAlignment = Alignment.Center
        ) {
            if (user?.profilePictureUrl != null) {
                // Here you would use Coil or another image loading library
                // For now we just show a placeholder
                Text(
                    text = user.name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.h3,
                    color = Color.White
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User name
        Text(
            text = user?.name ?: "User",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        // User email
        Text(
            text = user?.email ?: "",
            style = MaterialTheme.typography.body1,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        // User type badge
        user?.type?.let { userType ->
            Card(
                backgroundColor = Color(0xFF2962FF),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = userType.name,
                    style = MaterialTheme.typography.caption,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout button
        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color(0xFF2962FF)
            ),
            border = ButtonDefaults.outlinedBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2962FF))
            )
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Logout")
        }
    }
}

@Composable
private fun CarOwnerDetails(
    carOwner: CarOwner,
    cars: List<istick.app.beta.model.Car>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section title
        Text(
            text = "Car Owner Details",
            style = MaterialTheme.typography.h6,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // City
        DetailItem(
            icon = Icons.Default.LocationOn,
            label = "City",
            value = carOwner.city.ifEmpty { "Not specified" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Daily driving distance - replaced Speed with Info
        DetailItem(
            icon = Icons.Default.Info,  // Replaced Speed with Info
            label = "Daily Driving",
            value = "${carOwner.dailyDrivingDistance} km"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rating
        DetailItem(
            icon = Icons.Default.Star,
            label = "Rating",
            value = "${carOwner.rating} (${carOwner.reviewCount} reviews)"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Cars section
        Text(
            text = "Your Cars (${cars.size})",
            style = MaterialTheme.typography.h6,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // List of cars
        if (cars.isEmpty()) {
            Card(
                backgroundColor = Color(0xFF1A3B66),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No cars added yet",
                        color = Color.White
                    )
                }
            }
        } else {
            cars.forEach { car ->
                Card(
                    backgroundColor = Color(0xFF1A3B66),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,  // Replaced DirectionsCar with AccountCircle
                            contentDescription = null,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${car.make} ${car.model}",
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${car.year} • ${car.licensePlate}",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandDetails(
    brand: Brand,
    onNavigateToAnalytics: () -> Unit = {},  // Add this parameter
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section title
        Text(
            text = "Brand Details",
            style = MaterialTheme.typography.h6,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Company name - replaced Business with AccountCircle
        DetailItem(
            icon = Icons.Default.AccountCircle,  // Replaced Business with AccountCircle
            label = "Company",
            value = brand.companyDetails.companyName.ifEmpty { "Not specified" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Industry - replaced Category with Info
        DetailItem(
            icon = Icons.Default.Info,  // Replaced Category with Info
            label = "Industry",
            value = brand.companyDetails.industry.ifEmpty { "Not specified" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Website - replaced Language with LocationOn (since Link isn't available)
        DetailItem(
            icon = Icons.Default.LocationOn,  // Using LocationOn as a substitute for website/link
            label = "Website",
            value = brand.companyDetails.website.ifEmpty { "Not specified" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rating
        DetailItem(
            icon = Icons.Default.Star,
            label = "Rating",
            value = "${brand.rating} (${brand.reviewCount} reviews)"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Description",
            style = MaterialTheme.typography.subtitle1,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            backgroundColor = Color(0xFF1A3B66),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = brand.companyDetails.description.ifEmpty { "No description provided." },
                style = MaterialTheme.typography.body2,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Campaigns section (just a placeholder)
        Text(
            text = "Your Campaigns (${brand.campaigns.size})",
            style = MaterialTheme.typography.h6,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToAnalytics,  // Use the provided callback
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2962FF),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Info,  // Using Info icon as placeholder for Analytics
                contentDescription = "Analytics",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Campaign Analytics Dashboard")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Placeholder for campaigns
        if (brand.campaigns.isEmpty()) {
            Card(
                backgroundColor = Color(0xFF1A3B66),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No campaigns created yet",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = Color.Gray,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = Color.White
        )
    }
}