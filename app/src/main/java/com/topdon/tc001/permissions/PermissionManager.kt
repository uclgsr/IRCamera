package com.topdon.tc001.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity

/**
 * Permission Manager for MVP - focuses on core functionality with proper constants
 */
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

    /**
     * Request camera permissions - MVP version with proper error handling
     */
    suspend fun requestCameraPermissions(): Boolean {
        val cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val missingPermissions = cameraPermissions.filter { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "Camera permissions already granted")
            return true
        }
        
        Log.i(TAG, "Requesting camera permissions")
        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CAMERA_PERMISSIONS)
        
        // For MVP: Check permissions immediately after request
        // In production, this would use a proper callback mechanism
        return missingPermissions.all { 
            ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request bluetooth permissions - MVP version with proper error handling
     */
    suspend fun requestBluetoothPermissions(): Boolean {
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
            return true
        }
        
        Log.i(TAG, "Requesting bluetooth permissions")
        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        
        // For MVP: Check permissions immediately after request
        // In production, this would use a proper callback mechanism
        return missingPermissions.all { 
            ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request storage permissions - MVP version
     */
    suspend fun requestStoragePermissions(): Boolean {
        // Storage permissions not critical for MVP - most data is app-local
        return true
    }
}