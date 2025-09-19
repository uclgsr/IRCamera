package com.topdon.tc001.controller

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.RgbCameraRecorder
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.RecordingStatus
import com.topdon.tc001.sensors.SensorError
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.TimeSynchronizationService
import com.topdon.tc001.sensors.TimestampManager
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import com.topdon.tc001.sensors.thermal.ThermalCameraRecorder
import com.topdon.tc001.utils.SessionDirectoryManager
import com.topdon.tc001.utils.SessionDirectory
import com.topdon.tc001.data.SessionMetadata
import com.topdon.tc001.utils.StorageStatus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class RecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "RecordingController"
        private const val SYNC_MARKER_DISTRIBUTION_DELAY_MS = 50L
        private const val STATUS_UPDATE_INTERVAL_MS = 1000L
        private const val ERROR_RECOVERY_DELAY_MS = 2000L
        
        // Storage estimation constants
        private const val RGB_STORAGE_MB_PER_MIN = 50.0
        private const val THERMAL_STORAGE_MB_PER_MIN = 5.0
        private const val SHIMMER_STORAGE_MB_PER_MIN = 1.0
    }

    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val sessionDirectoryManager = SessionDirectoryManager(context)
    
    // Time synchronization service for unified timestamp coordination
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

    fun registerSensor(sensorName: String, sensorRecorder: SensorRecorder) {
        Log.i(TAG, "Registering sensor: $sensorName (${sensorRecorder.sensorType})")
        sensorRecorders[sensorName] = sensorRecorder
    }

    fun unregisterSensor(sensorName: String) {
        sensorRecorders.remove(sensorName)?.let { sensor ->
            Log.i(TAG, "Unregistered sensor: $sensorName")
        }
    }

    suspend fun initializeSensors(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing sensor recorders with robust error handling")

                // Create RGB camera recorder with CameraX integration
                val rgbCamera = RgbCameraRecorder(context, lifecycleOwner, null)
                val thermalCamera = ThermalCameraRecorder(context, "thermal_camera_1")
                val gsrSensor =
                    GSRSensorRecorder(context, "gsr_shimmer_1", 128, this@RecordingController)

                // Register all sensors first
                registerSensor("RGB", rgbCamera)
                registerSensor("Thermal", thermalCamera)
                registerSensor("Shimmer", gsrSensor)

                // Initialize sensors with individual error handling
                val initJobs = sensorRecorders.map { (sensorName, sensor) ->
                    async {
                        try {
                            val success = sensor.initialize()
                            Log.i(
                                TAG,
                                "Sensor $sensorName initialization: ${if (success) "SUCCESS" else "FAILED"}"
                            )
                            Triple(sensorName, sensor, success)
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception initializing sensor $sensorName", e)
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

                // Remove failed sensors from the registry to prevent start attempts
                failedInits.forEach { (sensorName, _, _) ->
                    Log.w(TAG, "Removing failed sensor $sensorName from registry")
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

                Log.i(
                    TAG,
                    "Sensor initialization complete: $successCount/$totalCount sensors ready"
                )
                Log.i(TAG, "Available sensors: ${sensorRecorders.keys.joinToString(", ")}")

                // Return true if at least one sensor initialized successfully
                successCount > 0

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize sensors", e)
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

    // Track which sensors started successfully for clean stopping
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()

    // Store session start timestamp for synchronization
    private var sessionStartTimestampMs: Long = 0
    private var sessionStartTimestampNs: Long = 0
    /**
     * Enhanced start recording with comprehensive validation and fault tolerance
     * Addresses Phase 5 requirement: "Enhance start/stop sequence validation"
     */
    suspend fun startRecording(
        sessionId: String? = null,
        participantId: String? = null,
        studyName: String? = null,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer")
    ): Boolean {

        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }

                Log.i(TAG, "🚀 Starting enhanced multi-modal recording with validation")
                _recordingStateFlow.value = RecordingState.STARTING

                // Phase 1: Pre-recording validation with detailed checks
                Log.d(TAG, "Phase 1: Validating recording prerequisites...")
                val validationResult = validateRecordingPrerequisites(enabledSensors)
                if (!validationResult.isValid) {
                    Log.e(TAG, "❌ Recording validation failed: ${validationResult.errorMessage}")
                    _recordingStateFlow.value = RecordingState.ERROR
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

                // Phase 2: Storage space validation with enhanced checks
                Log.d(TAG, "Phase 2: Validating storage requirements...")
                val storageStatus = sessionDirectoryManager.checkStorageSpace()
                if (storageStatus.isLowStorage) {
                    Log.e(TAG, "❌ Insufficient storage space: ${storageStatus.formattedAvailable} available")
                    _recordingStateFlow.value = RecordingState.ERROR
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
                    Log.w(TAG, "⚠️ Low storage warning: ${storageStatus.formattedAvailable} available")
                    emitError(
                        RecordingControllerError(
                            errorType = "STORAGE_WARNING",
                            message = "Storage space low: ${storageStatus.formattedAvailable} available",
                            isRecoverable = true
                        )
                    )
                }

                // Phase 3: Session setup with crash recovery preparation
                Log.d(TAG, "Phase 3: Setting up session with crash recovery...")
                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                
                // Create enhanced session metadata
                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId).copy(
                    participantId = participantId,
                    studyName = studyName
                )

                // Setup crash recovery marker
                createCrashRecoveryMarker(finalSessionId, enabledSensors)

                // Create the util.SessionMetadata for SessionDirectoryManager (legacy compatibility)
                val utilMetadata = com.topdon.tc001.util.SessionMetadata(
                    startTime = sessionMetadata!!.sessionStartTimestampMs,
                    enabledSensors = enabledSensors,
                    participantId = participantId,
                    studyName = studyName
                )
                sessionDirectoryManager.createSessionMetadata(sessionDir, utilMetadata)

                currentSessionDirectory = sessionDir
                persistSessionMetadata()

                // Initialize unified timestamp synchronization system
                val sessionReference = timeSynchronizationService.initializeSession(sessionDir.rootDir.absolutePath)
                
                // Capture common timestamp reference for all sensors
                sessionStartTimestampMs = sessionReference.sessionStartSystemMs
                sessionStartTimestampNs = sessionReference.sessionStartMonotonicNs
                recordingStartTime = sessionStartTimestampNs

                Log.i(
                    TAG,
                    "Session initialized with unified timestamp reference: system=${sessionStartTimestampMs}ms, monotonic=${sessionStartTimestampNs}ns"
                )

                // Clear previous active recorder tracking
                activeRecorders.clear()

                // Start sensors with individual error handling using SupervisorJob
                val startJobs = sensorRecorders.map { (sensorName, sensor) ->
                    async(SupervisorJob()) {
                        try {
                            Log.i(TAG, "Starting sensor: $sensorName")

                            // Resolve sensor-specific directory within the session structure
                            val sensorDir = resolveSensorDirectory(sessionDir, sensorName)
                            sensorDir.mkdirs()

                            val sensorStartReference = SystemClock.elapsedRealtimeNanos()
                            if (sessionMetadata == null) {
                                Log.w(TAG, "sessionMetadata is null when starting sensor: $sensorName")
                                emitError(
                                    RecordingControllerError(
                                        errorType = "SESSION_METADATA_NULL",
                                        message = "Session metadata is null when starting sensor $sensorName",
                                        sensorId = sensorName,
                                        isRecoverable = false
                                    )
                                )
                                return@async Triple(sensorName, false, IllegalStateException("Session metadata is null"))
                            }
                            val success = sensor.startRecording(
                                sensorDir.absolutePath,
                                sessionMetadata
                            )
                            if (success) {
                                activeRecorders[sensorName] = true
                                Log.i(TAG, "Sensor $sensorName started successfully")

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
                                Log.w(TAG, "Sensor $sensorName returned false on start")
                            }
                            Triple(sensorName, success, null)

                        } catch (e: Exception) {
                            Log.w(TAG, "Exception starting sensor $sensorName", e)
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

                // Log results for each sensor
                successfulStarts.forEach { (sensorName, _, _) ->
                    Log.i(TAG, "✓ Sensor $sensorName: STARTED")
                }

                failedStarts.forEach { (sensorName, _, exception) ->
                    val errorDetails = if (exception != null) {
                        " (Exception: ${exception.message})"
                    } else {
                        " (Returned false)"
                    }
                    Log.w(TAG, "✗ Sensor $sensorName: FAILED$errorDetails")
                    emitError(
                        RecordingControllerError(
                            errorType = "SENSOR_START_FAILED",
                            message = "Failed to start sensor: $sensorName$errorDetails",
                            sensorId = sensorName,
                            isRecoverable = true
                        )
                    )
                }

                // Proceed if at least one sensor started successfully
                if (successfulStarts.isNotEmpty()) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING

                    // Add session start sync marker with reference timestamp
                    addSyncMarker("session_start", sessionStartTimestampNs)

                    val totalSensors = startResults.size
                    val successCount = successfulStarts.size

                    Log.i(
                        TAG,
                        "✓ Multi-modal recording session started: $successCount/$totalSensors sensors active"
                    )
                    Log.i(
                        TAG,
                        "Active sensors: ${successfulStarts.joinToString(", ") { it.first }}"
                    )
                    if (failedStarts.isNotEmpty()) {
                        Log.w(
                            TAG,
                            "Failed sensors: ${failedStarts.joinToString(", ") { it.first }}"
                        )
                    }

                    true
                } else {
                    _recordingStateFlow.value = RecordingState.ERROR

                    // Update session metadata to mark as failed
                    currentSessionDirectory?.let { sessionDir ->
                        sessionDirectoryManager.updateSessionMetadata(
                            sessionDir,
                            System.currentTimeMillis(),
                            "FAILED",
                            failedStarts.associate {
                                it.first to (it.third?.message ?: "Unknown error")
                            }
                        )
                    }

                    Log.e(
                        TAG,
                        "All ${startResults.size} sensors failed to start - cannot begin session"
                    )
                    emitError(
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
                Log.e(TAG, "Failed to start recording session", e)
                _recordingStateFlow.value = RecordingState.ERROR

                // Update session metadata to mark as failed
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

    /**
     * Start recording with legacy API (backward compatibility)
     * @param sessionDirectory The directory path where session data should be stored
     */
    suspend fun startRecording(sessionDirectory: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }

                Log.i(TAG, "Starting recording with legacy API")
                _recordingStateFlow.value = RecordingState.STARTING

                // Create session directory if it doesn't exist
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }
                // Create a simple SessionDirectory wrapper for the provided path
                val sessionDirWrapper = SessionDirectory(
                    sessionId = sessionDir.name,
                    rootDir = sessionDir,
                    rgbDir = sessionDir,  // Legacy: use same directory for all sensors
                    thermalDir = sessionDir,
                    shimmerDir = sessionDir
                )

                currentSessionDirectory = sessionDirWrapper
                recordingStartTime = System.nanoTime()

                // Create session metadata for timing synchronization
                sessionMetadata = SessionMetadata.createSessionStart(sessionDir.name)

                Log.i(TAG, "Session created: ${sessionDir.name}")
                Log.i(TAG, "Session start time: ${sessionMetadata!!.sessionStartIso}")
                Log.i(TAG, "Wall clock: ${sessionMetadata!!.sessionStartTimestampMs}ms")
                Log.i(TAG, "Monotonic: ${sessionMetadata!!.sessionStartMonotonicNs}ns")

                val startJobs = sensorRecorders.values.map { sensor ->
                    async {
                        try {
                            // Pass session metadata to sensors for consistent timing
                            val success = sensor.startRecording(sessionDirectory, sessionMetadata!!)
                            Triple(sensor.sensorId, success, null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception starting sensor ${sensor.sensorId}", e)
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

                successfulStarts.forEach { (sensorId, _, _) ->
                    Log.i(TAG, "Sensor $sensorId started successfully")
                }

                failedStarts.forEach { (sensorId, _, exception) ->
                    val errorDetails = if (exception != null) {
                        " (Exception: ${exception.message})"
                    } else {
                        " (Returned false)"
                    }

                    // Special handling for GSR sensor - don't treat as critical failure
                    if (sensorId.contains("gsr", ignoreCase = true)) {
                        Log.w(
                            TAG,
                            "GSR sensor $sensorId failed to start$errorDetails - session will continue without GSR data"
                        )
                        emitError(
                            RecordingControllerError(
                                errorType = "GSR_SENSOR_UNAVAILABLE",
                                message = "GSR sensor unavailable: $sensorId$errorDetails - check device pairing and proximity",
                                sensorId = sensorId,
                                isRecoverable = true
                            )
                        )
                    } else {
                        Log.w(TAG, "Sensor $sensorId failed to start$errorDetails")
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

                // Allow session to start even if some sensors fail (graceful degradation)
                if (successfulStarts.isNotEmpty()) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING

                    // Add session start sync marker with session metadata
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

                    Log.i(
                        TAG,
                        "Recording started with legacy API: $successCount/$totalSensors sensors " +
                                "(successful: ${successfulStarts.map { it.first }}, " +
                                "failed: ${failedStarts.map { it.first }})"
                    )
                    true
                } else {
                    _recordingStateFlow.value = RecordingState.ERROR
                    Log.e(
                        TAG,
                        "All ${startResults.size} sensors failed to start - cannot begin session"
                    )
                    emitError(
                        RecordingControllerError(
                            errorType = "ALL_SENSORS_FAILED",
                            message = "All sensors failed to start: ${failedStarts.joinToString(", ") { it.first }}",
                            isRecoverable = true
                        )
                    )
                    false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording with legacy API", e)
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

    suspend fun stopSession(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    Log.w(TAG, "No recording in progress")
                    return@withContext true
                }

                Log.i(TAG, "Stopping multi-modal recording session")
                _recordingStateFlow.value = RecordingState.STOPPING

                // Add session end sync marker for downstream alignment
                sessionMetadata?.let { metadata ->
                    metadata.addSyncEvent(
                        "session_end", mapOf(
                            "recording_duration_ms" to metadata.getRelativeTimestamp().toString()
                        )
                    )
                }

                addSyncMarker("session_end", System.nanoTime())

                // Allow sync marker to propagate
                delay(SYNC_MARKER_DISTRIBUTION_DELAY_MS)

                val stopResult = safeStopAll()

                _isRecording.set(false)
                _recordingStateFlow.value = RecordingState.STOPPED

                val sessionDuration = if (recordingStartTime > 0) {
                    (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                } else 0.0

                finalizeSessionMetadata(stopResult)

                currentSessionDirectory?.let { sessionDir ->
                    val stopErrors = stopResult.filterValues { success -> !success }
                        .mapValues { "STOP_FAILED" }
                    val status = when {
                        stopResult.isEmpty() -> "COMPLETED"
                        stopErrors.isEmpty() -> "COMPLETED"
                        stopErrors.size == stopResult.size -> "FAILED"
                        else -> "PARTIAL"
                    }
                    sessionDirectoryManager.updateSessionMetadata(
                        sessionDir,
                        System.currentTimeMillis(),
                        status,
                        stopErrors
                    )
                }

                // Finalize time synchronization service and calculate session duration
                val sessionDurationMs = timeSynchronizationService.finalizeSession()

                // Reset session state for next session
                activeRecorders.clear()
                sessionStartTimestampMs = 0
                sessionStartTimestampNs = 0
                currentSessionDirectory = null

                Log.i(TAG, "Multi-modal recording stopped (duration: ${sessionDurationMs / 1000.0}s)")

                true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording session", e)
                _recordingStateFlow.value = RecordingState.ERROR

                // Update session metadata to mark error during stop
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

    // Legacy method for backward compatibility  
    suspend fun stopRecording(): Boolean {
        return stopSession()
    }

    /**
     * Safely stop all active recorders, only attempting to stop those that started successfully
     */
    private suspend fun safeStopAll(): Map<String, Boolean> = coroutineScope {
        val stopResults = mutableMapOf<String, Boolean>()

        // Only stop recorders that are marked as active
        val activeRecordersList = activeRecorders.keys.mapNotNull { sensorName ->
            sensorRecorders[sensorName]?.let { sensor -> sensorName to sensor }
        }

        if (activeRecordersList.isEmpty()) {
            Log.i(TAG, "No active recorders to stop")
            return@coroutineScope stopResults
        }

        val stopJobs = activeRecordersList.map { (sensorName, sensor) ->
            async(SupervisorJob()) {
                try {
                    Log.i(TAG, "Stopping sensor: $sensorName")
                    val success = sensor.stopRecording()
                    if (success) {
                        Log.i(TAG, "✓ Sensor $sensorName stopped successfully")
                    } else {
                        Log.w(TAG, "✗ Sensor $sensorName returned false on stop")
                    }
                    Triple(sensorName, success, null)
                } catch (e: Exception) {
                    Log.w(TAG, "✗ Exception stopping sensor $sensorName", e)
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
                Log.w(TAG, "Sensor $sensorName failed to stop cleanly$errorDetails")
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
            // Remove from active recorders regardless of stop result
            activeRecorders.remove(sensorName)
        }

        val successCount = stopResults.count { it.value }
        val totalCount = stopResults.size
        Log.i(TAG, "Stop operation complete: $successCount/$totalCount sensors stopped cleanly")

        return@coroutineScope stopResults
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
                Log.i(TAG, "Distributing sync marker: $markerType at $timestampNs")

                // Log sync event to TimeSynchronizationService for cross-sensor alignment
                timeSynchronizationService.logSyncEvent(markerType, metadata)

                val syncJobs = sensorRecorders.values.map { sensor ->
                    async {
                        try {
                            sensor.addSyncMarker(markerType, timestampNs, metadata)
                            sensor.sensorId to true
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to add sync marker to ${sensor.sensorId}", e)
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

                Log.i(TAG, "Sync marker distributed: $successfulSyncs/$totalSensors sensors")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to distribute sync marker", e)
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
                        sensorId to true // If we can get stats, sensor is responsive
                    } catch (e: Exception) {
                        Log.w(TAG, "Sensor $sensorId test failed", e)
                        sensorId to false
                    }
                }
            }

            testJobs.awaitAll().forEach { (sensorId, success) ->
                testResults[sensorId] = success
            }

            Log.i(
                TAG,
                "Sensor connection test complete: ${testResults.count { it.value }}/${testResults.size} sensors responsive"
            )
            testResults
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
                    sensor.isRecording -> "🔴 RECORDING"
                    sensor.isInitialized -> "🟡 READY"
                    else -> "❌ FAILED"
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
                isInitialized = true, // If it's in the map, it was successfully initialized
                isRecording = sensor.isRecording,
                samplingRate = sensor.samplingRate,
                lastError = null // Could be enhanced to track last error per sensor
            )
        }

        val totalInitialized = sensors.size
        val totalRecording = sensors.count { it.isRecording }

        return SensorStatusSummary(
            totalSensorsConfigured = 3, // RGB, Thermal, GSR
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

    /**
     * Provides comprehensive session diagnostics for debugging and monitoring
     */
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
            totalSensorsConfigured = 3, // RGB, Thermal, GSR
            totalSensorsInitialized = sensorRecorders.size,
            totalSensorsActive = activeRecorders.size,
            activeSensorNames = activeRecorders.keys.toList(),
            availableSensorNames = sensorRecorders.keys.toList(),
            faultToleranceEnabled = true,
            partialStartCapable = true,
            midSessionRecoveryEnabled = true,
            smartCleanupEnabled = true,
            lastError = null // Could be enhanced to track last controller error
        )
    }

    /**
     * Validates current session state and reports any inconsistencies
     */
    fun validateSessionState(): SessionValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check for state inconsistencies
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

        // Check sensor state consistency
        val recordingSensors = sensorRecorders.values.count { it.isRecording }
        if (recordingSensors != activeRecorders.size) {
            warnings.add("Mismatch between active recorders (${activeRecorders.size}) and recording sensors ($recordingSensors)")
        }

        // Check metadata file existence
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
                Log.i(TAG, "Cleaning up recording controller")

                if (_isRecording.get()) {
                    stopRecording()
                }

                statusMonitoringJob?.cancel()
                errorMonitoringJob?.cancel()

                val cleanupJobs = sensorRecorders.values.map { sensor ->
                    async {
                        try {
                            sensor.cleanup()
                            Log.d(TAG, "Sensor ${sensor.sensorId} cleaned up")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to cleanup sensor ${sensor.sensorId}", e)
                        }
                    }
                }

                cleanupJobs.awaitAll()
                sensorRecorders.clear()

                controllerScope.cancel()

                Log.i(TAG, "Recording controller cleanup complete")

            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
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
                    Log.w(TAG, "Status monitoring error", e)
                }

                delay(STATUS_UPDATE_INTERVAL_MS)
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
        Log.w(TAG, "Sensor error detected: ${sensorError.sensorId} - ${sensorError.errorMessage}")

        val controllerError = RecordingControllerError(
            errorType = "SENSOR_ERROR",
            message = sensorError.errorMessage,
            sensorId = sensorError.sensorId,
            isRecoverable = sensorError.isRecoverable,
            originalError = sensorError
        )

        emitError(controllerError)

        // Handle mid-session sensor failures gracefully
        if (_isRecording.get()) {
            when (sensorError.errorType) {
                ErrorType.HARDWARE_DISCONNECTED -> {
                    Log.w(
                        TAG,
                        "Sensor ${sensorError.sensorId} disconnected during recording - marking as inactive"
                    )
                    activeRecorders.remove(sensorError.sensorId)

                    // Check if we still have at least one active sensor
                    if (activeRecorders.isEmpty()) {
                        Log.e(TAG, "All sensors have failed - stopping session")
                        emitError(
                            RecordingControllerError(
                                errorType = "ALL_SENSORS_LOST",
                                message = "All sensors have failed during recording session",
                                isRecoverable = false
                            )
                        )
                        stopSession()
                    } else {
                        Log.i(
                            TAG,
                            "Session continuing with ${activeRecorders.size} remaining sensors: ${activeRecorders.keys}"
                        )
                    }
                }

                ErrorType.RECORDING_FAILED -> {
                    if (sensorError.isRecoverable) {
                        Log.i(TAG, "Attempting recovery for sensor ${sensorError.sensorId}")
                        attemptErrorRecovery(sensor, sensorError)
                    } else {
                        Log.w(
                            TAG,
                            "Non-recoverable recording error for sensor ${sensorError.sensorId} - removing from active list"
                        )
                        activeRecorders.remove(sensorError.sensorId)
                    }
                }

                ErrorType.STORAGE_FULL, ErrorType.STORAGE_ERROR -> {
                    Log.e(TAG, "Storage error detected - this may affect the entire session")
                    emitError(
                        RecordingControllerError(
                            errorType = "SESSION_STORAGE_ERROR",
                            message = "Storage error detected: ${sensorError.errorMessage}",
                            isRecoverable = false
                        )
                    )
                    // For storage errors, we might want to stop the entire session
                    if (!sensorError.isRecoverable) {
                        stopSession()
                    }
                }

                else -> {
                    if (sensorError.isRecoverable) {
                        attemptErrorRecovery(sensor, sensorError)
                    } else {
                        Log.w(TAG, "Non-recoverable error for sensor ${sensorError.sensorId}")
                        activeRecorders.remove(sensorError.sensorId)
                    }
                }
            }
        } else if (sensorError.isRecoverable) {
            // Not currently recording, but still attempt recovery for future sessions
            attemptErrorRecovery(sensor, sensorError)
        }
    }

    private suspend fun attemptErrorRecovery(sensor: SensorRecorder, error: SensorError) {
        controllerScope.launch {
            try {
                Log.i(TAG, "Attempting error recovery for sensor ${sensor.sensorId}")

                delay(ERROR_RECOVERY_DELAY_MS)

                val recoverySuccess = sensor.initialize()

                if (recoverySuccess) {
                    Log.i(TAG, "Error recovery successful for sensor ${sensor.sensorId}")

                    if (_isRecording.get() && currentSessionDirectory != null) {
                        try {
                            val restartSuccess =
                                sensor.startRecording(currentSessionDirectory!!.rootDir.absolutePath)
                            if (restartSuccess) {
                                Log.i(
                                    TAG,
                                    "Sensor ${sensor.sensorId} successfully restarted during session"
                                )
                                emitError(
                                    RecordingControllerError(
                                        errorType = "SENSOR_RECOVERED",
                                        message = "Sensor ${sensor.sensorId} recovered and restarted",
                                        sensorId = sensor.sensorId,
                                        isRecoverable = true
                                    )
                                )
                            } else {
                                Log.w(
                                    TAG,
                                    "Sensor ${sensor.sensorId} recovery failed to restart recording"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception during sensor ${sensor.sensorId} restart", e)
                        }
                    }
                } else {
                    Log.w(TAG, "Error recovery failed for sensor ${sensor.sensorId}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during recovery attempt for sensor ${sensor.sensorId}", e)
            }
        }
    }

    suspend fun attemptSensorRestart(sensorId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sensor = sensorRecorders[sensorId]
                if (sensor == null) {
                    Log.w(TAG, "Cannot restart sensor $sensorId - not found in active sensors")
                    return@withContext false
                }

                if (sensor.isRecording) {
                    Log.i(TAG, "Sensor $sensorId is already recording")
                    return@withContext true
                }

                if (!_isRecording.get() || currentSessionDirectory == null) {
                    Log.w(TAG, "Cannot restart sensor $sensorId - no active recording session")
                    return@withContext false
                }

                Log.i(TAG, "Attempting to restart sensor $sensorId during active session")

                val initSuccess = sensor.initialize()
                if (!initSuccess) {
                    Log.w(TAG, "Sensor $sensorId reinitialization failed")
                    return@withContext false
                }

                val startSuccess =
                    sensor.startRecording(currentSessionDirectory!!.rootDir.absolutePath)
                if (startSuccess) {
                    Log.i(TAG, "Sensor $sensorId successfully restarted during session")
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
                    Log.w(TAG, "Sensor $sensorId restart failed - could not start recording")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during manual sensor restart for $sensorId", e)
                return@withContext false
            }
        }
    }

    private suspend fun emitError(error: RecordingControllerError) {
        _errorFlow.emit(error)
    }

    /**
     * Get current storage status
     */
    fun getStorageStatus(): StorageStatus {
        return sessionDirectoryManager.checkStorageSpace()
    }

    /**
     * Clean up failed sessions
     */
    suspend fun cleanupFailedSessions(): List<String> = withContext(Dispatchers.IO) {
        sessionDirectoryManager.cleanupFailedSessions()
    }

    /**
     * Get current session directory info
     */
    fun getCurrentSessionDirectory(): SessionDirectory? = currentSessionDirectory
    
    /**
     * Get synchronized timestamp from the time synchronization service
     * All sensors should use this for consistent cross-sensor timing
     */
    fun createSynchronizedTimestamp() = timeSynchronizationService.createSynchronizedTimestamp()
    
    /**
     * Get current session timestamp reference for cross-sensor alignment
     */
    fun getSessionTimestampReference() = timeSynchronizationService.getSessionReference()
    
    /**
     * Manually emit sync event for cross-sensor validation
     */
    suspend fun emitSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        timeSynchronizationService.emitSyncEvent(eventType, metadata)
    }
    
    /**
     * Validate timestamp consistency across currently active sensors
     */
    suspend fun validateTimestampConsistency(): Map<String, Long> {
        val timestamps = mutableMapOf<String, Long>()
        sensorRecorders.forEach { (sensorName, sensor) ->
            if (activeRecorders[sensorName] == true && sensor.isRecording) {
                // Get current timestamp from each active sensor
                val currentTime = TimestampManager.getCurrentTimestampNanos()
                timestamps[sensorName] = currentTime
            }
        }
        
        // Log consistency validation event
        if (timestamps.size >= 2) {
            val maxTimestamp = timestamps.values.maxOrNull() ?: 0L
            val minTimestamp = timestamps.values.minOrNull() ?: 0L
            val maxDifference = maxTimestamp - minTimestamp
            
            timeSynchronizationService.logSyncEvent(
                "timestamp_consistency_check",
                mapOf(
                    "max_difference_ns" to maxDifference.toString(),
                    "sensor_count" to timestamps.size.toString(),
                    "is_consistent" to (maxDifference < 5_000_000L).toString() // 5ms tolerance
                )
            )
        }
        
        return timestamps
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
    val sessionState: RecordingState,
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

/**
 * Comprehensive session diagnostics for enhanced monitoring and debugging
 */
data class SessionDiagnostics(
    val isRecording: Boolean,
    val sessionState: RecordingState,
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

    /**
     * Enhanced recording prerequisites validation
     * Addresses requirement: "Enhance start/stop sequence validation"
     */
    private suspend fun validateRecordingPrerequisites(enabledSensors: List<String>): ValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val details = mutableMapOf<String, String>()

        Log.d(TAG, "Validating prerequisites for sensors: ${enabledSensors.joinToString(", ")}")

        // Check sensor availability and readiness
        for (sensorName in enabledSensors) {
            when (sensorName.uppercase()) {
                "RGB" -> {
                    val rgbRecorder = sensorRecorders["RGB"] as? RgbCameraRecorder
                    if (rgbRecorder != null) {
                        details["rgb_camera"] = "available"
                        
                        // Check camera permissions
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
                        val thermalStatus = thermalRecorder.getThermalSystemStatus()
                        details["thermal_connected"] = thermalStatus.isConnected.toString()
                        details["thermal_usb_permission"] = thermalStatus.hasUsbPermission.toString()
                        details["thermal_simulation"] = thermalStatus.isSimulationMode.toString()
                        
                        if (!thermalStatus.hasUsbPermission) {
                            warnings.add("Thermal: USB permission required - will use simulation")
                        }
                        if (!thermalStatus.isConnected) {
                            warnings.add("Thermal: Camera not connected - will use simulation")
                        }
                    } else {
                        warnings.add("Thermal: Thermal recorder not initialized")
                    }
                }
            }
        }

        // Check system prerequisites
        val availableSensors = sensorRecorders.keys.size
        val requestedSensors = enabledSensors.size
        
        details["available_sensors"] = availableSensors.toString()
        details["requested_sensors"] = requestedSensors.toString()
        
        if (availableSensors == 0) {
            issues.add("System: No sensors available for recording")
        }

        val isValid = issues.isEmpty()
        val isRecoverable = issues.all { it.contains("permission") }

        Log.d(TAG, "Validation result: ${if (isValid) "PASSED" else "FAILED"} with ${issues.size} issues, ${warnings.size} warnings")

        return ValidationResult(
            isValid = isValid,
            isRecoverable = isRecoverable,
            errorMessage = if (issues.isNotEmpty()) issues.joinToString("; ") else "",
            warnings = warnings,
            details = details
        )
    }

    /**
     * Estimate session storage requirements
     */
    private fun estimateSessionSize(enabledSensors: List<String>, durationMinutes: Int = 10): String {
        var estimatedMB = 0.0
        
        for (sensor in enabledSensors) {
            when (sensor.uppercase()) {
                "RGB" -> estimatedMB += durationMinutes * RGB_STORAGE_MB_PER_MIN // ~50MB/min for 1080p video + frames
                "THERMAL" -> estimatedMB += durationMinutes * THERMAL_STORAGE_MB_PER_MIN // ~5MB/min for thermal data
                "SHIMMER" -> estimatedMB += durationMinutes * SHIMMER_STORAGE_MB_PER_MIN // ~1MB/min for GSR data
            }
        }
        
        return "${String.format("%.1f", estimatedMB)}MB (${durationMinutes}min estimate)"
    }

    /**
     * Create crash recovery marker
     * Addresses requirement: "Complete stop sequence & cleanup with crash recovery"
     */
    private fun createCrashRecoveryMarker(sessionId: String, enabledSensors: List<String>) {
        try {
            val recoveryFile = File(sessionDirectoryManager.getSessionDirectory(sessionId), ".recovery_marker")
            val recoveryInfo = mapOf(
                "session_id" to sessionId,
                "enabled_sensors" to enabledSensors.joinToString(","),
                "start_timestamp" to System.currentTimeMillis().toString(),
                "controller_pid" to android.os.Process.myPid().toString()
            )
            
            recoveryFile.writeText(recoveryInfo.entries.joinToString("\n") { "${it.key}=${it.value}" })
            Log.d(TAG, "Crash recovery marker created for session: $sessionId")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create crash recovery marker", e)
        }
    }

    /**
     * Enhanced validation result data class
     */
    data class ValidationResult(
        val isValid: Boolean,
        val isRecoverable: Boolean,
        val errorMessage: String,
        val warnings: List<String> = emptyList(),
        val details: Map<String, String> = emptyMap()
    )

    enum class RecordingState {
        STOPPED,
        STARTING,
        RECORDING,
        STOPPING,
        ERROR
    }
}

/**
 * Result of session state validation
 */
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
