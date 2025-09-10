package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.BluetoothPermissionUtils;


/**
 * date: 2019/10/1 15:13
 * author: bichuanfeng
 */
class LegacyScanner extends AbstractScanner implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "LegacyScanner";
    
    LegacyScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
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
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for startLeScan()");
            handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, 
                "Missing Bluetooth scan permission");
            return;
        }
        
        try {
            bluetoothAdapter.startLeScan(this);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in startLeScan(): " + e.getMessage());
            handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, 
                "Bluetooth permission denied: " + e.getMessage());
        }
    }

    @Override
    protected void performStopScan() {
        Context context = EasyBLE.getInstance().getContext();
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for stopLeScan()");
            return;
        }
        
        try {
            bluetoothAdapter.stopLeScan(this);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in stopLeScan(): " + e.getMessage());
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
