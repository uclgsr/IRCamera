package com.mpdc4gsr.ble.core

/**
 * Callback interface for BLE request operations
 */
interface RequestCallback {
    fun onRequest()
    fun onResponse(response: ByteArray?)
    fun onFail(reason: Int, msg: String?)
}