package com.mpdc4gsr.ble.core

/**
 * Stub implementation of UnifiedDevice for compilation compatibility
 */
data class UnifiedDevice(
    val name: String,
    val address: String,
    val deviceType: DeviceType = DeviceType.UNKNOWN
) {
    enum class DeviceType {
        SHIMMER,
        TOPDON,
        UNKNOWN
    }

    fun isConnected(): Boolean = false
    fun getSignalStrength(): Int = -50
    fun getBatteryLevel(): Int = 80
    
    // Note: getAddress() and getName() are automatically provided by data class
}