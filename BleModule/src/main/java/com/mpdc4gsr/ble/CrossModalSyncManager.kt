package com.mpdc4gsr.ble

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.Volatile

class CrossModalSyncManager private constructor(context: Context) {
    private val context: Context?
    private val isInitialized = AtomicBoolean(false)
    private val isSynchronizing = AtomicBoolean(false)

    private val registeredDevices: MutableMap<String?, RegisteredDevice> =
        ConcurrentHashMap<String?, RegisteredDevice>()
    private val devicesByCategory: MutableMap<DeviceCategory?, MutableList<RegisteredDevice?>?> =
        ConcurrentHashMap<DeviceCategory?, MutableList<RegisteredDevice?>?>()

    private val masterTimestampNanos = AtomicLong(0)
    private val deviceClockOffsets: MutableMap<String?, Long?> = ConcurrentHashMap<String?, Long?>()
    private val syncQualityMap: MutableMap<String?, SyncQualityMetrics?> =
        ConcurrentHashMap<String?, SyncQualityMetrics?>()

    private val listeners: MutableList<CrossModalSyncListener> = ArrayList<CrossModalSyncListener>()

    init {
        this.context = context.getApplicationContext()
        initializeDeviceCategories()
    }

    private fun initializeDeviceCategories() {
        for (category in DeviceCategory.entries) {
            devicesByCategory.put(category, ArrayList<RegisteredDevice?>())
        }
        isInitialized.set(true)
        Log.i(TAG, "CrossModalSyncManager initialized with " + DeviceCategory.entries.size + " device categories")
    }

    fun registerDevice(
        deviceId: String, deviceName: String,
        category: DeviceCategory, deviceRef: Any,
        capabilities: DeviceCapabilities
    ): Boolean {
        if (!isInitialized.get()) {
            Log.e(TAG, "CrossModalSyncManager not initialized")
            return false
        }

        try {
            val device = RegisteredDevice(deviceId, deviceName, category, deviceRef, capabilities)

            registeredDevices.put(deviceId, device)

            val categoryDevices = devicesByCategory.get(category)
            if (categoryDevices != null) {
                categoryDevices.add(device)
            }

            deviceClockOffsets.put(deviceId, 0L)

            syncQualityMap.put(deviceId, SyncQualityMetrics(0, 0, 100.0, 0, System.nanoTime()))

            Log.i(TAG, "Registered device: " + deviceName + " [" + category.displayName + "] ID: " + deviceId)

            notifyDeviceRegistered(device)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device: " + deviceId, e)
            return false
        }
    }

    fun unregisterDevice(deviceId: String): Boolean {
        val device: RegisteredDevice? = registeredDevices.remove(deviceId)
        if (device != null) {
            val categoryDevices = devicesByCategory.get(device.category)
            if (categoryDevices != null) {
                categoryDevices.remove(device)
            }

            deviceClockOffsets.remove(deviceId)
            syncQualityMap.remove(deviceId)

            Log.i(TAG, "Unregistered device: " + device.deviceName + " ID: " + deviceId)

            notifyDeviceUnregistered(deviceId)

            return true
        }
        return false
    }

    fun startSynchronizedRecording(): Boolean {
        if (!isInitialized.get()) {
            Log.e(TAG, "Cannot start recording - manager not initialized")
            return false
        }

        if (isSynchronizing.get()) {
            Log.w(TAG, "Synchronization already in progress")
            return false
        }

        try {
            val activeDevices: MutableList<RegisteredDevice?> = ArrayList<RegisteredDevice?>()
            for (device in registeredDevices.values) {
                if (device.isActive) {
                    activeDevices.add(device)
                }
            }

            if (activeDevices.isEmpty()) {
                Log.w(TAG, "No active devices available for synchronized recording")
                return false
            }

            masterTimestampNanos.set(System.nanoTime())

            isSynchronizing.set(true)

            Log.i(TAG, "Started synchronized recording with " + activeDevices.size + " devices")

            notifySynchronizationStarted(activeDevices)

            return sendSyncCommand("START_RECORDING", masterTimestampNanos.get())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start synchronized recording", e)
            isSynchronizing.set(false)
            return false
        }
    }

    fun stopSynchronizedRecording(): Boolean {
        if (!isSynchronizing.get()) {
            Log.w(TAG, "No synchronization in progress")
            return false
        }

        try {
            val success = sendSyncCommand("STOP_RECORDING", System.nanoTime())

            isSynchronizing.set(false)
            masterTimestampNanos.set(0)

            Log.i(TAG, "Stopped synchronized recording")

            notifySynchronizationStopped()

            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop synchronized recording", e)
            return false
        }
    }

    private fun sendSyncCommand(command: String, masterTimestamp: Long): Boolean {
        var overallSuccess = true
        val deviceTimestamps: MutableMap<String?, Long?> = ConcurrentHashMap<String?, Long?>()

        for (device in registeredDevices.values) {
            if (!device.isActive) continue

            try {
                val deviceTimestamp = masterTimestamp + deviceClockOffsets.getOrDefault(device.deviceId, 0L)!!
                deviceTimestamps.put(device.deviceId, deviceTimestamp)

                val deviceSuccess = sendCommandToDevice(device, command, deviceTimestamp)
                if (!deviceSuccess) {
                    Log.w(TAG, "Failed to send command " + command + " to device: " + device.deviceName)
                    overallSuccess = false
                }

                device.lastSyncTimestamp = deviceTimestamp
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command to device: " + device.deviceName, e)
                overallSuccess = false
            }
        }

        notifySyncEvent(command, masterTimestamp, deviceTimestamps)

        return overallSuccess
    }

    private fun sendCommandToDevice(device: RegisteredDevice, command: String, timestamp: Long): Boolean {
        try {
            when (device.category) {
                DeviceCategory.BLE_SENSOR -> return sendBleDeviceCommand(device, command, timestamp)
                DeviceCategory.USB_CAMERA -> return sendUsbCameraCommand(device, command, timestamp)
                DeviceCategory.RGB_CAMERA -> return sendRgbCameraCommand(device, command, timestamp)
                DeviceCategory.NETWORK_DEVICE -> return sendNetworkDeviceCommand(device, command, timestamp)
                DeviceCategory.AUDIO_DEVICE -> return sendAudioDeviceCommand(device, command, timestamp)
                else -> {
                    Log.w(TAG, "Unknown device category: " + device.category)
                    return false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command to device: " + device.deviceName, e)
            return false
        }
    }

    private fun sendBleDeviceCommand(device: RegisteredDevice, command: String, timestamp: Long): Boolean {
        val deviceRef = device.deviceRef

        if (deviceRef is UnifiedDevice) {
            val unifiedDevice = deviceRef

            return sendUnifiedDeviceCommand(unifiedDevice, command, timestamp)
        }

        Log.w(TAG, "Unsupported BLE device type: " + deviceRef.javaClass.getSimpleName())
        return false
    }

    private fun sendUnifiedDeviceCommand(device: UnifiedDevice, command: String, timestamp: Long): Boolean {
        try {
            when (command) {
                "START_RECORDING" -> return device.startRecording(timestamp)
                "STOP_RECORDING" -> return device.stopRecording(timestamp)
                "SYNC_MARK" -> return device.addSyncMark(timestamp)
                else -> {
                    Log.w(TAG, "Unknown command for unified device: " + command)
                    return false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command to unified device", e)
            return false
        }
    }

    private fun sendUsbCameraCommand(device: RegisteredDevice, command: String, timestamp: Long): Boolean {
        Log.d(TAG, "USB Camera command: " + command + " for device: " + device.deviceName)
        return true
    }

    private fun sendRgbCameraCommand(device: RegisteredDevice, command: String, timestamp: Long): Boolean {
        Log.d(TAG, "RGB Camera command: " + command + " for device: " + device.deviceName)
        return true
    }

    private fun sendNetworkDeviceCommand(device: RegisteredDevice, command: String, timestamp: Long): Boolean {
        Log.d(TAG, "Network Device command: " + command + " for device: " + device.deviceName)
        return true
    }

    private fun sendAudioDeviceCommand(device: RegisteredDevice, command: String, timestamp: Long): Boolean {
        Log.d(TAG, "Audio Device command: " + command + " for device: " + device.deviceName)
        return true
    }

    fun getRegisteredDevices(): MutableList<RegisteredDevice?> {
        return ArrayList<RegisteredDevice?>(registeredDevices.values)
    }

    fun getDevicesByCategory(category: DeviceCategory): MutableList<RegisteredDevice?> {
        val categoryDevices = devicesByCategory.get(category)
        return if (categoryDevices != null) ArrayList<RegisteredDevice?>(categoryDevices) else ArrayList<RegisteredDevice?>()
    }

    fun getSyncQualityMetrics(deviceId: String): SyncQualityMetrics? {
        return syncQualityMap.get(deviceId)
    }

    fun isSynchronizing(): Boolean {
        return isSynchronizing.get()
    }

    fun addSyncListener(listener: CrossModalSyncListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeSyncListener(listener: CrossModalSyncListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun notifyDeviceRegistered(device: RegisteredDevice) {
        synchronized(listeners) {
            for (listener in listeners) {
                try {
                    listener.onDeviceRegistered(device)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying device registration", e)
                }
            }
        }
    }

    private fun notifyDeviceUnregistered(deviceId: String) {
        synchronized(listeners) {
            for (listener in listeners) {
                try {
                    listener.onDeviceUnregistered(deviceId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying device unregistration", e)
                }
            }
        }
    }

    private fun notifySynchronizationStarted(activeDevices: MutableList<RegisteredDevice?>) {
        synchronized(listeners) {
            for (listener in listeners) {
                try {
                    listener.onSynchronizationStarted(ArrayList<RegisteredDevice?>(activeDevices))
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying synchronization start", e)
                }
            }
        }
    }

    private fun notifySynchronizationStopped() {
        synchronized(listeners) {
            for (listener in listeners) {
                try {
                    listener.onSynchronizationStopped()
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying synchronization stop", e)
                }
            }
        }
    }

    private fun notifySyncEvent(
        eventType: String,
        masterTimestamp: Long,
        deviceTimestamps: MutableMap<String?, Long?>
    ) {
        synchronized(listeners) {
            for (listener in listeners) {
                try {
                    listener.onSyncEvent(
                        eventType,
                        masterTimestamp,
                        ConcurrentHashMap<String?, Long?>(deviceTimestamps)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying sync event", e)
                }
            }
        }
    }

    enum class DeviceCategory(displayName: String, description: String) {
        BLE_SENSOR("BLE Sensors", "Shimmer GSR, Topdon BLE devices"),
        USB_CAMERA("USB Cameras", "Thermal cameras, thermal-lite devices"),
        RGB_CAMERA("RGB Cameras", "Android camera, external cameras"),
        NETWORK_DEVICE("Network Devices", "PC Controllers, hub-spoke systems"),
        AUDIO_DEVICE("Audio Devices", "Microphones, audio recorders");

        val displayName: String?
        val description: String?

        init {
            this.displayName = displayName
            this.description = description
        }
    }

    interface CrossModalSyncListener {
        fun onDeviceRegistered(device: RegisteredDevice)

        fun onDeviceUnregistered(deviceId: String)

        fun onSynchronizationStarted(activeDevices: MutableList<RegisteredDevice?>)

        fun onSynchronizationStopped()

        fun onSyncEvent(eventType: String, masterTimestamp: Long, deviceTimestamps: MutableMap<String?, Long?>)

        fun onSyncQualityUpdate(deviceId: String, metrics: SyncQualityMetrics)
    }

    class RegisteredDevice(
        val deviceId: String?, val deviceName: String?, val category: DeviceCategory?,
        val deviceRef: Any, val capabilities: DeviceCapabilities?
    ) {
        @Volatile
        var isActive: Boolean = false

        @Volatile
        var lastSyncTimestamp: Long = 0
    }

    class DeviceCapabilities(
        private val supportsHardwareSync: Boolean, private val supportsTimestampGeneration: Boolean,
        val maxSamplingRateHz: Int, val syncAccuracyMicros: Long
    ) {
        fun supportsHardwareSync(): Boolean {
            return supportsHardwareSync
        }

        fun supportsTimestampGeneration(): Boolean {
            return supportsTimestampGeneration
        }
    }

    class SyncQualityMetrics(
        val avgClockDriftNanos: Long, val maxClockDriftNanos: Long,
        val syncAccuracyPercent: Double, val missedSyncEvents: Int, val lastUpdateTimestamp: Long
    )

    companion object {
        private const val TAG = "CrossModalSyncManager"
        private val instanceLock = Any()

        @Volatile
        private var instance: CrossModalSyncManager? = null
        fun getInstance(context: Context): CrossModalSyncManager? {
            if (instance == null) {
                synchronized(instanceLock) {
                    if (instance == null) {
                        instance = CrossModalSyncManager(context)
                    }
                }
            }
            return instance
        }
    }
}
