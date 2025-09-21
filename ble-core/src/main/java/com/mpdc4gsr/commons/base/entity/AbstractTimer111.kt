package com.mpdc4gsr.commons.base.entity

import android.os.Handler
import android.os.Looper
import java.util.Timer
import java.util.TimerTask

abstract class AbstractTimer(private val callbackOnMainThread: Boolean) {
    private val handler: Handler
    private var timer: Timer? = null

    init {
        handler = Handler(Looper.getMainLooper())
    }

    abstract fun onTick()

    @Synchronized
    fun start(delay: Long, period: Long) {
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    if (callbackOnMainThread) {
                        handler.post(object : Runnable {
                            override fun run() {
                                onTick()
                            }
                        })
                    } else {
                        onTick()
                    }
                }
            }, delay, period)
        }
    }

    @Synchronized
    fun stop() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    val isRunning: Boolean
        get() = timer != null
}
