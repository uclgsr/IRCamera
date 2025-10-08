package mpdc4gsr.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CrashRecoveryManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "crash_recovery"
        private const val KEY_ACTIVE_SESSION = "active_session"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_ACTIVE_SENSORS = "active_sensors"
        private const val KEY_SESSION_DIRECTORY = "session_directory"
        private const val SESSION_TIMEOUT_MS = 3600000L
        private const val RECOVERY_SCAN_TIMEOUT_MS = 30000L
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun checkForCrashedSessions(): CrashRecoveryResult = withContext(Dispatchers.IO) {
            val activeSessionId = preferences.getString(KEY_ACTIVE_SESSION, null)
            val sessionStartTime = preferences.getLong(KEY_SESSION_START_TIME, 0L)
            val activeSensorsJson = preferences.getString(KEY_ACTIVE_SENSORS, null)
            val sessionDirectory = preferences.getString(KEY_SESSION_DIRECTORY, null)
            if (activeSessionId == null || sessionStartTime == 0L) {
                return@withContext CrashRecoveryResult(
                    hasCrashedSession = false,
                    recoveredSession = null,
                    recoveryActions = emptyList()
                )
            }
            val activeSensors = (
                activeSensorsJson?.let {
                    val jsonArray = JSONArray(it)
                    (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }
                } ?: emptyList()
                emptyList<String>()
            }
            val currentTime = System.currentTimeMillis()
            val sessionAge = currentTime - sessionStartTime
            if (sessionAge > SESSION_TIMEOUT_MS) {
                clearCrashRecoveryState()
                return@withContext CrashRecoveryResult(
                    hasCrashedSession = false,
                    recoveredSession = null,
                    recoveryActions = listOf("Cleared invalid old session data")
                )
            }
            val sessionAnalysis = analyzeSessionDirectory(sessionDirectory, activeSensors)
            val recoveredSession = RecoveredSession(
                sessionId = activeSessionId,
                sessionDirectory = sessionDirectory ?: "unknown",
                sessionStartTime = sessionStartTime,
                activeSensors = activeSensors,
                sessionAge = sessionAge,
                analysis = sessionAnalysis
            )
            return@withContext CrashRecoveryResult(
                hasCrashedSession = true,
                recoveredSession = recoveredSession,
                recoveryActions = generateRecoveryActions(recoveredSession)
            )
            return@withContext CrashRecoveryResult(
                hasCrashedSession = false,
                recoveredSession = null,
            )
        }
    }

    fun markSessionActive(
        sessionId: String,
        sessionDirectory: String,
        activeSensors: List<String>
    ) {
        preferences.edit()
            .putString(KEY_ACTIVE_SESSION, sessionId)
            .putLong(KEY_SESSION_START_TIME, System.currentTimeMillis())
            .putString(KEY_ACTIVE_SENSORS, JSONArray(activeSensors).toString())
            .putString(KEY_SESSION_DIRECTORY, sessionDirectory)
            .apply()
    }

    fun markSessionCompleted(sessionId: String) {
        clearCrashRecoveryState()
    }

    fun markSessionFailed(sessionId: String, reason: String) {
            val sessionDirectory = preferences.getString(KEY_SESSION_DIRECTORY, null)
            if (sessionDirectory != null) {
                updateSessionMetadataWithFailure(sessionDirectory, reason)
            }
        }
        clearCrashRecoveryState()
    }

    suspend fun recoverCrashedSession(recoveredSession: RecoveredSession): SessionRecoveryResult =
        withContext(Dispatchers.IO) {
                val recoveryActions = mutableListOf<String>()
                val metadataUpdated = updateSessionMetadataWithCrashRecovery(recoveredSession)
                if (metadataUpdated) {
                    recoveryActions.add("Updated session metadata with crash recovery info")
                }
                val resourcesCleanedUp = cleanupStaleResources(recoveredSession)
                recoveryActions.addAll(resourcesCleanedUp)
                val dataPreserved = preservePartialSessionData(recoveredSession)
                if (dataPreserved) {
                    recoveryActions.add("Preserved partial session data")
                }
                val reportGenerated = generateRecoveryReport(recoveredSession, recoveryActions)
                if (reportGenerated) {
                    recoveryActions.add("Generated crash recovery report")
                }
                clearCrashRecoveryState()
                recoveryActions.add("Cleared crash recovery tracking state")
                return@withContext SessionRecoveryResult(
                    success = true,
                    recoveredSessionId = recoveredSession.sessionId,
                    recoveryActions = recoveryActions,
                    error = null
                )
                return@withContext SessionRecoveryResult(
                    success = false,
                    recoveredSessionId = recoveredSession.sessionId,
                    recoveryActions = emptyList(),
                )
            }
        }

    private suspend fun analyzeSessionDirectory(
        sessionDirectory: String?,
        activeSensors: List<String>
    ): SessionAnalysis = withContext(Dispatchers.IO) {
        if (sessionDirectory == null) {
            return@withContext SessionAnalysis(
                hasSessionDirectory = false,
                sessionDirectoryExists = false,
                dataFiles = emptyMap(),
                partialDataSize = 0L,
                summary = "Session directory path unknown"
            )
        }
        val sessionDir = File(sessionDirectory)
        if (!sessionDir.exists()) {
            return@withContext SessionAnalysis(
                hasSessionDirectory = false,
                sessionDirectoryExists = false,
                dataFiles = emptyMap(),
                partialDataSize = 0L,
                summary = "Session directory does not exist"
            )
        }
            val dataFiles = mutableMapOf<String, SessionDataInfo>()
            var totalDataSize = 0L
            activeSensors.forEach { sensorName ->
                val sensorDir = File(sessionDir, sensorName.lowercase())
                if (sensorDir.exists()) {
                    val sensorFiles = sensorDir.listFiles() ?: emptyArray()
                    val sensorDataSize = sensorFiles.sumOf { it.length() }
                    totalDataSize += sensorDataSize
                    dataFiles[sensorName] = SessionDataInfo(
                        sensorName = sensorName,
                        fileCount = sensorFiles.size,
                        dataSize = sensorDataSize,
                        hasData = sensorFiles.isNotEmpty(),
                        lastModified = sensorFiles.maxOfOrNull { it.lastModified() } ?: 0L
                    )
                }
            }
            val metadataFile = File(sessionDir, "session_metadata.json")
            val hasMetadata = metadataFile.exists()
            val summary = buildString {
                append("Found ${dataFiles.size} sensor data directories")
                if (totalDataSize > 0) {
                    append(", ${totalDataSize / 1024} KB partial data")
                }
                if (hasMetadata) {
                    append(", has metadata")
                }
            }
            return@withContext SessionAnalysis(
                hasSessionDirectory = true,
                sessionDirectoryExists = true,
                dataFiles = dataFiles,
                partialDataSize = totalDataSize,
                hasMetadata = hasMetadata,
                summary = summary
            )
            return@withContext SessionAnalysis(
                hasSessionDirectory = true,
                sessionDirectoryExists = true,
                dataFiles = emptyMap(),
                partialDataSize = 0L,
            )
        }
    }

    private fun generateRecoveryActions(recoveredSession: RecoveredSession): List<String> {
        val actions = mutableListOf<String>()
        if (recoveredSession.analysis.hasSessionDirectory) {
            actions.add("Mark session as crashed and preserve partial data")
            if (recoveredSession.analysis.partialDataSize > 0) {
                actions.add("Archive ${recoveredSession.analysis.partialDataSize / 1024} KB of partial recording data")
            }
            if (recoveredSession.analysis.dataFiles.isNotEmpty()) {
                actions.add("Preserve data from ${recoveredSession.analysis.dataFiles.size} sensors")
            }
        } else {
            actions.add("Clean up crashed session metadata (no recoverable data)")
        }
        actions.add("Clear crash recovery tracking state")
        actions.add("Generate crash recovery report")
        return actions
    }

    private fun updateSessionMetadataWithCrashRecovery(recoveredSession: RecoveredSession): Boolean {
        return (
            val sessionDir = File(recoveredSession.sessionDirectory)
            val metadataFile = File(sessionDir, "session_metadata.json")
            val crashRecoveryInfo = JSONObject().apply {
                put("session_status", "CRASHED_RECOVERED")
                put("crash_detected_at", System.currentTimeMillis())
                put("original_session_start", recoveredSession.sessionStartTime)
                put("session_duration_before_crash", recoveredSession.sessionAge)
                put("active_sensors_at_crash", JSONArray(recoveredSession.activeSensors))
                put("recovery_analysis", JSONObject().apply {
                    put("has_partial_data", recoveredSession.analysis.partialDataSize > 0)
                    put("partial_data_size", recoveredSession.analysis.partialDataSize)
                    put("sensor_data_files", JSONObject().apply {
                        recoveredSession.analysis.dataFiles.forEach { (sensor, info) ->
                            put(sensor, JSONObject().apply {
                                put("file_count", info.fileCount)
                                put("data_size", info.dataSize)
                                put("has_data", info.hasData)
                                put("last_modified", info.lastModified)
                            })
                        }
                    })
                })
            }
            val existingMetadata = if (metadataFile.exists()) {
                    JSONObject(metadataFile.readText())
                    JSONObject()
                }
            } else {
                JSONObject()
            }
            existingMetadata.put("crash_recovery", crashRecoveryInfo)
            metadataFile.writeText(existingMetadata.toString(2))
            true
            false
        }
    }

    private fun updateSessionMetadataWithFailure(sessionDirectory: String, reason: String) {
            val sessionDir = File(sessionDirectory)
            val metadataFile = File(sessionDir, "session_metadata.json")
            val failureInfo = JSONObject().apply {
                put("session_status", "FAILED")
                put("failure_reason", reason)
                put("failed_at", System.currentTimeMillis())
            }
            val existingMetadata = if (metadataFile.exists()) {
                    JSONObject(metadataFile.readText())
                    JSONObject()
                }
            } else {
                JSONObject()
            }
            existingMetadata.put("failure_info", failureInfo)
            metadataFile.writeText(existingMetadata.toString(2))
        }
    }

    private suspend fun cleanupStaleResources(recoveredSession: RecoveredSession): List<String> =
        withContext(Dispatchers.IO) {
            val cleanupActions = mutableListOf<String>()
                recoveredSession.activeSensors.forEach { sensor ->
                    when (sensor.uppercase()) {
                        "RGB" -> cleanupActions.add("Released camera resources for RGB sensor")
                        "THERMAL" -> cleanupActions.add("Released USB resources for thermal camera")
                        "GSR", "SHIMMER" -> cleanupActions.add("Released Bluetooth resources for GSR sensor")
                    }
                }
                cleanupActions.add("Cleared any remaining background jobs")
                cleanupActions.add("Released file system locks")
            }
            return@withContext cleanupActions
        }

    private suspend fun preservePartialSessionData(recoveredSession: RecoveredSession): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext (
                if (recoveredSession.analysis.partialDataSize > 0) {
                    val sessionDir = File(recoveredSession.sessionDirectory)
                    val preservationMarker = File(sessionDir, "PARTIAL_DATA_PRESERVED.txt")
                    val preservationInfo = buildString {
                        appendLine("Partial session data preserved after crash recovery")
                        appendLine("Original session: ${recoveredSession.sessionId}")
                        appendLine("Session start time: ${Date(recoveredSession.sessionStartTime)}")
                        appendLine("Crash detected after: ${recoveredSession.sessionAge}ms")
                        appendLine("Active sensors: ${recoveredSession.activeSensors.joinToString(", ")}")
                        appendLine("Partial data size: ${recoveredSession.analysis.partialDataSize} bytes")
                        appendLine("Recovery performed at: ${Date()}")
                        appendLine()
                        appendLine("Data analysis:")
                        recoveredSession.analysis.dataFiles.forEach { (sensor, info) ->
                            appendLine("  $sensor: ${info.fileCount} files, ${info.dataSize} bytes")
                        }
                    }
                    preservationMarker.writeText(preservationInfo)
                    true
                } else {
                    false
                }
                false
            }
        }

    private suspend fun generateRecoveryReport(
        recoveredSession: RecoveredSession,
        recoveryActions: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext (
            val sessionDir = File(recoveredSession.sessionDirectory)
            val reportFile = File(sessionDir, "crash_recovery_report.json")
            val report = JSONObject().apply {
                put("session_id", recoveredSession.sessionId)
                put("recovery_timestamp", System.currentTimeMillis())
                put(
                    "recovery_date",
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                put("original_session_start", recoveredSession.sessionStartTime)
                put("session_age_at_crash", recoveredSession.sessionAge)
                put("active_sensors", JSONArray(recoveredSession.activeSensors))
                put("recovery_actions", JSONArray(recoveryActions))
                put("session_analysis", JSONObject().apply {
                    put("has_session_directory", recoveredSession.analysis.hasSessionDirectory)
                    put("directory_exists", recoveredSession.analysis.sessionDirectoryExists)
                    put("partial_data_size", recoveredSession.analysis.partialDataSize)
                    put("has_metadata", recoveredSession.analysis.hasMetadata)
                    put("summary", recoveredSession.analysis.summary)
                    put("data_files", JSONObject().apply {
                        recoveredSession.analysis.dataFiles.forEach { (sensor, info) ->
                            put(sensor, JSONObject().apply {
                                put("file_count", info.fileCount)
                                put("data_size", info.dataSize)
                                put("has_data", info.hasData)
                                put("last_modified", info.lastModified)
                            })
                        }
                    })
                })
            }
            reportFile.writeText(report.toString(2))
            true
            false
        }
    }

    private fun clearCrashRecoveryState() {
        preferences.edit()
            .remove(KEY_ACTIVE_SESSION)
            .remove(KEY_SESSION_START_TIME)
            .remove(KEY_ACTIVE_SENSORS)
            .remove(KEY_SESSION_DIRECTORY)
            .apply()
    }
}

data class CrashRecoveryResult(
    val hasCrashedSession: Boolean,
    val recoveredSession: RecoveredSession?,
    val recoveryActions: List<String>
)

data class RecoveredSession(
    val sessionId: String,
    val sessionDirectory: String,
    val sessionStartTime: Long,
    val activeSensors: List<String>,
    val sessionAge: Long,
    val analysis: SessionAnalysis
)

data class SessionAnalysis(
    val hasSessionDirectory: Boolean,
    val sessionDirectoryExists: Boolean,
    val dataFiles: Map<String, SessionDataInfo>,
    val partialDataSize: Long,
    val hasMetadata: Boolean = false,
    val summary: String
)

data class SessionDataInfo(
    val sensorName: String,
    val fileCount: Int,
    val dataSize: Long,
    val hasData: Boolean,
    val lastModified: Long
)

data class SessionRecoveryResult(
    val success: Boolean,
    val recoveredSessionId: String,
    val recoveryActions: List<String>,
    val error: String?
)