package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.BluetoothPermissionUtils;
import com.topdon.ble.util.Logger;


class LeScanner extends AbstractScanner {
    private static final String TAG = "LeScanner";
    private BluetoothLeScanner bleScanner;

    LeScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    private BluetoothLeScanner getLeScanner() {
        if (bleScanner == null) {
            //如果蓝牙未开启的时候，获取到是null
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return bleScanner;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            parseScanResult(result.getDevice(), result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            handleScanCallback(false, null, false, ScanListener.ERROR_SCAN_FAILED, "onScanFailed. errorCode = " + errorCode);
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "onScanFailed. errorCode = " + errorCode);
            stopScan(true);
        }
    };

    @Override
    protected boolean isReady() {
        return getLeScanner() != null;
    }

    @Override
    protected void performStartScan() {
        Context context = EasyBLE.getInstance().getContext();
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for startScan()");
            handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, 
                "Missing Bluetooth scan permission");
            return;
        }
        
        ScanSettings settings;
        if (configuration.scanSettings == null) {
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
        } else {
            settings = configuration.scanSettings;
        }
        
        try {
            bleScanner.startScan(configuration.filters, settings, scanCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in startScan(): " + e.getMessage());
            handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, 
                "Bluetooth permission denied: " + e.getMessage());
        }
    }

    @Override
    protected void performStopScan() {
        if (bleScanner != null) {
            Context context = EasyBLE.getInstance().getContext();
            if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for stopScan()");
                return;
            }
            
            try {
                bleScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in stopScan(): " + e.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LE;
    }
}
