package com.mpdc4gsr.ble.core

/**
 * System BLE status enumeration
 */
enum class SystemBleStatus {
    NOT_INITIALIZED,
    READY,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR,
    NOT_SUPPORTED,
    ENABLED,
    DISABLED
}