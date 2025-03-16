// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/OfflineRepositoryWrapper.kt

package istick.app.beta.repository

import istick.app.beta.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wrapper for repositories that provides offline support
 * This class adds functionality like:
 * - Maintaining a pending operations queue
 * - Automatically retrying operations when connectivity is restored
 * - Providing UI indicators for offline state
 */
class OfflineRepositoryWrapper(
    private val networkMonitor: NetworkMonitor,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Queue of pending operations to execute when back online
    private val pendingOperations = mutableListOf<PendingOperation>()

    // State flows for UI
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _hasPendingOperations = MutableStateFlow(false)
    val hasPendingOperations: StateFlow<Boolean> = _hasPendingOperations.asStateFlow()

    // Number of operations to retry at once
    private val batchSize = 5

    init {
        // Start monitoring network state
        networkMonitor.startMonitoring()

        // Watch for network state changes
        coroutineScope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                _isOffline.value = !isOnline

                if (isOnline && pendingOperations.isNotEmpty()) {
                    // We're back online and have pending operations - process them
                    processPendingOperations()
                }
            }
        }
    }

    /**
     * Execute an operation with offline support
     * If offline, the operation will be queued and executed when back online
     *
     * @param key A unique identifier for this operation (to prevent duplicates)
     * @param operation The operation to execute
     * @param isRequired Whether this operation is required or can be dropped
     * @param onSuccess Callback for successful execution
     * @param onError Callback for errors
     */
    fun <T> executeWithOfflineSupport(
        key: String,
        operation: suspend () -> Result<T>,
        isRequired: Boolean = true,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        coroutineScope.launch {
            if (networkMonitor.isOnline.value) {
                // We're online, execute immediately
                try {
                    val result = operation()
                    result.fold(
                        onSuccess = { onSuccess(it) },
                        onFailure = {
                            // If operation fails while online, queue it if required
                            if (isRequired) {
                                queueOperation(key, operation, isRequired, onSuccess, onError)
                            } else {
                                onError(it)
                            }
                        }
                    )
                } catch (e: Exception) {
                    // If operation throws while online, queue it if required
                    if (isRequired) {
                        queueOperation(key, operation, isRequired, onSuccess, onError)
                    } else {
                        onError(e)
                    }
                }
            } else {
                // We're offline, queue the operation
                queueOperation(key, operation, isRequired, onSuccess, onError)

                // Notify the user we're offline
                onError(OfflineException("No network connection. Operation will be executed when online."))
            }
        }
    }

    /**
     * Queue an operation to be executed when back online
     */
    private fun <T> queueOperation(
        key: String,
        operation: suspend () -> Result<T>,
        isRequired: Boolean,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Remove any existing operation with the same key to prevent duplicates
        pendingOperations.removeAll { it.key == key }

        // Add the new operation to the queue
        pendingOperations.add(
            PendingOperation(
                key = key,
                execute = {
                    try {
                        val result = operation()
                        result.fold(
                            onSuccess = { onSuccess(it as T) },
                            onFailure = { onError(it) }
                        )
                        true // Operation completed
                    } catch (e: Exception) {
                        onError(e)
                        false // Operation failed
                    }
                },
                isRequired = isRequired,
                timestamp = System.currentTimeMillis()
            )
        )

        // Update UI state
        _hasPendingOperations.value = pendingOperations.isNotEmpty()
    }

    /**
     * Process pending operations in batches
     */
    private suspend fun processPendingOperations() = withContext(Dispatchers.Default) {
        // Process operations in batches to avoid overwhelming the network
        while (pendingOperations.isNotEmpty()) {
            // Take a batch of operations
            val batch = pendingOperations.take(batchSize).toList()

            // Execute each operation in the batch
            batch.forEach { operation ->
                try {
                    val success = operation.execute()
                    if (success) {
                        // Operation succeeded, remove it from the queue
                        pendingOperations.remove(operation)
                    } else if (!operation.isRequired) {
                        // Non-required operation failed, remove it
                        pendingOperations.remove(operation)
                    }
                } catch (e: Exception) {
                    // If operation fails and is not required, remove it
                    if (!operation.isRequired) {
                        pendingOperations.remove(operation)
                    }
                }
            }

            // Update UI state
            _hasPendingOperations.value = pendingOperations.isNotEmpty()

            // Small delay between batches
            kotlinx.coroutines.delay(500)
        }
    }

    /**
     * Clear all pending operations
     */
    fun clearPendingOperations() {
        pendingOperations.clear()
        _hasPendingOperations.value = false
    }

    /**
     * Get the number of pending operations
     */
    fun getPendingOperationsCount(): Int {
        return pendingOperations.size
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        networkMonitor.stopMonitoring()
    }

    /**
     * Represents a pending operation in the queue
     */
    private data class PendingOperation(
        val key: String,
        val execute: suspend () -> Boolean,
        val isRequired: Boolean,
        val timestamp: Long
    )

    /**
     * Exception thrown when an operation is queued due to being offline
     */
    class OfflineException(message: String) : Exception(message)
}