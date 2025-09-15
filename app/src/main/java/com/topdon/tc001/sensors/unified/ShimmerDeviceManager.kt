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
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.topdon.tc001.sensors.unified.model.DeviceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ShimmerDeviceManager - Complete device management system for Shimmer3 GSR+ sensors
 * 
 * Features:
 * - MAC address filtering (00:06:66, d0:39:72, 00:80:98 prefixes)  
 * - BLE scanning via ShimmerBluetoothManagerAndroid.startScanBtDevices()
 * - Device validation with name pattern matching (Shimmer3-GSR, GSRShimmer)
 * - Connection management with shimmerManager.connectShimmerThroughBTAddress()
 * - Callback-based event handling for connection state changes
 */
class ShimmerDeviceManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "ShimmerDeviceManager"
        
        // Official Shimmer MAC prefixes for device filtering
        private val SHIMMER_MAC_PREFIXES = listOf(
            "00:06:66", // Shimmer Research official prefix
            "d0:39:72", // Secondary Shimmer prefix
            "00:80:98"  // Additional Shimmer prefix
        )
        
        // Shimmer device name patterns for validation
        private val SHIMMER_NAME_PATTERNS = listOf(
            "Shimmer3-GSR",
            "GSRShimmer", 
            "Shimmer_",
            "SHM_"
        )
        
        private const val SCAN_TIMEOUT_MS = 10000L // 10 seconds
        private const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    
    private val discoveredDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val connectedDevices = ConcurrentHashMap<String, Shimmer>()
    
    private val _scanResults = MutableSharedFlow<List<DeviceInfo>>()
    val scanResults: SharedFlow<List<DeviceInfo>> = _scanResults.asSharedFlow()
    
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    
    private val isScanning = AtomicBoolean(false)
    private var scanJob: Job? = null
    
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
    
    // Shimmer callback handler for connection events and data
    private val shimmerCallback = object : CallbackObject() {
        override fun onBluetoothConnectionStateChanged(
            state: BT_STATE?,
            bluetoothAddress: String?
        ) {
            super.onBluetoothConnectionStateChanged(state, bluetoothAddress)
            
            bluetoothAddress?.let { address ->
                val connectionState = when (state) {
                    BT_STATE.CONNECTED -> {
                        Log.i(TAG, "Shimmer device connected: $address")
                        ConnectionState.CONNECTED
                    }
                    BT_STATE.CONNECTING -> {
                        Log.i(TAG, "Connecting to Shimmer device: $address")
                        ConnectionState.CONNECTING
                    }
                    BT_STATE.DISCONNECTED -> {
                        Log.i(TAG, "Shimmer device disconnected: $address")
                        connectedDevices.remove(address)
                        ConnectionState.DISCONNECTED
                    }
                    else -> {
                        Log.w(TAG, "Unknown connection state for device: $address")
                        ConnectionState.FAILED
                    }
                }
                
                lifecycleOwner.lifecycleScope.launch {
                    _connectionEvents.emit(ConnectionEvent(address, connectionState))
                }
            }
        }
        
        override fun onNewShimmerDeviceCallback(shimmerDevice: ShimmerDevice?) {
            super.onNewShimmerDeviceCallback(shimmerDevice)
            
            shimmerDevice?.let { device ->
                if (device is Shimmer) {
                    val address = device.bluetoothAddress
                    connectedDevices[address] = device
                    Log.i(TAG, "New Shimmer device registered: ${device.shimmerUserAssignedName} ($address)")
                }
            }
        }
        
        override fun onNewObjectCluster(bluetoothAddress: String?, objectCluster: ObjectCluster?) {
            super.onNewObjectCluster(bluetoothAddress, objectCluster)
            // Handle real-time data streaming - delegated to individual recorders
        }
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing Shimmer Device Manager")
        
        try {
            // Check permissions
            if (!hasRequiredPermissions()) {
                Log.e(TAG, "Missing required Bluetooth permissions")
                return@withContext false
            }
            
            // Initialize Bluetooth
            bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
            
            if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                Log.e(TAG, "Bluetooth not available or disabled")
                return@withContext false
            }
            
            // Initialize Shimmer manager
            shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerCallback)
            
            Log.i(TAG, "Shimmer Device Manager initialized successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer Device Manager", e)
            return@withContext false
        }
    }
    
    /**
     * Start BLE scanning for Shimmer devices with MAC filtering
     * Uses shimmerManager.startScanBtDevices() for device discovery
     */
    suspend fun startDeviceScanning(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting Shimmer device discovery with MAC filtering")
        
        if (isScanning.get()) {
            Log.w(TAG, "Device scanning already in progress")
            return@withContext true
        }
        
        val shimmerMgr = shimmerManager ?: run {
            Log.e(TAG, "Shimmer manager not initialized")
            return@withContext false
        }
        
        try {
            discoveredDevices.clear()
            isScanning.set(true)
            
            // Start BLE scanning via Shimmer API
            shimmerMgr.startScanBtDevices()
            
            // Start discovery monitoring job
            scanJob = lifecycleOwner.lifecycleScope.launch {
                var scanTime = 0L
                while (isScanning.get() && scanTime < SCAN_TIMEOUT_MS) {
                    delay(1000)
                    scanTime += 1000
                    
                    // Get discovered devices from Shimmer manager
                    val detectedDevices = shimmerMgr.btDevicesMap?.values?.mapNotNull { btDevice ->
                        if (isValidShimmerDevice(btDevice)) {
                            DeviceInfo(
                                address = btDevice.address,
                                name = btDevice.name ?: "Unknown Shimmer",
                                rssi = -50, // Default RSSI - actual value from scan callback
                                isConnectable = true,
                                deviceType = "Shimmer3 GSR+"
                            )
                        } else null
                    } ?: emptyList()
                    
                    // Update discovered devices map
                    detectedDevices.forEach { device ->
                        discoveredDevices[device.address] = device
                    }
                    
                    // Emit current results
                    _scanResults.emit(discoveredDevices.values.toList())
                    
                    Log.d(TAG, "Scan progress: ${discoveredDevices.size} Shimmer devices found")
                }
                
                stopDeviceScanning()
            }
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting device scan", e)
            isScanning.set(false)
            return@withContext false
        }
    }
    
    suspend fun stopDeviceScanning(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping Shimmer device scanning")
        
        try {
            isScanning.set(false)
            scanJob?.cancel()
            shimmerManager?.stopScanBtDevices()
            
            Log.i(TAG, "Device scanning stopped. Found ${discoveredDevices.size} Shimmer devices")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping device scan", e)
            return@withContext false
        }
    }
    
    /**
     * Connect to Shimmer device using shimmerManager.connectShimmerThroughBTAddress()
     */
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
            _connectionEvents.emit(ConnectionEvent(deviceInfo.address, ConnectionState.FAILED, e.message))
            return@withContext false
        }
    }
    
    /**
     * Disconnect from Shimmer device using shimmer.stop() and shimmer.disconnect()
     */
    suspend fun disconnectDevice(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Disconnecting Shimmer device: $deviceAddress")
        
        try {
            val shimmer = connectedDevices[deviceAddress] ?: run {
                Log.w(TAG, "Device not connected: $deviceAddress")
                return@withContext false
            }
            
            // Stop streaming and disconnect
            shimmer.stop()
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
        return connectedDevices.values.map { shimmer ->
            DeviceInfo(
                address = shimmer.bluetoothAddress,
                name = shimmer.shimmerUserAssignedName ?: "Connected Shimmer",
                rssi = -50, // Connected devices don't report RSSI
                isConnectable = false, // Already connected
                deviceType = "Shimmer3 GSR+"
            )
        }
    }
    
    fun getConnectedShimmer(deviceAddress: String): Shimmer? {
        return connectedDevices[deviceAddress]
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
        val name = btDevice.name
        
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
        val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}