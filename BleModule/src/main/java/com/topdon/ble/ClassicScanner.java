package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.BluetoothPermissionUtils;


class ClassicScanner extends AbstractScanner {
    private static final String TAG = "ClassicScanner";
    private boolean stopQuietly = false;

    ClassicScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    protected void performStartScan() {
        Context context = EasyBLE.getInstance().getContext();
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for startDiscovery()");
            handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, 
                "Missing Bluetooth scan permission");
            return;
        }
        
        try {
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in startDiscovery(): " + e.getMessage());
            handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, 
                "Bluetooth permission denied: " + e.getMessage());
        }
    }

    @Override
    protected void performStopScan() {
        Context context = EasyBLE.getInstance().getContext();
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for cancelDiscovery()");
            return;
        }
        
        try {
            bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in cancelDiscovery(): " + e.getMessage());
        }
    }

    @Override
    void setScanning(boolean scanning) {
        super.setScanning(scanning);
        if (scanning) {
            handleScanCallback(true, null, false, -1, "");
        } else if (!stopQuietly) {
            handleScanCallback(false, null, false, -1, "");
        } else {
            stopQuietly = false;
        }
    }

    @Override
    public void stopScan(boolean quietly) {
        if (isScanning()) {
            stopQuietly = quietly;
        }
        super.stopScan(quietly);
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.CLASSIC;
    }
}
