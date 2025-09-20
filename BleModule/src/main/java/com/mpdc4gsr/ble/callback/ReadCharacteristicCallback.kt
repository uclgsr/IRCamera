package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface ReadCharacteristicCallback : RequestFailedCallback {
    fun onCharacteristicRead(request: Request?, value: ByteArray?)
}
