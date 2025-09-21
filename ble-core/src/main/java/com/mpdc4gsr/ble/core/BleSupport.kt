package com.mpdc4gsr.ble.core

/**
 * Scan configuration for BLE operations
 */
data class ScanConfiguration(
    val scanTimeoutMillis: Long = 10000L,
    val scanMode: Int = 2, // SCAN_MODE_LOW_LATENCY
    val isAutoConnect: Boolean = false,
    val scanSettings: Any? = null
)

/**
 * Scanner types for BLE operations
 */
enum class ScannerType {
    LEGACY,
    LOLLIPOP,
    AUTO
}

/**
 * Device creator interface
 */
interface DeviceCreator {
    fun create(originDevice: android.bluetooth.BluetoothDevice): Device
}

/**
 * Default device creator implementation
 */
class DefaultDeviceCreator : DeviceCreator {
    override fun create(originDevice: android.bluetooth.BluetoothDevice): Device {
        return Device(originDevice)
    }
}

/**
 * Bond controller for managing device bonding
 */
interface BondController {
    fun bond(device: Device): Boolean
    fun removeBond(device: Device): Boolean
}

/**
 * Scanner interface for BLE scanning operations
 */
interface Scanner {
    fun startScan(scanListener: ScanListener)
    fun stopScan()
    fun isScanning(): Boolean
}

/**
 * Unified BLE manager for cross-platform operations
 */
interface UnifiedBleManager {
    fun startScan(): Boolean
    fun stopScan()
    fun connect(device: Device): Boolean
    fun disconnect(device: Device)
}