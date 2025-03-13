// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/MileageVerificationScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.camera.rememberCameraLauncher
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.MileageVerificationViewModel

@Composable
fun MileageVerificationScreen(
    viewModel: MileageVerificationViewModel,
    carId: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("mileage_verification_screen")
        viewModel.loadCarDetails(carId)
        viewModel.generateVerificationCode()
    }

    // Get state
    val car by viewModel.car.collectAsState()
    val state by viewModel.state.collectAsState()
    val verificationCode by viewModel.verificationCode.collectAsState()

    // Camera launcher
    val cameraLauncher = rememberCameraLauncher { imageBytes ->
        viewModel.processImage(imageBytes)
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mileage Verification") },
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
                .padding(padding)
                .background(Color(0xFF0F2030))
        ) {
            if (state.isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2962FF),
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (state.isVerified) {
                // Success state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Verification Successful!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your mileage has been verified.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onSuccess,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2962FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Cars")
                    }
                }
            } else {
                // Verification process
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Car details
                    car?.let { currentCar ->
                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "${currentCar.make} ${currentCar.model}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "License plate: ${currentCar.licensePlate}",
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Current mileage: ${currentCar.currentMileage} km",
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Verification code
                        Text(
                            text = "Verification Code",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            backgroundColor = Color(0xFF1A3B66),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = verificationCode,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2962FF),
                                    letterSpacing = 4.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Write this code on a piece of paper and place it next to your odometer",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Image capture
                        Text(
                            text = "Take a Photo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.imageUri != null) {
                            // Show captured image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A3B66)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Placeholder for image
                                Text(
                                    text = "Image Captured",
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Detected mileage
                            state.detectedMileage?.let { mileage ->
                                Card(
                                    backgroundColor = Color(0xFF1A3B66),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Detected Mileage",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "$mileage km",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2962FF)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetCapturedImage() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF2962FF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Retake Photo")
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = { viewModel.submitVerification(carId) },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF2962FF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Submit")
                                }
                            }
                        } else {
                            // Camera button
                            Button(
                                onClick = { cameraLauncher() },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF1A3B66)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color(0xFF2962FF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Take a photo of your odometer with the verification code")
                                }
                            }
                        }
                    }

                    // Error message
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            backgroundColor = Color(0xFF800000),
                            modifier = Modifier.fillMaxWidth()
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
                                    text = state.error ?: "",
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("mileage_verification_screen")
        }
    }
}