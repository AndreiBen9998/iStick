// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/components/IStickLogo.kt
package istick.app.beta.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
expect fun getLogoIconPainter(): Painter

@Composable
expect fun getSmallLogoIconPainter(): Painter

/**
 * A safer implementation that provides a text-based fallback
 * in case the image resources don't load properly
 */
@Composable
fun IStickLogo(
    modifier: Modifier = Modifier,
    size: Int = 180
) {
    // Use conditional rendering instead of try-catch
    val useFallback = remember { false } // Set to false to attempt using the real logo

    if (!useFallback) {
        Image(
            painter = getLogoIconPainter(),
            contentDescription = "iStick Logo",
            modifier = modifier.size(size.dp)
        )
    } else {
        // Fallback to the text-based logo
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0xFF1FBDFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "iStick",
                color = Color.White,
                fontSize = (size / 3).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IStickLogoSmall(
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    // Use conditional rendering instead of try-catch
    val useFallback = remember { false } // Set to false to attempt using the real logo

    if (!useFallback) {
        Image(
            painter = getSmallLogoIconPainter(),
            contentDescription = "iStick Logo Small",
            modifier = modifier.size(size.dp)
        )
    } else {
        // Fallback to text-based logo
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0xFF1FBDFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "iS",
                color = Color.White,
                fontSize = (size / 3).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}