// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/LoginScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.auth.FirebaseAuthRepository
import istick.app.beta.utils.PerformanceMonitor
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("login_screen")
    }

    // Email and password state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Error state
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Loading state
    var isLoading by remember { mutableStateOf(false) }

    // Auth repository
    val authRepository = remember { FirebaseAuthRepository() }

    // Coroutine scope
    val coroutineScope = rememberCoroutineScope()

    // Focus manager
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A1929)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        ) {
            // Logo/app name
            Text(
                text = "iStick",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2962FF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Car Advertising Platform",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        handleLogin(
                            email = email,
                            password = password,
                            authRepository = authRepository,
                            setLoading = { isLoading = it },
                            setError = { errorMessage = it },
                            onSuccess = onLoginSuccess,
                            coroutineScope = coroutineScope
                        )
                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            AnimatedVisibility(visible = errorMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login button
            Button(
                onClick = {
                    handleLogin(
                        email = email,
                        password = password,
                        authRepository = authRepository,
                        setLoading = { isLoading = it },
                        setError = { errorMessage = it },
                        onSuccess = onLoginSuccess,
                        coroutineScope = coroutineScope
                    )
                },
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2962FF),
                    contentColor = Color.White,
                    disabledBackgroundColor = Color(0xFF2962FF).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign up link
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't have an account?",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Sign up",
                    color = Color(0xFF2962FF)
                )
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("login_screen")
        }
    }
}

// Handle login logic
private fun handleLogin(
    email: String,
    password: String,
    authRepository: FirebaseAuthRepository,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onSuccess: () -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    // Validate input
    if (email.isEmpty() || password.isEmpty()) {
        setError("Email and password cannot be empty")
        return
    }

    // Validate email format
    if (!email.contains("@") || !email.contains(".")) {
        setError("Please enter a valid email address")
        return
    }

    // Validate password length
    if (password.length < 6) {
        setError("Password must be at least 6 characters")
        return
    }

    // Clear error message
    setError(null)

    // Set loading state
    setLoading(true)

    // Attempt to sign in
    coroutineScope.launch {
        val result = authRepository.signIn(email, password)

        // Handle result
        result.fold(
            onSuccess = {
                // Success
                setLoading(false)
                onSuccess()
            },
            onFailure = { error ->
                // Failure
                setLoading(false)
                setError(error.message ?: "Login failed. Please try again.")
            }
        )
    }
}