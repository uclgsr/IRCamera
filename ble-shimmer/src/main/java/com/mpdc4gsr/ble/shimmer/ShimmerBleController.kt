package com.mpdc4gsr.ble.shimmer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mpdc4gsr.ble.core.UnifiedBleManager.ShimmerScanCallback
import com.mpdc4gsr.ble.core.util.BluetoothPermissionUtils
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ShimmerBleController(private val context: Context, private val unifiedManager: UnifiedBleManager) {
    private val bluetoothAdapter: BluetoothAdapter?
    private val leScanner: BluetoothLeScanner?
    private val mainHandler: Handler

    private val isScanning = AtomicBoolean(false)
    private val connectedShimmerDevices: MutableList<ShimmerDevice> = ArrayList<ShimmerDevice>()
    private var currentScanListener: ShimmerScanListener? = null
    private val shimmerScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.getDevice()
                val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)

                if (deviceName != null && isShimmerDevice(deviceName)) {
                    val deviceType = determineShimmerDeviceType(deviceName, result.getScanRecord())

                    if (currentScanListener != null) {
                        currentScanListener!!.onShimmerDeviceFound(
                            device,
                            deviceType,
                            result.getRssi(),
                            if (result.getScanRecord() != null) result.getScanRecord()!!.getBytes() else ByteArray(0)
                        )
                    }

                    Log.d(
                        TAG,
                        "Found Shimmer device: " + deviceName + " (" + deviceType + ") RSSI: " + result.getRssi()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Shimmer scan result", e)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (result in results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning.set(false)
            if (currentScanListener != null) {
                currentScanListener!!.onScanError(errorCode, "Shimmer BLE scan failed with error: " + errorCode)
            }
            Log.e(TAG, "Shimmer BLE scan failed with error: " + errorCode)
        }
    }

    init {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        this.leScanner = if (bluetoothAdapter != null) bluetoothAdapter.getBluetoothLeScanner() else null
        this.mainHandler = Handler(Looper.getMainLooper())
    }

    fun initialize(): Boolean {
        try {
            if (bluetoothAdapter == null || leScanner == null) {
                Log.e(TAG, "Bluetooth LE not supported")
                return false
            }

            if (!bluetoothAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth not enabled")
                return false
            }

            Log.i(TAG, "Shimmer BLE Controller initialized with Nordic backend")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer BLE Controller", e)
            return false
        }
    }

    fun startDeviceDiscovery(listener: ShimmerScanListener): Boolean {
        if (isScanning.get()) {
            Log.w(TAG, "Shimmer scan already in progress")
            return false
        }

        try {
            if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for device discovery")
                return false
            }

            this.currentScanListener = listener

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0)
                .build()

            isScanning.set(true)
            try {
                leScanner!!.startScan(null, scanSettings, shimmerScanCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during scan start: " + e.message)
                isScanning.set(false)
                return false
            }

            mainHandler.postDelayed(Runnable {
                if (isScanning.get()) {
                    stopDeviceDiscovery()
                    if (currentScanListener != null) {
                        currentScanListener!!.onScanComplete()
                    }
                }
            }, 30000)

            Log.i(TAG, "Started Shimmer device discovery with Nordic BLE backend")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Shimmer device discovery", e)
            isScanning.set(false)
            return false
        }
    }

    fun stopDeviceDiscovery() {
        if (!isScanning.get()) {
            return
        }

        try {
            if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for stopping scan")
                isScanning.set(false)
                return
            }

            try {
                leScanner!!.stopScan(shimmerScanCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during scan stop: " + e.message)
            }
            isScanning.set(false)
            currentScanListener = null

            Log.i(TAG, "Stopped Shimmer device discovery")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Shimmer device discovery", e)
        }
    }

    fun connectDevice(
        device: BluetoothDevice,
        config: ShimmerDeviceConfig,
        listener: UnifiedBleManager.UnifiedConnectionListener
    ): UnifiedDevice? {
        try {
            Log.i(TAG, "Connecting to Shimmer device: " + device.getAddress())

            val shimmerDevice = ShimmerDevice(device, config, listener)
            shimmerDevice.connect()

            connectedShimmerDevices.add(shimmerDevice)

            Log.i(TAG, "Created Shimmer connection for device: " + device.getAddress())
            return shimmerDevice
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Shimmer device", e)
            return null
        }
    }


    fun connectAndInitializeDevice(
        device: BluetoothDevice,
        config: ShimmerDeviceConfig,
        listener: UnifiedBleManager.UnifiedConnectionListener,
        gsrRange: Int,
        samplingRate: Int
    ): UnifiedDevice? {
        try {
            Log.i(TAG, "Connecting and initializing Shimmer device: " + device.getAddress())


            val shimmerDevice = connectDevice(device, config, listener)
            if (shimmerDevice == null) {
                Log.e(TAG, "Failed to establish basic connection")
                return null
            }


            val maxWaitAttempts = 10
            var waitAttempts = 0
            while (!shimmerDevice.isConnected() && waitAttempts < maxWaitAttempts) {
                try {
                    Thread.sleep(500)
                    waitAttempts++
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }

            if (!shimmerDevice.isConnected()) {
                Log.e(TAG, "Device connection timeout")
                return shimmerDevice
            }


            if (shimmerDevice is ShimmerDevice) {
                val shimmer = shimmerDevice
                val initialized = shimmer.initializeForGSRRecording(gsrRange, samplingRate)

                if (initialized) {
                    Log.i(TAG, "Successfully connected and initialized Shimmer device: " + device.getAddress())
                } else {
                    Log.w(TAG, "Device connected but initialization incomplete: " + device.getAddress())
                }
            }

            return shimmerDevice
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect and initialize Shimmer device", e)
            return null
        }
    }

    val connectedDevices: MutableList<ShimmerDevice?>
        get() = ArrayList<ShimmerDevice?>(connectedShimmerDevices)


    fun scanForDevices(scanDurationMs: Long, callback: ShimmerScanCallback?) {
        if (callback == null) {
            Log.e(TAG, "Scan callback cannot be null")
            return
        }

        val foundDevices: MutableList<UnifiedDevice?> = ArrayList<UnifiedDevice?>()

        val scanListener: ShimmerScanListener = object : ShimmerScanListener {
            override fun onShimmerDeviceFound(
                device: BluetoothDevice,
                type: UnifiedBleManager.DeviceType?,
                rssi: Int,
                scanRecord: ByteArray?
            ) {
                try {
                    val unifiedDevice = createUnifiedDeviceFromBluetooth(device, type, rssi, scanRecord)
                    foundDevices.add(unifiedDevice)
                    callback.onDeviceFound(unifiedDevice)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating unified device from scan result", e)
                }
            }

            override fun onScanError(errorCode: Int, message: String?) {
                callback.onScanFailed(message)
            }

            override fun onScanComplete() {
                callback.onScanComplete(foundDevices)
            }
        }


        if (startDeviceDiscovery(scanListener)) {
            mainHandler.postDelayed(Runnable {
                if (isScanning.get()) {
                    stopDeviceDiscovery()
                    scanListener.onScanComplete()
                }
            }, scanDurationMs)
        } else {
            callback.onScanFailed("Failed to start BLE scan")
        }
    }


    private fun createUnifiedDeviceFromBluetooth(
        device: BluetoothDevice,
        type: UnifiedBleManager.DeviceType?,
        rssi: Int,
        scanRecord: ByteArray?
    ): UnifiedDevice? {
        try {
            val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)


            val config = ShimmerDeviceConfig.Builder()
                .setDeviceType(type)
                .setSamplingRate(128)
                .setConnectionTimeout(15000)
                .build()

            return ShimmerDevice(device, config, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating UnifiedDevice from BluetoothDevice", e)
            return null
        }
    }

    fun cleanup() {
        try {
            stopDeviceDiscovery()

            for (device in connectedShimmerDevices) {
                device.disconnect()
            }
            connectedShimmerDevices.clear()

            Log.i(TAG, "Shimmer BLE Controller cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Shimmer controller cleanup", e)
        }
    }

    private fun isShimmerDevice(deviceName: String?): Boolean {
        if (deviceName == null) return false

        for (pattern in SHIMMER_DEVICE_PATTERNS) {
            if (deviceName.lowercase(Locale.getDefault()).contains(pattern!!.lowercase(Locale.getDefault()))) {
                return true
            }
        }

        return false
    }

    private fun determineShimmerDeviceType(deviceName: String, scanRecord: ScanRecord?): UnifiedBleManager.DeviceType {
        val name = deviceName.lowercase(Locale.getDefault())

        if (name.contains("gsr")) {
            return UnifiedBleManager.DeviceType.SHIMMER_GSR
        } else if (name.contains("ppg")) {
            return UnifiedBleManager.DeviceType.SHIMMER_PPG
        } else if (name.contains("imu")) {
            return UnifiedBleManager.DeviceType.SHIMMER_IMU
        }

        return UnifiedBleManager.DeviceType.SHIMMER_GSR
    }

    interface ShimmerScanListener {
        fun onShimmerDeviceFound(
            device: BluetoothDevice?,
            type: UnifiedBleManager.DeviceType?,
            rssi: Int,
            scanRecord: ByteArray?
        )

        fun onScanError(errorCode: Int, message: String?)

        fun onScanComplete()
    }

    companion object {
        private const val TAG = "ShimmerBleController"


        private val SHIMMER_SERVICE_UUID: UUID? = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
        private val SHIMMER_DATA_CHAR_UUID: UUID? = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")
        private val SHIMMER_CMD_CHAR_UUID: UUID? = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3")

        private val SHIMMER_DEVICE_PATTERNS = arrayOf<String?>(
            "Shimmer",
            "ShimmerGSR",
            "ShimmerPPG",
            "ShimmerIMU",
            "Shimmer3",
            "Shimmer4"
        )

        private const val SHIMMER_START_STREAMING: Byte = 0x07
        private const val SHIMMER_STOP_STREAMING: Byte = 0x20
        private const val SHIMMER_GET_SAMPLING_RATE: Byte = 0x03
        private const val SHIMMER_SET_SAMPLING_RATE: Byte = 0x05
        private const val SHIMMER_GET_STATUS: Byte = 0x25
        private const val SHIMMER_GET_FW_VERSION: Byte = 0x2E
        private const val SHIMMER_SET_GSR_RANGE: Byte = 0x60
        private const val SHIMMER_GET_GSR_RANGE: Byte = 0x61
    }
}
