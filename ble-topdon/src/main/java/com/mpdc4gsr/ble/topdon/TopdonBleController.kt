package com.mpdc4gsr.ble.topdon

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
import com.mpdc4gsr.ble.core.util.BluetoothPermissionUtils
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TopdonBleController(private val context: Context, private val unifiedManager: UnifiedBleManager) {
    private val bluetoothAdapter: BluetoothAdapter?
    private val leScanner: BluetoothLeScanner?
    private val mainHandler: Handler

    private val isScanning = AtomicBoolean(false)
    private val connectedTopdonDevices: MutableList<TopdonDevice> = ArrayList<TopdonDevice>()
    private var currentScanListener: TopdonScanListener? = null
    private val topdonScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.getDevice()
                val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)

                if (deviceName != null && isTopdonDevice(deviceName)) {
                    val deviceType = determineTopdonDeviceType(deviceName, result.getScanRecord())

                    if (currentScanListener != null) {
                        currentScanListener!!.onTopdonDeviceFound(
                            device,
                            deviceType,
                            result.getRssi(),
                            if (result.getScanRecord() != null) result.getScanRecord()!!.getBytes() else ByteArray(0)
                        )
                    }

                    Log.d(TAG, "Found Topdon device: " + deviceName + " (" + deviceType + ") RSSI: " + result.getRssi())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Topdon scan result", e)
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
                currentScanListener!!.onScanError(errorCode, "Topdon BLE scan failed with error: " + errorCode)
            }
            Log.e(TAG, "Topdon BLE scan failed with error: " + errorCode)
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

            Log.i(TAG, "Topdon BLE Controller initialized with Nordic backend")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Topdon BLE Controller", e)
            return false
        }
    }

    fun startDeviceDiscovery(listener: TopdonScanListener): Boolean {
        if (isScanning.get()) {
            Log.w(TAG, "Topdon scan already in progress")
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
                leScanner!!.startScan(null, scanSettings, topdonScanCallback)
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

            Log.i(TAG, "Started Topdon device discovery with Nordic BLE backend")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Topdon device discovery", e)
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
                leScanner!!.stopScan(topdonScanCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during scan stop: " + e.message)
            }
            isScanning.set(false)
            currentScanListener = null

            Log.i(TAG, "Stopped Topdon device discovery")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Topdon device discovery", e)
        }
    }

    fun connectDevice(
        device: BluetoothDevice,
        config: TopdonDeviceConfig,
        listener: UnifiedBleManager.UnifiedConnectionListener
    ): UnifiedDevice? {
        try {
            Log.i(TAG, "Connecting to Topdon device: " + device.getAddress())

            val topdonDevice = TopdonDevice(device, config, listener)
            topdonDevice.connect()

            connectedTopdonDevices.add(topdonDevice)

            Log.i(TAG, "Created Topdon connection for device: " + device.getAddress())
            return topdonDevice
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Topdon device", e)
            return null
        }
    }

    val connectedDevices: MutableList<TopdonDevice?>
        get() = ArrayList<TopdonDevice?>(connectedTopdonDevices)

    fun cleanup() {
        try {
            stopDeviceDiscovery()

            for (device in connectedTopdonDevices) {
                device.disconnect()
            }
            connectedTopdonDevices.clear()

            Log.i(TAG, "Topdon BLE Controller cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Topdon controller cleanup", e)
        }
    }

    private fun isTopdonDevice(deviceName: String?): Boolean {
        if (deviceName == null) return false

        for (pattern in SUPPORTED_DEVICE_PATTERNS) {
            if (deviceName.lowercase(Locale.getDefault()).contains(pattern!!.lowercase(Locale.getDefault()))) {
                return true
            }
        }
        return false
    }

    private fun determineTopdonDeviceType(deviceName: String, scanRecord: ScanRecord?): UnifiedBleManager.DeviceType {
        val name = deviceName.lowercase(Locale.getDefault())

        if (name.contains("tc001") || name.contains("tc-001") || name.contains("thermal")) {
            return UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL
        } else if (name.contains("env") || name.contains("sensor")) {
            return UnifiedBleManager.DeviceType.MPDC4GSR_ENV
        } else if (name.contains("multi")) {
            return UnifiedBleManager.DeviceType.MPDC4GSR_MULTI
        }

        return UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL
    }

    interface TopdonScanListener {
        fun onTopdonDeviceFound(
            device: BluetoothDevice?,
            type: UnifiedBleManager.DeviceType?,
            rssi: Int,
            scanRecord: ByteArray?
        )

        fun onScanError(errorCode: Int, message: String?)

        fun onScanComplete()
    }

    companion object {
        private const val TAG = "MPDC4GSRBleController"

        private val MPDC4GSR_SERVICE_UUID: UUID? = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val MPDC4GSR_DATA_CHAR_UUID: UUID? = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        private val MPDC4GSR_CMD_CHAR_UUID: UUID? = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

        private val MPDC4GSR_THERMAL_SERVICE_UUID: UUID? = UUID.fromString("12345678-1234-5678-9012-123456789ABC")
        private val MPDC4GSR_THERMAL_DATA_CHAR_UUID: UUID? = UUID.fromString("12345678-1234-5678-9012-123456789ABD")

        private val SUPPORTED_DEVICE_PATTERNS = arrayOf<String?>(
            "Topdon",
            "TC001",
            "TC-001",
            "TOPDON",
            "TopdonThermal",
            "TopdonEnv",
            "TopdonSensor"
        )

        private const val MPDC4GSR_START_THERMAL: Byte = 0x01
        private const val MPDC4GSR_STOP_THERMAL: Byte = 0x02
        private const val MPDC4GSR_GET_TEMP_RANGE: Byte = 0x03
        private const val MPDC4GSR_SET_TEMP_RANGE: Byte = 0x04
        private const val MPDC4GSR_CALIBRATE: Byte = 0x05
    }
}
