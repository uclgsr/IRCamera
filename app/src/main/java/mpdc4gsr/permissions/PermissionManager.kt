package mpdc4gsr.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.kotlinx.coroutines.suspendCancellableCoroutine
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

        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "Camera permissions already granted")
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        Log.i(TAG, "Requesting camera permissions")


        permissionController.setPermissionCallback(REQUEST_CAMERA_PERMISSIONS) { granted ->
            Log.i(TAG, "Camera permissions result: $granted")
            continuation.resume(granted)
        }

        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CAMERA_PERMISSIONS)
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

        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "Bluetooth permissions already granted")
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        Log.i(TAG, "Requesting bluetooth permissions")


        permissionController.setPermissionCallback(REQUEST_BLUETOOTH_PERMISSIONS) { granted ->
            Log.i(TAG, "Bluetooth permissions result: $granted")
            continuation.resume(granted)
        }

        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
    }


    suspend fun requestStoragePermissions(): Boolean {

        return true
    }
}