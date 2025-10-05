package mpdc4gsr.feature.network.data

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Handles incoming commands from PC and executes appropriate actions.
 * Processes START, STOP, SYNC, PING, and GET_STATUS commands as specified in the issue.
 */
class CommandHandler(
    private val recordingController: ComprehensiveRecordingController,
    private val networkManager: NetworkManager
) {
    companion object {
        private const val TAG = "CommandHandler"
        private const val STATUS_UPDATE_INTERVAL_MS = 5000L
    }

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    /**
     * Process incoming command from PC
     */
    suspend fun handleCommand(commandLine: String) {
        try {
            AppLogger.d(TAG, "Processing command: $commandLine")

            val response = when {
                commandLine.startsWith("START") -> handleStartCommand(commandLine)
                commandLine.startsWith("STOP") -> handleStopCommand(commandLine)
                commandLine.startsWith("SYNC") -> handleSyncCommand(commandLine)
                commandLine.startsWith("PING") -> handlePingCommand()
                commandLine.startsWith("GET_STATUS") -> handleGetStatusCommand()
                commandLine.startsWith("{") -> handleJsonCommand(commandLine)
                else -> {
                    AppLogger.w(TAG, "Unknown command: $commandLine")
                    "ERROR cmd=UNKNOWN code=UNKNOWN_COMMAND msg=\"Unknown command: $commandLine\""
                }
            }

            if (response.isNotEmpty()) {
                networkManager.sendResponse(response)
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling command: $commandLine", e)
            val errorResponse =
                "ERROR cmd=UNKNOWN code=HANDLER_ERROR msg=\"Command handler error: ${e.message}\""
            networkManager.sendResponse(errorResponse)
        }
    }

    private suspend fun handleStartCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
            try {
                if (recordingController.isRecording) {
                    AppLogger.w(TAG, "START command received but already recording")
                    return@withContext "ERROR cmd=START code=ALREADY_RECORDING msg=\"Recording session already active\""
                }

                AppLogger.i(TAG, "Executing START command")
                val success = recordingController.startRecording()

                if (success) {
                    AppLogger.i(TAG, "Recording started successfully via remote command")
                    // Send acknowledgment with session info
                    val sessionInfo = "session_started"
                    "START-ACK session_id=${sessionInfo}"
                } else {
                    AppLogger.e(TAG, "Failed to start recording via remote command")
                    "ERROR cmd=START code=START_FAILED msg=\"Failed to start recording session\""
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Exception during START command", e)
                "ERROR cmd=START code=START_EXCEPTION msg=\"Start error: ${e.message}\""
            }
        }

    private suspend fun handleStopCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
            try {
                if (!recordingController.isRecording) {
                    AppLogger.i(TAG, "STOP command received but not currently recording")
                    return@withContext "STOP-ACK msg=\"No active recording session\""
                }

                AppLogger.i(TAG, "Executing STOP command")
                val success = recordingController.stopRecording()

                if (success) {
                    AppLogger.i(TAG, "Recording stopped successfully via remote command")
                    "STOP-ACK msg=\"Recording session stopped\""
                } else {
                    AppLogger.e(TAG, "Failed to stop recording via remote command")
                    "ERROR cmd=STOP code=STOP_FAILED msg=\"Failed to stop recording session\""
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Exception during STOP command", e)
                "ERROR cmd=STOP code=STOP_EXCEPTION msg=\"Stop error: ${e.message}\""
            }
        }

    private suspend fun handleSyncCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
            try {
                AppLogger.i(TAG, "Executing SYNC command")
                val phoneTimestamp = System.currentTimeMillis()

                // Extract PC timestamp if provided in the command
                val pcTimestamp = extractTimestampFromCommand(commandLine)

                if (pcTimestamp != null) {
                    Log.d(
                        TAG,
                        "Clock sync - PC timestamp: $pcTimestamp, Phone timestamp: $phoneTimestamp"
                    )
                    "SYNC-RESP t_pc=$pcTimestamp t_ph=$phoneTimestamp"
                } else {
                    AppLogger.d(TAG, "Clock sync - Phone timestamp: $phoneTimestamp")
                    "SYNC-RESP t_ph=$phoneTimestamp"
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Exception during SYNC command", e)
                "ERROR cmd=SYNC code=SYNC_EXCEPTION msg=\"Sync error: ${e.message}\""
            }
        }

    private fun handlePingCommand(): String {
        AppLogger.d(TAG, "Responding to PING")
        return "PONG"
    }

    private suspend fun handleGetStatusCommand(): String = withContext(Dispatchers.IO) {
        try {
            val status = if (recordingController.isRecording) "recording" else "idle"
            val uptime = System.currentTimeMillis() / 1000 // seconds since epoch

            // Build sensor list (this could be enhanced to query actual sensor states)
            val sensors = mutableListOf<String>()
            if (recordingController.isRecording) {
                sensors.addAll(listOf("RGB", "Thermal", "GSR"))
            }

            // Create JSON response for rich status info
            val statusJson = JSONObject().apply {
                put("status", status)
                put("uptime", uptime)
                put("sensors", sensors)
                put("timestamp", System.currentTimeMillis())
            }

            AppLogger.d(TAG, "Status query response: $statusJson")
            "STATUS $statusJson"
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception during GET_STATUS command", e)
            "ERROR cmd=GET_STATUS code=STATUS_EXCEPTION msg=\"Status error: ${e.message}\""
        }
    }

    private suspend fun handleJsonCommand(jsonString: String): String =
        withContext(Dispatchers.IO) {
            try {
                val jsonObj = JSONObject(jsonString)
                val command = jsonObj.optString("cmd", "")

                return@withContext when (command) {
                    "START" -> handleStartCommand("START")
                    "STOP" -> handleStopCommand("STOP")
                    "SYNC" -> {
                        val pcTimestamp = jsonObj.optLong("t_pc", -1L)
                        val syncCmd = if (pcTimestamp > 0) "SYNC t_pc=$pcTimestamp" else "SYNC"
                        handleSyncCommand(syncCmd)
                    }

                    "PING" -> handlePingCommand()
                    "GET_STATUS" -> handleGetStatusCommand()
                    else -> "ERROR cmd=$command code=UNKNOWN_JSON_COMMAND msg=\"Unknown JSON command: $command\""
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error processing JSON command: $jsonString", e)
                "ERROR cmd=JSON code=JSON_PARSE_ERROR msg=\"Invalid JSON command: ${e.message}\""
            }
        }

    private fun extractTimestampFromCommand(commandLine: String): Long? {
        return try {
            val regex = Regex("t_pc=(\\d+)")
            val matchResult = regex.find(commandLine)
            matchResult?.groups?.get(1)?.value?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send periodic status updates while recording
     */
    fun startPeriodicStatusUpdates() {
        handlerScope.launch {
            while (true) {
                kotlinx.coroutines.delay(STATUS_UPDATE_INTERVAL_MS)

                if (recordingController.isRecording) {
                    try {
                        val statusResponse = handleGetStatusCommand()
                        networkManager.sendTelemetry(statusResponse)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Error sending periodic status update", e)
                    }
                }
            }
        }
    }

    /**
     * Send event-driven notifications for significant events
     */
    fun notifySessionStarted(sessionId: String) {
        handlerScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val message =
                    "STATUS Recording started at $timestamp, session: $sessionId, sensors: [RGB,Thermal,GSR]"
                networkManager.sendTelemetry(message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error sending session started notification", e)
            }
        }
    }

    fun notifySessionStopped(sessionId: String, duration: Long) {
        handlerScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val message =
                    "STATUS Recording stopped at $timestamp, duration: ${duration}ms, files saved"
                networkManager.sendTelemetry(message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error sending session stopped notification", e)
            }
        }
    }

    fun notifyError(errorType: String, errorMessage: String) {
        handlerScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val message = "WARN $errorType at $timestamp: $errorMessage"
                networkManager.sendTelemetry(message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error sending error notification", e)
            }
        }
    }
}