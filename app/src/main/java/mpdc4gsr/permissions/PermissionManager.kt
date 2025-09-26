package mpdc4gsr.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


class PermissionManager(
    private val activity: FragmentActivity,
    private val permissionController: PermissionController
) {
    companion object {
        private const val TAG = "PermissionManager"
        private const val REQUEST_CAMERA_PERMISSIONS = 100
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 101
        private const val REQUEST_STORAGE_PERMISSIONS = 102
        private const val REQUEST_ALL_PERMISSIONS = 200
    }


    suspend fun requestCameraPermissions(): Boolean = suspendCancellableCoroutine { continuation ->
        val cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val missingPermissions = cameraPermissions.filter { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {            continuation.resume(true)
            return@suspendCancellableCoroutine
        }        // Use proper permission callback mechanism from PermissionController        permissionController.ensureAll { granted, deniedPermissions ->            if (deniedPermissions.isNotEmpty()) {}")
            }
            continuation.resume(granted)
        }

        // Camera permissions will be requested through the callback mechanism above
        // No additional request needed since ensureAll() handles the permission flow
    }


    suspend fun requestBluetoothPermissions(): Boolean = suspendCancellableCoroutine { continuation ->
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = bluetoothPermissions.filter { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {            continuation.resume(true)
            return@suspendCancellableCoroutine
        }        // Use proper permission callback mechanism from PermissionController        permissionController.ensureAll { granted, deniedPermissions ->            if (!granted) {}")
            }
            continuation.resume(granted)
        }

        // Enhanced callback with detailed error handling is now handled above
        // The permissions will be requested through the callback mechanism
    }


    suspend fun requestStoragePermissions(): Boolean {

        return true
    }

    /**
     * Request all permissions required for GSR sensor recording in a unified flow
     * This ensures smooth multi-sensor recording without mid-session permission issues
     */
    suspend fun requestAllRequiredPermissionsForGSR(): Boolean {        val cameraSuccess = requestCameraPermissions()
        val bluetoothSuccess = requestBluetoothPermissions()

        val allSuccess = cameraSuccess && bluetoothSuccess

        if (allSuccess) {        } else {        }

        return allSuccess
    }

    /**
     * Check if all GSR-related permissions are granted
     */
    fun hasAllGSRPermissions(): Boolean {
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val allPermissions = bluetoothPermissions + cameraPermissions

        return allPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}