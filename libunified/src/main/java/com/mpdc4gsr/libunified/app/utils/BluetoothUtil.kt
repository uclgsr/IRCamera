package com.mpdc4gsr.libunified.app.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.tools.PermissionTool

/**
 * Bluetooth utility class for handling Bluetooth operations
 * Based on existing PermissionTool and DeviceConfig integrations
 */
object BluetoothUtil {

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    /**
     * Check if Bluetooth is available on the device
     */
    fun isBluetoothSupported(context: Context): Boolean {
        return getBluetoothAdapter(context) != null
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        val adapter = getBluetoothAdapter(context)
        return adapter?.isEnabled == true
    }

    /**
     * Check if device has Bluetooth permissions
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return PermissionTool.hasBtPermission(context)
    }

    /**
     * Request Bluetooth permissions
     */
    fun requestBluetoothPermissions(
        context: Context,
        callback: (Boolean) -> Unit
    ) {
        PermissionTool.requestBluetooth(context, true, object : PermissionTool.Callback {
            override fun onResult(result: Boolean) {
                callback(result)
            }
            
            override fun onNever(isJump: Boolean) {
                XLog.w("Bluetooth permissions permanently denied, isJump: $isJump")
                callback(false)
            }
        })
    }

    /**
     * Check if scanning is allowed based on permissions and Bluetooth state
     */
    fun canStartScanning(context: Context): Boolean {
        if (!isBluetoothSupported(context)) {
            XLog.w("Bluetooth not supported on this device")
            return false
        }
        
        if (!isBluetoothEnabled(context)) {
            XLog.w("Bluetooth is not enabled")
            return false
        }
        
        if (!hasBluetoothPermissions(context)) {
            XLog.w("Bluetooth permissions not granted")
            return false
        }
        
        return true
    }

    /**
     * Get device compatibility info
     */
    fun getDeviceInfo(): Map<String, Any> {
        return mapOf(
            "tcLiteVendorId" to DeviceConfig.TCLITE_VENDOR_ID,
            "tcLiteProductId" to DeviceConfig.TCLITE_PRODUCT_ID,
            "topdonVendorId" to DeviceConfig.TOPDON_VENDOR_ID,
            "topdonProductId" to DeviceConfig.TOPDON_PRODUCT_ID
        )
    }

    /**
     * Check for required permissions based on Android version
     */
    private fun checkBluetoothPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get required Bluetooth permissions for current Android version
     */
    fun getRequiredBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Enable Bluetooth (requires user interaction)
     */
    fun enableBluetooth(context: Context): Boolean {
        val adapter = getBluetoothAdapter(context)
        return if (adapter != null && !adapter.isEnabled) {
            // Note: Direct enabling is deprecated, should use Intent to request user
            XLog.i("Bluetooth needs to be enabled by user")
            false
        } else {
            true
        }
    }

    /**
     * Get Bluetooth adapter name
     */
    fun getBluetoothAdapterName(context: Context): String? {
        val adapter = getBluetoothAdapter(context)
        return try {
            if (hasBluetoothPermissions(context)) {
                adapter?.name
            } else {
                XLog.w("No permission to get Bluetooth adapter name")
                null
            }
        } catch (e: SecurityException) {
            XLog.w("SecurityException getting Bluetooth adapter name: ${e.message}")
            null
        }
    }
}