// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/network/NetworkMonitorImpl.kt
package istick.app.beta.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of network monitoring
 */
class AndroidNetworkMonitor(
    private val context: Context
) : NetworkMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            CoroutineScope(Dispatchers.Main).launch {
                _isOnline.value = true
            }
        }

        override fun onLost(network: Network) {
            CoroutineScope(Dispatchers.Main).launch {
                // Only set to offline if there are no other networks available
                val hasActiveNetwork = connectivityManager.activeNetwork != null
                _isOnline.value = hasActiveNetwork
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            // Check if we have internet capability
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )

            // Check if we can actually reach internet endpoints (e.g., not behind captive portal)
            val hasValidated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

            CoroutineScope(Dispatchers.Main).launch {
                _isOnline.value = hasInternet && hasValidated
            }
        }
    }

    override fun startMonitoring() {
        // Set initial state
        _isOnline.value = checkInitialNetworkState()

        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    private fun checkInitialNetworkState(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * Create a network monitor with context
 */
fun createNetworkMonitor(context: Context): NetworkMonitor {
    return AndroidNetworkMonitor(context)
}