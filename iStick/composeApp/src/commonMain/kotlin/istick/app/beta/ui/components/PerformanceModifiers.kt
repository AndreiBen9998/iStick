// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/components/PerformanceModifiers.kt
package istick.app.beta.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.measureRenderTime(
    componentName: String,
    performanceMonitor: PerformanceMonitor
): Modifier = composed {
    val traceName = "render_$componentName"

    DisposableEffect(Unit) {
        performanceMonitor.startTrace(traceName)
        onDispose {
            performanceMonitor.stopTrace(traceName)
        }
    }

    this
}