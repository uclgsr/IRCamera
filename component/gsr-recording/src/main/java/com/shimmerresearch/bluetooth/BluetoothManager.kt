package com.shimmerresearch.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Bluetooth management utilities for Shimmer devices
 * Based on official Shimmer Android API Bluetooth handling
 *
 * Provides device discovery, pairing, and connection management
 */
class BluetoothManager(private val context: Context) {
    companion object {
        private const val TAG = "ShimmerBluetoothManager"

        // Shimmer device name patterns
        val SHIMMER_DEVICE_PATTERNS =
            arrayOf(
                "Shimmer3",
                "Shimmer",
                "shimmer",
                "GSR",
            )

        // Connection states
        const val STATE_NONE = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_LISTEN = 3
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothManager.adapter
    }
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private val listeners = mutableListOf<BluetoothConnectionListener>()

    interface BluetoothConnectionListener {
        fun onDeviceDiscovered(device: BluetoothDevice)

        fun onConnectionStateChanged(
            device: BluetoothDevice,
            state: Int,
        )

        fun onConnectionError(
            device: BluetoothDevice,
            error: String,
        )
    }

    fun addListener(listener: BluetoothConnectionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BluetoothConnectionListener) {
        listeners.remove(listener)
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        val adapter = bluetoothAdapter
        return adapter != null && adapter.isEnabled
    }

    /**
     * Check if required Bluetooth permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        // Check for Android 12+ (API 31) specific permissions
        val connectPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // For older versions, BLUETOOTH_CONNECT doesn't exist, so we check basic permissions
                true
            }

        val locationPermission =
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        return connectPermission && locationPermission
    }

    /**
     * Get list of bonded (paired) Shimmer devices
     */
    fun getBondedShimmerDevices(): List<BluetoothDevice> {
        if (!isBluetoothAvailable() || !hasRequiredPermissions()) {
            Log.w(TAG, "Bluetooth not available or permissions missing")
            return emptyList()
        }

        return try {
            bluetoothAdapter?.bondedDevices?.filter { device ->
                isShimmerDevice(device)
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting bonded devices", e)
            emptyList()
        }
    }

    /**
     * Check if a device is a Shimmer device based on name patterns
     */
    fun isShimmerDevice(device: BluetoothDevice): Boolean {
        val deviceName =
            try {
                if (hasRequiredPermissions()) {
                    device.name
                } else {
                    null
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot get device name due to permissions", e)
                null
            }

        return deviceName?.let { name ->
            SHIMMER_DEVICE_PATTERNS.any { pattern ->
                name.contains(pattern, ignoreCase = true)
            }
        } ?: false
    }

    /**
     * Find the first available Shimmer device
     */
    fun findFirstShimmerDevice(): BluetoothDevice? {
        return getBondedShimmerDevices().firstOrNull()
    }

    /**
     * Find Shimmer device by MAC address
     */
    fun findShimmerDeviceByAddress(address: String): BluetoothDevice? {
        return try {
            bluetoothAdapter?.getRemoteDevice(address)?.takeIf { device ->
                isShimmerDevice(device)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error finding device by address: $address", e)
            null
        }
    }

    /**
     * Get device information string
     */
    fun getDeviceInfo(device: BluetoothDevice): String {
        return try {
            if (hasRequiredPermissions()) {
                "Device: ${device.name ?: "Unknown"} (${device.address})"
            } else {
                "Device: ${device.address} (name requires permissions)"
            }
        } catch (e: SecurityException) {
            "Device: ${device.address} (info requires permissions)"
        }
    }

    /**
     * Check device connection state
     */
    fun getConnectionState(device: BluetoothDevice): Int {
        return try {
            // Check permissions before accessing device properties
            if (!hasRequiredPermissions()) {
                Log.w(TAG, "Required permissions not available for connection state check")
                return STATE_NONE
            }

            // This would normally check the actual connection state
            // For now, return based on bonding state
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> STATE_CONNECTED
                BluetoothDevice.BOND_BONDING -> STATE_CONNECTING
                else -> STATE_NONE
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception checking connection state", e)
            STATE_NONE
        }
    }

    /**
     * Validate device for GSR recording
     */
    fun validateShimmerDevice(device: BluetoothDevice): ValidationResult {
        val issues = mutableListOf<String>()

        if (!isBluetoothAvailable()) {
            issues.add("Bluetooth not available")
        }

        if (!hasRequiredPermissions()) {
            issues.add("Missing Bluetooth permissions")
        }

        if (!isShimmerDevice(device)) {
            issues.add("Device is not recognized as Shimmer device")
        }

        try {
            // Check permissions before accessing device properties
            if (hasRequiredPermissions() && device.bondState != BluetoothDevice.BOND_BONDED) {
                issues.add("Device is not paired")
            }
        } catch (e: SecurityException) {
            issues.add("Cannot check device pairing status due to permission restrictions")
        }

        val isValid = issues.isEmpty()
        val message =
            if (isValid) {
                "Device validation successful"
            } else {
                "Device validation failed: ${issues.joinToString(", ")}"
            }

        return ValidationResult(isValid, message, issues)
    }

    /**
     * Device validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val issues: List<String>,
    )

    /**
     * Get Bluetooth adapter information
     */
    fun getBluetoothAdapterInfo(): BluetoothAdapterInfo {
        return BluetoothAdapterInfo(
            isAvailable = bluetoothAdapter != null,
            isEnabled = bluetoothAdapter?.isEnabled ?: false,
            address =
                try {
                    if (hasRequiredPermissions()) {
                        bluetoothAdapter?.address ?: "Unknown"
                    } else {
                        "Permission required"
                    }
                } catch (e: SecurityException) {
                    "Permission required"
                },
            name =
                try {
                    if (hasRequiredPermissions()) {
                        bluetoothAdapter?.name ?: "Unknown"
                    } else {
                        "Permission required"
                    }
                } catch (e: SecurityException) {
                    "Permission required"
                },
        )
    }

    data class BluetoothAdapterInfo(
        val isAvailable: Boolean,
        val isEnabled: Boolean,
        val address: String,
        val name: String,
    )

    /**
     * Clean up resources
     */
    fun cleanup() {
        listeners.clear()
        discoveredDevices.clear()
    }
}
