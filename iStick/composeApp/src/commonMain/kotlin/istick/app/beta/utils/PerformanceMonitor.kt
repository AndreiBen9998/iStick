// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/utils/PerformanceMonitor.kt
package istick.app.beta.utils

import android.content.Context
import android.util.Log
import android.app.ActivityManager

class PerformanceMonitor(private val context: Context) {
    private val traces = mutableMapOf<String, Any?>()
    private val startTimes = mutableMapOf<String, Long>()

    fun startTrace(name: String) {
        try {
            startTimes[name] = System.currentTimeMillis()
            Log.d("PerformanceMonitor", "Started trace: $name")
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error starting trace $name: ${e.message}")
        }
    }

    fun stopTrace(name: String) {
        try {
            startTimes[name]?.let { startTime ->
                val duration = System.currentTimeMillis() - startTime
                Log.d("PerformanceMonitor", "Trace $name completed in $duration ms")
            }

            startTimes.remove(name)
            traces.remove(name)
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error stopping trace $name: ${e.message}")
        }
    }

    fun recordMetric(name: String, value: Long) {
        Log.d("PerformanceMonitor", "Metric: $name = $value")
    }

    fun monitorMemory() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val availableMem = memoryInfo.availMem
            val totalMem = memoryInfo.totalMem
            val percentUsed = ((totalMem - availableMem).toFloat() / totalMem.toFloat()) * 100

            Log.d("PerformanceMonitor", "Memory used: $percentUsed%")
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error monitoring memory: ${e.message}")
        }
    }
}