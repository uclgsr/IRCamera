package com.mpdc4gsr.ble.core

/**
 * Interface for BLE scan operations
 */
interface ScanListener {
    fun onScanStart()
    fun onScanStop()
    fun onScanResult(device: Device, rssi: Int, data: ByteArray)
    fun onScanFailed(reason: Int)
    fun onScanComplete()
    fun onScanError(errorCode: Int, errorMessage: String)
}