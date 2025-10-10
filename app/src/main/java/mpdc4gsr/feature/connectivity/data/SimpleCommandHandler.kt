package mpdc4gsr.feature.connectivity.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SimpleCommandHandler(
    private val recordingController: SimpleRecordingInterface,
    private val networkManager: NetworkManager
) {
    companion object {
        private const val STATUS_UPDATE_INTERVAL_MS = 5000L
    }

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    suspend fun handleCommand(commandLine: String) {
        try {
            val response = when {
                commandLine.startsWith("START") -> handleStartCommand()
                commandLine.startsWith("STOP") -> handleStopCommand()
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

    private suspend fun handleStartCommand(): String = withContext(Dispatchers.IO) {
        try {
            if (recordingController.isRecording) {
                return@withContext "ERROR cmd=START code=ALREADY_RECORDING msg=\"Recording session already active\""
            }
            val success = recordingController.startRecording()
            if (success) {
                val sessionId = "session_${System.currentTimeMillis()}"
                "START-ACK session_id=$sessionId"
            } else {
                "ERROR cmd=START code=START_FAILED msg=\"Failed to start recording session\""
            }
        } catch (e: Exception) {
            "ERROR cmd=START code=START_EXCEPTION msg=\"Start error: ${e.message}\""
        }
    }

    private suspend fun handleStopCommand(): String = withContext(Dispatchers.IO) {
        try {
            if (!recordingController.isRecording) {
                return@withContext "STOP-ACK msg=\"No active recording session\""
            }
            val success = recordingController.stopRecording()
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
            val statusMap = recordingController.getStatus()
            // Create JSON response for rich status info
            val statusJson = JSONObject().apply {
                statusMap.forEach { (key, value) ->
                    put(key, value)
                }
            }            "STATUS $statusJson"
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
                    "START" -> handleStartCommand()
                    "STOP" -> handleStopCommand()
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
                            "SimpleCommandHandler",
                            "Unexpected Exception in SimpleCommandHandler catch block",
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
                    "SimpleCommandHandler",
                    "Unexpected Exception in SimpleCommandHandler catch block",
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
                    "SimpleCommandHandler",
                    "Unexpected Exception in SimpleCommandHandler catch block",
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
                    "SimpleCommandHandler",
                    "Unexpected Exception in SimpleCommandHandler catch block",
                    e
                )
            }
        }
    }
}
