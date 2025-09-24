package com.mpdc4gsr.ble.core

/**
 * Stub implementation of ShimmerBleController for compilation compatibility
 * This provides minimal functionality to allow the app to build while
 * maintaining the interface contract.
 */
class ShimmerBleController {
    
    fun isConnected(): Boolean = false
    
    fun connect(address: String): Boolean = false
    
    fun disconnect() {
        // Stub implementation
    }
    
    fun startStreaming() {
        // Stub implementation
    }
    
    fun stopStreaming() {
        // Stub implementation
    }
    
    fun getDeviceName(): String = "Unknown Device"
}