package com.mpdc4gsr.ble.core.callback

interface RequestCallback {
    fun onRequestSuccess()
    fun onRequestFailed(error: Exception?)
}