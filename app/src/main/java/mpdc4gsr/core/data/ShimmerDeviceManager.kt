package mpdc4gsr.core.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mpdc4gsr.core.data.model.DeviceInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ShimmerDeviceManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 30000L
        private const val SHIMMER_SERVICE_UUID = "49535343-FE7D-4AE5-8FA9-9FAFD205E455"
        private const val RECONNECTION_ATTEMPTS = 3
        private const val RECONNECTION_DELAY_MS = 2000L
        private const val CONNECTION_TIMEOUT_MS = 15000L
        private const val MAX_CONCURRENT_DEVICES = 3
        private const val DEVICE_SYNC_TIMEOUT_MS = 5000L
        private const val DATA_INTEGRITY_CHECK_INTERVAL_MS = 10000L
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
    private var currentScanCallback: android.bluetooth.le.ScanCallback? = null
    private var connectionMonitorJob: Job? = null
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
            if (!hasRequiredPermissions()) {
                return@withContext false
            }
            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter?.isEnabled != true) {
                return@withContext false
            }
            shimmerManager = ShimmerBluetoothManagerAndroid(context, mainHandler)
            startConnectionMonitoring()
            return@withContext true
            return@withContext false
        }
    }

    private fun startConnectionMonitoring() {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                val disconnectedDevices = connectedDevices.filter { (address, shimmer) ->
                        shimmer.bluetoothRadioState == BT_STATE.DISCONNECTED
                        false
                    }
                }
                disconnectedDevices.forEach { (address, shimmer) ->
                    launch {
                        handleDeviceDisconnection(address, shouldAttemptReconnection = true)
                    }
                }
            }
        }
    }

    suspend fun startDeviceScanning(): Boolean = withContext(Dispatchers.IO) {
        if (isScanning.get()) {
            return@withContext true
        }
        val shimmerMgr = shimmerManager ?: run {
            return@withContext false
        }
        if (!hasRequiredPermissions()) {
            return@withContext false
        }
            discoveredDevices.clear()
            isScanning.set(true)
            val pairedDevices = getPairedShimmerDevices()
            pairedDevices.forEach { device ->
                val deviceInfo = DeviceInfo(
                    address = device.address,
                    name = device.name ?: "Unknown Shimmer",
                    rssi = -50,
                    deviceType = detectShimmerDeviceType(device),
                    isGSRCapable = true
                )
                discoveredDevices[device.address] = deviceInfo
            }
            _scanResults.emit(discoveredDevices.values.toList())
            performEnhancedBluetoothLeScanning()
            lifecycleOwner.lifecycleScope.launch {
                delay(SCAN_TIMEOUT_MS)
                if (isScanning.get()) {
                    stopDeviceScanning()
                }
            }
            return@withContext true
            isScanning.set(false)
            return@withContext false
        }
    }

    private fun getPairedShimmerDevices(): List<BluetoothDevice> {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.bondedDevices
            ?.filter { isValidShimmerDevice(it) } ?: emptyList()
    }

    private suspend fun performEnhancedBluetoothLeScanning() {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            return
        }
        if (!hasRequiredPermissions()) {
            return
        }
        // Create scan filters for Shimmer devices
        val scanFilters = mutableListOf<ScanFilter>().apply {
            // Filter by Shimmer service UUID
            add(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString(SHIMMER_SERVICE_UUID)))
                    .build()
            )
            // Filter by device name patterns
            SHIMMER_NAME_PATTERNS.forEach { pattern ->
                add(
                    ScanFilter.Builder()
                        .setDeviceName(pattern)
                        .build()
                )
            }
            // Filter by MAC address prefixes if needed
            SHIMMER_MAC_PREFIXES.forEach { prefix ->
                add(
                    ScanFilter.Builder()
                        .setDeviceAddress(prefix)
                        .build()
                )
            }
        }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                results?.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                val errorMessage = when (errorCode) {
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning not supported on this device"
                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal scanning error"
                    else -> "Unknown scanning error: $errorCode"
                }
                isScanning.set(false)
            }
        }
            bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
            currentScanCallback = scanCallback
            isScanning.set(false)
            isScanning.set(false)
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        if (isValidShimmerDevice(device) && !discoveredDevices.containsKey(device.address)) {
                TAG,
                "Discovered new Shimmer device: ${device.name} (${device.address}) RSSI: $rssi"
            )
            val deviceInfo = DeviceInfo(
                address = device.address,
                name = device.name ?: "Unknown Shimmer",
                rssi = rssi,
                deviceType = detectShimmerDeviceType(device),
                isGSRCapable = true
            )
            discoveredDevices[device.address] = deviceInfo
            lifecycleOwner.lifecycleScope.launch {
                _scanResults.emit(discoveredDevices.values.toList())
            }
        } else if (discoveredDevices.containsKey(device.address)) {
            discoveredDevices[device.address]?.let { existingInfo ->
                discoveredDevices[device.address] = existingInfo.copy(rssi = rssi)
            }
        }
    }

    private fun detectShimmerDeviceType(device: BluetoothDevice): String {
        val deviceName = (
            device.name?.lowercase() ?: ""
            ""
        }
        return when {
            deviceName.contains("shimmer3") && deviceName.contains("gsr") -> "Shimmer3 GSR+"
            deviceName.contains("shimmer3") -> "Shimmer3"
            deviceName.contains("gsr") -> "Shimmer GSR Unit"
            deviceName.contains("shimmer") -> "Shimmer Device"
            else -> "Unknown Shimmer"
        }
    }

    suspend fun stopDeviceScanning() = withContext(Dispatchers.IO) {
        if (!isScanning.get()) {
            return@withContext
        }
            currentScanCallback?.let { callback ->
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
                    bluetoothLeScanner?.stopScan(callback)
                }
            }
            isScanning.set(false)
            currentScanCallback = null
            isScanning.set(false)
        }
    }

    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
            TAG,
            "Initiating connection to Shimmer device: ${deviceInfo.address} (${deviceInfo.name})"
        )
        val shimmerMgr = shimmerManager ?: run {
            _connectionEvents.emit(
                ConnectionEvent(
                    deviceInfo.address,
                    ConnectionState.FAILED,
                    "Shimmer manager not initialized"
                )
            )
            return@withContext false
        }
        if (connectedDevices.containsKey(deviceInfo.address)) {
            return@withContext true
        }
            _connectionEvents.emit(
                ConnectionEvent(
                    deviceInfo.address,
                    ConnectionState.CONNECTING,
                    "Connecting to ${deviceInfo.name}..."
                )
            )
            if (!hasRequiredPermissions()) {
                _connectionEvents.emit(
                    ConnectionEvent(
                        deviceInfo.address,
                        ConnectionState.FAILED,
                        "Missing Bluetooth permissions"
                    )
                )
                return@withContext false
            }
            shimmerMgr.connectShimmerThroughBTAddress(deviceInfo.address)
            var attempts = 0
            val maxAttempts = CONNECTION_TIMEOUT_MS / 1000
            val statusUpdateInterval = 3
            while (attempts < maxAttempts) {
                if (connectedDevices.containsKey(deviceInfo.address)) {
                    reconnectionAttempts.remove(deviceInfo.address)
                    _connectionEvents.emit(
                        ConnectionEvent(
                            deviceInfo.address,
                            ConnectionState.CONNECTED,
                            "Connected to ${deviceInfo.name}"
                        )
                    )
                    return@withContext true
                }
                if (attempts % statusUpdateInterval == 0 && attempts > 0) {
                    val remainingTime = maxAttempts - attempts
                    _connectionEvents.emit(
                        ConnectionEvent(
                            deviceInfo.address,
                            ConnectionState.CONNECTING,
                            "Connecting... (${remainingTime}s remaining)"
                        )
                    )
                }
                delay(1000)
                attempts++
            }
                TAG,
                "⏰ Connection timeout for device: ${deviceInfo.address} after ${CONNECTION_TIMEOUT_MS}ms"
            )
            _connectionEvents.emit(
                ConnectionEvent(
                    deviceInfo.address,
                    ConnectionState.TIMEOUT,
                    "Connection timeout - device may be out of range"
                )
            )
            return@withContext false
            val errorMessage = when {
                    "Permission denied for Bluetooth connection"

                    "Connection timeout - check device proximity"

                    "Device unavailable - may be connected to another app"

            }
            _connectionEvents.emit(
                ConnectionEvent(
                    deviceInfo.address,
                    ConnectionState.FAILED,
                    errorMessage
                )
            )
            return@withContext false
        }
    }

    suspend fun disconnectDevice(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
            val shimmer = connectedDevices[deviceAddress] ?: run {
                return@withContext false
            }
            shimmer.stopStreaming()
            shimmer.disconnect()
            connectedDevices.remove(deviceAddress)
            return@withContext true
            return@withContext false
        }
    }

    fun getConnectedDevices(): List<DeviceInfo> {
        return connectedDevices.values.map { shimmer ->
            DeviceInfo(
                address = shimmer.getMacId() ?: "Unknown",
                name = "Connected Shimmer (${shimmer.getMacId()})",
                rssi = -50,
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
            connectedDevices.remove(deviceAddress)
            _connectionEvents.emit(ConnectionEvent(deviceAddress, ConnectionState.DISCONNECTED))
            if (shouldAttemptReconnection) {
                val currentAttempts = reconnectionAttempts.getOrDefault(deviceAddress, 0)
                if (currentAttempts < RECONNECTION_ATTEMPTS) {
                        TAG,
                        "Starting automatic reconnection for device: $deviceAddress (attempt ${currentAttempts + 1}/$RECONNECTION_ATTEMPTS)"
                    )
                    reconnectionAttempts[deviceAddress] = currentAttempts + 1
                    _connectionEvents.emit(
                        ConnectionEvent(
                            deviceAddress,
                            ConnectionState.CONNECTING,
                            "Reconnecting..."
                        )
                    )
                    delay(RECONNECTION_DELAY_MS)
                    val deviceInfo = discoveredDevices[deviceAddress]
                    if (deviceInfo != null) {
                        val reconnectSuccess = connectToDevice(deviceInfo)
                        if (reconnectSuccess) {
                                TAG,
                                "Automatic reconnection successful for device: $deviceAddress"
                            )
                            reconnectionAttempts.remove(deviceAddress)
                        } else {
                            if (currentAttempts + 1 >= RECONNECTION_ATTEMPTS) {
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
                                handleDeviceDisconnection(deviceAddress, true)
                            }
                        }
                    } else {
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
        val addresses = connectedDevices.keys.toList()
        var allDisconnected = true
        addresses.forEach { address ->
            if (!disconnectDevice(address)) {
                allDisconnected = false
            }
        }
        return@withContext allDisconnected
    }

    suspend fun startMultiDeviceTesting(targetDeviceCount: Int = 3): Boolean =
        withContext(Dispatchers.IO) {
                if (targetDeviceCount > MAX_CONCURRENT_DEVICES) {
                        TAG,
                        "Target device count $targetDeviceCount exceeds maximum ${MAX_CONCURRENT_DEVICES}, limiting"
                    )
                }
                val actualTargetCount = minOf(targetDeviceCount, MAX_CONCURRENT_DEVICES)
                val connectedCount = connectedDevices.size
                if (connectedCount < actualTargetCount) {
                        TAG,
                        "Only $connectedCount devices connected, need $actualTargetCount for comprehensive testing"
                    )
                    if (connectedCount < 2) {
                        return@withContext false
                    }
                }
                val streamingResults = startSynchronizedStreamingOnAllDevices()
                if (streamingResults) {
                        TAG,
                        " Multi-device testing started successfully with ${connectedDevices.size} devices"
                    )
                    return@withContext true
                } else {
                    return@withContext false
                }
                return@withContext false
            }
        }

    private suspend fun startSynchronizedStreamingOnAllDevices(): Boolean {
        return (
            coroutineScope {
                val streamingJobs = connectedDevices.map { (address, shimmer) ->
                    async {
                            shimmer.startStreaming()
                            true
                            false
                        }
                    }
                }
                val results = streamingJobs.awaitAll()
                val successCount = results.count { it }
                    TAG,
                    "Synchronized streaming started: $successCount/${connectedDevices.size} devices successful"
                )
                if (successCount >= 2) {
                        TAG,
                        " Multi-device streaming barrier successful with $successCount devices"
                    )
                    true
                } else {
                        TAG,
                        " Multi-device streaming barrier failed - insufficient devices streaming"
                    )
                    false
                }
            }
            false
        }
    }

    suspend fun stopMultiDeviceTesting(): Boolean = withContext(Dispatchers.IO) {
            val stopResults = connectedDevices.map { (address, shimmer) ->
                async {
                        shimmer.stopStreaming()
                        true
                        false
                    }
                }
            }.awaitAll()
            val successCount = stopResults.count { it }
                TAG,
                "Multi-device testing stopped: $successCount/${connectedDevices.size} devices stopped successfully"
            )
            return@withContext true
            return@withContext false
        }
    }

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

    fun getShimmerSystemStatus(): ShimmerSystemStatus {
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        val hasPermissions = hasRequiredPermissions()
        val scanning = isScanning.get()
        val connectedCount = connectedDevices.size
        val discoveredCount = discoveredDevices.size
        val systemState = when {
            !hasPermissions -> ShimmerSystemState.PERMISSIONS_REQUIRED
            !bluetoothEnabled -> ShimmerSystemState.BLUETOOTH_DISABLED
            scanning -> ShimmerSystemState.SCANNING
            connectedCount > 0 -> ShimmerSystemState.CONNECTED
            discoveredCount > 0 -> ShimmerSystemState.DEVICES_FOUND
            else -> ShimmerSystemState.READY
        }
        val statusMessage = when (systemState) {
            ShimmerSystemState.PERMISSIONS_REQUIRED -> "Bluetooth permissions required for GSR sensor access"
            ShimmerSystemState.BLUETOOTH_DISABLED -> "Enable Bluetooth to connect to GSR sensors"
            ShimmerSystemState.SCANNING -> "Scanning for nearby Shimmer GSR devices..."
            ShimmerSystemState.CONNECTED -> "$connectedCount GSR sensor(s) connected and ready"
            ShimmerSystemState.DEVICES_FOUND -> "$discoveredCount GSR device(s) found - tap to connect"
            ShimmerSystemState.READY -> "Ready to scan for GSR sensors"
        }
        return ShimmerSystemStatus(
            state = systemState,
            message = statusMessage,
            isBluetoothEnabled = bluetoothEnabled,
            hasRequiredPermissions = hasPermissions,
            isScanning = scanning,
            connectedDeviceCount = connectedCount,
            discoveredDeviceCount = discoveredCount,
            connectedDevices = connectedDevices.values.map { shimmer ->
                ConnectedDeviceInfo(
                    address = shimmer.macId,
                    name = shimmer.shimmerUserAssignedName ?: "Shimmer Device",
                    isStreaming = shimmer.isStreaming,
                    connectionState = when (shimmer.bluetoothRadioState) {
                        BT_STATE.CONNECTED -> "Connected"
                        BT_STATE.STREAMING -> "Streaming"
                        BT_STATE.CONNECTING -> "Connecting"
                        BT_STATE.DISCONNECTED -> "Disconnected"
                        else -> "Unknown"
                    }
                )
            }
        )
    }

    data class ShimmerSystemStatus(
        val state: ShimmerSystemState,
        val message: String,
        val isBluetoothEnabled: Boolean,
        val hasRequiredPermissions: Boolean,
        val isScanning: Boolean,
        val connectedDeviceCount: Int,
        val discoveredDeviceCount: Int,
        val connectedDevices: List<ConnectedDeviceInfo>
    )

    data class ConnectedDeviceInfo(
        val address: String,
        val name: String,
        val isStreaming: Boolean,
        val connectionState: String
    )

    enum class ShimmerSystemState {
        PERMISSIONS_REQUIRED,
        BLUETOOTH_DISABLED,
        SCANNING,
        DEVICES_FOUND,
        CONNECTED,
        READY
    }

    fun getErrorMessage(error: ConnectionEvent): String {
        return when (error.state) {
            ConnectionState.FAILED -> {
                when {
                    error.message?.contains("permission", ignoreCase = true) == true ->
                        "GSR sensor connection failed: Please grant Bluetooth permissions and try again"

                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "GSR sensor connection timeout: Move closer to the device and ensure it's powered on"

                    error.message?.contains("unavailable", ignoreCase = true) == true ->
                        "GSR sensor unavailable: Device may be connected to another app"

                    else ->
                        "GSR sensor connection failed: ${error.message ?: "Unknown error"}"
                }
            }

            ConnectionState.TIMEOUT ->
                "GSR sensor connection timeout: Check device proximity and battery level"

            ConnectionState.DISCONNECTED ->
                "GSR sensor disconnected: ${error.message ?: "Device connection lost"}"

            else -> error.message ?: "GSR sensor status: ${error.state}"
        }
    }

    fun getActionableRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val status = getShimmerSystemStatus()
        when (status.state) {
            ShimmerSystemState.PERMISSIONS_REQUIRED -> {
                recommendations.add("Grant Bluetooth and Location permissions in app settings")
                recommendations.add("Ensure 'Nearby devices' permission is enabled for Android 12+")
            }

            ShimmerSystemState.BLUETOOTH_DISABLED -> {
                recommendations.add("Enable Bluetooth in device settings")
                recommendations.add("Ensure Bluetooth LE is supported on this device")
            }

            ShimmerSystemState.READY -> {
                recommendations.add("Power on your Shimmer GSR device")
                recommendations.add("Ensure the device is within 10 meters")
                recommendations.add("Tap 'Scan for Devices' to discover GSR sensors")
            }

            ShimmerSystemState.DEVICES_FOUND -> {
                recommendations.add("Select a discovered device to connect")
                recommendations.add("Ensure the device is not connected to another app")
            }

            ShimmerSystemState.CONNECTED -> {
                recommendations.add("GSR sensors ready for recording")
                if (status.connectedDeviceCount > 1) {
                    recommendations.add("Multi-device setup detected - great for research!")
                }
            }

            ShimmerSystemState.SCANNING -> {
                recommendations.add("Scanning in progress... please wait")
                recommendations.add("Ensure GSR devices are powered on and nearby")
            }
        }
        return recommendations
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = null
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
        val hasValidPrefix = SHIMMER_MAC_PREFIXES.any { prefix ->
            address.startsWith(prefix, ignoreCase = true)
        }
        val hasValidName = name?.let { deviceName ->
            SHIMMER_NAME_PATTERNS.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            }
        } ?: false
        val isValid = hasValidPrefix || hasValidName
        if (isValid) {
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
