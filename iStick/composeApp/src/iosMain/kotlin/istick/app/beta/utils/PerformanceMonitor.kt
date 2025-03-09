// File: iStick/composeApp/src/iosMain/kotlin/istick/app/beta/utils/PerformanceMonitor.kt
package istick.app.beta.utils

import platform.Foundation.NSLog
import platform.UIKit.UIDevice
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class PerformanceMonitor actual constructor(context: Any?) {
    private val traces = mutableMapOf<String, Long>()
    private val startTimes = mutableMapOf<String, Long>()

    actual fun startTrace(name: String) {
        try {
            startTimes[name] = System.currentTimeMillis()
            NSLog("PerformanceMonitor: Started trace: $name")
        } catch (e: Exception) {
            NSLog("PerformanceMonitor: Error starting trace $name: ${e.message}")
        }
    }

    actual fun stopTrace(name: String) {
        try {
            startTimes[name]?.let { startTime ->
                val duration = System.currentTimeMillis() - startTime
                NSLog("PerformanceMonitor: Trace $name completed in $duration ms")
                
                // Store the trace duration
                traces[name] = duration
            }

            startTimes.remove(name)
        } catch (e: Exception) {
            NSLog("PerformanceMonitor: Error stopping trace $name: ${e.message}")
        }
    }

    actual fun recordMetric(name: String, value: Long) {
        NSLog("PerformanceMonitor: Metric: $name = $value")
    }

    actual fun monitorMemory() {
        // Basic iOS memory usage info - not as detailed as Android
        NSLog("PerformanceMonitor: iOS device name: ${UIDevice.currentDevice.name}")
        NSLog("PerformanceMonitor: iOS system name: ${UIDevice.currentDevice.systemName}")
        NSLog("PerformanceMonitor: iOS system version: ${UIDevice.currentDevice.systemVersion}")
    }
    
    // Get all trace results
    fun getAllTraces(): Map<String, Long> {
        return traces.toMap()
    }
    
    // Clear all traces
    fun clearTraces() {
        traces.clear()
        startTimes.clear()
    }
}