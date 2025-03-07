// În ui.components/PerformanceModifiers.kt
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