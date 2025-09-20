package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface RequestFailedCallback : RequestCallback {
    fun onRequestFailed(request: Request?, failType: Int, value: Any?)
}
