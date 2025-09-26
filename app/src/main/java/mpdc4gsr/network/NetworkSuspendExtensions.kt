package mpdc4gsr.network

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Modern suspend function extensions for NetworkClient replacing callback patterns
 * Demonstrates conversion of callback-based async operations to suspending functions
 */

/**
 * Suspend version of startDiscovery replacing callback pattern
 * Uses suspendCancellableCoroutine for proper cancellation support
 */
suspend fun NetworkClient.startDiscoveryAsync(): Boolean = suspendCancellableCoroutine { continuation ->
    val callback: (Boolean) -> Unit = { success ->
        if (continuation.isActive) {
            continuation.resume(success)
        }
    }

    startDiscovery(callback)

    // Handle cancellation
    continuation.invokeOnCancellation {
        Log.d("NetworkSuspendExtensions", "Discovery cancelled")
        // Cancel any ongoing discovery operations if needed
    }
}

/**
 * Example usage of modern suspend function patterns:
 *
 * // Old callback style:
 * networkClient.startDiscovery { success ->
 *     if (success) {
 *         // handle success
 *     } else {
 *         // handle failure
 *     }
 * }
 *
 * // New suspend style:
 * try {
 *     val success = networkClient.startDiscoveryAsync()
 *     if (success) {
 *         // handle success
 *     }
 * } catch (e: Exception) {
 *     // handle failure
 * }
 */