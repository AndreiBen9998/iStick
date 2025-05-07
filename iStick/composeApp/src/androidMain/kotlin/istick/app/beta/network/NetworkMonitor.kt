package istick.app.beta.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {
    private val TAG = "NetworkMonitor"

    private val _isOnline = MutableStateFlow(true) // Optimistic initial value
    override val isOnline: StateFlow<Boolean> = _isOnline

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        try {
            // Get connectivity manager
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Create network callback
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    // Only set offline if no other network is available
                    if (!isCurrentlyConnected()) {
                        _isOnline.value = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing network monitor", e)
        }
    }

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
}

fun createNetworkMonitor(context: Context): NetworkMonitor {
    return AndroidNetworkMonitor(context)
}