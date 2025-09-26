package mpdc4gsr.permissions

import android.content.Context


class EnhancedPermissionManager(
    private val context: Context,
    private val permissionController: PermissionController
) {
    companion object {
        private const val TAG = "EnhancedPermissionManager"
    }


    suspend fun requestCameraPermissions(): Boolean {        return permissionController.hasCameraPermission()
    }


    suspend fun requestBluetoothPermissions(): Boolean {        return permissionController.hasBluetoothPermissions()
    }

    suspend fun requestAllCriticalPermissions(): Boolean {        return requestCameraPermissions() && requestBluetoothPermissions()
    }
}