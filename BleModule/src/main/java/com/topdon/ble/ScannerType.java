package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;

/**
 * date: 2019/12/2 11:51
 * author: bichuanfeng
 */
public enum ScannerType {
    /**
     * {@link BluetoothLeScanner}
     */
    LE,
    /**
     * {@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)}
     */
    LEGACY,
    /**
     * {@link BluetoothAdapter#startDiscovery()}，，
     */
    CLASSIC
}
