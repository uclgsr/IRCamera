package com.mpdc4gsr.libunified.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication

/**
 * Permission utility class for handling Android permissions
 * Based on existing BaseApplication and permission management patterns
 */
object PermissionUtils {

    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions in a list are granted
     */
    fun areAllPermissionsGranted(context: Context, permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Check camera permission
     */
    fun hasCameraPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }

    /**
     * Check storage permissions based on Android version
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_IMAGES) &&
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 uses READ_EXTERNAL_STORAGE
            isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 10 and below
            isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE) &&
            isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check location permissions
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
               isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Check Bluetooth permissions based on Android version
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ uses new Bluetooth permissions
            isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN) &&
            isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT) &&
            hasLocationPermissions(context)
        } else {
            // Android 11 and below uses location permissions for Bluetooth
            hasLocationPermissions(context)
        }
    }

    /**
     * Check audio recording permission
     */
    fun hasAudioRecordPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Get required permissions for camera functionality
     */
    fun getCameraPermissions(): List<String> {
        return listOf(Manifest.permission.CAMERA)
    }

    /**
     * Get required permissions for storage functionality
     */
    fun getStoragePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Get required permissions for location functionality
     */
    fun getLocationPermissions(): List<String> {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Get required permissions for Bluetooth functionality
     */
    fun getBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            getLocationPermissions()
        }
    }

    /**
     * Get required permissions for audio recording
     */
    fun getAudioRecordPermissions(): List<String> {
        return listOf(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Get all required permissions for the app
     */
    fun getAllRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        permissions.addAll(getCameraPermissions())
        permissions.addAll(getStoragePermissions())
        permissions.addAll(getLocationPermissions())
        permissions.addAll(getBluetoothPermissions())
        permissions.addAll(getAudioRecordPermissions())
        return permissions.distinct()
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasCameraPermission(context) &&
               hasStoragePermissions(context) &&
               hasLocationPermissions(context) &&
               hasBluetoothPermissions(context) &&
               hasAudioRecordPermission(context)
    }

    /**
     * Get missing permissions from a list of required permissions
     */
    fun getMissingPermissions(context: Context, requiredPermissions: List<String>): List<String> {
        return requiredPermissions.filter { !isPermissionGranted(context, it) }
    }

    /**
     * Log permission status for debugging
     */
    fun logPermissionStatus(context: Context) {
        XLog.d("=== Permission Status ===")
        XLog.d("Camera: ${hasCameraPermission(context)}")
        XLog.d("Storage: ${hasStoragePermissions(context)}")
        XLog.d("Location: ${hasLocationPermissions(context)}")
        XLog.d("Bluetooth: ${hasBluetoothPermissions(context)}")
        XLog.d("Audio Record: ${hasAudioRecordPermission(context)}")
        XLog.d("All Required: ${hasAllRequiredPermissions(context)}")
    }

    /**
     * Check if permission was permanently denied
     */
    fun isPermissionPermanentlyDenied(context: Context, permission: String): Boolean {
        return !isPermissionGranted(context, permission) && 
               BaseApplication.instance.activitys.isNotEmpty()
    }

    /**
     * Get permission status summary
     */
    fun getPermissionSummary(context: Context): Map<String, Boolean> {
        return mapOf(
            "camera" to hasCameraPermission(context),
            "storage" to hasStoragePermissions(context),
            "location" to hasLocationPermissions(context),
            "bluetooth" to hasBluetoothPermissions(context),
            "audio" to hasAudioRecordPermission(context)
        )
    }
}