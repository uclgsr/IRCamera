package mpdc4gsr.feature.connectivity.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.core.data.TimeSyncManager

/**
 * Central dispatcher for protocol messages exchanged with the external PC controller.
 *
 * Responsibilities:
 *  * keep lightweight session state (command handler, sync manager)
 *  * translate protocol messages into callback invocations
 *  * serialise responses / acknowledgements back to the PC controller
 */
class ProtocolHandler(
    private val context: Context,
    private val networkServer: NetworkServer,
) {

    companion object {
        private const val TAG = "ProtocolHandler"
    }

    /**
     * Callback hooks implemented by higher layers (typically [RecordingService] or
     * [CommandServer]) to actually perform the requested action.
     */
    interface CommandHandler {
        suspend fun onStartRecording(sessionId: String): CommandResult
        suspend fun onStopRecording(sessionId: String): CommandResult
        suspend fun onSyncRequest(pcTimestamp: Long): SyncResult
    }


    data class CommandResult(
        val success: Boolean,
        val message: String,
        val data: Map<String, String> = emptyMap(),
    )

    data class SyncResult(
        val success: Boolean,
        val phoneTimestamp: Long? = null,
        val offsetNs: Long? = null,
    )

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var commandHandler: CommandHandler? = null
    private var timeSyncManager: TimeSyncManager? = null

    fun setCommandHandler(handler: CommandHandler) {
        commandHandler = handler
    }


    fun setTimeSyncManager(manager: TimeSyncManager?) {
        timeSyncManager = manager
    }


    fun onClientConnected() {
        AppLogger.i(TAG, "Client connection established")
        // Optional: send diagnostics or reset state here if needed in the future.
    }


    fun onClientDisconnected() {
        AppLogger.i(TAG, "Client disconnected")
    }

    /**
     * Consume an incoming message, invoke the appropriate callback and, if necessary,
     * send a response back to the PC controller.
     */
    suspend fun handleIncomingMessage(message: Protocol.ProtocolMessage) {
        processMessage(message)?.let { response ->
            networkServer.sendMessage(response)
        }
    }

    /**
     * Process a message and return a response payload when a reply is necessary.
     * Returns `null` when no response is required.
     */
    suspend fun processMessage(message: Protocol.ProtocolMessage): String? {
        return try {
            when (message.type) {
                Protocol.MSG_HELLO -> {
                    AppLogger.i(TAG, "HELLO received from PC")
                    Protocol.createAckMessage(
                        Protocol.MSG_HELLO,
                        mapOf(
                            "device" to android.os.Build.MODEL,
                            "protocol_version" to Protocol.PROTOCOL_VERSION,
                        ),
                    )
                }


                Protocol.MSG_START_RECORD -> handleStartRecording(message)
                Protocol.MSG_STOP_RECORD -> handleStopRecording(message)
                Protocol.MSG_SYNC_REQUEST -> handleSyncRequest(message)
                else -> handleUnknown(message)
            }
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Failed to process message ${message.type}", t)
            Protocol.createErrorMessage(
                message.type,
                Protocol.ERR_FAIL,
                t.message ?: "Unhandled exception",
            )
        }
    }


    private suspend fun handleStartRecording(message: Protocol.ProtocolMessage): String {
        val sessionId = message.parameters["session_id"] ?: generateDefaultSessionId()
        val handler =
            commandHandler ?: return Protocol.createErrorMessage(
                Protocol.MSG_START_RECORD,
                Protocol.ERR_FAIL,
                "Command handler unavailable",
            )

        val result = handler.onStartRecording(sessionId)
            return if (result.success) {
            Protocol.createAckMessage(
                Protocol.MSG_START_RECORD,
                result.data + mapOf("session_id" to sessionId, "message" to result.message),
            )
        } else {
            Protocol.createErrorMessage(
                Protocol.MSG_START_RECORD,
                Protocol.ERR_FAIL,
                result.message,
            )
        }
    }


    private suspend fun handleStopRecording(message: Protocol.ProtocolMessage): String {
        val sessionId = message.parameters["session_id"] ?: generateDefaultSessionId()
        val handler =
            commandHandler ?: return Protocol.createErrorMessage(
                Protocol.MSG_STOP_RECORD,
                Protocol.ERR_FAIL,
                "Command handler unavailable",
            )

        val result = handler.onStopRecording(sessionId)
            return if (result.success) {
            Protocol.createAckMessage(
                Protocol.MSG_STOP_RECORD,
                result.data + mapOf("session_id" to sessionId, "message" to result.message),
            )
        } else {
            Protocol.createErrorMessage(
                Protocol.MSG_STOP_RECORD,
                Protocol.ERR_FAIL,
                result.message,
            )
        }
    }


    private suspend fun handleSyncRequest(message: Protocol.ProtocolMessage): String {
        val handler =
            commandHandler ?: return Protocol.createErrorMessage(
                Protocol.MSG_SYNC_REQUEST,
                Protocol.ERR_FAIL,
                "Command handler unavailable",
            )

        val pcTimestamp =
            message.parameters["t_pc"]?.toLongOrNull() ?: System.currentTimeMillis()
        val result = handler.onSyncRequest(pcTimestamp)
            return if (result.success) {
            val phoneTimestamp = result.phoneTimestamp ?: System.currentTimeMillis()
            val offsetNs = result.offsetNs ?: 0L
            timeSyncManager?.recordSyncResult(pcTimestamp, phoneTimestamp, offsetNs)
            Protocol.createSyncResultMessage(
                t1 = pcTimestamp,
                t2 = phoneTimestamp,
                t3 = phoneTimestamp,
                offsetMs = offsetNs / 1_000_000,
                rttMs = 0,
            )
        } else {
            Protocol.createErrorMessage(
                Protocol.MSG_SYNC_REQUEST,
                Protocol.ERR_FAIL,
                "Sync request rejected",
            )
        }
    }


    private fun handleUnknown(message: Protocol.ProtocolMessage): String {
        AppLogger.w(TAG, "Unsupported protocol message: ${message.type}")
            return Protocol.createErrorMessage(
            message.type,
            Protocol.ERR_FAIL,
            "Unsupported command",
        )
    }


    private fun generateDefaultSessionId(): String =
        "session_${System.currentTimeMillis()}"
}
