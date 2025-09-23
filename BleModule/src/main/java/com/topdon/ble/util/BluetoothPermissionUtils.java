package com.topdon.ble.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth permissions utility class
 * Provides methods for checking Bluetooth-related permissions
 */
public class BluetoothPermissionUtils {

    /**
     * Check if the app has all required Bluetooth permissions
     */
    public static boolean hasBluetoothPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires new permissions
            return hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT) &&
                    hasPermission(context, Manifest.permission.BLUETOOTH_SCAN);
        } else {
            // Pre-Android 12 permissions
            return hasPermission(context, Manifest.permission.BLUETOOTH) &&
                    hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN) &&
                    hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Check if the app has BLE scanning permissions
     */
    public static boolean hasBleScanningPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasPermission(context, Manifest.permission.BLUETOOTH_SCAN);
        } else {
            return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    /**
     * Get list of missing Bluetooth permissions
     */
    public static List<String> getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            // Pre-Android 12 permissions
            if (!hasPermission(context, Manifest.permission.BLUETOOTH)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH);
            }
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (!hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        return missingPermissions;
    }

    private static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}