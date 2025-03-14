package istick.app.beta.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import istick.app.beta.R

@Composable
actual fun getLogoIconPainter(): Painter {
    return painterResource(id = R.drawable.istick_logo)
}

@Composable
actual fun getSmallLogoIconPainter(): Painter {
    return painterResource(id = R.drawable.istick_logo_small)
}