package mpdc4gsr.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController

/**
 * Enhanced NetworkManager with automatic reconnection, settings persistence,
 * and comprehensive error recovery as specified in the requirements.
 */
class NetworkManager(
    private val context: Context,
    private val recordingController: ComprehensiveRecordingController
) {
    companion object {
        private const val TAG = "NetworkManager"
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

    /**
     * Connect using saved settings
     */
    suspend fun connectUsingSavedSettings(): Boolean {
        if (!networkSettings.isConfigured()) {
            Log.w(TAG, "No connection settings configured")
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
                        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                        if (bluetoothAdapter?.isEnabled == true) {
                            try {
                                val device = bluetoothAdapter.getRemoteDevice(address)
                                lastConnectionConfig = ConnectionConfig(
                                    NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM,
                                    bluetoothDevice = device
                                )
                                connectBluetooth(device)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting Bluetooth device: $address", e)
                                _lastError.value = "Bluetooth device not available"
                                false
                            }
                        } else {
                            Log.e(TAG, "Bluetooth adapter not available or disabled")
                            _lastError.value = "Bluetooth not available"
                            false
                        }
                    } else {
                        Log.e(TAG, "No saved Bluetooth device")
                        _lastError.value = "No Bluetooth device configured"
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting saved Bluetooth device info", e)
                    _lastError.value = "Error accessing Bluetooth settings"
                    false
                }
            }
        }
    }

    /**
     * Enable/disable automatic reconnection
     */
    fun setAutoReconnectEnabled(enabled: Boolean) {
        isAutoReconnectEnabled = enabled
        networkSettings.autoReconnect = enabled
        Log.i(TAG, "Auto-reconnect ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Connect to PC server via Wi-Fi TCP with settings persistence
     */
    suspend fun connectWifi(host: String, port: Int = DEFAULT_PC_PORT): Boolean {
        if (activeConnection != null) {
            Log.w(TAG, "Disconnecting existing connection before connecting via Wi-Fi")
            disconnect()
        }

        Log.i(TAG, "Attempting Wi-Fi connection to $host:$port")

        // Save settings
        networkSettings.pcIpAddress = host
        networkSettings.pcPort = port
        networkSettings.preferredConnectionType = NetworkSettings.ConnectionType.WIFI_TCP
        _connectionSummary.value = networkSettings.getConnectionSummary()

        lastConnectionConfig = ConnectionConfig(NetworkSettings.ConnectionType.WIFI_TCP, host, port)

        val tcpClient = TcpClient(host, port)
        return connectWithClient(tcpClient, "Wi-Fi TCP")
    }

    /**
     * Connect to PC server via Bluetooth RFCOMM with settings persistence
     */
    suspend fun connectBluetooth(bluetoothDevice: BluetoothDevice): Boolean {
        if (activeConnection != null) {
            Log.w(TAG, "Disconnecting existing connection before connecting via Bluetooth")
            disconnect()
        }

        Log.i(
            TAG,
            "Attempting Bluetooth connection to ${bluetoothDevice.name} (${bluetoothDevice.address})"
        )

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

        val bluetoothClient = BluetoothClient(bluetoothDevice)
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
                currentReconnectAttempts = 0  // Reset attempts on successful connection
                Log.i(TAG, "Successfully connected via $connectionType")
                return true
            } else {
                Log.e(TAG, "Failed to connect via $connectionType")
                client.cleanup()

                // Attempt reconnection if enabled
                if (isAutoReconnectEnabled && currentReconnectAttempts < networkSettings.reconnectAttempts) {
                    scheduleReconnection()
                }
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during $connectionType connection", e)
            _lastError.value = "$connectionType connection failed: ${e.message}"
            client.cleanup()
            return false
        }
    }

    /**
     * Disconnect from PC server
     */
    suspend fun disconnect() {
        Log.i(TAG, "Disconnecting from PC server")

        stopPeriodicUpdates()

        activeConnection?.let { connection ->
            // Send disconnection notice if possible
            try {
                connection.sendMessage("BYE")
            } catch (e: Exception) {
                Log.w(TAG, "Could not send BYE message", e)
            }

            connection.disconnect()
            connection.cleanup()
        }

        activeConnection = null
        _connectionState.value = CommandConnection.ConnectionState.DISCONNECTED
    }

    /**
     * Send response message to PC with metrics tracking
     */
    suspend fun sendResponse(message: String): Boolean {
        val result = activeConnection?.sendMessage(message) ?: false
        if (result) {
            connectionMetrics.recordMessageSent(message.length)
        }
        return result
    }

    /**
     * Send telemetry/status message to PC with metrics tracking
     */
    suspend fun sendTelemetry(message: String): Boolean {
        val result = activeConnection?.sendMessage(message) ?: false
        if (result) {
            connectionMetrics.recordMessageSent(message.length)
        }
        return result
    }

    /**
     * Check if currently connected to PC
     */
    fun isConnected(): Boolean {
        return activeConnection?.isConnected() ?: false
    }

    /**
     * Get connection info for UI display
     */
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

    /**
     * Clean up resources
     */
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
                Log.e(TAG, "Error handling incoming message: $message", e)
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
                        Log.i(TAG, "Sent initial handshake: $helloMessage")
                    } else {
                        Log.w(TAG, "Failed to send initial handshake message")
                        val error = NetworkErrorCodes.NetworkError(
                            NetworkErrorCodes.ERROR_PROTOCOL_VIOLATION,
                            details = "Failed to send handshake message"
                        )
                        _lastError.value = error.message
                        _lastErrorCode.value = error
                    }
                } else {
                    Log.w(TAG, "No active connection for handshake")
                    _lastError.value = "No connection available"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending initial handshake", e)
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
                Log.i(TAG, "$connectionType connection established")
                _lastError.value = null
                currentReconnectAttempts = 0
                connectionMetrics.recordConnectionStart()
                sendInitialHandshake()
                startPeriodicUpdates()
                startTelemetryUpdates()
            }

            CommandConnection.ConnectionState.DISCONNECTED -> {
                Log.i(TAG, "$connectionType connection closed")
                connectionMetrics.recordConnectionEnd()
                stopPeriodicUpdates()
                stopTelemetryUpdates()

                // Attempt reconnection if enabled and not manually disconnected
                if (isAutoReconnectEnabled && activeConnection != null) {
                    scheduleReconnection()
                }
            }

            CommandConnection.ConnectionState.ERROR -> {
                Log.w(TAG, "$connectionType connection error")
                _lastError.value = "$connectionType connection error"
                connectionMetrics.recordConnectionEnd()
                stopPeriodicUpdates()
                stopTelemetryUpdates()

                // Attempt reconnection on error if enabled
                if (isAutoReconnectEnabled) {
                    scheduleReconnection()
                }
            }

            else -> { /* Other states handled by state flow */
            }
        }
    }

    private fun scheduleReconnection() {
        if (currentReconnectAttempts >= networkSettings.reconnectAttempts) {
            Log.w(
                TAG,
                "Maximum reconnection attempts reached (${networkSettings.reconnectAttempts})"
            )
            _lastError.value =
                "Connection failed after ${networkSettings.reconnectAttempts} attempts"
            return
        }

        currentReconnectAttempts++
        connectionMetrics.recordReconnectAttempt()
        Log.i(
            TAG,
            "Scheduling reconnection attempt $currentReconnectAttempts/${networkSettings.reconnectAttempts}"
        )

        reconnectionJob?.cancel()
        reconnectionJob = managerScope.launch {
            delay(RECONNECT_DELAY_MS)
            attemptReconnection()
        }
    }

    private suspend fun attemptReconnection() {
        lastConnectionConfig?.let { config ->
            Log.i(TAG, "Attempting reconnection...")
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
                Log.w(TAG, "Reconnection attempt $currentReconnectAttempts failed")
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
                    Log.e(TAG, "Error sending telemetry update", e)
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
                        mpdc4gsr.controller.RecordingState.RECORDING -> {
                            val message =
                                "STATUS Recording started locally, sensors: [RGB,Thermal,GSR]"
                            sendTelemetry(message)
                        }

                        mpdc4gsr.controller.RecordingState.STOPPED -> {
                            val message = "STATUS Recording stopped locally"
                            sendTelemetry(message)
                        }

                        else -> { /* Other states */
                        }
                    }
                }
            }
        }
    }

    /**
     * Get network settings for configuration
     */
    fun getNetworkSettings(): NetworkSettings = networkSettings

    /**
     * Get connection metrics for monitoring
     */
    fun getConnectionMetrics(): ConnectionMetrics = connectionMetrics

    /**
     * Get detailed connection information including metrics
     */
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