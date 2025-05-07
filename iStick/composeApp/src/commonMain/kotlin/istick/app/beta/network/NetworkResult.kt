// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/network/NetworkResult.kt
package istick.app.beta.network

/**
 * Sealed class for network operation results
 */
sealed class NetworkResult<out T> {
    /**
     * Successful operation with data
     */
    data class Success<T>(val data: T) : NetworkResult<T>()
    
    /**
     * Error with message
     */
    data class Error(val message: String, val code: Int = 0) : NetworkResult<Nothing>()
    
    /**
     * Loading state
     */
    object Loading : NetworkResult<Nothing>()
    
    /**
     * Helper function to handle result with callbacks
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Helper function to handle error with callbacks
     */
    inline fun onError(action: (String) -> Unit): NetworkResult<T> {
        if (this is Error) action(message)
        return this
    }
    
    /**
     * Helper function to handle loading state with callbacks
     */
    inline fun onLoading(action: () -> Unit): NetworkResult<T> {
        if (this is Loading) action()
        return this
    }
}