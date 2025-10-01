package com.topdon.ble.callback;

import android.Manifest;

import com.topdon.ble.Device;

/**
 *
 * <p>
 * date: 2021/8/12 09:17
 * author: bichuanfeng
 */
public interface ScanListener {
    /**
     * 。 {@link Manifest.permission#ACCESS_COARSE_LOCATION}  {@link Manifest.permission#ACCESS_FINE_LOCATION}
     */
    int ERROR_LACK_LOCATION_PERMISSION = 0;
    /**
     *
     */
    int ERROR_LOCATION_SERVICE_CLOSED = 1;
    /**
     *
     */
    int ERROR_SCAN_FAILED = 2;

    /**
     *
     */
    void onScanStart();

    /**
     *
     */
    void onScanStop();

    /**
     * BLE
     *
     * @deprecated {@link #onScanResult(Device, boolean)}，，
     */
    @Deprecated
    default void onScanResult(Device device) {
    }

    /**
     * BLE
     *
     * @param device
     * @param isConnectedBySys
     */
    void onScanResult(Device device, boolean isConnectedBySys);

    /**
     *
     *
     * @param errorCode {@link #ERROR_LACK_LOCATION_PERMISSION}, {@link #ERROR_LOCATION_SERVICE_CLOSED}
     */
    void onScanError(int errorCode, String errorMsg);
}
