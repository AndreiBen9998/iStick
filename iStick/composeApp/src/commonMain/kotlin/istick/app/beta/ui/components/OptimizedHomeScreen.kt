// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/components/OptimizedHomeScreen.kt
package istick.app.beta.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Campaign
import istick.app.beta.model.CampaignStatus
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun ShimmerCampaignItem() {
    val shimmerColors = listOf(
        Color(0xFF1A3B66),
        Color(0xFF2A4B76),
        Color(0xFF1A3B66),
    )
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(10f, 10f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        backgroundColor = Color(0xFF1A3B66),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(24.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Second line placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status placeholder
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(14.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )
        }
    }
}

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

    // Using collectAsState for state collection
    val offers by viewModel.campaigns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
        if (isLoading && offers.isEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(8.dp)
            ) {
                items(5) { // Show 5 shimmer items
                    ShimmerCampaignItem()
                }
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
                        items = offers,
                        key = { it.id }
                    ) { campaign ->
                        OfferCard(
                            campaign = campaign,
                            onClick = { onCampaignClick(campaign) }
                        )
                    }

                    if (isLoading && offers.isNotEmpty()) {
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
                if (error != null) {
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
                                text = error ?: "",
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
    // State for the "like" functionality
    var isLiked by remember { mutableStateOf(false) }

    // Animation for card press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale animation when pressing
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Not using ripple here
                onClick = onClick
            ),
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like button with pulsating animation
                    PulsatingHeartButton(
                        isLiked = isLiked,
                        onClick = { isLiked = !isLiked }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${campaign.payment.amount} ${campaign.payment.currency}",
                        color = Color(0xFF2962FF),
                        fontWeight = FontWeight.Bold
                    )
                }
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

            // Status with animation
            AnimatedStatusBadge(status = campaign.status)
        }
    }
}

// Pulsating heart animation
@Composable
private fun PulsatingHeartButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Create transition for animation
    val transition = updateTransition(isLiked, label = "likeTransition")

    // Scale animation
    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                // When becoming liked - bounce effect
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            } else {
                // When becoming unliked - simple spring
                spring(stiffness = Spring.StiffnessLow)
            }
        },
        label = "scale"
    ) { liked -> if (liked) 1.3f else 1.0f }

    // Color animation
    val color by transition.animateColor(
        transitionSpec = { tween(durationMillis = 300) },
        label = "color"
    ) { liked -> if (liked) Color.Red else Color.Gray }

    // We'll use simple icons since we're limited in icon availability
    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Liked" else "Not liked",
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Animated status badge
@Composable
private fun AnimatedStatusBadge(status: CampaignStatus) {
    val color = when(status) {
        CampaignStatus.ACTIVE -> Color(0xFF4CAF50)
        CampaignStatus.DRAFT -> Color(0xFFFF9800)
        CampaignStatus.PAUSED -> Color(0xFFBDBDBD)
        CampaignStatus.COMPLETED -> Color(0xFF2196F3)
        CampaignStatus.CANCELLED -> Color(0xFFF44336)
        // Add a default case to ensure when is exhaustive
        else -> Color.Gray
    }

    // Animation for appearing
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .alpha(alpha)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.caption,
            color = color
        )
    }
}