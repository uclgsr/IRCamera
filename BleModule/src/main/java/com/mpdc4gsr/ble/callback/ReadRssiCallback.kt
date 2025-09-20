package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface ReadRssiCallback : RequestFailedCallback {
    fun onRssiRead(request: Request?, rssi: Int)
}
