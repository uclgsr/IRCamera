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
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicBoolean

class PermissionController(private val activity: FragmentActivity) {

    private val isInitialized = AtomicBoolean(false)
    private var permissionCallback: ((Boolean, List<String>) -> Unit)? = null
    private var usbPermissionCallback: ((Boolean, UsbDevice?) -> Unit)? = null

    // Manages the currently displayed dialog to prevent window leaks.
    private var currentDialog: AlertDialog? = null

    // State for sequential permission requests.
    private var remainingPermissionGroups: MutableList<List<String>> = mutableListOf()
    private var allRequestedPermissions: List<String> = emptyList()

    private lateinit var usbManager: UsbManager
    private lateinit var powerManager: PowerManager

    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
            powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            Log.i(TAG, "PermissionController initialized")
        }
    }

    fun cleanup() {
        // Dismiss any active dialog to prevent window leaks when the activity is destroyed.
        currentDialog?.dismiss()
        currentDialog = null
        permissionCallback = null
        usbPermissionCallback = null
        Log.i(TAG, "PermissionController cleaned up")
    }

    // region Public Permission Checks
    fun hasAllRequiredPermissions(): Boolean {
        return getMissingPermissions().isEmpty() && isPermittedTopdonDeviceConnected()
    }

    fun hasCameraPermission(): Boolean = hasPermissions(CAMERA_PERMISSIONS)
    fun hasStoragePermissions(): Boolean = hasPermissions(getRequiredStoragePermissions())
    fun hasBluetoothPermissions(): Boolean = hasPermissions(getRequiredBluetoothPermissions())
    fun hasNotificationPermissions(): Boolean = hasPermissions(NOTIFICATION_PERMISSIONS)
    fun isBatteryOptimizationDisabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true // Optimizations do not exist on older APIs.
        }

    /**
     * Checks if a specific Topdon thermal camera is connected and has been granted permission.
     */
    fun isPermittedTopdonDeviceConnected(): Boolean {
        return usbManager.deviceList.values.any { device ->
            isTopdonDevice(device) && usbManager.hasPermission(device)
        }
    }
    // endregion

    // region Permission Request Flows
    /**
     * The main entry point to check for and request all necessary permissions.
     */
    fun ensureAll(callback: (Boolean, List<String>) -> Unit) {
        if (currentDialog != null) {
            Log.w(TAG, "Dialog already showing, cannot start new permission request flow.")
            callback(false, getMissingPermissions())
            return
        }

        permissionCallback = callback
        val missingPermissions = getMissingPermissions()

        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "All permissions are already granted.")
            callback(true, emptyList())
            return
        }

        showPermissionRationaleDialog(missingPermissions) { userAccepted ->
            if (userAccepted) {
                requestPermissionsSequentially(missingPermissions)
            } else {
                Log.w(TAG, "User declined the permission rationale.")
                callback(false, missingPermissions)
            }
        }
    }

    fun checkAndRequestThermalCameraPermissions(callback: (Boolean, List<UsbDevice>) -> Unit) {
        val thermalDevices = usbManager.deviceList.values.filter { isTopdonDevice(it) }

        if (thermalDevices.isEmpty()) {
            Log.w(TAG, "No Topdon TC001 thermal camera devices found.")
            callback(false, emptyList())
            return
        }

        val (devicesWithPermission, devicesNeedingPermission) = thermalDevices.partition {
            usbManager.hasPermission(it)
        }

        if (devicesNeedingPermission.isEmpty()) {
            Log.i(TAG, "All connected thermal camera devices already have permission.")
            callback(true, devicesWithPermission)
            return
        }

        // Request permission for the first device that needs it.
        requestUsbPermission(devicesNeedingPermission.first()) { granted, device ->
            val allPermittedDevices = devicesWithPermission.toMutableList()
            if (granted && device != null) {
                allPermittedDevices.add(device)
            }
            callback(granted, allPermittedDevices)
        }
    }

    fun requestBatteryOptimizationExemption(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            callback(true)
            return
        }

        showBatteryOptimizationRationaleDialog { userAccepted ->
            if (userAccepted) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open battery optimization settings.", e)
                    callback(false)
                }
            } else {
                Log.w(TAG, "User declined battery optimization exemption.")
                callback(false)
            }
        }
    }
    // endregion

    // region System Result Handlers
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQUEST_PERMISSIONS) return

        val deniedPermissions = permissions.indices
            .filter { grantResults[it] != PackageManager.PERMISSION_GRANTED }
            .map { permissions[it] }

        if (deniedPermissions.isNotEmpty()) {
            Log.w(TAG, "Denied permissions from group: ${deniedPermissions.joinToString()}")
        }

        if (remainingPermissionGroups.isNotEmpty()) {
            requestNextPermissionGroup()
        } else {
            // All groups have been requested, finalize the process.
            val stillMissing = allRequestedPermissions.filter { !isPermissionGranted(it) }
            if (stillMissing.isEmpty()) {
                Log.i(TAG, "All permissions successfully granted.")
                permissionCallback?.invoke(true, emptyList())
            } else {
                Log.w(TAG, "Final missing permissions: ${stillMissing.joinToString()}")
                handleDeniedPermissions(stillMissing)
                permissionCallback?.invoke(false, stillMissing)
            }
        }
    }

    fun onActivityResult(requestCode: Int) {
        if (requestCode == REQUEST_BATTERY_OPTIMIZATION) {
            Log.i(TAG, "Returned from battery optimization settings. New status: disabled=${isBatteryOptimizationDisabled()}")
            // The result is not directly returned; we must re-check the status.
            // A callback here could be used to notify the caller of the change.
        }
    }
    // endregion

    // region Private Helpers
    private fun requestPermissionsSequentially(missingPermissions: List<String>) {
        remainingPermissionGroups = groupPermissionsLogically(missingPermissions).toMutableList()
        allRequestedPermissions = missingPermissions
        requestNextPermissionGroup()
    }

    private fun requestNextPermissionGroup() {
        if (remainingPermissionGroups.isEmpty()) {
            // This case is handled in onRequestPermissionsResult after the last group is processed.
            Log.d(TAG, "All permission groups have been requested.")
            return
        }
        val nextGroup = remainingPermissionGroups.removeAt(0)
        Log.i(TAG, "Requesting permission group: ${nextGroup.joinToString()}")
        ActivityCompat.requestPermissions(activity, nextGroup.toTypedArray(), REQUEST_PERMISSIONS)
    }

    private fun requestUsbPermission(device: UsbDevice, callback: (Boolean, UsbDevice?) -> Unit) {
        usbPermissionCallback = callback

        if (usbManager.hasPermission(device)) {
            Log.i(TAG, "USB permission already granted for ${device.productName}")
            callback(true, device)
            return
        }

        showUsbPermissionRationaleDialog(device) { userAccepted ->
            if (userAccepted) {
                try {
                    val permissionIntent = PendingIntent.getBroadcast(
                        activity,
                        REQUEST_USB_PERMISSION,
                        Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    usbManager.requestPermission(device, permissionIntent)
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

    private fun getMissingPermissions(): List<String> {
        val requiredPermissions = CAMERA_PERMISSIONS +
                getRequiredStoragePermissions() +
                getRequiredBluetoothPermissions() +
                NOTIFICATION_PERMISSIONS

        return requiredPermissions.filterNot { isPermissionGranted(it) }.distinct()
    }

    private fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasPermissions(permissions: Array<String>): Boolean =
        permissions.all { isPermissionGranted(it) }

    private fun getRequiredStoragePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }

    private fun getRequiredBluetoothPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }

    private fun isTopdonDevice(device: UsbDevice): Boolean =
        device.vendorId == TOPDON_VENDOR_ID && device.productId == TC001_PRODUCT_ID
    // endregion

    // region Dialog Management
    private fun showPermissionRationaleDialog(
        missingPermissions: List<String>,
        callback: (Boolean) -> Unit
    ) {
        val permissionNames = getPermissionNames(missingPermissions)
        val message = "This app requires the following permissions for full functionality:\n\n" +
                permissionNames.joinToString("\n") { "• $it" }

        showDialog(
            title = "Permissions Required",
            message = message,
            positiveButtonText = "Grant Permissions",
            negativeButtonText = "Cancel",
            onPositive = { callback(true) },
            onNegative = { callback(false) }
        )
    }

    private fun showUsbPermissionRationaleDialog(device: UsbDevice, callback: (Boolean) -> Unit) {
        val message = "This permission is required to connect to the thermal camera.\n\n" +
                "Device: ${device.productName ?: "Unknown Device"}"
        showDialog(
            title = "Thermal Camera Access",
            message = message,
            positiveButtonText = "Allow",
            negativeButtonText = "Deny",
            onPositive = { callback(true) },
            onNegative = { callback(false) }
        )
    }

    private fun showBatteryOptimizationRationaleDialog(callback: (Boolean) -> Unit) {
        val message = "For reliable multi-sensor recording, this app needs to run continuously in the background.\n\n" +
                "Please disable battery optimization to ensure uninterrupted recording."
        showDialog(
            title = "Background Operation Required",
            message = message,
            positiveButtonText = "Allow",
            negativeButtonText = "Skip",
            onPositive = { callback(true) },
            onNegative = { callback(false) }
        )
    }

    private fun handleDeniedPermissions(deniedPermissions: List<String>) {
        val permanentlyDenied = deniedPermissions.any {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        if (permanentlyDenied) {
            showPermanentlyDeniedDialog(deniedPermissions)
        } else {
            showTemporarilyDeniedDialog(deniedPermissions)
        }
    }

    private fun showPermanentlyDeniedDialog(denied: List<String>) {
        val message = "Some permissions have been permanently denied. To enable them, you must go to the app settings.\n\n" +
                getPermissionNames(denied).joinToString("\n") { "• $it" }
        showDialog(
            title = "Permissions Required",
            message = message,
            positiveButtonText = "Open Settings",
            negativeButtonText = "Cancel",
            onPositive = { openAppSettings() }
        )
    }

    private fun showTemporarilyDeniedDialog(denied: List<String>) {
        val message = "The following permissions were denied but are required for some features:\n\n" +
                getPermissionNames(denied).joinToString("\n") { "• $it" }
        showDialog(
            title = "Permissions Denied",
            message = message,
            positiveButtonText = "OK"
        )
    }

    /**
     * A generic, centralized dialog builder to prevent leaks and reduce code duplication.
     */
    private fun showDialog(
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String? = null,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) {
        // Dismiss any existing dialog to avoid conflicts or leaks.
        currentDialog?.dismiss()

        val builder = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { _, _ -> onPositive?.invoke() }
            .setOnDismissListener { currentDialog = null }

        negativeButtonText?.let {
            builder.setNegativeButton(it) { _, _ -> onNegative?.invoke() }
        }

        currentDialog = builder.create()
        currentDialog?.show()
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
    // endregion

    companion object {
        private const val TAG = "PermissionController"
        const val ACTION_USB_PERMISSION = "mpdc4gsr.USB_PERMISSION"

        private const val REQUEST_PERMISSIONS = 100
        private const val REQUEST_USB_PERMISSION = 101
        private const val REQUEST_BATTERY_OPTIMIZATION = 102

        private val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private val STORAGE_PERMISSIONS_LEGACY = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        private val STORAGE_PERMISSIONS_ANDROID_13 = arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        private val BLUETOOTH_PERMISSIONS_LEGACY = arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        private val BLUETOOTH_PERMISSIONS_ANDROID_12 = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        private val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        // Note: USB permissions are handled per-device via UsbManager, not as standard runtime permissions.
        // This array was removed as it was misleading and unused in the permission request flow.

        private const val TOPDON_VENDOR_ID = 16902
        private const val TC001_PRODUCT_ID = 14082

        // Mapping from permission constant to a user-friendly name and description.
        private fun getPermissionNames(permissions: List<String>): List<String> {
            return permissions.mapNotNull {
                when (it) {
                    Manifest.permission.CAMERA -> "Camera (for video recording)"
                    Manifest.permission.RECORD_AUDIO -> "Microphone (for recording audio)"
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage (to save recordings)"
                    Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES -> "Media Access (to manage recordings)"
                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth (for GSR sensor)"
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION -> "Location (required for Bluetooth scanning)"
                    Manifest.permission.POST_NOTIFICATIONS -> "Notifications (to show recording status)"
                    else -> it.substringAfterLast('.').replace('_', ' ') // Fallback name
                }
            }.distinct()
        }

        private fun groupPermissionsLogically(permissions: List<String>): List<List<String>> {
            val groups = mutableListOf<List<String>>()
            val allPermissions = permissions.toMutableSet()

            fun extractGroup(groupPermissions: Array<String>) {
                val found = allPermissions.filter { it in groupPermissions }
                if (found.isNotEmpty()) {
                    groups.add(found)
                    allPermissions.removeAll(found)
                }
            }
            // Order is important: Request less intrusive permissions first if desired.
            extractGroup(CAMERA_PERMISSIONS)
            extractGroup(getRequiredBluetoothPermissions()) // Contains location, which has a sensitive rationale.
            extractGroup(getRequiredStoragePermissions())
            extractGroup(NOTIFICATION_PERMISSIONS)

            // Add any remaining permissions as a final group.
            if (allPermissions.isNotEmpty()) {
                groups.add(allPermissions.toList())
            }
            return groups
        }
    }
}