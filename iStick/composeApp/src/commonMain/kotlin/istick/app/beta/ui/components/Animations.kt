package istick.app.beta.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

// Modifier extension for press animation
fun Modifier.animatePress(onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// Animated entrance/exit transitions
val listItemEnterTransition = fadeIn(animationSpec = tween(300)) +
        slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))

val listItemExitTransition = fadeOut(animationSpec = tween(300)) +
        slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(300))

// Add a heart animation composable
@Composable
fun PulsatingHeart(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(isActive, label = "heartTransition")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                keyframes {
                    durationMillis = 300
                    0.6f at 0
                    1.3f at 150
                    1.0f at 300
                }
            } else {
                spring(stiffness = Spring.StiffnessLow)
            }
        },
        label = "heartScale"
    ) { active -> if (active) 1f else 0.8f }

    val color by transition.animateColor(
        transitionSpec = { tween(durationMillis = 300) },
        label = "heartColor"
    ) { active -> if (active) Color.Red else Color.Gray }

    Icon(
        imageVector = if (isActive) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = "Like",
        tint = color,
        modifier = modifier
            .scale(scale)
            .clickable { onClick() }
    )
}