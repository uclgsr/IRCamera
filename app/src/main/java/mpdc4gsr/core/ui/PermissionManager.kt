package mpdc4gsr.core.ui

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PermissionManager(
    private val activity: ComponentActivity,
    private val permissionController: PermissionController
) {
    companion object {
        private const val TAG = "PermissionManager"
    }

    /**
     * Requests all permissions defined as critical by the controller.
     * This function suspends until the user responds and returns true if all were granted.
     */
    suspend fun requestAllCriticalPermissions(): Boolean {
        AppLogger.i(TAG, "Requesting all critical permissions")
        return requestPermissionsWithController()
    }

    /**
     * Specifically requests camera and audio permissions.
     */
    suspend fun requestCameraPermissions(): Boolean {
        if (permissionController.hasCameraPermissions()) {
            AppLogger.i(TAG, "Camera permissions already granted")
            return true
        }
        AppLogger.i(TAG, "Requesting camera permissions")
        return requestPermissionsWithController()
    }

    /**
     * Specifically requests all necessary Bluetooth and Location permissions.
     */
    suspend fun requestBluetoothPermissions(): Boolean {
        if (permissionController.canConnectToShimmer()) {
            AppLogger.i(TAG, "Bluetooth permissions already granted")
            return true
        }
        AppLogger.i(TAG, "Requesting bluetooth permissions for GSR sensor access")
        return requestPermissionsWithController()
    }

    /**
     * Bridges the callback-based `permissionController.ensureAll` method
     * to a suspend function using a standard coroutine pattern.
     */
    private suspend fun requestPermissionsWithController(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            permissionController.ensureAll { isGranted, denied ->
                if (continuation.isActive) {
                    if (!isGranted) {
                        AppLogger.w(TAG, "Permissions denied: ${denied.joinToString()}")
                    }
                    continuation.resume(isGranted)
                }
            }
        }
    }
}