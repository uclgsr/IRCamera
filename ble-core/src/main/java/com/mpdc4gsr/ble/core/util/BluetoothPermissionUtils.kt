package com.mpdc4gsr.ble.core.util

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

object BluetoothPermissionUtils {
    private const val TAG = "BluetoothPermissionUtils"

    fun hasBluetoothPermissions(context: Context?): Boolean {
        if (context == null) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBleScanningPermissions(context: Context?): Boolean {
        if (context == null) return false

        if (!hasBluetoothPermissions(context)) {
            return false
        }

        var hasLocationPermission = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }

        return hasLocationPermission
    }

    fun getMissingPermissions(context: Context?): MutableList<String?> {
        val missingPermissions: MutableList<String?> = ArrayList<String?>()

        if (context == null) return missingPermissions

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        return missingPermissions
    }

    fun getPermissionRationale(permission: String): String {
        when (permission) {
            Manifest.permission.BLUETOOTH_SCAN -> return "Required to scan for nearby Shimmer GSR devices"
            Manifest.permission.BLUETOOTH_CONNECT -> return "Required to connect to and communicate with Shimmer GSR devices"
            Manifest.permission.BLUETOOTH -> return "Required for Bluetooth communication with Shimmer GSR devices"
            Manifest.permission.BLUETOOTH_ADMIN -> return "Required for advanced Bluetooth operations with Shimmer GSR devices"
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION -> return "Required for Bluetooth Low Energy device scanning (Android system requirement)"
            else -> return "Required for Shimmer GSR device functionality"
        }
    }

    fun hasBluetoothConnectPermission(context: Context?): Boolean {
        if (context == null) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBluetoothScanPermission(context: Context?): Boolean {
        if (context == null) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getDeviceName(context: Context?, device: BluetoothDevice?): String {
        if (device == null) return "Unknown Device"

        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for device name access")
            return "Permission Required"
        }

        try {
            val name = device.getName()
            return if (name != null) name else "Unknown Device"
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting device name: " + e.message)
            return "Permission Denied"
        }
    }

    fun getDeviceAddress(context: Context?, device: BluetoothDevice?): String? {
        if (device == null) return "Unknown Address"

        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for device address access")
            return "Permission Required"
        }

        try {
            return device.getAddress()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting device address: " + e.message)
            return "Permission Denied"
        }
    }

    fun getDeviceType(context: Context?, device: BluetoothDevice?): Int {
        if (device == null) return BluetoothDevice.DEVICE_TYPE_UNKNOWN

        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for device type access")
            return BluetoothDevice.DEVICE_TYPE_UNKNOWN
        }

        try {
            return device.getType()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting device type: " + e.message)
            return BluetoothDevice.DEVICE_TYPE_UNKNOWN
        }
    }

    fun getDeviceBondState(context: Context?, device: BluetoothDevice?): Int {
        if (device == null) return BluetoothDevice.BOND_NONE

        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for bond state access")
            return BluetoothDevice.BOND_NONE
        }

        try {
            return device.getBondState()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting bond state: " + e.message)
            return BluetoothDevice.BOND_NONE
        }
    }
}
