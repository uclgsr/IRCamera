package mpdc4gsr.network.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.network.NetworkServer
import mpdc4gsr.network.ProtocolHandler
import mpdc4gsr.sensors.managers.TimeSyncManager
import org.json.JSONObject

/**
 * CommandServer - Handles command reception and coordination from PC
 * 
 * This class manages the command server that listens for PC commands:
 * - START command - triggers coordinated sensor recording
 * - STOP command - halts recording and saves data
 * - SYNC command - performs time synchronization exchange
 * - STATUS commands - provides device status updates
 */
class CommandServer(
    private val context: Context,
    private val port: Int = 8080
) {
    companion object {
        private const val TAG = "CommandServer"
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
    
    /**
     * Initialize and start the command server
     */
    suspend fun start(callback: CommandCallback, syncManager: TimeSyncManager) {
        Log.i(TAG, "Starting command server on port $port")
        
        this.commandCallback = callback
        this.timeSyncManager = syncManager
        
        try {
            // Initialize network components
            networkServer = NetworkServer(context, port).apply {
                setConnectionCallback { connected ->
                    _connectionStatus.value = if (connected) 
                        ConnectionStatus.CONNECTED 
                    else 
                        ConnectionStatus.DISCONNECTED
                }
            }
            
            protocolHandler = ProtocolHandler().apply {
                setCommandCallback(createProtocolCallback())
            }
            
            // Start network server
            serverScope.launch {
                networkServer?.startServer()
            }
            
            _serverStatus.value = ServerStatus.RUNNING
            Log.i(TAG, "Command server started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start command server", e)
            _serverStatus.value = ServerStatus.ERROR
            throw e
        }
    }
    
    /**
     * Stop the command server
     */
    fun stop() {
        Log.i(TAG, "Stopping command server")
        
        networkServer?.stopServer()
        serverScope.cancel()
        
        _serverStatus.value = ServerStatus.STOPPED
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        
        Log.i(TAG, "Command server stopped")
    }
    
    /**
     * Send acknowledgment to PC
     */
    suspend fun sendAck(originalMessageId: String, status: String = "success", data: JSONObject? = null) {
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
            
            Log.d(TAG, "Sent ACK for message $originalMessageId with status $status")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ACK", e)
        }
    }
    
    /**
     * Send status update to PC
     */
    suspend fun sendStatusUpdate(status: String, data: JSONObject? = null) {
        try {
            val statusMessage = JSONObject().apply {
                put("message_type", "status_update")
                put("status", status)
                put("timestamp", timeSyncManager?.getSyncedTimestampNs() ?: System.currentTimeMillis())
                put("device_id", android.os.Build.MODEL)
                data?.let { put("data", it) }
            }
            
            networkServer?.sendMessage(statusMessage.toString())
            
            Log.d(TAG, "Sent status update: $status")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send status update", e)
        }
    }
    
    /**
     * Create protocol callback for handling received commands
     */
    private fun createProtocolCallback(): ProtocolHandler.ProtocolCallback {
        return object : ProtocolHandler.ProtocolCallback {
            override suspend fun onCommand(command: String, params: JSONObject): String {
                Log.i(TAG, "Received command: $command")
                
                val messageId = params.optString("message_id", "unknown")
                
                return try {
                    when (command.uppercase()) {
                        "START_RECORD" -> {
                            val sessionId = params.optString("session_id", "default_session")
                            val configuration = params.optJSONObject("configuration") ?: JSONObject()
                            
                            emitCommandEvent(CommandEvent.StartRecord(sessionId, configuration))
                            
                            val success = commandCallback?.onStartRecording(sessionId, configuration) ?: false
                            
                            sendAck(messageId, if (success) "success" else "failed")
                            
                            if (success) "START-ACK" else "START-FAILED"
                        }
                        
                        "STOP_RECORD" -> {
                            emitCommandEvent(CommandEvent.StopRecord)
                            
                            val success = commandCallback?.onStopRecording() ?: false
                            
                            sendAck(messageId, if (success) "success" else "failed")
                            
                            if (success) "STOP-ACK" else "STOP-FAILED"
                        }
                        
                        "SYNC_REQUEST" -> {
                            val pcAddress = params.optString("pc_address", "127.0.0.1")
                            
                            emitCommandEvent(CommandEvent.SyncRequest(pcAddress))
                            
                            val success = commandCallback?.onSyncRequest(pcAddress) ?: false
                            
                            sendAck(messageId, if (success) "synced" else "sync_failed")
                            
                            if (success) "SYNC-ACK" else "SYNC-FAILED"
                        }
                        
                        "STATUS_REQUEST" -> {
                            emitCommandEvent(CommandEvent.StatusRequest)
                            
                            val statusData = commandCallback?.onStatusRequest() ?: JSONObject()
                            
                            sendAck(messageId, "status_data", statusData)
                            
                            "STATUS-ACK"
                        }
                        
                        else -> {
                            Log.w(TAG, "Unknown command: $command")
                            sendAck(messageId, "unknown_command")
                            "UNKNOWN-COMMAND"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing command: $command", e)
                    sendAck(messageId, "error")
                    "ERROR"
                }
            }
            
            override fun onConnectionStatusChanged(connected: Boolean) {
                _connectionStatus.value = if (connected) 
                    ConnectionStatus.CONNECTED 
                else 
                    ConnectionStatus.DISCONNECTED
                
                Log.i(TAG, "Connection status changed: ${if (connected) "connected" else "disconnected"}")
            }
        }
    }
    
    /**
     * Emit command event for logging and monitoring
     */
    private fun emitCommandEvent(event: CommandEvent) {
        serverScope.launch {
            _commandEvents.emit(event)
        }
    }
    
    // Data classes and enums
    sealed class CommandEvent {
        data class StartRecord(val sessionId: String, val configuration: JSONObject) : CommandEvent()
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
}