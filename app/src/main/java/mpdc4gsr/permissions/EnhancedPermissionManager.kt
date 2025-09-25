package mpdc4gsr.permissions

import android.content.Context
import android.util.Log


class EnhancedPermissionManager(
    private val context: Context,
    private val permissionController: PermissionController
) {
    companion object {
        private const val TAG = "EnhancedPermissionManager"
    }


    suspend fun requestCameraPermissions(): Boolean {
        Log.d(TAG, "Enhanced camera permission request")

        return permissionController.hasCameraPermission()
    }


    suspend fun requestBluetoothPermissions(): Boolean {
        Log.d(TAG, "Enhanced Bluetooth permission request")

        return permissionController.hasBluetoothPermissions()
    }

    suspend fun requestAllCriticalPermissions(): Boolean {
        Log.d(TAG, "Enhanced request all critical permissions")
        return requestCameraPermissions() && requestBluetoothPermissions()
    }
}