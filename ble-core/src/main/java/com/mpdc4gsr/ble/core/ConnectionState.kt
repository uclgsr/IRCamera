package com.mpdc4gsr.ble.core

enum class ConnectionState {
    DISCONNECTED,

    CONNECTING,

    SCANNING_FOR_RECONNECTION,

    CONNECTED,

    SERVICE_DISCOVERING,

    SERVICE_DISCOVERED,

    RELEASED,

    TIMEOUT,

    MTU_SUCCESS
}