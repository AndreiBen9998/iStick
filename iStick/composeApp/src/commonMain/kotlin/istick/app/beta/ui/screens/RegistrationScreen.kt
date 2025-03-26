// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/RegistrationScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.model.UserType
import istick.app.beta.utils.PerformanceMonitor
import istick.app.beta.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("registration_screen")
    }

    // Create repositories
    val authRepository = remember { istick.app.beta.di.DependencyInjection.getAuthRepository() }
    val userRepository = remember { istick.app.beta.di.DependencyInjection.getUserRepository() }

    // Create ViewModel
    val viewModel = remember {
        istick.app.beta.viewmodel.ViewModelFactory.createRegistrationViewModel(
            authRepository,
            userRepository
        )
    }

    // Get ViewModel state
    val registrationStep by viewModel.registrationStep.collectAsState()
    val userType by viewModel.userType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val registrationSuccess by viewModel.registrationSuccess.collectAsState()

    // Focus manager
    val focusManager = LocalFocusManager.current

    // If registration is successful, navigate to main screen
    LaunchedEffect(registrationSuccess) {
        if (registrationSuccess) {
            performanceMonitor.recordMetric("registration_success", 1)
            onRegistrationSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A1929))
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (registrationStep == RegistrationViewModel.RegistrationStep.SELECT_TYPE) {
                    onBackToLogin()
                } else {
                    viewModel.goBack()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = when (registrationStep) {
                    RegistrationViewModel.RegistrationStep.SELECT_TYPE -> "Create Account"
                    RegistrationViewModel.RegistrationStep.ACCOUNT_DETAILS -> "Account Details"
                    RegistrationViewModel.RegistrationStep.USER_DETAILS -> when (userType) {
                        UserType.CAR_OWNER -> "Car Owner Details"
                        UserType.BRAND -> "Brand Details"
                    }
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // Content based on registration step
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Step 1: Select user type
            AnimatedVisibility(
                visible = registrationStep == RegistrationViewModel.RegistrationStep.SELECT_TYPE,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { -it }
            ) {
                UserTypeSelectionScreen(
                    onSelectUserType = { viewModel.selectUserType(it) }
                )
            }

            // Step 2: Account details
            AnimatedVisibility(
                visible = registrationStep == RegistrationViewModel.RegistrationStep.ACCOUNT_DETAILS,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { -it }
            ) {
                AccountDetailsScreen(
                    viewModel = viewModel,
                    onContinue = { viewModel.moveToUserDetails() },
                    focusManager = focusManager
                )
            }

            // Step 3: User details
            AnimatedVisibility(
                visible = registrationStep == RegistrationViewModel.RegistrationStep.USER_DETAILS,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { -it }
            ) {
                when (userType) {
                    UserType.CAR_OWNER -> CarOwnerDetailsScreen(
                        viewModel = viewModel,
                        onRegister = { viewModel.register() },
                        focusManager = focusManager
                    )
                    UserType.BRAND -> BrandDetailsScreen(
                        viewModel = viewModel,
                        onRegister = { viewModel.register() },
                        focusManager = focusManager
                    )
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF2962FF),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Error message
        if (error != null) {
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                action = {
                    TextButton(onClick = { viewModel.resetError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                backgroundColor = Color(0xFFB71C1C)
            ) {
                Text(text = error ?: "")
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("registration_screen")
        }
    }
}

@Composable
fun UserTypeSelectionScreen(
    onSelectUserType: (UserType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose account type",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Text(
            text = "Select how you want to use iStick",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Car owner option
        UserTypeCard(
            title = "Car Owner",
            description = "I want to earn money by placing ads on my car",
            icon = Icons.Default.Person,
            onClick = { onSelectUserType(UserType.CAR_OWNER) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Brand option
        UserTypeCard(
            title = "Brand",
            description = "I want to advertise my products on cars",
            icon = Icons.Default.Home,
            onClick = { onSelectUserType(UserType.BRAND) }
        )
    }
}

@Composable
fun UserTypeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = Color(0xFF1A3B66),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2962FF),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AccountDetailsScreen(
    viewModel: RegistrationViewModel,
    onContinue: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val name by viewModel.name.collectAsState()

    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val nameError by viewModel.nameError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create your account",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Full Name") },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Name",
                    tint = Color.Gray
                )
            },
            isError = nameError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        if (nameError != null) {
            Text(
                text = nameError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            leadingIcon = {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Color.Gray
                )
            },
            isError = emailError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        if (emailError != null) {
            Text(
                text = emailError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Check else Icons.Default.Clear,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        // Confirm password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
            label = { Text("Confirm Password") },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Confirm Password",
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Check else Icons.Default.Clear,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
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

        if (passwordError != null) {
            Text(
                text = passwordError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2962FF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CarOwnerDetailsScreen(
    viewModel: RegistrationViewModel,
    onRegister: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    val city by viewModel.city.collectAsState()
    val dailyDrivingDistance by viewModel.dailyDrivingDistance.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Car Owner Details",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            text = "Tell us a bit about yourself. This information helps brands match you with relevant campaigns.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // City field
        OutlinedTextField(
            value = city,
            onValueChange = { viewModel.updateCity(it) },
            label = { Text("City") },
            leadingIcon = {
                Icon(
                    Icons.Default.Place,
                    contentDescription = "City",
                    tint = Color.Gray
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        // Slider for daily driving distance
        Text(
            text = "Daily Driving Distance: $dailyDrivingDistance km",
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Slider(
            value = dailyDrivingDistance.toFloat(),
            onValueChange = { viewModel.updateDailyDrivingDistance(it.toInt()) },
            valueRange = 0f..200f,
            steps = 20,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2962FF),
                activeTrackColor = Color(0xFF2962FF),
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Register button
        Button(
            onClick = onRegister,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2962FF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Complete Registration",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You can add your car details after registration",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun BrandDetailsScreen(
    viewModel: RegistrationViewModel,
    onRegister: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    val companyName by viewModel.companyName.collectAsState()
    val industry by viewModel.industry.collectAsState()
    val website by viewModel.website.collectAsState()
    val description by viewModel.description.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Brand Details",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            text = "Tell us about your brand. This helps car owners understand your business better.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Company name field
        OutlinedTextField(
            value = companyName,
            onValueChange = { viewModel.updateCompanyName(it) },
            label = { Text("Company Name") },
            leadingIcon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Company",
                    tint = Color.Gray
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        // Industry field
        OutlinedTextField(
            value = industry,
            onValueChange = { viewModel.updateIndustry(it) },
            label = { Text("Industry") },
            leadingIcon = {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Industry",
                    tint = Color.Gray
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        // Website field
        OutlinedTextField(
            value = website,
            onValueChange = { viewModel.updateWebsite(it) },
            label = { Text("Website (Optional)") },
            leadingIcon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Website",
                    tint = Color.Gray
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
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

        // Description field
        OutlinedTextField(
            value = description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Company Description (Optional)") },
            leadingIcon = {
                Icon(
                    Icons.Default.Create,
                    contentDescription = "Description",
                    tint = Color.Gray
                )
            },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color(0xFF2962FF),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF2962FF),
                unfocusedLabelColor = Color.Gray,
                backgroundColor = Color(0xFF1A3B66)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Register button
        Button(
            onClick = onRegister,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2962FF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Complete Registration",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You can create your first campaign after registration",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}