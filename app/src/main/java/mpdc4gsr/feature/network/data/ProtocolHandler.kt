package mpdc4gsr.feature.network.data

import android.content.Context
import mpdc4gsr.core.data.TimeSyncManager
import mpdc4gsr.core.data.utils.TimeManager

class ProtocolHandler(
    private val context: Context,
    private val networkServer: NetworkServer
) {
    companion object {    }

    private val timeManager = TimeManager.getInstance(context)
    private var timeSyncManager: TimeSyncManager? = null

    // Command callback interfaces
    interface CommandHandler {
        suspend fun onStartRecording(sessionId: String): CommandResult
        suspend fun onStopRecording(sessionId: String): CommandResult
        suspend fun onSyncRequest(pcTimestamp: Long): SyncResult
    }

    // Extended interface for cases that need configuration and client address
    interface CommandCallback {
        suspend fun onStartRecording(sessionId: String, configuration: org.json.JSONObject): Boolean
        suspend fun onStopRecording(): Boolean
        suspend fun onSyncRequest(pcAddress: String): Boolean
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

    fun setTimeSyncManager(syncManager: TimeSyncManager?) {
        timeSyncManager = syncManager
    }

    suspend fun processMessage(message: Protocol.ProtocolMessage): String? {        return when (message.type) {
            Protocol.MSG_SYNC_REQUEST -> handleSyncRequest(message)
            Protocol.MSG_SYNC_RESULT -> handleSyncResult(message)
            Protocol.MSG_START_RECORD -> handleStartRecord(message)
            Protocol.MSG_STOP_RECORD -> handleStopRecord(message)
            else -> {                Protocol.createErrorMessage(message.type, Protocol.ERR_FAIL, "Unknown command")
            }
        }
    }

    private suspend fun handleSyncRequest(message: Protocol.ProtocolMessage): String {
        return try {
            val pcTimestamp = message.parameters["t_pc"]?.toLong()
            if (pcTimestamp == null) {
                Protocol.createErrorMessage(
                    Protocol.MSG_SYNC_REQUEST,
                    Protocol.ERR_FAIL,
                    "Missing t_pc parameter"
                )
            } else {
                // Use TimeSyncManager if available for enhanced sync handling
                val syncManager = timeSyncManager
                if (syncManager != null) {
                    try {
                        val syncResult = syncManager.performSyncResponse(pcTimestamp)
                        if (syncResult.success) {
                            Protocol.createSyncResponseMessage(syncResult.t1, syncResult.t2)
                        } else {
                            Protocol.createErrorMessage(
                                Protocol.MSG_SYNC_REQUEST,
                                Protocol.ERR_FAIL,
                                "Sync failed"
                            )
                        }
                    } catch (e: Exception) {                        Protocol.createErrorMessage(
                            Protocol.MSG_SYNC_REQUEST,
                            Protocol.ERR_FAIL,
                            "Sync manager error"
                        )
                    }
                } else {
                    // Fallback to command handler or default behavior
                    val handler = commandHandler
                    if (handler != null) {
                        val syncResult = handler.onSyncRequest(pcTimestamp)
                        if (syncResult.success) {
                            Protocol.createSyncResponseMessage(
                                pcTimestamp,
                                syncResult.phoneTimestamp
                            )
                        } else {
                            Protocol.createErrorMessage(
                                Protocol.MSG_SYNC_REQUEST,
                                Protocol.ERR_FAIL,
                                "Sync failed"
                            )
                        }
                    } else {
                        // Default sync handling without callback
                        val phoneTime =
                            timeManager.getCurrentTimestampNs() / 1_000_000 // Convert to ms
                        Protocol.createSyncResponseMessage(pcTimestamp, phoneTime)
                    }
                }
            }
        } catch (e: Exception) {            Protocol.createErrorMessage(
                Protocol.MSG_SYNC_REQUEST,
                Protocol.ERR_FAIL,
                "Sync error: ${e.message}"
            )
        }
    }

    private suspend fun handleSyncResult(message: Protocol.ProtocolMessage): String? {
        return try {
            val syncManager = timeSyncManager
            if (syncManager == null) {                return null // No response needed for SYNC_RESULT
            }
            val t1 = message.parameters["t1"]?.toLong()
            val t2 = message.parameters["t2"]?.toLong()
            val t3 = message.parameters["t3"]?.toLong()
            val offset = message.parameters["offset"]?.toLong()
            val rtt = message.parameters["rtt"]?.toLong()
            if (t1 == null || t2 == null || t3 == null || offset == null || rtt == null) {                return null
            }
            // Complete the sync calculation with data from PC
            try {
                syncManager.completeSyncCalculation(t1, t2, t3, offset, rtt, 0)            } catch (e: Exception) {            }
            null // No response needed for SYNC_RESULT
        } catch (e: Exception) {            null
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
                        Protocol.createErrorMessage(
                            Protocol.MSG_START_RECORD,
                            Protocol.ERR_FAIL,
                            result.message
                        )
                    }
                } else {
                    Protocol.createErrorMessage(
                        Protocol.MSG_START_RECORD,
                        Protocol.ERR_FAIL,
                        "No command handler registered"
                    )
                }
            }
        } catch (e: Exception) {            Protocol.createErrorMessage(
                Protocol.MSG_START_RECORD,
                Protocol.ERR_FAIL,
                "Start error: ${e.message}"
            )
        }
    }

    private suspend fun handleStopRecord(message: Protocol.ProtocolMessage): String {
        return try {
            val sessionId = message.parameters["session_id"]
            if (sessionId.isNullOrEmpty()) {
                Protocol.createErrorMessage(
                    Protocol.MSG_STOP_RECORD,
                    Protocol.ERR_FAIL,
                    "Missing session_id parameter"
                )
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
                        Protocol.createErrorMessage(
                            Protocol.MSG_STOP_RECORD,
                            Protocol.ERR_FAIL,
                            result.message
                        )
                    }
                } else {
                    Protocol.createErrorMessage(
                        Protocol.MSG_STOP_RECORD,
                        Protocol.ERR_FAIL,
                        "No command handler registered"
                    )
                }
            }
        } catch (e: Exception) {            Protocol.createErrorMessage(
                Protocol.MSG_STOP_RECORD,
                Protocol.ERR_FAIL,
                "Stop error: ${e.message}"
            )
        }
    }

    suspend fun sendGsrData(timestamp: Long, value: Double): Boolean {
        val message = Protocol.createDataGsrMessage(timestamp, value)
        return networkServer.sendMessage(message)
    }

    suspend fun sendFrame(frameType: String, timestamp: Long, frameData: ByteArray): Boolean {
        val header = "${Protocol.MSG_FRAME} type=$frameType ts=$timestamp size=${frameData.size}"
        return networkServer.sendBinaryData(header, frameData)
    }

    suspend fun enablePreviewStreaming() {
        // Note: This would integrate with existing preview streaming infrastructure
        // For now, log that protocol-based streaming is enabled    }

    suspend fun disablePreviewStreaming() {    }
}