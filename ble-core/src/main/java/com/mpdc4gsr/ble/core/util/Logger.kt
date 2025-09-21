package com.mpdc4gsr.ble.core.util

interface Logger {
    fun log(priority: Int, type: Int, msg: String?)

    fun log(priority: Int, type: Int, msg: String?, th: Throwable?)

    var isEnabled: Boolean

    companion object {
        const val TYPE_GENERAL: Int = 0

        const val TYPE_SCAN_STATE: Int = 1

        const val TYPE_CONNECTION_STATE: Int = 2

        const val TYPE_CHARACTERISTIC_READ: Int = 3

        const val TYPE_CHARACTERISTIC_CHANGED: Int = 4

        const val TYPE_READ_REMOTE_RSSI: Int = 5

        const val TYPE_MTU_CHANGED: Int = 6

        const val TYPE_REQUEST_FAILED: Int = 7
        const val TYPE_DESCRIPTOR_READ: Int = 8
        const val TYPE_NOTIFICATION_CHANGED: Int = 9
        const val TYPE_INDICATION_CHANGED: Int = 10
        const val TYPE_CHARACTERISTIC_WRITE: Int = 11
        const val TYPE_PHY_CHANGE: Int = 12
    }
}
