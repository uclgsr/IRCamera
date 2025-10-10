package mpdc4gsr.feature.connectivity.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CommandHandler(
    private val recordingController: RecordingController,
    private val networkManager: NetworkManager
) {
    companion object {
        private const val STATUS_UPDATE_INTERVAL_MS = 5000L
    }


    private val handlerScope = CoroutineScope(Dispatchers.IO)

    suspend fun handleCommand(commandLine: String) {
        try {
            val response = when {
                commandLine.startsWith("START") -> handleStartCommand(commandLine)
                commandLine.startsWith("STOP") -> handleStopCommand(commandLine)
                commandLine.startsWith("SYNC") -> handleSyncCommand(commandLine)
                commandLine.startsWith("PING") -> handlePingCommand()
                commandLine.startsWith("GET_STATUS") -> handleGetStatusCommand()
                commandLine.startsWith("{") -> handleJsonCommand(commandLine)
                else -> {
                    "ERROR cmd=UNKNOWN code=UNKNOWN_COMMAND msg=\"Unknown command: $commandLine\""
                }
            }

            if (response.isNotEmpty()) {
                networkManager.sendResponse(response)
            }
        } catch (e: Exception) {
            val errorResponse =
                "ERROR cmd=UNKNOWN code=HANDLER_ERROR msg=\"Command handler error: ${e.message}\""
            networkManager.sendResponse(errorResponse)
        }
    }


    private suspend fun handleStartCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
            try {
                if (recordingController.isRecording) {
                    return@withContext "ERROR cmd=START code=ALREADY_RECORDING msg=\"Recording session already active\""
                }

                val success = recordingController.startRecording(
                    triggerSource = RecordingController.TriggerSource.REMOTE_PC
                )
                if (success) {
                    val sessionInfo = recordingController.getCurrentSessionId() ?: "unknown"
                    "START-ACK session_id=${sessionInfo}"
                } else {
                    "ERROR cmd=START code=START_FAILED msg=\"Failed to start recording session\""
                }
            } catch (e: Exception) {
                "ERROR cmd=START code=START_EXCEPTION msg=\"Start error: ${e.message}\""
            }
        }


    private suspend fun handleStopCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
            try {
                if (!recordingController.isRecording) {
                    return@withContext "STOP-ACK msg=\"No active recording session\""
                }

                val success = recordingController.stopRecording(
                    triggerSource = RecordingController.TriggerSource.REMOTE_PC
                )
                if (success) {
                    "STOP-ACK msg=\"Recording session stopped\""
                } else {
                    "ERROR cmd=STOP code=STOP_FAILED msg=\"Failed to stop recording session\""
                }
            } catch (e: Exception) {
                "ERROR cmd=STOP code=STOP_EXCEPTION msg=\"Stop error: ${e.message}\""
            }
        }


    private suspend fun handleSyncCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
            try {
                val phoneTimestamp = System.currentTimeMillis()
                // Extract PC timestamp if provided in the command
                val pcTimestamp = extractTimestampFromCommand(commandLine)
                if (pcTimestamp != null) {
                    "SYNC-RESP t_pc=$pcTimestamp t_ph=$phoneTimestamp"
                } else {
                    "SYNC-RESP t_ph=$phoneTimestamp"
                }
            } catch (e: Exception) {
                "ERROR cmd=SYNC code=SYNC_EXCEPTION msg=\"Sync error: ${e.message}\""
            }
        }


    private fun handlePingCommand(): String {
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
            val statusJson =
                JSONObject().apply {
                    put("status", status)
                    put("uptime", uptime)
                    put("sensors", sensors)
                    put("timestamp", System.currentTimeMillis())
                }
            "STATUS $statusJson"
        } catch (e: Exception) {
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


    fun startPeriodicStatusUpdates() {
        handlerScope.launch {
            while (true) {
                kotlinx.coroutines.delay(STATUS_UPDATE_INTERVAL_MS)
                if (recordingController.isRecording) {
                    try {
                        val statusResponse = handleGetStatusCommand()
                        networkManager.sendTelemetry(statusResponse)
                    } catch (e: Exception) {
                        mpdc4gsr.core.common.AppLogger.e(
                            "CommandHandler",
                            "Unexpected Exception in CommandHandler catch block",
                            e
                        )
                    }
                }
            }
        }
    }


    fun notifySessionStarted(sessionId: String) {
        handlerScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val message =
                    "STATUS Recording started at $timestamp, session: $sessionId, sensors: [RGB,Thermal,GSR]"
                networkManager.sendTelemetry(message)
            } catch (e: Exception) {
                mpdc4gsr.core.common.AppLogger.e(
                    "CommandHandler",
                    "Unexpected Exception in CommandHandler catch block",
                    e
                )
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
                mpdc4gsr.core.common.AppLogger.e(
                    "CommandHandler",
                    "Unexpected Exception in CommandHandler catch block",
                    e
                )
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
                mpdc4gsr.core.common.AppLogger.e(
                    "CommandHandler",
                    "Unexpected Exception in CommandHandler catch block",
                    e
                )
            }
        }
    }
}

