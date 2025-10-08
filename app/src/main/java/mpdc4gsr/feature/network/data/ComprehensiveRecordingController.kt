package mpdc4gsr.feature.network.data

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.CrashRecoveryManager
import mpdc4gsr.feature.system.service.RecordingService
import mpdc4gsr.core.data.SensorRecorder
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.core.data.utils.SessionDirectory
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class ComprehensiveRecordingController(
    private val context: Context,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner? = null,
    private val permissionManager: mpdc4gsr.core.ui.PermissionManager? = null
) {
    companion object {        private val DEFAULT_TRIGGER_SOURCE = TriggerSource.LOCAL_UI

        // Sensor configuration constants
        private const val RGB_SENSOR_NAME = "RGB"
        private const val THERMAL_SENSOR_NAME = "Thermal"

        // Health monitoring constants
        private const val HEALTH_CHECK_INTERVAL_MS = 5000L
        private const val HEALTH_CHECK_ERROR_DELAY_MS = 10000L
        private const val STATS_UPDATE_INTERVAL_MS = 2000L
        private const val STATS_UPDATE_ERROR_DELAY_MS = 5000L

        // Reconnection settings
        private const val MAX_RECONNECTION_ATTEMPTS = 3
        private const val GSR_SENSOR_NAME = "GSR"
        private const val THERMAL_SENSOR_ID = "thermal_camera_1"
        private const val GSR_SENSOR_ID = "gsr_shimmer_1"
        private const val THERMAL_FRAME_RATE_HZ = 9.0 // TOPDON TC001 specs
        private const val THERMAL_WIDTH_PIXELS = 256
        private const val THERMAL_HEIGHT_PIXELS = 192
        private const val GSR_SAMPLING_RATE_HZ = 128
    }

    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()
    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()
    private val sensorHealthStatus = ConcurrentHashMap<String, SensorHealthInfo>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()

    // Session orchestration state
    private val currentSessionState = AtomicReference(SessionState.IDLE)
    private var lastTriggerSource: TriggerSource? = null

    // Use a thread-safe list for session events
    private val sessionEvents =
        CopyOnWriteArrayList<RecordingControllerSessionEvent>()
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
    private val _recordingStatsFlow = MutableStateFlow(
        RecordingStats(
            sessionId = "",
            duration = 0L,
            activeSensors = 0,
            totalSamples = 0L,
            avgDataRate = 0.0,
            storageUsedMB = 0.0,
            errors = 0,
            warnings = 0,
            qualityScore = 1.0
        )
    )
    val recordingStatsFlow: StateFlow<RecordingStats> = _recordingStatsFlow.asStateFlow()
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Crash recovery integration
    private var crashRecoveryMarker: File? = null
    private val crashRecoveryManager = CrashRecoveryManager(context)
    fun addSensorRecorder(name: String, recorder: SensorRecorder) {
        sensorRecorders[name] = recorder
        sensorHealthStatus[name] = SensorHealthInfo(
            sensorId = name,
            isHealthy = true,
            lastHealthCheck = System.currentTimeMillis(),
            consecutiveFailures = 0,
            lastError = null
        )        updateSensorStatusFlow()
    }

    suspend fun checkForCrashedSessions(): Boolean {
        return try {
            val crashRecoveryResult = crashRecoveryManager.checkForCrashedSessions()
            if (crashRecoveryResult.hasCrashedSession) {                crashRecoveryResult.recoveredSession?.let { recoveredSession ->
                    val recoveryResult =
                        crashRecoveryManager.recoverCrashedSession(recoveredSession)
                    if (recoveryResult.success) {                    } else {                    }
                }
                true
            } else {                false
            }
        } catch (e: Exception) {            false
        }
    }

    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf(
            RGB_SENSOR_NAME,
            THERMAL_SENSOR_NAME,
            GSR_SENSOR_NAME
        ),
        estimatedDurationMinutes: Int = 30,
        triggerSource: TriggerSource = TriggerSource.LOCAL_UI
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Enforce single-session operation
                if (_isRecording.get()) {                    return@withContext true
                }
                // Transition to STARTING state
                val transitionSuccess =
                    transitionSessionState(SessionState.IDLE, SessionState.STARTING)
                if (!transitionSuccess) {                    return@withContext false
                }
                lastTriggerSource = triggerSource
                addSessionEvent("SESSION_START_REQUESTED", triggerSource = triggerSource)
                Log.i(
                    TAG,
                    "Starting comprehensive recording with validation (trigger: ${triggerSource.name})"
                )
                _recordingStateFlow.value = RecordingState.STARTING
                // Phase 1: Validate recording prerequisites
                val validationResult =
                    validateRecordingPrerequisites(enabledSensors, estimatedDurationMinutes)
                if (!validationResult.isValid) {                    transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
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
                if (!requestRequiredPermissions(enabledSensors)) {                    transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
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
                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId)
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
                        try {                            val sensorDir = File(sessionDir.rootDir, sensorName.lowercase())
                            sensorDir.mkdirs()
                            sessionMetadata?.let { meta ->
                                val success = sensor.startRecording(sensorDir.absolutePath, meta)
                                sensorResults[sensorName] = success
                                if (success) {
                                    activeRecorders[sensorName] = true
                                    sensorsStarted++
                                    updateSensorHealth(sensorName, true)                                } else {
                                    updateSensorHealth(sensorName, false)
                                    failedSensors.add(sensorName)                                }
                                success
                            } ?: run {
                                // Handle case where sessionMetadata is null
                                val success = sensor.startRecording(sensorDir.absolutePath)
                                sensorResults[sensorName] = success
                                if (success) {
                                    activeRecorders[sensorName] = true
                                    sensorsStarted++
                                    updateSensorHealth(sensorName, true)                                } else {
                                    updateSensorHealth(sensorName, false)
                                    failedSensors.add(sensorName)                                }
                                success
                            }
                        } catch (e: Exception) {
                            // Isolate sensor failures - don't let one sensor crash the entire session
                            Log.w(
                                TAG,
                                "Exception starting sensor $sensorName (isolated): ${e.message}",
                                e
                            )
                            updateSensorHealth(sensorName, false)
                            sensorResults[sensorName] = false
                            failedSensors.add(sensorName)
                            // Continue with other sensors instead of failing the entire session
                        }
                    } else {                        sensorResults[sensorName] = false
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
                    startStatisticsUpdates()                    // Log detailed status with fault tolerance info
                    if (failedSensors.isNotEmpty()) {
                        Log.w(
                            TAG,
                            " Partial recording: Failed sensors [${failedSensors.joinToString(", ")}] - continuing with active sensors"
                        )
                    }
                    Log.i(
                        TAG,
                        " Sensor status: ${sensorResults.entries.joinToString { "${it.key}=${if (it.value) "" else ""}" }}"
                    )
                    return@withContext true
                } else {                    cleanupFailedRecording()
                    transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }
            } catch (e: Exception) {                cleanupFailedRecording()
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
            val estimatedSpaceGB =
                estimateSessionSize(enabledSensors, estimatedDurationMinutes) / 1024.0
            if (availableSpaceGB < estimatedSpaceGB + RecordingConstants.MIN_STORAGE_SPACE_GB) {
                return ValidationResult(
                    false,
                    "Insufficient storage: ${
                        String.format(
                            "%.1f",
                            availableSpaceGB
                        )
                    }GB available, " +
                            "${
                                String.format(
                                    "%.1f",
                                    estimatedSpaceGB + RecordingConstants.MIN_STORAGE_SPACE_GB
                                )
                            }GB required"
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
            if (unhealthySensors.isNotEmpty()) {            }
            return ValidationResult(true, "All prerequisites validated successfully")
        } catch (e: Exception) {            return ValidationResult(false, "Validation error: ${e.message}")
        }
    }

    private suspend fun requestRequiredPermissions(enabledSensors: List<String>): Boolean {
        return try {            // In a real implementation, you'd check and request permissions here
            true
        } catch (e: Exception) {            false
        }
    }

    private fun estimateSessionSize(enabledSensors: List<String>, durationMinutes: Int): Double {
        var estimatedMB = 0.0
        for (sensor in enabledSensors) {
            when (sensor.uppercase()) {
                "RGB" -> estimatedMB += durationMinutes * RecordingConstants.RGB_STORAGE_MB_PER_MIN
                "THERMAL" -> estimatedMB += durationMinutes * RecordingConstants.THERMAL_STORAGE_MB_PER_MIN
                "SHIMMER" -> estimatedMB += durationMinutes * RecordingConstants.SHIMMER_STORAGE_MB_PER_MIN
            }
        }
        return estimatedMB
    }

    private fun createCrashRecoveryMarker(sessionId: String, enabledSensors: List<String>) {
        try {
            crashRecoveryMarker = File(context.filesDir, "crash_recovery_$sessionId.marker")
            crashRecoveryMarker?.writeText("RECORDING_ACTIVE:$sessionId:${System.currentTimeMillis()}")
            val sessionDirectory = currentSessionDirectory?.rootDir?.absolutePath ?: ""
            crashRecoveryManager.markSessionActive(sessionId, sessionDirectory, enabledSensors)        } catch (e: Exception) {        }
    }

    suspend fun stopRecording(triggerSource: TriggerSource = TriggerSource.LOCAL_UI): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {                    return@withContext true
                }
                val transitionSuccess =
                    transitionSessionState(SessionState.RECORDING, SessionState.STOPPING)
                if (!transitionSuccess) {
                    Log.w(
                        TAG,
                        "Failed to transition to STOPPING state - current state: ${currentSessionState.get().name}"
                    )
                }
                addSessionEvent("SESSION_STOP_REQUESTED", triggerSource = triggerSource)
                Log.i(
                    TAG,
                    "Stopping comprehensive recording with graceful teardown (trigger: ${triggerSource.name})"
                )
                _recordingStateFlow.value = RecordingState.STOPPING
                _isRecording.set(false)
                val stopResults = mutableMapOf<String, Boolean>()
                val sensorErrors = mutableListOf<String>()
                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                        try {
                            sensorRecorders[sensorName]?.stopRecording()
                            stopResults[sensorName] = true                        } catch (e: Exception) {
                            Log.w(
                                TAG,
                                " Error stopping sensor $sensorName (isolated): ${e.message}",
                                e
                            )
                            stopResults[sensorName] = false
                            sensorErrors.add("$sensorName: ${e.message}")
                        }
                    }
                }
                val sessionEndTime = System.currentTimeMillis()
                val sessionDuration = sessionEndTime - sessionStartTime.get()
                finalizeSession(stopResults, sensorErrors, sessionEndTime, sessionDuration)
                activeRecorders.clear()
                reconnectionAttempts.clear()
                crashRecoveryMarker?.delete()
                currentSessionId?.let { sessionId ->
                    crashRecoveryManager.markSessionCompleted(sessionId)
                }                stopForegroundService()
                sessionMetadata = null
                currentSessionId = null
                currentSessionDirectory = null
                _recordingStateFlow.value = RecordingState.IDLE                Log.i(
                    TAG,
                    " Stop results: ${stopResults.entries.joinToString { "${it.key}=${if (it.value) "" else ""}" }}"
                )
                val finalSessionState = when {
                    stopResults.isEmpty() -> SessionState.STOPPED_COMPLETED
                    stopResults.values.all { it } -> SessionState.STOPPED_COMPLETED
                    stopResults.values.any { it } -> SessionState.STOPPED_INCOMPLETE
                    else -> SessionState.STOPPED_FAILED
                }
                transitionSessionState(SessionState.STOPPING, finalSessionState)
                if (sensorErrors.isNotEmpty()) {                }
                return@withContext true
            } catch (e: Exception) {                transitionSessionState(currentSessionState.get(), SessionState.STOPPED_FAILED)
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
                            val healthInfo = sensorHealthStatus[sensorName]
                            if (healthInfo != null && !healthInfo.isHealthy && healthInfo.consecutiveFailures >= 3) {                                attemptSensorReconnection(sensorName)
                            }
                        }
                    }
                    updateSensorStatusFlow()
                    delay(HEALTH_CHECK_INTERVAL_MS)
                } catch (e: Exception) {                    delay(HEALTH_CHECK_ERROR_DELAY_MS)
                }
            }
        }
    }

    private suspend fun attemptSensorReconnection(sensorName: String) {
        val currentAttempts = reconnectionAttempts.getOrDefault(sensorName, 0)
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {            activeRecorders[sensorName] = false
            return
        }
        try {
            Log.i(
                TAG,
                "Attempting to reconnect sensor $sensorName (attempt ${currentAttempts + 1}/$MAX_RECONNECTION_ATTEMPTS)"
            )
            reconnectionAttempts[sensorName] = currentAttempts + 1
            val sensor = sensorRecorders[sensorName]
            if (sensor != null) {
                try {
                    sensor.stopRecording()
                    delay(1000)
                } catch (e: Exception) {                }
                sessionMetadata?.let { meta ->
                    val sessionDir = currentSessionDirectory?.rootDir
                    if (sessionDir != null) {
                        val sensorDir = File(sessionDir, sensorName.lowercase())
                        val success = sensor.startRecording(sensorDir.absolutePath, meta)
                        if (success) {                            reconnectionAttempts[sensorName] = 0 // Reset attempts on success
                            updateSensorHealth(sensorName, true)
                        } else {                            updateSensorHealth(sensorName, false)
                        }
                    }
                }
            }
        } catch (e: Exception) {            updateSensorHealth(sensorName, false)
        }
    }

    private fun startStatisticsUpdates() {
        recordingScope.launch {
            while (_isRecording.get()) {
                try {
                    updateRecordingStats()
                    delay(STATS_UPDATE_INTERVAL_MS)
                } catch (e: Exception) {                    delay(STATS_UPDATE_ERROR_DELAY_MS)
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
                sensorId = name,
                isActive = activeRecorders[name] ?: false,
                isHealthy = health.isHealthy,
                lastSampleTime = System.currentTimeMillis(),
                samplesRecorded = 0L, // Would need to track this from sensors
                errorCount = 0 // Would need to track this from sensors
            )
        }
        _sensorStatusFlow.value = statusList
    }

    private fun updateRecordingStats() {
        val currentTime = System.currentTimeMillis()
        val duration = if (sessionStartTime.get() > 0) currentTime - sessionStartTime.get() else 0
        val stats = RecordingStats(
            sessionId = currentSessionId ?: "",
            duration = duration,
            activeSensors = activeRecorders.count { it.value },
            totalSamples = 0L, // Would need to aggregate from sensors
            avgDataRate = 0.0, // Would need to calculate from sensors
            storageUsedMB = 0.0, // Would need to calculate from file sizes
            errors = sensorHealthStatus.count { !it.value.isHealthy },
            warnings = 0, // Would need to track warnings
            qualityScore = if (sensorHealthStatus.isEmpty()) 1.0 else sensorHealthStatus.values.count { it.isHealthy }
                .toDouble() / sensorHealthStatus.size
        )
        _recordingStatsFlow.value = stats
    }

    private fun checkSensorHealth(sensorName: String) {
        val sensor = sensorRecorders[sensorName]
        val isHealthy = sensor?.isRecording == true
        updateSensorHealth(sensorName, isHealthy)
    }

    fun getCurrentSessionDirectory(): String? {
        return try {
            currentSessionDirectory?.rootDir?.absolutePath
        } catch (e: Exception) {            null
        }
    }

    private fun cleanupFailedRecording() {
        activeRecorders.clear()
        sessionMetadata = null
        crashRecoveryMarker?.delete()
        currentSessionId?.let { sessionId ->
            crashRecoveryManager.markSessionFailed(sessionId, "Recording startup failed")
        }
        currentSessionId = null
    }

    private fun getAvailableSpaceGB(): Double {
        return try {
            // Use the same logic as UnifiedSessionUtils to ensure consistency
            val rootDir = context.getExternalFilesDir(null) ?: context.filesDir
            val sessionDir = File(rootDir, "sessions").apply { mkdirs() }
            val freeSpaceBytes = sessionDir.freeSpace
            freeSpaceBytes / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {            RecordingConstants.FALLBACK_AVAILABLE_SPACE_GB
        }
    }

    private fun startForegroundService() {
        try {
            RecordingService.startRecording(
                context,
                currentSessionDirectory?.rootDir?.absolutePath ?: ""
            )        } catch (e: Exception) {        }
    }

    suspend fun initializeSensors(): Boolean {
        return try {            // Get or create a proper lifecycle owner
            val effectiveLifecycleOwner = lifecycleOwner ?: createManagedLifecycleOwner()
            // Initialize RGB Camera Recorder
            try {
                val rgbRecorder = mpdc4gsr.core.data.RgbCameraRecorder(
                    context = context,
                    lifecycleOwner = effectiveLifecycleOwner,
                    previewView = null, // No preview needed for background recording
                    useFrontCamera = false,
                    permissionManager = permissionManager
                )
                addSensorRecorder(RGB_SENSOR_NAME, rgbRecorder)            } catch (e: Exception) {            }
            // Initialize Thermal Camera Recorder
            try {
                val thermalRecorder = mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder(
                    context = context,
                    sensorIdParam = THERMAL_SENSOR_ID,
                    thermalFrameRate = THERMAL_FRAME_RATE_HZ,
                    thermalResolution = Pair(THERMAL_WIDTH_PIXELS, THERMAL_HEIGHT_PIXELS)
                )
                addSensorRecorder(THERMAL_SENSOR_NAME, thermalRecorder)            } catch (e: Exception) {            }
            // Initialize GSR Sensor Recorder using Shimmer3GSRRecorder to avoid circular dependency
            try {
                val gsrRecorder = mpdc4gsr.core.data.Shimmer3GSRRecorder(
                    context = context,
                    lifecycleOwner = effectiveLifecycleOwner,
                    sensorId = GSR_SENSOR_ID,
                    samplingRateHz = GSR_SAMPLING_RATE_HZ
                )
                addSensorRecorder(GSR_SENSOR_NAME, gsrRecorder)            } catch (e: Exception) {            }
            val registeredSensors = sensorRecorders.keys.toList()
            Log.i(
                TAG,
                "Sensor initialization completed - registered sensors: ${registeredSensors.joinToString()}"
            )
            // Return true if at least one sensor was registered successfully
            registeredSensors.isNotEmpty()
        } catch (e: Exception) {            false
        }
    }

    private suspend fun createManagedLifecycleOwner(): LifecycleOwner {
        return withContext(Dispatchers.Main) {
            object : LifecycleOwner {
                private val lifecycleRegistry = LifecycleRegistry(this).apply {
                    // Properly initialize the lifecycle state for camera operations
                    currentState = Lifecycle.State.INITIALIZED
                    currentState = Lifecycle.State.CREATED
                    currentState = Lifecycle.State.STARTED
                    currentState = Lifecycle.State.RESUMED
                }
                override val lifecycle: Lifecycle get() = lifecycleRegistry
            }
        }
    }

    fun getAvailableSensors(): List<SensorHealthSummary> {
        return sensorRecorders.keys.map { sensorName ->
            SensorHealthSummary(
                sensorId = sensorName,
                name = sensorName,
                isHealthy = sensorHealthStatus[sensorName]?.isHealthy ?: false
            )
        }
    }

    suspend fun startSession(sessionId: String): Boolean {
        return startRecording(sessionId = sessionId)
    }

    suspend fun stopSession(): Boolean {
        return stopRecording()
    }

    fun getActiveSensorCount(): Int {
        return activeRecorders.count { it.value }
    }

    suspend fun addSyncMarker(markerType: String, timestampNs: Long) {
        try {
            if (!isRecording) {                return
            }            activeRecorders.keys.forEach { sensorName ->
                sensorRecorders[sensorName]?.let { recorder ->
                    try {
                        recorder.addSyncMarker(markerType, timestampNs)                    } catch (e: Exception) {                    }
                }
            }
        } catch (e: Exception) {        }
    }

    suspend fun cleanup() {
        try {            activeRecorders.clear()
            sensorHealthStatus.clear()
            reconnectionAttempts.clear()
        } catch (e: Exception) {        }
    }

    private fun stopForegroundService() {
        try {
            RecordingService.stopRecording(context)        } catch (e: Exception) {        }
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
                    val sessionInfoFile = File(sessionDir, "session_info.json")
                    sessionInfoFile.writeText(createSessionInfoJson(sessionInfo))
                    writeSessionManifest()                } else {                }
            }
        } catch (e: Exception) {        }
    }

    private fun createSessionInfoJson(sessionInfo: SessionInfoData): String {
        return JSONObject().apply {
            put("session_id", sessionInfo.sessionId)
            put("start_time", sessionInfo.startTime)
            put("end_time", sessionInfo.endTime)
            put("duration_ms", sessionInfo.durationMs)
            put("duration_seconds", sessionInfo.durationSeconds)
            put("recording_status", sessionInfo.recordingStatus)
            put("active_sensors", JSONArray(sessionInfo.activeSensors))
            put("sensor_stop_results", JSONObject(sessionInfo.sensorStopResults as Map<*, *>))
            sessionInfo.errors?.let { put("errors", JSONArray(it)) }
            put("finalized_at", sessionInfo.finalizedAt)
        }.toString(2)
    }

    fun generateSessionManifest(): SessionManifest {
        val sessionId = currentSessionId ?: "unknown"
        val startTime = sessionStartTime.get()
        val stopTime = if (currentSessionState.get() in listOf(
                SessionState.STOPPED_COMPLETED,
                SessionState.STOPPED_FAILED,
                SessionState.STOPPED_INCOMPLETE
            )
        ) System.currentTimeMillis() else null
        val duration = stopTime?.let { it - startTime }
        val sensorActivitySummary = sensorRecorders.keys.associateWith { sensorName ->
            val wasActive = activeRecorders[sensorName] == true
            val healthInfo = sensorHealthStatus[sensorName]
            val sensorEvents = sessionEvents.filter { it.sensorId == sensorName }
            val dropoutEvents = sensorEvents.filter {
                it.eventType == "SENSOR_DROPOUT" || it.eventType == "SENSOR_DISCONNECTION"
            }.sortedBy { it.timestampMs }
            val reconnectionEvents = sensorEvents.filter {
                it.eventType == "SENSOR_RECONNECTION_SUCCESS" || it.eventType == "SENSOR_RESUMED"
            }.sortedBy { it.timestampMs }
            val dropouts = dropoutEvents.map { dropoutEvent ->
                val reconnection =
                    reconnectionEvents.firstOrNull { it.timestampMs > dropoutEvent.timestampMs }
                val durationMs = when {
                    reconnection != null -> reconnection.timestampMs - dropoutEvent.timestampMs
                    stopTime != null -> stopTime - dropoutEvent.timestampMs
                    else -> 0L
                }
                DropoutEvent(
                    sensorId = sensorName,
                    startTime = dropoutEvent.timestampMs,
                    endTime = reconnection?.timestampMs,
                    reason = dropoutEvent.errorMessage ?: "Unknown reason",
                    recoverable = true
                )
            }
            val reconnections = sensorEvents.filter {
                it.eventType == "SENSOR_RECONNECTION_SUCCESS" || it.eventType == "SENSOR_RESUMED"
            }.mapIndexed { index, event ->
                ReconnectionEvent(
                    sensorId = sensorName,
                    timestamp = event.timestampMs,
                    successful = event.success,
                    attemptCount = index + 1,
                    errorMessage = if (!event.success) event.errorMessage else null
                )
            }
            SensorActivityInfo(
                sensorName = sensorName,
                wasActive = wasActive,
                startedSuccessfully = wasActive,
                finalStatus = when {
                    wasActive && healthInfo?.isHealthy == true -> "COMPLETED"
                    wasActive && healthInfo?.isHealthy == false -> "COMPLETED_WITH_ERRORS"
                    !wasActive -> "INACTIVE"
                    else -> "UNKNOWN"
                },
                errorMessages = healthInfo?.lastError?.let { listOf(it) } ?: emptyList(),
                dropouts = dropouts,
                reconnections = reconnections
            )
        }
        val events = sessionEvents.map { event ->
            SessionEvent(
                eventType = event.eventType,
                timestampMs = event.timestampMs,
                sensorId = event.sensorId,
                triggerSource = convertFromRecordingControllerTriggerSource(event.triggerSource),
                metadata = event.metadata,
                success = event.success,
                errorMessage = event.errorMessage
            )
        }
        val errors = sessionEvents.filter { !it.success }.map {
            "${it.eventType}: ${it.errorMessage ?: "Unknown error"}"
        }
        val warnings = sessionEvents.filter {
            it.eventType.contains("WARNING") || it.eventType.contains("CRITICAL") ||
                    it.eventType.contains("DROPOUT") || it.eventType.contains("RECONNECTION")
        }.map { "${it.eventType}: ${it.metadata}" }
        val fileReferences = mutableMapOf<String, String>()
        currentSessionDirectory?.rootDir?.let { sessionDir ->
            fileReferences["session_info"] = "${sessionDir.name}/session_info.json"
            fileReferences["session_manifest"] = "${sessionDir.name}/session_manifest.json"
        }
        return SessionManifest(
            sessionId = sessionId,
            startTime = startTime,
            stopTime = stopTime,
            duration = duration,
            triggerSource = lastTriggerSource ?: TriggerSource.LOCAL_UI,
            sensorActivitySummary = sensorActivitySummary,
            events = events,
            errors = errors,
            warnings = warnings,
            fileReferences = fileReferences,
            sessionState = currentSessionState.get()
        )
    }

    private suspend fun writeSessionManifest() {
        try {
            currentSessionDirectory?.rootDir?.let { sessionDir ->
                val manifest = generateSessionManifest()
                val manifestFile = File(sessionDir, "session_manifest.json")
                val manifestJson = JSONObject().apply {
                    put("sessionId", manifest.sessionId)
                    put("startTime", manifest.startTime)
                    manifest.stopTime?.let { put("stopTime", it) }
                    manifest.duration?.let { put("duration", it) }
                    put("triggerSource", manifest.triggerSource)
                    val sensorSummary = JSONObject()
                    manifest.sensorActivitySummary.forEach { (sensorName, info) ->
                        val sensorInfo = JSONObject().apply {
                            put("sensorName", info.sensorName)
                            put("wasActive", info.wasActive)
                            put("startedSuccessfully", info.startedSuccessfully)
                            put("finalStatus", info.finalStatus)
                            put("errorMessages", JSONArray(info.errorMessages))
                            if (info.dropouts.isNotEmpty()) {
                                val dropoutsArray = JSONArray()
                                info.dropouts.forEach { dropout ->
                                    dropoutsArray.put(JSONObject().apply {
                                        put("sensorId", dropout.sensorId)
                                        put("startTime", dropout.startTime)
                                        dropout.endTime?.let { put("endTime", it) }
                                        dropout.reason?.let { put("reason", it) }
                                        put("recoverable", dropout.recoverable)
                                    })
                                }
                                put("dropouts", dropoutsArray)
                            }
                            if (info.reconnections.isNotEmpty()) {
                                val reconnectionsArray = JSONArray()
                                info.reconnections.forEach { reconnection ->
                                    reconnectionsArray.put(JSONObject().apply {
                                        put("timestamp", reconnection.timestamp)
                                        put("attemptCount", reconnection.attemptCount)
                                        put("successful", reconnection.successful)
                                        reconnection.errorMessage?.let { put("errorMessage", it) }
                                    })
                                }
                                put("reconnections", reconnectionsArray)
                            }
                        }
                        sensorSummary.put(sensorName, sensorInfo)
                    }
                    put("sensorActivitySummary", sensorSummary)
                    val eventsArray = JSONArray()
                    manifest.events.forEach { event ->
                        eventsArray.put(JSONObject().apply {
                            put("eventType", event.eventType)
                            put("timestampMs", event.timestampMs)
                            event.sensorId?.let { put("sensorId", it) }
                            event.triggerSource.let { put("triggerSource", it) }
                            put("success", event.success)
                            event.errorMessage?.let { put("errorMessage", it) }
                            if (event.metadata.isNotEmpty()) {
                                put("metadata", JSONObject(event.metadata))
                            }
                        })
                    }
                    put("events", eventsArray)
                    if (manifest.errors.isNotEmpty()) {
                        put("errors", JSONArray(manifest.errors))
                    }
                    if (manifest.warnings.isNotEmpty()) {
                        put("warnings", JSONArray(manifest.warnings))
                    }
                    put("fileReferences", JSONObject(manifest.fileReferences as Map<*, *>))
                    put("sessionState", manifest.sessionState)
                }
                manifestFile.writeText(manifestJson.toString(2))            }
        } catch (e: Exception) {        }
    }

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
        } catch (e: Exception) {            "Error getting status"
        }
    }

    private fun transitionSessionState(from: SessionState, to: SessionState): Boolean {
        return currentSessionState.compareAndSet(from, to).also { success ->
            if (success) {                addSessionEvent(
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
        val event = RecordingControllerSessionEvent(
            eventType = eventType,
            timestampMs = System.currentTimeMillis(),
            sensorId = sensorId,
            triggerSource = convertTriggerSource(triggerSource ?: DEFAULT_TRIGGER_SOURCE),
            metadata = metadata,
            success = success,
            errorMessage = errorMessage
        )
        sessionEvents.add(event)
        " } ?: ""}")
    }

    private fun convertTriggerSource(source: TriggerSource): RecordingController.TriggerSource {
        return when (source) {
            TriggerSource.LOCAL_UI -> RecordingController.TriggerSource.LOCAL_UI
            TriggerSource.LOCAL_NOTIFICATION -> RecordingController.TriggerSource.LOCAL_NOTIFICATION
            TriggerSource.REMOTE_PC -> RecordingController.TriggerSource.REMOTE_PC
            TriggerSource.AUTOMATIC -> RecordingController.TriggerSource.AUTOMATIC
            TriggerSource.CRASH_RECOVERY -> RecordingController.TriggerSource.CRASH_RECOVERY
        }
    }

    private fun convertFromRecordingControllerTriggerSource(source: RecordingController.TriggerSource?): TriggerSource? {
        return when (source) {
            RecordingController.TriggerSource.LOCAL_UI -> TriggerSource.LOCAL_UI
            RecordingController.TriggerSource.LOCAL_NOTIFICATION -> TriggerSource.LOCAL_NOTIFICATION
            RecordingController.TriggerSource.REMOTE_PC -> TriggerSource.REMOTE_PC
            RecordingController.TriggerSource.AUTOMATIC -> TriggerSource.AUTOMATIC
            RecordingController.TriggerSource.CRASH_RECOVERY -> TriggerSource.CRASH_RECOVERY
            null -> null
        }
    }

    private fun convertSessionState(state: SessionState): RecordingController.SessionState {
        return when (state) {
            SessionState.IDLE -> RecordingController.SessionState.IDLE
            SessionState.STARTING -> RecordingController.SessionState.STARTING
            SessionState.RECORDING -> RecordingController.SessionState.RECORDING
            SessionState.ACTIVE -> RecordingController.SessionState.RECORDING  // Map ACTIVE to RECORDING
            SessionState.STOPPING -> RecordingController.SessionState.STOPPING
            SessionState.COMPLETED -> RecordingController.SessionState.STOPPED_COMPLETED
            SessionState.STOPPED_COMPLETED -> RecordingController.SessionState.STOPPED_COMPLETED
            SessionState.STOPPED_FAILED -> RecordingController.SessionState.STOPPED_FAILED
            SessionState.STOPPED_INCOMPLETE -> RecordingController.SessionState.STOPPED_INCOMPLETE
            SessionState.FAILED -> RecordingController.SessionState.STOPPED_FAILED
            SessionState.CANCELLED -> RecordingController.SessionState.STOPPED_INCOMPLETE
        }
    }

    private fun convertFromRecordingControllerSessionState(state: RecordingController.SessionState): SessionState {
        return when (state) {
            RecordingController.SessionState.IDLE -> SessionState.IDLE
            RecordingController.SessionState.STARTING -> SessionState.STARTING
            RecordingController.SessionState.RECORDING -> SessionState.RECORDING
            RecordingController.SessionState.STOPPING -> SessionState.STOPPING
            RecordingController.SessionState.STOPPED_COMPLETED -> SessionState.STOPPED_COMPLETED
            RecordingController.SessionState.STOPPED_FAILED -> SessionState.STOPPED_FAILED
            RecordingController.SessionState.STOPPED_INCOMPLETE -> SessionState.STOPPED_INCOMPLETE
        }
    }
}
