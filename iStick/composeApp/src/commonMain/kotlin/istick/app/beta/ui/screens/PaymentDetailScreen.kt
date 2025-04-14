// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/PaymentDetailScreen.kt
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
import istick.app.beta.payment.*
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentDetailScreen(
    viewModel: PaymentViewModel,
    performanceMonitor: PerformanceMonitor,
    paymentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("payment_detail_screen")
        viewModel.loadPaymentDetails(paymentId)
    }

    // Get state from view model
    val payment by viewModel.selectedPayment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Local state for refund dialog
    var showRefundDialog by remember { mutableStateOf(false) }
    var refundReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF2962FF)
                )
            } else if (payment == null) {
                Text(
                    text = "Payment not found",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                payment?.let { currentPayment ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Payment status card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            backgroundColor = when (currentPayment.status) {
                                PaymentStatus.PENDING -> Color(0xFFFFF9C4)
                                PaymentStatus.PROCESSING -> Color(0xFFBBDEFB)
                                PaymentStatus.COMPLETED -> Color(0xFFC8E6C9)
                                PaymentStatus.FAILED -> Color(0xFFFFCDD2)
                                PaymentStatus.REFUNDED -> Color(0xFFFFCDD2)
                            },
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (currentPayment.status) {
                                        PaymentStatus.PENDING -> Icons.Default.HourglassEmpty
                                        PaymentStatus.PROCESSING -> Icons.Default.Refresh
                                        PaymentStatus.COMPLETED -> Icons.Default.Done
                                        PaymentStatus.FAILED -> Icons.Default.Error
                                        PaymentStatus.REFUNDED -> Icons.Default.Create
                                    },
                                    contentDescription = null,
                                    tint = when (currentPayment.status) {
                                        PaymentStatus.PENDING -> Color(0xFFF57F17)
                                        PaymentStatus.PROCESSING -> Color(0xFF1565C0)
                                        PaymentStatus.COMPLETED -> Color(0xFF2E7D32)
                                        PaymentStatus.FAILED -> Color(0xFFB71C1C)
                                        PaymentStatus.REFUNDED -> Color(0xFFB71C1C)
                                    }
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = currentPayment.status.name,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentPayment.status) {
                                            PaymentStatus.PENDING -> Color(0xFFF57F17)
                                            PaymentStatus.PROCESSING -> Color(0xFF1565C0)
                                            PaymentStatus.COMPLETED -> Color(0xFF2E7D32)
                                            PaymentStatus.FAILED -> Color(0xFFB71C1C)
                                            PaymentStatus.REFUNDED -> Color(0xFFB71C1C)
                                        }
                                    )

                                    Text(
                                        text = when (currentPayment.status) {
                                            PaymentStatus.PENDING -> "Payment is being processed"
                                            PaymentStatus.PROCESSING -> "Payment is being processed"
                                            PaymentStatus.COMPLETED -> "Payment was successful"
                                            PaymentStatus.FAILED -> "Payment failed"
                                            PaymentStatus.REFUNDED -> "Payment was refunded"
                                        },
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        // Payment details card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            backgroundColor = Color(0xFF1A3B66),
                            elevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Payment Details",
                                    style = MaterialTheme.typography.h6,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Payment ID
                                DetailRow(label = "Payment ID", value = currentPayment.id)

                                // Amount
                                DetailRow(
                                    label = "Amount",
                                    value = "${currentPayment.amount} ${currentPayment.currency}"
                                )

                                // Date
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                val formattedDate = dateFormat.format(Date(currentPayment.createdAt))
                                DetailRow(label = "Date", value = formattedDate)

                                // Payment Method
                                DetailRow(
                                    label = "Payment Method",
                                    value = currentPayment.paymentMethod?.title ?: "N/A"
                                )

                                // Campaign ID
                                DetailRow(label = "Campaign ID", value = currentPayment.campaignId)

                                // Car Owner ID
                                DetailRow(label = "Car Owner ID", value = currentPayment.carOwnerId)

                                // Brand ID
                                DetailRow(label = "Brand ID", value = currentPayment.brandId)

                                // Notes
                                if (currentPayment.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Notes",
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentPayment.notes,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Actions
                        if (currentPayment.status == PaymentStatus.COMPLETED) {
                            Button(
                                onClick = { showRefundDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFB71C1C),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Create,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Request Refund")
                            }
                        }
                    }

                    // Refund dialog
                    if (showRefundDialog) {
                        AlertDialog(
                            onDismissRequest = { showRefundDialog = false },
                            title = {
                                Text(
                                    text = "Request Refund",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Please provide a reason for the refund request:",
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = refundReason,
                                        onValueChange = { refundReason = it },
                                        label = { Text("Reason") },
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            textColor = Color.White,
                                            cursorColor = Color.White,
                                            focusedBorderColor = Color(0xFF2962FF),
                                            unfocusedBorderColor = Color.Gray,
                                            backgroundColor = Color(0xFF1A3B66)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (refundReason.isNotEmpty()) {
                                            viewModel.requestRefund(currentPayment.id, refundReason)
                                            showRefundDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFB71C1C)
                                    ),
                                    enabled = refundReason.isNotEmpty()
                                ) {
                                    Text("Submit Request")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showRefundDialog = false },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color.Gray
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            },
                            backgroundColor = Color(0xFF1A3B66),
                            contentColor = Color.White
                        )
                    }
                }
            }

            // Error message
            if (error != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = Color.White)
                        }
                    },
                    backgroundColor = Color(0xFFB71C1C),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(error ?: "")
                }
            }

            // Success message
            if (successMessage != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    backgroundColor = Color(0xFF388E3C),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(successMessage ?: "")
                }
            }
        }
    }

    // Stop performance trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("payment_detail_screen")
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            color = Color.White
        )
    }
}