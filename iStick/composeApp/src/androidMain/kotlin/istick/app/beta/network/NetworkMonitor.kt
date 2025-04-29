// androidMain/kotlin/istick/app/beta/network/NetworkMonitor.kt
package istick.app.beta.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkMonitorImpl : NetworkMonitor {
    private val TAG = "NetworkMonitor"

    private val _isOnline = MutableStateFlow(true) // Optimistic initial value
    override val isOnline: StateFlow<Boolean> = _isOnline

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun startMonitoring() {
        try {
            if (connectivityManager == null || networkCallback == null) {
                Log.e(TAG, "Cannot start network monitoring: not properly initialized")
                return
            }

            // Check current connectivity state
            _isOnline.value = isCurrentlyConnected()

            // Register for network callbacks
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting network monitoring", e)
        }
    }

    override fun stopMonitoring() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback ?: return)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping network monitoring", e)
        }
    }

    private fun isCurrentlyConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current connectivity", e)
            true // Assume online if check fails
        }
    }

    companion object {
        fun create(context: Context): NetworkMonitorImpl {
            val monitor = NetworkMonitorImpl()

            try {
                // Get connectivity manager
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // Create network callback
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        monitor._isOnline.value = true
                    }

                    override fun onLost(network: Network) {
                        // Only set offline if no other network is available
                        if (!monitor.isCurrentlyConnected()) {
                            monitor._isOnline.value = false
                        }
                    }
                }

                monitor.connectivityManager = connectivityManager
                monitor.networkCallback = networkCallback
            } catch (e: Exception) {
                Log.e("NetworkMonitor", "Error initializing network monitor", e)
            }

            return monitor
        }
    }
}

actual fun createNetworkMonitor(): NetworkMonitor {
    // We can't access context here, so we create a default implementation
    // The real implementation will be created in the AppInitializer
    return object : NetworkMonitor {
        private val _isOnline = MutableStateFlow(true)
        override val isOnline: StateFlow<Boolean> = _isOnline

        override fun startMonitoring() {
            // No-op in default implementation
        }

        override fun stopMonitoring() {
            // No-op in default implementation
        }
    }
}