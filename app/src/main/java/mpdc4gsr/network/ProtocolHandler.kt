package mpdc4gsr.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mpdc4gsr.utils.TimeManager

/**
 * Handles protocol messages according to the standardized networking specification.
 * Processes incoming commands from PC and generates appropriate responses.
 */
class ProtocolHandler(
    private val context: Context,
    private val networkServer: NetworkServer
) {
    companion object {
        private const val TAG = "ProtocolHandler"
    }

    private val timeManager = TimeManager.getInstance(context)

    // Command callback interfaces
    interface CommandHandler {
        suspend fun onStartRecording(sessionId: String): CommandResult
        suspend fun onStopRecording(sessionId: String): CommandResult
        suspend fun onSyncRequest(pcTimestamp: Long): SyncResult
    }

    data class CommandResult(
        val success: Boolean,
        val message: String = "",
        val data: Map<String, String> = emptyMap()
    )

    data class SyncResult(
        val success: Boolean,
        val phoneTimestamp: Long = 0L,
        val offsetNs: Long = 0L
    )

    private var commandHandler: CommandHandler? = null

    fun setCommandHandler(handler: CommandHandler) {
        commandHandler = handler
    }

    /**
     * Process incoming protocol messages and return appropriate responses
     */
    suspend fun processMessage(message: Protocol.ProtocolMessage): String? {
        Log.d(TAG, "Processing protocol message: ${message.type}")

        return when (message.type) {
            Protocol.MSG_SYNC_REQUEST -> handleSyncRequest(message)
            Protocol.MSG_START_RECORD -> handleStartRecord(message)
            Protocol.MSG_STOP_RECORD -> handleStopRecord(message)
            else -> {
                Log.w(TAG, "Unknown message type: ${message.type}")
                Protocol.createErrorMessage(message.type, Protocol.ERR_FAIL, "Unknown command")
            }
        }
    }

    private suspend fun handleSyncRequest(message: Protocol.ProtocolMessage): String {
        return try {
            val pcTimestamp = message.parameters["t_pc"]?.toLong()
            if (pcTimestamp == null) {
                Protocol.createErrorMessage(Protocol.MSG_SYNC_REQUEST, Protocol.ERR_FAIL, "Missing t_pc parameter")
            } else {
                val handler = commandHandler
                if (handler != null) {
                    val syncResult = handler.onSyncRequest(pcTimestamp)
                    if (syncResult.success) {
                        Protocol.createSyncResponseMessage(pcTimestamp, syncResult.phoneTimestamp)
                    } else {
                        Protocol.createErrorMessage(Protocol.MSG_SYNC_REQUEST, Protocol.ERR_FAIL, "Sync failed")
                    }
                } else {
                    // Default sync handling without callback
                    val phoneTime = timeManager.getCurrentTimestampNs() / 1_000_000 // Convert to ms
                    Protocol.createSyncResponseMessage(pcTimestamp, phoneTime)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling sync request", e)
            Protocol.createErrorMessage(Protocol.MSG_SYNC_REQUEST, Protocol.ERR_FAIL, "Sync error: ${e.message}")
        }
    }

    private suspend fun handleStartRecord(message: Protocol.ProtocolMessage): String {
        return try {
            val sessionId = message.parameters["session_id"]
            if (sessionId.isNullOrEmpty()) {
                Protocol.createErrorMessage(
                    Protocol.MSG_START_RECORD,
                    Protocol.ERR_FAIL,
                    "Missing session_id parameter"
                )
            } else {
                val handler = commandHandler
                if (handler != null) {
                    val result = handler.onStartRecording(sessionId)
                    if (result.success) {
                        Protocol.createAckMessage(
                            Protocol.MSG_START_RECORD,
                            mapOf("session_id" to sessionId) + result.data
                        )
                    } else {
                        Protocol.createErrorMessage(Protocol.MSG_START_RECORD, Protocol.ERR_FAIL, result.message)
                    }
                } else {
                    Protocol.createErrorMessage(
                        Protocol.MSG_START_RECORD,
                        Protocol.ERR_FAIL,
                        "No command handler registered"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling start record", e)
            Protocol.createErrorMessage(Protocol.MSG_START_RECORD, Protocol.ERR_FAIL, "Start error: ${e.message}")
        }
    }

    private suspend fun handleStopRecord(message: Protocol.ProtocolMessage): String {
        return try {
            val sessionId = message.parameters["session_id"]
            if (sessionId.isNullOrEmpty()) {
                Protocol.createErrorMessage(Protocol.MSG_STOP_RECORD, Protocol.ERR_FAIL, "Missing session_id parameter")
            } else {
                val handler = commandHandler
                if (handler != null) {
                    val result = handler.onStopRecording(sessionId)
                    if (result.success) {
                        Protocol.createAckMessage(
                            Protocol.MSG_STOP_RECORD,
                            mapOf("session_id" to sessionId) + result.data
                        )
                    } else {
                        Protocol.createErrorMessage(Protocol.MSG_STOP_RECORD, Protocol.ERR_FAIL, result.message)
                    }
                } else {
                    Protocol.createErrorMessage(
                        Protocol.MSG_STOP_RECORD,
                        Protocol.ERR_FAIL,
                        "No command handler registered"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling stop record", e)
            Protocol.createErrorMessage(Protocol.MSG_STOP_RECORD, Protocol.ERR_FAIL, "Stop error: ${e.message}")
        }
    }

    /**
     * Send a GSR data update to the PC
     */
    suspend fun sendGsrData(timestamp: Long, value: Double): Boolean {
        val message = Protocol.createDataGsrMessage(timestamp, value)
        return networkServer.sendMessage(message)
    }

    /**
     * Send a thermal/RGB frame to the PC
     */
    suspend fun sendFrame(frameType: String, timestamp: Long, frameData: ByteArray): Boolean {
        val header = "${Protocol.MSG_FRAME} type=$frameType ts=$timestamp size=${frameData.size}"
        return networkServer.sendBinaryData(header, frameData)
    }

    /**
     * Start live preview streaming to PC
     * This integrates with the existing PreviewStreamer but sends protocol messages
     */
    suspend fun enablePreviewStreaming() {
        // Note: This would integrate with existing preview streaming infrastructure
        // For now, log that protocol-based streaming is enabled
        Log.i(TAG, "Protocol-based preview streaming enabled")
    }

    /**
     * Stop live preview streaming
     */
    suspend fun disablePreviewStreaming() {
        Log.i(TAG, "Protocol-based preview streaming disabled")
    }
}