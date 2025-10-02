package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.topdon.ble.util.Logger;



class LegacyScanner extends AbstractScanner implements BluetoothAdapter.LeScanCallback {

    LegacyScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    protected void performStartScan() {
        try {
            bluetoothAdapter.startLeScan(this);
        } catch (SecurityException e) {
            logger.log(android.util.Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permission for legacy scan: " + e.getMessage());
        }
    }

    @Override
    protected void performStopScan() {
        try {
            bluetoothAdapter.stopLeScan(this);
        } catch (SecurityException e) {
            logger.log(android.util.Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permission to stop legacy scan: " + e.getMessage());
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        parseScanResult(device, false, null, rssi, scanRecord);
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LEGACY;
    }
}
