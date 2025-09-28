package mpdc4gsr.network.managers

import android.util.Log
import mpdc4gsr.network.ProtocolHandler
import mpdc4gsr.sync.TimeSyncManager
import org.json.JSONObject

/**
 * Command server that handles protocol commands and delegates to appropriate callbacks
 */
class CommandServer {
    companion object {
        private const val TAG = "CommandServer"
    }

    private var commandCallback: ProtocolHandler.CommandCallback? = null
    private var timeSyncManager: TimeSyncManager? = null

    fun setCommandCallback(callback: ProtocolHandler.CommandCallback?) {
        commandCallback = callback
    }

    fun setTimeSyncManager(syncManager: TimeSyncManager?) {
        timeSyncManager = syncManager
    }

    /**
     * Handle start recording command
     */
    suspend fun handleStartRecording(sessionId: String): ProtocolHandler.CommandResult {
        return commandCallback?.let { callback ->
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
    }

    /**
     * Handle stop recording command
     */
    suspend fun handleStopRecording(sessionId: String): ProtocolHandler.CommandResult {
        return commandCallback?.let { callback ->
            // This would stop the actual recording
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
    }

    /**
     * Handle sync request command
     */
    suspend fun handleSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
        return commandCallback?.let { callback ->
            // TODO: The `pcAddress` is needed here but not provided by the handler.
            val success = callback.onSyncRequest("") // Passing empty string for now.
            if (success) {
                timeSyncManager?.let {
                    ProtocolHandler.SyncResult(
                        success = true,
                        phoneTimestamp = System.currentTimeMillis(),
                        offsetNs = 0L // This should be calculated by the sync manager.
                    )
                } ?: ProtocolHandler.SyncResult(success = false)
            } else {
                ProtocolHandler.SyncResult(success = false)
            }
        } ?: ProtocolHandler.SyncResult(
            success = false
        )
    }
}