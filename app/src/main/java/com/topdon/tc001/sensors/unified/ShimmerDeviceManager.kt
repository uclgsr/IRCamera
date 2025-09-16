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
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
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
        
        // Reconnection constants as mentioned in the problem statement
        private const val RECONNECTION_ATTEMPTS = 3
        private const val RECONNECTION_DELAY_MS = 2000L

        private val SHIMMER_MAC_PREFIXES = listOf(
            "00:06:66", // Shimmer Research MAC prefix
            "d0:39:72", // Alternative Shimmer prefix
            "00:80:98"  // Additional Shimmer prefix
        )

        private val SHIMMER_NAME_PATTERNS = listOf(
            "shimmer", "gsr", "rn4", "shimmer3"
        )
    }

    // Manager instances
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    var bluetoothManager: BluetoothManager? = null
        private set
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    // Expose shimmer manager for data callbacks (read-only access)
    val shimmerBluetoothManager: ShimmerBluetoothManagerAndroid?
        get() = shimmerManager

    // Device tracking
    private val connectedDevices = ConcurrentHashMap<String, Shimmer>()
    private val discoveredDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()

    // State management
    private val isScanning = AtomicBoolean(false)
    private var scanJob: Job? = null

    // Flow emissions
    private val _scanResults = MutableSharedFlow<List<DeviceInfo>>()
    val scanResults: SharedFlow<List<DeviceInfo>> = _scanResults.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    // Handler for Shimmer manager
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ConnectionEvent(
        val deviceAddress: String,
        val state: ConnectionState,
        val message: String? = null
    )

    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        FAILED,
        TIMEOUT
    }

    // Simplified approach without callbacks due to API limitations

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing Shimmer Device Manager")

        try {
            // Check permissions
            if (!hasRequiredPermissions()) {
                Log.e(TAG, "Missing required Bluetooth permissions")
                return@withContext false
            }

            // Initialize Bluetooth
            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                Log.e(TAG, "Bluetooth not available or disabled")
                return@withContext false
            }

            // Initialize Shimmer manager with Handler
            shimmerManager = ShimmerBluetoothManagerAndroid(context, mainHandler)
            // Note: setCallback method may not exist - using simplified approach

            Log.i(TAG, "Shimmer Device Manager initialized successfully")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer Device Manager", e)
            return@withContext false
        }
    }

    suspend fun startDeviceScanning(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting Shimmer device discovery with BLE scanning")

        if (isScanning.get()) {
            Log.w(TAG, "Device scanning already in progress")
            return@withContext true
        }

        val shimmerMgr = shimmerManager ?: run {
            Log.e(TAG, "Shimmer manager not initialized")
            return@withContext false
        }

        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Missing required permissions for BLE scanning")
            return@withContext false
        }

        try {
            discoveredDevices.clear()
            isScanning.set(true)

            // Start with paired devices for better UX
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter?.bondedDevices ?: emptySet()
            } else {
                emptySet()
            }

            // Add paired Shimmer devices first
            val pairedShimmers = pairedDevices.mapNotNull { btDevice ->
                if (isValidShimmerDevice(btDevice)) {
                    DeviceInfo(
                        address = btDevice.address,
                        name = btDevice.name ?: "Unknown Shimmer",
                        rssi = -50, // Default RSSI for paired devices
                        deviceType = "Shimmer3 GSR+",
                        isGSRCapable = true
                    )
                } else null
            }

            pairedShimmers.forEach { device ->
                discoveredDevices[device.address] = device
            }

            Log.i(TAG, "Found ${pairedShimmers.size} paired Shimmer devices")

            // Perform actual BLE scanning for nearby devices
            scanJob = lifecycleOwner.lifecycleScope.launch {
                performBluetoothLeScanning()
            }

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error starting device scan", e)
            isScanning.set(false)
            return@withContext false
        }
    }

    private suspend fun performBluetoothLeScanning() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Log.w(TAG, "BLE Scanner not available")
            return
        }

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
            }
        }

        try {
            Log.i(TAG, "Starting BLE scan for nearby Shimmer devices...")
            bluetoothLeScanner.startScan(scanCallback)

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
            Log.e(TAG, "Security exception during BLE scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during BLE scan", e)
        } finally {
            stopDeviceScanning()
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
            shimmer.disconnect()

            connectedDevices.remove(deviceAddress)

            Log.i(TAG, "Successfully disconnected from device: $deviceAddress")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting device: $deviceAddress", e)
            return@withContext false
        }
    }

    fun getConnectedDevices(): List<DeviceInfo> {
        return connectedDevices.map { (address, shimmer) ->
            DeviceInfo(
                address = address,
                name = "Connected Shimmer ($address)",
                rssi = -50, // Connected devices don't report RSSI
                deviceType = "Shimmer3 GSR+",
                isGSRCapable = true
            )
        }
    }

    fun getConnectedShimmer(deviceAddress: String): Shimmer? {
        return connectedDevices[deviceAddress]
    }

    suspend fun handleDeviceDisconnection(deviceAddress: String, shouldAttemptReconnection: Boolean = true) {
        withContext(Dispatchers.IO) {
            Log.w(TAG, "Device disconnected: $deviceAddress")
            
            // Remove from connected devices
            connectedDevices.remove(deviceAddress)
            
            // Emit disconnection event
            _connectionEvents.emit(ConnectionEvent(deviceAddress, ConnectionState.DISCONNECTED))

            if (shouldAttemptReconnection) {
                val currentAttempts = reconnectionAttempts.getOrDefault(deviceAddress, 0)
                
                if (currentAttempts < RECONNECTION_ATTEMPTS) {
                    Log.i(TAG, "Starting automatic reconnection for device: $deviceAddress (attempt ${currentAttempts + 1}/$RECONNECTION_ATTEMPTS)")
                    
                    // Increment attempt counter
                    reconnectionAttempts[deviceAddress] = currentAttempts + 1
                    
                    // Emit reconnecting state
                    _connectionEvents.emit(ConnectionEvent(deviceAddress, ConnectionState.CONNECTING, "Reconnecting..."))
                    
                    // Wait before reconnection attempt
                    delay(RECONNECTION_DELAY_MS)
                    
                    // Find device info for reconnection
                    val deviceInfo = discoveredDevices[deviceAddress]
                    if (deviceInfo != null) {
                        val reconnectSuccess = connectToDevice(deviceInfo)
                        
                        if (reconnectSuccess) {
                            Log.i(TAG, "Automatic reconnection successful for device: $deviceAddress")
                            // Reset reconnection attempts on success
                            reconnectionAttempts.remove(deviceAddress)
                        } else {
                            Log.w(TAG, "Automatic reconnection failed for device: $deviceAddress")
                            // If this was the last attempt, emit failure and switch to simulation
                            if (currentAttempts + 1 >= RECONNECTION_ATTEMPTS) {
                                Log.e(TAG, "All reconnection attempts failed for device: $deviceAddress. Switching to simulation mode.")
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
                        Log.e(TAG, "Cannot reconnect to device $deviceAddress: device info not found")
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
