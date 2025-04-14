// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/CampaignAnalyticsScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.model.Brand
import istick.app.beta.model.UserType
import istick.app.beta.viewmodel.CampaignAnalyticsViewModel

/**
 * Screen for displaying campaign analytics to brands
 */
@Composable
fun CampaignAnalyticsScreen(
    viewModel: CampaignAnalyticsViewModel,
    performanceMonitor: PerformanceMonitor,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("campaign_analytics_screen")
        viewModel.loadAnalytics()
    }

    // Get state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val user by viewModel.user.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFF0A1929),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F2030))
        ) {
            // Check if user is a Brand
            if (user?.type != UserType.BRAND) {
                // Show message that this is only for brands
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Analytics Dashboard is available only for Brand accounts",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isLoading) {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2962FF))
                }
            } else if (error != null) {
                // Show error message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading analytics",
                        color = Color.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "Unknown error",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadAnalytics() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2962FF)
                        )
                    ) {
                        Text("Retry")
                    }
                }
            } else if (campaigns.isEmpty()) {
                // Show no campaigns message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No active campaigns found",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Create a campaign to start seeing analytics",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { /* Navigate to campaign creation */ },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2962FF)
                        )
                    ) {
                        Text("Create Campaign")
                    }
                }
            } else {
                // Show React component using WebView or similar approach
                CampaignAnalyticsDashboardWrapper()
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("campaign_analytics_screen")
        }
    }
}

/**
 * Wrapper to integrate React component with Compose
 * This is a placeholder that would use a WebView or similar approach to load the React component
 */
@Composable
fun CampaignAnalyticsDashboardWrapper() {
    // In a real implementation, this would use a WebView or similar approach to load the React component
    // For now, we'll just show a message that the dashboard would be here
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Campaign Analytics Dashboard",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This is where the React-based Analytics Dashboard would be displayed, showing metrics like:",
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // List of metrics
        Column(
            modifier = Modifier
                .background(Color(0xFF1A3B66), shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Text("• Number of applications received", color = Color.White)
            Text("• Number of cars currently displaying ads", color = Color.White)
            Text("• Geographical distribution of cars", color = Color.White)
            Text("• Total mileage and visibility metrics", color = Color.White)
            Text("• Cost efficiency analysis", color = Color.White)
            Text("• Campaign comparison", color = Color.White)
        }
    }
}