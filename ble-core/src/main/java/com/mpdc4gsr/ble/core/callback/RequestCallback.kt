package com.mpdc4gsr.ble.core.callback

import com.mpdc4gsr.ble.core.Request

interface RequestCallback {
    fun onSuccess(request: Request, data: ByteArray?)
    fun onFailed(request: Request, failReason: Int)
}