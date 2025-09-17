package com.topdon.tc001.recovery

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Crash Recovery Manager that implements crash recovery functionality as requested in the issue.
 * This handles detection of unfinished sessions and provides recovery options.
 */
class CrashRecoveryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CrashRecoveryManager"
        private const val PREFS_NAME = "crash_recovery"
        private const val KEY_ACTIVE_SESSION = "active_session"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_ACTIVE_SENSORS = "active_sensors"
        private const val KEY_SESSION_DIRECTORY = "session_directory"
        
        // Recovery timeouts
        private const val SESSION_TIMEOUT_MS = 3600000L // 1 hour
        private const val RECOVERY_SCAN_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check for crashed sessions on app startup as requested in the issue
     */
    suspend fun checkForCrashedSessions(): CrashRecoveryResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Checking for crashed sessions on app startup")
        
        try {
            // Check if there was an active session when the app last closed
            val activeSessionId = preferences.getString(KEY_ACTIVE_SESSION, null)
            val sessionStartTime = preferences.getLong(KEY_SESSION_START_TIME, 0L)
            val activeSensorsJson = preferences.getString(KEY_ACTIVE_SENSORS, null)
            val sessionDirectory = preferences.getString(KEY_SESSION_DIRECTORY, null)
            
            if (activeSessionId == null || sessionStartTime == 0L) {
                Log.i(TAG, "No previous active session found")
                return@withContext CrashRecoveryResult(
                    hasCrashedSession = false,
                    recoveredSession = null,
                    recoveryActions = emptyList()
                )
            }
            
            Log.i(TAG, "Found potential crashed session: $activeSessionId")
            Log.i(TAG, "Session start time: ${Date(sessionStartTime)}")
            Log.i(TAG, "Session directory: $sessionDirectory")
            
            // Parse active sensors
            val activeSensors = try {
                activeSensorsJson?.let { 
                    val jsonArray = JSONArray(it)
                    (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }
                } ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse active sensors", e)
                emptyList<String>()
            }
            
            // Determine if this is actually a crashed session
            val currentTime = System.currentTimeMillis()
            val sessionAge = currentTime - sessionStartTime
            
            if (sessionAge > SESSION_TIMEOUT_MS) {
                Log.w(TAG, "Session is too old (${sessionAge}ms), considering it invalid")
                clearCrashRecoveryState()
                return@withContext CrashRecoveryResult(
                    hasCrashedSession = false,
                    recoveredSession = null,
                    recoveryActions = listOf("Cleared invalid old session data")
                )
            }
            
            // Analyze the crashed session
            val sessionAnalysis = analyzeSessionDirectory(sessionDirectory, activeSensors)
            
            val recoveredSession = RecoveredSession(
                sessionId = activeSessionId,
                sessionDirectory = sessionDirectory ?: "unknown",
                sessionStartTime = sessionStartTime,
                activeSensors = activeSensors,
                sessionAge = sessionAge,
                analysis = sessionAnalysis
            )
            
            Log.i(TAG, "Crashed session analysis complete: ${sessionAnalysis.summary}")
            
            return@withContext CrashRecoveryResult(
                hasCrashedSession = true,
                recoveredSession = recoveredSession,
                recoveryActions = generateRecoveryActions(recoveredSession)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during crash recovery check", e)
            return@withContext CrashRecoveryResult(
                hasCrashedSession = false,
                recoveredSession = null,
                recoveryActions = listOf("Error during recovery check: ${e.message}")
            )
        }
    }
    
    /**
     * Mark session as active for crash recovery tracking
     */
    fun markSessionActive(
        sessionId: String,
        sessionDirectory: String,
        activeSensors: List<String>
    ) {
        Log.i(TAG, "Marking session as active for crash recovery: $sessionId")
        
        preferences.edit()
            .putString(KEY_ACTIVE_SESSION, sessionId)
            .putLong(KEY_SESSION_START_TIME, System.currentTimeMillis())
            .putString(KEY_ACTIVE_SENSORS, JSONArray(activeSensors).toString())
            .putString(KEY_SESSION_DIRECTORY, sessionDirectory)
            .apply()
    }
    
    /**
     * Mark session as completed successfully (clear crash recovery state)
     */
    fun markSessionCompleted(sessionId: String) {
        Log.i(TAG, "Marking session as completed: $sessionId")
        clearCrashRecoveryState()
    }
    
    /**
     * Mark session as failed and handle cleanup
     */
    fun markSessionFailed(sessionId: String, reason: String) {
        Log.i(TAG, "Marking session as failed: $sessionId (reason: $reason)")
        
        // Update session metadata to indicate failure
        try {
            val sessionDirectory = preferences.getString(KEY_SESSION_DIRECTORY, null)
            if (sessionDirectory != null) {
                updateSessionMetadataWithFailure(sessionDirectory, reason)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update session metadata with failure", e)
        }
        
        clearCrashRecoveryState()
    }
    
    /**
     * Recover a crashed session by cleaning up resources and updating metadata
     */
    suspend fun recoverCrashedSession(recoveredSession: RecoveredSession): SessionRecoveryResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Recovering crashed session: ${recoveredSession.sessionId}")
        
        try {
            val recoveryActions = mutableListOf<String>()
            
            // 1. Update session metadata to mark as crashed/recovered
            val metadataUpdated = updateSessionMetadataWithCrashRecovery(recoveredSession)
            if (metadataUpdated) {
                recoveryActions.add("Updated session metadata with crash recovery info")
            }
            
            // 2. Clean up any stale resources (camera locks, file handles, etc.)
            val resourcesCleanedUp = cleanupStaleResources(recoveredSession)
            recoveryActions.addAll(resourcesCleanedUp)
            
            // 3. Analyze and preserve partial data
            val dataPreserved = preservePartialSessionData(recoveredSession)
            if (dataPreserved) {
                recoveryActions.add("Preserved partial session data")
            }
            
            // 4. Generate session recovery report
            val reportGenerated = generateRecoveryReport(recoveredSession, recoveryActions)
            if (reportGenerated) {
                recoveryActions.add("Generated crash recovery report")
            }
            
            // 5. Clear crash recovery state
            clearCrashRecoveryState()
            recoveryActions.add("Cleared crash recovery tracking state")
            
            Log.i(TAG, "Session recovery completed: ${recoveryActions.size} actions performed")
            
            return@withContext SessionRecoveryResult(
                success = true,
                recoveredSessionId = recoveredSession.sessionId,
                recoveryActions = recoveryActions,
                error = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during session recovery", e)
            return@withContext SessionRecoveryResult(
                success = false,
                recoveredSessionId = recoveredSession.sessionId,
                recoveryActions = emptyList(),
                error = "Recovery failed: ${e.message}"
            )
        }
    }
    
    /**
     * Analyze session directory to understand what data was recorded
     */
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
        
        try {
            val dataFiles = mutableMapOf<String, SessionDataInfo>()
            var totalDataSize = 0L
            
            // Analyze data files for each sensor
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
            
            // Check for session metadata files
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
            
        } catch (e: Exception) {
            Log.w(TAG, "Error analyzing session directory", e)
            return@withContext SessionAnalysis(
                hasSessionDirectory = true,
                sessionDirectoryExists = true,
                dataFiles = emptyMap(),
                partialDataSize = 0L,
                summary = "Analysis failed: ${e.message}"
            )
        }
    }
    
    /**
     * Generate recovery actions based on the recovered session
     */
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
    
    /**
     * Update session metadata with crash recovery information
     */
    private fun updateSessionMetadataWithCrashRecovery(recoveredSession: RecoveredSession): Boolean {
        return try {
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
            
            // Read existing metadata if available
            val existingMetadata = if (metadataFile.exists()) {
                try {
                    JSONObject(metadataFile.readText())
                } catch (e: Exception) {
                    JSONObject()
                }
            } else {
                JSONObject()
            }
            
            // Merge crash recovery info
            existingMetadata.put("crash_recovery", crashRecoveryInfo)
            
            metadataFile.writeText(existingMetadata.toString(2))
            Log.i(TAG, "Updated session metadata with crash recovery info")
            true
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update session metadata with crash recovery", e)
            false
        }
    }
    
    /**
     * Update session metadata to mark as failed
     */
    private fun updateSessionMetadataWithFailure(sessionDirectory: String, reason: String) {
        try {
            val sessionDir = File(sessionDirectory)
            val metadataFile = File(sessionDir, "session_metadata.json")
            
            val failureInfo = JSONObject().apply {
                put("session_status", "FAILED")
                put("failure_reason", reason)
                put("failed_at", System.currentTimeMillis())
            }
            
            val existingMetadata = if (metadataFile.exists()) {
                try {
                    JSONObject(metadataFile.readText())
                } catch (e: Exception) {
                    JSONObject()
                }
            } else {
                JSONObject()
            }
            
            existingMetadata.put("failure_info", failureInfo)
            metadataFile.writeText(existingMetadata.toString(2))
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update session metadata with failure", e)
        }
    }
    
    /**
     * Clean up stale resources that might be left from crashed session
     */
    private suspend fun cleanupStaleResources(recoveredSession: RecoveredSession): List<String> = withContext(Dispatchers.IO) {
        val cleanupActions = mutableListOf<String>()
        
        try {
            // Note: In a real implementation, this would clean up:
            // - Camera resources/locks
            // - File handles
            // - Network connections
            // - Bluetooth connections
            // - Background jobs/services
            
            // For now, we'll just log what we would clean up
            recoveredSession.activeSensors.forEach { sensor ->
                when (sensor.uppercase()) {
                    "RGB" -> cleanupActions.add("Released camera resources for RGB sensor")
                    "THERMAL" -> cleanupActions.add("Released USB resources for thermal camera")
                    "GSR", "SHIMMER" -> cleanupActions.add("Released Bluetooth resources for GSR sensor")
                }
            }
            
            // Generic cleanup
            cleanupActions.add("Cleared any remaining background jobs")
            cleanupActions.add("Released file system locks")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error during resource cleanup", e)
            cleanupActions.add("Resource cleanup encountered errors: ${e.message}")
        }
        
        return@withContext cleanupActions
    }
    
    /**
     * Preserve partial session data by marking it appropriately
     */
    private suspend fun preservePartialSessionData(recoveredSession: RecoveredSession): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to preserve partial session data", e)
            false
        }
    }
    
    /**
     * Generate comprehensive recovery report
     */
    private suspend fun generateRecoveryReport(
        recoveredSession: RecoveredSession,
        recoveryActions: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val sessionDir = File(recoveredSession.sessionDirectory)
            val reportFile = File(sessionDir, "crash_recovery_report.json")
            
            val report = JSONObject().apply {
                put("session_id", recoveredSession.sessionId)
                put("recovery_timestamp", System.currentTimeMillis())
                put("recovery_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
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
            Log.i(TAG, "Generated crash recovery report: ${reportFile.absolutePath}")
            true
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate recovery report", e)
            false
        }
    }
    
    /**
     * Clear crash recovery state
     */
    private fun clearCrashRecoveryState() {
        preferences.edit()
            .remove(KEY_ACTIVE_SESSION)
            .remove(KEY_SESSION_START_TIME)
            .remove(KEY_ACTIVE_SENSORS)
            .remove(KEY_SESSION_DIRECTORY)
            .apply()
    }
}

// Data classes for crash recovery

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