package mpdc4gsr.feature.network.data

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
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
        continuation.invokeOnCancellation {
            AppLogger.d("NetworkSuspendExtensions", "Discovery cancelled")
            // Cancel any ongoing discovery operations if needed
        }
    }
