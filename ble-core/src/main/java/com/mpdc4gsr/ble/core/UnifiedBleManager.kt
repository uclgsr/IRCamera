package com.mpdc4gsr.ble.core

import android.app.Application
import android.content.Context
import com.topdon.ble.Connection

/**
 * Stub implementation of UnifiedBleManager for compilation compatibility
 * This provides minimal functionality to allow the app to build while
 * maintaining the interface contract.
 */
class UnifiedBleManager private constructor(private val application: Application) {

    companion object {
        private var instance: UnifiedBleManager? = null

        fun getInstance(application: Application): UnifiedBleManager {
            if (instance == null) {
                synchronized(UnifiedBleManager::class.java) {
                    if (instance == null) {
                        instance = UnifiedBleManager(application)
                    }
                }
            }
            return instance!!
        }

        fun getInstance(context: Context): UnifiedBleManager {
            return getInstance(context.applicationContext as Application)
        }
    }

    fun initialize(): Boolean {
        // Stub implementation for initialization
        return true
    }

    fun initialize(context: Context, enableNordicBackend: Boolean) {
        // Stub implementation for initialization with parameters
    }

    fun enableMultiDeviceMode(enabled: Boolean) {
        // Stub implementation for multi-device mode
    }

    fun markAsGsrSensor(deviceAddress: String) {
        // Stub implementation for marking GSR sensor
    }

    fun connectWithEnhancements(deviceAddress: String): Connection? {
        // Stub implementation - returns null to indicate failure
        return null
    }

    data class SystemBleStatus(
        val status: BleStatus,
        val activeConnections: Int = 0,
        val totalDevicesConnected: Int = 0,
        val multiDeviceMode: Boolean = false
    )

    enum class BleStatus {
        AVAILABLE, UNAVAILABLE, DISABLED
    }

    fun getSystemStatus(): SystemBleStatus {
        return SystemBleStatus(
            status = BleStatus.AVAILABLE,
            activeConnections = 0,
            totalDevicesConnected = 0,
            multiDeviceMode = false
        )
    }

    fun isInitialized(): Boolean = true

    fun getContext(): Context = application

    // Shimmer device management methods
    fun getConnectedShimmerDevices(): List<UnifiedDevice> {
        // Stub implementation - return empty list
        return emptyList()
    }

    fun scanForShimmerDevices(callback: ShimmerScanCallback) {
        // Stub implementation - immediately call scan complete
        callback.onScanComplete()
    }

    fun scanForShimmerDevices(durationMs: Long, callback: ShimmerScanCallback) {
        // Stub implementation - immediately call scan complete with duration parameter
        callback.onScanComplete()
    }

    // Additional stub methods can be added here as needed
    interface ShimmerScanCallback {
        fun onDeviceFound(device: UnifiedDevice)
        fun onScanComplete(foundDevices: List<UnifiedDevice> = emptyList())
        fun onScanFailed(errorCode: Int)
        fun onError(error: String)
    }
}