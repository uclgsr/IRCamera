package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface WriteCharacteristicCallback : RequestFailedCallback {
    fun onCharacteristicWrite(request: Request?, value: ByteArray?)
}
