// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/PaymentScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import istick.app.beta.payment.*
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.PaymentViewModel

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    performanceMonitor: PerformanceMonitor,
    campaignId: String,
    carOwnerId: String,
    onPaymentComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("payment_screen")
        viewModel.initialize()
    }

    // Get state from view model
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val amount by viewModel.paymentAmount.collectAsState()
    val currency by viewModel.paymentCurrency.collectAsState()
    val selectedMethodId by viewModel.selectedMethodId.collectAsState()
    val notes by viewModel.paymentNotes.collectAsState()

    // Local state for adding a new payment method
    var showAddMethodDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Payment") },
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
                // Loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF2962FF)
                )
            } else {
                // Main content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Payment details section
                    item {
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

                                // Amount Field
                                OutlinedTextField(
                                    value = if (amount > 0) amount.toString() else "",
                                    onValueChange = {
                                        try {
                                            val newAmount = it.toDoubleOrNull() ?: 0.0
                                            viewModel.updatePaymentAmount(newAmount)
                                        } catch (e: Exception) {
                                            // Ignore invalid input
                                        }
                                    },
                                    label = { Text("Amount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        textColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedBorderColor = Color(0xFF2962FF),
                                        unfocusedBorderColor = Color.Gray,
                                        focusedLabelColor = Color(0xFF2962FF),
                                        unfocusedLabelColor = Color.Gray,
                                        backgroundColor = Color(0xFF0F2030)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Currency Dropdown
                                var expandedCurrency by remember { mutableStateOf(false) }
                                Column {
                                    Text(
                                        text = "Currency",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F2030), RoundedCornerShape(4.dp))
                                            .border(
                                                width = 1.dp,
                                                color = Color.Gray,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { expandedCurrency = true }
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = currency,
                                            color = Color.White
                                        )

                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Currency",
                                            tint = Color.Gray,
                                            modifier = Modifier.align(Alignment.CenterEnd)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = expandedCurrency,
                                        onDismissRequest = { expandedCurrency = false },
                                        modifier = Modifier.background(Color(0xFF1A3B66))
                                    ) {
                                        listOf("RON", "EUR", "USD").forEach { currencyOption ->
                                            DropdownMenuItem(onClick = {
                                                viewModel.updatePaymentCurrency(currencyOption)
                                                expandedCurrency = false
                                            }) {
                                                Text(
                                                    text = currencyOption,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Notes Field
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { viewModel.updatePaymentNotes(it) },
                                    label = { Text("Payment Notes (Optional)") },
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        textColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedBorderColor = Color(0xFF2962FF),
                                        unfocusedBorderColor = Color.Gray,
                                        focusedLabelColor = Color(0xFF2962FF),
                                        unfocusedLabelColor = Color.Gray,
                                        backgroundColor = Color(0xFF0F2030)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                            }
                        }
                    }

                    // Payment method selection section
                    item {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.h6,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (paymentMethods.isEmpty()) {
                            Card(
                                backgroundColor = Color(0xFF1A3B66),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No payment methods found",
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showAddMethodDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFF2962FF)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Payment Method")
                                    }
                                }
                            }
                        }
                    }

                    // Payment methods list
                    items(paymentMethods) { method ->
                        val isSelected = selectedMethodId == method.id
                        PaymentMethodItem(
                            method = method,
                            isSelected = isSelected,
                            onSelect = { viewModel.updateSelectedMethodId(method.id) },
                            onDelete = { viewModel.removePaymentMethod(method.id) },
                            onSetDefault = { viewModel.setDefaultPaymentMethod(method.id) }
                        )
                    }

                    // Add payment method button
                    if (paymentMethods.isNotEmpty()) {
                        item {
                            Button(
                                onClick = { showAddMethodDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF2962FF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Another Payment Method")
                            }
                        }
                    }

                    // Process payment button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.processPayment(campaignId, carOwnerId)
                            },
                            enabled = selectedMethodId != null && amount > 0,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2962FF),
                                disabledBackgroundColor = Color.Gray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Process Payment",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Bottom spacing
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
                        TextButton(onClick = {
                            viewModel.clearSuccessMessage()
                            if (successMessage?.contains("processed") == true) {
                                onPaymentComplete()
                            }
                        }) {
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

            // Add payment method dialog
            if (showAddMethodDialog) {
                AddPaymentMethodDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddMethodDialog = false }
                )
            }
        }
    }

    // Stop performance trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("payment_screen")
        }
    }
}

@Composable
fun PaymentMethodItem(
    method: PaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = if (isSelected) Color(0xFF244674) else Color(0xFF1A3B66),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSelect),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF2962FF))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Payment method icon
            Icon(
                imageVector = when (method.type) {
                    PaymentMethodType.BANK_TRANSFER -> Icons.Default.AccountBalance
                    PaymentMethodType.CREDIT_CARD -> Icons.Default.CreditCard
                    PaymentMethodType.PAYPAL -> Icons.Default.AccountCircle  // Using a generic icon
                    PaymentMethodType.REVOLUT -> Icons.Default.AccountCircle  // Using a generic icon
                },
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2962FF) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Method details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (method.lastFour.isNotEmpty()) {
                    Text(
                        text = when(method.type) {
                            PaymentMethodType.BANK_TRANSFER -> "Account ending in ${method.lastFour}"
                            PaymentMethodType.CREDIT_CARD -> "Card ending in ${method.lastFour}"
                            else -> ""
                        },
                        color = Color.Gray,
                        style = MaterialTheme.typography.caption
                    )
                }

                if (method.isDefault) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Default",
                        color = Color(0xFF2962FF),
                        style = MaterialTheme.typography.caption
                    )
                }
            }

            // Action buttons
            if (!method.isDefault) {
                IconButton(onClick = onSetDefault) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Set as default",
                        tint = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AddPaymentMethodDialog(
    viewModel: PaymentViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get state from view model
    val methodType by viewModel.methodType.collectAsState()
    val methodTitle by viewModel.methodTitle.collectAsState()
    val accountNumber by viewModel.accountNumber.collectAsState()
    val makeDefault by viewModel.makeDefault.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Payment Method",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Method type selection
                Text(
                    text = "Payment Type",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PaymentMethodType.values().forEach { type ->
                        MethodTypeButton(
                            type = type,
                            isSelected = methodType == type,
                            onClick = { viewModel.updateMethodType(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Method title
                OutlinedTextField(
                    value = methodTitle,
                    onValueChange = { viewModel.updateMethodTitle(it) },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., My Bank Account") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF2962FF),
                        unfocusedLabelColor = Color.Gray,
                        backgroundColor = Color(0xFF1A3B66)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Account/card number
                if (methodType == PaymentMethodType.BANK_TRANSFER || methodType == PaymentMethodType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { viewModel.updateAccountNumber(it) },
                        label = {
                            Text(
                                when (methodType) {
                                    PaymentMethodType.BANK_TRANSFER -> "Account Number"
                                    PaymentMethodType.CREDIT_CARD -> "Card Number"
                                    else -> ""
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color(0xFF2962FF),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF2962FF),
                            unfocusedLabelColor = Color.Gray,
                            backgroundColor = Color(0xFF1A3B66)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Make default checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = makeDefault,
                        onCheckedChange = { viewModel.updateMakeDefault(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2962FF),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set as default payment method",
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addPaymentMethod()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2962FF)
                ),
                enabled = methodTitle.isNotEmpty() && (
                    methodType != PaymentMethodType.BANK_TRANSFER && methodType != PaymentMethodType.CREDIT_CARD ||
                    accountNumber.isNotEmpty()
                )
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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

@Composable
fun MethodTypeButton(
    type: PaymentMethodType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        PaymentMethodType.BANK_TRANSFER -> Icons.Default.AccountBalance
        PaymentMethodType.CREDIT_CARD -> Icons.Default.CreditCard
        PaymentMethodType.PAYPAL -> Icons.Default.AccountCircle  // Placeholder
        PaymentMethodType.REVOLUT -> Icons.Default.AccountCircle  // Placeholder
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) Color(0xFF2962FF) else Color(0xFF1A3B66),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) Color(0xFF2962FF) else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (type) {
                PaymentMethodType.BANK_TRANSFER -> "Bank"
                PaymentMethodType.CREDIT_CARD -> "Card"
                PaymentMethodType.PAYPAL -> "PayPal"
                PaymentMethodType.REVOLUT -> "Revolut"
            },
            style = MaterialTheme.typography.caption,
            color = if (isSelected) Color(0xFF2962FF) else Color.Gray
        )
    }
}