// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/PaymentHistoryScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun PaymentHistoryScreen(
    viewModel: PaymentViewModel,
    performanceMonitor: PerformanceMonitor,
    onBack: () -> Unit,
    onViewDetails: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("payment_history_screen")
        viewModel.initialize()
    }

    // Get state from view model
    val pendingPayments by viewModel.pendingPayments.collectAsState()
    val completedPayments by viewModel.completedPayments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Local state for tab selection
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending", "Completed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History") },
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    backgroundColor = Color(0xFF1A3B66),
                    contentColor = Color.White
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = Color(0xFF2962FF),
                            unselectedContentColor = Color.White
                        )
                    }
                }

                // Content based on selected tab
                when (selectedTab) {
                    0 -> PaymentList(
                        payments = pendingPayments,
                        emptyMessage = "No pending payments",
                        onViewDetails = onViewDetails,
                        isLoading = isLoading
                    )
                    1 -> PaymentList(
                        payments = completedPayments,
                        emptyMessage = "No completed payments",
                        onViewDetails = onViewDetails,
                        isLoading = isLoading
                    )
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
        }
    }

    // Stop performance trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("payment_history_screen")
        }
    }
}

@Composable
fun PaymentList(
    payments: List<PaymentTransaction>,
    emptyMessage: String,
    onViewDetails: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF2962FF))
        } else if (payments.isEmpty()) {
            Text(
                text = emptyMessage,
                color = Color.Gray
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(payments) { payment ->
                    PaymentHistoryItem(
                        payment = payment,
                        onClick = { onViewDetails(payment.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryItem(
    payment: PaymentTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(payment.createdAt))

    Card(
        backgroundColor = Color(0xFF1A3B66),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (payment.status) {
                            PaymentStatus.PENDING -> Color.Yellow
                            PaymentStatus.PROCESSING -> Color(0xFF2962FF)
                            PaymentStatus.COMPLETED -> Color(0xFF4CAF50)
                            PaymentStatus.FAILED -> Color.Red
                            PaymentStatus.REFUNDED -> Color.Red
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Payment details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payment #${payment.id}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    color = Color.Gray,
                    style = MaterialTheme.typography.caption
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Status: ${payment.status.name}",
                    color = when (payment.status) {
                        PaymentStatus.PENDING -> Color.Yellow
                        PaymentStatus.PROCESSING -> Color(0xFF2962FF)
                        PaymentStatus.COMPLETED -> Color(0xFF4CAF50)
                        PaymentStatus.FAILED -> Color.Red
                        PaymentStatus.REFUNDED -> Color.Red
                    },
                    style = MaterialTheme.typography.caption
                )
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${payment.amount} ${payment.currency}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = payment.paymentMethod?.title ?: "N/A",
                    color = Color.Gray,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}