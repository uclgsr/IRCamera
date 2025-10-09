package mpdc4gsr.feature.network.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mpdc4gsr.core.data.TimeSyncManager
import org.json.JSONObject

class CommandServer(
    private val context: Context,
    private val port: Int = 8080
) {
    companion object {}

    // Data classes and enums - defined first to avoid forward reference issues
    sealed class CommandEvent {
        data class StartRecord(val sessionId: String, val configuration: JSONObject) :
            CommandEvent()

        object StopRecord : CommandEvent()
        data class SyncRequest(val pcAddress: String) : CommandEvent()
        object StatusRequest : CommandEvent()
    }

    enum class ServerStatus {
        STOPPED,
        STARTING,
        RUNNING,
        ERROR
    }

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTED,
        ERROR
    }

    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()
    private val _commandEvents = MutableSharedFlow<CommandEvent>()
    val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    private var networkServer: NetworkServer? = null
    private var protocolHandler: ProtocolHandler? = null
    private var timeSyncManager: TimeSyncManager? = null

    // Command callback interface for RecordingController integration
    interface CommandCallback {
        suspend fun onStartRecording(sessionId: String, configuration: JSONObject): Boolean
        suspend fun onStopRecording(): Boolean
        suspend fun onSyncRequest(pcAddress: String): Boolean
        suspend fun onStatusRequest(): JSONObject
    }

    private var commandCallback: CommandCallback? = null

    suspend fun start(callback: CommandCallback, syncManager: TimeSyncManager) {
        this.commandCallback = callback
        this.timeSyncManager = syncManager
        try {
            // Initialize network components
            networkServer = NetworkServer(context, port)
            networkServer?.let { server ->
                protocolHandler = ProtocolHandler(context, server).apply {
                    setCommandHandler(createProtocolCallback())
                }
            }
            // Start network server and monitor connection status
            serverScope.launch {
                val startResult = networkServer?.start()
                if (startResult == true) {
                    _serverStatus.value = ServerStatus.RUNNING                    // Monitor connection status
                    networkServer?.connectionStateFlow?.collect { connected ->
                        _connectionStatus.value = if (connected)
                            ConnectionStatus.CONNECTED
                        else
                            ConnectionStatus.DISCONNECTED
                    }
                } else {
                    _serverStatus.value = ServerStatus.ERROR
                }
            }
        } catch (e: Exception) {
            _serverStatus.value = ServerStatus.ERROR
            throw e
        }
    }

    suspend fun stop() {
        serverScope.launch {
            networkServer?.stop()
        }.join()
        serverScope.cancel()
        _serverStatus.value = ServerStatus.STOPPED
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        commandCallback = null
    }

    suspend fun sendAck(
        originalMessageId: String,
        status: String = "success",
        data: JSONObject? = null
    ) {
        try {
            val ackMessage = JSONObject().apply {
                put("message_type", "ack")
                put("original_message_id", originalMessageId)
                put("status", status)
                put("timestamp", System.currentTimeMillis())
                put("device_id", android.os.Build.MODEL)
                data?.let { put("data", it) }
            }
            networkServer?.sendMessage(ackMessage.toString())
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("CommandServer", "Unexpected Exception in CommandServer catch block", e)
        }
    }

    suspend fun sendStatusUpdate(status: String, data: JSONObject? = null) {
        try {
            val statusMessage = JSONObject().apply {
                put("message_type", "status_update")
                put("status", status)
                put("timestamp", System.currentTimeMillis())
                put("device_id", android.os.Build.MODEL)
                data?.let { put("data", it) }
            }
            networkServer?.sendMessage(statusMessage.toString())
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("CommandServer", "Unexpected Exception in CommandServer catch block", e)
        }
    }

    private fun createProtocolCallback(): ProtocolHandler.CommandHandler {
        return object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                return try {
                    // Delegate to recording controller
                    commandCallback?.let { callback ->
                        // Pass empty configuration for now - protocol handler should provide full config
                        val success = callback.onStartRecording(sessionId, JSONObject())
                        ProtocolHandler.CommandResult(
                            success = success,
                            message = if (success) "Recording started" else "Recording start failed",
                            data = mapOf("session_id" to sessionId)
                        )
                    } ?: ProtocolHandler.CommandResult(
                        success = false,
                        message = "Command callback not available"
                    )
                } catch (e: Exception) {
                    ProtocolHandler.CommandResult(
                        success = false,
                        message = "Recording start failed: ${e.message}"
                    )
                }
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                return try {
                    commandCallback?.let { callback ->
                        val success = callback.onStopRecording()
                        ProtocolHandler.CommandResult(
                            success = success,
                            message = if (success) "Recording stopped" else "Recording stop failed",
                            data = mapOf("session_id" to sessionId)
                        )
                    } ?: ProtocolHandler.CommandResult(
                        success = false,
                        message = "Command callback not available"
                    )
                } catch (e: Exception) {
                    ProtocolHandler.CommandResult(
                        success = false,
                        message = "Recording stop failed: ${e.message}"
                    )
                }
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                return try {
                    commandCallback?.let { callback ->
                        // Protocol handler should provide PC address, using empty string for now
                        val success = callback.onSyncRequest("")
                        if (success) {
                            timeSyncManager?.let {
                                ProtocolHandler.SyncResult(
                                    success = true,
                                    phoneTimestamp = System.currentTimeMillis(),
                                    offsetNs = 0L // Should be calculated by sync manager
                                )
                            } ?: ProtocolHandler.SyncResult(success = false)
                        } else {
                            ProtocolHandler.SyncResult(success = false)
                        }
                    } ?: ProtocolHandler.SyncResult(
                        success = false
                    )
                } catch (e: Exception) {
                    ProtocolHandler.SyncResult(success = false)
                }
            }
        }
    }
}