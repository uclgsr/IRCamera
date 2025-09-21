package com.mpdc4gsr.ble.core.callback

/**
 * Callback interface for BLE request operations
 */
interface RequestCallback {
    fun onRequestCompleted(request: Any)
    fun onRequestFailed(request: Any, failType: Int, src: Any?)
}

/**
 * Callback for MTU change operations
 */
interface MtuChangeCallback {
    fun onMtuChanged(mtu: Int, status: Int)
}

/**
 * Callback for notification change operations  
 */
interface NotificationChangeCallback {
    fun onNotificationChanged(enabled: Boolean, status: Int)
}

/**
 * Callback for PHY change operations
 */
interface PhyChangeCallback {
    fun onPhyChanged(txPhy: Int, rxPhy: Int, status: Int)
}

/**
 * Callback for read characteristic operations
 */
interface ReadCharacteristicCallback {
    fun onCharacteristicRead(value: ByteArray?, status: Int)
}

/**
 * Callback for read descriptor operations
 */
interface ReadDescriptorCallback {
    fun onDescriptorRead(value: ByteArray?, status: Int)
}

/**
 * Callback for RSSI read operations
 */
interface ReadRssiCallback {
    fun onRssiRead(rssi: Int, status: Int)
}

/**
 * Callback for request failed operations
 */
interface RequestFailedCallback {
    fun onRequestFailed(failType: Int, src: Any?)
}

/**
 * Callback for write characteristic operations
 */
interface WriteCharacteristicCallback {
    fun onCharacteristicWritten(status: Int)
}