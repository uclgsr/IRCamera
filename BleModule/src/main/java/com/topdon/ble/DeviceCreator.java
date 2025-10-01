package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import androidx.annotation.Nullable;


/**
 * {@link Device}，BLE，{@link Device}
 * <p>
 * date: 2021/8/12 00:07
 * author: bichuanfeng
 */
public interface DeviceCreator {
    /**
     * ，{@link Device}，
     *
     * @param scanResult
     * @return ，null，，，{@link Device}，
     */
    @Nullable
    Device create(BluetoothDevice device, ScanResult scanResult);
}
