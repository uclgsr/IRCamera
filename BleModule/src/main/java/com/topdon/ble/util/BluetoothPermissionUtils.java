package com.topdon.ble.util;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothPermissionUtils {
    private static final String TAG = "BluetoothPermissionUtils";

    public static boolean hasBluetoothPermissions(Context context) {
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {

            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static boolean hasBleScanningPermissions(Context context) {
        if (context == null) return false;

        if (!hasBluetoothPermissions(context)) {
            return false;
        }

        boolean hasLocationPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {


            hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {

            hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return hasLocationPermission;
    }

    public static List<String> getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();

        if (context == null) return missingPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        } else {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        return missingPermissions;
    }

    public static String getPermissionRationale(String permission) {
        switch (permission) {
            case Manifest.permission.BLUETOOTH_SCAN:
                return "Required to scan for nearby Shimmer GSR devices";
            case Manifest.permission.BLUETOOTH_CONNECT:
                return "Required to connect to and communicate with Shimmer GSR devices";
            case Manifest.permission.BLUETOOTH:
                return "Required for Bluetooth communication with Shimmer GSR devices";
            case Manifest.permission.BLUETOOTH_ADMIN:
                return "Required for advanced Bluetooth operations with Shimmer GSR devices";
            case Manifest.permission.ACCESS_COARSE_LOCATION:
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Required for Bluetooth Low Energy device scanning (Android system requirement)";
            default:
                return "Required for Shimmer GSR device functionality";
        }
    }

    public static boolean hasBluetoothConnectPermission(Context context) {
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static boolean hasBluetoothScanPermission(Context context) {
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }

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
