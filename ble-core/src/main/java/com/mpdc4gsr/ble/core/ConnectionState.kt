<<<<<<<< HEAD:ble-core/src/main/java/com/mpdc4gsr/ble/core/ConnectionState.kt
package com.mpdc4gsr.ble.core
========
package com.mpdc4gsr.ble;

public enum ConnectionState {
>>>>>>>> dev:ble-core/src/main/java/com/mpdc4gsr/ble/core/ConnectionState.java

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