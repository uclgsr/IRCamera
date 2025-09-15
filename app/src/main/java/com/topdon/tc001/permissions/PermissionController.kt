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
    }

    private val isInitialized = AtomicBoolean(false)
    private var permissionCallback: ((Boolean, List<String>) -> Unit)? = null
    private var usbPermissionCallback: ((Boolean, UsbDevice?) -> Unit)? = null

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
                hasNotificationPermissions()
    }

    fun requestAllPermissions(callback: (Boolean, List<String>) -> Unit) {
        permissionCallback = callback

        val missingPermissions = getMissingPermissions()
        if (missingPermissions.isEmpty()) {
            callback(true, emptyList())
            return
        }

        Log.i(TAG, "Requesting ${missingPermissions.size} missing permissions")

        showPermissionExplanationDialog(missingPermissions) { userAccepted ->
            if (userAccepted) {
                activity.requestPermissions(missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
            } else {
                callback(false, missingPermissions)
            }
        }
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

                val allGranted = deniedPermissions.isEmpty()

                if (allGranted) {
                    Log.i(TAG, "All requested permissions granted")
                    permissionCallback?.invoke(true, emptyList())
                } else {
                    Log.w(TAG, "Some permissions denied: ${deniedPermissions.joinToString(", ")}")
                    handleDeniedPermissions(deniedPermissions)
                    permissionCallback?.invoke(false, deniedPermissions)
                }
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
    }

    fun requestBatteryOptimizationExemption(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = activity.packageName
            val powerManager =
                activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.i(TAG, "Requesting battery optimization exemption")

                AlertDialog.Builder(activity)
                    .setTitle("Battery Optimization")
                    .setMessage("For reliable multi-sensor recording, please disable battery optimization for this app. This ensures continuous recording operation.")
                    .setPositiveButton("Allow") { _, _ ->
                        try {
                            val intent =
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            activity.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open battery optimization settings", e)
                            callback(false)
                        }
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        callback(false)
                    }
                    .show()
            } else {
                Log.i(TAG, "Battery optimization already disabled")
                callback(true)
            }
        } else {

            callback(true)
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

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
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
        val criticalPermissions = deniedPermissions.filter { permission ->
            permission in CAMERA_PERMISSIONS ||
                    permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                    permission == Manifest.permission.ACCESS_COARSE_LOCATION
        }

        if (criticalPermissions.isNotEmpty()) {
            val message = buildString {
                appendLine("Some critical permissions were denied:")
                appendLine()
                getPermissionNames(criticalPermissions).forEach { name ->
                    appendLine("• $name")
                }
                appendLine()
                appendLine("Without these permissions, the app cannot function properly.")
                appendLine("Please grant these permissions in Settings > Apps > IRCamera > Permissions")
            }

            AlertDialog.Builder(activity)
                .setTitle("Critical Permissions Denied")
                .setMessage(message)
                .setPositiveButton("Open Settings") { _, _ ->
                    openAppSettings()
                }
                .setNegativeButton("Continue Limited") { _, _ ->

                }
                .show()
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
