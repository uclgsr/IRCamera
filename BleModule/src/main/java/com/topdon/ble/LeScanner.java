package com.topdon.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.Logger;

class LeScanner extends AbstractScanner {
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
    private BluetoothLeScanner bleScanner;

    LeScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    private BluetoothLeScanner getLeScanner() {
        if (bleScanner == null) {
            //，null
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return bleScanner;
    }

    @Override
    protected boolean isReady() {
        return getLeScanner() != null;
    }

    @Override
    protected void performStartScan() {
        ScanSettings settings;
        if (configuration.scanSettings == null) {
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
        } else {
            settings = configuration.scanSettings;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Context context = getEasyBle().getContext();
            if (context == null || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing BLUETOOTH_SCAN permission for LE scan");
                return;
            }
        }
        
        try {
            bleScanner.startScan(configuration.filters, settings, scanCallback);
        } catch (SecurityException e) {
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permission to start LE scan: " + e.getMessage());
        }
    }

    @Override
    protected void performStopScan() {
        if (bleScanner != null) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                logger.log(android.util.Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permission to stop LE scan: " + e.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LE;
    }
}
