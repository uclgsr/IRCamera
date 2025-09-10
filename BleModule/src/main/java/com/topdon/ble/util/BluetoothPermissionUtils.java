package com.topdon.ble.util;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Utility class for safe Bluetooth operations with proper permission handling
 * Provides wrapper methods that check permissions before calling Bluetooth APIs
 */
public class BluetoothPermissionUtils {
    private static final String TAG = "BluetoothPermissionUtils";
    
    /**
     * Check if the app has required Bluetooth permissions for the current Android version
     * @param context Application context
     * @return true if permissions are granted, false otherwise
     */
    public static boolean hasBluetoothPermissions(Context context) {
        if (context == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ requires new Bluetooth permissions
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            // API 30 and below use legacy permissions
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Check if the app has BLUETOOTH_CONNECT permission (required for device property access)
     * @param context Application context
     * @return true if permission is granted, false otherwise
     */
    public static boolean hasBluetoothConnectPermission(Context context) {
        if (context == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Check if the app has BLUETOOTH_SCAN permission (required for scanning operations)
     * @param context Application context
     * @return true if permission is granted, false otherwise
     */
    public static boolean hasBluetoothScanPermission(Context context) {
        if (context == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Safely get device name with permission check
     * @param context Application context
     * @param device Bluetooth device
     * @return Device name or fallback string if permission denied
     */
    public static String getDeviceName(Context context, BluetoothDevice device) {
        if (device == null) return "Unknown Device";
        
        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for device name access");
            return "Permission Required";
        }
        
        try {
            String name = device.getName();
            return name != null ? name : "Unknown Device";
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException getting device name: " + e.getMessage());
            return "Permission Denied";
        }
    }
    
    /**
     * Safely get device address with permission check
     * @param context Application context
     * @param device Bluetooth device
     * @return Device address or fallback string if permission denied
     */
    public static String getDeviceAddress(Context context, BluetoothDevice device) {
        if (device == null) return "Unknown Address";
        
        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for device address access");
            return "Permission Required";
        }
        
        try {
            return device.getAddress();
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException getting device address: " + e.getMessage());
            return "Permission Denied";
        }
    }
    
    /**
     * Safely get device type with permission check
     * @param context Application context
     * @param device Bluetooth device
     * @return Device type or DEVICE_TYPE_UNKNOWN if permission denied
     */
    public static int getDeviceType(Context context, BluetoothDevice device) {
        if (device == null) return BluetoothDevice.DEVICE_TYPE_UNKNOWN;
        
        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for device type access");
            return BluetoothDevice.DEVICE_TYPE_UNKNOWN;
        }
        
        try {
            return device.getType();
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException getting device type: " + e.getMessage());
            return BluetoothDevice.DEVICE_TYPE_UNKNOWN;
        }
    }
    
    /**
     * Safely get device bond state with permission check
     * @param context Application context
     * @param device Bluetooth device
     * @return Bond state or BOND_NONE if permission denied
     */
    public static int getDeviceBondState(Context context, BluetoothDevice device) {
        if (device == null) return BluetoothDevice.BOND_NONE;
        
        if (!hasBluetoothConnectPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for bond state access");
            return BluetoothDevice.BOND_NONE;
        }
        
        try {
            return device.getBondState();
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException getting bond state: " + e.getMessage());
            return BluetoothDevice.BOND_NONE;
        }
    }
}