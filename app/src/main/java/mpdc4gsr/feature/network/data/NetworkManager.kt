package mpdc4gsr.feature.network.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkManager(
    private val context: Context,
    private val recordingController: RecordingController
) {
    companion object {
        private const val DEFAULT_PC_PORT = 8080
        private const val RECONNECT_DELAY_MS = 5000L
        private const val TELEMETRY_INTERVAL_MS = 5000L
    }

    private var activeConnection: CommandConnection? = null
    private var commandHandler: CommandHandler? = null
    private val networkSettings = NetworkSettings(context)
    private val connectionMetrics = ConnectionMetrics()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionMonitorJob: Job? = null
    private var reconnectionJob: Job? = null
    private var telemetryJob: Job? = null
    private var isAutoReconnectEnabled = true
    private var currentReconnectAttempts = 0
    private var lastConnectionConfig: ConnectionConfig? = null
    private val _connectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<CommandConnection.ConnectionState> =
        _connectionState.asStateFlow()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    private val _lastErrorCode = MutableStateFlow<NetworkErrorCodes.NetworkError?>(null)
    val lastErrorCode: StateFlow<NetworkErrorCodes.NetworkError?> = _lastErrorCode.asStateFlow()
    private val _connectionSummary = MutableStateFlow("Not configured")
    val connectionSummary: StateFlow<String> = _connectionSummary.asStateFlow()

    data class ConnectionConfig(
        val type: NetworkSettings.ConnectionType,
        val host: String? = null,
        val port: Int? = null,
        val bluetoothDevice: BluetoothDevice? = null
    )

    init {
        commandHandler = CommandHandler(recordingController, this)
        _connectionSummary.value = networkSettings.getConnectionSummary()
        // Setup session event broadcasting
        setupSessionEventBroadcasting()
    }

    suspend fun connectUsingSavedSettings(): Boolean {
        if (!networkSettings.isConfigured()) {
            _lastError.value = "No connection settings configured"
            return false
        }
        return when (networkSettings.preferredConnectionType) {
            NetworkSettings.ConnectionType.WIFI_TCP -> {
                lastConnectionConfig = ConnectionConfig(
                    NetworkSettings.ConnectionType.WIFI_TCP,
                    networkSettings.pcIpAddress,
                    networkSettings.pcPort
                )
                connectWifi(networkSettings.pcIpAddress, networkSettings.pcPort)
            }

            NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM -> {
                try {
                    val (address, _) = networkSettings.getSavedBluetoothDeviceInfo()
                    if (address != null) {
                        val bluetoothManager =
                            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        val bluetoothAdapter = bluetoothManager?.adapter
                        if (bluetoothAdapter?.isEnabled == true) {
                            try {
                                val device = bluetoothAdapter.getRemoteDevice(address)
                                lastConnectionConfig = ConnectionConfig(
                                    NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM,
                                    bluetoothDevice = device
                                )
                                connectBluetooth(device)
                            } catch (e: Exception) {
                                _lastError.value = "Bluetooth device not available"
                                false
                            }
                        } else {
                            _lastError.value = "Bluetooth not available"
                            false
                        }
                    } else {
                        _lastError.value = "No Bluetooth device configured"
                        false
                    }
                } catch (e: Exception) {
                    _lastError.value = "Error accessing Bluetooth settings"
                    false
                }
            }
        }
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        isAutoReconnectEnabled = enabled
        networkSettings.autoReconnect = enabled
        "enabled" else "disabled"
    }")
}

suspend fun connectWifi(host: String, port: Int = DEFAULT_PC_PORT): Boolean {
    if (activeConnection != null) {
        disconnect()
    }        // Save settings
    networkSettings.pcIpAddress = host
    networkSettings.pcPort = port
    networkSettings.preferredConnectionType = NetworkSettings.ConnectionType.WIFI_TCP
    _connectionSummary.value = networkSettings.getConnectionSummary()
    lastConnectionConfig = ConnectionConfig(NetworkSettings.ConnectionType.WIFI_TCP, host, port)
    val tcpClient = TcpClient(host, port)
    return connectWithClient(tcpClient, "Wi-Fi TCP")
}

suspend fun connectBluetooth(bluetoothDevice: BluetoothDevice): Boolean {
    if (activeConnection != null) {
        disconnect()
    }
    // Save settings asynchronously
    managerScope.launch {
        networkSettings.saveBluetoothDevice(bluetoothDevice)
    }
    networkSettings.preferredConnectionType = NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM
    _connectionSummary.value = networkSettings.getConnectionSummary()
    lastConnectionConfig =
        ConnectionConfig(
            NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM,
            bluetoothDevice = bluetoothDevice
        )
    val bluetoothClient = BluetoothClient(context, bluetoothDevice)
    return connectWithClient(bluetoothClient, "Bluetooth RFCOMM")
}

private suspend fun connectWithClient(
    client: CommandConnection,
    connectionType: String
): Boolean {
    try {
        // Set up callbacks before connecting
        client.setMessageCallback { message ->
            handleIncomingMessage(message)
        }
        client.setConnectionCallback { state ->
            _connectionState.value = state
            handleConnectionStateChange(state, connectionType)
        }
        // Attempt connection
        val success = client.connect()
        if (success) {
            activeConnection = client
            currentReconnectAttempts = 0  // Reset attempts on successful connection                return true
        } else {
            client.cleanup()
            // Attempt reconnection if enabled
            if (isAutoReconnectEnabled && currentReconnectAttempts < networkSettings.reconnectAttempts) {
                scheduleReconnection()
            }
            return false
        }
    } catch (e: Exception) {
        _lastError.value = "$connectionType connection failed: ${e.message}"
        client.cleanup()
        return false
    }
}

suspend fun disconnect() {
    stopPeriodicUpdates()
    activeConnection?.let { connection ->
        // Send disconnection notice if possible
        try {
            connection.sendMessage("BYE")
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("NetworkManager", "Unexpected Exception in NetworkManager catch block", e)
        }
        connection.disconnect()
        connection.cleanup()
    }
    activeConnection = null
    _connectionState.value = CommandConnection.ConnectionState.DISCONNECTED
}

suspend fun sendResponse(message: String): Boolean {
    val result = activeConnection?.sendMessage(message) ?: false
    if (result) {
        connectionMetrics.recordMessageSent(message.length)
    }
    return result
}

suspend fun sendTelemetry(message: String): Boolean {
    val result = activeConnection?.sendMessage(message) ?: false
    if (result) {
        connectionMetrics.recordMessageSent(message.length)
    }
    return result
}

fun isConnected(): Boolean {
    return activeConnection?.isConnected() ?: false
}

fun getConnectionInfo(): Map<String, Any> {
    val connection = activeConnection
    return mapOf(
        "connected" to (connection?.isConnected() ?: false),
        "type" to when (connection) {
            is TcpClient -> "Wi-Fi TCP"
            is BluetoothClient -> "Bluetooth RFCOMM"
            else -> "None"
        },
        "state" to _connectionState.value.name,
        "last_error" to (_lastError.value ?: "None")
    )
}

fun cleanup() {
    managerScope.launch {
        disconnect()
    }
    managerScope.cancel()
}

private fun handleIncomingMessage(message: String) {
    managerScope.launch {
        try {
            commandHandler?.handleCommand(message)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("NetworkManager", "Unexpected Exception in NetworkManager catch block", e)
        }
    }
}

private fun sendInitialHandshake() {
    managerScope.launch {
        try {
            // Send HELLO message to register with PC
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            val sensors = listOf("RGB", "Thermal", "GSR")
            val helloMessage = Protocol.createHelloMessage(deviceId, sensors)
            val connection = activeConnection
            if (connection != null) {
                val success = connection.sendMessage(helloMessage)
                if (success) {
                } else {
                    val error = NetworkErrorCodes.NetworkError(
                        NetworkErrorCodes.ERROR_PROTOCOL_VIOLATION,
                        details = "Failed to send handshake message"
                    )
                    _lastError.value = error.message
                    _lastErrorCode.value = error
                }
            } else {
                _lastError.value = "No connection available"
            }
        } catch (e: Exception) {
            _lastError.value = "Handshake error: ${e.message}"
        }
    }
}

private fun startPeriodicUpdates() {
    commandHandler?.startPeriodicStatusUpdates()
}

private fun stopPeriodicUpdates() {
    connectionMonitorJob?.cancel()
    connectionMonitorJob = null
}

// Event notification methods for integration with RecordingController
fun notifySessionStarted(sessionId: String) {
    commandHandler?.notifySessionStarted(sessionId)
}

fun notifySessionStopped(sessionId: String, duration: Long) {
    commandHandler?.notifySessionStopped(sessionId, duration)
}

fun notifyError(errorType: String, errorMessage: String) {
    commandHandler?.notifyError(errorType, errorMessage)
}

// Enhanced connection management methods
private fun handleConnectionStateChange(
    state: CommandConnection.ConnectionState,
    connectionType: String
) {
    when (state) {
        CommandConnection.ConnectionState.CONNECTED -> {
            _lastError.value = null
            currentReconnectAttempts = 0
            connectionMetrics.recordConnectionStart()
            sendInitialHandshake()
            startPeriodicUpdates()
            startTelemetryUpdates()
        }

        CommandConnection.ConnectionState.DISCONNECTED -> {
            connectionMetrics.recordConnectionEnd()
            stopPeriodicUpdates()
            stopTelemetryUpdates()
            // Attempt reconnection if enabled and not manually disconnected
            if (isAutoReconnectEnabled && activeConnection != null) {
                scheduleReconnection()
            }
        }

        CommandConnection.ConnectionState.ERROR -> {
            _lastError.value = "$connectionType connection error"
            connectionMetrics.recordConnectionEnd()
            stopPeriodicUpdates()
            stopTelemetryUpdates()
            // Attempt reconnection on error if enabled
            if (isAutoReconnectEnabled) {
                scheduleReconnection()
            }
        }

        else -> {
        }
    }
}

private fun scheduleReconnection() {
    if (currentReconnectAttempts >= networkSettings.reconnectAttempts) {
        _lastError.value =
            "Connection failed after ${networkSettings.reconnectAttempts} attempts"
        return
    }
    currentReconnectAttempts++
    connectionMetrics.recordReconnectAttempt() reconnectionJob ?. cancel ()
    reconnectionJob = managerScope.launch {
        delay(RECONNECT_DELAY_MS)
        attemptReconnection()
    }
}

private suspend fun attemptReconnection() {
    lastConnectionConfig?.let { config ->
        _connectionState.value = CommandConnection.ConnectionState.CONNECTING
        val success = when (config.type) {
            NetworkSettings.ConnectionType.WIFI_TCP -> {
                config.host?.let { host ->
                    config.port?.let { port ->
                        connectWifi(host, port)
                    }
                } ?: false
            }

            NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM -> {
                config.bluetoothDevice?.let { device ->
                    connectBluetooth(device)
                } ?: false
            }
        }
        if (!success) {
            if (currentReconnectAttempts < networkSettings.reconnectAttempts) {
                scheduleReconnection()
            }
        }
    }
}

private fun startTelemetryUpdates() {
    telemetryJob?.cancel()
    telemetryJob = managerScope.launch {
        while (isConnected()) {
            try {
                if (recordingController.isRecording) {
                    val statusResponse = commandHandler?.let { handler ->
                        // Create a basic status update
                        val status = "recording"
                        val uptime = System.currentTimeMillis() / 1000
                        "STATUS {\"status\":\"$status\",\"uptime\":$uptime,\"timestamp\":${System.currentTimeMillis()}}"
                    }
                    statusResponse?.let { sendTelemetry(it) }
                }
                delay(TELEMETRY_INTERVAL_MS)
            } catch (e: Exception) {
                break
            }
        }
    }
}

private fun stopTelemetryUpdates() {
    telemetryJob?.cancel()
    telemetryJob = null
}

private fun setupSessionEventBroadcasting() {
    // Monitor recording state changes to notify PC
    managerScope.launch {
        recordingController.recordingStateFlow.collect { state ->
            if (isConnected()) {
                when (state) {
                    RecordingState.RECORDING -> {
                        val message =
                            "STATUS Recording started locally, sensors: [RGB,Thermal,GSR]"
                        sendTelemetry(message)
                    }

                    RecordingState.STOPPED -> {
                        val message = "STATUS Recording stopped locally"
                        sendTelemetry(message)
                    }

                    else -> {
                    }
                }
            }
        }
    }
}

fun getNetworkSettings(): NetworkSettings = networkSettings

fun getConnectionMetrics(): ConnectionMetrics = connectionMetrics

suspend fun getDetailedConnectionInfo(): Map<String, Any> {
    val baseInfo = mapOf(
        "connected" to isConnected(),
        "type" to when (activeConnection) {
            is TcpClient -> "Wi-Fi TCP"
            is BluetoothClient -> "Bluetooth RFCOMM"
            else -> "None"
        },
        "state" to _connectionState.value.name,
        "last_error" to (_lastError.value ?: "None"),
        "auto_reconnect_enabled" to isAutoReconnectEnabled,
        "reconnect_attempts" to currentReconnectAttempts,
        "max_reconnect_attempts" to networkSettings.reconnectAttempts
    )
    val metricsInfo = connectionMetrics.getMetricsSummary()
    return baseInfo + metricsInfo
}
}
