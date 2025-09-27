package mpdc4gsr.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicBoolean

class PermissionController(
    private val activity: FragmentActivity
) {
    companion object {
        private const val TAG = "PermissionController"

        private const val REQUEST_PERMISSIONS = 100
        private const val REQUEST_USB_PERMISSION = 101
        private const val REQUEST_BATTERY_OPTIMIZATION = 102
        
        // Simple permission request cooldown for MVP
        private const val PERMISSION_REQUEST_COOLDOWN_MS = 10000L // 10 seconds between requests

        private val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        private val STORAGE_PERMISSIONS_LEGACY = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        private val STORAGE_PERMISSIONS_ANDROID_13 = arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )

        private val BLUETOOTH_PERMISSIONS_LEGACY = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private val BLUETOOTH_PERMISSIONS_ANDROID_12 = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private val FOREGROUND_SERVICE_PERMISSIONS = arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CAMERA,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
        )

        private val NOTIFICATION_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }


        private val USB_PERMISSIONS = arrayOf(
            "android.permission.USB_PERMISSION",
            "android.permission.ACCESS_USB_ACCESSORY"
        )


        private const val TOPDON_VENDOR_ID = 16902
        private const val TC001_PRODUCT_ID = 14082
    }

    private val isInitialized = AtomicBoolean(false)
    private var permissionCallback: ((Boolean, List<String>) -> Unit)? = null
    private var usbPermissionCallback: ((Boolean, UsbDevice?) -> Unit)? = null

    // Simple permission request state for MVP
    private var lastPermissionRequestTime: Long = 0


    private var remainingPermissionGroups: MutableList<List<String>> = mutableListOf()
    private var allRequestedPermissions: List<String> = emptyList()

    private var usbManager: UsbManager? = null

    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
            Log.i(TAG, "PermissionController initialized")
        }
    }

    fun hasAllRequiredPermissions(): Boolean {
        return hasBasicPermissions() &&
                hasBluetoothPermissions() &&
                hasStoragePermissions() &&
                // Skip foreground service permissions as they are manifest permissions
                hasNotificationPermissions() &&
                hasUsbPermissions()
    }


    fun hasUsbPermissions(): Boolean {
        return usbManager?.deviceList?.values?.any { device ->
            device.vendorId == TOPDON_VENDOR_ID && device.productId == TC001_PRODUCT_ID
        } ?: false || hasManualUsbPermissions()
    }

    private fun hasManualUsbPermissions(): Boolean {

        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    }


    fun checkAndRequestThermalCameraPermissions(callback: (Boolean, List<UsbDevice>) -> Unit) {
        val thermalDevices = usbManager?.deviceList?.values?.filter { device ->
            device.vendorId == TOPDON_VENDOR_ID && device.productId == TC001_PRODUCT_ID
        } ?: emptyList()

        if (thermalDevices.isEmpty()) {
            Log.w(TAG, "No Topdon TC001 thermal camera devices found")
            callback(false, emptyList())
            return
        }

        val devicesWithPermission = mutableListOf<UsbDevice>()
        val devicesNeedingPermission = mutableListOf<UsbDevice>()

        for (device in thermalDevices) {
            if (usbManager?.hasPermission(device) == true) {
                devicesWithPermission.add(device)
            } else {
                devicesNeedingPermission.add(device)
            }
        }

        if (devicesNeedingPermission.isEmpty()) {
            Log.i(TAG, "All thermal camera devices have permissions")
            callback(true, devicesWithPermission)
            return
        }


        requestUsbPermission(devicesNeedingPermission.first()) { granted, device ->
            if (granted && device != null) {
                devicesWithPermission.add(device)
            }
            callback(granted, devicesWithPermission)
        }
    }


    fun ensureAll(callback: (Boolean, List<String>) -> Unit) {
        Log.d(TAG, "ensureAll() called - checking permissions")
        permissionCallback = callback

        val missingPermissions = getMissingPermissions()
        Log.d(TAG, "Missing permissions: ${missingPermissions.joinToString(", ")}")
        
        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "All permissions already granted")
            callback(true, emptyList())
            return
        }

        // Simple cooldown check - only request if enough time has passed
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastPermissionRequestTime
        
        if (timeSinceLastRequest < PERMISSION_REQUEST_COOLDOWN_MS) {
            Log.w(TAG, "Permission request on cooldown - ${timeSinceLastRequest}ms since last request")
            callback(false, missingPermissions)
            return
        }

        // Update timestamp and request permissions
        lastPermissionRequestTime = currentTime

        Log.d(TAG, "Requesting permissions")
        showPermissionRationaleDialog(missingPermissions) { userAccepted ->
            if (userAccepted) {
                requestPermissionsSequentially(missingPermissions)
            } else {
                callback(false, missingPermissions)
            }
        }
    }

    @Deprecated(
        "Use ensureAll() for better clarity and future compatibility.",
        ReplaceWith("ensureAll(callback)")
    )
    fun requestAllPermissions(callback: (Boolean, List<String>) -> Unit) {

        ensureAll(callback)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                val deniedPermissions = mutableListOf<String>()

                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permission)
                    }
                }

                Log.i(
                    TAG,
                    "Permission result: granted=${permissions.size - deniedPermissions.size}, denied=${deniedPermissions.size}"
                )


                if (remainingPermissionGroups.isNotEmpty()) {
                    Log.i(
                        TAG,
                        "Continuing with next permission group (${remainingPermissionGroups.size} groups remaining)"
                    )
                    requestNextPermissionGroup()
                } else {

                    val stillMissingPermissions =
                        allRequestedPermissions.filter { !isPermissionGranted(it) }
                    if (stillMissingPermissions.isEmpty()) {
                        Log.i(TAG, "All permissions successfully granted")
                        permissionCallback?.invoke(true, emptyList())
                    } else {
                        Log.w(
                            TAG,
                            "Some permissions still denied: ${stillMissingPermissions.joinToString(", ")}"
                        )
                        handleDeniedPermissions(stillMissingPermissions)
                        permissionCallback?.invoke(false, stillMissingPermissions)
                    }
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        when (requestCode) {
            REQUEST_BATTERY_OPTIMIZATION -> {

                val isBatteryOptimizationDisabled = isBatteryOptimizationDisabled()
                Log.i(TAG, "Battery optimization result: disabled=$isBatteryOptimizationDisabled")


            }
        }
    }

    fun requestUsbPermission(device: UsbDevice, callback: (Boolean, UsbDevice?) -> Unit) {
        usbPermissionCallback = callback

        val manager = usbManager
        if (manager == null) {
            Log.e(TAG, "USB Manager not available")
            callback(false, null)
            return
        }

        if (manager.hasPermission(device)) {
            Log.i(TAG, "USB permission already granted for device ${device.productName}")
            callback(true, device)
            return
        }

        Log.i(
            TAG,
            "Requesting USB permission for device: ${device.productName} (VID=${
                device.vendorId.toString(16)
            }, PID=${device.productId.toString(16)})"
        )


        showUsbPermissionRationaleDialog(device) { userAccepted ->
            if (userAccepted) {
                try {
                    val permissionIntent = android.app.PendingIntent.getBroadcast(
                        activity,
                        REQUEST_USB_PERMISSION,
                        Intent("mpdc4gsr.USB_PERMISSION"),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    manager.requestPermission(device, permissionIntent)

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request USB permission", e)
                    callback(false, null)
                }
            } else {
                Log.w(TAG, "User declined USB permission rationale")
                callback(false, null)
            }
        }
    }

    private fun showUsbPermissionRationaleDialog(device: UsbDevice, callback: (Boolean) -> Unit) {
        val message = buildString {
            appendLine("USB Device Permission Required")
            appendLine()
            appendLine("Device: ${device.productName ?: "Unknown Device"}")
            appendLine("Vendor ID: 0x${device.vendorId.toString(16).uppercase()}")
            appendLine("Product ID: 0x${device.productId.toString(16).uppercase()}")
            appendLine()
            appendLine("This permission is required to:")
            appendLine("• Connect to the thermal camera")
            appendLine("• Capture thermal imaging data")
            appendLine("• Perform real-time thermal analysis")
            appendLine()
            appendLine("The permission is granted on a per-device basis and is safe.")
        }

        AlertDialog.Builder(activity)
            .setTitle("Thermal Camera Access")
            .setMessage(message)
            .setPositiveButton("Allow Access") { _, _ ->
                callback(true)
            }
            .setNegativeButton("Deny") { _, _ ->
                callback(false)
            }
            .setCancelable(false)
            .show()
    }

    fun requestBatteryOptimizationExemption(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = activity.packageName
            val powerManager =
                activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.i(TAG, "Requesting battery optimization exemption")

                showBatteryOptimizationRationaleDialog { userAccepted ->
                    if (userAccepted) {
                        try {
                            val intent =
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            activity.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open battery optimization settings", e)

                            try {
                                val fallbackIntent =
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                activity.startActivity(fallbackIntent)

                                Log.w(
                                    TAG,
                                    "Fallback battery optimization settings opened - treating as not granted"
                                )
                                callback(false)
                            } catch (fallbackException: Exception) {
                                Log.e(
                                    TAG,
                                    "Failed to open fallback battery optimization settings",
                                    fallbackException
                                )
                                callback(false)
                            }
                        }
                    } else {
                        Log.w(TAG, "User declined battery optimization exemption")
                        callback(false)
                    }
                }
            } else {
                Log.i(TAG, "Battery optimization already disabled")
                callback(true)
            }
        } else {

            callback(true)
        }
    }

    private fun showBatteryOptimizationRationaleDialog(callback: (Boolean) -> Unit) {
        val message = buildString {
            appendLine("Battery Optimization Exemption")
            appendLine()
            appendLine("For reliable multi-sensor recording, this app needs to run continuously in the background.")
            appendLine()
            appendLine("Please disable battery optimization to ensure:")
            appendLine("• Uninterrupted video recording")
            appendLine("• Continuous GSR sensor data collection")
            appendLine("• Reliable thermal imaging capture")
            appendLine("• Stable network communication with PC")
            appendLine()
            appendLine("This setting allows the app to run efficiently without being killed by the system during long recording sessions.")
            appendLine()
            appendLine("You can always change this setting later in your device's battery settings.")
        }

        AlertDialog.Builder(activity)
            .setTitle("Background Operation Required")
            .setMessage(message)
            .setPositiveButton("Allow") { _, _ ->
                callback(true)
            }
            .setNegativeButton("Skip") { _, _ ->
                callback(false)
            }
            .setCancelable(false)
            .show()
    }


    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager =
                activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true
        }
    }

    fun getMissingPermissions(): List<String> {
        Log.d(TAG, "getMissingPermissions() called")
        val missing = mutableListOf<String>()

        Log.d(TAG, "Checking CAMERA_PERMISSIONS: ${CAMERA_PERMISSIONS.joinToString(", ")}")
        CAMERA_PERMISSIONS.forEach { permission ->
            val granted = isPermissionGranted(permission)
            Log.d(TAG, "Permission $permission: granted=$granted")
            if (!granted) {
                missing.add(permission)
            }
        }

        val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }

        Log.d(TAG, "Checking storage permissions (API ${Build.VERSION.SDK_INT}): ${storagePermissions.joinToString(", ")}")
        storagePermissions.forEach { permission ->
            val granted = isPermissionGranted(permission)
            Log.d(TAG, "Permission $permission: granted=$granted")
            if (!granted) {
                missing.add(permission)
            }
        }

        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }

        Log.d(TAG, "Checking bluetooth permissions (API ${Build.VERSION.SDK_INT}): ${bluetoothPermissions.joinToString(", ")}")
        bluetoothPermissions.forEach { permission ->
            val granted = isPermissionGranted(permission)
            Log.d(TAG, "Permission $permission: granted=$granted")
            if (!granted) {
                missing.add(permission)
            }
        }

        // FOREGROUND_SERVICE permissions are manifest permissions, not runtime permissions
        // Skip checking them as they don't require user dialogs
        Log.d(TAG, "Skipping FOREGROUND_SERVICE_PERMISSIONS as they are manifest permissions")

        Log.d(TAG, "Checking NOTIFICATION_PERMISSIONS: ${NOTIFICATION_PERMISSIONS.joinToString(", ")}")
        NOTIFICATION_PERMISSIONS.forEach { permission ->
            val granted = isPermissionGranted(permission)
            Log.d(TAG, "Permission $permission: granted=$granted")
            if (!granted) {
                missing.add(permission)
            }
        }

        val result = missing.distinct()
        Log.d(TAG, "getMissingPermissions() returning ${result.size} missing permissions: ${result.joinToString(", ")}")
        return result
    }

    fun getPermissionNames(permissions: List<String>): List<String> {
        return permissions.mapNotNull { permission ->
            when (permission) {
                Manifest.permission.CAMERA -> "Camera (for RGB video recording)"
                Manifest.permission.RECORD_AUDIO -> "Audio (for video recording with sound)"
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage (for saving recordings)"

                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED -> "Media Access (for managing recordings)"

                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN -> "Bluetooth (for Shimmer GSR sensor)"

                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION -> "Location (required for Bluetooth scanning)"

                Manifest.permission.POST_NOTIFICATIONS -> "Notifications (for recording status)"
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_CAMERA,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION -> "Background Recording (for continuous operation)"

                else -> null
            }
        }.distinct()
    }


    /**
     * Checks if all camera-related permissions are granted.
     *
     * Note: This method previously checked only a single camera permission (e.g., Manifest.permission.CAMERA).
     * It now requires that *all* permissions in CAMERA_PERMISSIONS are granted.
     * Ensure that this stricter check is intended wherever this method is used.
     */
    fun hasCameraPermission(): Boolean {
        return CAMERA_PERMISSIONS.all { isPermissionGranted(it) }
    }

    fun hasAudioPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    }

    fun hasLocationPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }


    fun canStartRecording(): Boolean {
        return hasCameraPermission() && hasStoragePermissions()
    }


    fun canConnectToShimmer(): Boolean {
        return hasBluetoothPermissions() && hasLocationPermission()
    }

    fun canConnectToShimmerLimited(): Boolean {
        // Can attempt Bluetooth connection without location if we have the core Bluetooth permissions
        return hasBluetoothPermissions()
    }

    fun getBluetoothConnectionStatus(): String {
        return when {
            hasBluetoothPermissions() && hasLocationPermission() -> "Full Bluetooth functionality available"
            hasBluetoothPermissions() -> "Limited Bluetooth functionality - device scanning may not work without location permission"
            else -> "Bluetooth permissions required for GSR sensor connection"
        }
    }


    fun canShowNotifications(): Boolean {
        return hasNotificationPermissions()
    }


    fun getPermissionStatusMessage(): String {
        val status = mutableListOf<String>()

        if (!hasCameraPermission()) {
            status.add("Camera permission required for video recording")
        }
        if (!hasBluetoothPermissions()) {
            status.add("Bluetooth permissions required for GSR sensor")
        }
        if (!hasLocationPermission()) {
            if (hasBluetoothPermissions()) {
                status.add("Location permission missing - Bluetooth scanning limited, manual pairing may still work")
            } else {
                status.add("Location permission required for Bluetooth scanning")
            }
        }
        if (!hasStoragePermissions()) {
            status.add("Storage permissions required for saving recordings")
        }
        if (!hasNotificationPermissions() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            status.add("Notification permission required for recording status")
        }

        return if (status.isEmpty()) {
            "All permissions granted"
        } else {
            "Missing permissions:\n• ${status.joinToString("\n• ")}"
        }
    }


    private fun hasBasicPermissions(): Boolean {
        return CAMERA_PERMISSIONS.all { isPermissionGranted(it) }
    }

    fun hasBluetoothPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }

        return requiredPermissions.all { isPermissionGranted(it) }
    }

    fun hasStoragePermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }

        return requiredPermissions.all { isPermissionGranted(it) }
    }

    fun hasNotificationPermissions(): Boolean {
        return NOTIFICATION_PERMISSIONS.all { isPermissionGranted(it) }
    }

    private fun hasForegroundServicePermissions(): Boolean {
        return FOREGROUND_SERVICE_PERMISSIONS.all { isPermissionGranted(it) }
    }

    private fun requestPermissionsSequentially(missingPermissions: List<String>) {

        val permissionGroups = groupPermissionsLogically(missingPermissions)

        if (permissionGroups.isEmpty()) {
            permissionCallback?.invoke(true, emptyList())
            return
        }


        remainingPermissionGroups = permissionGroups.toMutableList()
        allRequestedPermissions = missingPermissions


        requestNextPermissionGroup()
    }

    private fun requestNextPermissionGroup() {
        Log.d(TAG, "requestNextPermissionGroup() called")
        if (remainingPermissionGroups.isEmpty()) {
            Log.d(TAG, "No more permission groups remaining")
            val stillMissingPermissions =
                allRequestedPermissions.filter { !isPermissionGranted(it) }
            if (stillMissingPermissions.isEmpty()) {
                Log.i(TAG, "All permission groups successfully granted")
                permissionCallback?.invoke(true, emptyList())
            } else {
                Log.w(
                    TAG,
                    "Some permissions still missing after all groups: ${
                        stillMissingPermissions.joinToString(", ")
                    }"
                )
                handleDeniedPermissions(stillMissingPermissions)
                permissionCallback?.invoke(false, stillMissingPermissions)
            }
            return
        }

        val nextGroup = remainingPermissionGroups.removeAt(0)
        Log.i(TAG, "Requesting permission group: ${nextGroup.joinToString(", ")}")
        Log.d(TAG, "Calling activity.requestPermissions() with REQUEST_PERMISSIONS=$REQUEST_PERMISSIONS")
        activity.requestPermissions(nextGroup.toTypedArray(), REQUEST_PERMISSIONS)
        Log.d(TAG, "activity.requestPermissions() call completed")
    }

    private fun groupPermissionsLogically(permissions: List<String>): List<List<String>> {
        val groups = mutableListOf<List<String>>()


        val cameraGroup = permissions.filter { it in CAMERA_PERMISSIONS }
        if (cameraGroup.isNotEmpty()) {
            groups.add(cameraGroup)
        }


        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }

        val bluetoothGroup = permissions.filter { it in bluetoothPermissions }
        if (bluetoothGroup.isNotEmpty()) {
            groups.add(bluetoothGroup)
        }


        val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }

        val storageGroup = permissions.filter { it in storagePermissions }
        if (storageGroup.isNotEmpty()) {
            groups.add(storageGroup)
        }


        val notificationGroup = permissions.filter {
            it in NOTIFICATION_PERMISSIONS || it in FOREGROUND_SERVICE_PERMISSIONS
        }
        if (notificationGroup.isNotEmpty()) {
            groups.add(notificationGroup)
        }

        return groups
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRationaleDialog(
        missingPermissions: List<String>,
        callback: (Boolean) -> Unit
    ) {
        Log.d(TAG, "showPermissionRationaleDialog() called with ${missingPermissions.size} permissions")
        val permissionNames = getPermissionNames(missingPermissions)
        Log.d(TAG, "Permission names: ${permissionNames.joinToString(", ")}")

        val message = buildString {
            appendLine("This app requires the following permissions for multi-sensor recording:")
            appendLine()
            permissionNames.forEach { name ->
                appendLine("• $name")
            }
            appendLine()
            appendLine("These permissions are essential for:")
            appendLine("• Recording high-quality RGB video")
            appendLine("• Connecting to Shimmer GSR sensors via Bluetooth")
            appendLine("• Interfacing with thermal cameras via USB")
            appendLine("• Saving and managing recording data")
            appendLine("• Running continuous background recording")
            appendLine()
            appendLine("The app will not function properly without these permissions.")
        }

        Log.d(TAG, "Creating AlertDialog for permission rationale")
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                Log.d(TAG, "User clicked 'Grant Permissions'")
                callback(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d(TAG, "User clicked 'Cancel'")
                callback(false)
            }
            .setCancelable(false)
            .create()
            
        Log.d(TAG, "Showing permission rationale dialog")
        dialog.show()
        Log.d(TAG, "Permission rationale dialog.show() called")
    }

    private fun showPermissionExplanationDialog(
        missingPermissions: List<String>,
        callback: (Boolean) -> Unit
    ) {
        val permissionNames = getPermissionNames(missingPermissions)

        val message = buildString {
            appendLine("This app requires the following permissions for multi-sensor recording:")
            appendLine()
            permissionNames.forEach { name ->
                appendLine("• $name")
            }
            appendLine()
            appendLine("These permissions are essential for:")
            appendLine("• Recording high-quality RGB video")
            appendLine("• Connecting to Shimmer GSR sensors via Bluetooth")
            appendLine("• Interfacing with thermal cameras via USB")
            appendLine("• Saving and managing recording data")
            appendLine("• Running continuous background recording")
        }

        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                callback(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                callback(false)
            }
            .setCancelable(false)
            .show()
    }

    private fun handleDeniedPermissions(deniedPermissions: List<String>) {

        val permanentlyDeniedPermissions = deniedPermissions.filter { permission ->
            !activity.shouldShowRequestPermissionRationale(permission)
        }

        if (permanentlyDeniedPermissions.isNotEmpty()) {
            showPermanentlyDeniedDialog(permanentlyDeniedPermissions, deniedPermissions)
        } else {
            showDeniedPermissionsDialog(deniedPermissions)
        }
    }

    private fun showPermanentlyDeniedDialog(
        permanentlyDenied: List<String>,
        allDenied: List<String>
    ) {
        val criticalPermissions = getCriticalPermissions(allDenied)
        val locationPermissions = getLocationPermissions(permanentlyDenied)
        val permissionNames = getPermissionNames(permanentlyDenied)

        val message = buildString {
            appendLine("Some permissions have been permanently denied:")
            appendLine()
            permissionNames.forEach { name ->
                appendLine("• $name")
            }
            appendLine()
            
            if (criticalPermissions.isNotEmpty()) {
                appendLine("Critical permissions required for:")
                if (criticalPermissions.any { it in CAMERA_PERMISSIONS }) {
                    appendLine("• Video recording functionality")
                }
                if (criticalPermissions.any { it in getBluetoothPermissions() }) {
                    appendLine("• Shimmer GSR sensor connection")
                }
                appendLine()
                appendLine("The app cannot function properly without these permissions.")
                appendLine("Please enable them in Settings > Apps > IRCamera > Permissions")
            }
            
            if (locationPermissions.isNotEmpty()) {
                if (criticalPermissions.isNotEmpty()) {
                    appendLine()
                    appendLine("Location permissions are needed for:")
                } else {
                    appendLine("Location permissions are required for:")
                }
                appendLine("• Bluetooth device scanning for GSR sensors")
                appendLine("• Automatic address detection for recordings")
                appendLine()
                if (criticalPermissions.isEmpty()) {
                    appendLine("You can continue with limited Bluetooth functionality.")
                    appendLine("Manual sensor pairing may still be possible.")
                }
            }
            
            if (criticalPermissions.isEmpty() && locationPermissions.isEmpty()) {
                appendLine("You can continue with limited functionality, but some features may not work properly.")
            }
        }

        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(if (criticalPermissions.isEmpty()) "Continue Limited" else "Exit") { _, _ ->
                if (criticalPermissions.isNotEmpty()) {
                    // Only exit for truly critical permissions (camera, core bluetooth)
                    activity.finish()
                }
                // For location-only or other non-critical permissions, allow continuation
            }
            .setCancelable(false)
            .show()
    }

    private fun showDeniedPermissionsDialog(deniedPermissions: List<String>) {
        val criticalPermissions = getCriticalPermissions(deniedPermissions)
        val permissionNames = getPermissionNames(deniedPermissions)

        val message = buildString {
            appendLine("The following permissions were denied:")
            appendLine()
            permissionNames.forEach { name ->
                appendLine("• $name")
            }
            appendLine()
            if (criticalPermissions.isNotEmpty()) {
                appendLine("These permissions are required for core functionality.")
                appendLine("Would you like to try granting them again?")
            } else {
                appendLine("You can continue with limited functionality.")
            }
        }

        AlertDialog.Builder(activity)
            .setTitle("Permissions Denied")
            .setMessage(message)
            .setPositiveButton(if (criticalPermissions.isNotEmpty()) "Try Again" else "OK") { _, _ ->
                if (criticalPermissions.isNotEmpty()) {

                    ensureAll(permissionCallback ?: { _, _ -> })
                }
            }
            .setNegativeButton(if (criticalPermissions.isNotEmpty()) "Continue Limited" else null) { _, _ ->

            }
            .show()
    }

    private fun getCriticalPermissions(permissions: List<String>): List<String> {
        return permissions.filter { permission ->
            permission in CAMERA_PERMISSIONS ||
                    permission == Manifest.permission.BLUETOOTH_SCAN ||
                    permission == Manifest.permission.BLUETOOTH_CONNECT
        }
    }

    fun getLocationPermissions(permissions: List<String>): List<String> {
        return permissions.filter { permission ->
            permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                    permission == Manifest.permission.ACCESS_COARSE_LOCATION
        }
    }

    private fun getBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }
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

    /**
     * Reset permission request cooldown (simplified for MVP)
     */
    fun resetPermissionState() {
        Log.i(TAG, "Resetting permission cooldown")
        lastPermissionRequestTime = 0
    }

    /**
     * Check if basic permissions are available for core functionality
     */
    fun hasMinimumPermissions(): Boolean {
        return hasBasicPermissions() && hasStoragePermissions()
    }

    /**
     * Simple cooldown check - are we still in cooldown period?
     */
    fun shouldSkipPermissionRequest(): Boolean {
        val timeSinceLastRequest = System.currentTimeMillis() - lastPermissionRequestTime
        return timeSinceLastRequest < PERMISSION_REQUEST_COOLDOWN_MS
    }

    fun isLocationPermissionPermanentlyDenied(): Boolean {
        return (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) && 
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) ||
               (!isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && 
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}
