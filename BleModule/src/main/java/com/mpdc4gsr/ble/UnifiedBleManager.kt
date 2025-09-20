package com.mpdc4gsr.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.ShimmerBleController.ShimmerScanListener
import com.mpdc4gsr.ble.TopdonBleController.TopdonScanListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.Volatile

class UnifiedBleManager private constructor(context: Context) {
    private val context: Context
    private val bluetoothManager: BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter?

    private val deviceMetrics = ConcurrentHashMap<String?, ConnectionMetrics>()
    private val multiDeviceMode = AtomicBoolean(false)
    private val activeConnections = AtomicInteger(0)

    private val enhancedErrorRecovery = AtomicBoolean(true)
    private val connectionOptimization = AtomicBoolean(true)
    private val dataLossDetection = AtomicBoolean(true)

    private val shimmerController: ShimmerBleController?
    private val topdonController: TopdonBleController?
    private val connectedDevices = ConcurrentHashMap<String?, UnifiedDevice>()
    private val isInitialized = AtomicBoolean(false)
    private val isScanning = AtomicBoolean(false)
    private val gsrDevices = ConcurrentHashMap<String?, Boolean?>()
    private var easyBLE: EasyBLE? = null

    init {
        this.context = context.getApplicationContext()
        this.bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        this.bluetoothAdapter = if (bluetoothManager != null) bluetoothManager.getAdapter() else null


        this.shimmerController = ShimmerBleController(context, this)
        this.topdonController = TopdonBleController(context, this)

        Log.i(TAG, "UnifiedBleManager initialized with comprehensive BLE support and cross-modal coordination")
    }

    fun initialize(): Boolean {
        if (isInitialized.get()) {
            return true
        }

        try {
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter not available")
                return false
            }

            if (!bluetoothAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth is not enabled")
                return false
            }

            this.easyBLE = EasyBLE.Companion.getBuilder().setUseNordicBleBackend(true).build()

            shimmerController!!.initialize()
            topdonController!!.initialize()

            try {
                val syncManager: CrossModalSyncManager = CrossModalSyncManager.Companion.getInstance(context)
                Log.i(TAG, "Cross-modal synchronization manager initialized for unified BLE coordination")
            } catch (e: Exception) {
                Log.w(TAG, "Cross-modal sync manager initialization failed, continuing without sync", e)
            }

            isInitialized.set(true)
            Log.i(TAG, "Unified BLE Manager initialized successfully with cross-modal capabilities")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Unified BLE Manager", e)
            return false
        }
    }

    fun startUnifiedDeviceDiscovery(listener: UnifiedScanListener): Boolean {
        if (!isInitialized.get()) {
            Log.e(TAG, "Manager not initialized")
            return false
        }

        if (isScanning.get()) {
            Log.w(TAG, "Scanning already in progress")
            return false
        }

        try {
            isScanning.set(true)

            shimmerController!!.startDeviceDiscovery(ShimmerScanAdapter(listener))

            topdonController!!.startDeviceDiscovery(TopdonScanAdapter(listener))

            Log.i(TAG, "Started unified device discovery for Shimmer and Topdon devices")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start unified device discovery", e)
            isScanning.set(false)
            return false
        }
    }

    fun stopUnifiedDeviceDiscovery() {
        if (!isScanning.get()) {
            return
        }

        try {
            shimmerController!!.stopDeviceDiscovery()
            topdonController!!.stopDeviceDiscovery()
            isScanning.set(false)

            Log.i(TAG, "Stopped unified device discovery")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping unified device discovery", e)
        }
    }

    fun connectToShimmerDevice(
        device: BluetoothDevice,
        config: ShimmerDeviceConfig,
        listener: UnifiedConnectionListener
    ): UnifiedDevice? {
        check(isInitialized.get()) { "Manager not initialized" }

        try {
            val unifiedDevice = shimmerController!!.connectDevice(device, config, listener)
            if (unifiedDevice != null) {
                connectedDevices.put(device.getAddress(), unifiedDevice)
                Log.i(TAG, "Connected to Shimmer device: " + device.getAddress())
            }
            return unifiedDevice
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Shimmer device", e)
            return null
        }
    }

    fun connectToTopdonDevice(
        device: BluetoothDevice,
        config: TopdonDeviceConfig,
        listener: UnifiedConnectionListener
    ): UnifiedDevice? {
        check(isInitialized.get()) { "Manager not initialized" }

        try {
            val unifiedDevice = topdonController!!.connectDevice(device, config, listener)
            if (unifiedDevice != null) {
                connectedDevices.put(device.getAddress(), unifiedDevice)
                Log.i(TAG, "Connected to Topdon device: " + device.getAddress())
            }
            return unifiedDevice
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Topdon device", e)
            return null
        }
    }

    fun getConnectedDevices(): MutableList<UnifiedDevice?> {
        return ArrayList<UnifiedDevice?>(connectedDevices.values)
    }

    fun getConnectedDevicesByType(type: DeviceType?): MutableList<UnifiedDevice?> {
        val devices: MutableList<UnifiedDevice?> = ArrayList<UnifiedDevice?>()
        for (device in connectedDevices.values) {
            if (device.getDeviceType() == type) {
                devices.add(device)
            }
        }
        return devices
    }

    fun disconnectDevice(address: String) {
        val device = connectedDevices.get(address)
        if (device != null) {
            device.disconnect()
            connectedDevices.remove(address)
            Log.i(TAG, "Disconnected device: " + address)
        }
    }

    fun disconnectAllDevices() {
        for (device in connectedDevices.values) {
            device.disconnect()
        }
        connectedDevices.clear()
        Log.i(TAG, "Disconnected all devices")
    }

    fun cleanup() {
        try {
            stopUnifiedDeviceDiscovery()
            disconnectAllDevices()

            shimmerController!!.cleanup()
            topdonController!!.cleanup()

            isInitialized.set(false)
            Log.i(TAG, "UnifiedBleManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    fun initialize(context: Context, enableMultiDevice: Boolean): Boolean {
        if (isInitialized.get()) {
            return true
        }

        this.multiDeviceMode.set(enableMultiDevice)
        return initialize()
    }

    fun enableMultiDeviceMode(enabled: Boolean) {
        this.multiDeviceMode.set(enabled)
        Log.i(TAG, "Multi-device mode " + (if (enabled) "enabled" else "disabled"))
    }

    val systemStatus: SystemBleStatus
        get() = SystemBleStatus(
            activeConnections.get(),
            multiDeviceMode.get(),
            enhancedErrorRecovery.get(),
            deviceMetrics.size.toLong()
        )

    fun markAsGsrSensor(deviceAddress: String) {
        gsrDevices.put(deviceAddress, true)
        Log.i(TAG, "Device " + deviceAddress + " marked as GSR sensor")
    }

    val connectedShimmerDevices: MutableList<UnifiedDevice>
        get() {
            val shimmerDevices: MutableList<UnifiedDevice> = ArrayList<UnifiedDevice>()


            if (shimmerController != null) {
                try {
                    shimmerDevices.addAll(shimmerController.getConnectedDevices())
                    Log.d(
                        TAG,
                        "Found " + shimmerDevices.size + " connected Shimmer devices"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting connected Shimmer devices", e)
                }
            }

            return shimmerDevices
        }


    fun scanForShimmerDevices(scanDurationMs: Long, callback: ShimmerScanCallback?) {
        if (shimmerController != null) {
            shimmerController.scanForDevices(scanDurationMs, callback)
        } else {
            Log.e(TAG, "Shimmer controller not initialized for scanning")
            if (callback != null) {
                callback.onScanFailed("Shimmer controller not available")
            }
        }
    }

    val connectedTopdonDevices: MutableList<UnifiedDevice>
        get() {
            val topdonDevices: MutableList<UnifiedDevice> = ArrayList<UnifiedDevice>()


            if (topdonController != null) {
                try {
                    topdonDevices.addAll(topdonController.getConnectedDevices())
                    Log.d(
                        TAG,
                        "Found " + topdonDevices.size + " connected Topdon devices"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting connected Topdon devices", e)
                }
            }

            return topdonDevices
        }

    val systemBleStatus: SystemBleStatus
        get() = SystemBleStatus(
            activeConnections.get(),
            true,
            true,
            connectedDevices.size.toLong()
        )

    fun connectWithEnhancements(deviceAddress: String): Connection? {
        Log.i(TAG, "Enhanced connection attempt for device: " + deviceAddress)

        val metrics = deviceMetrics.computeIfAbsent(
            deviceAddress
        ) { k: String? -> ConnectionMetrics() }
        metrics.connectAttempts.incrementAndGet()

        val connection = easyBLE!!.connect(deviceAddress)

        if (connection != null) {
            activeConnections.incrementAndGet()
            metrics.lastConnectionTime.set(System.currentTimeMillis())
            metrics.successfulConnections.incrementAndGet()
            Log.i(TAG, "Enhanced connection successful for device: " + deviceAddress)
        } else {
            Log.w(TAG, "Enhanced connection failed for device: " + deviceAddress)
        }

        return connection
    }

    fun registerDevicesForCrossModalSync(): Boolean {
        try {
            val syncManager: CrossModalSyncManager = CrossModalSyncManager.Companion.getInstance(context)

            val shimmerDevices = this.connectedShimmerDevices
            for (device in shimmerDevices) {
                val capabilities =
                    CrossModalSyncManager.DeviceCapabilities(
                        true,
                        true,
                        128,
                        1000
                    )

                syncManager.registerDevice(
                    device.getDeviceId(),
                    device.getDeviceName(),
                    CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                    device,
                    capabilities
                )
            }

            val topdonDevices = this.connectedTopdonDevices
            for (device in topdonDevices) {
                val capabilities =
                    CrossModalSyncManager.DeviceCapabilities(
                        true,
                        true,
                        30,
                        5000
                    )

                syncManager.registerDevice(
                    device.getDeviceId(),
                    device.getDeviceName(),
                    CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                    device,
                    capabilities
                )
            }

            Log.i(
                TAG, "Registered " + (shimmerDevices.size + topdonDevices.size) +
                        " BLE devices for cross-modal synchronization"
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register devices for cross-modal sync", e)
            return false
        }
    }

    fun startCrossModalRecording(): Boolean {
        try {
            registerDevicesForCrossModalSync()

            val syncManager: CrossModalSyncManager = CrossModalSyncManager.Companion.getInstance(context)
            return syncManager.startSynchronizedRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start cross-modal recording", e)
            return false
        }
    }

    fun stopCrossModalRecording(): Boolean {
        try {
            val syncManager: CrossModalSyncManager = CrossModalSyncManager.Companion.getInstance(context)
            return syncManager.stopSynchronizedRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop cross-modal recording", e)
            return false
        }
    }

    val crossModalSyncManager: CrossModalSyncManager
        get() = CrossModalSyncManager.Companion.getInstance(context)

    enum class DeviceType {
        SHIMMER_GSR,
        SHIMMER_PPG,
        SHIMMER_IMU,
        MPDC4GSR_THERMAL,
        MPDC4GSR_ENV,
        MPDC4GSR_MULTI,
        UNKNOWN
    }


    interface ShimmerScanCallback {
        fun onDeviceFound(device: UnifiedDevice?)

        fun onScanComplete(foundDevices: MutableList<UnifiedDevice?>?)

        fun onScanFailed(error: String?)
    }

    interface UnifiedScanListener {
        fun onShimmerDeviceFound(device: BluetoothDevice?, type: DeviceType?, rssi: Int, scanRecord: ByteArray?)

        fun onTopdonDeviceFound(device: BluetoothDevice?, type: DeviceType?, rssi: Int, scanRecord: ByteArray?)

        fun onUnknownDeviceFound(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?)

        fun onScanError(errorCode: Int, message: String?)

        fun onScanComplete()
    }

    interface UnifiedConnectionListener {
        fun onDeviceConnected(device: UnifiedDevice?)

        fun onDeviceDisconnected(device: UnifiedDevice?, reason: Int)

        fun onConnectionError(device: UnifiedDevice?, errorCode: Int, message: String?)

        fun onDataReceived(device: UnifiedDevice?, data: ByteArray?)

        fun onDeviceReady(device: UnifiedDevice?)
    }

    class ConnectionMetrics {
        val connectAttempts: AtomicLong = AtomicLong(0)
        val successfulConnections: AtomicLong = AtomicLong(0)
        val disconnections: AtomicLong = AtomicLong(0)
        val dataPacketsReceived: AtomicLong = AtomicLong(0)
        val lastConnectionTime: AtomicLong = AtomicLong(0)

        val reliabilityScore: Double
            get() {
                val attempts = connectAttempts.get()
                return if (attempts > 0) successfulConnections.get().toDouble() / attempts else 0.0
            }
    }

    class SystemBleStatus(
        val activeConnections: Int, val multiDeviceMode: Boolean,
        val enhancedErrorRecovery: Boolean, val totalDevicesConnected: Long
    )

    private class ShimmerScanAdapter(private val unifiedListener: UnifiedScanListener) : ShimmerScanListener {
        override fun onShimmerDeviceFound(
            device: BluetoothDevice?,
            type: DeviceType?,
            rssi: Int,
            scanRecord: ByteArray?
        ) {
            unifiedListener.onShimmerDeviceFound(device, type, rssi, scanRecord)
        }

        override fun onScanError(errorCode: Int, message: String?) {
            unifiedListener.onScanError(errorCode, message)
        }

        override fun onScanComplete() {
            unifiedListener.onScanComplete()
        }
    }

    private class TopdonScanAdapter(private val unifiedListener: UnifiedScanListener) : TopdonScanListener {
        override fun onTopdonDeviceFound(
            device: BluetoothDevice?,
            type: DeviceType?,
            rssi: Int,
            scanRecord: ByteArray?
        ) {
            unifiedListener.onTopdonDeviceFound(device, type, rssi, scanRecord)
        }

        override fun onScanError(errorCode: Int, message: String?) {
            unifiedListener.onScanError(errorCode, message)
        }

        override fun onScanComplete() {
            unifiedListener.onScanComplete()
        }
    }

    companion object {
        private const val TAG = "UnifiedBleManager"

        @Volatile
        private var instance: UnifiedBleManager? = null

        fun getInstance(context: Context): UnifiedBleManager? {
            if (instance == null) {
                synchronized(UnifiedBleManager::class.java) {
                    if (instance == null) {
                        instance = UnifiedBleManager(context)
                    }
                }
            }
            return instance
        }
    }
}
