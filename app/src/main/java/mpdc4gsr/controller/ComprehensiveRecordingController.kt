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
import mpdc4gsr.controller.RecordingController.SessionManifest
import mpdc4gsr.controller.RecordingController.SessionEvent
import mpdc4gsr.controller.RecordingController.SensorActivityInfo
import mpdc4gsr.controller.RecordingController.SensorHealthInfo
import mpdc4gsr.controller.RecordingController.DropoutEvent
import mpdc4gsr.controller.RecordingController.ReconnectionEvent
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.utils.SessionDirectory
import mpdc4gsr.utils.SessionDirectoryManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class ComprehensiveRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val permissionManager: PermissionManager? = null
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

    // Session orchestration enums
    enum class TriggerSource {
        LOCAL_UI,
        LOCAL_NOTIFICATION,
        REMOTE_PC,
        AUTOMATIC,
        CRASH_RECOVERY
    }

    enum class SessionState {
        IDLE,
        STARTING,
        RECORDING,
        STOPPING,
        STOPPED_COMPLETED,
        STOPPED_FAILED,
        STOPPED_INCOMPLETE
    }


    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()


    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()
    private val sensorHealthStatus = ConcurrentHashMap<String, SensorHealthInfo>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()

    // Session orchestration state
    private val currentSessionState = java.util.concurrent.atomic.AtomicReference(SessionState.IDLE)
    private var lastTriggerSource: TriggerSource? = null
    private val sessionEvents = mutableListOf<SessionEvent>()

    private val _errorFlow = MutableStateFlow<RecordingError?>(null)
    val errorFlow: StateFlow<RecordingError?> = _errorFlow.asStateFlow()

    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private var sessionMetadata: SessionMetadata? = null
    private var currentSessionId: String? = null
    private var currentSessionDirectory: SessionDirectory? = null
    private val sessionStartTime = AtomicLong(0)

    private val _recordingStateFlow = MutableStateFlow(RecordingState.IDLE)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingStateFlow.asStateFlow()

    private val _sensorStatusFlow = MutableStateFlow<List<SensorStatusInfo>>(emptyList())
    val sensorStatusFlow: StateFlow<List<SensorStatusInfo>> = _sensorStatusFlow.asStateFlow()

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
                        Log.i(
                            TAG,
                            "Successfully recovered crashed session with ${recoveryResult.recoveryActions.size} actions"
                        )
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
        estimatedDurationMinutes: Int = 30,
        triggerSource: TriggerSource = TriggerSource.LOCAL_UI
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Enforce single-session operation
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress, ignoring ${triggerSource.name} trigger")
                    return@withContext true
                }

                // Transition to STARTING state
                val transitionSuccess = transitionSessionState(SessionState.IDLE, SessionState.STARTING)
                if (!transitionSuccess) {
                    Log.w(TAG, "Failed to transition to STARTING state - invalid current state")
                    return@withContext false
                }

                lastTriggerSource = triggerSource
                addSessionEvent("SESSION_START_REQUESTED", triggerSource = triggerSource)

                Log.i(TAG, "Starting comprehensive recording with validation (trigger: ${triggerSource.name})")
                _recordingStateFlow.value = RecordingState.STARTING

                // Phase 1: Validate recording prerequisites
                val validationResult = validateRecordingPrerequisites(enabledSensors, estimatedDurationMinutes)
                if (!validationResult.isValid) {
                    Log.e(TAG, "Prerequisites validation failed: ${validationResult.failureReason}")
                    transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                    _recordingStateFlow.value = RecordingState.ERROR
                    addSessionEvent(
                        "VALIDATION_FAILED",
                        triggerSource = triggerSource,
                        success = false,
                        errorMessage = validationResult.failureReason
                    )
                    return@withContext false
                }

                // Phase 2: Request required permissions before starting
                if (!requestRequiredPermissions(enabledSensors)) {
                    Log.e(TAG, "Failed to obtain required permissions")
                    transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                    _recordingStateFlow.value = RecordingState.ERROR
                    addSessionEvent(
                        "PERMISSION_FAILED",
                        triggerSource = triggerSource,
                        success = false,
                        errorMessage = "Required permissions not granted"
                    )
                    return@withContext false
                }


                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                currentSessionDirectory = sessionDir

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

                    // Transition session state to RECORDING on successful start
                    transitionSessionState(SessionState.STARTING, SessionState.RECORDING)

                    // Start monitoring services
                    startHealthMonitoring()
                    startStatisticsUpdates()

                    Log.i(TAG, "🚀 Recording started successfully with $sensorsStarted/${enabledSensors.size} sensors")

                    // Log detailed status with fault tolerance info
                    if (failedSensors.isNotEmpty()) {
                        Log.w(
                            TAG,
                            "⚠️ Partial recording: Failed sensors [${failedSensors.joinToString(", ")}] - continuing with active sensors"
                        )
                    }

                    Log.i(
                        TAG,
                        "📊 Sensor status: ${sensorResults.entries.joinToString { "${it.key}=${if (it.value) "✅" else "❌"}" }}"
                    )

                    return@withContext true
                } else {
                    Log.e(TAG, "❌ No sensors started successfully - aborting recording")
                    cleanupFailedRecording()
                    transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Critical error starting recording", e)
                cleanupFailedRecording()
                transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
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
            Log.i(TAG, "Requesting permissions for sensors: $enabledSensors")
            // For now, assume permissions are granted since we're in a service
            // In a real implementation, you'd check permissions here
            true
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
            val sessionDirectory = currentSessionDirectory?.rootDir?.absolutePath ?: ""
            crashRecoveryManager.markSessionActive(sessionId, sessionDirectory, enabledSensors)

            Log.d(TAG, "Created crash recovery markers for session: $sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create crash recovery markers", e)
        }
    }


    suspend fun stopRecording(triggerSource: TriggerSource = TriggerSource.LOCAL_UI): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    Log.w(TAG, "No recording in progress (trigger: ${triggerSource.name})")
                    return@withContext true
                }

                // Transition to STOPPING state
                val transitionSuccess = transitionSessionState(SessionState.RECORDING, SessionState.STOPPING)
                if (!transitionSuccess) {
                    Log.w(
                        TAG,
                        "Failed to transition to STOPPING state - current state: ${currentSessionState.get().name}"
                    )
                }

                addSessionEvent("SESSION_STOP_REQUESTED", triggerSource = triggerSource)
                Log.i(TAG, "Stopping comprehensive recording with graceful teardown (trigger: ${triggerSource.name})")
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
                currentSessionDirectory = null
                _recordingStateFlow.value = RecordingState.IDLE

                Log.i(TAG, "🏁 Recording stopped successfully (duration: ${sessionDuration}ms)")
                Log.i(
                    TAG,
                    "📊 Stop results: ${stopResults.entries.joinToString { "${it.key}=${if (it.value) "✅" else "❌"}" }}"
                )

                // Determine final session state based on stop results
                val finalSessionState = when {
                    stopResults.isEmpty() -> SessionState.STOPPED_COMPLETED
                    stopResults.values.all { it } -> SessionState.STOPPED_COMPLETED
                    stopResults.values.any { it } -> SessionState.STOPPED_INCOMPLETE
                    else -> SessionState.STOPPED_FAILED
                }

                // Transition to final state
                transitionSessionState(SessionState.STOPPING, finalSessionState)

                if (sensorErrors.isNotEmpty()) {
                    Log.w(TAG, "⚠️ Some sensors had stop errors but session was finalized successfully")
                }

                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Critical error during recording stop", e)
                // Even on failure, try to clean up gracefully
                transitionSessionState(currentSessionState.get(), SessionState.STOPPED_FAILED)
                cleanupFailedRecording()
                return@withContext false
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
                                Log.w(
                                    TAG,
                                    "Sensor $sensorName has failed ${healthInfo.consecutiveFailures} times - attempting reconnection"
                                )
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
                    val sessionDir = currentSessionDirectory?.rootDir
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
        val statusList = sensorHealthStatus.map { (name, health) ->
            SensorStatusInfo(
                name = name,
                isRecording = activeRecorders[name] ?: false,
                samplesRecorded = 0L, // Could be updated with real data
                storageUsedMB = 0.0, // Could be updated with real data
                isHealthy = health.isHealthy
            )
        }
        _sensorStatusFlow.value = statusList
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

    /**
     * Get the current session directory path
     * @return Current session directory path or null if no session is active
     */
    fun getCurrentSessionDirectory(): String? {
        return try {
            currentSessionDirectory?.rootDir?.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Error getting current session directory", e)
            null
        }
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
                currentSessionDirectory?.rootDir?.absolutePath ?: ""
            )
            Log.i(TAG, "Started foreground recording service")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start foreground service - continuing without notification", e)
        }
    }

    // Initialize sensors and return success status
    suspend fun initializeSensors(): Boolean {
        return try {
            Log.i(TAG, "Initializing sensors")
            // Basic sensor initialization logic
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing sensors", e)
            false
        }
    }

    // Get list of available sensors
    fun getAvailableSensors(): List<SensorHealthSummary> {
        return sensorRecorders.keys.map { sensorName ->
            SensorHealthSummary(
                sensorId = sensorName,
                name = sensorName,
                isHealthy = sensorHealthStatus[sensorName]?.isHealthy ?: false
            )
        }
    }

    // Start a recording session
    suspend fun startSession(sessionDirectory: String): Boolean {
        return startRecording(sessionDirectory)
    }

    // Stop the current recording session
    suspend fun stopSession(): Boolean {
        return stopRecording()
    }

    // Get count of active sensors
    fun getActiveSensorCount(): Int {
        return activeRecorders.count { it.value }
    }

    // Add sync marker to recording
    suspend fun addSyncMarker(markerType: String, timestampNs: Long) {
        try {
            if (!isRecording) {
                Log.w(TAG, "Cannot add sync marker: not currently recording")
                return
            }

            Log.i(TAG, "Adding sync marker: $markerType at $timestampNs")

            // Add marker to each active recorder
            sensorRecorders.values.forEach { recorder ->
                try {
                    // All SensorRecorder implementations should have addSyncMarker method
                    // but catch any potential runtime issues gracefully
                    recorder.addSyncMarker(markerType, timestampNs)
                    Log.d(TAG, "Successfully added sync marker to ${recorder.javaClass.simpleName}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add sync marker to ${recorder.javaClass.simpleName}: ${e.message}")
                    // Continue with other recorders instead of failing entirely
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sync marker", e)
        }
    }

    // Cleanup resources
    suspend fun cleanup() {
        try {
            Log.i(TAG, "Cleaning up ComprehensiveRecordingController")
            activeRecorders.clear()
            sensorHealthStatus.clear()
            reconnectionAttempts.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
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
                val sessionDir = currentSessionDirectory?.rootDir
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


    /**
     * Get current sensor status summary
     */
    fun getSensorStatusSummary(): String {
        return try {
            val activeCount = activeRecorders.values.count { it }
            val healthyCount = sensorHealthStatus.values.count { it.isHealthy }
            val totalCount = sensorRecorders.size

            buildString {
                append("Active: $activeCount/$totalCount, ")
                append("Healthy: $healthyCount/$totalCount")
                if (isRecording) {
                    append(" [RECORDING]")
                } else {
                    append(" [IDLE]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sensor status summary", e)
            "Error getting status"
        }
    }

    // Session orchestration helper methods
    private fun transitionSessionState(from: SessionState, to: SessionState): Boolean {
        return currentSessionState.compareAndSet(from, to).also { success ->
            if (success) {
                Log.d(TAG, "Session state transition: ${from.name} -> ${to.name}")
                addSessionEvent(
                    "STATE_TRANSITION", metadata = mapOf(
                        "from" to from.name,
                        "to" to to.name
                    )
                )
            } else {
                Log.w(
                    TAG,
                    "Failed session state transition: ${from.name} -> ${to.name} (current: ${currentSessionState.get().name})"
                )
            }
        }
    }

    private fun addSessionEvent(
        eventType: String,
        sensorId: String? = null,
        triggerSource: TriggerSource? = null,
        success: Boolean = true,
        errorMessage: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        val event = SessionEvent(
            eventType = eventType,
            timestampMs = System.currentTimeMillis(),
            sensorId = sensorId,
            triggerSource = triggerSource,
            metadata = metadata,
            success = success,
            errorMessage = errorMessage
        )
        sessionEvents.add(event)
        Log.d(TAG, "Session event: $eventType${sensorId?.let { " ($it)" } ?: ""}")
    }

    // Session manifest generation
    fun generateSessionManifest(): SessionManifest {
        val sessionDirectory = currentSessionDirectory?.rootDir?.name ?: currentSessionId ?: "unknown"
        val startTime = sessionStartTime.get()
        val stopTime = if (currentSessionState.get() in listOf(
                SessionState.STOPPED_COMPLETED,
                SessionState.STOPPED_FAILED,
                SessionState.STOPPED_INCOMPLETE
            )
        ) {
            System.currentTimeMillis()
        } else null

        val duration = stopTime?.let { it - startTime }

        val sensorActivitySummary = sensorRecorders.keys.associateWith { sensorName ->
            val wasActive = activeRecorders[sensorName] == true
            val healthInfo = sensorHealthStatus[sensorName]

            RecordingController.SensorActivityInfo(
                sensorName = sensorName,
                wasActive = wasActive,
                startedSuccessfully = wasActive,
                finalStatus = if (wasActive) "COMPLETED" else "INACTIVE",
                errorMessages = healthInfo?.lastError?.let { listOf(it) } ?: emptyList<String>()
            )
        }

        val errors = sessionEvents.filter { !it.success }.map {
            "${it.eventType}: ${it.errorMessage ?: "Unknown error"}"
        }

        val warnings = sessionEvents.filter {
            it.eventType.contains("WARNING") || it.eventType.contains("CRITICAL")
        }.map { "${it.eventType}: ${it.metadata}" }

        return RecordingController.SessionManifest(
            sessionId = sessionDirectory,
            startTime = startTime,
            stopTime = stopTime,
            duration = duration,
            triggerSource = RecordingController.TriggerSource.LOCAL_UI, // Convert or use appropriate mapping
            sensorActivitySummary = sensorActivitySummary,
            events = sessionEvents.toList(),
            errors = errors,
            warnings = warnings,
            fileReferences = emptyMap(), // Will be populated by individual recorders
            sessionState = RecordingController.SessionState.STOPPED_COMPLETED // Convert or use appropriate mapping
        )
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
    IDLE, STARTING, RECORDING, STOPPING, STOPPED, ERROR
}

data class SensorHealthSummary(
    val sensorId: String,
    val name: String,
    val isHealthy: Boolean
)

data class SensorStatusInfo(
    val name: String,
    val isRecording: Boolean,
    val samplesRecorded: Long = 0,
    val storageUsedMB: Double = 0.0,
    val isHealthy: Boolean = true
)

data class RecordingError(
    val message: String,
    val isRecoverable: Boolean = true
)