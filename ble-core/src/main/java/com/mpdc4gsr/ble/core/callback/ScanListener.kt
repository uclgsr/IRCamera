package com.mpdc4gsr.ble.core.callback

import com.mpdc4gsr.ble.core.Device

interface ScanListener {
    fun onScanResult(device: Device, rssi: Int, data: ByteArray)
    fun onScanFailed(reason: Int)
    fun onScanComplete()
}