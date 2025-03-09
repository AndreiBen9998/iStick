// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/CampaignListScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Campaign
import istick.app.beta.ui.components.OptimizedHomeScreen as HomeScreen
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.HomeViewModel

@Composable
fun CampaignListScreen(
    viewModel: HomeViewModel,
    performanceMonitor: PerformanceMonitor,
    onCampaignClick: (Campaign) -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("campaign_list_screen")
    }

    // Use the HomeScreen component that already has all the necessary UI elements
    HomeScreen(
        viewModel = viewModel,
        performanceMonitor = performanceMonitor,
        onCampaignClick = onCampaignClick,
        modifier = modifier
    )

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("campaign_list_screen")
        }
    }
}