package com.mpdc4gsr.ble.core

import com.topdon.ble.Device

/**
 * Unified device wrapper that provides a common interface for BLE devices
 */
class UnifiedDevice(
    private val device: Device
) {
    fun getAddress(): String = device.address
    fun getName(): String? = device.name
    
    fun getUnderlyingDevice(): Device = device
}