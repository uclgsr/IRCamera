package com.mpdc4gsr.module.user.ble

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topdon.ble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class BleDeviceManager(
    private val context: Context,
) : CoroutineScope {
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
        DISABLED,
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
            easyBLE =
                EasyBLE
                    .getBuilder()
                    .build()
                    .apply {
                        initialize(context.applicationContext as android.app.Application)
                    }
            setupDeviceDiscovery()
        }
    }

    private fun setupDeviceDiscovery() {
        easyBLE?.addScanListener(
            object : com.topdon.ble.callback.ScanListener {
                override fun onScanStart() {
                }

                override fun onScanStop() {
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
                    }
                    updateDiscoveredDevices()
                }

                override fun onScanError(
                    errorCode: Int,
                    errorMsg: String?,
                ) {
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
            easyBLE?.startScan()
        }
    }

    fun stopDeviceDiscovery() {
        launch {
            easyBLE?.stopScan()
        }
    }

    fun connectToDevice(deviceAddress: String): Boolean {
        return try {
            val deviceInfo = deviceInfoMap[deviceAddress]
            if (deviceInfo == null) {
                return false
            }
            val config = createOptimalConnectionConfig(deviceInfo.isGsrSensor)
            val observer = createDeviceObserver(deviceAddress)
            val connection = easyBLE?.connect(deviceAddress, config, observer)
            if (connection != null) {
                deviceConnections[deviceAddress] = connection
                updateDeviceStatus()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createOptimalConnectionConfig(isGsrSensor: Boolean): ConnectionConfiguration =
        ConnectionConfiguration().apply {
            if (isGsrSensor) {
                setConnectTimeoutMillis(10000)
                setAutoReconnect(true)
            } else {
                setConnectTimeoutMillis(5000)
                setAutoReconnect(false)
            }
        }

    private fun createDeviceObserver(deviceAddress: String): EventObserver =
        object : EventObserver {
            override fun onConnectionStateChanged(device: Device) {
                launch {
                    val connectionState = device.connectionState
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
                    updateDeviceStatus()
                }
            }

            override fun onConnectTimeout(
                device: Device,
                type: Int,
            ) {
                launch {
                    updateDeviceStatus()
                }
            }

            override fun onCharacteristicChanged(
                device: Device,
                service: java.util.UUID,
                characteristic: java.util.UUID,
                value: ByteArray?,
            ) {
            }

            override fun onNotificationChanged(
                request: Request,
                isEnabled: Boolean,
            ) {
            }

            override fun onCharacteristicRead(
                request: Request,
                value: ByteArray?,
            ) {
            }

            override fun onCharacteristicWrite(
                request: Request,
                value: ByteArray?,
            ) {
            }

            override fun onRequestFailed(
                request: Request,
                failType: Int,
                value: Any?,
            ) {
            }

            override fun onMtuChanged(
                request: Request,
                mtu: Int,
            ) {
            }

            override fun onRssiRead(
                request: Request,
                rssi: Int,
            ) {
            }

            override fun onDescriptorRead(
                request: Request,
                value: ByteArray?,
            ) {
            }

            override fun onBluetoothAdapterStateChanged(state: Int) {
            }

            override fun onPhyChange(
                request: Request,
                txPhy: Int,
                rxPhy: Int,
            ) {
            }
        }

    fun disconnectDevice(deviceAddress: String) {
        launch {
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

    fun getDiscoveredDeviceCount(): Int = deviceInfoMap.size

    fun getGsrDeviceCount(): Int = gsrSensorAddresses.size

    fun getPairedDeviceCount(): Int = deviceInfoMap.values.count { it.isPaired }

    fun isScanning(): Boolean = easyBLE?.isScanning() ?: false

    fun release() {
        launch {
            stopDeviceDiscovery()
            deviceConnections.values.forEach { connection ->
                connection.disconnect()
            }
            deviceConnections.clear()
            deviceInfoMap.clear()
            easyBLE?.release()
        }
    }
}
