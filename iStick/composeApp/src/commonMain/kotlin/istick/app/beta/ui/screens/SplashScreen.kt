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
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
// For Android-specific version
import android.graphics.drawable.VectorDrawable
import androidx.core.graphics.drawable.DrawableCompat

/**
 * This composable provides platform-specific customizations for the splash screen
 */
@Composable
expect fun PlatformSplashScreenEffect()

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Apply platform-specific effects if needed
    PlatformSplashScreenEffect()

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

// With this:
            Box(
                modifier = Modifier.scale(scale.value)
                    .size(220.dp, 120.dp) // Adjusted size to maintain aspect ratio
            ) {
                Image(
                    painter = rememberVectorPainter(
                        ImageVector.Builder(
                            defaultWidth = 1106.44.dp,
                            defaultHeight = 601.84.dp,
                            viewportWidth = 1106.44f,
                            viewportHeight = 601.84f
                        ).apply {
                            // Only include the chameleon and the "i" in iStick for the splash screen
                            // This keeps the unique branding elements while ensuring they render properly
                            addPath(
                                pathData = PathParser.createPathFromPathData("M610.29,1.02c-24.82,8.39-0.28,38.87-13.64,42.42-0.67 0.18 -1.39 0.19 -2.07 0.09 -50.18-7.69-87.14 0.56 -133.13,57.8-31.09,38.51-59.76,138.65,21.34,193.57 0.13 0.09 0.27 0.18 0.41 0.26,53.03,30.6,124.41,8.28,117.78-60.83-0.58-34.36-53.99-53.63-68.75-19.07-8.41,20.77,20.99,37.27,25.55,11,0,0-0.67-0.15-0.67-0.15-2.41,8.19-13.51,12.72-16.48,3.24-0.09-0.29-0.16-0.6-0.2-0.9-1.78-13.5,13.01-17.9,23.8-13.31 0.22 0.09 0.43 0.2 0.64 0.32,30.44,17.17-2.43,66.04-31.53,57.03-58.69-13.55-66.22-68.75-35.36-94.54 0.42 -0.35 0.96 -0.64 0.96 -0.64 0.53 -0.33,2.48-1.55,3.57-1.67,1.8-0.2,4.08,2.29,4.85,4.51,2.74,7.88-10.88,19.07-9.12,24.09 0.35 ,1.01,1.06,1.95,2.23,2.8,7.22,5.28,13.54-8.37,19.72-13.59,3.43-2.9,19.77-10.08,11.06-16.85-4.45-3.45-7.08-0.14-9.77-0.29-1.88-0.1-0.77-4.4-2.36-8.63-0.06-0.17-0.19-0.33-0.34-0.5,2.35-0.67,4.79-1.22,7.34-1.64,10.27-2.39,20.36-4.06,29.85-6.09,1.87-0.4,3.8 0.21 ,5.07,1.64,2.64,2.95,5.54,6.42,4.29,7.75-1.92,2.04-5.97,1.51-7.11,7.23-2.22,11.21,14.51,4.73,18.9,4.39,7.92-0.61,21.25,4.84,23.31-4.21,3.12-13.73-28.87-4.11-21.35-21.52,0-0.01 0.01 -0.02 0.02 -0.04 0.89 -2.04,3.8-4.02,5.24-4.69,7.3-4.69,45.43-19.07,53.31-19.12 0.24 ,0,0.48 0.03 0.71 0.07 ,14.36,2.35,35.63,2.22,47.11-3.42-0.64 0.01 -1.3 0.02 -1.97 0.02 -5.93,0-13.2-0.61-19.84-2.58-9.18-2.74-15.63-5.03-20.02-8.08-0.96-0.67-1.1-2.03-0.34-2.92h0c0.67-0.79,1.82-0.92,2.68-0.33,3.97,2.73,10.26,4.94,18.82,7.49,9.03,2.69,19.36,2.67,25.27,2.14 0.35 -0.03 0.68 0.04 0.98 0.17 0.72 -0.83,1.31-1.72,1.72-2.69,1.27-2.44,4.01-18.11,4.67-26.57 0.35 -5.59,2.62-8.55,2.62-15.58,0-26.18-61.33-87.17-89.76-77.56Z"),
                                fill = SolidColor(Color(0xFF1FBDFF))
                            )
                            // Add the eye
                            addPath(
                                pathData = PathParser.createPathFromPathData("M 648.49 50.6 C 659.949908559 50.6 669.24 59.890091441 669.24 71.35 C 669.24 82.809908559 659.949908559 92.1 648.49 92.1 C 637.030091441 92.1 627.74 82.809908559 627.74 71.35 C 627.74 59.890091441 637.030091441 50.6 648.49 50.6 Z"),
                                fill = SolidColor(Color.White)
                            )
                            // Add the pupil
                            addPath(
                                pathData = PathParser.createPathFromPathData("M652.85,61.97c16,8.64-8.58,34.02-14.24,10.26-0.14-0.58 0.22 -1.17 0.77 -1.29l4.4-0.96c0.25-0.06 0.39 -0.35 0.28 -0.6l-2.42-5.71c-0.28-0.65 0.06 -1.39 0.71 -1.55,3.41-0.82,7.27-1.89,10.5-0.15Z"),
                                fill = SolidColor(Color.Black)
                            )

                            // Add the "i" in iStick
                            addPath(
                                pathData = PathParser.createPathFromPathData("M0,232.35h62.04v362.67H0V232.35Z"),
                                fill = SolidColor(Color(0xFF1FBDFF))
                            )
                        }.build(),
                    ),
                    contentDescription = "iStick Logo",
                    modifier = Modifier.fillMaxSize()
                )

                // Add the "Stick" text separately for better readability
                Text(
                    text = "Stick",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .offset(x = (-10).dp, y = (60).dp)
                )
            }

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