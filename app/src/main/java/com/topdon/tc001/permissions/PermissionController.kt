package com.topdon.tc001.permissions

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

        // USB permissions for thermal camera integration
        private val USB_PERMISSIONS = arrayOf(
            "android.permission.USB_PERMISSION",
            "android.permission.ACCESS_USB_ACCESSORY"
        )

        // Thermal camera device identifiers
        private const val TOPDON_VENDOR_ID = 16902
        private const val TC001_PRODUCT_ID = 14082
    }

    private val isInitialized = AtomicBoolean(false)
    private var permissionCallback: ((Boolean, List<String>) -> Unit)? = null
    private var usbPermissionCallback: ((Boolean, UsbDevice?) -> Unit)? = null
    
    // Track remaining permission groups for sequential processing
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
                hasForegroundServicePermissions() &&
                hasNotificationPermissions() &&
                hasUsbPermissions()
    }

    /**
     * Check if USB permissions are granted for thermal camera integration
     */
    fun hasUsbPermissions(): Boolean {
        return usbManager?.deviceList?.values?.any { device ->
            device.vendorId == TOPDON_VENDOR_ID && device.productId == TC001_PRODUCT_ID
        } ?: false || hasManualUsbPermissions()
    }

    private fun hasManualUsbPermissions(): Boolean {
        // Check if we have at least USB host permissions
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    }

    /**
     * Request USB permission for a specific thermal camera device
     */
    fun requestUsbPermission(device: UsbDevice, callback: (Boolean, UsbDevice?) -> Unit) {
        usbPermissionCallback = callback
        
        if (usbManager?.hasPermission(device) == true) {
            Log.i(TAG, "USB permission already granted for device: ${device.deviceName}")
            callback(true, device)
            return
        }

        Log.i(TAG, "Requesting USB permission for device: ${device.deviceName}")
        
        val permissionIntent = Intent("com.topdon.tc001.USB_PERMISSION").apply {
            setPackage(activity.packageName)
        }
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.getBroadcast(
                activity,
                REQUEST_USB_PERMISSION,
                permissionIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getBroadcast(
                activity,
                REQUEST_USB_PERMISSION,
                permissionIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        usbManager?.requestPermission(device, pendingIntent)
    }

    /**
     * Check for connected thermal camera devices and request permissions
     */
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

        // Request permission for the first device needing it
        requestUsbPermission(devicesNeedingPermission.first()) { granted, device ->
            if (granted && device != null) {
                devicesWithPermission.add(device)  
            }
            callback(granted, devicesWithPermission)
        }
    }

    /**
     * Centralized method to ensure all required permissions are granted before starting a session.
     * This groups required permissions logically and requests them together in a single system prompt,
     * rather than truly sequencing prompts one-by-one. Handles user responses gracefully.
     *
     * @param callback Called with (success, deniedPermissions) when permission check completes
     */
    fun ensureAll(callback: (Boolean, List<String>) -> Unit) {
        permissionCallback = callback

        val missingPermissions = getMissingPermissions()
        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "All permissions already granted")
            callback(true, emptyList())
            return
        }

        Log.i(TAG, "Requesting ${missingPermissions.size} missing permissions: ${missingPermissions.joinToString(", ")}")

        // Show rationale first, then request permissions
        showPermissionRationaleDialog(missingPermissions) { userAccepted ->
            if (userAccepted) {
                requestPermissionsSequentially(missingPermissions)
            } else {
                Log.w(TAG, "User declined permission rationale")
                callback(false, missingPermissions)
            }
        }
    }

@Deprecated("Use ensureAll() for better clarity and future compatibility.", ReplaceWith("ensureAll(callback)"))
fun requestAllPermissions(callback: (Boolean, List<String>) -> Unit) {
    // Legacy method - delegates to ensureAll for consistency
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

                Log.i(TAG, "Permission result: granted=${permissions.size - deniedPermissions.size}, denied=${deniedPermissions.size}")
                
                // Continue processing remaining permission groups
                if (remainingPermissionGroups.isNotEmpty()) {
                    Log.i(TAG, "Continuing with next permission group (${remainingPermissionGroups.size} groups remaining)")
                    requestNextPermissionGroup()
                } else {
                    // All groups processed, evaluate final result
                    val stillMissingPermissions = allRequestedPermissions.filter { !isPermissionGranted(it) }
                    if (stillMissingPermissions.isEmpty()) {
                        Log.i(TAG, "All permissions successfully granted")
                        permissionCallback?.invoke(true, emptyList())
                    } else {
                        Log.w(TAG, "Some permissions still denied: ${stillMissingPermissions.joinToString(", ")}")
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
                // Check if battery optimization is now disabled
                val isBatteryOptimizationDisabled = isBatteryOptimizationDisabled()
                Log.i(TAG, "Battery optimization result: disabled=$isBatteryOptimizationDisabled")
                
                // Note: We can't reliably get a callback here since the battery optimization
                // request flow doesn't provide a direct way to get the callback reference.
                // The calling code should re-check isBatteryOptimizationDisabled() after this.
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

        // Show rationale for USB permission
        showUsbPermissionRationaleDialog(device) { userAccepted ->
            if (userAccepted) {
                try {
                    val permissionIntent = android.app.PendingIntent.getBroadcast(
                        activity,
                        REQUEST_USB_PERMISSION,
                        Intent("com.topdon.topInfrared.USB_PERMISSION"),
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
                            // Fallback to general battery optimization settings
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                activity.startActivity(fallbackIntent)
                                // Since we can't get a result from the fallback, invoke the callback with false
                                Log.w(TAG, "Fallback battery optimization settings opened - treating as not granted")
                                callback(false)
                            } catch (fallbackException: Exception) {
                                Log.e(TAG, "Failed to open fallback battery optimization settings", fallbackException)
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
            // Not needed on older Android versions
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

    /**
     * Check if battery optimization is disabled for this app
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true // Not applicable on older versions
        }
    }

    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()

        CAMERA_PERMISSIONS.forEach { permission ->
            if (!isPermissionGranted(permission)) {
                missing.add(permission)
            }
        }

        val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }

        storagePermissions.forEach { permission ->
            if (!isPermissionGranted(permission)) {
                missing.add(permission)
            }
        }

        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }

        bluetoothPermissions.forEach { permission ->
            if (!isPermissionGranted(permission)) {
                missing.add(permission)
            }
        }

        FOREGROUND_SERVICE_PERMISSIONS.forEach { permission ->
            if (!isPermissionGranted(permission)) {
                missing.add(permission)
            }
        }

        NOTIFICATION_PERMISSIONS.forEach { permission ->
            if (!isPermissionGranted(permission)) {
                missing.add(permission)
            }
        }

        return missing.distinct()
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


    fun hasCameraPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.CAMERA)
    }

    fun hasAudioPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    }

    fun hasLocationPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Check if all permissions required for recording are granted
     */
    fun canStartRecording(): Boolean {
        return hasCameraPermission() && hasStoragePermissions()
    }

    /**
     * Check if all permissions required for Shimmer GSR connection are granted
     */
    fun canConnectToShimmer(): Boolean {
        return hasBluetoothPermissions() && hasLocationPermission()
    }

    /**
     * Check if notification permissions are granted (Android 13+)
     */
    fun canShowNotifications(): Boolean {
        return hasNotificationPermissions()
    }

    /**
     * Get status message for permission categories
     */
    fun getPermissionStatusMessage(): String {
        val status = mutableListOf<String>()
        
        if (!hasCameraPermission()) {
            status.add("Camera permission required for video recording")
        }
        if (!hasBluetoothPermissions()) {
            status.add("Bluetooth permissions required for GSR sensor")
        }
        if (!hasLocationPermission()) {
            status.add("Location permission required for Bluetooth scanning")
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
        // Group permissions logically for better user experience
        val permissionGroups = groupPermissionsLogically(missingPermissions)
        
        if (permissionGroups.isEmpty()) {
            permissionCallback?.invoke(true, emptyList())
            return
        }

        // Store all permission groups and track original request
        remainingPermissionGroups = permissionGroups.toMutableList()
        allRequestedPermissions = missingPermissions
        
        // Request the first group of permissions
        requestNextPermissionGroup()
    }
    
    private fun requestNextPermissionGroup() {
        if (remainingPermissionGroups.isEmpty()) {
            // All groups processed, check final results
            val stillMissingPermissions = allRequestedPermissions.filter { !isPermissionGranted(it) }
            if (stillMissingPermissions.isEmpty()) {
                Log.i(TAG, "All permission groups successfully granted")
                permissionCallback?.invoke(true, emptyList())
            } else {
                Log.w(TAG, "Some permissions still missing after all groups: ${stillMissingPermissions.joinToString(", ")}")
                handleDeniedPermissions(stillMissingPermissions)
                permissionCallback?.invoke(false, stillMissingPermissions)
            }
            return
        }
        
        val nextGroup = remainingPermissionGroups.removeAt(0)
        Log.i(TAG, "Requesting permission group: ${nextGroup.joinToString(", ")}")
        activity.requestPermissions(nextGroup.toTypedArray(), REQUEST_PERMISSIONS)
    }

    private fun groupPermissionsLogically(permissions: List<String>): List<List<String>> {
        val groups = mutableListOf<List<String>>()
        
        // Group 1: Camera and Audio (if recording with sound)
        val cameraGroup = permissions.filter { it in CAMERA_PERMISSIONS }
        if (cameraGroup.isNotEmpty()) {
            groups.add(cameraGroup)
        }

        // Group 2: Bluetooth and Location (needed together for BLE)
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS_ANDROID_12
        } else {
            BLUETOOTH_PERMISSIONS_LEGACY
        }
        
        val bluetoothGroup = permissions.filter { it in bluetoothPermissions }
        if (bluetoothGroup.isNotEmpty()) {
            groups.add(bluetoothGroup)
        }

        // Group 3: Storage permissions
        val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }
        
        val storageGroup = permissions.filter { it in storagePermissions }
        if (storageGroup.isNotEmpty()) {
            groups.add(storageGroup)
        }

        // Group 4: Notifications and Foreground Service
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
            appendLine()
            appendLine("The app will not function properly without these permissions.")
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
        // Check if any permissions were permanently denied
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
        val permissionNames = getPermissionNames(permanentlyDenied)

        val message = buildString {
            appendLine("Some permissions have been permanently denied:")
            appendLine()
            permissionNames.forEach { name ->
                appendLine("• $name")
            }
            appendLine()
            if (criticalPermissions.isNotEmpty()) {
                appendLine("These are critical permissions required for:")
                if (criticalPermissions.any { it in CAMERA_PERMISSIONS }) {
                    appendLine("• Video recording functionality")
                }
                if (criticalPermissions.any { it in getBluetoothPermissions() }) {
                    appendLine("• Shimmer GSR sensor connection")
                }
                if (criticalPermissions.any { it == Manifest.permission.ACCESS_FINE_LOCATION || it == Manifest.permission.ACCESS_COARSE_LOCATION }) {
                    appendLine("• Bluetooth device scanning")
                }
                appendLine()
                appendLine("The app cannot function properly without these permissions.")
                appendLine("Please enable them in Settings > Apps > IRCamera > Permissions")
            } else {
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
                    // Exit app if critical permissions are denied
                    activity.finish()
                }
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
                    // Retry permission request for critical permissions
                    ensureAll(permissionCallback ?: { _, _ -> })
                }
            }
            .setNegativeButton(if (criticalPermissions.isNotEmpty()) "Continue Limited" else null) { _, _ ->
                // Continue with limited functionality
            }
            .show()
    }

    private fun getCriticalPermissions(permissions: List<String>): List<String> {
        return permissions.filter { permission ->
            permission in CAMERA_PERMISSIONS ||
            permission == Manifest.permission.ACCESS_FINE_LOCATION ||
            permission == Manifest.permission.ACCESS_COARSE_LOCATION ||
            permission == Manifest.permission.BLUETOOTH_SCAN ||
            permission == Manifest.permission.BLUETOOTH_CONNECT
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
}
