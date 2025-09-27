package mpdc4gsr.permissions

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class PermissionController(private val activity: FragmentActivity) {

    private val usbManager: UsbManager =
        activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private var onPermissionsResult: ((isGranted: Boolean, denied: List<String>) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val denied = grants.filter { !it.value }.keys.toList()
            if (denied.isEmpty()) {
                Log.i(TAG, "All requested permissions were granted.")
                onPermissionsResult?.invoke(true, emptyList())
            } else {
                Log.w(TAG, "Some permissions were denied: ${denied.joinToString()}")
                handleDeniedPermissions(denied)
                onPermissionsResult?.invoke(false, denied)
            }
        }

    private val batteryOptimizationLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.i(TAG, "Returned from battery optimization settings.")
        }

    fun ensureAll(callback: (isGranted: Boolean, denied: List<String>) -> Unit) {
        this.onPermissionsResult = callback
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            Log.i(TAG, "All required permissions are already granted.")
            callback(true, emptyList())
            return
        }
        Log.i(TAG, "Found ${missing.size} missing permissions. Showing rationale.")
        showPermissionRationaleDialog(missing) { userAccepted ->
            if (userAccepted) {
                Log.i(TAG, "User accepted rationale. Launching permission request.")
                permissionLauncher.launch(missing.toTypedArray())
            } else {
                Log.w(TAG, "User declined permission rationale.")
                callback(false, missing)
            }
        }
    }

    fun requestUsbPermission(
        device: UsbDevice,
        callback: (isGranted: Boolean, device: UsbDevice?) -> Unit
    ) {
        if (usbManager.hasPermission(device)) {
            Log.i(TAG, "USB permission already granted for device ${device.productName}")
            callback(true, device)
            return
        }
        showUsbPermissionRationaleDialog(device) { userAccepted ->
            if (userAccepted) {
                try {
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val permissionIntent = PendingIntent.getBroadcast(
                        activity,
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        flags
                    )
                    usbManager.requestPermission(device, permissionIntent)
                    Log.i(TAG, "USB permission request sent for ${device.productName}.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request USB permission", e)
                    callback(false, null)
                }
            } else {
                Log.w(TAG, "User declined USB permission rationale.")
                callback(false, null)
            }
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            Log.i(TAG, "Battery optimization exemption not needed or already granted.")
            return
        }
        showBatteryOptimizationRationaleDialog { userAccepted ->
            if (userAccepted) {
                try {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    batteryOptimizationLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch battery optimization settings.", e)
                }
            } else {
                Log.w(TAG, "User declined battery optimization exemption.")
            }
        }
    }

    fun getMissingPermissions(): List<String> {
        return ALL_PERMISSIONS.filterNot { activity.isPermissionGranted(it) }
    }

    fun getPermissionStatusMessage(): String {
        val missing = getMissingPermissions()
        if (missing.isEmpty()) return "All permissions granted"
        val names = getPermissionNames(missing)
        return "Missing permissions:\n• ${names.joinToString("\n• ")}"
    }

    fun canStartRecording(): Boolean = hasCameraPermissions() && hasStoragePermissions()
    fun canConnectToShimmer(): Boolean = hasBluetoothPermissions() && hasLocationPermission()

    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager =
                activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true
        }
    }

    fun hasCameraPermissions(): Boolean =
        CAMERA_PERMISSIONS.all { activity.isPermissionGranted(it) }

    fun hasStoragePermissions(): Boolean =
        STORAGE_PERMISSIONS.all { activity.isPermissionGranted(it) }

    fun hasBluetoothPermissions(): Boolean =
        BLUETOOTH_PERMISSIONS.all { activity.isPermissionGranted(it) }

    fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any { activity.isPermissionGranted(it) }

    fun hasUsbPermissions(): Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    private fun showPermissionRationaleDialog(missing: List<String>, onResult: (Boolean) -> Unit) {
        val names = getPermissionNames(missing)
        val message = """
            This app requires the following permissions for multi-sensor recording:
            
            • ${names.joinToString("\n• ")}
            
            These permissions are essential for:
            • Recording video (Camera)
            • Connecting to GSR sensors (Bluetooth & Location)
            • Saving recordings (Media Access)
            • Displaying status updates (Notifications)
            
            The app will not function correctly without these permissions.
        """.trimIndent()

        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ -> onResult(true) }
            .setNegativeButton("Cancel") { _, _ -> onResult(false) }
            .setCancelable(false)
            .show()
    }

    private fun handleDeniedPermissions(denied: List<String>) {
        val permanentlyDenied = denied.filter { !activity.shouldShowRequestPermissionRationale(it) }
        if (permanentlyDenied.isNotEmpty()) {
            showPermanentlyDeniedDialog(permanentlyDenied)
        } else {
            Log.w(TAG, "User temporarily denied: ${denied.joinToString()}")
        }
    }

    private fun showPermanentlyDeniedDialog(permanentlyDenied: List<String>) {
        val names = getPermissionNames(permanentlyDenied)
        val message = """
            You have permanently denied the following critical permissions:
            
            • ${names.joinToString("\n• ")}
            
            To enable the app's core features, please grant these permissions manually in your device settings.
        """.trimIndent()

        AlertDialog.Builder(activity)
            .setTitle("Permissions Permanently Denied")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUsbPermissionRationaleDialog(
        device: UsbDevice,
        callback: (Boolean) -> Unit
    ) { /* ... implementation ... */
    }

    private fun showBatteryOptimizationRationaleDialog(callback: (Boolean) -> Unit) { /* ... implementation ... */
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
        }
    }

    fun getPermissionNames(permissions: List<String>): List<String> {
        return permissions.mapNotNull { PERMISSION_MAP[it] }.distinct()
    }

    // Add missing methods for compatibility
    fun hasAllRequiredPermissions(): Boolean {
        return getMissingPermissions().isEmpty()
    }

    fun initialize() {
        // This method is called for initialization purposes
        // Currently no specific initialization required
        Log.i(TAG, "PermissionController initialized")
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Handle legacy permission results if needed
        // Modern implementation uses ActivityResultLauncher
        Log.i(TAG, "Legacy onRequestPermissionsResult called with requestCode: $requestCode")
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        // Handle legacy activity results if needed 
        // Modern implementation uses ActivityResultLauncher
        Log.i(
            TAG,
            "Legacy onActivityResult called with requestCode: $requestCode, resultCode: $resultCode"
        )
    }

    fun requestBatteryOptimizationExemption(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            Log.i(TAG, "Battery optimization exemption not needed or already granted.")
            callback(true)
            return
        }
        showBatteryOptimizationRationaleDialog { userAccepted ->
            if (userAccepted) {
                try {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    batteryOptimizationLauncher.launch(intent)
                    callback(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch battery optimization settings.", e)
                    callback(false)
                }
            } else {
                Log.w(TAG, "User declined battery optimization exemption.")
                callback(false)
            }
        }
    }

    companion object {
        private const val TAG = "PermissionController"
        const val ACTION_USB_PERMISSION = "mpdc4gsr.USB_PERMISSION"

        private fun Context.isPermissionGranted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        private val CAMERA_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private val STORAGE_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        private val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private val NOTIFICATION_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        private val ALL_PERMISSIONS = listOfNotNull(
            *CAMERA_PERMISSIONS,
            *STORAGE_PERMISSIONS,
            *BLUETOOTH_PERMISSIONS,
            *LOCATION_PERMISSIONS,
            *NOTIFICATION_PERMISSIONS
        ).distinct()
        private val PERMISSION_MAP = mapOf(
            Manifest.permission.CAMERA to "Camera",
            Manifest.permission.RECORD_AUDIO to "Microphone",
            Manifest.permission.WRITE_EXTERNAL_STORAGE to "Storage",
            Manifest.permission.READ_EXTERNAL_STORAGE to "Storage",
            Manifest.permission.READ_MEDIA_VIDEO to "Media Access (Videos)",
            Manifest.permission.READ_MEDIA_IMAGES to "Media Access (Images)",
            Manifest.permission.BLUETOOTH_SCAN to "Bluetooth Scanning",
            Manifest.permission.BLUETOOTH_CONNECT to "Bluetooth Connections",
            Manifest.permission.BLUETOOTH to "Bluetooth",
            Manifest.permission.BLUETOOTH_ADMIN to "Bluetooth Administration",
            Manifest.permission.ACCESS_FINE_LOCATION to "Precise Location",
            Manifest.permission.ACCESS_COARSE_LOCATION to "Approximate Location",
            Manifest.permission.POST_NOTIFICATIONS to "Notifications"
        )
    }
}