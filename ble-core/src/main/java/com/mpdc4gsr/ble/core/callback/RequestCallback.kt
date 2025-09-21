package com.mpdc4gsr.ble.core.callback

interface RequestCallback {
    fun onResult(success: Boolean, message: String? = null)
    fun onProgress(progress: Int)
}