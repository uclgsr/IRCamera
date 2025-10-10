package mpdc4gsr.core.hardware.gsr

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.Message
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.shimmerresearch.bluetooth.ShimmerBluetooth
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ObjectCluster
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mpdc4gsr.core.hardware.gsr.model.DeviceInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class GsrDeviceManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) {
    companion object {
        private const val TAG = "GsrDeviceManager"
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

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coroutineScope: CoroutineScope
        get() = lifecycleOwner?.lifecycleScope ?: managerScope

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
    private var classicDiscoveryReceiver: BroadcastReceiver? = null
    private var connectionMonitorJob: Job? = null
    private val _scanResults = MutableSharedFlow<List<DeviceInfo>>(replay = 1, extraBufferCapacity = 4)
    val scanResults: SharedFlow<List<DeviceInfo>> = _scanResults.asSharedFlow()
    private val _connectionEvents =
        MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    private val _dataEvents =
        MutableSharedFlow<Pair<String, ObjectCluster>>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val dataEvents: SharedFlow<Pair<String, ObjectCluster>> = _dataEvents.asSharedFlow()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val shimmerHandler =
        Handler(Looper.getMainLooper()) { message ->
            handleShimmerMessage(message)
            true
        }

    data class ConnectionEvent(
        val deviceAddress: String,
        val state: ConnectionState,
        val message: String? = null
    )

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, FAILED, TIMEOUT
    }

    private fun handleShimmerMessage(message: Message) {
        when (message.what) {
            ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE -> handleStateChangePayload(message.obj)
            ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET -> {
                val cluster = message.obj as? ObjectCluster ?: return
                val macAddress = cluster.macAddress ?: return
                _dataEvents.tryEmit(macAddress to cluster)
            }

            Shimmer.MESSAGE_TOAST -> {
                val toastMessage = message.data?.getString(Shimmer.TOAST)
                if (!toastMessage.isNullOrBlank()) {
                    Log.i(TAG, "Shimmer message: $toastMessage")
                }
            }
        }
    }

    private fun handleStateChangePayload(payload: Any?) {
        val state: BT_STATE?
        val macAddress: String?
        when (payload) {
            is ObjectCluster -> {
                state = payload.mState
                macAddress = payload.macAddress
            }

            is CallbackObject -> {
                state = payload.mState
                macAddress = payload.mBluetoothAddress
            }

            else -> return
        }

        if (state == null || macAddress.isNullOrBlank()) {
            return
        }

        when (state) {
            BT_STATE.CONNECTED -> {
                val shimmer = runCatching {
                    shimmerManager?.getShimmerDeviceBtConnectedFromMac(macAddress) as? Shimmer
                }.getOrNull()
                if (shimmer != null) {
                    connectedDevices[macAddress] = shimmer
                    reconnectionAttempts.remove(macAddress)
                }
                emitConnectionEvent(macAddress, ConnectionState.CONNECTED, "Connected to device")
            }

            BT_STATE.CONNECTING -> emitConnectionEvent(macAddress, ConnectionState.CONNECTING, "Connecting...")

            BT_STATE.DISCONNECTED,
            BT_STATE.CONNECTION_LOST,
            BT_STATE.NONE -> {
                connectedDevices.remove(macAddress)
                emitConnectionEvent(macAddress, ConnectionState.DISCONNECTED, "Device disconnected")
            }

            BT_STATE.FAILED_TO_CONNECT -> {
                connectedDevices.remove(macAddress)
                emitConnectionEvent(macAddress, ConnectionState.FAILED, "Failed to connect")
            }

            BT_STATE.CONNECTION_TIMEOUT -> emitConnectionEvent(
                macAddress,
                ConnectionState.TIMEOUT,
                "Connection timeout"
            )

            else -> Unit
        }
    }

    private fun emitConnectionEvent(
        macAddress: String,
        state: ConnectionState,
        message: String? = null,
    ) {
        coroutineScope.launch {
            _connectionEvents.emit(
                ConnectionEvent(
                    deviceAddress = macAddress,
                    state = state,
                    message = message,
                ),
            )
        }
    }

    private fun startClassicDiscoveryInternal() {
        mainHandler.post {
            if (classicDiscoveryReceiver == null) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            BluetoothDevice.ACTION_FOUND -> {
                                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                                if (device != null) {
                                    handleDiscoveredDevice(device, rssi)
                                }
                            }

                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                coroutineScope.launch {
                                    _scanResults.emit(discoveredDevices.values.sortedBy { it.name ?: it.address })
                                }
                            }
                        }
                    }
                }
                runCatching { context.registerReceiver(receiver, filter) }
                classicDiscoveryReceiver = receiver
            }
            try {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED ||
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
                ) {
                    bluetoothAdapter?.let { adapter ->
                        if (adapter.isDiscovering) {
                            adapter.cancelDiscovery()
                        }
                        adapter.startDiscovery()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start classic discovery", e)
            }
        }
    }

    private fun stopClassicDiscoveryInternal() {
        mainHandler.post {
            try {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED ||
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
                ) {
                    bluetoothAdapter?.takeIf { it.isDiscovering }?.cancelDiscovery()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to stop classic discovery", e)
            }
            classicDiscoveryReceiver?.let { receiver ->
                runCatching { context.unregisterReceiver(receiver) }
            }
            classicDiscoveryReceiver = null
        }
    }

    private fun handleDiscoveredDevice(device: BluetoothDevice, rssi: Int) {
        if (!isValidShimmerDevice(device)) {
            return
        }
        val normalizedRssi = if (rssi == Short.MIN_VALUE.toInt()) -50 else rssi
        val name = resolveDeviceName(device) ?: "Unknown Shimmer"
        val info = DeviceInfo(
            address = device.address,
            name = name,
            rssi = normalizedRssi,
            deviceType = detectShimmerDeviceType(device),
            isGSRCapable = true,
        )
        discoveredDevices[device.address] = info
        coroutineScope.launch {
            _scanResults.emit(discoveredDevices.values.sortedBy { it.name ?: it.address })
        }
    }

    private fun stopBleScan() {
        val callback = currentScanCallback ?: return
        try {
            val adapter = bluetoothAdapter ?: bluetoothManager?.adapter
            ?: (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            val scanner = adapter?.bluetoothLeScanner
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
            ) {
                scanner?.stopScan(callback)
            }
        } catch (e: SecurityException) {
            mpdc4gsr.core.common.AppLogger.w(TAG, "Missing Bluetooth permissions while stopping scan", e)
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(TAG, "Unexpected error stopping BLE scan", e)
        } finally {
            currentScanCallback = null
        }
    }

    private fun registerShimmerCallbacks(manager: ShimmerBluetoothManagerAndroid) {
        val clazz = ShimmerBluetoothManagerAndroid::class.java
        runCatching {
            clazz.getMethod("setAllDeviceStatesHandler", Handler::class.java).invoke(manager, shimmerHandler)
        }
        runCatching {
            clazz.getMethod("setDataHandler", Handler::class.java).invoke(manager, shimmerHandler)
        }
        runCatching {
            clazz.getMethod("setAllDeviceObjectClusterHandler", Handler::class.java).invoke(manager, shimmerHandler)
        }
        runCatching {
            clazz.getMethod("setMessageHandler", Handler::class.java).invoke(manager, shimmerHandler)
        }
    }


    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!hasRequiredPermissions()) {
                return@withContext false
            }
            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter?.isEnabled != true) {
                return@withContext false
            }
            shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerHandler).also { manager ->
                registerShimmerCallbacks(manager)
            }
            startConnectionMonitoring()
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    private fun startConnectionMonitoring() {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = coroutineScope.launch {
            while (isActive) {
                delay(5000)
                val disconnectedDevices = connectedDevices.filter { (address, shimmer) ->
                    try {
                        shimmer.bluetoothRadioState == BT_STATE.DISCONNECTED
                    } catch (e: Exception) {
                        false
                    }
                }
                disconnectedDevices.forEach { (address, shimmer) ->
                    Log.w(TAG, "Device disconnected: $address")
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
        if (!hasRequiredPermissions()) {
            return@withContext false
        }
        try {
            val adapter = bluetoothAdapter ?: bluetoothManager?.adapter
            if (adapter == null || !adapter.isEnabled) {
                return@withContext false
            }
            discoveredDevices.clear()
            getPairedShimmerDevices().forEach { device ->
                handleDiscoveredDevice(device, -50)
            }
            isScanning.set(true)
            _scanResults.emit(discoveredDevices.values.sortedBy { it.name ?: it.address })
            startClassicDiscoveryInternal()
            performEnhancedBluetoothLeScanning()
            scanJob?.cancel()
            scanJob = coroutineScope.launch {
                delay(SCAN_TIMEOUT_MS)
                stopDeviceScanning()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GSR scan", e)
            stopDeviceScanning()
            false
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
        val scanFilters =
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString(SHIMMER_SERVICE_UUID)))
                    .build(),
            )
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
        try {
            bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
            currentScanCallback = scanCallback
        } catch (e: SecurityException) {
            isScanning.set(false)
        } catch (e: Exception) {
            isScanning.set(false)
        }
    }

    private fun handleScanResult(result: ScanResult) {
        handleDiscoveredDevice(result.device, result.rssi)
    }

    private fun detectShimmerDeviceType(device: BluetoothDevice): String {
        val deviceName = try {
            device.name?.lowercase() ?: ""
        } catch (e: SecurityException) {
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
        scanJob?.cancel()
        scanJob = null
        stopBleScan()
        stopClassicDiscoveryInternal()
        if (isScanning.getAndSet(false)) {
            runCatching {
                _scanResults.emit(discoveredDevices.values.sortedBy { it.name ?: it.address })
            }
        }
    }

    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        Log.i(
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
        try {
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
            Log.w(
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
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Permission denied for Bluetooth connection"

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Connection timeout - check device proximity"

                e.message?.contains("unavailable", ignoreCase = true) == true ->
                    "Device unavailable - may be connected to another app"

                else -> "Connection failed: ${e.message}"
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
        try {
            val shimmer = connectedDevices[deviceAddress] ?: run {
                return@withContext false
            }
            shimmer.stopStreaming()
            shimmer.disconnect()
            connectedDevices.remove(deviceAddress)
            return@withContext true
        } catch (e: Exception) {
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

    fun getDeviceInfo(address: String): DeviceInfo? = discoveredDevices[address]

    fun isDeviceConnected(address: String): Boolean = connectedDevices.containsKey(address)

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
                    Log.i(
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
                            Log.i(
                                TAG,
                                "Automatic reconnection successful for device: $deviceAddress"
                            )
                            reconnectionAttempts.remove(deviceAddress)
                        } else {
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
            try {
                if (targetDeviceCount > MAX_CONCURRENT_DEVICES) {
                    Log.w(
                        TAG,
                        "Target device count $targetDeviceCount exceeds maximum ${MAX_CONCURRENT_DEVICES}, limiting"
                    )
                }
                val actualTargetCount = minOf(targetDeviceCount, MAX_CONCURRENT_DEVICES)
                val connectedCount = connectedDevices.size
                if (connectedCount < actualTargetCount) {
                    Log.w(
                        TAG,
                        "Only $connectedCount devices connected, need $actualTargetCount for comprehensive testing"
                    )
                    if (connectedCount < 2) {
                        return@withContext false
                    }
                }
                val streamingResults = startSynchronizedStreamingOnAllDevices()
                if (streamingResults) {
                    Log.i(
                        TAG,
                        " Multi-device testing started successfully with ${connectedDevices.size} devices"
                    )
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                return@withContext false
            }
        }

    private suspend fun startSynchronizedStreamingOnAllDevices(): Boolean {
        return try {
            coroutineScope {
                val streamingJobs = connectedDevices.map { (address, shimmer) ->
                    async {
                        try {
                            shimmer.startStreaming()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
                val results = streamingJobs.awaitAll()
                val successCount = results.count { it }
                Log.i(
                    TAG,
                    "Synchronized streaming started: $successCount/${connectedDevices.size} devices successful"
                )
                if (successCount >= 2) {
                    Log.i(
                        TAG,
                        " Multi-device streaming barrier successful with $successCount devices"
                    )
                    true
                } else {
                    Log.e(
                        TAG,
                        " Multi-device streaming barrier failed - insufficient devices streaming"
                    )
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun stopMultiDeviceTesting(): Boolean = withContext(Dispatchers.IO) {
        try {
            val stopResults = connectedDevices.map { (address, shimmer) ->
                async {
                    try {
                        shimmer.stopStreaming()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }.awaitAll()
            val successCount = stopResults.count { it }
            Log.i(
                TAG,
                "Multi-device testing stopped: $successCount/${connectedDevices.size} devices stopped successfully"
            )
            return@withContext true
        } catch (e: Exception) {
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

