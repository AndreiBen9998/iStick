// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/CampaignDetailScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import istick.app.beta.model.Campaign
import istick.app.beta.model.Car
import istick.app.beta.model.CampaignApplication
import istick.app.beta.model.StickerPosition
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.CampaignDetailViewModel

@Composable
fun CampaignDetailScreen(
    viewModel: CampaignDetailViewModel,
    campaignId: String,
    userId: String,
    performanceMonitor: PerformanceMonitor,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("campaign_detail_screen")
        viewModel.loadCampaignDetails(campaignId)
        viewModel.loadUserCars(userId)
    }

    // State
    val campaign by remember { viewModel.campaign }
    val userCars by remember { viewModel.userCars }
    val selectedCarId by remember { viewModel.selectedCarId }
    val application by remember { viewModel.application }
    val isLoading by remember { viewModel.isLoading }
    val error by remember { viewModel.error }

    // Scroll state
    val scrollState = rememberScrollState()

    // Dialog state
    var showApplyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Details") },
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
            if (isLoading && campaign == null) {
                // Show loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF2962FF)
                )
            } else if (campaign == null) {
                // Show error or empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = error ?: "Campaign not found",
                        color = Color.White
                    )
                }
            } else {
                // Campaign details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Campaign title and status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = campaign?.title ?: "",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        campaign?.status?.let { status ->
                            StatusBadge(status)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        backgroundColor = Color(0xFF1A3B66),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = campaign?.description ?: "",
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Payment details
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    campaign?.payment?.let { payment ->
                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF2962FF),
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "${payment.amount} ${payment.currency}",
                                        style = MaterialTheme.typography.h6,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Payment Frequency: ${payment.paymentFrequency.name}",
                                    color = Color.Gray
                                )

                                Text(
                                    text = "Payment Method: ${payment.paymentMethod.name}",
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sticker details
                    Text(
                        text = "Sticker Details",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    campaign?.stickerDetails?.let { stickerDetails ->
                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Photo,
                                        contentDescription = null,
                                        tint = Color(0xFF2962FF),
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Dimensions: ${stickerDetails.width} x ${stickerDetails.height} cm",
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Delivery Method: ${stickerDetails.deliveryMethod.name}",
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Allowed Positions:",
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                stickerDetails.positions.forEach { position ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = formatStickerPosition(position),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Campaign requirements
                    Text(
                        text = "Requirements",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    campaign?.requirements?.let { requirements ->
                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (requirements.minDailyDistance > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = Color(0xFF2962FF),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "Minimum daily driving: ${requirements.minDailyDistance} km",
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (requirements.cities.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.LocationCity,
                                            contentDescription = null,
                                            tint = Color(0xFF2962FF),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = "Available in cities:",
                                                color = Color.White
                                            )

                                            requirements.cities.forEach { city ->
                                                Text(
                                                    text = "• $city",
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (requirements.carMakes.isNotEmpty() || requirements.carModels.isNotEmpty() ||
                                    requirements.carYearMin != null || requirements.carYearMax != null) {
                                    Row(
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = Color(0xFF2962FF),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = "Car requirements:",
                                                color = Color.White
                                            )

                                            if (requirements.carMakes.isNotEmpty()) {
                                                Text(
                                                    text = "• Makes: ${requirements.carMakes.joinToString(", ")}",
                                                    color = Color.Gray
                                                )
                                            }

                                            if (requirements.carModels.isNotEmpty()) {
                                                Text(
                                                    text = "• Models: ${requirements.carModels.joinToString(", ")}",
                                                    color = Color.Gray
                                                )
                                            }

                                            if (requirements.carYearMin != null || requirements.carYearMax != null) {
                                                val yearText = when {
                                                    requirements.carYearMin != null && requirements.carYearMax != null ->
                                                        "between ${requirements.carYearMin} and ${requirements.carYearMax}"
                                                    requirements.carYearMin != null ->
                                                        "from ${requirements.carYearMin} and newer"
                                                    requirements.carYearMax != null ->
                                                        "up to ${requirements.carYearMax}"
                                                    else -> ""
                                                }

                                                Text(
                                                    text = "• Year: $yearText",
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Application status or apply button
                    if (application != null) {
                        // Application exists, show status
                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Your Application",
                                    style = MaterialTheme.typography.subtitle1,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                ApplicationStatusBadge(application?.status)

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = when (application?.status) {
                                        istick.app.beta.model.ApplicationStatus.PENDING -> "Your application is being reviewed."
                                        istick.app.beta.model.ApplicationStatus.APPROVED -> "Congratulations! Your application has been approved."
                                        istick.app.beta.model.ApplicationStatus.REJECTED -> "Unfortunately, your application has been rejected."
                                        istick.app.beta.model.ApplicationStatus.COMPLETED -> "This campaign is completed."
                                        null -> ""
                                    },
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    } else {
                        // No application, show apply button
                        Button(
                            onClick = { showApplyDialog = true },
                            enabled = userCars.isNotEmpty() && campaign?.status == istick.app.beta.model.CampaignStatus.ACTIVE,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2962FF),
                                contentColor = Color.White,
                                disabledBackgroundColor = Color.Gray,
                                disabledContentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("Apply for this Campaign")
                        }

                        if (userCars.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You need to add a car before applying",
                                color = Color.Gray,
                                style = MaterialTheme.typography.caption
                            )
                        } else if (campaign?.status != istick.app.beta.model.CampaignStatus.ACTIVE) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "This campaign is not accepting applications",
                                color = Color.Gray,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // Show error if any
            if (error != null && campaign != null) {
                Snackbar(
                    backgroundColor = Color(0xFFD32F2F),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error ?: "")
                }
            }
        }
    }

    // Application dialog
    if (showApplyDialog) {
        ApplyDialog(
            cars = userCars,
            selectedCarId = selectedCarId,
            onSelectCar = { viewModel.selectCar(it) },
            onApply = {
                viewModel.applyToCampaign()
                showApplyDialog = false
            },
            onDismiss = { showApplyDialog = false }
        )
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("campaign_detail_screen")
        }
    }
}

@Composable
private fun StatusBadge(status: istick.app.beta.model.CampaignStatus) {
    val (backgroundColor, textColor) = when (status) {
        istick.app.beta.model.CampaignStatus.ACTIVE -> Color(0xFF4CAF50) to Color.White
        istick.app.beta.model.CampaignStatus.DRAFT -> Color(0xFFFF9800) to Color.Black
        istick.app.beta.model.CampaignStatus.PAUSED -> Color(0xFFBDBDBD) to Color.Black
        istick.app.beta.model.CampaignStatus.COMPLETED -> Color(0xFF2196F3) to Color.White
        istick.app.beta.model.CampaignStatus.CANCELLED -> Color(0xFFF44336) to Color.White
    }

    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.name,
            color = textColor,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ApplicationStatusBadge(status: istick.app.beta.model.ApplicationStatus?) {
    if (status == null) return

    val (backgroundColor, textColor) = when (status) {
        istick.app.beta.model.ApplicationStatus.PENDING -> Color(0xFFFF9800) to Color.Black
        istick.app.beta.model.ApplicationStatus.APPROVED -> Color(0xFF4CAF50) to Color.White
        istick.app.beta.model.ApplicationStatus.REJECTED -> Color(0xFFF44336) to Color.White
        istick.app.beta.model.ApplicationStatus.COMPLETED -> Color(0xFF2196F3) to Color.White
    }

    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.name,
            color = textColor,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ApplyDialog(
    cars: List<Car>,
    selectedCarId: String?,
    onSelectCar: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Apply for Campaign",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Select the car you want to use for this campaign:")

                Spacer(modifier = Modifier.height(16.dp))

                cars.forEach { car ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onSelectCar(car.id) }
                    ) {
                        RadioButton(
                            selected = car.id == selectedCarId,
                            onClick = { onSelectCar(car.id) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF2962FF)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "${car.make} ${car.model}",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${car.year} • ${car.licensePlate}",
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                enabled = selectedCarId != null,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2962FF)
                )
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        },
        backgroundColor = Color(0xFF1A3B66),
        contentColor = Color.White
    )
}

private fun formatStickerPosition(position: StickerPosition): String {
    return when (position) {
        StickerPosition.DOOR_LEFT -> "Left Door"
        StickerPosition.DOOR_RIGHT -> "Right Door"
        StickerPosition.HOOD -> "Hood"
        StickerPosition.TRUNK -> "Trunk"
        StickerPosition.REAR_WINDOW -> "Rear Window"
        StickerPosition.SIDE_PANEL -> "Side Panel"
    }
}