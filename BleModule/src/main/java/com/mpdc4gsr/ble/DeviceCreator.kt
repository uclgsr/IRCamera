package com.mpdc4gsr.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

interface DeviceCreator {
    fun create(device: BluetoothDevice?, scanResult: ScanResult?): Device?
}
