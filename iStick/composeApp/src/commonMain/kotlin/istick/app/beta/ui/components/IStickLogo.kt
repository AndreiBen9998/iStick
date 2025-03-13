// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/components/IStickLogo.kt
package istick.app.beta.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
expect fun getLogoIconPainter(): Painter

@Composable
expect fun getSmallLogoIconPainter(): Painter

@Composable
fun IStickLogo(
    modifier: Modifier = Modifier,
    size: Int = 100
) {
    Image(
        painter = getLogoIconPainter(),
        contentDescription = "iStick Logo",
        modifier = modifier.size(size.dp)
    )
}

/**
 * Simplified version of the logo for use in smaller contexts like app bar
 */
@Composable
fun IStickLogoSmall(
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    Image(
        painter = getSmallLogoIconPainter(),
        contentDescription = "iStick Logo",
        modifier = modifier.size(size.dp)
    )
}