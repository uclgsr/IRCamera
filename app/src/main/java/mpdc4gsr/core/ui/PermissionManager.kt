package mpdc4gsr.core.ui

import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PermissionManager(
    private val activity: ComponentActivity,
    private val permissionController: PermissionController
) {
    companion object {
    }

    suspend fun requestAllCriticalPermissions(): Boolean {
        return requestPermissionsWithController()
    }

    suspend fun requestCameraPermissions(): Boolean {
        if (permissionController.hasCameraPermissions()) {
            return true
        }
        return requestPermissionsWithController()
    }

    suspend fun requestBluetoothPermissions(): Boolean {
        if (permissionController.canConnectToShimmer()) {
            return true
        }
        return requestPermissionsWithController()
    }

    private suspend fun requestPermissionsWithController(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            permissionController.ensureAll { isGranted, denied ->
                if (continuation.isActive) {
                    if (!isGranted) {
                    }
                    continuation.resume(isGranted)
                }
            }
        }
    }
}