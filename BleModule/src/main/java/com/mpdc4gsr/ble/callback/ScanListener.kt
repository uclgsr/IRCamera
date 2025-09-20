package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Device

interface ScanListener {
    fun onScanStart()

    fun onScanStop()

    @Deprecated("")
    fun onScanResult(device: Device?) {
    }

    fun onScanResult(device: Device?, isConnectedBySys: Boolean)

    fun onScanError(errorCode: Int, errorMsg: String?)

    companion object {
        const val ERROR_LACK_LOCATION_PERMISSION: Int = 0

        const val ERROR_LOCATION_SERVICE_CLOSED: Int = 1

        const val ERROR_SCAN_FAILED: Int = 2

        const val ERROR_LACK_BLUETOOTH_PERMISSION: Int = 3
    }
}
