// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/network/NetworkMonitor.kt
package istick.app.beta.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for monitoring network connectivity
 */
interface NetworkMonitor {
    /**
     * Indicates whether the device is currently connected to the network
     */
    val isOnline: StateFlow<Boolean>
    
    /**
     * Start monitoring network state changes
     */
    fun startMonitoring()
    
    /**
     * Stop monitoring network state changes
     */
    fun stopMonitoring()
}

/**
 * Platform-specific implementation expected
 */
expect fun createNetworkMonitor(): NetworkMonitor