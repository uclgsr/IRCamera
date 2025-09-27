package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import com.topdon.ble.util.Logger;


/**
 * date: 2020/5/9 16:20
 * author: bichuanfeng
 */
class ClassicScanner extends AbstractScanner {
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
        try {
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            logger.log(android.util.Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permission for classic scan: " + e.getMessage());
        }
    }

    @Override
    protected void performStopScan() {
        try {
            bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            logger.log(android.util.Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permission to stop classic scan: " + e.getMessage());
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
