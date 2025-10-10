package mpdc4gsr.feature.connectivity.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.common.AppLogger

/**
 * Owns the lifecycle of [NetworkServer] and bridges connection events to the higher-level
 * [ProtocolHandler].  The original implementation had become difficult to reason about,
 * so this version emphasises predictable state transitions, small helper methods and
 * consistent coroutine ownership.
 */
class NetworkConnectionManager(
    private val networkServer: NetworkServer,
    private val protocolHandler: ProtocolHandler,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    companion object {
        private const val TAG = "NetworkConnectionManager"
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val CONNECTION_TIMEOUT_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    /**
     * High-level state of the server / connection workflow.
     */
    enum class ConnectionState {
        STOPPED,
        STARTING,
        WAITING_FOR_CLIENT,
        CONNECTED,
        RECONNECTING,
        ERROR,
    }


    private val _connectionState = MutableStateFlow(ConnectionState.STOPPED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var reconnectAttempts = 0

    private var connectionJob: Job? = null
    private var messageJob: Job? = null
    private var timeoutJob: Job? = null

    /**
     * Starts listening for PC controller connections.
     */
    fun start() {
        if (_connectionState.value != ConnectionState.STOPPED &&
            _connectionState.value != ConnectionState.ERROR
        ) {
            return
        }


        updateState(ConnectionState.STARTING)
        _lastError.value = null

        scope.launch {
            runCatching { networkServer.start() }
                .onSuccess { started ->
                    if (started) {
                        reconnectAttempts = 0
                        attachObservers()
                        updateState(ConnectionState.WAITING_FOR_CLIENT)
                    } else {
                        setError("Failed to start network server")
                    }
                }
                .onFailure { throwable ->
                    setError("Server start failure: ${throwable.message}")
                }
        }
    }

    /**
     * Stops the server and cancels any scheduled work.
     */
    fun stop() {
        scope.launch {
            runCatching { networkServer.stop() }
                .onFailure { throwable ->
                    AppLogger.e(TAG, "Error stopping server", throwable)
                    _lastError.value = throwable.message
                }

            cancelObservers()
            updateState(ConnectionState.STOPPED)
        }
    }

    /**
     * Forces a reconnection attempt from the current state.
     */
    fun forceReconnect() {
        scope.launch {
            reconnectAttempts = 0
            performReconnectionCycle()
        }
    }

    /**
     * Releases resources when the manager is no longer required.
     */
    fun cleanup() {
        stop()
        scope.cancel()
    }

    /**
     * Diagnostic snapshot for UI / logging.
     */
    fun getConnectionInfo(): Map<String, Any> =
        mapOf(
            "state" to _connectionState.value.name,
            "lastError" to (_lastError.value ?: "none"),
            "reconnectAttempts" to reconnectAttempts,
            "serverRunning" to networkServer.isRunning(),
            "clientConnected" to networkServer.isClientConnected(),
        )

    // -----------------------------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------------------------
            private fun attachObservers() {
        cancelObservers()

        connectionJob =
            scope.launch {
                networkServer.connectionStateFlow.collect { connected ->
                    if (connected) {
                        handleClientConnected()
                    } else {
                        handleClientDisconnected()
                    }
                }
            }


        messageJob =
            scope.launch {
                networkServer.messageFlow.collect { message ->
                    protocolHandler.handleIncomingMessage(message)
                    resetTimeoutWatcher()
                }
            }
    }


    private fun cancelObservers() {
        connectionJob?.cancel()
        messageJob?.cancel()
        timeoutJob?.cancel()
        connectionJob = null
        messageJob = null
        timeoutJob = null
    }


    private fun handleClientConnected() {
        AppLogger.i(TAG, "Client connected")
        reconnectAttempts = 0
        updateState(ConnectionState.CONNECTED)
        protocolHandler.onClientConnected()
        startTimeoutWatcher()
    }


    private fun handleClientDisconnected() {
        AppLogger.w(TAG, "Client disconnected")
        timeoutJob?.cancel()
        protocolHandler.onClientDisconnected()

        when (_connectionState.value) {
            ConnectionState.CONNECTED,
            ConnectionState.RECONNECTING -> scheduleReconnect()

            ConnectionState.WAITING_FOR_CLIENT,
            ConnectionState.STARTING,
            ConnectionState.ERROR,
            ConnectionState.STOPPED -> updateState(ConnectionState.WAITING_FOR_CLIENT)
        }
    }


    private fun scheduleReconnect() {
        timeoutJob?.cancel()

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            setError("Max reconnection attempts exceeded")
            return
        }


        reconnectAttempts += 1
        updateState(ConnectionState.RECONNECTING)

        scope.launch {
            delay(RECONNECT_DELAY_MS)
            performReconnectionCycle()
        }
    }


    private suspend fun performReconnectionCycle() {
        runCatching {
            networkServer.stop()
            delay(250)
            networkServer.start()
        }.onSuccess { started ->
            if (started) {
                updateState(ConnectionState.WAITING_FOR_CLIENT)
            } else {
                setError("Failed to restart server during reconnection")
            }
        }.onFailure { throwable ->
            setError("Reconnection failure: ${throwable.message}")
        }
    }


    private fun startTimeoutWatcher() {
        timeoutJob?.cancel()
        timeoutJob =
            scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    AppLogger.w(TAG, "Connection timeout reached, forcing reconnect")
                    scheduleReconnect()
                }
            }
    }


    private fun resetTimeoutWatcher() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            startTimeoutWatcher()
        }
    }


    private fun updateState(newState: ConnectionState) {
        _connectionState.value = newState
    }


    private fun setError(message: String) {
        _lastError.value = message
        updateState(ConnectionState.ERROR)
        AppLogger.e(TAG, message)
    }
}
