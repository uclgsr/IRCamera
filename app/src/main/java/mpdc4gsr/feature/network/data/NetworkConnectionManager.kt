package mpdc4gsr.feature.network.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkConnectionManager(
    private val context: Context,
    private val networkServer: NetworkServer,
    private val protocolHandler: ProtocolHandler
) {
    companion object {
        private const val TAG = "NetworkConnectionManager"
        private const val RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val CONNECTION_TIMEOUT_MS = 30000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()
    private var reconnectAttempts = 0
    private var connectionTimeoutJob: kotlinx.coroutines.Job? = null

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR,
        RECONNECTING
    }

    init {
        // Monitor network server connection state
        scope.launch {
            networkServer.connectionStateFlow.collect { connected ->
                if (connected) {
                    onConnectionEstablished()
                } else {
                    onConnectionLost()
                }
            }
        }
        // Monitor protocol messages for connection health
        scope.launch {
            networkServer.messageFlow.collect { message ->
                onProtocolMessageReceived(message)
            }
        }
    }

    suspend fun startServer(): Boolean {
        return try {
            _connectionState.value = ConnectionState.CONNECTING
            _errorState.value = null
            val started = networkServer.start()
            if (started) {
                AppLogger.i(TAG, "Network server started successfully")
                // Server is running, waiting for client connections
                _connectionState.value = ConnectionState.DISCONNECTED // Waiting for PC to connect
                true
            } else {
                AppLogger.e(TAG, "Failed to start network server")
                _connectionState.value = ConnectionState.ERROR
                _errorState.value = "Failed to start server"
                false
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error starting network server", e)
            _connectionState.value = ConnectionState.ERROR
            _errorState.value = "Server start error: ${e.message}"
            false
        }
    }

    suspend fun stopServer() {
        try {
            connectionTimeoutJob?.cancel()
            networkServer.stop()
            _connectionState.value = ConnectionState.DISCONNECTED
            _errorState.value = null
            reconnectAttempts = 0
            AppLogger.i(TAG, "Network server stopped")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping network server", e)
        }
    }

    private fun onConnectionEstablished() {
        AppLogger.i(TAG, "PC Controller connection established")
        _connectionState.value = ConnectionState.CONNECTED
        _errorState.value = null
        reconnectAttempts = 0
        // Start connection timeout monitoring
        connectionTimeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (_connectionState.value == ConnectionState.CONNECTED) {
                AppLogger.w(TAG, "Connection timeout - no activity for ${CONNECTION_TIMEOUT_MS}ms")
                checkConnectionHealth()
            }
        }
        // Enable preview streaming when PC connects
        scope.launch {
            try {
                protocolHandler.enablePreviewStreaming()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error enabling preview streaming", e)
            }
        }
    }

    private fun onConnectionLost() {
        AppLogger.i(TAG, "PC Controller connection lost")
        connectionTimeoutJob?.cancel()
        if (_connectionState.value == ConnectionState.CONNECTED) {
            // Connection was active, this is unexpected
            _connectionState.value = ConnectionState.ERROR
            _errorState.value = "Connection lost unexpectedly"
            // Disable preview streaming
            scope.launch {
                try {
                    protocolHandler.disablePreviewStreaming()
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error disabling preview streaming", e)
                }
            }
            // Attempt reconnection if not at max attempts
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            } else {
                AppLogger.e(TAG, "Max reconnection attempts reached")
                _connectionState.value = ConnectionState.ERROR
                _errorState.value = "Max reconnection attempts exceeded"
            }
        } else {
            // Normal disconnection or server waiting for connections
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private fun scheduleReconnect() {
        _connectionState.value = ConnectionState.RECONNECTING
        reconnectAttempts++
        scope.launch {
            Log.i(
                TAG,
                "Scheduling reconnection attempt $reconnectAttempts in ${RECONNECT_DELAY_MS}ms"
            )
            delay(RECONNECT_DELAY_MS)
            if (isActive && _connectionState.value == ConnectionState.RECONNECTING) {
                attemptReconnection()
            }
        }
    }

    private suspend fun attemptReconnection() {
        try {
            AppLogger.i(TAG, "Attempting reconnection $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")
            // Restart the server to accept new connections
            networkServer.stop()
            delay(1000) // Brief pause before restart
            val restarted = networkServer.start()
            if (restarted) {
                AppLogger.i(TAG, "Server restarted for reconnection")
                _connectionState.value = ConnectionState.DISCONNECTED // Waiting for PC
            } else {
                AppLogger.e(TAG, "Failed to restart server for reconnection")
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    _errorState.value = "Reconnection failed after $MAX_RECONNECT_ATTEMPTS attempts"
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during reconnection attempt", e)
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            } else {
                _connectionState.value = ConnectionState.ERROR
                _errorState.value = "Reconnection error: ${e.message}"
            }
        }
    }

    private fun onProtocolMessageReceived(message: Protocol.ProtocolMessage) {
        // Reset connection timeout when we receive messages
        connectionTimeoutJob?.cancel()
        if (_connectionState.value == ConnectionState.CONNECTED) {
            // Restart timeout for next message
            connectionTimeoutJob = scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    checkConnectionHealth()
                }
            }
        }
        // Handle connection-related protocol messages
        when (message.type) {
            Protocol.MSG_HELLO -> {
                AppLogger.d(TAG, "Received HELLO from PC - connection healthy")
            }

            Protocol.MSG_ERROR -> {
                val errorCode = message.parameters["code"]
                val errorMsg = message.parameters["msg"]
                AppLogger.w(TAG, "Received ERROR from PC: $errorCode - $errorMsg")
                _errorState.value = "PC Error: $errorMsg"
            }

            else -> {
                // Other messages indicate healthy connection
                AppLogger.d(TAG, "Received ${message.type} - connection active")
            }
        }
    }

    private fun checkConnectionHealth() {
        AppLogger.w(TAG, "Checking connection health due to inactivity")
        // In a real implementation, we might send a ping/keepalive message
        // For now, just log the health check
    }

    suspend fun forceReconnect() {
        AppLogger.i(TAG, "Force reconnection requested")
        reconnectAttempts = 0
        _connectionState.value = ConnectionState.RECONNECTING
        attemptReconnection()
    }

    fun getConnectionInfo(): Map<String, Any> {
        return mapOf(
            "state" to _connectionState.value.name,
            "error" to (_errorState.value ?: "none"),
            "reconnect_attempts" to reconnectAttempts,
            "server_running" to networkServer.isRunning(),
            "client_connected" to networkServer.isClientConnected()
        )
    }

    fun cleanup() {
        scope.coroutineContext.job.cancel()
        connectionTimeoutJob?.cancel()
        AppLogger.i(TAG, "NetworkConnectionManager cleaned up")
    }
}