// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/SplashScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.ui.components.IStickLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for the logo
    val scale = remember { Animatable(0.0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseOutBack
            )
        )
        delay(1500) // Wait for 1.5 seconds
        onTimeout()
    }

    // Pulsating effect for the tagline
    val infiniteTransition = rememberInfiniteTransition()
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            IStickLogo(
                modifier = Modifier.scale(scale.value),
                size = 150
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tagline
            Text(
                text = "Car Advertising Platform",
                color = Color.White.copy(alpha = alpha.value),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Version info at the bottom
        Text(
            text = "Version 1.0",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}