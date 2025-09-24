package mpdc4gsr.network

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController

/**
 * High-level coordinator for bidirectional command/control networking.
 * Manages connection lifecycle and decides which transport to use (TCP or Bluetooth).
 * Android app acts as client connecting to PC server.
 */
class NetworkManager(
    private val context: Context,
    private val recordingController: ComprehensiveRecordingController
) {
    companion object {
        private const val TAG = "NetworkManager"
        private const val DEFAULT_PC_PORT = 8080
    }
    
    private var activeConnection: CommandConnection? = null
    private var commandHandler: CommandHandler? = null
    
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionMonitorJob: Job? = null
    
    private val _connectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<CommandConnection.ConnectionState> = _connectionState.asStateFlow()
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    init {
        commandHandler = CommandHandler(recordingController, this)
    }
    
    /**
     * Connect to PC server via Wi-Fi TCP
     */
    suspend fun connectWifi(host: String, port: Int = DEFAULT_PC_PORT): Boolean {
        if (activeConnection != null) {
            Log.w(TAG, "Disconnecting existing connection before connecting via Wi-Fi")
            disconnect()
        }
        
        Log.i(TAG, "Attempting Wi-Fi connection to $host:$port")
        
        val tcpClient = TcpClient(host, port)
        return connectWithClient(tcpClient, "Wi-Fi TCP")
    }
    
    /**
     * Connect to PC server via Bluetooth RFCOMM
     */
    suspend fun connectBluetooth(bluetoothDevice: BluetoothDevice): Boolean {
        if (activeConnection != null) {
            Log.w(TAG, "Disconnecting existing connection before connecting via Bluetooth")
            disconnect()
        }
        
        Log.i(TAG, "Attempting Bluetooth connection to ${bluetoothDevice.name} (${bluetoothDevice.address})")
        
        val bluetoothClient = BluetoothClient(bluetoothDevice)
        return connectWithClient(bluetoothClient, "Bluetooth RFCOMM")
    }
    
    private suspend fun connectWithClient(client: CommandConnection, connectionType: String): Boolean {
        try {
            // Set up callbacks before connecting
            client.setMessageCallback { message ->
                handleIncomingMessage(message)
            }
            
            client.setConnectionCallback { state ->
                _connectionState.value = state
                when (state) {
                    CommandConnection.ConnectionState.CONNECTED -> {
                        Log.i(TAG, "$connectionType connection established")
                        _lastError.value = null
                        sendInitialHandshake()
                        startPeriodicUpdates()
                    }
                    CommandConnection.ConnectionState.DISCONNECTED -> {
                        Log.i(TAG, "$connectionType connection closed")
                        stopPeriodicUpdates()
                    }
                    CommandConnection.ConnectionState.ERROR -> {
                        Log.w(TAG, "$connectionType connection error")
                        _lastError.value = "$connectionType connection error"
                        stopPeriodicUpdates()
                    }
                    else -> { /* Other states handled by state flow */ }
                }
            }
            
            // Attempt connection
            val success = client.connect()
            if (success) {
                activeConnection = client
                Log.i(TAG, "Successfully connected via $connectionType")
                return true
            } else {
                Log.e(TAG, "Failed to connect via $connectionType")
                client.cleanup()
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
     * Send response message to PC
     */
    suspend fun sendResponse(message: String): Boolean {
        return activeConnection?.sendMessage(message) ?: false
    }
    
    /**
     * Send telemetry/status message to PC
     */
    suspend fun sendTelemetry(message: String): Boolean {
        return activeConnection?.sendMessage(message) ?: false
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
                )
                val sensors = listOf("RGB", "Thermal", "GSR")
                val helloMessage = Protocol.createHelloMessage(deviceId, sensors)
                
                activeConnection?.sendMessage(helloMessage)
                Log.i(TAG, "Sent initial handshake: $helloMessage")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending initial handshake", e)
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
}