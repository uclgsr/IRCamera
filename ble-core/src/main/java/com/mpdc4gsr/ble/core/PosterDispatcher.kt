package com.mpdc4gsr.ble.core

import android.os.Handler
import android.os.Looper

/**
 * Poster dispatcher for handling BLE events on different threads
 */
interface PosterDispatcher {
    fun post(event: Runnable)
    fun postDelayed(event: Runnable, delayMillis: Long)
    fun post(observer: EventObserver, methodInfo: com.mpdc4gsr.commons.poster.MethodInfo)
    fun clearTasks()
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
    
    override fun post(observer: EventObserver, methodInfo: com.mpdc4gsr.commons.poster.MethodInfo) {
        handler.post {
            // Handle the method info call to observer
            when (methodInfo.name) {
                "onConnectFailed" -> {
                    val device = methodInfo.parameters?.get(0)?.value as Device
                    val reason = methodInfo.parameters?.get(1)?.value as Int
                    observer.onConnectFailed(device, reason)
                }
                "onBluetoothAdapterStateChanged" -> {
                    val state = methodInfo.parameters?.get(0)?.value as Int
                    observer.onBluetoothAdapterStateChanged(state)
                }
                "onBluetoothOff" -> {
                    observer.onBluetoothOff()
                }
            }
        }
    }
    
    override fun clearTasks() {
        handler.removeCallbacksAndMessages(null)
    }
}