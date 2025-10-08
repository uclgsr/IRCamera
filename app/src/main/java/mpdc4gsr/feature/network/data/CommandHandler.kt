package mpdc4gsr.feature.network.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CommandHandler(
    private val recordingController: ComprehensiveRecordingController,
    private val networkManager: NetworkManager
) {
    companion object {
        private const val STATUS_UPDATE_INTERVAL_MS = 5000L
    }

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    suspend fun handleCommand(commandLine: String) {
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
            val errorResponse =
            networkManager.sendResponse(errorResponse)
        }
    }

    private suspend fun handleStartCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
                if (recordingController.isRecording) {
                    return@withContext "ERROR cmd=START code=ALREADY_RECORDING msg=\"Recording session already active\""
                }
                val success = recordingController.startRecording()
                if (success) {
                    // Send acknowledgment with session info
                    val sessionInfo = "session_started"
                    "START-ACK session_id=${sessionInfo}"
                } else {
                    "ERROR cmd=START code=START_FAILED msg=\"Failed to start recording session\""
                }
            }
        }

    private suspend fun handleStopCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
                if (!recordingController.isRecording) {
                    return@withContext "STOP-ACK msg=\"No active recording session\""
                }
                val success = recordingController.stopRecording()
                if (success) {
                    "STOP-ACK msg=\"Recording session stopped\""
                } else {
                    "ERROR cmd=STOP code=STOP_FAILED msg=\"Failed to stop recording session\""
                }
            }
        }

    private suspend fun handleSyncCommand(commandLine: String): String =
        withContext(Dispatchers.IO) {
                val phoneTimestamp = System.currentTimeMillis()
                // Extract PC timestamp if provided in the command
                val pcTimestamp = extractTimestampFromCommand(commandLine)
                if (pcTimestamp != null) {
                        TAG,
                        "Clock sync - PC timestamp: $pcTimestamp, Phone timestamp: $phoneTimestamp"
                    )
                    "SYNC-RESP t_pc=$pcTimestamp t_ph=$phoneTimestamp"
                } else {
                    "SYNC-RESP t_ph=$phoneTimestamp"
                }
            }
        }

    private fun handlePingCommand(): String {
        return "PONG"
    }

    private suspend fun handleGetStatusCommand(): String = withContext(Dispatchers.IO) {
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
            "STATUS $statusJson"
        }
    }

    private suspend fun handleJsonCommand(jsonString: String): String =
        withContext(Dispatchers.IO) {
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
            }
        }

    private fun extractTimestampFromCommand(commandLine: String): Long? {
        return (
            val regex = Regex("t_pc=(\\d+)")
            val matchResult = regex.find(commandLine)
            matchResult?.groups?.get(1)?.value?.toLong()
            null
        }
    }

    fun startPeriodicStatusUpdates() {
        handlerScope.launch {
            while (true) {
                kotlinx.coroutines.delay(STATUS_UPDATE_INTERVAL_MS)
                if (recordingController.isRecording) {
                        val statusResponse = handleGetStatusCommand()
                        networkManager.sendTelemetry(statusResponse)
                    }
                }
            }
        }
    }

    fun notifySessionStarted(sessionId: String) {
        handlerScope.launch {
                val timestamp = System.currentTimeMillis()
                val message =
                    "STATUS Recording started at $timestamp, session: $sessionId, sensors: [RGB,Thermal,GSR]"
                networkManager.sendTelemetry(message)
            }
        }
    }

    fun notifySessionStopped(sessionId: String, duration: Long) {
        handlerScope.launch {
                val timestamp = System.currentTimeMillis()
                val message =
                    "STATUS Recording stopped at $timestamp, duration: ${duration}ms, files saved"
                networkManager.sendTelemetry(message)
            }
        }
    }

    fun notifyError(errorType: String, errorMessage: String) {
        handlerScope.launch {
                val timestamp = System.currentTimeMillis()
                val message = "WARN $errorType at $timestamp: $errorMessage"
                networkManager.sendTelemetry(message)
            }
        }
    }
}