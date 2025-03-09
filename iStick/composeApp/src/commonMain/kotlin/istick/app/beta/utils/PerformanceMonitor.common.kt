// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/utils/PerformanceMonitor.common.kt
package istick.app.beta.utils

expect class PerformanceMonitor(context: Any?) {
    fun startTrace(name: String)
    fun stopTrace(name: String)
    fun recordMetric(name: String, value: Long)
    fun monitorMemory()

    // Adding these methods to the common interface
    fun getAllTraces(): Map<String, Long>
    fun clearTraces()
}