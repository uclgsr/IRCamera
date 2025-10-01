package com.topdon.ble;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;

/**
 *
 * <p>
 * date: 2021/8/12 15:31
 * author: bichuanfeng
 */
public class ScanConfiguration {
    int scanPeriodMillis = 10000;
    boolean acceptSysConnectedDevice;
    ScanSettings scanSettings;
    boolean onlyAcceptBleDevice;
    int rssiLowLimit = -120;
    List<ScanFilter> filters;

    public int getScanPeriodMillis() {
        return scanPeriodMillis;
    }

    /**
     *
     *
     * @param scanPeriodMillis
     */
    public ScanConfiguration setScanPeriodMillis(int scanPeriodMillis) {
        //1
        if (scanPeriodMillis >= 1000) {
            this.scanPeriodMillis = scanPeriodMillis;
        }
        return this;
    }

    public boolean isAcceptSysConnectedDevice() {
        return acceptSysConnectedDevice;
    }

    /**
     * （）
     */
    public ScanConfiguration setAcceptSysConnectedDevice(boolean acceptSysConnectedDevice) {
        this.acceptSysConnectedDevice = acceptSysConnectedDevice;
        return this;
    }

    public ScanSettings getScanSettings() {
        return scanSettings;
    }

    /**
     * {@link BluetoothLeScanner}
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScanConfiguration setScanSettings(ScanSettings scanSettings) {
        Inspector.requireNonNull(scanSettings, "scanSettings can't be null");
        this.scanSettings = scanSettings;
        return this;
    }

    public boolean isOnlyAcceptBleDevice() {
        return onlyAcceptBleDevice;
    }

    /**
     * ble
     */
    public ScanConfiguration setOnlyAcceptBleDevice(boolean onlyAcceptBleDevice) {
        this.onlyAcceptBleDevice = onlyAcceptBleDevice;
        return this;
    }

    public int getRssiLowLimit() {
        return rssiLowLimit;
    }

    /**
     *
     */
    public ScanConfiguration setRssiLowLimit(int rssiLowLimit) {
        this.rssiLowLimit = rssiLowLimit;
        return this;
    }

    public List<ScanFilter> getFilters() {
        return filters;
    }

    /**
     * 。{@link BluetoothLeScanner#startScan(List, ScanSettings, ScanCallback)}
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScanConfiguration setFilters(List<ScanFilter> filters) {
        this.filters = filters;
        return this;
    }
}
