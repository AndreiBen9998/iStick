// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/CampaignCreationScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.model.*
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.CampaignCreationViewModel
import istick.app.beta.camera.rememberCameraLauncher
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke

@Composable
fun CampaignCreationScreen(
    viewModel: CampaignCreationViewModel,
    performanceMonitor: PerformanceMonitor,
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("campaign_creation_screen")
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Collect states from ViewModel
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val stickerWidth by viewModel.stickerWidth.collectAsState()
    val stickerHeight by viewModel.stickerHeight.collectAsState()
    val stickerPositions by viewModel.stickerPositions.collectAsState()
    val deliveryMethod by viewModel.deliveryMethod.collectAsState()
    val minDailyDistance by viewModel.minDailyDistance.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val carMakes by viewModel.carMakes.collectAsState()
    val carModels by viewModel.carModels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val stickerImageUrl by viewModel.stickerImageUrl.collectAsState()

    // Camera launcher for sticker upload
    val cameraLauncher = rememberCameraLauncher { imageBytes ->
        viewModel.uploadStickerImage(imageBytes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Campaign") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
                .background(Color(0xFF0F2030))
                .padding(padding)
        ) {
            // Progress indicator at the top
            LinearProgressIndicator(
                progress = when (currentStep) {
                    CampaignCreationViewModel.Step.BASIC_INFO -> 0.25f
                    CampaignCreationViewModel.Step.STICKER_DETAILS -> 0.5f
                    CampaignCreationViewModel.Step.TARGETING -> 0.75f
                    CampaignCreationViewModel.Step.REVIEW -> 1f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF2962FF),
                backgroundColor = Color(0xFF1A3B66)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Step title
                Text(
                    text = when (currentStep) {
                        CampaignCreationViewModel.Step.BASIC_INFO -> "Basic Information"
                        CampaignCreationViewModel.Step.STICKER_DETAILS -> "Sticker Details"
                        CampaignCreationViewModel.Step.TARGETING -> "Targeting Options"
                        CampaignCreationViewModel.Step.REVIEW -> "Review & Create"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Basic Info Step
                AnimatedVisibility(visible = currentStep == CampaignCreationViewModel.Step.BASIC_INFO) {
                    Column {
                        // Campaign Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { viewModel.updateTitle(it) },
                            label = { Text("Campaign Title") },
                            placeholder = { Text("e.g., Summer Promotion 2025") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color.White,
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF2962FF),
                                unfocusedBorderColor = Color.Gray,
                                backgroundColor = Color(0xFF1A3B66)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campaign Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text("Description") },
                            placeholder = { Text("Describe your campaign") },
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payment Amount
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = amount.toString(),
                                onValueChange = {
                                    try {
                                        val value = it.toDoubleOrNull() ?: 0.0
                                        viewModel.updateAmount(value)
                                    } catch (e: Exception) {
                                        // Ignore invalid input
                                    }
                                },
                                label = { Text("Monthly Payment") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color(0xFF2962FF),
                                    unfocusedBorderColor = Color.Gray,
                                    backgroundColor = Color(0xFF1A3B66)
                                ),
                                modifier = Modifier.weight(0.6f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Currency Dropdown
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(56.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(Color(0xFF1A3B66), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                var expanded by remember { mutableStateOf(false) }

                                Text(
                                    text = currency,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = true }
                                )

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color(0xFF1A3B66))
                                ) {
                                    listOf("RON", "EUR", "USD").forEach { currencyOption ->
                                        DropdownMenuItem(onClick = {
                                            viewModel.updateCurrency(currencyOption)
                                            expanded = false
                                        }) {
                                            Text(
                                                text = currencyOption,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticker Details Step
                AnimatedVisibility(visible = currentStep == CampaignCreationViewModel.Step.STICKER_DETAILS) {
                    Column {
                        // Sticker Image Upload
                        Text(
                            text = "Sticker Design",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Image upload area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF2962FF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A3B66))
                                .clickable { cameraLauncher() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (stickerImageUrl.isNotEmpty()) {
                                // Here we would use an actual image loading library
                                // For now just showing a placeholder
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF2962FF).copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Image Uploaded",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Upload Sticker Design",
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sticker Dimensions
                        Text(
                            text = "Sticker Dimensions (cm)",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Width
                            OutlinedTextField(
                                value = stickerWidth.toString(),
                                onValueChange = {
                                    try {
                                        val value = it.toIntOrNull() ?: 0
                                        viewModel.updateStickerWidth(value)
                                    } catch (e: Exception) {
                                        // Ignore invalid input
                                    }
                                },
                                label = { Text("Width") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color(0xFF2962FF),
                                    unfocusedBorderColor = Color.Gray,
                                    backgroundColor = Color(0xFF1A3B66)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Height
                            OutlinedTextField(
                                value = stickerHeight.toString(),
                                onValueChange = {
                                    try {
                                        val value = it.toIntOrNull() ?: 0
                                        viewModel.updateStickerHeight(value)
                                    } catch (e: Exception) {
                                        // Ignore invalid input
                                    }
                                },
                                label = { Text("Height") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color(0xFF2962FF),
                                    unfocusedBorderColor = Color.Gray,
                                    backgroundColor = Color(0xFF1A3B66)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sticker Positions
                        Text(
                            text = "Allowed Positions",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        StickerPositionsSelector(
                            selectedPositions = stickerPositions,
                            onPositionToggle = { position ->
                                viewModel.toggleStickerPosition(position)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Delivery Method
                        Text(
                            text = "Delivery Method",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DeliveryMethodOption(
                                method = DeliveryMethod.CENTER,
                                selected = deliveryMethod == DeliveryMethod.CENTER,
                                onSelect = { viewModel.updateDeliveryMethod(DeliveryMethod.CENTER) },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            DeliveryMethodOption(
                                method = DeliveryMethod.HOME_KIT,
                                selected = deliveryMethod == DeliveryMethod.HOME_KIT,
                                onSelect = { viewModel.updateDeliveryMethod(DeliveryMethod.HOME_KIT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Targeting Step
                AnimatedVisibility(visible = currentStep == CampaignCreationViewModel.Step.TARGETING) {
                    Column {
                        // Minimum Daily Distance
                        Text(
                            text = "Minimum Daily Driving Distance (km)",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "$minDailyDistance km",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Slider(
                            value = minDailyDistance.toFloat(),
                            onValueChange = { viewModel.updateMinDailyDistance(it.toInt()) },
                            valueRange = 0f..200f,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2962FF),
                                activeTrackColor = Color(0xFF2962FF),
                                inactiveTrackColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Target Cities
                        Text(
                            text = "Target Cities",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        var newCity by remember { mutableStateOf("") }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCity,
                                onValueChange = { newCity = it },
                                label = { Text("Add City") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newCity.isNotEmpty()) {
                                            viewModel.addCity(newCity)
                                            newCity = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color(0xFF2962FF),
                                    unfocusedBorderColor = Color.Gray,
                                    backgroundColor = Color(0xFF1A3B66)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (newCity.isNotEmpty()) {
                                        viewModel.addCity(newCity)
                                        newCity = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xFF2962FF), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add City",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // City chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cities.forEach { city ->
                                Chip(
                                    text = city,
                                    onRemove = { viewModel.removeCity(city) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Car Makes/Models
                        Text(
                            text = "Preferred Car Makes (Optional)",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        var newCarMake by remember { mutableStateOf("") }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCarMake,
                                onValueChange = { newCarMake = it },
                                label = { Text("Car Make") },
                                placeholder = { Text("e.g., Toyota, BMW") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newCarMake.isNotEmpty()) {
                                            viewModel.addCarMake(newCarMake)
                                            newCarMake = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color(0xFF2962FF),
                                    unfocusedBorderColor = Color.Gray,
                                    backgroundColor = Color(0xFF1A3B66)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (newCarMake.isNotEmpty()) {
                                        viewModel.addCarMake(newCarMake)
                                        newCarMake = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xFF2962FF), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Car Make",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Car make chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            carMakes.forEach { make ->
                                Chip(
                                    text = make,
                                    onRemove = { viewModel.removeCarMake(make) }
                                )
                            }
                        }
                    }
                }

                // Review Step
                AnimatedVisibility(visible = currentStep == CampaignCreationViewModel.Step.REVIEW) {
                    Column {
                        Text(
                            text = "Campaign Summary",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Title & Description
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.h6,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White.copy(alpha = 0.7f)
                                )

                                Divider(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                // Payment
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Monthly Payment:",
                                        color = Color.White
                                    )

                                    Text(
                                        text = "$amount $currency",
                                        color = Color(0xFF2962FF),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sticker dimensions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Sticker Size:",
                                        color = Color.White
                                    )

                                    Text(
                                        text = "${stickerWidth}cm × ${stickerHeight}cm",
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Positions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Allowed Positions:",
                                        color = Color.White
                                    )

                                    Text(
                                        text = stickerPositions.joinToString(", ") {
                                            it.name.replace("_", " ").lowercase().capitalize()
                                        },
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Delivery
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Delivery:",
                                        color = Color.White
                                    )

                                    Text(
                                        text = when(deliveryMethod) {
                                            DeliveryMethod.CENTER -> "Authorized Center"
                                            DeliveryMethod.HOME_KIT -> "Home Kit Delivery"
                                        },
                                        color = Color.White
                                    )
                                }

                                Divider(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                // Targeting
                                Text(
                                    text = "Targeting Requirements",
                                    style = MaterialTheme.typography.subtitle2,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Min. Daily Distance:",
                                        color = Color.White
                                    )

                                    Text(
                                        text = "$minDailyDistance km",
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (cities.isNotEmpty()) {
                                    Text(
                                        text = "Target Cities: ${cities.joinToString(", ")}",
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (carMakes.isNotEmpty()) {
                                    Text(
                                        text = "Preferred Car Makes: ${carMakes.joinToString(", ")}",
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirmation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var termsAccepted by remember { mutableStateOf(false) }

                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2962FF),
                                    uncheckedColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "I confirm that the information provided is correct and agree to the Terms of Service.",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.createCampaign { campaignId ->
                                        onSuccess(campaignId)
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2962FF),
                                contentColor = Color.White,
                                disabledBackgroundColor = Color(0xFF2962FF).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "Create Campaign",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Step navigation
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back button (hidden on first step)
                    AnimatedVisibility(visible = currentStep != CampaignCreationViewModel.Step.BASIC_INFO) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.Gray)
                            ),
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text("Back")
                        }
                    }

                    // Spacer to push buttons to the edges (when back button is visible)
                    if (currentStep != CampaignCreationViewModel.Step.BASIC_INFO) {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Next/Continue button (hidden on last step)
                    AnimatedVisibility(visible = currentStep != CampaignCreationViewModel.Step.REVIEW) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2962FF),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text("Next")
                        }
                    }
                }
            }

            // Error Snackbar
            if (error != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = Color.White)
                        }
                    },
                    backgroundColor = Color(0xFFB71C1C)
                ) {
                    Text(text = error ?: "")
                }
            }
        }
    }

    // Stop performance trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("campaign_creation_screen")
        }
    }
}

@Composable
private fun StickerPositionsSelector(
    selectedPositions: List<StickerPosition>,
    onPositionToggle: (StickerPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val positions = listOf(
        StickerPosition.DOOR_LEFT to "Left Door",
        StickerPosition.DOOR_RIGHT to "Right Door",
        StickerPosition.HOOD to "Hood",
        StickerPosition.TRUNK to "Trunk",
        StickerPosition.REAR_WINDOW to "Rear Window",
        StickerPosition.SIDE_PANEL to "Side Panel"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        positions.chunked(2).forEach { rowPositions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                rowPositions.forEach { (position, label) ->
                    val isSelected = position in selectedPositions

                    Card(
                        backgroundColor = if (isSelected) Color(0xFF2962FF) else Color(0xFF1A3B66),
                        contentColor = Color.White,
                        border = if (isSelected) null else BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clickable { onPositionToggle(position) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Text(
                                text = label,
                                color = Color.White
                            )
                        }
                    }

                    if (rowPositions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryMethodOption(
    method: DeliveryMethod,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = if (selected) Color(0xFF2962FF) else Color(0xFF1A3B66),
        contentColor = Color.White,
        border = if (selected) null else BorderStroke(1.dp, Color.Gray),
        modifier = modifier
            .clickable(onClick = onSelect)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                when (method) {
                    DeliveryMethod.CENTER -> Icons.Default.AccountCircle
                    DeliveryMethod.HOME_KIT -> Icons.Default.Home
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (method) {
                    DeliveryMethod.CENTER -> "Authorized Center"
                    DeliveryMethod.HOME_KIT -> "Home Kit Delivery"
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (method) {
                    DeliveryMethod.CENTER -> "Professional installation at certified locations"
                    DeliveryMethod.HOME_KIT -> "Kit sent to car owner with application instructions"
                },
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun Chip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF2962FF),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// Helper extension function to capitalize first letter of string
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}