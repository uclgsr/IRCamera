package mpdc4gsr.core.ui

import android.util.Log
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PermissionManager(
    private val activity: androidx.activity.ComponentActivity,
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
        Log.i(TAG, "Requesting all critical permissions")
        return requestPermissionsWithController()
    }

    /**
     * Specifically requests camera and audio permissions.
     */
    suspend fun requestCameraPermissions(): Boolean {
        if (permissionController.hasCameraPermissions()) {
            Log.i(TAG, "Camera permissions already granted")
            return true
        }
        Log.i(TAG, "Requesting camera permissions")
        return requestPermissionsWithController()
    }

    /**
     * Specifically requests all necessary Bluetooth and Location permissions.
     */
    suspend fun requestBluetoothPermissions(): Boolean {
        if (permissionController.canConnectToShimmer()) {
            Log.i(TAG, "Bluetooth permissions already granted")
            return true
        }
        Log.i(TAG, "Requesting bluetooth permissions for GSR sensor access")
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
                        Log.w(TAG, "Permissions denied: ${denied.joinToString()}")
                    }
                    continuation.resume(isGranted)
                }
            }
        }
    }
}