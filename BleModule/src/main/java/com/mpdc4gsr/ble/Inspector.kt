package com.mpdc4gsr.ble

internal object Inspector {
    fun <T> requireNonNull(obj: T?, message: String?): T? {
        if (obj == null) throw EasyBLEException(message)
        return obj
    }
}
