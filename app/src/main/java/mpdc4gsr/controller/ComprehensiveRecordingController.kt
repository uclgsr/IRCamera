package mpdc4gsr.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.util.SessionDirectoryManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class ComprehensiveRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val permissionManager: PermissionManager
) {
    companion object {
        private const val TAG = "ComprehensiveRecordingController"
        private const val FALLBACK_AVAILABLE_SPACE_GB = 10.0
        private const val RGB_STORAGE_MB_PER_MIN = 50.0
        private const val THERMAL_STORAGE_MB_PER_MIN = 5.0
        private const val SHIMMER_STORAGE_MB_PER_MIN = 1.0
        private const val MIN_STORAGE_SPACE_GB = 1.0
        private const val SESSION_TIMEOUT_MS = 30000L
    }


    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()


    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()
    private val sensorHealthStatus = ConcurrentHashMap<String, SensorHealthInfo>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()


    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private var sessionMetadata: SessionMetadata? = null
    private var currentSessionId: String? = null
    private val sessionStartTime = AtomicLong(0)


    private val _recordingStateFlow = MutableStateFlow(RecordingState.IDLE)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingStateFlow.asStateFlow()

    private val _sensorStatusFlow = MutableStateFlow(emptyMap<String, SensorStatus>())
    val sensorStatusFlow: StateFlow<Map<String, SensorStatus>> = _sensorStatusFlow.asStateFlow()

    private val _recordingStatsFlow = MutableStateFlow(RecordingStats.empty())
    val recordingStatsFlow: StateFlow<RecordingStats> = _recordingStatsFlow.asStateFlow()

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Crash recovery integration
    private var crashRecoveryMarker: File? = null
    private val crashRecoveryManager = mpdc4gsr.core.CrashRecoveryManager(context)


    fun addSensorRecorder(name: String, recorder: SensorRecorder) {
        sensorRecorders[name] = recorder
        sensorHealthStatus[name] = SensorHealthInfo(
            name = name,
            isHealthy = true,
            lastHealthCheck = System.currentTimeMillis(),
            consecutiveFailures = 0
        )
        Log.d(TAG, "Added sensor recorder with health monitoring: $name")
        updateSensorStatusFlow()
    }

    /**
     * Check for crashed sessions on app startup and handle recovery
     * Should be called when the controller is initialized
     */
    suspend fun checkForCrashedSessions(): Boolean {
        return try {
            val crashRecoveryResult = crashRecoveryManager.checkForCrashedSessions()
            if (crashRecoveryResult.hasCrashedSession) {
                Log.w(TAG, "Detected crashed session: ${crashRecoveryResult.recoveredSession?.sessionId}")
                crashRecoveryResult.recoveredSession?.let { recoveredSession ->
                    val recoveryResult = crashRecoveryManager.recoverCrashedSession(recoveredSession)
                    if (recoveryResult.success) {
                        Log.i(TAG, "Successfully recovered crashed session with ${recoveryResult.recoveryActions.size} actions")
                    } else {
                        Log.e(TAG, "Failed to recover crashed session: ${recoveryResult.error}")
                    }
                }
                true
            } else {
                Log.i(TAG, "No crashed sessions detected")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for crashed sessions", e)
            false
        }
    }


    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer"),
        estimatedDurationMinutes: Int = 30
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }

                Log.i(TAG, "Starting comprehensive recording with validation")
                _recordingStateFlow.value = RecordingState.STARTING

                // Phase 1: Validate recording prerequisites
                val validationResult = validateRecordingPrerequisites(enabledSensors, estimatedDurationMinutes)
                if (!validationResult.isValid) {
                    Log.e(TAG, "Prerequisites validation failed: ${validationResult.failureReason}")
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }

                // Phase 2: Request required permissions before starting
                if (!requestRequiredPermissions(enabledSensors)) {
                    Log.e(TAG, "Failed to obtain required permissions")
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }


                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)

                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId).copy(
                    experimentalConditions = mapOf(
                        "estimatedDurationMinutes" to estimatedDurationMinutes,
                        "enabledSensors" to enabledSensors
                    )
                )

                currentSessionId = finalSessionId
                sessionStartTime.set(System.currentTimeMillis())


                createCrashRecoveryMarker(finalSessionId, enabledSensors)

                // Phase 3: Start foreground service immediately after session setup
                startForegroundService()

                // Phase 4: Start sensors with individual fault isolation
                var sensorsStarted = 0
                val sensorResults = mutableMapOf<String, Boolean>()
                val failedSensors = mutableListOf<String>()

                for (sensorName in enabledSensors) {
                    val sensor = sensorRecorders[sensorName]
                    if (sensor != null) {
                        try {
                            Log.i(TAG, "Starting sensor: $sensorName")
                            val sensorDir = File(sessionDir.rootDir, sensorName.lowercase())
                            sensorDir.mkdirs()

                            sessionMetadata?.let { meta ->
                                val success = sensor.startRecording(sensorDir.absolutePath, meta)
                                sensorResults[sensorName] = success

                                if (success) {
                                    activeRecorders[sensorName] = true
                                    sensorsStarted++
                                    updateSensorHealth(sensorName, true)
                                    Log.i(TAG, "✅ Started sensor: $sensorName")
                                } else {
                                    updateSensorHealth(sensorName, false)
                                    failedSensors.add(sensorName)
                                    Log.w(TAG, "❌ Failed to start sensor: $sensorName - continuing with others")
                                }
                            }
                        } catch (e: Exception) {
                            // Isolate sensor failures - don't let one sensor crash the entire session
                            Log.w(TAG, "Exception starting sensor $sensorName (isolated): ${e.message}", e)
                            updateSensorHealth(sensorName, false)
                            sensorResults[sensorName] = false
                            failedSensors.add(sensorName)
                            // Continue with other sensors instead of failing the entire session
                        }
                    } else {
                        Log.w(TAG, "⚠️ Sensor recorder not available: $sensorName - continuing with others")
                        sensorResults[sensorName] = false
                        failedSensors.add(sensorName)
                    }
                }

                // Phase 5: Evaluate session success with fault tolerance
                if (sensorsStarted > 0) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING

                    // Start monitoring services
                    startHealthMonitoring()
                    startStatisticsUpdates()

                    Log.i(TAG, "🚀 Recording started successfully with $sensorsStarted/${enabledSensors.size} sensors")
                    
                    // Log detailed status with fault tolerance info
                    if (failedSensors.isNotEmpty()) {
                        Log.w(TAG, "⚠️ Partial recording: Failed sensors [${failedSensors.joinToString(", ")}] - continuing with active sensors")
                    }
                    
                    Log.i(
                        TAG,
                        "📊 Sensor status: ${sensorResults.entries.joinToString { "${it.key}=${if (it.value) "✅" else "❌"}" }}"
                    )

                    return@withContext true
                } else {
                    Log.e(TAG, "❌ No sensors started successfully - aborting recording")
                    cleanupFailedRecording()
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Critical error starting recording", e)
                cleanupFailedRecording()
                _recordingStateFlow.value = RecordingState.ERROR
                return@withContext false
            }
        }
    }


    private suspend fun validateRecordingPrerequisites(
        enabledSensors: List<String>,
        estimatedDurationMinutes: Int
    ): ValidationResult {
        try {

            val availableSpaceGB = getAvailableSpaceGB()
            val estimatedSpaceGB = estimateSessionSize(enabledSensors, estimatedDurationMinutes) / 1024.0

            if (availableSpaceGB < estimatedSpaceGB + MIN_STORAGE_SPACE_GB) {
                return ValidationResult(
                    false,
                    "Insufficient storage: ${String.format("%.1f", availableSpaceGB)}GB available, " +
                            "${String.format("%.1f", estimatedSpaceGB + MIN_STORAGE_SPACE_GB)}GB required"
                )
            }


            val unavailableSensors = enabledSensors.filter { sensorRecorders[it] == null }
            if (unavailableSensors.isNotEmpty()) {
                return ValidationResult(
                    false,
                    "Sensors not available: ${unavailableSensors.joinToString()}"
                )
            }


            val unhealthySensors = enabledSensors.filter {
                sensorHealthStatus[it]?.isHealthy == false
            }
            if (unhealthySensors.isNotEmpty()) {
                Log.w(TAG, "⚠️ Sensors with health issues: ${unhealthySensors.joinToString()}")

            }

            return ValidationResult(true, "All prerequisites validated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during prerequisite validation", e)
            return ValidationResult(false, "Validation error: ${e.message}")
        }
    }


    private suspend fun requestRequiredPermissions(enabledSensors: List<String>): Boolean {
        return try {
            var allPermissionsGranted = true


            if (enabledSensors.contains("RGB")) {
                if (!permissionManager.requestCameraPermissions()) {
                    Log.w(TAG, "Camera permissions not granted")
                    allPermissionsGranted = false
                }
            }


            if (enabledSensors.contains("Shimmer")) {
                if (!permissionManager.requestBluetoothPermissions()) {
                    Log.w(TAG, "Bluetooth permissions not granted")
                    allPermissionsGranted = false
                }
            }


            if (!permissionManager.requestStoragePermissions()) {
                Log.w(TAG, "Storage permissions not granted")

            }

            allPermissionsGranted
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permissions", e)
            false
        }
    }


    private fun estimateSessionSize(enabledSensors: List<String>, durationMinutes: Int): Double {
        var estimatedMB = 0.0

        for (sensor in enabledSensors) {
            when (sensor.uppercase()) {
                "RGB" -> estimatedMB += durationMinutes * RGB_STORAGE_MB_PER_MIN
                "THERMAL" -> estimatedMB += durationMinutes * THERMAL_STORAGE_MB_PER_MIN
                "SHIMMER" -> estimatedMB += durationMinutes * SHIMMER_STORAGE_MB_PER_MIN
            }
        }

        return estimatedMB
    }


    private fun createCrashRecoveryMarker(sessionId: String, enabledSensors: List<String>) {
        try {
            // Create file marker (existing functionality)
            crashRecoveryMarker = File(context.filesDir, "crash_recovery_$sessionId.marker")
            crashRecoveryMarker?.writeText("RECORDING_ACTIVE:$sessionId:${System.currentTimeMillis()}")
            
            // Mark session active in CrashRecoveryManager with SharedPreferences
            val sessionDirectory = sessionDirectoryManager.getCurrentSessionDirectory()?.rootDir?.absolutePath ?: ""
            crashRecoveryManager.markSessionActive(sessionId, sessionDirectory, enabledSensors)
            
            Log.d(TAG, "Created crash recovery markers for session: $sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create crash recovery markers", e)
        }
    }


    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    return@withContext true
                }

                Log.i(TAG, "Stopping comprehensive recording with graceful teardown")
                _recordingStateFlow.value = RecordingState.STOPPING
                _isRecording.set(false)

                // Phase 1: Stop each sensor in individual try-catch blocks for isolation
                val stopResults = mutableMapOf<String, Boolean>()
                val sensorErrors = mutableListOf<String>()
                
                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                        try {
                            sensorRecorders[sensorName]?.stopRecording()
                            stopResults[sensorName] = true
                            Log.i(TAG, "✅ Stopped sensor: $sensorName")
                        } catch (e: Exception) {
                            // Isolate sensor stop failures - log and continue with others
                            Log.w(TAG, "❌ Error stopping sensor $sensorName (isolated): ${e.message}", e)
                            stopResults[sensorName] = false
                            sensorErrors.add("$sensorName: ${e.message}")
                        }
                    }
                }

                // Phase 2: Finalize session with complete metadata
                val sessionEndTime = System.currentTimeMillis()
                val sessionDuration = sessionEndTime - sessionStartTime.get()
                
                finalizeSession(stopResults, sensorErrors, sessionEndTime, sessionDuration)

                // Phase 3: Cleanup resources
                activeRecorders.clear()
                reconnectionAttempts.clear()

                // Phase 4: Remove crash recovery marker and clear SharedPreferences state
                crashRecoveryMarker?.let {
                    if (it.exists()) {
                        it.delete()
                    }
                }
                currentSessionId?.let { sessionId ->
                    crashRecoveryManager.markSessionCompleted(sessionId)
                }
                Log.d(TAG, "Removed crash recovery markers and cleared persistent state")

                // Phase 5: Stop foreground service and remove notification
                stopForegroundService()

                // Phase 6: Update state
                sessionMetadata = null
                currentSessionId = null
                _recordingStateFlow.value = RecordingState.IDLE

                Log.i(TAG, "🏁 Recording stopped successfully (duration: ${sessionDuration}ms)")
                Log.i(
                    TAG,
                    "📊 Stop results: ${stopResults.entries.joinToString { "${it.key}=${if (it.value) "✅" else "❌"}" }}"
                )
                
                if (sensorErrors.isNotEmpty()) {
                    Log.w(TAG, "⚠️ Some sensors had stop errors but session was finalized successfully")
                }

                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Critical error during recording stop", e)
                // Even on failure, try to clean up gracefully
                cleanupFailedRecording()
                return@withContext false
            }
        }
    }
        }
    }


    private fun startHealthMonitoring() {
        recordingScope.launch {
            while (_isRecording.get()) {
                try {
                    for ((sensorName, isActive) in activeRecorders) {
                        if (isActive) {
                            checkSensorHealth(sensorName)
                            
                            // Check if sensor needs reconnection
                            val healthInfo = sensorHealthStatus[sensorName]
                            if (healthInfo != null && !healthInfo.isHealthy && healthInfo.consecutiveFailures >= 3) {
                                Log.w(TAG, "Sensor $sensorName has failed ${healthInfo.consecutiveFailures} times - attempting reconnection")
                                attemptSensorReconnection(sensorName)
                            }
                        }
                    }
                    updateSensorStatusFlow()
                    delay(5000)
                } catch (e: Exception) {
                    Log.w(TAG, "Error during health monitoring", e)
                    delay(10000)
                }
            }
        }
    }

    private suspend fun attemptSensorReconnection(sensorName: String) {
        val currentAttempts = reconnectionAttempts[sensorName] ?: 0
        val maxAttempts = 3
        
        if (currentAttempts >= maxAttempts) {
            Log.w(TAG, "Max reconnection attempts reached for $sensorName - marking as inactive but continuing session")
            activeRecorders[sensorName] = false
            return
        }
        
        try {
            Log.i(TAG, "Attempting to reconnect sensor $sensorName (attempt ${currentAttempts + 1}/$maxAttempts)")
            reconnectionAttempts[sensorName] = currentAttempts + 1
            
            val sensor = sensorRecorders[sensorName]
            if (sensor != null) {
                // Stop and restart the sensor
                try {
                    sensor.stopRecording()
                    delay(1000)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping sensor during reconnection", e)
                }
                
                // Try to restart
                sessionMetadata?.let { meta ->
                    val sessionDir = sessionDirectoryManager.getCurrentSessionDirectory()?.rootDir
                    if (sessionDir != null) {
                        val sensorDir = File(sessionDir, sensorName.lowercase())
                        val success = sensor.startRecording(sensorDir.absolutePath, meta)
                        
                        if (success) {
                            Log.i(TAG, "Successfully reconnected sensor $sensorName")
                            reconnectionAttempts[sensorName] = 0 // Reset attempts on success
                            updateSensorHealth(sensorName, true)
                        } else {
                            Log.w(TAG, "Failed to reconnect sensor $sensorName")
                            updateSensorHealth(sensorName, false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sensor reconnection for $sensorName", e)
            updateSensorHealth(sensorName, false)
        }
    }


    private fun startStatisticsUpdates() {
        recordingScope.launch {
            while (_isRecording.get()) {
                try {
                    updateRecordingStats()
                    delay(2000)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating statistics", e)
                    delay(5000)
                }
            }
        }
    }

    private fun updateSensorHealth(sensorName: String, isHealthy: Boolean) {
        val currentHealth = sensorHealthStatus[sensorName] ?: return
        val updatedHealth = currentHealth.copy(
            isHealthy = isHealthy,
            lastHealthCheck = System.currentTimeMillis(),
            consecutiveFailures = if (isHealthy) 0 else currentHealth.consecutiveFailures + 1
        )
        sensorHealthStatus[sensorName] = updatedHealth
    }

    private fun updateSensorStatusFlow() {
        val statusMap = sensorHealthStatus.mapValues { (name, health) ->
            SensorStatus(
                name = name,
                isActive = activeRecorders[name] ?: false,
                isHealthy = health.isHealthy,
                lastUpdate = health.lastHealthCheck
            )
        }
        _sensorStatusFlow.value = statusMap
    }

    private fun updateRecordingStats() {
        val currentTime = System.currentTimeMillis()
        val duration = if (sessionStartTime.get() > 0) currentTime - sessionStartTime.get() else 0

        val stats = RecordingStats(
            isRecording = _isRecording.get(),
            sessionId = currentSessionId,
            durationMs = duration,
            activeSensors = activeRecorders.count { it.value },
            totalSensors = sensorRecorders.size,
            healthySensors = sensorHealthStatus.count { it.value.isHealthy },
            recordingState = _recordingStateFlow.value
        )

        _recordingStatsFlow.value = stats
    }

    private fun checkSensorHealth(sensorName: String) {
        // Check sensor-specific health metrics and recording status
        val sensor = sensorRecorders[sensorName]
        val isHealthy = sensor?.isRecording == true
        
        // Additional health checks could include:
        // - Data throughput monitoring
        // - Connection stability for BLE sensors
        // - File system write success for recording sensors
        // - Hardware-specific status queries
        
        updateSensorHealth(sensorName, isHealthy)
    }

    private fun cleanupFailedRecording() {
        activeRecorders.clear()
        sessionMetadata = null
        
        // Clean up crash recovery state on failure
        currentSessionId?.let { sessionId ->
            crashRecoveryManager.markSessionFailed(sessionId, "Recording startup failed")
        }
        
        crashRecoveryMarker?.delete()
        currentSessionId = null
    }

    private fun getAvailableSpaceGB(): Double {
        return try {
            val sessionDir = File(context.filesDir, "sessions")
            sessionDir.freeSpace / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {
            FALLBACK_AVAILABLE_SPACE_GB
        }
    }

    private fun startForegroundService() {
        try {
            // Start RecordingService with foreground notification
            mpdc4gsr.core.RecordingService.startRecording(
                context,
                sessionDirectoryManager.getCurrentSessionDirectory()?.rootDir?.absolutePath ?: ""
            )
            Log.i(TAG, "Started foreground recording service")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start foreground service - continuing without notification", e)
        }
    }

    private fun stopForegroundService() {
        try {
            // Stop RecordingService and remove notification
            mpdc4gsr.core.RecordingService.stopRecording(context)
            Log.i(TAG, "Stopped foreground recording service")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop foreground service gracefully", e)
        }
    }

    private suspend fun finalizeSession(
        stopResults: Map<String, Boolean>,
        sensorErrors: List<String>,
        sessionEndTime: Long,
        sessionDuration: Long
    ) {
        try {
            currentSessionId?.let { sessionId ->
                val sessionDir = sessionDirectoryManager.getCurrentSessionDirectory()?.rootDir
                if (sessionDir != null) {
                    // Create comprehensive session_info.json with all metadata
                    val sessionInfo = SessionInfoData(
                        sessionId = sessionId,
                        startTime = sessionStartTime.get(),
                        endTime = sessionEndTime,
                        durationMs = sessionDuration,
                        durationSeconds = sessionDuration / 1000.0,
                        recordingStatus = if (sensorErrors.isEmpty()) "COMPLETED" else "COMPLETED_WITH_ERRORS",
                        activeSensors = activeRecorders.keys.toList(),
                        sensorStopResults = stopResults,
                        errors = sensorErrors.takeIf { it.isNotEmpty() },
                        finalizedAt = System.currentTimeMillis()
                    )

                    // Write session_info.json using Gson serialization
                    val sessionInfoFile = File(sessionDir, "session_info.json")
                    sessionInfoFile.writeText(createSessionInfoJson(sessionInfo))
                    
                    Log.i(TAG, "Session finalized with metadata: ${sessionInfoFile.absolutePath}")
                } else {
                    Log.w(TAG, "Cannot finalize session - session directory not available")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error finalizing session metadata", e)
        }
    }

    private fun createSessionInfoJson(sessionInfo: SessionInfoData): String {
        // Simple JSON serialization without external dependencies
        return JSONObject().apply {
            put("session_id", sessionInfo.sessionId)
            put("start_time", sessionInfo.startTime)
            put("end_time", sessionInfo.endTime)
            put("duration_ms", sessionInfo.durationMs)
            put("duration_seconds", sessionInfo.durationSeconds)
            put("recording_status", sessionInfo.recordingStatus)
            put("active_sensors", JSONArray(sessionInfo.activeSensors))
            put("sensor_stop_results", JSONObject(sessionInfo.sensorStopResults as Map<String, Any>))
            sessionInfo.errors?.let { put("errors", JSONArray(it)) }
            put("finalized_at", sessionInfo.finalizedAt)
        }.toString(2)
    }
}


data class ValidationResult(val isValid: Boolean, val failureReason: String)

data class SessionInfoData(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val durationSeconds: Double,
    val recordingStatus: String,
    val activeSensors: List<String>,
    val sensorStopResults: Map<String, Boolean>,
    val errors: List<String>?,
    val finalizedAt: Long
)

data class SensorHealthInfo(
    val name: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val consecutiveFailures: Int
)

data class SensorStatus(
    val name: String,
    val isActive: Boolean,
    val isHealthy: Boolean,
    val lastUpdate: Long
)

data class RecordingStats(
    val isRecording: Boolean,
    val sessionId: String?,
    val durationMs: Long,
    val activeSensors: Int,
    val totalSensors: Int,
    val healthySensors: Int,
    val recordingState: RecordingState
) {
    companion object {
        fun empty() = RecordingStats(
            false, null, 0, 0, 0, 0, RecordingState.IDLE
        )
    }
}

enum class RecordingState {
    IDLE, STARTING, RECORDING, STOPPING, ERROR
}