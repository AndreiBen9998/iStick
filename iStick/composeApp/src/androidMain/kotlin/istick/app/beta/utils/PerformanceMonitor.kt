// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/utils/PerformanceMonitor.kt
package istick.app.beta.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.annotation.Keep

@Keep
actual class PerformanceMonitor actual constructor(private val context: Any?) {
    // Safe cast to Android Context
    private val androidContext: Context? = context as? Context

    // Traces storage
    private val traces = mutableMapOf<String, Long>()
    private val startTimes = mutableMapOf<String, Long>()

    actual fun startTrace(name: String) {
        try {
            val startTime = System.currentTimeMillis()
            startTimes[name] = startTime
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
            val ctx = androidContext
            if (ctx != null) {
                val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                val availableMem = memoryInfo.availMem
                val totalMem = memoryInfo.totalMem
                val percentUsed = ((totalMem - availableMem).toFloat() / totalMem.toFloat()) * 100

                Log.d("PerformanceMonitor", "Memory used: $percentUsed%")
                Log.d("PerformanceMonitor", "Total Memory: $totalMem")
                Log.d("PerformanceMonitor", "Available Memory: $availableMem")
            } else {
                Log.e("PerformanceMonitor", "Context is not an Android Context")
            }
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error monitoring memory: ${e.message}")
        }
    }

    // Get all trace results
    actual fun getAllTraces(): Map<String, Long> {
        return traces.toMap()
    }

    // Clear all traces
    actual fun clearTraces() {
        traces.clear()
        startTimes.clear()
    }
}