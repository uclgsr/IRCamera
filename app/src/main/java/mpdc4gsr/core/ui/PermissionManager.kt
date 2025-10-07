package mpdc4gsr.core.ui

import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import mpdc4gsr.core.utils.AppLogger
import kotlin.coroutines.resume

class PermissionManager(
    private val activity: ComponentActivity,
    private val permissionController: PermissionController
) {
    companion object {
        private const val TAG = "PermissionManager"
    }

    suspend fun requestAllCriticalPermissions(): Boolean {
        AppLogger.i(TAG, "Requesting all critical permissions")
        return requestPermissionsWithController()
    }

    suspend fun requestCameraPermissions(): Boolean {
        if (permissionController.hasCameraPermissions()) {
            AppLogger.i(TAG, "Camera permissions already granted")
            return true
        }
        AppLogger.i(TAG, "Requesting camera permissions")
        return requestPermissionsWithController()
    }

    suspend fun requestBluetoothPermissions(): Boolean {
        if (permissionController.canConnectToShimmer()) {
            AppLogger.i(TAG, "Bluetooth permissions already granted")
            return true
        }
        AppLogger.i(TAG, "Requesting bluetooth permissions for GSR sensor access")
        return requestPermissionsWithController()
    }

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