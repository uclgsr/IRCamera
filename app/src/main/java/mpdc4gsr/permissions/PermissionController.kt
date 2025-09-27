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

/**
 * A centralised controller for handling Android runtime permissions using the modern
 * AndroidX Activity Result APIs.
 *
 * This refactored class removes the need for manual request codes and overriding
 * `onRequestPermissionsResult` or `onActivityResult` in the Activity.
 */
class PermissionController(private val activity: FragmentActivity) {

    // --- Private Properties ---
    private val usbManager: UsbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager

    private var onPermissionsResult: ((isGranted: Boolean, denied: List<String>) -> Unit)? = null

    // --- Activity Result Launchers ---

    /**
     * Modern launcher for requesting multiple runtime permissions.
     * The callback handles the results, replacing the old `onRequestPermissionsResult`.
     */
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

    /**
     * Modern launcher for activities that return a result, like the battery optimization screen.
     * This replaces the `startActivityForResult` and `onActivityResult` pattern.
     */
    private val batteryOptimizationLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // The result from the battery optimization screen isn't reliable.
            // We simply log that the user has returned from the settings screen.
            Log.i(TAG, "Returned from battery optimization settings.")
        }

    // --- Public API ---

    /**
     * The primary method to check for and request all necessary permissions.
     * It presents a clear rationale to the user before launching the system's permission dialog.
     * @param callback A lambda that will be invoked with the result: `true` if all permissions
     * are granted, `false` otherwise, along with a list of any denied permissions.
     */
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

    /**
     * Requests permission for a specific USB device (e.g., a thermal camera).
     * Note: USB permission is granted via a `PendingIntent` and a `BroadcastReceiver`,
     * which is a separate mechanism from standard runtime permissions.
     */
    fun requestUsbPermission(device: UsbDevice, callback: (isGranted: Boolean, device: UsbDevice?) -> Unit) {
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
                        0, // Request code is not needed here
                        Intent(ACTION_USB_PERMISSION),
                        flags
                    )
                    // The result of this is handled by a BroadcastReceiver listening for ACTION_USB_PERMISSION
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

    /**
     * Requests the user to disable battery optimization for the app to ensure
     * reliable background operation during long recording sessions.
     */
    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            Log.i(TAG, "Battery optimization exemption not needed or already granted.")
            return
        }

        showBatteryOptimizationRationaleDialog { userAccepted ->
            if (userAccepted) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
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

    // --- Permission Status Checkers ---

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
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true
        }
    }
    
    private fun hasCameraPermissions(): Boolean = CAMERA_PERMISSIONS.all { activity.isPermissionGranted(it) }
    private fun hasStoragePermissions(): Boolean = STORAGE_PERMISSIONS.all { activity.isPermissionGranted(it) }
    private fun hasBluetoothPermissions(): Boolean = BLUETOOTH_PERMISSIONS.all { activity.isPermissionGranted(it) }
    private fun hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any { activity.isPermissionGranted(it) }

    // --- Rationale and Denial Dialogs ---

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
            // Optionally, show a dialog explaining why the denied permissions are needed
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
    
    // --- Other Dialogs (Unchanged) ---
    private fun showUsbPermissionRationaleDialog(device: UsbDevice, callback: (Boolean) -> Unit) { /* ... same as original ... */ }
    private fun showBatteryOptimizationRationaleDialog(callback: (Boolean) -> Unit) { /* ... same as original ... */ }

    // --- Helpers and Constants ---

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

    companion object {
        private const val TAG = "PermissionController"
        const val ACTION_USB_PERMISSION = "mpdc4gsr.USB_PERMISSION"

        // Helper extension function for checking permissions
        private fun Context.isPermissionGranted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        // --- Permission Groups based on SDK Version ---
        private val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        private val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
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

        private val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        // Consolidated list of all runtime permissions
        private val ALL_PERMISSIONS = listOfNotNull(
            *CAMERA_PERMISSIONS,
            *STORAGE_PERMISSIONS,
            *BLUETOOTH_PERMISSIONS,
            *LOCATION_PERMISSIONS,
            *NOTIFICATION_PERMISSIONS
        ).distinct()

        // Map for user-friendly permission names
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
