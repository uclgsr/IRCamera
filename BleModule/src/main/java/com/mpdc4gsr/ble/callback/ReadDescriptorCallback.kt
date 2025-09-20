package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface ReadDescriptorCallback : RequestFailedCallback {
    fun onDescriptorRead(request: Request?, value: ByteArray?)
}
