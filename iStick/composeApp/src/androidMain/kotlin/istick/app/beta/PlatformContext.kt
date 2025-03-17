package istick.app.beta

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import istick.app.beta.di.DependencyInjection

actual fun getPlatformContext(): Any? {
    return DependencyInjection.getPlatformContext()
}

@Composable
actual fun getPlatformContextComposable(): Any? {
    return LocalContext.current
}