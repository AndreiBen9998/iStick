// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/utils/PerformanceMonitor.kt
package istick.app.beta.utils

import android.content.Context
import android.util.Log
import android.app.ActivityManager

actual class PerformanceMonitor actual constructor(private val context: Any?) {
    private val traces = mutableMapOf<String, Long>()
    private val startTimes = mutableMapOf<String, Long>()

    actual fun startTrace(name: String) {
        try {
            startTimes[name] = System.currentTimeMillis()
            Log.d("PerformanceMonitor", "Started trace: $name")
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error starting trace $name: ${e.message}")
        }
    }

    actual fun stopTrace(name: String) {
        try {
            startTimes[name]?.let { startTime ->
                val duration = System.currentTimeMillis() - startTime
                Log.d("PerformanceMonitor", "Trace $name completed in $duration ms")

                // Store the trace duration
                traces[name] = duration
            } ?: Log.w("PerformanceMonitor", "Tried to stop non-existent trace: $name")

            startTimes.remove(name)
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error stopping trace $name: ${e.message}")
        }
    }

    actual fun recordMetric(name: String, value: Long) {
        try {
            Log.d("PerformanceMonitor", "Metric: $name = $value")
        } catch (e: Exception) {
            // Silently fail on platforms that don't support logging
        }
    }

    actual fun monitorMemory() {
        try {
            if (context is Context) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                val availableMem = memoryInfo.availMem
                val totalMem = memoryInfo.totalMem
                val percentUsed = ((totalMem - availableMem).toFloat() / totalMem.toFloat()) * 100

                Log.d("PerformanceMonitor", "Memory used: $percentUsed%")
            } else {
                Log.e("PerformanceMonitor", "Context is not an Android Context")
            }
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error monitoring memory: ${e.message}")
        }
    }

    // Get all trace results - useful for debugging
    actual fun getAllTraces(): Map<String, Long> {
        return traces.toMap()
    }

    // Clear all traces
    actual fun clearTraces() {
        traces.clear()
        startTimes.clear()
    }
}