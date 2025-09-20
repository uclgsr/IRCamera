package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface MtuChangeCallback : RequestFailedCallback {
    fun onMtuChanged(request: Request?, mtu: Int)
}
