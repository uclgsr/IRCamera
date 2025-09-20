package com.mpdc4gsr.commons.util

import android.util.Log
import com.elvishew.xlog.XLog
import com.mpdc4gsr.ble.BuildConfig

object LLog {
    const val MAX_LENGTH: Int = 2000
    private val isDebug = BuildConfig.DEBUG

    fun d(tag: String?, value: String?) {
        XLog.tag(tag).d(value)
    }

    fun i(tag: String?, value: String?) {
        XLog.tag(tag).i(value)
    }

    fun w(tag: String?, value: String?) {
        XLog.tag(tag).w(value)
    }

    fun e(tag: String?, value: String?) {
        XLog.tag(tag).e(value)
    }

    fun LogMaxPrint(tag: String?, msg: String) {
        if (msg.length > MAX_LENGTH) {
            var length = MAX_LENGTH + 1
            var remain = msg
            var index = 0
            while (length > MAX_LENGTH) {
                index++
                Log.v(tag + "[" + index + "]", " \n" + remain.substring(0, MAX_LENGTH))
                remain = remain.substring(MAX_LENGTH)
                length = remain.length
            }
            if (length <= MAX_LENGTH) {
                index++
                Log.v(tag + "[" + index + "]", " \n" + remain)
            }
        } else {
            Log.v(tag, msg)
        }
    }
}
