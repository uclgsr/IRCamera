package com.mpdc4gsr.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.CrossModalSyncManager.CrossModalSyncListener
import com.mpdc4gsr.ble.CrossModalSyncManager.RegisteredDevice
import com.mpdc4gsr.ble.CrossModalSyncManager.SyncQualityMetrics
import com.mpdc4gsr.ble.UnifiedBleManager.UnifiedScanListener
import com.mpdc4gsr.ble.util.BluetoothPermissionUtils

class CrossModalIntegrationExample(private val context: Context) {
    private var bleManager: UnifiedBleManager? = null
    private var syncManager: CrossModalSyncManager? = null

    fun initializeComprehensiveSystem(): Boolean {
        Log.i(TAG, "=== Initializing Comprehensive Cross-Modal System ===")

        try {
            bleManager = UnifiedBleManager.Companion.getInstance(context)
            if (!bleManager!!.initialize()) {
                Log.e(TAG, "Failed to initialize unified BLE manager")
                return false
            }
            Log.i(TAG, "✓ Unified BLE Manager initialized")

            syncManager = CrossModalSyncManager.Companion.getInstance(context)
            Log.i(TAG, "✓ Cross-Modal Sync Manager initialized")

            syncManager!!.addSyncListener(ComprehensiveSyncListener())
            Log.i(TAG, "✓ Sync listener registered")

            if (!startComprehensiveDeviceDiscovery()) {
                Log.e(TAG, "Failed to start device discovery")
                return false
            }
            Log.i(TAG, "✓ Device discovery started")

            Log.i(TAG, "=== Comprehensive Cross-Modal System Ready ===")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize comprehensive system", e)
            return false
        }
    }

    private fun startComprehensiveDeviceDiscovery(): Boolean {
        try {
            val bleSuccess = bleManager!!.startUnifiedDeviceDiscovery(ComprehensiveDeviceListener())

            if (bleSuccess) {
                Log.i(TAG, "BLE device discovery started successfully")
                return true
            } else {
                Log.w(TAG, "BLE device discovery failed to start")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting comprehensive device discovery", e)
            return false
        }
    }

    fun registerNonBleDevices(): Boolean {
        Log.i(TAG, "=== Registering Non-BLE Devices for Cross-Modal Sync ===")

        try {
            val thermalCapabilities =
                CrossModalSyncManager.DeviceCapabilities(
                    false,
                    true,
                    30,
                    10000
                )

            val thermalRegistered = syncManager!!.registerDevice(
                "usb_thermal_001",
                "USB Thermal Camera TC001",
                CrossModalSyncManager.DeviceCategory.USB_CAMERA,
                Any(),  // Placeholder device reference
                thermalCapabilities
            )

            val rgbCapabilities =
                CrossModalSyncManager.DeviceCapabilities(
                    false,
                    true,
                    60,
                    16667
                )

            val rgbRegistered = syncManager!!.registerDevice(
                "rgb_camera_001",
                "Android RGB Camera",
                CrossModalSyncManager.DeviceCategory.RGB_CAMERA,
                Any(),  // Placeholder device reference
                rgbCapabilities
            )

            val networkCapabilities =
                CrossModalSyncManager.DeviceCapabilities(
                    true,
                    true,
                    1000,
                    1000
                )

            val networkRegistered = syncManager!!.registerDevice(
                "pc_controller_001",
                "PC Controller Hub",
                CrossModalSyncManager.DeviceCategory.NETWORK_DEVICE,
                Any(),  // Placeholder device reference
                networkCapabilities
            )

            Log.i(TAG, "Device registration results:")
            Log.i(TAG, "  Thermal Camera: " + (if (thermalRegistered) "✓" else "✗"))
            Log.i(TAG, "  RGB Camera: " + (if (rgbRegistered) "✓" else "✗"))
            Log.i(TAG, "  PC Controller: " + (if (networkRegistered) "✓" else "✗"))

            return thermalRegistered && rgbRegistered && networkRegistered
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register non-BLE devices", e)
            return false
        }
    }

    fun startMultiModalRecording(): Boolean {
        Log.i(TAG, "=== Starting Multi-Modal Recording ===")

        try {
            if (!registerNonBleDevices()) {
                Log.w(TAG, "Some non-BLE device registrations failed, continuing anyway")
            }

            if (!bleManager!!.registerDevicesForCrossModalSync()) {
                Log.w(TAG, "BLE device registration failed, continuing anyway")
            }

            val allDevices = syncManager!!.getRegisteredDevices()
            Log.i(TAG, "Total registered devices: " + allDevices.size)

            for (device in allDevices) {
                Log.i(TAG, "  - " + device.getDeviceName() + " [" + device.getCategory().getDisplayName() + "]")
            }

            val syncStarted = syncManager!!.startSynchronizedRecording()

            if (syncStarted) {
                Log.i(TAG, "✓ Multi-modal synchronized recording started successfully")

                val bleRecordingStarted = bleManager!!.startCrossModalRecording()
                Log.i(TAG, "BLE cross-modal recording: " + (if (bleRecordingStarted) "✓" else "✗"))

                return true
            } else {
                Log.e(TAG, "✗ Failed to start synchronized recording")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start multi-modal recording", e)
            return false
        }
    }

    fun stopMultiModalRecording(): Boolean {
        Log.i(TAG, "=== Stopping Multi-Modal Recording ===")

        try {
            val syncStopped = syncManager!!.stopSynchronizedRecording()

            val bleStopped = bleManager!!.stopCrossModalRecording()

            Log.i(TAG, "Recording stop results:")
            Log.i(TAG, "  Synchronized recording: " + (if (syncStopped) "✓" else "✗"))
            Log.i(TAG, "  BLE recording: " + (if (bleStopped) "✓" else "✗"))

            return syncStopped && bleStopped
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop multi-modal recording", e)
            return false
        }
    }

    fun printSystemStatus() {
        Log.i(TAG, "=== Comprehensive System Status ===")

        try {
            val bleStatus = bleManager!!.getSystemBleStatus()
            Log.i(TAG, "BLE System:")
            Log.i(TAG, "  Active connections: " + bleStatus.activeConnections)
            Log.i(TAG, "  Multi-device mode: " + bleStatus.multiDeviceMode)
            Log.i(TAG, "  Enhanced error recovery: " + bleStatus.enhancedErrorRecovery)
            Log.i(TAG, "  Total devices connected: " + bleStatus.totalDevicesConnected)

            val isSynchronizing = syncManager!!.isSynchronizing()
            Log.i(TAG, "Cross-Modal Sync:")
            Log.i(TAG, "  Synchronization active: " + isSynchronizing)

            for (category in CrossModalSyncManager.DeviceCategory.entries) {
                val categoryDevices = syncManager!!.getDevicesByCategory(category)
                var activeCount = 0
                for (device in categoryDevices) {
                    if (device.isActive()) activeCount++
                }

                if (!categoryDevices.isEmpty()) {
                    Log.i(
                        TAG, "  " + category.getDisplayName() + ": " +
                                activeCount + "/" + categoryDevices.size + " active"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system status", e)
        }
    }

    private inner class ComprehensiveDeviceListener : UnifiedScanListener {
        override fun onShimmerDeviceFound(
            device: BluetoothDevice,
            type: UnifiedBleManager.DeviceType?, rssi: Int, scanRecord: ByteArray?
        ) {
            val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)
            Log.i(TAG, "Shimmer device discovered: " + deviceName + " [" + type + "] RSSI: " + rssi)

            try {
                val capabilities =
                    CrossModalSyncManager.DeviceCapabilities(
                        true,
                        true,
                        128,
                        1000
                    )

                syncManager!!.registerDevice(
                    BluetoothPermissionUtils.getDeviceAddress(context, device),
                    if (deviceName != null && !deviceName.isEmpty()) deviceName else "Shimmer Device",
                    CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                    device,
                    capabilities
                )

                Log.i(TAG, "Auto-registered Shimmer device for cross-modal sync")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-register Shimmer device", e)
            }
        }

        override fun onTopdonDeviceFound(
            device: BluetoothDevice,
            type: UnifiedBleManager.DeviceType?, rssi: Int, scanRecord: ByteArray?
        ) {
            val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)
            Log.i(TAG, "Topdon device discovered: " + deviceName + " [" + type + "] RSSI: " + rssi)

            try {
                val capabilities =
                    CrossModalSyncManager.DeviceCapabilities(
                        true,
                        true,
                        30,
                        5000
                    )

                syncManager!!.registerDevice(
                    BluetoothPermissionUtils.getDeviceAddress(context, device),
                    if (deviceName != null && !deviceName.isEmpty()) deviceName else "Topdon Device",
                    CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                    device,
                    capabilities
                )

                Log.i(TAG, "Auto-registered Topdon device for cross-modal sync")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-register Topdon device", e)
            }
        }

        override fun onUnknownDeviceFound(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)
            Log.d(TAG, "Unknown device discovered: " + deviceName + " RSSI: " + rssi)
        }

        override fun onScanError(errorCode: Int, message: String?) {
            Log.e(TAG, "Device discovery error: " + message + " (code: " + errorCode + ")")
        }

        override fun onScanComplete() {
            Log.i(TAG, "Device discovery completed")
        }
    }

    private inner class ComprehensiveSyncListener : CrossModalSyncListener {
        override fun onDeviceRegistered(device: RegisteredDevice) {
            Log.i(
                TAG, "Device registered for sync: " + device.getDeviceName() +
                        " [" + device.getCategory().getDisplayName() + "]"
            )
        }

        override fun onDeviceUnregistered(deviceId: String) {
            Log.i(TAG, "Device unregistered from sync: " + deviceId)
        }

        override fun onSynchronizationStarted(activeDevices: MutableList<RegisteredDevice>) {
            Log.i(TAG, "Synchronization started with " + activeDevices.size + " devices:")
            for (device in activeDevices) {
                Log.i(TAG, "  - " + device.getDeviceName() + " [" + device.getCategory().getDisplayName() + "]")
            }
        }

        override fun onSynchronizationStopped() {
            Log.i(TAG, "Synchronization stopped")
        }

        override fun onSyncEvent(
            eventType: String, masterTimestamp: Long,
            deviceTimestamps: MutableMap<String?, Long?>
        ) {
            Log.i(TAG, "Sync event: " + eventType + " at " + masterTimestamp)
            Log.d(TAG, "Device timestamps: " + deviceTimestamps.size + " devices")
        }

        override fun onSyncQualityUpdate(
            deviceId: String,
            metrics: SyncQualityMetrics
        ) {
            Log.d(
                TAG, "Sync quality update for " + deviceId +
                        ": accuracy=" + metrics.getSyncAccuracyPercent() + "%"
            )
        }
    }

    companion object {
        private const val TAG = "CrossModalExample"
    }
}
