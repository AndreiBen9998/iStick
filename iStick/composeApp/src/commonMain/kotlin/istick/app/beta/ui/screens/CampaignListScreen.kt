// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/CampaignListScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Search
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

    // Get state from view model
    val campaigns = remember { mutableStateOf(emptyList<Campaign>()) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    // Collect state values manually
    LaunchedEffect(viewModel) {
        viewModel.campaigns.collect { campaigns.value = it }
    }

    LaunchedEffect(viewModel) {
        viewModel.isLoading.collect { isLoading.value = it }
    }

    LaunchedEffect(viewModel) {
        viewModel.error.collect { error.value = it }
    }

    // Local state
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    // Filter campaigns based on search query
    val filteredCampaigns = remember(campaigns.value, searchQuery) {
        if (searchQuery.isEmpty()) {
            campaigns.value
        } else {
            campaigns.value.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030))
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                cursorColor = Color.White,
                leadingIconColor = Color.White,
                trailingIconColor = Color.White,
                backgroundColor = Color(0xFF1A3B66),
                focusedBorderColor = Color(0xFF2962FF),
                unfocusedBorderColor = Color.Gray
            ),
            placeholder = { Text("Search campaigns", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading.value && campaigns.value.isEmpty()) {
                // Show loading indicator if initial load
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2962FF),
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (filteredCampaigns.isEmpty()) {
                // Show empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (searchQuery.isEmpty()) "No campaigns available yet"
                        else "No results found for \"$searchQuery\"",
                        color = Color.White
                    )
                }
            } else {
                // Campaign list
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredCampaigns,
                        key = { it.id }
                    ) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            onClick = { onCampaignClick(campaign) }
                        )
                    }
                }
            }

            // Show error message if any
            if (error.value != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Card(
                        backgroundColor = Color(0xFF800000),
                        elevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
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

                            IconButton(
                                onClick = { viewModel.refresh() }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Refresh button
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
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }

    // Stop performance trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("campaign_list_screen")
        }
    }
}

@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
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