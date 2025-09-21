package com.mpdc4gsr.ble.core

import android.os.Handler
import android.os.Looper

/**
 * Poster dispatcher for handling BLE events on different threads
 */
interface PosterDispatcher {
    fun post(event: Runnable)
    fun postDelayed(event: Runnable, delayMillis: Long)
}

/**
 * Default implementation using Android Handler
 */
class DefaultPosterDispatcher : PosterDispatcher {
    private val handler = Handler(Looper.getMainLooper())
    
    override fun post(event: Runnable) {
        handler.post(event)
    }
    
    override fun postDelayed(event: Runnable, delayMillis: Long) {
        handler.postDelayed(event, delayMillis)
    }
}