package mpdc4gsr.feature.network.data

import kotlinx.coroutines.suspendCancellableCoroutine
import mpdc4gsr.core.utils.AppLogger
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
        continuation.invokeOnCancellation {
            AppLogger.d("NetworkSuspendExtensions", "Discovery cancelled")
            // Cancel any ongoing discovery operations if needed
        }
    }
