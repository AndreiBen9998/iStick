// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/SplashScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.ui.components.IStickLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    val scale = remember { Animatable(0.0f) }
    var showTagline by remember { mutableStateOf(false) }
    var showVersionInfo by remember { mutableStateOf(false) }

    // Launch animations in sequence
    LaunchedEffect(key1 = true) {
        // Logo animation
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 700,
                easing = EaseOutBack
            )
        )

        // Show tagline after logo animation
        showTagline = true

        // Show version info
        delay(300)
        showVersionInfo = true

        // Wait and then exit splash screen
        delay(1500)
        onTimeout()
    }

    // Pulsating effect for the tagline
    val infiniteTransition = rememberInfiniteTransition()
    val taglineAlpha = infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Slight bounce effect for the logo
    val logoOffset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Background color
    val bgColor = Color(0xFF0A1929)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = logoOffset.value.dp)
        ) {
            // Logo with scaling animation
            IStickLogo(
                modifier = Modifier.scale(scale.value),
                size = 180  // Larger logo
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated tagline
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(animationSpec = tween(500)) +
                        slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(500))
            ) {
                Text(
                    text = "Car Advertising Platform",
                    color = Color.White.copy(alpha = taglineAlpha.value),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle text
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(animationSpec = tween(700)) +
                        slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(700))
            ) {
                Text(
                    text = "Connect brands with drivers",
                    color = Color(0xFF1FBDFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Version info at the bottom
        AnimatedVisibility(
            visible = showVersionInfo,
            enter = fadeIn(animationSpec = tween(500)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Version 1.0 Beta",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}