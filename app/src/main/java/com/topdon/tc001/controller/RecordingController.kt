package com.topdon.tc001.controller

import android.content.Context
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.rgb.RgbCameraRecorder
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.RecordingStatus
import com.topdon.tc001.sensors.SensorError
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import com.topdon.tc001.sensors.thermal.ThermalCameraRecorder
import com.topdon.tc001.util.SessionDirectoryManager
import com.topdon.tc001.util.SessionDirectory
import com.topdon.tc001.util.SessionMetadata
import com.topdon.tc001.util.StorageStatus

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
    }

    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val sessionDirectoryManager = SessionDirectoryManager(context)

    private var _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()


    private var currentSessionDirectory: SessionDirectory? = null
    private var sessionMetadata: SessionMetadata? = null
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

    suspend fun initializeSensors(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing sensor recorders with robust error handling")


                // Create a dummy TextureView for the RGB camera recorder
                val dummyTextureView = TextureView(context)
                val rgbCamera = RgbCameraRecorder(context, dummyTextureView)
                val thermalCamera = ThermalCameraRecorder(context, "thermal_camera_1")
                val gsrSensor =
                    GSRSensorRecorder(context, "gsr_shimmer_1", 128, this@RecordingController)

                val initJobs = listOf(
                    async {
                        try {
                            "rgb_camera_1" to rgbCamera.initialize()
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception initializing RGB camera", e)
                            "rgb_camera_1" to false
                        }
                    },
                    async {
                        try {
                            "thermal_camera_1" to thermalCamera.initialize()
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception initializing thermal camera", e)
                            "thermal_camera_1" to false
                        }
                    },
                    async {
                        try {
                            "gsr_shimmer_1" to gsrSensor.initialize()
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception initializing GSR sensor", e)
                            "gsr_shimmer_1" to false
                        }
                    }
                )

                val initResults = initJobs.awaitAll()

                initResults.forEach { (sensorId, success) ->
                    if (success) {
                        when (sensorId) {

                            "thermal_camera_1" -> sensorRecorders[sensorId] = thermalCamera
                            "gsr_shimmer_1" -> sensorRecorders[sensorId] = gsrSensor
                        }
                        Log.i(TAG, "Sensor $sensorId initialized successfully")
                    } else {
                        Log.w(TAG, "Sensor $sensorId failed to initialize")
                        emitError(
                            RecordingControllerError(
                                errorType = "SENSOR_INIT_FAILED",
                                message = "Failed to initialize sensor: $sensorId",
                                sensorId = sensorId,
                                isRecoverable = true
                            )
                        )
                    }
                }

                startMonitoring()

                val successCount = sensorRecorders.size
                val totalCount = initResults.size

                Log.i(
                    TAG,
                    "Sensor initialization complete: $successCount/$totalCount sensors ready"
                )

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

                // Check storage space before starting
                val storageStatus = sessionDirectoryManager.checkStorageSpace()
                if (storageStatus.isLowStorage) {
                    Log.e(TAG, "Insufficient storage space: ${storageStatus.formattedAvailable} available")
                    emitError(
                        RecordingControllerError(
                            errorType = "STORAGE_FULL",
                            message = "Insufficient storage space. Only ${storageStatus.formattedAvailable} available. Need at least 500MB free.",
                            isRecoverable = false
                        )
                    )
                    return@withContext false
                }

                if (storageStatus.shouldWarn) {
                    Log.w(TAG, "Low storage warning: ${storageStatus.formattedAvailable} available")
                    emitError(
                        RecordingControllerError(
                            errorType = "STORAGE_WARNING",
                            message = "Storage space low: ${storageStatus.formattedAvailable} available",
                            isRecoverable = true
                        )
                    )
                }

                Log.i(TAG, "Starting multi-modal recording")
                _recordingStateFlow.value = RecordingState.STARTING

                // Generate session ID and create directory structure
                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                
                // Create session metadata
                val metadata = SessionMetadata(
                    startTime = System.currentTimeMillis(),
                    enabledSensors = enabledSensors,
                    participantId = participantId,
                    studyName = studyName
                )
                sessionDirectoryManager.createSessionMetadata(sessionDir, metadata)

                currentSessionDirectory = sessionDir
                recordingStartTime = System.nanoTime()

                // Clean up any failed sessions before starting new one
                val cleanedSessions = sessionDirectoryManager.cleanupFailedSessions()
                if (cleanedSessions.isNotEmpty()) {
                    Log.i(TAG, "Cleaned up ${cleanedSessions.size} failed sessions: ${cleanedSessions.joinToString(", ")}")
                }

                val startJobs = sensorRecorders.values.map { sensor ->
                    async {
                        try {
                            val success = sensor.startRecording(sessionDir.rootDir.absolutePath)
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

                if (successfulStarts.isNotEmpty()) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING

                    addSyncMarker("session_start", recordingStartTime)

                    val totalSensors = startResults.size
                    val successCount = successfulStarts.size
                    val failedCount = failedStarts.size

                    Log.i(
                        TAG,
                        "Multi-modal recording started with $successCount/$totalSensors sensors " +
                                "(successful: ${successfulStarts.map { it.first }}, " +
                                "failed: ${failedStarts.map { it.first }})"
                    )
                    true
                } else {
                    _recordingStateFlow.value = RecordingState.ERROR
                    
                    // Update session metadata to mark as failed
                    currentSessionDirectory?.let { sessionDir ->
                        sessionDirectoryManager.updateSessionMetadata(
                            sessionDir,
                            System.currentTimeMillis(),
                            "FAILED",
                            failedStarts.associate { it.first to (it.third?.message ?: "Unknown error") }
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
                    false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
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

                Log.i(TAG, "Session created: $finalSessionId")
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
                        Log.w(TAG, "GSR sensor $sensorId failed to start$errorDetails - session will continue without GSR data")
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
                    sessionMetadata?.addSyncEvent("session_start", mapOf(
                        "total_sensors" to startResults.size.toString(),
                        "successful_sensors" to successfulStarts.size.toString(),
                        "failed_sensors" to failedStarts.size.toString()
                    ))
                    
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
                        errorType = "START_FAILED",
                        message = "Failed to start recording: ${e.message}",
                        isRecoverable = true
                    )
                )
                false
            }
        }
    }

    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    Log.w(TAG, "No recording in progress")
                    return@withContext true
                }

                Log.i(TAG, "Stopping multi-modal recording")
                _recordingStateFlow.value = RecordingState.STOPPING

                // Add session end sync marker and finalize session metadata
                sessionMetadata?.let { metadata ->
                    metadata.addSyncEvent("session_end", mapOf(
                        "recording_duration_ms" to metadata.getRelativeTimestamp().toString()
                    ))
                    sessionMetadata = metadata.markSessionEnd()
                }

                addSyncMarker("session_end", System.nanoTime())

                delay(SYNC_MARKER_DISTRIBUTION_DELAY_MS)

                val stopJobs = sensorRecorders.values.map { sensor ->
                    async {
                        try {
                            val success = sensor.stopRecording()
                            Triple(sensor.sensorId, success, null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception stopping sensor ${sensor.sensorId}", e)
                            Triple(sensor.sensorId, false, e)
                        }
                    }
                }

                val stopResults = stopJobs.awaitAll()
                val successfulStops = stopResults.filter { it.second }
                val failedStops = stopResults.filter { !it.second }

                successfulStops.forEach { (sensorId, _, _) ->
                    Log.i(TAG, "Sensor $sensorId stopped successfully")
                }

                failedStops.forEach { (sensorId, _, exception) ->
                    val errorDetails = if (exception != null) {
                        " (Exception: ${exception.message})"
                    } else {
                        " (Returned false)"
                    }
                    Log.w(TAG, "Sensor $sensorId failed to stop cleanly$errorDetails")
                }

                _isRecording.set(false)
                _recordingStateFlow.value = RecordingState.STOPPED

                val sessionDuration = (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                
                // Update session metadata with completion information
                currentSessionDirectory?.let { sessionDir ->
                    val status = if (failedStops.isEmpty()) "COMPLETED" else "COMPLETED_WITH_ERRORS"
                    val errors = failedStops.associate { 
                        it.first to (it.third?.message ?: "Failed to stop cleanly")
                    }
                    sessionDirectoryManager.updateSessionMetadata(
                        sessionDir,
                        System.currentTimeMillis(),
                        status,
                        errors
                    )
                }
                
                Log.i(TAG, "Multi-modal recording stopped (duration: ${sessionDuration}s)")


                true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
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
                        errorType = "STOP_FAILED",
                        message = "Failed to stop recording: ${e.message}",
                        isRecoverable = true
                    )
                )
                false
            }
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
            appendLine("=== Recording Controller Status ===")
            appendLine("Session State: ${summary.sessionState}")
            appendLine("Sensors: ${summary.totalSensorsRecording}/${summary.totalSensorsInitialized} recording")
            appendLine("Status: ${summary.statusMessage}")
            appendLine()
            appendLine("Individual Sensors:")
            summary.sensors.forEach { sensor ->
                val status = when {
                    sensor.isRecording -> "🔴 RECORDING"
                    sensor.isInitialized -> "🟡 READY"
                    else -> "❌ FAILED"
                }
                appendLine("  ${sensor.sensorType}: $status")
            }
            if (_isRecording.get()) {
                val stats = getRecordingStatistics()
                appendLine()
                appendLine("Session Stats:")
                appendLine("  Duration: ${String.format("%.1f", stats.sessionDurationSeconds)}s")
                appendLine("  Total Samples: ${stats.totalSamplesRecorded}")
                appendLine("  Storage Used: ${String.format("%.2f", stats.totalStorageUsedMB)}MB")
            }
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
                        Log.w(
                            TAG,
                            "Sensor error: ${sensorError.sensorId} - ${sensorError.errorMessage}"
                        )

                        val controllerError = RecordingControllerError(
                            errorType = "SENSOR_ERROR",
                            message = sensorError.errorMessage,
                            sensorId = sensorError.sensorId,
                            isRecoverable = sensorError.isRecoverable,
                            originalError = sensorError
                        )

                        emitError(controllerError)

                        if (sensorError.isRecoverable) {
                            attemptErrorRecovery(sensor, sensorError)
                        }
                    }
                }
            }
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
                            val restartSuccess = sensor.startRecording(currentSessionDirectory!!.rootDir.absolutePath)
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

                val startSuccess = sensor.startRecording(currentSessionDirectory!!.rootDir.absolutePath)
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
}
}

enum class RecordingState {
    STOPPED,
    STARTING,
    RECORDING,
    STOPPING,
    ERROR
}

data class RecordingControllerError(
    val errorType: String,
    val message: String,
    val sensorId: String? = null,
    val isRecoverable: Boolean = true,
    val timestampNs: Long = System.nanoTime(),
    val originalError: SensorError? = null
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
