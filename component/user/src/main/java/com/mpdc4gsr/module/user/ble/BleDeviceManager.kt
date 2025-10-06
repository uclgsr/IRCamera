package com.mpdc4gsr.module.user.ble

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

class BleDeviceManager(private val context: Context) : CoroutineScope {
    companion object {
        private const val TAG = "BleDeviceManager"
        private val GSR_DEVICE_NAMES =
            setOf(
                "Shimmer3 GSR+",
                "Shimmer",
                "GSR_Unit",
            )
    }

    enum class SystemBleStatus {
        NOT_SUPPORTED,
        ENABLED,
        DISABLED
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()
    private var easyBLE: EasyBLE? = null
    private val gsrSensorAddresses = mutableSetOf<String>()
    private val _discoveredDevices = MutableLiveData<List<BleDeviceInfo>>()
    val discoveredDevices: LiveData<List<BleDeviceInfo>> = _discoveredDevices
    private val _pairedDevices = MutableLiveData<List<BleDeviceInfo>>()
    val pairedDevices: LiveData<List<BleDeviceInfo>> = _pairedDevices
    private val _deviceStatus = MutableLiveData<Map<String, DeviceConnectionStatus>>()
    val deviceStatus: LiveData<Map<String, DeviceConnectionStatus>> = _deviceStatus
    private val deviceConnections = ConcurrentHashMap<String, Connection>()
    private val deviceInfoMap = ConcurrentHashMap<String, BleDeviceInfo>()

    data class BleDeviceInfo(
        val address: String,
        val name: String?,
        val rssi: Int,
        val isGsrSensor: Boolean,
        val isPaired: Boolean,
        val lastSeen: Long = System.currentTimeMillis(),
    )

    data class DeviceConnectionStatus(
        val address: String,
        val connectionState: ConnectionState,
        val reliabilityScore: Double,
        val dataIntegrity: Double,
        val isActive: Boolean,
    )

    fun initialize(enableNordicBackend: Boolean = true) {
        launch {
            Log.i(TAG, "Initializing BLE Device Manager with Nordic backend: $enableNordicBackend")
            easyBLE =
                EasyBLE.getBuilder()
                    .build().apply {
                        initialize(context.applicationContext as android.app.Application)
                    }
            setupDeviceDiscovery()
            Log.i(TAG, "BLE Device Manager initialized successfully")
        }
    }

    private fun setupDeviceDiscovery() {
        easyBLE?.addScanListener(
            object : com.topdon.ble.callback.ScanListener {
                override fun onScanStart() {
                    Log.d(TAG, "Enhanced BLE scan started")
                }

                override fun onScanStop() {
                    Log.d(TAG, "Enhanced BLE scan stopped")
                }

                override fun onScanResult(
                    device: Device,
                    isConnectedBySys: Boolean,
                ) {
                    val deviceInfo =
                        BleDeviceInfo(
                            address = device.address,
                            name = device.name,
                            rssi = device.rssi,
                            isGsrSensor = isGsrSensorDevice(device),
                            isPaired = isConnectedBySys,
                        )
                    deviceInfoMap[device.address] = deviceInfo
                    if (deviceInfo.isGsrSensor) {
                        gsrSensorAddresses.add(device.address)
                        Log.i(TAG, "GSR sensor detected: ${device.name} (${device.address})")
                    }
                    updateDiscoveredDevices()
                }

                override fun onScanError(
                    errorCode: Int,
                    errorMsg: String?,
                ) {
                    Log.e(
                        TAG,
                        "Enhanced BLE scan failed with error code: $errorCode, message: $errorMsg"
                    )
                }
            },
        )
    }

    private fun isGsrSensorDevice(device: Device): Boolean {
        val deviceName = device.name?.uppercase() ?: return false
        return GSR_DEVICE_NAMES.any { gsrName ->
            deviceName.contains(gsrName.uppercase())
        }
    }

    fun startDeviceDiscovery() {
        launch {
            Log.i(TAG, "Starting enhanced device discovery")
            easyBLE?.startScan()
        }
    }

    fun stopDeviceDiscovery() {
        launch {
            Log.i(TAG, "Stopping device discovery")
            easyBLE?.stopScan()
        }
    }

    fun connectToDevice(deviceAddress: String): Boolean {
        return try {
            Log.i(TAG, "Attempting enhanced connection to device: $deviceAddress")
            val deviceInfo = deviceInfoMap[deviceAddress]
            if (deviceInfo == null) {
                Log.e(TAG, "Device info not found for address: $deviceAddress")
                return false
            }
            val config = createOptimalConnectionConfig(deviceInfo.isGsrSensor)
            val observer = createDeviceObserver(deviceAddress)
            val connection = easyBLE?.connect(deviceAddress, config, observer)
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

    private fun createOptimalConnectionConfig(isGsrSensor: Boolean): ConnectionConfiguration {
        return ConnectionConfiguration().apply {
            if (isGsrSensor) {
                setConnectTimeoutMillis(10000)
                setAutoReconnect(true)
                Log.d(TAG, "Applied GSR-optimized connection configuration")
            } else {
                setConnectTimeoutMillis(5000)
                setAutoReconnect(false)
            }
        }
    }

    private fun createDeviceObserver(deviceAddress: String): EventObserver {
        return object : EventObserver {
            override fun onConnectionStateChanged(device: Device) {
                launch {
                    val connectionState = device.connectionState
                    Log.i(
                        TAG,
                        "Device connection state changed: $deviceAddress, state: $connectionState"
                    )
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
                            Log.d(
                                TAG,
                                "Connection state: $connectionState for device: $deviceAddress"
                            )
                        }
                    }
                    updateDeviceStatus()
                }
            }

            override fun onConnectFailed(
                device: Device,
                failType: Int,
            ) {
                launch {
                    Log.w(TAG, "Device connection failed: $deviceAddress, error: $failType")
                    updateDeviceStatus()
                }
            }

            override fun onConnectTimeout(
                device: Device,
                type: Int,
            ) {
                launch {
                    Log.w(TAG, "Device connection timeout: $deviceAddress, type: $type")
                    updateDeviceStatus()
                }
            }

            override fun onCharacteristicChanged(
                device: Device,
                service: java.util.UUID,
                characteristic: java.util.UUID,
                value: ByteArray?,
            ) {
                Log.d(
                    TAG,
                    "Characteristic changed for $deviceAddress: service=$service, char=$characteristic"
                )
            }

            override fun onNotificationChanged(
                request: Request,
                isEnabled: Boolean,
            ) {
                Log.d(
                    TAG,
                    "Notification changed for $deviceAddress: ${request.type}, enabled: $isEnabled"
                )
            }

            override fun onCharacteristicRead(
                request: Request,
                value: ByteArray?,
            ) {
                Log.d(TAG, "Characteristic read for $deviceAddress: ${request.type}")
            }

            override fun onCharacteristicWrite(
                request: Request,
                value: ByteArray?,
            ) {
                Log.d(TAG, "Characteristic write for $deviceAddress: ${request.type}")
            }

            override fun onRequestFailed(
                request: Request,
                failType: Int,
                value: Any?,
            ) {
                Log.w(
                    TAG,
                    "Request failed for $deviceAddress: ${request.type}, fail type: $failType"
                )
            }

            override fun onMtuChanged(
                request: Request,
                mtu: Int,
            ) {
                Log.i(TAG, "MTU changed for $deviceAddress: $mtu")
            }

            override fun onRssiRead(
                request: Request,
                rssi: Int,
            ) {
                Log.d(TAG, "RSSI read for $deviceAddress: $rssi")
            }

            override fun onDescriptorRead(
                request: Request,
                value: ByteArray?,
            ) {
                Log.d(TAG, "Descriptor read for $deviceAddress: ${request.type}")
            }

            override fun onBluetoothAdapterStateChanged(state: Int) {
                Log.d(TAG, "Bluetooth adapter state changed: $state")
            }

            override fun onPhyChange(
                request: Request,
                txPhy: Int,
                rxPhy: Int,
            ) {
                Log.d(TAG, "PHY change for $deviceAddress: TX=$txPhy, RX=$rxPhy")
            }
        }
    }

    fun disconnectDevice(deviceAddress: String) {
        launch {
            Log.i(TAG, "Disconnecting device: $deviceAddress")
            deviceConnections[deviceAddress]?.disconnect()
        }
    }

    fun getSystemBleStatus(): Any? { // UnifiedBleManager.SystemBleStatus replaced
        return try {
            val adapter = easyBLE?.bluetoothAdapter
            when {
                adapter == null -> SystemBleStatus.NOT_SUPPORTED
                adapter.isEnabled -> SystemBleStatus.ENABLED
                else -> SystemBleStatus.DISABLED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system BLE status", e)
            null
        }
    }

    private fun updateDiscoveredDevices() {
        _discoveredDevices.postValue(deviceInfoMap.values.toList())
    }

    private fun updatePairedDevices() {
        val paired = deviceInfoMap.values.filter { it.isPaired }
        _pairedDevices.postValue(paired)
    }

    private fun updateDeviceStatus() {
        val statusMap =
            deviceConnections.mapValues { (address, connection) ->
                DeviceConnectionStatus(
                    address = address,
                    connectionState = connection.connectionState,
                    reliabilityScore = 1.0,
                    dataIntegrity = 1.0,
                    isActive = connection.connectionState == ConnectionState.SERVICE_DISCOVERED,
                )
            }
        _deviceStatus.postValue(statusMap)
    }

    fun getDiscoveredDeviceCount(): Int {
        return deviceInfoMap.size
    }

    fun getGsrDeviceCount(): Int {
        return gsrSensorAddresses.size
    }

    fun getPairedDeviceCount(): Int {
        return deviceInfoMap.values.count { it.isPaired }
    }

    fun isScanning(): Boolean {
        return easyBLE?.isScanning() ?: false
    }

    fun release() {
        launch {
            Log.i(TAG, "Releasing BLE Device Manager")
            stopDeviceDiscovery()
            deviceConnections.values.forEach { connection ->
                connection.disconnect()
            }
            deviceConnections.clear()
            deviceInfoMap.clear()
            easyBLE?.release()
            Log.i(TAG, "BLE Device Manager released successfully")
        }
    }
}
