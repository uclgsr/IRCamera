package mpdc4gsr.core.ui

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
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import mpdc4gsr.core.utils.AppLogger

class PermissionController(private val activity: ComponentActivity) {
    private val usbManager: UsbManager =
        activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private var onPermissionsResult: ((isGranted: Boolean, denied: List<String>) -> Unit)? = null
    private var currentDialog: AlertDialog? = null
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val denied = grants.filter { !it.value }.keys.toList()
            if (denied.isEmpty()) {
                AppLogger.i(TAG, "All requested permissions were granted.")
                onPermissionsResult?.invoke(true, emptyList())
            } else {
                AppLogger.w(TAG, "Some permissions were denied: ${denied.joinToString()}")
                handleDeniedPermissions(denied)
                onPermissionsResult?.invoke(false, denied)
            }
        }
    private val batteryOptimizationLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            AppLogger.i(TAG, "Returned from battery optimization settings.")
        }

    fun ensureAll(callback: (isGranted: Boolean, denied: List<String>) -> Unit) {
        this.onPermissionsResult = callback
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            AppLogger.i(TAG, "All required permissions are already granted.")
            callback(true, emptyList())
            return
        }
        AppLogger.i(TAG, "Found ${missing.size} missing permissions. Showing rationale.")
        showPermissionRationaleDialog(missing) { userAccepted ->
            if (userAccepted) {
                AppLogger.i(TAG, "User accepted rationale. Launching permission request.")
                permissionLauncher.launch(missing.toTypedArray())
            } else {
                AppLogger.w(TAG, "User declined permission rationale.")
                callback(false, missing)
            }
        }
    }

    fun requestUsbPermission(
        device: UsbDevice,
        callback: (isGranted: Boolean, device: UsbDevice?) -> Unit
    ) {
        if (usbManager.hasPermission(device)) {
            AppLogger.i(TAG, "USB permission already granted for device ${device.productName}")
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
                    AppLogger.i(TAG, "USB permission request sent for ${device.productName}.")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to request USB permission", e)
                    callback(false, null)
                }
            } else {
                AppLogger.w(TAG, "User declined USB permission rationale.")
                callback(false, null)
            }
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            AppLogger.i(TAG, "Battery optimization exemption not needed or already granted.")
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
                    AppLogger.e(TAG, "Failed to launch battery optimization settings.", e)
                }
            } else {
                AppLogger.w(TAG, "User declined battery optimization exemption.")
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
        // Dismiss any existing dialog first
        dismissCurrentDialog()
        // Check if activity is still valid
        if (activity.isFinishing || activity.isDestroyed) {
            onResult(false)
            return
        }
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
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                onResult(true)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onResult(false)
            }
            .setCancelable(false)
            .setOnDismissListener { currentDialog = null }
            .create()
        currentDialog?.show()
    }

    private fun handleDeniedPermissions(denied: List<String>) {
        val permanentlyDenied = denied.filter { !activity.shouldShowRequestPermissionRationale(it) }
        if (permanentlyDenied.isNotEmpty()) {
            showPermanentlyDeniedDialog(permanentlyDenied)
        } else {
            AppLogger.w(TAG, "User temporarily denied: ${denied.joinToString()}")
        }
    }

    private fun showPermanentlyDeniedDialog(permanentlyDenied: List<String>) {
        // Dismiss any existing dialog first
        dismissCurrentDialog()
        // Check if activity is still valid
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        val names = getPermissionNames(permanentlyDenied)
        val message = """
            You have permanently denied the following critical permissions:
            
            • ${names.joinToString("\n• ")}
            
            To enable the app's core features, please grant these permissions manually in your device settings.
        """.trimIndent()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Permissions Permanently Denied")
            .setMessage(message)
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener { currentDialog = null }
            .create()
        currentDialog?.show()
    }

    private fun showUsbPermissionRationaleDialog(
        device: UsbDevice,
        callback: (Boolean) -> Unit
    ) {
        currentDialog?.dismiss()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("USB Device Permission Required")
            .setMessage("The app needs permission to access USB device:\n${device.deviceName}\n\nThis is required for thermal camera communication.")
            .setPositiveButton("Grant") { dialog, _ ->
                callback(true)
                dialog.dismiss()
            }
            .setNegativeButton("Deny") { dialog, _ ->
                callback(false)
                dialog.dismiss()
            }
            .setOnCancelListener {
                callback(false)
            }
            .create()
        currentDialog?.show()
    }

    private fun showBatteryOptimizationRationaleDialog(callback: (Boolean) -> Unit) {
        currentDialog?.dismiss()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Battery Optimization")
            .setMessage("Disabling battery optimization ensures uninterrupted sensor data recording.\n\nThis prevents the system from stopping background sensor operations.")
            .setPositiveButton("Disable Optimization") { dialog, _ ->
                callback(true)
                dialog.dismiss()
            }
            .setNegativeButton("Keep Enabled") { dialog, _ ->
                callback(false)
                dialog.dismiss()
            }
            .setOnCancelListener {
                callback(false)
            }
            .create()
        currentDialog?.show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to open app settings", e)
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
        AppLogger.i(TAG, "PermissionController initialized")
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Handle legacy permission results if needed
        // Modern implementation uses ActivityResultLauncher
        AppLogger.i(TAG, "Legacy onRequestPermissionsResult called with requestCode: $requestCode")
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
            AppLogger.i(TAG, "Battery optimization exemption not needed or already granted.")
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
                    AppLogger.e(TAG, "Failed to launch battery optimization settings.", e)
                    callback(false)
                }
            } else {
                AppLogger.w(TAG, "User declined battery optimization exemption.")
                callback(false)
            }
        }
    }

    private fun dismissCurrentDialog() {
        currentDialog?.let { dialog ->
            if (dialog.isShowing) {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to dismiss dialog: ${e.message}")
                }
            }
            currentDialog = null
        }
    }

    fun cleanup() {
        dismissCurrentDialog()
        onPermissionsResult = null
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