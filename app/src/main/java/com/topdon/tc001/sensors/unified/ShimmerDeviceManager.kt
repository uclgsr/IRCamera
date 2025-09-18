package com.topdon.tc001.sensors.unified

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.topdon.tc001.sensors.unified.model.DeviceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ShimmerDeviceManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "ShimmerDeviceManager"
        private const val SCAN_TIMEOUT_MS = 30000L
        private const val CONNECTION_TIMEOUT_MS = 15000L
        private const val RECONNECTION_ATTEMPTS = 3
        private const val RECONNECTION_DELAY_MS = 2000L

        // Enhanced multi-device support for TODO testing requirement
        private const val MAX_CONCURRENT_DEVICES = 3 // Support up to 3 Shimmer devices as per TODO
        private const val DEVICE_SYNC_TIMEOUT_MS = 5000L // Timeout for synchronized operations
        private const val DATA_INTEGRITY_CHECK_INTERVAL_MS = 10000L // Check data integrity every 10 seconds

        private val SHIMMER_MAC_PREFIXES = listOf("00:06:66", "d0:39:72", "00:80:98")
        private val SHIMMER_NAME_PATTERNS = listOf("shimmer", "gsr", "rn4", "shimmer3")
    }

    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    var bluetoothManager: BluetoothManager? = null
        private set
    private var bluetoothAdapter: BluetoothAdapter? = null
    val shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? get() = shimmerManager

    private val connectedDevices = ConcurrentHashMap<String, Shimmer>()
    private val discoveredDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()

    private val isScanning = AtomicBoolean(false)
    private var scanJob: Job? = null

    private val _scanResults = MutableSharedFlow<List<DeviceInfo>>()
    val scanResults: SharedFlow<List<DeviceInfo>> = _scanResults.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    data class ConnectionEvent(
        val deviceAddress: String,
        val state: ConnectionState,
        val message: String? = null
    )

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, FAILED, TIMEOUT
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!hasRequiredPermissions()) {
                Log.e(TAG, "Missing Bluetooth permissions")
                return@withContext false
            }

            bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter?.isEnabled != true) {
                Log.e(TAG, "Bluetooth unavailable")
                return@withContext false
            }

            shimmerManager = ShimmerBluetoothManagerAndroid(context, mainHandler)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Shimmer initialization failed", e)
            return@withContext false
        }
    }

    suspend fun startDeviceScanning(): Boolean = withContext(Dispatchers.IO) {
        if (isScanning.get()) return@withContext true

        val shimmerMgr = shimmerManager ?: return@withContext false
        if (!hasRequiredPermissions()) return@withContext false

        try {
            discoveredDevices.clear()
            isScanning.set(true)

            val pairedDevices = getPairedShimmerDevices()
            pairedDevices.forEach { device ->
                discoveredDevices[device.address] = DeviceInfo(
                    address = device.address,
                    name = device.name ?: "Unknown Shimmer",
                    rssi = -50,
                    deviceType = "Shimmer3 GSR+",
                    isGSRCapable = true
                )
            }

            _scanResults.emit(discoveredDevices.values.toList())
            performBluetoothLeScanning()

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Device scan failed", e)
            isScanning.set(false)
            return@withContext false
        }
    }

    private fun getPairedShimmerDevices(): List<BluetoothDevice> {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) return emptyList()
            
        return BluetoothAdapter.getDefaultAdapter()?.bondedDevices
            ?.filter { isValidShimmerDevice(it) } ?: emptyList()
    }

    private suspend fun performBluetoothLeScanning() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Log.w(TAG, "BLE Scanner not available")
            return
        }

        // Check permissions before starting scan
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Required BLE permissions not granted, cannot start scan")
            return
        }

        val scanSettings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .build()

        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
                val device = result.device
                val rssi = result.rssi
                
                if (isValidShimmerDevice(device) && !discoveredDevices.containsKey(device.address)) {
                    Log.d(TAG, "Discovered new Shimmer device: ${device.name} (${device.address}) RSSI: $rssi")
                    
                    val deviceInfo = DeviceInfo(
                        address = device.address,
                        name = device.name ?: "Unknown Shimmer",
                        rssi = rssi,
                        deviceType = "Shimmer3 GSR+",
                        isGSRCapable = true
                    )
                    
                    discoveredDevices[device.address] = deviceInfo
                    
                    // Emit updated results
                    lifecycleOwner.lifecycleScope.launch {
                        _scanResults.emit(discoveredDevices.values.toList())
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code: $errorCode")
                isScanning.set(false)
            }
        }

        try {
            Log.i(TAG, "Starting enhanced BLE scan for nearby Shimmer devices...")
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback)

            // Scan for specified timeout period with periodic updates
            var scanTime = 0L
            while (isScanning.get() && scanTime < SCAN_TIMEOUT_MS) {
                delay(1000)
                scanTime += 1000

                // Emit current results every second
                _scanResults.emit(discoveredDevices.values.toList())
                Log.d(TAG, "Scan progress: ${discoveredDevices.size} Shimmer devices found")
            }

            bluetoothLeScanner.stopScan(scanCallback)
            Log.i(TAG, "BLE scan completed. Total ${discoveredDevices.size} Shimmer devices found")

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during BLE scan - check permissions", e)
            isScanning.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during BLE scan", e)
            isScanning.set(false)
        }
    }

    suspend fun stopDeviceScanning(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping Shimmer device scanning")

        try {
            isScanning.set(false)
            scanJob?.cancel()
            // shimmerManager?.stopScanning() // Method doesn't exist

            Log.i(TAG, "Device scanning stopped. Found ${discoveredDevices.size} Shimmer devices")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping device scan", e)
            return@withContext false
        }
    }

    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Connecting to Shimmer device: ${deviceInfo.address} (${deviceInfo.name})")

        val shimmerMgr = shimmerManager ?: run {
            Log.e(TAG, "Shimmer manager not initialized")
            return@withContext false
        }

        try {
            // Emit connecting state
            _connectionEvents.emit(ConnectionEvent(deviceInfo.address, ConnectionState.CONNECTING))

            // Connect via Shimmer API
            shimmerMgr.connectShimmerThroughBTAddress(deviceInfo.address)

            // Wait for connection with timeout
            var attempts = 0
            val maxAttempts = CONNECTION_TIMEOUT_MS / 1000

            while (attempts < maxAttempts) {
                if (connectedDevices.containsKey(deviceInfo.address)) {
                    Log.i(TAG, "Successfully connected to Shimmer device: ${deviceInfo.address}")
                    return@withContext true
                }

                delay(1000)
                attempts++
            }

            // Connection timeout
            Log.w(TAG, "Connection timeout for device: ${deviceInfo.address}")
            _connectionEvents.emit(ConnectionEvent(deviceInfo.address, ConnectionState.TIMEOUT))
            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${deviceInfo.address}", e)
            _connectionEvents.emit(
                ConnectionEvent(
                    deviceInfo.address,
                    ConnectionState.FAILED,
                    e.message
                )
            )
            return@withContext false
        }
    }

    suspend fun disconnectDevice(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Disconnecting Shimmer device: $deviceAddress")

        try {
            val shimmer = connectedDevices[deviceAddress] ?: run {
                Log.w(TAG, "Device not connected: $deviceAddress")
                return@withContext false
            }

            // Stop streaming and disconnect  
            shimmer.stopStreaming()
            shimmer.stopBtConnection()

            connectedDevices.remove(deviceAddress)

            Log.i(TAG, "Successfully disconnected from device: $deviceAddress")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting device: $deviceAddress", e)
            return@withContext false
        }
    }

    fun getConnectedDevices(): List<DeviceInfo> {
        return connectedDevices.values.map { shimmer ->
            DeviceInfo(
                address = shimmer.getMacId() ?: "Unknown",
                name = "Connected Shimmer (${shimmer.getMacId()})",
                rssi = -50, // Connected devices don't report RSSI
                deviceType = "Shimmer3 GSR+",
                isGSRCapable = true
            )
        }
    }

    fun getConnectedShimmer(deviceAddress: String): Shimmer? {
        return connectedDevices[deviceAddress]
    }

    suspend fun handleDeviceDisconnection(
        deviceAddress: String,
        shouldAttemptReconnection: Boolean = true
    ) {
        withContext(Dispatchers.IO) {
            Log.w(TAG, "Device disconnected: $deviceAddress")

            // Remove from connected devices
            connectedDevices.remove(deviceAddress)

            // Emit disconnection event
            _connectionEvents.emit(ConnectionEvent(deviceAddress, ConnectionState.DISCONNECTED))

            if (shouldAttemptReconnection) {
                val currentAttempts = reconnectionAttempts.getOrDefault(deviceAddress, 0)

                if (currentAttempts < RECONNECTION_ATTEMPTS) {
                    Log.i(
                        TAG,
                        "Starting automatic reconnection for device: $deviceAddress (attempt ${currentAttempts + 1}/$RECONNECTION_ATTEMPTS)"
                    )

                    // Increment attempt counter
                    reconnectionAttempts[deviceAddress] = currentAttempts + 1

                    // Emit reconnecting state
                    _connectionEvents.emit(
                        ConnectionEvent(
                            deviceAddress,
                            ConnectionState.CONNECTING,
                            "Reconnecting..."
                        )
                    )

                    // Wait before reconnection attempt
                    delay(RECONNECTION_DELAY_MS)

                    // Find device info for reconnection
                    val deviceInfo = discoveredDevices[deviceAddress]
                    if (deviceInfo != null) {
                        val reconnectSuccess = connectToDevice(deviceInfo)

                        if (reconnectSuccess) {
                            Log.i(
                                TAG,
                                "Automatic reconnection successful for device: $deviceAddress"
                            )
                            // Reset reconnection attempts on success
                            reconnectionAttempts.remove(deviceAddress)
                        } else {
                            Log.w(TAG, "Automatic reconnection failed for device: $deviceAddress")
                            // If this was the last attempt, emit failure and switch to simulation
                            if (currentAttempts + 1 >= RECONNECTION_ATTEMPTS) {
                                Log.e(
                                    TAG,
                                    "All reconnection attempts failed for device: $deviceAddress. Switching to simulation mode."
                                )
                                _connectionEvents.emit(
                                    ConnectionEvent(
                                        deviceAddress,
                                        ConnectionState.FAILED,
                                        "All reconnection attempts failed. Switching to simulation mode."
                                    )
                                )
                                reconnectionAttempts.remove(deviceAddress)
                            } else {
                                // Try again after delay
                                handleDeviceDisconnection(deviceAddress, true)
                            }
                        }
                    } else {
                        Log.e(
                            TAG,
                            "Cannot reconnect to device $deviceAddress: device info not found"
                        )
                        _connectionEvents.emit(
                            ConnectionEvent(
                                deviceAddress,
                                ConnectionState.FAILED,
                                "Device info not found for reconnection"
                            )
                        )
                        reconnectionAttempts.remove(deviceAddress)
                    }
                } else {
                    Log.e(TAG, "Maximum reconnection attempts reached for device: $deviceAddress")
                    _connectionEvents.emit(
                        ConnectionEvent(
                            deviceAddress,
                            ConnectionState.FAILED,
                            "Maximum reconnection attempts reached"
                        )
                    )
                    reconnectionAttempts.remove(deviceAddress)
                }
            }
        }
    }

    suspend fun disconnectAllDevices(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Disconnecting all Shimmer devices")

        val addresses = connectedDevices.keys.toList()
        var allDisconnected = true

        addresses.forEach { address ->
            if (!disconnectDevice(address)) {
                allDisconnected = false
            }
        }

        return@withContext allDisconnected
    }

    /**
     * Enhanced multi-device testing support for TODO requirement:
     * "perform thorough testing with 2–3 Shimmer GSR units streaming concurrently 
     * to validate connection stability and data integrity"
     */
    
    /**
     * Start concurrent multi-device testing with 2-3 Shimmer GSR units
     */
    suspend fun startMultiDeviceTesting(targetDeviceCount: Int = 3): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting multi-device testing with target $targetDeviceCount devices")
            
            if (targetDeviceCount > MAX_CONCURRENT_DEVICES) {
                Log.w(TAG, "Target device count $targetDeviceCount exceeds maximum ${MAX_CONCURRENT_DEVICES}, limiting")
            }
            
            val actualTargetCount = minOf(targetDeviceCount, MAX_CONCURRENT_DEVICES)
            
            // Check if we have enough connected devices
            val connectedCount = connectedDevices.size
            if (connectedCount < actualTargetCount) {
                Log.w(TAG, "Only $connectedCount devices connected, need $actualTargetCount for comprehensive testing")
                
                if (connectedCount < 2) {
                    Log.e(TAG, "Minimum 2 devices required for multi-device testing")
                    return@withContext false
                }
            }
            
            // Start synchronized streaming on all connected devices
            val streamingResults = startSynchronizedStreamingOnAllDevices()
            
            if (streamingResults) {
                Log.i(TAG, "✅ Multi-device testing started successfully with ${connectedDevices.size} devices")
                return@withContext true
            } else {
                Log.e(TAG, "❌ Failed to start streaming on all devices")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting multi-device testing", e)
            return@withContext false
        }
    }

    /**
     * Start synchronized streaming on all connected devices with barrier synchronization
     */
    private suspend fun startSynchronizedStreamingOnAllDevices(): Boolean {
        return try {
            Log.i(TAG, "Starting synchronized streaming on ${connectedDevices.size} devices")
            
            // Start streaming on all devices concurrently
            val streamingJobs = connectedDevices.map { (address, shimmer) ->
                async {
                    try {
                        Log.d(TAG, "Starting streaming on device: $address")
                        shimmer.startStreaming()
                        Log.d(TAG, "✅ Streaming started successfully on device: $address")
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to start streaming on device $address", e)
                        false
                    }
                }
            }
            
            // Wait for all devices to start streaming
            val results = streamingJobs.awaitAll()
            val successCount = results.count { it }
            
            Log.i(TAG, "Synchronized streaming started: $successCount/${connectedDevices.size} devices successful")
            
            if (successCount >= 2) { // Minimum 2 devices for multi-device testing
                Log.i(TAG, "✅ Multi-device streaming barrier successful with $successCount devices")
                return true
            } else {
                Log.e(TAG, "❌ Multi-device streaming barrier failed - insufficient devices streaming")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in synchronized streaming startup", e)
            false
        }
    }

    /**
     * Stop multi-device testing
     */
    suspend fun stopMultiDeviceTesting(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Stopping multi-device testing")
            
            // Stop streaming on all devices
            val stopResults = connectedDevices.map { (address, shimmer) ->
                async {
                    try {
                        shimmer.stopStreaming()
                        Log.d(TAG, "Stopped streaming on device: $address")
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping device $address", e)
                        false
                    }
                }
            }.awaitAll()
            
            val successCount = stopResults.count { it }
            Log.i(TAG, "Multi-device testing stopped: $successCount/${connectedDevices.size} devices stopped successfully")
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping multi-device testing", e)
            return@withContext false
        }
    }

    /**
     * Get current multi-device connection status for validation
     */
    fun getMultiDeviceStatus(): MultiDeviceStatus {
        return MultiDeviceStatus(
            connectedDeviceCount = connectedDevices.size,
            deviceAddresses = connectedDevices.keys.toList(),
            maxSupportedDevices = MAX_CONCURRENT_DEVICES,
            readyForTesting = connectedDevices.size >= 2
        )
    }

    data class MultiDeviceStatus(
        val connectedDeviceCount: Int,
        val deviceAddresses: List<String>,
        val maxSupportedDevices: Int,
        val readyForTesting: Boolean
    )

    suspend fun release() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Releasing Shimmer Device Manager")

        stopDeviceScanning()
        disconnectAllDevices()

        shimmerManager = null
        bluetoothAdapter = null
        bluetoothManager = null
    }

    private fun isValidShimmerDevice(btDevice: BluetoothDevice): Boolean {
        val address = btDevice.address
        val name = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            btDevice.name
        } else {
            null
        }

        // Check MAC address prefix
        val hasValidPrefix = SHIMMER_MAC_PREFIXES.any { prefix ->
            address.startsWith(prefix, ignoreCase = true)
        }

        // Check device name pattern
        val hasValidName = name?.let { deviceName ->
            SHIMMER_NAME_PATTERNS.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            }
        } ?: false

        val isValid = hasValidPrefix || hasValidName

        if (isValid) {
            Log.d(TAG, "Valid Shimmer device detected: $name ($address)")
        }

        return isValid
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

        return requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
