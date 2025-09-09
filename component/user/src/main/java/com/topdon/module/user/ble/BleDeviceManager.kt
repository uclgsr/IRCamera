package com.topdon.module.user.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topdon.ble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * BLE Device Manager for User Component - Enhanced device pairing and management interface
 * 
 * Provides systematic BLE device management capabilities for the Multi-Modal Physiological 
 * Sensing Platform user interface, enabling enhanced device pairing, monitoring, and 
 * configuration through Nordic BLE enhanced backend.
 * 
 * Features:
 * - Enhanced device discovery with filtering
 * - Automatic GSR sensor detection and pairing  
 * - Real-time device status monitoring
 * - User-friendly device management interface
 * - Multi-device coordination for hub-spoke systems
 * 
 * @author IRCamera User Component Enhancement Team
 */
class BleDeviceManager(private val context: Context) : CoroutineScope {
    
    companion object {
        private const val TAG = "BleDeviceManager"
        
        // Known GSR sensor identifiers
        private val GSR_DEVICE_NAMES = setOf(
            "Shimmer3 GSR+",
            "Shimmer",
            "GSR_Unit"
        )
    }
    
    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()
    
    // Enhanced BLE components
    private val enhancedBleManager = EnhancedBleManager.getInstance()
    private var easyBLE: EasyBLE? = null
    
    // Device management state
    private val _discoveredDevices = MutableLiveData<List<BleDeviceInfo>>()
    val discoveredDevices: LiveData<List<BleDeviceInfo>> = _discoveredDevices
    
    private val _pairedDevices = MutableLiveData<List<BleDeviceInfo>>()
    val pairedDevices: LiveData<List<BleDeviceInfo>> = _pairedDevices
    
    private val _deviceStatus = MutableLiveData<Map<String, DeviceConnectionStatus>>()
    val deviceStatus: LiveData<Map<String, DeviceConnectionStatus>> = _deviceStatus
    
    private val deviceConnections = ConcurrentHashMap<String, Connection>()
    private val deviceInfoMap = ConcurrentHashMap<String, BleDeviceInfo>()
    
    /**
     * BLE device information for user interface
     */
    data class BleDeviceInfo(
        val address: String,
        val name: String?,
        val rssi: Int,
        val isGsrSensor: Boolean,
        val isPaired: Boolean,
        val lastSeen: Long = System.currentTimeMillis()
    )
    
    /**
     * Device connection status for monitoring
     */
    data class DeviceConnectionStatus(
        val address: String,
        val connectionState: ConnectionState,
        val reliabilityScore: Double,
        val dataIntegrity: Double,
        val isActive: Boolean
    )
    
    /**
     * Initialize BLE Device Manager with enhanced Nordic backend
     */
    fun initialize(enableNordicBackend: Boolean = true) {
        launch {
            Log.i(TAG, "Initializing BLE Device Manager with Nordic backend: $enableNordicBackend")
            
            // Initialize enhanced BLE manager
            enhancedBleManager.initialize(context, enableNordicBackend)
            enhancedBleManager.enableMultiDeviceMode(true)
            
            // Get EasyBLE instance with Nordic backend
            easyBLE = EasyBLE.getBuilder()
                .setUseNordicBleBackend(enableNordicBackend)
                .build().apply {
                    initialize(context.applicationContext as android.app.Application)
                }
            
            // Setup device discovery listener
            setupDeviceDiscovery()
            
            Log.i(TAG, "BLE Device Manager initialized successfully")
        }
    }
    
    /**
     * Setup enhanced device discovery with GSR sensor detection
     */
    private fun setupDeviceDiscovery() {
        easyBLE?.addScanListener(object : com.topdon.ble.callback.ScanListener {
            override fun onScanStarted() {
                Log.d(TAG, "Enhanced BLE scan started")
            }
            
            override fun onScanResult(device: Device, rssi: Int, scanRecord: ByteArray?) {
                val deviceInfo = BleDeviceInfo(
                    address = device.address,
                    name = device.name,
                    rssi = rssi,
                    isGsrSensor = isGsrSensorDevice(device),
                    isPaired = false // Will be updated based on connection status
                )
                
                deviceInfoMap[device.address] = deviceInfo
                
                // Mark GSR sensors in enhanced manager
                if (deviceInfo.isGsrSensor) {
                    enhancedBleManager.markAsGsrSensor(device.address)
                    Log.i(TAG, "GSR sensor detected: ${device.name} (${device.address})")
                }
                
                updateDiscoveredDevices()
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Enhanced BLE scan failed with error code: $errorCode")
            }
            
            override fun onScanFinished(scanResults: List<Device>) {
                Log.d(TAG, "Enhanced BLE scan finished, found ${scanResults.size} devices")
            }
        })
    }
    
    /**
     * Detect if a device is a GSR sensor based on name and characteristics
     */
    private fun isGsrSensorDevice(device: Device): Boolean {
        val deviceName = device.name?.uppercase() ?: return false
        return GSR_DEVICE_NAMES.any { gsrName -> 
            deviceName.contains(gsrName.uppercase()) 
        }
    }
    
    /**
     * Start enhanced device discovery
     */
    fun startDeviceDiscovery() {
        launch {
            Log.i(TAG, "Starting enhanced device discovery")
            easyBLE?.startScan()
        }
    }
    
    /**
     * Stop device discovery
     */
    fun stopDeviceDiscovery() {
        launch {
            Log.i(TAG, "Stopping device discovery")
            easyBLE?.stopScan()
        }
    }
    
    /**
     * Connect to a BLE device with enhanced monitoring
     */
    fun connectToDevice(deviceAddress: String): Boolean {
        return try {
            Log.i(TAG, "Attempting enhanced connection to device: $deviceAddress")
            
            val deviceInfo = deviceInfoMap[deviceAddress]
            if (deviceInfo == null) {
                Log.e(TAG, "Device info not found for address: $deviceAddress")
                return false
            }
            
            val connection = enhancedBleManager.connectWithEnhancements(
                deviceAddress = deviceAddress,
                config = createOptimalConnectionConfig(deviceInfo.isGsrSensor),
                observer = createDeviceObserver(deviceAddress)
            )
            
            if (connection != null) {
                deviceConnections[deviceAddress] = connection
                updateDeviceStatus()
                Log.i(TAG, "Enhanced connection successful for device: $deviceAddress")
                true
            } else {
                Log.e(TAG, "Enhanced connection failed for device: $deviceAddress")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device $deviceAddress", e)
            false
        }
    }
    
    /**
     * Create optimal connection configuration for different device types
     */
    private fun createOptimalConnectionConfig(isGsrSensor: Boolean): ConnectionConfiguration {
        return ConnectionConfiguration().apply {
            if (isGsrSensor) {
                // Optimized settings for GSR sensors
                requestConnectionPriority = BluetoothGatt.CONNECTION_PRIORITY_HIGH
                autoConnect = true
                connectTimeoutMillis = 10000
                Log.d(TAG, "Applied GSR-optimized connection configuration")
            } else {
                // Standard settings for other devices
                requestConnectionPriority = BluetoothGatt.CONNECTION_PRIORITY_BALANCED
                autoConnect = false
                connectTimeoutMillis = 5000
            }
        }
    }
    
    /**
     * Create enhanced device observer with user component integration
     */
    private fun createDeviceObserver(deviceAddress: String): EventObserver {
        return object : EventObserver {
            override fun onConnectionStateChanged(device: Device) {
                launch {
                    val connectionState = device.connectionState
                    Log.i(TAG, "Device connection state changed: $deviceAddress, state: $connectionState")
                    
                    when (connectionState) {
                        ConnectionState.SERVICE_DISCOVERED -> {
                            val deviceInfo = deviceInfoMap[deviceAddress]?.copy(isPaired = true)
                            if (deviceInfo != null) {
                                deviceInfoMap[deviceAddress] = deviceInfo
                            }
                            updatePairedDevices()
                        }
                        ConnectionState.DISCONNECTED -> {
                            deviceConnections.remove(deviceAddress)
                            val deviceInfo = deviceInfoMap[deviceAddress]?.copy(isPaired = false)
                            if (deviceInfo != null) {
                                deviceInfoMap[deviceAddress] = deviceInfo
                            }
                            updatePairedDevices()
                        }
                        else -> {
                            // Handle other connection states
                            Log.d(TAG, "Connection state: $connectionState for device: $deviceAddress")
                        }
                    }
                    updateDeviceStatus()
                }
            }
            
            override fun onConnectFailed(device: Device, failType: Int) {
                launch {
                    Log.w(TAG, "Device connection failed: $deviceAddress, error: $failType")
                    updateDeviceStatus()
                }
            }
            
            override fun onConnectTimeout(device: Device, type: Int) {
                launch {
                    Log.w(TAG, "Device connection timeout: $deviceAddress, type: $type")
                    updateDeviceStatus()
                }
            }
            
            override fun onCharacteristicChanged(device: Device, service: java.util.UUID, 
                                              characteristic: java.util.UUID, value: ByteArray?) {
                Log.d(TAG, "Characteristic changed for $deviceAddress: service=$service, char=$characteristic")
            }
            
            override fun onNotificationChanged(request: Request, isEnabled: Boolean) {
                Log.d(TAG, "Notification changed for $deviceAddress: ${request.type}, enabled: $isEnabled")
            }
            
            override fun onCharacteristicRead(request: Request, value: ByteArray?) {
                Log.d(TAG, "Characteristic read for $deviceAddress: ${request.type}")
            }
            
            override fun onCharacteristicWrite(request: Request, value: ByteArray?) {
                Log.d(TAG, "Characteristic write for $deviceAddress: ${request.type}")
            }
            
            override fun onRequestFailed(request: Request, failType: Int, value: Any?) {
                Log.w(TAG, "Request failed for $deviceAddress: ${request.type}, fail type: $failType")
            }
            
            override fun onMtuChanged(request: Request, mtu: Int) {
                Log.i(TAG, "MTU changed for $deviceAddress: $mtu")
            }
            
            override fun onRssiRead(request: Request, rssi: Int) {
                Log.d(TAG, "RSSI read for $deviceAddress: $rssi")
            }
            
            override fun onDescriptorRead(request: Request, value: ByteArray?) {
                Log.d(TAG, "Descriptor read for $deviceAddress: ${request.type}")
            }
            
            override fun onBluetoothAdapterStateChanged(state: Int) {
                Log.d(TAG, "Bluetooth adapter state changed: $state")
            }
            
            override fun onPhyChange(request: Request, txPhy: Int, rxPhy: Int) {
                Log.d(TAG, "PHY change for $deviceAddress: TX=$txPhy, RX=$rxPhy")
            }
        }
    }
    
    /**
     * Disconnect from a specific device
     */
    fun disconnectDevice(deviceAddress: String) {
        launch {
            Log.i(TAG, "Disconnecting device: $deviceAddress")
            deviceConnections[deviceAddress]?.disconnect()
        }
    }
    
    /**
     * Get current system BLE status for user interface
     */
    fun getSystemBleStatus(): EnhancedBleManager.SystemBleStatus? {
        return try {
            enhancedBleManager.getSystemStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system BLE status", e)
            null
        }
    }
    
    /**
     * Update discovered devices list
     */
    private fun updateDiscoveredDevices() {
        _discoveredDevices.postValue(deviceInfoMap.values.toList())
    }
    
    /**
     * Update paired devices list
     */
    private fun updatePairedDevices() {
        val paired = deviceInfoMap.values.filter { it.isPaired }
        _pairedDevices.postValue(paired)
    }
    
    /**
     * Update device status with metrics
     */
    private fun updateDeviceStatus() {
        val statusMap = deviceConnections.mapValues { (address, connection) ->
            val metrics = enhancedBleManager.getDeviceMetrics(address)
            DeviceConnectionStatus(
                address = address,
                connectionState = connection.connectionState,
                reliabilityScore = metrics?.reliabilityScore ?: 1.0,
                dataIntegrity = metrics?.dataIntegrity ?: 1.0,
                isActive = connection.connectionState == ConnectionState.SERVICE_DISCOVERED
            )
        }
        _deviceStatus.postValue(statusMap)
    }
    
    /**
     * Release all resources
     */
    fun release() {
        launch {
            Log.i(TAG, "Releasing BLE Device Manager")
            
            stopDeviceDiscovery()
            
            // Disconnect all devices
            deviceConnections.values.forEach { connection ->
                connection.disconnect()
            }
            
            deviceConnections.clear()
            deviceInfoMap.clear()
            
            enhancedBleManager.release()
            easyBLE?.release()
            
            Log.i(TAG, "BLE Device Manager released successfully")
        }
    }
}