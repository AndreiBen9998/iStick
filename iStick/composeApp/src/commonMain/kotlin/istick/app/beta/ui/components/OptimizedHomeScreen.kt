// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/components/OptimizedHomeScreen.kt
package istick.app.beta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Campaign
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    performanceMonitor: PerformanceMonitor,
    onCampaignClick: (Campaign) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("home_screen_render")
    }

    // Using collectAsState from androidx.compose.runtime instead
    val offers = remember { mutableStateOf(emptyList<Campaign>()) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    // Collect state values manually
    LaunchedEffect(viewModel) {
        viewModel.campaigns.collect { offers.value = it }
    }

    LaunchedEffect(viewModel) {
        viewModel.isLoading.collect { isLoading.value = it }
    }

    LaunchedEffect(viewModel) {
        viewModel.error.collect { error.value = it }
    }

    // Layout principal
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Available Campaigns",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Display loader during initial loading
        if (isLoading.value && offers.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF2962FF),
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            // LazyColumn with offers
            val lazyListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(
                        items = offers.value,
                        key = { it.id }
                    ) { campaign ->
                        OfferCard(
                            campaign = campaign,
                            onClick = { onCampaignClick(campaign) }
                        )
                    }

                    if (isLoading.value && offers.value.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2962FF),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Floating action button for refresh
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.refresh()
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    backgroundColor = Color(0xFF2962FF)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }

                // Error display
                if (error.value != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        backgroundColor = Color(0xFF800000),
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = error.value ?: "",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Stop performance trace
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("home_screen_render")
        }
    }
}

@Composable
private fun OfferCard(
    campaign: Campaign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick),
        backgroundColor = Color(0xFF1A3B66),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title and Payment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campaign.title,
                    style = MaterialTheme.typography.h6,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${campaign.payment.amount} ${campaign.payment.currency}",
                    color = Color(0xFF2962FF),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = campaign.description,
                style = MaterialTheme.typography.body2,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status
            Text(
                text = "Status: ${campaign.status.name}",
                style = MaterialTheme.typography.caption,
                color = when(campaign.status) {
                    istick.app.beta.model.CampaignStatus.ACTIVE -> Color(0xFF4CAF50)
                    istick.app.beta.model.CampaignStatus.DRAFT -> Color(0xFFFF9800)
                    istick.app.beta.model.CampaignStatus.PAUSED -> Color(0xFFBDBDBD)
                    istick.app.beta.model.CampaignStatus.COMPLETED -> Color(0xFF2196F3)
                    istick.app.beta.model.CampaignStatus.CANCELLED -> Color(0xFFF44336)
                }
            )
        }
    }
}