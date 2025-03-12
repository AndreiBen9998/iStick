// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/components/OptimizedList.kt
package istick.app.beta.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Campaign
import androidx.compose.animation.animateItemPlacement
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

// Define these variables in an appropriate scope
private val _isEndReached = MutableStateFlow(false)
val isEndReached: StateFlow<Boolean> = _isEndReached
private val isLoading = false // Define this properly based on your app's state
private fun onLoadMore() {} // Define this function based on your app's logic

@Composable
fun OptimizedOffersList(
    offers: List<Campaign>,
    onOfferClick: (Campaign) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    val visibleOffers by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                emptyList()
            } else {
                val firstVisibleIndex = visibleItemsInfo.first().index
                val lastVisibleIndex = visibleItemsInfo.last().index

                offers.subList(
                    kotlin.math.max(0, firstVisibleIndex - 5),
                    kotlin.math.min(offers.size, lastVisibleIndex + 5)
                )
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        items(
            items = offers,
            key = { it.id }
        ) { offer ->
            OfferItemCard(
                offer = offer,
                onClick = { onOfferClick(offer) },
                modifier = Modifier.animateItemPlacement() // Add animation for reordering
            )
        }

        // Add pagination support
        if (!isEndReached && !isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLoadMore,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2962FF)
                        )
                    ) {
                        Text("Load More")
                    }
                }
            }
        }
    }
}

// Define a separate card implementation specifically for the OptimizedList
@Composable
private fun OfferItemCard(
    offer: Campaign,
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
                    text = offer.title,
                    style = MaterialTheme.typography.h6,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${offer.payment.amount} ${offer.payment.currency}",
                    color = Color(0xFF2962FF),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = offer.description,
                style = MaterialTheme.typography.body2,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status
            Text(
                text = "Status: ${offer.status.name}",
                style = MaterialTheme.typography.caption,
                color = when(offer.status) {
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