package com.mpdc4gsr.ble.core

internal object Inspector {
    fun <T> requireNonNull(obj: T?, message: String?): T? {
        if (obj == null) throw EasyBLEException(message)
        return obj
    }
}