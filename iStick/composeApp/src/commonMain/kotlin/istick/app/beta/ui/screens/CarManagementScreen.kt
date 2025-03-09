// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/CarManagementScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Car
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.CarManagementViewModel

@Composable
fun CarManagementScreen(
    viewModel: CarManagementViewModel,
    performanceMonitor: PerformanceMonitor,
    userId: String,
    onBackClick: () -> Unit,
    onAddCarClick: () -> Unit,
    onCarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("car_management_screen")
        viewModel.loadCars(userId)
    }

    // Get state from view model
    val cars by viewModel.cars.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cars") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFF0A1929),
                contentColor = Color.White
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCarClick,
                backgroundColor = Color(0xFF2962FF),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Car")
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F2030))
        ) {
            if (isLoading && cars.isEmpty()) {
                // Show loading indicator for initial load
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2962FF),
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (cars.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Simple text instead of icon
                    Text(
                        text = "🚗",  // Car emoji
                        fontSize = MaterialTheme.typography.h3.fontSize,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Cars Added Yet",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onAddCarClick,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2962FF)
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Add Car")
                    }
                }
            } else {
                // Car list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = cars,
                        key = { it.id }
                    ) { car ->
                        CarItem(
                            car = car,
                            onClick = { onCarClick(car.id) }
                        )
                    }

                    // Add some space at the bottom for the FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Stop performance trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("car_management_screen")
        }
    }
}

@Composable
private fun CarItem(
    car: Car,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        backgroundColor = Color(0xFF1A3B66),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Simple text instead of icon
                Text(
                    text = "🚗",  // Car emoji
                    fontSize = MaterialTheme.typography.h6.fontSize,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.h6,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${car.year} • ${car.color}",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }

                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // License plate
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝",  // License plate emoji
                    fontSize = MaterialTheme.typography.body2.fontSize
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "License: ${car.licensePlate}",
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mileage
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊",  // Chart emoji
                    fontSize = MaterialTheme.typography.body2.fontSize
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Mileage: ${car.currentMileage} km",
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Verification status
            if (car.verification.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (car.verification.last().isVerified) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "⏳",  // Hourglass emoji
                            fontSize = MaterialTheme.typography.body2.fontSize
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (car.verification.last().isVerified)
                            "Verified"
                        else
                            "Verification pending",
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}