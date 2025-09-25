package mpdc4gsr.network.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.network.NetworkServer
import mpdc4gsr.network.ProtocolHandler
import mpdc4gsr.sync.TimeSyncManager
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
            networkServer = NetworkServer(context, port)
            
            protocolHandler = ProtocolHandler(context, networkServer!!).apply {
                setCommandHandler(createProtocolCallback())
            }
            
            // Start network server and monitor connection status
            serverScope.launch {
                val startResult = networkServer?.start()
                if (startResult == true) {
                    _serverStatus.value = ServerStatus.RUNNING
                    Log.i(TAG, "Command server started successfully")
                    
                    // Monitor connection status
                    networkServer?.connectionStateFlow?.collect { connected ->
                        _connectionStatus.value = if (connected) 
                            ConnectionStatus.CONNECTED 
                        else 
                            ConnectionStatus.DISCONNECTED
                    }
                } else {
                    _serverStatus.value = ServerStatus.ERROR
                    Log.e(TAG, "Failed to start network server")
                }
            }
            
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
        
        serverScope.launch {
            networkServer?.stop()
        }
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
    private fun createProtocolCallback(): ProtocolHandler.CommandHandler {
        return object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                Log.i(TAG, "Starting recording for session: $sessionId")
                
                return try {
                    // Delegate to recording controller
                    recordingController?.let { controller ->
                        // This would start the actual recording
                        ProtocolHandler.CommandResult(
                            success = true,
                            message = "Recording started",
                            data = mapOf("session_id" to sessionId)
                        )
                    } ?: ProtocolHandler.CommandResult(
                        success = false,
                        message = "Recording controller not available"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start recording", e)
                    ProtocolHandler.CommandResult(
                        success = false,
                        message = "Recording start failed: ${e.message}"
                    )
                }
            }
            
            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                Log.i(TAG, "Stopping recording for session: $sessionId")
                
                return try {
                    recordingController?.let { controller ->
                        // This would stop the actual recording
                        ProtocolHandler.CommandResult(
                            success = true,
                            message = "Recording stopped",
                            data = mapOf("session_id" to sessionId)
                        )
                    } ?: ProtocolHandler.CommandResult(
                        success = false,
                        message = "Recording controller not available"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop recording", e)
                    ProtocolHandler.CommandResult(
                        success = false,
                        message = "Recording stop failed: ${e.message}"
                    )
                }
            }
            
            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                Log.i(TAG, "Processing sync request from PC")
                
                return try {
                    timeSyncManager?.let { syncManager ->
                        val phoneTime = System.currentTimeMillis()
                        // This would perform actual sync calculation
                        ProtocolHandler.SyncResult(
                            success = true,
                            phoneTimestamp = phoneTime,
                            offsetNs = 0L // Would be calculated by sync manager
                        )
                    } ?: ProtocolHandler.SyncResult(
                        success = false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process sync request", e)
                    ProtocolHandler.SyncResult(success = false)
                }
            }
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