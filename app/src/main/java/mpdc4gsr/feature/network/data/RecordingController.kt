package mpdc4gsr.feature.network.data

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.sensors.api.ErrorType
import mpdc4gsr.core.sensors.api.RecordingStats
import mpdc4gsr.core.sensors.api.RecordingStatus
import mpdc4gsr.core.sensors.api.SensorError
import mpdc4gsr.core.sensors.api.SensorRecorder
import mpdc4gsr.core.data.utils.SessionDirectory
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.data.utils.StorageStatus
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.network.data.RecordingConstants.RGB_STORAGE_MB_PER_MIN
import mpdc4gsr.feature.network.data.RecordingConstants.SHIMMER_STORAGE_MB_PER_MIN
import mpdc4gsr.feature.network.data.RecordingConstants.THERMAL_STORAGE_MB_PER_MIN
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class RecordingController(
    private val context: Context,
    lifecycleOwner: LifecycleOwner? = null
) {
    companion object {
        // Reconnection settings
        private const val MAX_RECONNECTION_ATTEMPTS = 3
        // Type aliases for public API compatibility
        typealias SessionManifest = mpdc4gsr.feature.network.data.SessionManifest
        typealias SensorActivityInfo = RecordingControllerSensorActivityInfo
        typealias SessionEvent = RecordingControllerSessionEvent
        typealias SensorHealthInfo = RecordingControllerSensorHealthInfo
        typealias DropoutEvent = RecordingControllerDropoutEvent
        typealias ReconnectionEvent = RecordingControllerReconnectionEvent
    }

    private val controllerLifecycleOwner: LifecycleOwner by lazy {
        lifecycleOwner ?: createFallbackLifecycleOwner()
    }
    val lifecycleOwner: LifecycleOwner
        get() = controllerLifecycleOwner
    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private val timeSynchronizationService = TimeSynchronizationService()
    private var _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()
    private var currentSessionDirectory: SessionDirectory? = null
    private var sessionMetadata: SessionMetadata? = null
    private val sessionMetadataLock = Any()
    private var recordingStartTime: Long = 0
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statusMonitoringJob: Job? = null
    private var errorMonitoringJob: Job? = null
    private val _recordingStateFlow = MutableStateFlow(RecordingState.STOPPED)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingStateFlow.asStateFlow()
    private val _sensorStatusFlow = MutableSharedFlow<List<RecordingStatus>>()
    val sensorStatusFlow: SharedFlow<List<RecordingStatus>> = _sensorStatusFlow.asSharedFlow()
    private val _errorFlow = MutableSharedFlow<RecordingControllerError>()
    val errorFlow: SharedFlow<RecordingControllerError> = _errorFlow.asSharedFlow()
    private val _syncEventFlow = MutableSharedFlow<SyncEvent>()
    val syncEventFlow: SharedFlow<SyncEvent> = _syncEventFlow.asSharedFlow()
    private fun createFallbackLifecycleOwner(): LifecycleOwner {
        return object : LifecycleOwner {
            private val lifecycleRegistry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }

            override fun getLifecycle(): Lifecycle = lifecycleRegistry
        }
    }

    fun registerSensor(sensorName: String, sensorRecorder: SensorRecorder) {
        sensorRecorders[sensorName] = sensorRecorder
    }

    fun registerRgbCameraWithPreview(rgbCameraRecorder: RgbCameraRecorder) {
        registerSensor("RGB", rgbCameraRecorder)
    }

    fun unregisterSensor(sensorName: String) {
        sensorRecorders.remove(sensorName)?.let { sensor -> }
    }

    suspend fun initializeSensors(skipRgbCamera: Boolean = false): Boolean {
        return withContext(Dispatchers.IO) {
            try {                // Only create default RGB camera if not externally provided
                if (!skipRgbCamera && !sensorRecorders.containsKey("RGB")) {
                    val rgbCamera = RgbCameraRecorder(context, controllerLifecycleOwner, null)
                    registerSensor("RGB", rgbCamera)
                }
                val thermalCamera = ThermalCameraRecorder(context, "thermal_camera_1")
                val gsrSensor =
                    GSRSensorRecorder(context, "gsr_shimmer_1", 128, this@RecordingController)
                registerSensor("Thermal", thermalCamera)
                registerSensor("Shimmer", gsrSensor)
                val initJobs = sensorRecorders.map { (sensorName, sensor) ->
                    async {
                        try {
                            val success = sensor.initialize()
                            Triple(sensorName, sensor, success)
                        } catch (e: Exception) {
                            emitError(
                                RecordingControllerError(
                                    errorType = "SENSOR_INIT_EXCEPTION",
                                    message = "Sensor $sensorName threw exception during initialization: ${e.message}",
                                    sensorId = sensorName,
                                    isRecoverable = true
                                )
                            )
                            Triple(sensorName, sensor, false)
                        }
                    }
                }
                val initResults = initJobs.awaitAll()
                val successfulInits = initResults.filter { it.third }
                val failedInits = initResults.filter { !it.third }
                failedInits.forEach { (sensorName, _, _) ->
                    sensorRecorders.remove(sensorName)
                    emitError(
                        RecordingControllerError(
                            errorType = "SENSOR_INIT_FAILED",
                            message = "Failed to initialize sensor: $sensorName",
                            sensorId = sensorName,
                            isRecoverable = true
                        )
                    )
                }
                startMonitoring()
                val successCount = successfulInits.size
                val totalCount = initResults.size
            }")
            successCount > 0
        } catch (e: Exception) {
            emitError(
                RecordingControllerError(
                    errorType = "INIT_FAILED",
                    message = "Sensor initialization failed: ${e.message}",
                    isRecoverable = false
                )
            )
            false
        }
    }
}

private val activeRecorders = ConcurrentHashMap<String, Boolean>()
private val sensorHealthStatus =
    ConcurrentHashMap<String, RecordingControllerSensorHealthInfo>()
private val reconnectionAttempts = ConcurrentHashMap<String, Int>()

// Session orchestration state
private var currentSessionState = AtomicReference(SessionState.IDLE)
private var lastTriggerSource: TriggerSource? = null
private var sessionEvents = mutableListOf<RecordingControllerSessionEvent>()
private var sessionStartTimestampMs: Long = 0
private var sessionStartTimestampNs: Long = 0
suspend fun startRecording(
    sessionId: String? = null,
    participantId: String? = null,
    studyName: String? = null,
    enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer"),
    triggerSource: TriggerSource = TriggerSource.LOCAL_UI
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Enforce single-session operation
            if (_isRecording.get()) {
                return@withContext true
            }
            // Transition to STARTING state
            val transitionSuccess =
                transitionSessionState(SessionState.IDLE, SessionState.STARTING)
            if (!transitionSuccess) {
                return@withContext false
            }
            lastTriggerSource = triggerSource
            addSessionEvent("SESSION_START_REQUESTED", triggerSource = triggerSource)
            _recordingStateFlow.value = RecordingState.STARTING
            // Phase 1: Prerequisite Checks                val validationResult = validateRecordingPrerequisites(enabledSensors)
            if (!validationResult.isValid) {
                transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                _recordingStateFlow.value = RecordingState.ERROR
                addSessionEvent(
                    "VALIDATION_FAILED",
                    errorMessage = validationResult.errorMessage
                )
                emitError(
                    RecordingControllerError(
                        errorType = "VALIDATION_FAILED",
                        message = validationResult.errorMessage,
                        isRecoverable = validationResult.isRecoverable,
                        details = validationResult.details
                    )
                )
                return@withContext false
            }
            // Phase 2: Storage validation                val storageStatus = sessionDirectoryManager.checkStorageSpace()
            if (storageStatus.isLowStorage) {
                transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                _recordingStateFlow.value = RecordingState.ERROR
                addSessionEvent(
                    "STORAGE_CHECK_FAILED",
                    errorMessage = "Insufficient storage: ${storageStatus.formattedAvailable}"
                )
                emitError(
                    RecordingControllerError(
                        errorType = "STORAGE_FULL",
                        message = "Insufficient storage space. Only ${storageStatus.formattedAvailable} available. Need at least 500MB free.",
                        isRecoverable = false,
                        details = mapOf(
                            "available_space" to storageStatus.formattedAvailable,
                            "required_space" to "500MB",
                            "estimated_session_size" to estimateSessionSize(enabledSensors)
                        )
                    )
                )
                return@withContext false
            }
            if (storageStatus.shouldWarn) {
                addSessionEvent(
                    "STORAGE_WARNING",
                    metadata = mapOf("available" to storageStatus.formattedAvailable)
                )
                emitError(
                    RecordingControllerError(
                        errorType = "STORAGE_WARNING",
                        message = "Storage space low: ${storageStatus.formattedAvailable} available",
                        isRecoverable = true
                    )
                )
            }
            val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
            val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
            sessionMetadata = SessionMetadata.createSessionStart(finalSessionId).copy(
                participantId = participantId,
                studyName = studyName
            )
            createCrashRecoveryMarker(finalSessionId, enabledSensors, sessionDir)
            val utilMetadata = mpdc4gsr.core.data.utils.SessionMetadata(
                startTime = sessionMetadata!!.sessionStartTimestampMs,
                enabledSensors = enabledSensors,
                participantId = participantId,
                studyName = studyName
            )
            sessionDirectoryManager.createSessionMetadata(sessionDir, utilMetadata)
            currentSessionDirectory = sessionDir
            persistSessionMetadata()
            val sessionReference =
                timeSynchronizationService.initializeSession(sessionDir.rootDir.absolutePath)
            sessionStartTimestampMs = sessionReference.sessionStartSystemMs
            sessionStartTimestampNs = sessionReference.sessionStartMonotonicNs
            recordingStartTime = sessionStartTimestampNs activeRecorders . clear ()
            val startJobs = sensorRecorders.map { (sensorName, sensor) ->
                async(SupervisorJob()) {
                    try {
                        val sensorDir = resolveSensorDirectory(sessionDir, sensorName)
                        sensorDir.mkdirs()
                        val sensorStartReference = SystemClock.elapsedRealtimeNanos()
                        val currentSessionMetadata = sessionMetadata
                        if (currentSessionMetadata == null) {
                            emitError(
                                RecordingControllerError(
                                    errorType = "SESSION_METADATA_NULL",
                                    message = "Session metadata is null when starting sensor $sensorName",
                                    sensorId = sensorName,
                                    isRecoverable = false
                                )
                            )
                            return@async Triple(
                                sensorName,
                                false,
                                IllegalStateException("Session metadata is null")
                            )
                        }
                        val success = sensor.startRecording(
                            sensorDir.absolutePath,
                            currentSessionMetadata
                        )
                        if (success) {
                            activeRecorders[sensorName] = true
                            updateSensorHealth(sensorName, true)
                            addSessionEvent("SENSOR_START_SUCCESS", sensorId = sensorName)
                            val relativePath = runCatching {
                                sensorDir.relativeTo(sessionDir.rootDir).path
                            }.getOrElse { sensorDir.name }
                            updateSessionMetadata {
                                markSensorStart(
                                    sensorName = sensorName,
                                    sensorId = sensor.sensorId,
                                    sensorType = sensor.sensorType,
                                    startMonotonicNs = sensorStartReference,
                                    metadata = mapOf(
                                        "directory" to relativePath
                                    )
                                )
                                when (sensorName.lowercase()) {
                                    "rgb", "camera", "rgbcamera" -> addModalityFile(
                                        "rgb_video",
                                        "$relativePath/${SessionDirectoryManager.RGB_VIDEO_FILE}"
                                    )

                                    "thermal", "thermalcamera" -> addModalityFile(
                                        "thermal_frames",
                                        "$relativePath/${SessionDirectoryManager.THERMAL_FRAMES_FILE}"
                                    )

                                    "shimmer", "gsr", "gsrsensor" -> addModalityFile(
                                        "shimmer_data",
                                        "$relativePath/${SessionDirectoryManager.SHIMMER_DATA_FILE}"
                                    )
                                }
                            }
                        } else {
                        }
                        Triple(sensorName, success, null)
                    } catch (e: Exception) {
                        updateSensorHealth(sensorName, false, "Start exception: ${e.message}")
                        addSessionEvent(
                            "SENSOR_START_EXCEPTION",
                            sensorId = sensorName,
                            success = false,
                            errorMessage = e.message
                        )
                        emitError(
                            RecordingControllerError(
                                errorType = "SENSOR_START_EXCEPTION",
                                message = "Sensor $sensorName threw exception during start: ${e.message}",
                                sensorId = sensorName,
                                isRecoverable = true
                            )
                        )
                        Triple(sensorName, false, e)
                    }
                }
            }
            val startResults = startJobs.awaitAll()
            val successfulStarts = startResults.filter { it.second }
            val failedStarts = startResults.filter { !it.second }
            // Enhanced sensor start result processing
            successfulStarts.forEach { (sensorName, _, _) ->
                addSessionEvent("SENSOR_STARTED", sensorId = sensorName, success = true)
            }
            failedStarts.forEach { (sensorName, _, exception) ->
                val errorDetails = if (exception != null) {
                    " (Exception: ${exception.message})"
                } else {
                    " (Returned false)"
                } updateSensorHealth (sensorName, false, "Start failed$errorDetails")
                addSessionEvent(
                    "SENSOR_START_FAILED", sensorId = sensorName, success = false,
                    errorMessage = "Start failed$errorDetails"
                )
                emitError(
                    RecordingControllerError(
                        errorType = "SENSOR_START_FAILED",
                        message = "Failed to start sensor: $sensorName$errorDetails",
                        sensorId = sensorName,
                        isRecoverable = true
                    )
                )
            }
            // Implement partial sensor start capability - proceed if ANY sensor starts
            if (successfulStarts.isNotEmpty()) {
                _isRecording.set(true)
                _recordingStateFlow.value = RecordingState.RECORDING
                // Transition to RECORDING state
                transitionSessionState(SessionState.STARTING, SessionState.RECORDING)
                addSessionEvent(
                    "SESSION_RECORDING_STARTED", success = true, metadata = mapOf(
                        "active_sensors" to successfulStarts.size.toString(),
                        "total_sensors" to startResults.size.toString(),
                        "partial_start" to (failedStarts.isNotEmpty()).toString()
                    )
                )
                addSyncMarker("session_start", sessionStartTimestampNs)
                val totalSensors = startResults.size
                val successCount = successfulStarts.size
                val sessionTypeMessage = if (failedStarts.isEmpty()) {
                    "Full multi-modal recording session started"
                } else {
                    "Partial multi-modal recording session started"
                } Log . i (
                        TAG,
                "Active sensors: ${successfulStarts.joinToString(", ") { it.first }}"
                )
                if (failedStarts.isNotEmpty()) {
                }
                // Start health monitoring for active sensors
                startSensorHealthMonitoring()
                true
            } else {
                // No sensors started successfully - abort session
                _recordingStateFlow.value = RecordingState.ERROR
                transitionSessionState(SessionState.STARTING, SessionState.STOPPED_FAILED)
                addSessionEvent(
                    "SESSION_START_FAILED",
                    success = false,
                    errorMessage = "No sensors could be started"
                )
                currentSessionDirectory?.let { sessionDir ->
                    sessionDirectoryManager.updateSessionMetadata(
                        sessionDir,
                        System.currentTimeMillis(),
                        "FAILED",
                        failedStarts.associate {
                            it.first to (it.third?.message ?: "Unknown error")
                        }
                    )
                } emitError (
                        RecordingControllerError(
                            errorType = "ALL_SENSORS_FAILED",
                            message = "All sensors failed to start: ${failedStarts.joinToString(", ") { it.first }}",
                            isRecoverable = true
                        )
                        )
                safeStopAll()
                false
            }
        } catch (e: Exception) {
            _recordingStateFlow.value = RecordingState.ERROR
            currentSessionDirectory?.let { sessionDir ->
                sessionDirectoryManager.updateSessionMetadata(
                    sessionDir,
                    System.currentTimeMillis(),
                    "ERROR",
                    mapOf("error" to (e.message ?: "Unknown exception"))
                )
            }
            emitError(
                RecordingControllerError(
                    errorType = "START_FAILED",
                    message = "Failed to start recording: ${e.message}",
                    isRecoverable = true
                )
            )
            false
        }
    }
}

suspend fun startRecording(sessionDirectory: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (_isRecording.get()) {
                return@withContext true
            } _recordingStateFlow . value = RecordingState . STARTING
            val sessionDir = File(sessionDirectory)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            val sessionDirWrapper = SessionDirectory(
                sessionId = sessionDir.name,
                rootDir = sessionDir,
                rgbDir = sessionDir,
                thermalDir = sessionDir,
                shimmerDir = sessionDir
            )
            currentSessionDirectory = sessionDirWrapper
            recordingStartTime = System.nanoTime()
            sessionMetadata = SessionMetadata.createSessionStart(sessionDir.name)
            val startJobs = sensorRecorders.values.map { sensor ->
                async {
                    try {
                        val success = sensor.startRecording(sessionDirectory, sessionMetadata!!)
                        Triple(sensor.sensorId, success, null)
                    } catch (e: Exception) {
                        emitError(
                            RecordingControllerError(
                                errorType = "SENSOR_START_EXCEPTION",
                                message = "Sensor ${sensor.sensorId} threw exception during start: ${e.message}",
                                sensorId = sensor.sensorId,
                                isRecoverable = true
                            )
                        )
                        Triple(sensor.sensorId, false, e)
                    }
                }
            }
            val startResults = startJobs.awaitAll()
            val successfulStarts = startResults.filter { it.second }
            val failedStarts = startResults.filter { !it.second }
            successfulStarts.forEach { (sensorId, _, _) -> }
            failedStarts.forEach { (sensorId, _, exception) ->
                val errorDetails = if (exception != null) {
                    " (Exception: ${exception.message})"
                } else {
                    " (Returned false)"
                }
                if (sensorId.contains("gsr", ignoreCase = true)) {
                    emitError(
                        RecordingControllerError(
                            errorType = "GSR_SENSOR_UNAVAILABLE",
                            message = "GSR sensor unavailable: $sensorId$errorDetails - check device pairing and proximity",
                            sensorId = sensorId,
                            isRecoverable = true
                        )
                    )
                } else {
                    emitError(
                        RecordingControllerError(
                            errorType = "SENSOR_START_FAILED",
                            message = "Failed to start sensor: $sensorId$errorDetails",
                            sensorId = sensorId,
                            isRecoverable = true
                        )
                    )
                }
            }
            if (successfulStarts.isNotEmpty()) {
                _isRecording.set(true)
                _recordingStateFlow.value = RecordingState.RECORDING
                sessionMetadata?.addSyncEvent(
                    "session_start", mapOf(
                        "total_sensors" to startResults.size.toString(),
                        "successful_sensors" to successfulStarts.size.toString(),
                        "failed_sensors" to failedStarts.size.toString()
                    )
                )
                addSyncMarker("session_start", recordingStartTime)
                val totalSensors = startResults.size
                val successCount = successfulStarts.size
                val gsrFailed = failedStarts.any { it.first.contains("gsr", ignoreCase = true) }
                val statusMessage = if (gsrFailed && successCount > 0) {
                    "Multi-modal recording started with $successCount/$totalSensors sensors (GSR unavailable - check Shimmer device)"
                } else {
                    "Multi-modal recording started with $successCount/$totalSensors sensors"
                }
                true
            } else {
                _recordingStateFlow.value = RecordingState.ERROR emitError (
                        RecordingControllerError(
                            errorType = "ALL_SENSORS_FAILED",
                            message = "All sensors failed to start: ${failedStarts.joinToString(", ") { it.first }}",
                            isRecoverable = true
                        )
                        )
                false
            }
        } catch (e: Exception) {
            _recordingStateFlow.value = RecordingState.ERROR
            emitError(
                RecordingControllerError(
                    errorType = "SESSION_START_FAILED",
                    message = "Failed to start recording session: ${e.message}",
                    isRecoverable = true
                )
            )
            safeStopAll()
            false
        }
    }
}

suspend fun stopSession(triggerSource: TriggerSource = TriggerSource.LOCAL_UI): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (!_isRecording.get()) {
                return@withContext true
            }
            // Transition to STOPPING state
            val transitionSuccess =
                transitionSessionState(SessionState.RECORDING, SessionState.STOPPING)
            if (!transitionSuccess) {
            }
            addSessionEvent(
                "SESSION_STOP_REQUESTED",
                triggerSource = triggerSource
            ) _recordingStateFlow . value = RecordingState . STOPPING
                    sessionMetadata?.let { metadata ->
                        metadata.addSyncEvent(
                            "session_end", mapOf(
                                "recording_duration_ms" to metadata.getRelativeTimestamp().toString()
                            )
                        )
                    }
            addSyncMarker("session_end", System.nanoTime())
            delay(RecordingConstants.SYNC_MARKER_DISTRIBUTION_DELAY_MS)
            val stopResult = safeStopAll()
            _isRecording.set(false)
            _recordingStateFlow.value = RecordingState.STOPPED
            val sessionDuration = if (recordingStartTime > 0) {
                (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
            } else 0.0
            finalizeSessionMetadata(stopResult)
            // Determine final session state based on stop results
            val finalSessionState = when {
                stopResult.isEmpty() -> SessionState.STOPPED_COMPLETED
                stopResult.values.all { it } -> SessionState.STOPPED_COMPLETED
                stopResult.values.any { it } -> SessionState.STOPPED_INCOMPLETE
                else -> SessionState.STOPPED_FAILED
            }
            transitionSessionState(SessionState.STOPPING, finalSessionState)
            addSessionEvent(
                "SESSION_FINALIZED", success = true, metadata = mapOf(
                    "duration_seconds" to sessionDuration.toString(),
                    "final_state" to finalSessionState.toString(),
                    "stopped_sensors" to stopResult.size.toString(),
                    "successful_stops" to stopResult.values.count { it }.toString()
                )
            )
            currentSessionDirectory?.let { sessionDir ->
                val stopErrors = stopResult.filterValues { success -> !success }
                    .mapValues { "STOP_FAILED" }
                val status = when (finalSessionState) {
                    SessionState.STOPPED_COMPLETED -> "COMPLETED"
                    SessionState.STOPPED_FAILED -> "FAILED"
                    SessionState.STOPPED_INCOMPLETE -> "PARTIAL"
                    else -> "UNKNOWN"
                }
                sessionDirectoryManager.updateSessionMetadata(
                    sessionDir,
                    System.currentTimeMillis(),
                    status,
                    stopErrors
                )
            }
            val sessionDurationMs = timeSynchronizationService.finalizeSession()
            activeRecorders.clear()
            sessionStartTimestampMs = 0
            sessionStartTimestampNs = 0
            currentSessionDirectory = null
            true
        } catch (e: Exception) {
            _recordingStateFlow.value = RecordingState.ERROR
            transitionSessionState(currentSessionState.get(), SessionState.STOPPED_FAILED)
            addSessionEvent(
                "SESSION_STOP_ERROR",
                triggerSource = triggerSource,
                success = false,
                errorMessage = e.message
            )
            currentSessionDirectory?.let { sessionDir ->
                sessionDirectoryManager.updateSessionMetadata(
                    sessionDir,
                    System.currentTimeMillis(),
                    "STOP_ERROR",
                    mapOf("stop_error" to (e.message ?: "Unknown exception"))
                )
            }
            emitError(
                RecordingControllerError(
                    errorType = "SESSION_STOP_FAILED",
                    message = "Failed to stop recording session: ${e.message}",
                    isRecoverable = true
                )
            )
            false
        }
    }
}

suspend fun stopRecording(triggerSource: TriggerSource = TriggerSource.LOCAL_UI): Boolean {
    return stopSession(triggerSource)
}

private suspend fun safeStopAll(): Map<String, Boolean> = coroutineScope {
    val stopResults = mutableMapOf<String, Boolean>()
    val activeRecordersList = activeRecorders.keys.mapNotNull { sensorName ->
        sensorRecorders[sensorName]?.let { sensor -> sensorName to sensor }
    }
    if (activeRecordersList.isEmpty()) {
        return@coroutineScope stopResults
    }
    val stopJobs = activeRecordersList.map { (sensorName, sensor) ->
        async(SupervisorJob()) {
            try {
                val success = sensor.stopRecording()
                if (success) {
                } else {
                }
                Triple(sensorName, success, null)
            } catch (e: Exception) {
                Triple(sensorName, false, e)
            }
        }
    }
    val stopJobResults = stopJobs.awaitAll()
    stopJobResults.forEach { (sensorName, success, exception) ->
        stopResults[sensorName] = success
        if (!success) {
            val errorDetails = if (exception != null) {
                " (Exception: ${exception.message})"
            } else {
                " (Returned false)"
            }
        }
        val stopTimestampNs = SystemClock.elapsedRealtimeNanos()
        val sensor = sensorRecorders[sensorName]
        val stats = runCatching { sensor?.getRecordingStats() }.getOrNull()
        updateSessionMetadata {
            markSensorStop(
                sensorName = sensorName,
                stopMonotonicNs = stopTimestampNs,
                success = success,
                stats = stats,
                metadata = mapOf("stop_success" to success.toString()),
                errorMessage = exception?.message,
                sensorId = sensor?.sensorId,
                sensorType = sensor?.sensorType
            )
        }
        activeRecorders.remove(sensorName)
    }
    val successCount = stopResults.count { it.value }
    val totalCount = stopResults.size        return@coroutineScope stopResults
}

private fun resolveSensorDirectory(
    sessionDir: SessionDirectory,
    sensorName: String
): File {
    return when (sensorName.lowercase()) {
        "rgb", "camera", "rgbcamera" -> sessionDir.rgbDir
        "thermal", "thermalcamera" -> sessionDir.thermalDir
        "shimmer", "gsr", "gsrsensor" -> sessionDir.shimmerDir
        else -> File(sessionDir.rootDir, sensorName.lowercase())
    }
}

private fun persistSessionMetadata() {
    val metadata = sessionMetadata ?: return
    val sessionDir = currentSessionDirectory ?: return
    synchronized(sessionMetadataLock) {
        metadata.saveToFile(sessionDir.rootDir)
    }
}

private fun updateSessionMetadata(block: SessionMetadata.() -> Unit) {
    val metadata = sessionMetadata
    val sessionDir = currentSessionDirectory
    if (metadata != null && sessionDir != null) {
        synchronized(sessionMetadataLock) {
            metadata.block()
            metadata.saveToFile(sessionDir.rootDir)
        }
    }
}

private fun finalizeSessionMetadata(stopResults: Map<String, Boolean>) {
    val sessionDir = currentSessionDirectory ?: return
    synchronized(sessionMetadataLock) {
        val metadata = sessionMetadata ?: return
        metadata.recordStopResults(stopResults)
        sessionMetadata = metadata.markSessionEnd()
        sessionMetadata?.saveToFile(sessionDir.rootDir)
    }
}

suspend fun addSyncMarker(
    markerType: String,
    timestampNs: Long,
    metadata: Map<String, String> = emptyMap()
) {
    controllerScope.launch {
        try {
            timeSynchronizationService.logSyncEvent(markerType, metadata)
            val syncJobs = sensorRecorders.values.map { sensor ->
                async {
                    try {
                        sensor.addSyncMarker(markerType, timestampNs, metadata)
                        sensor.sensorId to true
                    } catch (e: Exception) {
                        sensor.sensorId to false
                    }
                }
            }
            val syncResults = syncJobs.awaitAll()
            val successfulSyncs = syncResults.count { it.second }
            val totalSensors = syncResults.size
            val syncEvent = SyncEvent(
                markerType = markerType,
                timestampNs = timestampNs,
                metadata = metadata,
                successfulSensors = successfulSyncs,
                totalSensors = totalSensors
            )
            _syncEventFlow.emit(syncEvent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
        }
    }
}

suspend fun testSensorConnections(): Map<String, Boolean> {
    return withContext(Dispatchers.IO) {
        val testResults = mutableMapOf<String, Boolean>()
        val testJobs = sensorRecorders.map { (sensorId, sensor) ->
            async {
                try {
                    val stats = sensor.getRecordingStats()
                    sensorId to true
                } catch (e: Exception) {
                    sensorId to false
                }
            }
        }
        testJobs.awaitAll().forEach { (sensorId, success) ->
            testResults[sensorId] = success
        } testResults
    }
}

fun getStatusReport(): String {
    val summary = getSensorStatusSummary()
    return buildString {
        appendLine("=== Enhanced Recording Controller Status ===")
        appendLine("Session State: ${summary.sessionState}")
        appendLine("Sensors: ${summary.totalSensorsRecording}/${summary.totalSensorsInitialized} recording")
        appendLine("Status: ${summary.statusMessage}")
        appendLine("Session Directory: ${currentSessionDirectory ?: "None"}")
        appendLine("Active Recorders: ${activeRecorders.size}")
        appendLine()
        appendLine("Individual Sensors:")
        summary.sensors.forEach { sensor ->
            val status = when {
                sensor.isRecording -> " RECORDING"
                sensor.isInitialized -> "🟡 READY"
                else -> " FAILED"
            }
            val activeStatus =
                if (activeRecorders.containsKey(sensor.sensorId)) " (ACTIVE)" else ""
            appendLine("  ${sensor.sensorType}: $status$activeStatus")
        }
        if (_isRecording.get()) {
            val stats = getRecordingStatistics()
            appendLine()
            appendLine("Session Stats:")
            appendLine("  Duration: ${String.format("%.1f", stats.sessionDurationSeconds)}s")
            appendLine("  Total Samples: ${stats.totalSamplesRecorded}")
            appendLine("  Storage Used: ${String.format("%.2f", stats.totalStorageUsedMB)}MB")
            if (sessionStartTimestampMs > 0) {
                appendLine("  Session Start: ${sessionStartTimestampMs}ms")
                appendLine("  Reference Timestamp: ${sessionStartTimestampNs}ns")
            }
        }
        appendLine()
        appendLine("Fault Tolerance Status:")
        appendLine("  Partial Start: ENABLED")
        appendLine("  Mid-Session Recovery: ENABLED")
        appendLine("  Smart Cleanup: ENABLED")
        appendLine("  Session Metadata: ${if (currentSessionDirectory != null) "ACTIVE" else "INACTIVE"}")
    }
}

fun getRecordingStatistics(): RecordingStatistics {
    val sensorStats = sensorRecorders.values.map { it.getRecordingStats() }
    val totalSamples = sensorStats.sumOf { it.totalSamplesRecorded }
    val totalStorage = sensorStats.sumOf { it.storageUsedMB }
    val totalDropped = sensorStats.sumOf { it.droppedSamples }
    val sessionDuration = if (recordingStartTime > 0) {
        (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
    } else 0.0
    return RecordingStatistics(
        isRecording = _isRecording.get(),
        sessionDurationSeconds = sessionDuration,
        activeSensors = sensorRecorders.size,
        totalSamplesRecorded = totalSamples,
        totalStorageUsedMB = totalStorage,
        totalDroppedSamples = totalDropped,
        sensorStatistics = sensorStats
    )
}

fun getAvailableSensors(): List<SensorInfo> {
    return sensorRecorders.values.map { sensor ->
        SensorInfo(
            sensorId = sensor.sensorId,
            sensorType = sensor.sensorType,
            isRecording = sensor.isRecording,
            samplingRate = sensor.samplingRate
        )
    }
}

fun getSensorStatusSummary(): SensorStatusSummary {
    val sensors = sensorRecorders.values.map { sensor ->
        DetailedSensorStatus(
            sensorId = sensor.sensorId,
            sensorType = sensor.sensorType,
            isInitialized = true,
            isRecording = sensor.isRecording,
            samplingRate = sensor.samplingRate,
            lastError = null
        )
    }
    val totalInitialized = sensors.size
    val totalRecording = sensors.count { it.isRecording }
    return SensorStatusSummary(
        totalSensorsConfigured = 3,
        totalSensorsInitialized = totalInitialized,
        totalSensorsRecording = totalRecording,
        isSessionActive = _isRecording.get(),
        sessionState = _recordingStateFlow.value,
        sensors = sensors
    )
}

fun getActiveSensorCount(): Int {
    return sensorRecorders.values.count { it.isRecording }
}

fun getCurrentSessionId(): String? = sessionMetadata?.sessionId

fun getSessionDiagnostics(): SessionDiagnostics {
    val currentTime = System.currentTimeMillis()
    val sessionDuration = if (sessionStartTimestampMs > 0) {
        currentTime - sessionStartTimestampMs
    } else 0L
    return SessionDiagnostics(
        isRecording = _isRecording.get(),
        sessionState = _recordingStateFlow.value,
        sessionDirectory = currentSessionDirectory?.rootDir?.absolutePath,
        sessionDurationMs = sessionDuration,
        sessionStartTimestamp = sessionStartTimestampMs,
        referenceTimestampNs = sessionStartTimestampNs,
        totalSensorsConfigured = 3,
        totalSensorsInitialized = sensorRecorders.size,
        totalSensorsActive = activeRecorders.size,
        activeSensorNames = activeRecorders.keys.toList(),
        availableSensorNames = sensorRecorders.keys.toList(),
        faultToleranceEnabled = true,
        partialStartCapable = true,
        midSessionRecoveryEnabled = true,
        smartCleanupEnabled = true,
        lastError = null
    )
}

fun validateSessionState(): SessionValidationResult {
    val issues = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    if (_isRecording.get() && activeRecorders.isEmpty()) {
        issues.add("Recording flag is true but no active recorders found")
    }
    if (!_isRecording.get() && activeRecorders.isNotEmpty()) {
        issues.add("Recording flag is false but active recorders exist")
    }
    if (_isRecording.get() && currentSessionDirectory == null) {
        issues.add("Recording is active but no session directory set")
    }
    if (_isRecording.get() && sessionStartTimestampMs == 0L) {
        warnings.add("Recording is active but session start timestamp not set")
    }
    val recordingSensors = sensorRecorders.values.count { it.isRecording }
    if (recordingSensors != activeRecorders.size) {
        warnings.add("Mismatch between active recorders (${activeRecorders.size}) and recording sensors ($recordingSensors)")
    }
    if (_isRecording.get() && currentSessionDirectory != null) {
        val metadataFile = File(currentSessionDirectory!!.rootDir, "session_metadata.json")
        if (!metadataFile.exists()) {
            warnings.add("Session metadata file not found")
        }
    }
    return SessionValidationResult(
        isValid = issues.isEmpty(),
        issues = issues,
        warnings = warnings,
        checkedAt = System.currentTimeMillis()
    )
}

suspend fun cleanup() {
    withContext(Dispatchers.IO) {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }
            statusMonitoringJob?.cancel()
            errorMonitoringJob?.cancel()
            val cleanupJobs = sensorRecorders.values.map { sensor ->
                async {
                    try {
                        sensor.cleanup()
                    } catch (e: Exception) {
                        mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
                    }
                }
            }
            cleanupJobs.awaitAll()
            sensorRecorders.clear()
            controllerScope.cancel()
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
        }
    }
}

private fun startMonitoring() {
    statusMonitoringJob = controllerScope.launch {
        while (isActive) {
            try {
                val statusList = sensorRecorders.values.map { sensor ->
                    sensor.getRecordingStats().let { stats ->
                        RecordingStatus(
                            sensorId = stats.sensorId,
                            sensorType = stats.sensorType,
                            isRecording = sensor.isRecording,
                            samplesRecorded = stats.totalSamplesRecorded,
                            currentDataRate = stats.averageDataRate,
                            storageUsedMB = stats.storageUsedMB,
                            timestampNs = System.nanoTime()
                        )
                    }
                }
                _sensorStatusFlow.emit(statusList)
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
            }
            delay(RecordingConstants.STATUS_UPDATE_INTERVAL_MS)
        }
    }
    errorMonitoringJob = controllerScope.launch {
        sensorRecorders.values.forEach { sensor ->
            launch {
                sensor.getErrorFlow().collect { sensorError ->
                    handleSensorError(sensor, sensorError)
                }
            }
        }
    }
}

private suspend fun handleSensorError(sensor: SensorRecorder, sensorError: SensorError) {
    val controllerError = RecordingControllerError(
        errorType = "SENSOR_ERROR",
        message = sensorError.errorMessage,
        sensorId = sensorError.sensorId,
        isRecoverable = sensorError.isRecoverable,
        originalError = sensorError
    )
    emitError(controllerError)
    if (_isRecording.get()) {
        when (sensorError.errorType) {
            ErrorType.HARDWARE_DISCONNECTED -> {
                activeRecorders.remove(sensorError.sensorId)
                if (activeRecorders.isEmpty()) {
                    emitError(
                        RecordingControllerError(
                            errorType = "ALL_SENSORS_LOST",
                            message = "All sensors have failed during recording session",
                            isRecoverable = false
                        )
                    )
                    stopSession()
                } else {
                }
            }

            ErrorType.RECORDING_FAILED -> {
                if (sensorError.isRecoverable) {
                    attemptErrorRecovery(sensor, sensorError)
                } else {
                    activeRecorders.remove(sensorError.sensorId)
                }
            }

            ErrorType.STORAGE_FULL, ErrorType.STORAGE_ERROR -> {
                emitError(
                    RecordingControllerError(
                        errorType = "SESSION_STORAGE_ERROR",
                        message = "Storage error detected: ${sensorError.errorMessage}",
                        isRecoverable = false
                    )
                )
                if (!sensorError.isRecoverable) {
                    stopSession()
                }
            }

            else -> {
                if (sensorError.isRecoverable) {
                    attemptErrorRecovery(sensor, sensorError)
                } else {
                    activeRecorders.remove(sensorError.sensorId)
                }
            }
        }
    } else if (sensorError.isRecoverable) {
        attemptErrorRecovery(sensor, sensorError)
    }
}

private suspend fun attemptErrorRecovery(sensor: SensorRecorder, error: SensorError) {
    controllerScope.launch {
        try {
            delay(RecordingConstants.ERROR_RECOVERY_DELAY_MS)
            val recoverySuccess = sensor.initialize()
            if (recoverySuccess) {
                if (_isRecording.get() && currentSessionDirectory != null) {
                    try {
                        val restartSuccess =
                            sensor.startRecording(currentSessionDirectory!!.rootDir.absolutePath)
                        if (restartSuccess) {
                            emitError(
                                RecordingControllerError(
                                    errorType = "SENSOR_RECOVERED",
                                    message = "Sensor ${sensor.sensorId} recovered and restarted",
                                    sensorId = sensor.sensorId,
                                    isRecoverable = true
                                )
                            )
                        } else {
                        }
                    } catch (e: Exception) {
                        mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
                    }
                }
            } else {
            }
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
        }
    }
}

suspend fun attemptSensorRestart(sensorId: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val sensor = sensorRecorders[sensorId]
            if (sensor == null) {
                return@withContext false
            }
            if (sensor.isRecording) {
                return@withContext true
            }
            if (!_isRecording.get() || currentSessionDirectory == null) {
                return@withContext false
            }
            val initSuccess = sensor.initialize()
            if (!initSuccess) {
                return@withContext false
            }
            val startSuccess =
                sensor.startRecording(currentSessionDirectory!!.rootDir.absolutePath)
            if (startSuccess) {
                emitError(
                    RecordingControllerError(
                        errorType = "SENSOR_MANUALLY_RESTARTED",
                        message = "Sensor $sensorId manually restarted during session",
                        sensorId = sensorId,
                        isRecoverable = true
                    )
                )
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }
}

private suspend fun emitError(error: RecordingControllerError) {
    _errorFlow.emit(error)
}

fun getStorageStatus(): StorageStatus {
    return sessionDirectoryManager.checkStorageSpace()
}

suspend fun cleanupFailedSessions(): List<String> = withContext(Dispatchers.IO) {
    sessionDirectoryManager.cleanupFailedSessions()
}

fun getCurrentSessionDirectory(): SessionDirectory? = currentSessionDirectory
fun createSynchronizedTimestamp() = timeSynchronizationService.createSynchronizedTimestamp()
fun getSessionTimestampReference() = timeSynchronizationService.getSessionReference()
suspend fun emitSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
    timeSynchronizationService.emitSyncEvent(eventType, metadata)
}

fun getTimeSynchronizationService(): TimeSynchronizationService = timeSynchronizationService
suspend fun validateTimestampConsistency(): Map<String, Long> {
    val timestamps = mutableMapOf<String, Long>()
    sensorRecorders.forEach { (sensorName, sensor) ->
        if (activeRecorders[sensorName] == true && sensor.isRecording) {
            val currentTime = TimestampManager.getCurrentTimestampNanos()
            timestamps[sensorName] = currentTime
        }
    }
    if (timestamps.size >= 2) {
        val maxTimestamp = timestamps.values.maxOrNull() ?: 0L
        val minTimestamp = timestamps.values.minOrNull() ?: 0L
        val maxDifference = maxTimestamp - minTimestamp
        timeSynchronizationService.logSyncEvent(
            "timestamp_consistency_check",
            mapOf(
                "max_difference_ns" to maxDifference.toString(),
                "sensor_count" to timestamps.size.toString(),
                "is_consistent" to (maxDifference < 5_000_000L).toString()
            )
        )
    }
    return timestamps
}

private suspend fun validateRecordingPrerequisites(enabledSensors: List<String>): ValidationResult {
    val issues = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val details = mutableMapOf<String, String>()
}")
for (sensorName in enabledSensors) {
    when (sensorName.uppercase()) {
        "RGB" -> {
            val rgbRecorder = sensorRecorders["RGB"] as? RgbCameraRecorder
            if (rgbRecorder != null) {
                details["rgb_camera"] = "available"
                if (!rgbRecorder.hasCameraPermission()) {
                    issues.add("RGB: Camera permission required")
                }
            } else {
                warnings.add("RGB: Camera recorder not initialized")
            }
        }

        "SHIMMER" -> {
            val gsrRecorder = sensorRecorders["Shimmer"] as? GSRSensorRecorder
            if (gsrRecorder != null) {
                details["gsr_devices"] = "available"
                warnings.add("GSR: Will use best available mode (hardware or simulation)")
            } else {
                warnings.add("GSR: Shimmer recorder not initialized")
            }
        }

        "THERMAL" -> {
            val thermalRecorder = sensorRecorders["Thermal"] as? ThermalCameraRecorder
            if (thermalRecorder != null) {
                try {
                    val thermalStatus = thermalRecorder.getThermalSystemStatus()
                    details["thermal_connected"] = thermalStatus.isConnected.toString()
                    details["thermal_usb_permission"] =
                        thermalStatus.hasUsbPermission.toString()
                    details["thermal_simulation"] =
                        thermalStatus.isSimulationMode.toString()
                    if (!thermalStatus.hasUsbPermission) {
                        warnings.add("Thermal: USB permission required - will use simulation")
                    }
                    if (!thermalStatus.isConnected) {
                        warnings.add("Thermal: Camera not connected - will use simulation")
                    }
                } catch (e: Exception) {
                    warnings.add("Thermal: Status unavailable")
                }
            } else {
                warnings.add("Thermal: Thermal recorder not initialized")
            }
        }
    }
}

val availableSensors = sensorRecorders.keys.size
val requestedSensors = enabledSensors.size
details["available_sensors"] = availableSensors.toString()
details["requested_sensors"] = requestedSensors.toString()
if (availableSensors == 0) {
    issues.add("System: No sensors available for recording")
}

val isValid = issues.isEmpty()
val isRecoverable = issues.all { it.contains("permission") }
return ValidationResult(
isValid = isValid,
isRecoverable = isRecoverable,
errorMessage = if (issues.isNotEmpty()) issues.joinToString("; ") else "",
warnings = warnings,
details = details
)
}

private fun estimateSessionSize(
    enabledSensors: List<String>,
    durationMinutes: Int = 10
): String {
    var estimatedMB = 0.0
    for (sensor in enabledSensors) {
        when (sensor.uppercase()) {
            "RGB" -> estimatedMB += durationMinutes * RGB_STORAGE_MB_PER_MIN
            "THERMAL" -> estimatedMB += durationMinutes * THERMAL_STORAGE_MB_PER_MIN
            "SHIMMER" -> estimatedMB += durationMinutes * SHIMMER_STORAGE_MB_PER_MIN
        }
    }
    return "${String.format("%.1f", estimatedMB)}MB (${durationMinutes}min estimate)"
}

private fun createCrashRecoveryMarker(
    sessionId: String,
    enabledSensors: List<String>,
    sessionDir: SessionDirectory
) {
    try {
        val recoveryFile = File(sessionDir.rootDir, ".recovery_marker")
        val recoveryInfo = mapOf(
            "session_id" to sessionId,
            "enabled_sensors" to enabledSensors.joinToString(","),
            "start_timestamp" to System.currentTimeMillis().toString(),
            "controller_pid" to android.os.Process.myPid().toString()
        )
        recoveryFile.writeText(recoveryInfo.entries.joinToString("\n") { "${it.key}=${it.value}" })
    } catch (e: Exception) {
        mpdc4gsr.core.utils.AppLogger.e("RecordingController", "Unexpected Exception in RecordingController catch block", e)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val isRecoverable: Boolean,
    val errorMessage: String,
    val warnings: List<String> = emptyList(),
    val details: Map<String, String> = emptyMap()
)

enum class RecordingState {
    IDLE,
    STOPPED,
    STARTING,
    RECORDING,
    STOPPING,
    ERROR
}

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

data class SessionValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String>,
    val checkedAt: Long
) {
    val hasIssues: Boolean get() = issues.isNotEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    val summary: String
        get() = when {
            isValid && !hasWarnings -> "Session state is valid"
            isValid && hasWarnings -> "Session state is valid with ${warnings.size} warnings"
            else -> "Session state has ${issues.size} issues"
        }
}

// Session orchestration helper methods
private fun transitionSessionState(from: SessionState, to: SessionState): Boolean {
    return currentSessionState.compareAndSet(from, to).also { success ->
        if (success) {
            addSessionEvent(
                "STATE_TRANSITION", metadata = mapOf(
                    "from" to from.toString(),
                    "to" to to.toString()
                )
            )
        } else {
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
        triggerSource = triggerSource,
        metadata = metadata,
        success = success,
        errorMessage = errorMessage
    )
    sessionEvents.add(event)
    " } ?: ""}")
}

// Enhanced sensor health tracking
private fun updateSensorHealth(sensorName: String, isHealthy: Boolean, error: String? = null) {
    val currentHealth = sensorHealthStatus[sensorName] ?: RecordingControllerSensorHealthInfo(
        sensorId = sensorName,
        isHealthy = true,
        lastHealthCheck = 0L,
        consecutiveFailures = 0
    )
    val updatedHealth = currentHealth.copy(
        isHealthy = isHealthy,
        lastHealthCheck = System.currentTimeMillis(),
        consecutiveFailures = if (isHealthy) 0 else currentHealth.consecutiveFailures + 1,
        lastError = error
    )
    sensorHealthStatus[sensorName] = updatedHealth
    if (!isHealthy && updatedHealth.consecutiveFailures >= 3) {
        addSessionEvent(
            "SENSOR_HEALTH_CRITICAL",
            sensorId = sensorName,
            success = false,
            errorMessage = error
        )
    }
}

// Sensor reconnection logic
private suspend fun attemptSensorReconnection(sensorName: String): Boolean {
    val currentAttempts = reconnectionAttempts[sensorName] ?: 0
    if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
        activeRecorders[sensorName] = false
        addSessionEvent("SENSOR_RECONNECTION_EXHAUSTED", sensorId = sensorName, success = false)
        return false
    }
    reconnectionAttempts[sensorName] = currentAttempts + 1
    addSessionEvent(
        "SENSOR_RECONNECTION_ATTEMPT", sensorId = sensorName, metadata = mapOf(
            "attempt" to "${currentAttempts + 1}",
            "max_attempts" to "$MAX_RECONNECTION_ATTEMPTS"
        )
    )
    val sensor = sensorRecorders[sensorName]
    if (sensor != null) {
        try {
            // Stop and clean up current state
            sensor.stopRecording()
            delay(1000)
            // Attempt reconnection based on sensor type
            val reconnectSuccess = when (sensorName.uppercase()) {
                "GSR", "SHIMMER" -> {
                    // GSR/Shimmer Bluetooth reconnection
                    try {
                        sensor.initialize()
                    } catch (e: Exception) {
                        false
                    }
                }

                "THERMAL" -> {
                    // Thermal camera USB reconnection
                    try {
                        sensor.initialize()
                    } catch (e: Exception) {
                        false
                    }
                }

                "RGB" -> {
                    // RGB camera is usually always available
                    try {
                        sensor.initialize()
                    } catch (e: Exception) {
                        false
                    }
                }

                else -> false
            }
            if (reconnectSuccess) {
                reconnectionAttempts[sensorName] = 0
                updateSensorHealth(sensorName, true)
                addSessionEvent("SENSOR_RECONNECTION_SUCCESS", sensorId = sensorName)
                return true
            } else {
                updateSensorHealth(sensorName, false, "Reconnection failed")
                addSessionEvent(
                    "SENSOR_RECONNECTION_FAILED",
                    sensorId = sensorName,
                    success = false
                )
            }
        } catch (e: Exception) {
            updateSensorHealth(sensorName, false, "Reconnection exception: ${e.message}")
            addSessionEvent(
                "SENSOR_RECONNECTION_EXCEPTION",
                sensorId = sensorName,
                success = false,
                errorMessage = e.message
            )
        }
    }
    return false
}

// Sensor health monitoring during recording
private fun startSensorHealthMonitoring() {
    statusMonitoringJob = controllerScope.launch {
        while (_isRecording.get() && isActive) {
            try {
                // Check health of all active sensors
                val activeSensorNames = activeRecorders.filter { it.value }.keys.toList()
                for (sensorName in activeSensorNames) {
                    val sensor = sensorRecorders[sensorName]
                    if (sensor != null) {
                        try {
                            // Check if sensor is still recording
                            val isStillRecording = sensor.isRecording
                            val healthInfo = sensorHealthStatus[sensorName]
                            if (!isStillRecording && activeRecorders[sensorName] == true) {
                                // Sensor stopped unexpectedly - attempt reconnection                                    updateSensorHealth(
                                sensorName,
                                false,
                                "Unexpected stop during recording"
                                )
                                addSessionEvent(
                                    "SENSOR_DROPOUT", sensorId = sensorName, success = false,
                                    errorMessage = "Sensor stopped unexpectedly"
                                )
                                // Attempt automatic reconnection
                                val reconnectSuccess = attemptSensorReconnection(sensorName)
                                if (reconnectSuccess) {
                                    // Resume recording after reconnection
                                    currentSessionDirectory?.let { sessionDir ->
                                        val sensorDir =
                                            resolveSensorDirectory(sessionDir, sensorName)
                                        sessionMetadata?.let { metadata ->
                                            val restartSuccess =
                                                sensor.startRecording(
                                                    sensorDir.absolutePath,
                                                    metadata
                                                )
                                            if (restartSuccess) {
                                                activeRecorders[sensorName] = true
                                                updateSensorHealth(sensorName, true)
                                                addSessionEvent(
                                                    "SENSOR_RESUMED",
                                                    sensorId = sensorName,
                                                    success = true
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (isStillRecording) {
                                // Sensor is healthy
                                updateSensorHealth(sensorName, true)
                            }
                        } catch (e: Exception) {
                            updateSensorHealth(
                                sensorName,
                                false,
                                "Health check exception: ${e.message}"
                            )
                        }
                    }
                }
                // Update sensor status flow for UI
                updateSensorStatusFlow()
                // Wait before next health check
                delay(RecordingConstants.STATUS_UPDATE_INTERVAL_MS)
            } catch (e: Exception) {
                delay(RecordingConstants.ERROR_RECOVERY_DELAY_MS)
            }
        }
    }
}

private fun updateSensorStatusFlow() {
    val statusList = sensorRecorders.map { (sensorName, sensor) ->
        RecordingStatus(
            sensorId = sensor.sensorId,
            sensorType = sensor.sensorType,
            isRecording = sensor.isRecording,
            samplesRecorded = 0L, // Default values for compilation
            currentDataRate = 0.0,
            storageUsedMB = 0.0,
            timestampNs = System.nanoTime()
        )
    }
    controllerScope.launch {
        _sensorStatusFlow.emit(statusList)
    }
}

// Session manifest generation
fun generateSessionManifest(): SessionManifest {
    val sessionDirectory = currentSessionDirectory?.rootDir?.name ?: "unknown"
    val startTime = sessionStartTimestampMs
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
        RecordingControllerSensorActivityInfo(
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
    val convertedSensorActivitySummary = sensorActivitySummary.mapValues { (_, info) ->
        SensorActivityInfo(
            sensorName = info.sensorName,
            wasActive = info.wasActive,
            startedSuccessfully = info.startedSuccessfully,
            finalStatus = info.finalStatus,
            errorMessages = info.errorMessages,
            dropouts = info.dropouts.map { dropout ->
                DropoutEvent(
                    sensorId = info.sensorName,
                    startTime = dropout.timestampMs,
                    endTime = dropout.durationMs?.takeIf { it > 0 }
                        ?.let { dropout.timestampMs + it },
                    reason = dropout.reason,
                    recoverable = true
                )
            },
            reconnections = info.reconnections.map { reconnection ->
                ReconnectionEvent(
                    sensorId = info.sensorName,
                    timestamp = reconnection.timestampMs,
                    successful = reconnection.successful,
                    attemptCount = reconnection.attemptNumber,
                    errorMessage = if (!reconnection.successful) "Reconnection failed" else null
                )
            }
        )
    }
    val convertedEvents = sessionEvents.map { event ->
        mpdc4gsr.feature.network.data.SessionEvent(
            eventType = event.eventType,
            timestampMs = event.timestampMs,
            sensorId = event.sensorId,
            triggerSource = convertFromRecordingControllerTriggerSource(event.triggerSource),
            metadata = event.metadata,
            success = event.success,
            errorMessage = event.errorMessage
        )
    }
    return SessionManifest(
        sessionId = sessionDirectory,
        startTime = startTime,
        stopTime = stopTime,
        duration = duration,
        triggerSource = convertFromRecordingControllerTriggerSource(lastTriggerSource)
            ?: mpdc4gsr.feature.network.data.TriggerSource.LOCAL_UI,
        sensorActivitySummary = convertedSensorActivitySummary,
        events = convertedEvents,
        errors = errors,
        warnings = warnings,
        fileReferences = emptyMap(), // Will be populated by individual recorders
        sessionState = convertFromRecordingControllerSessionState(currentSessionState.get())
    )
}

private fun convertFromRecordingControllerTriggerSource(source: RecordingController.TriggerSource?): mpdc4gsr.feature.network.data.TriggerSource? {
    return when (source) {
        RecordingController.TriggerSource.LOCAL_UI -> mpdc4gsr.feature.network.data.TriggerSource.LOCAL_UI
        RecordingController.TriggerSource.LOCAL_NOTIFICATION -> mpdc4gsr.feature.network.data.TriggerSource.LOCAL_NOTIFICATION
        RecordingController.TriggerSource.REMOTE_PC -> mpdc4gsr.feature.network.data.TriggerSource.REMOTE_PC
        RecordingController.TriggerSource.AUTOMATIC -> mpdc4gsr.feature.network.data.TriggerSource.AUTOMATIC
        RecordingController.TriggerSource.CRASH_RECOVERY -> mpdc4gsr.feature.network.data.TriggerSource.CRASH_RECOVERY
        null -> null
    }
}

private fun convertFromRecordingControllerSessionState(state: RecordingController.SessionState): mpdc4gsr.feature.network.data.SessionState {
    return when (state) {
        RecordingController.SessionState.IDLE -> mpdc4gsr.feature.network.data.SessionState.IDLE
        RecordingController.SessionState.STARTING -> mpdc4gsr.feature.network.data.SessionState.STARTING
        RecordingController.SessionState.RECORDING -> mpdc4gsr.feature.network.data.SessionState.RECORDING
        RecordingController.SessionState.STOPPING -> mpdc4gsr.feature.network.data.SessionState.STOPPING
        RecordingController.SessionState.STOPPED_COMPLETED -> mpdc4gsr.feature.network.data.SessionState.STOPPED_COMPLETED
        RecordingController.SessionState.STOPPED_FAILED -> mpdc4gsr.feature.network.data.SessionState.STOPPED_FAILED
        RecordingController.SessionState.STOPPED_INCOMPLETE -> mpdc4gsr.feature.network.data.SessionState.STOPPED_INCOMPLETE
    }
}
}

data class RecordingControllerError(
    val errorType: String,
    val message: String,
    val sensorId: String? = null,
    val isRecoverable: Boolean = true,
    val timestampNs: Long = System.nanoTime(),
    val originalError: SensorError? = null,
    val details: Map<String, String> = emptyMap()
)

data class SyncEvent(
    val markerType: String,
    val timestampNs: Long,
    val metadata: Map<String, String>,
    val successfulSensors: Int,
    val totalSensors: Int
)

data class RecordingStatistics(
    val isRecording: Boolean,
    val sessionDurationSeconds: Double,
    val activeSensors: Int,
    val totalSamplesRecorded: Long,
    val totalStorageUsedMB: Double,
    val totalDroppedSamples: Long,
    val sensorStatistics: List<RecordingStats>
)

data class SensorInfo(
    val sensorId: String,
    val sensorType: String,
    val isRecording: Boolean,
    val samplingRate: Double
)

data class DetailedSensorStatus(
    val sensorId: String,
    val sensorType: String,
    val isInitialized: Boolean,
    val isRecording: Boolean,
    val samplingRate: Double,
    val lastError: String?
)

data class SensorStatusSummary(
    val totalSensorsConfigured: Int,
    val totalSensorsInitialized: Int,
    val totalSensorsRecording: Int,
    val isSessionActive: Boolean,
    val sessionState: RecordingController.RecordingState,
    val sensors: List<DetailedSensorStatus>
) {
    val hasFailedSensors: Boolean get() = totalSensorsInitialized < totalSensorsConfigured
    val hasPartialRecording: Boolean get() = totalSensorsRecording > 0 && totalSensorsRecording < totalSensorsInitialized
    val statusMessage: String
        get() = when {
            totalSensorsRecording == totalSensorsInitialized && totalSensorsInitialized > 0 -> "All sensors recording"
            totalSensorsRecording > 0 -> "Partial recording: $totalSensorsRecording/$totalSensorsInitialized sensors active"
            totalSensorsInitialized > 0 -> "Sensors ready but not recording"
            else -> "No sensors available"
        }
}

// RecordingController-specific data classes used for manifest generation
data class RecordingControllerSensorHealthInfo(
    val sensorId: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val consecutiveFailures: Int,
    val lastError: String? = null,
    val reconnectionAttempts: Int = 0
)

data class RecordingControllerSessionEvent(
    val eventType: String,
    val timestampMs: Long,
    val sensorId: String? = null,
    val triggerSource: RecordingController.TriggerSource? = null,
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

data class RecordingControllerSessionManifest(
    val sessionId: String,
    val sessionName: String? = null,
    val startTime: Long,
    val stopTime: Long? = null,
    val duration: Long? = null,
    val triggerSource: RecordingController.TriggerSource,
    val sensorActivitySummary: Map<String, RecordingControllerSensorActivityInfo>,
    val events: List<RecordingControllerSessionEvent>,
    val errors: List<String>,
    val warnings: List<String>,
    val fileReferences: Map<String, String>,
    val sessionState: RecordingController.SessionState
)

data class RecordingControllerSensorActivityInfo(
    val sensorName: String,
    val wasActive: Boolean,
    val startedSuccessfully: Boolean,
    val framesOrSamplesCaptured: Long? = null,
    val dataSize: Long? = null,
    val dropouts: List<RecordingControllerDropoutEvent> = emptyList(),
    val reconnections: List<RecordingControllerReconnectionEvent> = emptyList(),
    val finalStatus: String,
    val errorMessages: List<String> = emptyList()
)

data class RecordingControllerDropoutEvent(
    val timestampMs: Long,
    val reason: String,
    val durationMs: Long? = null
)

data class RecordingControllerReconnectionEvent(
    val timestampMs: Long,
    val attemptNumber: Int,
    val successful: Boolean,
    val delayMs: Long
)

data class SessionDiagnostics(
    val isRecording: Boolean,
    val sessionState: RecordingController.RecordingState,
    val sessionDirectory: String?,
    val sessionDurationMs: Long,
    val sessionStartTimestamp: Long,
    val referenceTimestampNs: Long,
    val totalSensorsConfigured: Int,
    val totalSensorsInitialized: Int,
    val totalSensorsActive: Int,
    val activeSensorNames: List<String>,
    val availableSensorNames: List<String>,
    val faultToleranceEnabled: Boolean,
    val partialStartCapable: Boolean,
    val midSessionRecoveryEnabled: Boolean,
    val smartCleanupEnabled: Boolean,
    val lastError: String?
) {
    val sessionHealthScore: Double
        get() = when {
            !isRecording -> 0.0
            totalSensorsActive == 0 -> 0.0
            totalSensorsConfigured == 0 -> 1.0
            else -> totalSensorsActive.toDouble() / totalSensorsConfigured.toDouble()
        }
    val statusSummary: String
        get() = when {
            !isRecording -> "Idle"
            totalSensorsActive == totalSensorsConfigured -> "Full recording (${totalSensorsActive} sensors)"
            totalSensorsActive > 0 -> "Partial recording (${totalSensorsActive}/${totalSensorsConfigured} sensors)"
            else -> "Recording failed (no active sensors)"
        }
}
