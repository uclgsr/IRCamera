package mpdc4gsr.feature.connectivity.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun NetworkClient.startDiscoveryAsync(): Boolean =
    suspendCancellableCoroutine { continuation ->
        val callback: (Boolean) -> Unit = { success ->
            if (continuation.isActive) {
                continuation.resume(success)
            }
        }

        startDiscovery(callback)
        // Handle cancellation
        continuation.invokeOnCancellation {            // Cancel any ongoing discovery operations if needed
        }
    }

