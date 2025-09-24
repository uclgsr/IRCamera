package com.mpdc4gsr.ble.core

/**
 * Unified interface for BLE devices across different backends.
 * Provides a consistent API for device interaction regardless of the underlying BLE implementation.
 */
interface UnifiedDevice {
    /**
     * Get the device's Bluetooth address
     */
    fun getAddress(): String

    /**
     * Get the device's display name
     */
    fun getName(): String?

    /**
     * Get the device's RSSI value
     */
    fun getRssi(): Int

    /**
     * Check if the device is currently connected
     */
    fun isConnected(): Boolean

    /**
     * Get device-specific properties
     */
    fun getProperties(): Map<String, Any>
}

/**
 * Implementation of UnifiedDevice for basic BLE devices
 */
data class BasicUnifiedDevice(
    private val deviceAddress: String,
    private val deviceName: String?,
    private val deviceRssi: Int = -100,
    private val deviceConnected: Boolean = false,
    private val deviceProperties: Map<String, Any> = emptyMap()
) : UnifiedDevice {

    override fun getAddress(): String = deviceAddress

    override fun getName(): String? = deviceName

    override fun getRssi(): Int = deviceRssi

    override fun isConnected(): Boolean = deviceConnected

    override fun getProperties(): Map<String, Any> = deviceProperties
}