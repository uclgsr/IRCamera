package com.mpdc4gsr.ble.util

import android.util.Log

class DefaultLogger(private val tag: String?) : Logger {
    private var isEnabled = false

    override fun isEnabled(): Boolean {
        return isEnabled
    }

    override fun setEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
    }

    override fun log(priority: Int, type: Int, msg: String?) {
        if (isEnabled) {
            Log.println(priority, tag, msg!!)
        }
    }

    override fun log(priority: Int, type: Int, msg: String?, th: Throwable?) {
        if (isEnabled) {
            if (msg != null) {
                log(priority, type, msg + "\n" + Log.getStackTraceString(th))
            } else {
                log(priority, type, Log.getStackTraceString(th))
            }
        }
    }
}
