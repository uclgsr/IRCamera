package com.mpdc4gsr.ble

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.annotation.RequiresApi

class ScanConfiguration {
    var scanPeriodMillis: Int = 10000
    var isAcceptSysConnectedDevice: Boolean = false
    var scanSettings: ScanSettings? = null
    var isOnlyAcceptBleDevice: Boolean = false
    var rssiLowLimit: Int = -120
    var filters: MutableList<ScanFilter?>? = null

    fun setScanPeriodMillis(scanPeriodMillis: Int): ScanConfiguration {
        if (scanPeriodMillis >= 1000) {
            this.scanPeriodMillis = scanPeriodMillis
        }
        return this
    }

    fun setAcceptSysConnectedDevice(acceptSysConnectedDevice: Boolean): ScanConfiguration {
        this.isAcceptSysConnectedDevice = acceptSysConnectedDevice
        return this
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setScanSettings(scanSettings: ScanSettings?): ScanConfiguration {
        Inspector.requireNonNull<ScanSettings?>(scanSettings, "scanSettings can't be null")
        this.scanSettings = scanSettings
        return this
    }

    fun setOnlyAcceptBleDevice(onlyAcceptBleDevice: Boolean): ScanConfiguration {
        this.isOnlyAcceptBleDevice = onlyAcceptBleDevice
        return this
    }

    fun setRssiLowLimit(rssiLowLimit: Int): ScanConfiguration {
        this.rssiLowLimit = rssiLowLimit
        return this
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setFilters(filters: MutableList<ScanFilter?>?): ScanConfiguration {
        this.filters = filters
        return this
    }
}
