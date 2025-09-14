package com.topdon.tc001.sensors.shimmer

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
import com.topdon.tc001.sensors.shimmer.model.ConnectionQuality
import com.topdon.tc001.sensors.shimmer.model.ShimmerDeviceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class ShimmerDeviceManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    companion object {
        private const val TAG = "ShimmerDeviceManager"

        private val SHIMMER_MAC_PREFIXES = listOf("00:06:66", "d0:39:72", "00:80:98")
        private val SHIMMER_DEVICE_NAMES = listOf("Shimmer", "GSR", "Shimmer3")

        private const val SCAN_DURATION_MS = 10000L  // 10 seconds default scan
        private const val RSSI_UPDATE_INTERVAL_MS = 2000L  // Update RSSI every 2 seconds
        private const val MIN_SIGNAL_STRENGTH = -85  // Minimum acceptable RSSI

        fun hasDiscoveryPermissions(context: Context): Boolean {
            return getRequiredPermissions().all { permission ->
                ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun getRequiredPermissions(): Array<String> =
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
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null

    private val discoveredDevices = ConcurrentHashMap<String, ShimmerDeviceInfo>()
    private var discoveryJob: Job? = null
    private var rssiUpdateJob: Job? = null

    private val _discoveredDeviceList = MutableStateFlow<List<ShimmerDeviceInfo>>(emptyList())
    val discoveredDeviceList: StateFlow<List<ShimmerDeviceInfo>> =
        _discoveredDeviceList.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatus = MutableStateFlow("Ready")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        Log.d(TAG, "Initializing Shimmer Device Manager")
        initializeBluetooth()
    }

    private fun initializeBluetooth() {
        try {
            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported on this device")
                _scanStatus.value = "Bluetooth Not Supported"
                return
            }

            if (!bluetoothAdapter!!.isEnabled) {
                Log.w(TAG, "Bluetooth is not enabled")
                _scanStatus.value = "Bluetooth Disabled"
                return
            }

            shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerCallback)
            _scanStatus.value = "Ready"

            Log.d(TAG, "Bluetooth components initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Bluetooth components", e)
            _scanStatus.value = "Initialization Failed"
        }
    }

    suspend fun startDeviceDiscovery(durationMs: Long = SCAN_DURATION_MS): Flow<List<ShimmerDeviceInfo>> =
        flow {
            if (!hasDiscoveryPermissions(context)) {
                Log.e(TAG, "Required permissions not granted for device discovery")
                _scanStatus.value = "Permission Denied"
                emit(emptyList())
                return@flow
            }

            if (bluetoothAdapter?.isEnabled != true) {
                Log.e(TAG, "Bluetooth is not enabled")
                _scanStatus.value = "Bluetooth Disabled"
                emit(emptyList())
                return@flow
            }

            try {
                _isScanning.value = true
                _scanStatus.value = "Scanning for Shimmer Devices"
                discoveredDevices.clear()

                addPairedShimmerDevices()

                withContext(Dispatchers.Main) {
                    shimmerManager?.startScanBtDevices()
                }

                startRSSIMonitoring()

                val scanSteps = (durationMs / 500).toInt()  // Update every 500ms
                repeat(scanSteps) { step ->
                    delay(500)

                    val progress = ((step + 1) * 100) / scanSteps
                    _scanStatus.value = "Scanning... ${progress}%"

                    val sortedDevices = getSortedDeviceList()
                    _discoveredDeviceList.value = sortedDevices
                    emit(sortedDevices)

                    Log.d(
                        TAG,
                        "Scan progress: ${progress}% - Found ${discoveredDevices.size} Shimmer devices"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during device discovery", e)
                _scanStatus.value = "Scan Error"
                emit(emptyList())
            } finally {

                withContext(Dispatchers.Main) {
                    shimmerManager?.stopScanBtDevices()
                }

                stopRSSIMonitoring()
                _isScanning.value = false
                _scanStatus.value = "Scan Complete - Found ${discoveredDevices.size} devices"

                val finalDevices = getSortedDeviceList()
                _discoveredDeviceList.value = finalDevices
                emit(finalDevices)
            }
        }

    private fun addPairedShimmerDevices() {
        try {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                if (isShimmerDevice(device)) {
                    val deviceInfo =
                        createDeviceInfo(device, -50, true) // Estimate RSSI for paired devices
                    discoveredDevices[device.address] = deviceInfo
                    Log.d(
                        TAG,
                        "Added paired Shimmer device: ${deviceInfo.name} (${deviceInfo.macAddress})"
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing paired devices", e)
        }
    }

    private fun startRSSIMonitoring() {
        rssiUpdateJob = lifecycleOwner.lifecycleScope.launch {
            while (isActive && _isScanning.value) {
                delay(RSSI_UPDATE_INTERVAL_MS)

                discoveredDevices.values.forEach { deviceInfo ->


                }
            }
        }
    }

    private fun stopRSSIMonitoring() {
        rssiUpdateJob?.cancel()
        rssiUpdateJob = null
    }

    private fun isShimmerDevice(device: BluetoothDevice): Boolean {
        val macAddress = device.address ?: return false
        val deviceName = device.name ?: ""

        val hasValidMAC = SHIMMER_MAC_PREFIXES.any { prefix ->
            macAddress.startsWith(prefix, ignoreCase = true)
        }

        val hasValidName = SHIMMER_DEVICE_NAMES.any { name ->
            deviceName.contains(name, ignoreCase = true)
        }

        return hasValidMAC || hasValidName
    }

    private fun createDeviceInfo(
        device: BluetoothDevice,
        rssi: Int,
        isPaired: Boolean
    ): ShimmerDeviceInfo {
        val macAddress = device.address
        val name = device.name ?: "Unknown Shimmer"

        return ShimmerDeviceInfo(
            macAddress = macAddress,
            name = name,
            rssi = rssi,
            isPaired = isPaired,
            priority = ShimmerDeviceInfo.calculatePriority(macAddress, name, rssi, isPaired),
            connectionState = if (isPaired) "Paired" else "Discovered"
        )
    }

    private fun getSortedDeviceList(): List<ShimmerDeviceInfo> {
        return discoveredDevices.values
            .filter { it.rssi >= MIN_SIGNAL_STRENGTH }  // Filter weak signals
            .sortedWith(compareByDescending<ShimmerDeviceInfo> { it.priority }
                .thenByDescending { it.rssi }
                .thenBy { it.name })
    }

    fun getDevice(macAddress: String): ShimmerDeviceInfo? = discoveredDevices[macAddress]

    fun getBestDevice(): ShimmerDeviceInfo? = getSortedDeviceList().firstOrNull()

    fun getDevicesByQuality(minQuality: ConnectionQuality): List<ShimmerDeviceInfo> {
        return getSortedDeviceList().filter { device ->
            ConnectionQuality.fromRSSI(device.rssi).minScore >= minQuality.minScore
        }
    }

    private fun updateDeviceRSSI(macAddress: String, newRssi: Int) {
        discoveredDevices[macAddress]?.let { device ->
            val updatedDevice = device.withRSSI(newRssi)
            discoveredDevices[macAddress] = updatedDevice

            lifecycleOwner.lifecycleScope.launch {
                _discoveredDeviceList.value = getSortedDeviceList()
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        stopRSSIMonitoring()

        shimmerManager?.stopScanBtDevices()
        _isScanning.value = false
        _scanStatus.value = "Stopped"

        Log.d(TAG, "Device discovery stopped")
    }

    fun clearDevices() {
        discoveredDevices.clear()
        _discoveredDeviceList.value = emptyList()
        Log.d(TAG, "Cleared discovered devices")
    }

    private val shimmerCallback =
        object : ShimmerBluetoothManagerAndroid.ShimmerBluetoothManagerCallback {

            override fun onDeviceFound(
                bluetoothDevice: BluetoothDevice,
                rssi: Int,
                scanRecord: ByteArray?
            ) {
                if (isShimmerDevice(bluetoothDevice)) {
                    val deviceInfo = createDeviceInfo(bluetoothDevice, rssi, false)
                    discoveredDevices[bluetoothDevice.address] = deviceInfo

                    Log.d(
                        TAG,
                        "Shimmer device discovered: ${deviceInfo.name} (${deviceInfo.macAddress}) " +
                                "RSSI: ${rssi}dBm, Priority: ${deviceInfo.priority}"
                    )

                    lifecycleOwner.lifecycleScope.launch {
                        _discoveredDeviceList.value = getSortedDeviceList()
                    }
                }
            }

            override fun onScanFinished() {
                Log.d(TAG, "Device scan finished. Found ${discoveredDevices.size} Shimmer devices")
                _scanStatus.value = "Scan Finished"
            }

            override fun onDeviceConnected(shimmer: com.shimmerresearch.android.Shimmer) {}
            override fun onDeviceDisconnected(shimmer: com.shimmerresearch.android.Shimmer) {}
            override fun onNewObjectCluster(callBackObject: com.shimmerresearch.driver.CallbackObject) {}
        }

    fun cleanup() {
        stopDiscovery()
        clearDevices()
        discoveryJob?.cancel()

        shimmerManager?.stopScanBtDevices()

        Log.d(TAG, "Shimmer Device Manager cleanup completed")
    }
}
