package mpdc4gsr.network

import android.content.Context
import android.util.Log
import mpdc4gsr.sync.TimeSyncManager
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
    private var timeSyncManager: TimeSyncManager? = null

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
     * Set the TimeSyncManager for enhanced sync handling
     */
    fun setTimeSyncManager(syncManager: TimeSyncManager?) {
        timeSyncManager = syncManager
    }

    /**
     * Process incoming protocol messages and return appropriate responses
     */
    suspend fun processMessage(message: Protocol.ProtocolMessage): String? {
        Log.d(TAG, "Processing protocol message: ${message.type}")

        return when (message.type) {
            Protocol.MSG_SYNC_REQUEST -> handleSyncRequest(message)
            Protocol.MSG_SYNC_RESULT -> handleSyncResult(message)
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
                // Use TimeSyncManager if available for enhanced sync handling
                val syncManager = timeSyncManager
                if (syncManager != null) {
                    try {
                        val syncResult = syncManager.performSyncResponse(pcTimestamp)
                        if (syncResult.success) {
                            Protocol.createSyncResponseMessage(syncResult.t1, syncResult.t2)
                        } else {
                            Protocol.createErrorMessage(Protocol.MSG_SYNC_REQUEST, Protocol.ERR_FAIL, "Sync failed")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "TimeSyncManager performSyncResponse failed", e)
                        Protocol.createErrorMessage(Protocol.MSG_SYNC_REQUEST, Protocol.ERR_FAIL, "Sync manager error")
                    }
                } else {
                    // Fallback to command handler or default behavior
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling sync request", e)
            Protocol.createErrorMessage(Protocol.MSG_SYNC_REQUEST, Protocol.ERR_FAIL, "Sync error: ${e.message}")
        }
    }

    /**
     * Handle SYNC_RESULT message from PC containing calculated offset and RTT
     */
    private suspend fun handleSyncResult(message: Protocol.ProtocolMessage): String? {
        return try {
            val syncManager = timeSyncManager
            if (syncManager == null) {
                Log.w(TAG, "No TimeSyncManager available for SYNC_RESULT")
                return null // No response needed for SYNC_RESULT
            }

            val t1 = message.parameters["t1"]?.toLong()
            val t2 = message.parameters["t2"]?.toLong()
            val t3 = message.parameters["t3"]?.toLong()
            val offset = message.parameters["offset"]?.toLong()
            val rtt = message.parameters["rtt"]?.toLong()

            if (t1 == null || t2 == null || t3 == null || offset == null || rtt == null) {
                Log.w(TAG, "SYNC_RESULT missing required parameters")
                return null
            }

            // Complete the sync calculation with data from PC
            try {
                syncManager.completeSyncCalculation(t1, t2, t3, offset, rtt, 0)
                Log.d(TAG, "SYNC_RESULT processed: offset=${offset}ms, rtt=${rtt}ms")
            } catch (e: Exception) {
                Log.w(TAG, "TimeSyncManager completeSyncCalculation failed", e)
            }

            null // No response needed for SYNC_RESULT
        } catch (e: Exception) {
            Log.e(TAG, "Error handling sync result", e)
            null
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