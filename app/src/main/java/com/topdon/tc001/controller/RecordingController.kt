package com.topdon.tc001.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.*
import com.topdon.tc001.sensors.rgb.RgbCameraRecorder
import com.topdon.tc001.sensors.thermal.ThermalCameraRecorder
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central coordinator for all sensor recording in the Multi-Modal Physiological Sensing Platform.
 * 
 * This controller manages the complete sensor recording pipeline:
 * - Initialization and cleanup of all sensor recorders
 * - Synchronized start/stop across all sensors
 * - Real-time monitoring and error handling
 * - Temporal synchronization and sync marker distribution
 * - Performance monitoring and quality assurance
 * 
 * The RecordingController implements the core logic for the Android Sensor Node (Spoke)
 * in the Hub-and-Spoke architecture, coordinating multi-modal data collection.
 * 
 * @author IRCamera Android Sensor Node (Spoke)
 */
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

    // Sensor recorders
    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    
    // Recording state
    private var _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()
    
    private var currentSessionDirectory: String? = null
    private var recordingStartTime: Long = 0
    
    // Coroutine management
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statusMonitoringJob: Job? = null
    private var errorMonitoringJob: Job? = null
    
    // Data flows
    private val _recordingStateFlow = MutableStateFlow(RecordingState.STOPPED)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingStateFlow.asStateFlow()
    
    private val _sensorStatusFlow = MutableSharedFlow<List<RecordingStatus>>()
    val sensorStatusFlow: SharedFlow<List<RecordingStatus>> = _sensorStatusFlow.asSharedFlow()
    
    private val _errorFlow = MutableSharedFlow<RecordingControllerError>()
    val errorFlow: SharedFlow<RecordingControllerError> = _errorFlow.asSharedFlow()
    
    private val _syncEventFlow = MutableSharedFlow<SyncEvent>()
    val syncEventFlow: SharedFlow<SyncEvent> = _syncEventFlow.asSharedFlow()

    /**
     * Initialize all sensor recorders
     */
    suspend fun initializeSensors(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing sensor recorders")
                
                // Create sensor recorders
                val rgbCamera = RgbCameraRecorder(context, lifecycleOwner, "rgb_camera_1")
                val thermalCamera = ThermalCameraRecorder(context, "thermal_camera_1")
                val gsrSensor = GSRSensorRecorder(context, "gsr_shimmer_1")
                
                // Initialize each sensor
                val initResults = listOf(
                    "rgb_camera_1" to rgbCamera.initialize(),
                    "thermal_camera_1" to thermalCamera.initialize(),
                    "gsr_shimmer_1" to gsrSensor.initialize()
                )
                
                // Add successfully initialized sensors
                initResults.forEach { (sensorId, success) ->
                    if (success) {
                        when (sensorId) {
                            "rgb_camera_1" -> sensorRecorders[sensorId] = rgbCamera
                            "thermal_camera_1" -> sensorRecorders[sensorId] = thermalCamera
                            "gsr_shimmer_1" -> sensorRecorders[sensorId] = gsrSensor
                        }
                        Log.i(TAG, "Sensor $sensorId initialized successfully")
                    } else {
                        Log.w(TAG, "Sensor $sensorId failed to initialize")
                        emitError(RecordingControllerError(
                            errorType = "SENSOR_INIT_FAILED",
                            message = "Failed to initialize sensor: $sensorId",
                            sensorId = sensorId,
                            isRecoverable = true
                        ))
                    }
                }
                
                // Start monitoring
                startMonitoring()
                
                val successCount = sensorRecorders.size
                val totalCount = initResults.size
                
                Log.i(TAG, "Sensor initialization complete: $successCount/$totalCount sensors ready")
                
                // Return true if at least one sensor is available
                successCount > 0
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize sensors", e)
                emitError(RecordingControllerError(
                    errorType = "INIT_FAILED",
                    message = "Sensor initialization failed: ${e.message}",
                    isRecoverable = false
                ))
                false
            }
        }
    }

    /**
     * Start recording on all available sensors
     */
    suspend fun startRecording(sessionDirectory: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }
                
                Log.i(TAG, "Starting multi-modal recording")
                _recordingStateFlow.value = RecordingState.STARTING
                
                // Create session directory
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }
                
                currentSessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()
                
                // Start all sensors concurrently
                val startJobs = sensorRecorders.values.map { sensor ->
                    async {
                        val success = sensor.startRecording(sessionDirectory)
                        sensor.sensorId to success
                    }
                }
                
                val startResults = startJobs.awaitAll()
                val successfulStarts = startResults.filter { it.second }
                val failedStarts = startResults.filter { !it.second }
                
                // Log results
                successfulStarts.forEach { (sensorId, _) ->
                    Log.i(TAG, "Sensor $sensorId started successfully")
                }
                
                failedStarts.forEach { (sensorId, _) ->
                    Log.w(TAG, "Sensor $sensorId failed to start")
                    emitError(RecordingControllerError(
                        errorType = "SENSOR_START_FAILED",
                        message = "Failed to start sensor: $sensorId",
                        sensorId = sensorId,
                        isRecoverable = true
                    ))
                }
                
                if (successfulStarts.isNotEmpty()) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING
                    
                    // Add initial sync marker
                    addSyncMarker("session_start", recordingStartTime)
                    
                    Log.i(TAG, "Multi-modal recording started with ${successfulStarts.size} sensors")
                    true
                } else {
                    _recordingStateFlow.value = RecordingState.ERROR
                    Log.e(TAG, "All sensors failed to start")
                    false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                _recordingStateFlow.value = RecordingState.ERROR
                emitError(RecordingControllerError(
                    errorType = "START_FAILED",
                    message = "Failed to start recording: ${e.message}",
                    isRecoverable = true
                ))
                false
            }
        }
    }

    /**
     * Stop recording on all sensors
     */
    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    Log.w(TAG, "No recording in progress")
                    return@withContext true
                }
                
                Log.i(TAG, "Stopping multi-modal recording")
                _recordingStateFlow.value = RecordingState.STOPPING
                
                // Add final sync marker
                addSyncMarker("session_end", System.nanoTime())
                
                // Wait a moment for sync marker to propagate
                delay(SYNC_MARKER_DISTRIBUTION_DELAY_MS)
                
                // Stop all sensors concurrently
                val stopJobs = sensorRecorders.values.map { sensor ->
                    async {
                        val success = sensor.stopRecording()
                        sensor.sensorId to success
                    }
                }
                
                val stopResults = stopJobs.awaitAll()
                val successfulStops = stopResults.filter { it.second }
                val failedStops = stopResults.filter { !it.second }
                
                // Log results
                successfulStops.forEach { (sensorId, _) ->
                    Log.i(TAG, "Sensor $sensorId stopped successfully")
                }
                
                failedStops.forEach { (sensorId, _) ->
                    Log.w(TAG, "Sensor $sensorId failed to stop cleanly")
                }
                
                _isRecording.set(false)
                _recordingStateFlow.value = RecordingState.STOPPED
                
                val sessionDuration = (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                Log.i(TAG, "Multi-modal recording stopped (duration: ${sessionDuration}s)")
                
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                _recordingStateFlow.value = RecordingState.ERROR
                emitError(RecordingControllerError(
                    errorType = "STOP_FAILED",
                    message = "Failed to stop recording: ${e.message}",
                    isRecoverable = true
                ))
                false
            }
        }
    }

    /**
     * Add synchronization marker to all sensors
     */
    suspend fun addSyncMarker(markerType: String, timestampNs: Long, metadata: Map<String, String> = emptyMap()) {
        controllerScope.launch {
            try {
                Log.i(TAG, "Distributing sync marker: $markerType at $timestampNs")
                
                // Distribute sync marker to all active sensors
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
                
                // Emit sync event
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

    /**
     * Get current recording statistics for all sensors
     */
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

    /**
     * Get list of available sensors and their status
     */
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

    /**
     * Get count of currently active (recording) sensors
     */
    fun getActiveSensorCount(): Int {
        return sensorRecorders.values.count { it.isRecording }
    }

    /**
     * Clean up all resources
     */
    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Cleaning up recording controller")
                
                // Stop recording if active
                if (_isRecording.get()) {
                    stopRecording()
                }
                
                // Stop monitoring
                statusMonitoringJob?.cancel()
                errorMonitoringJob?.cancel()
                
                // Cleanup all sensors
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
                
                // Cancel controller scope
                controllerScope.cancel()
                
                Log.i(TAG, "Recording controller cleanup complete")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    private fun startMonitoring() {
        // Start status monitoring
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
        
        // Start error monitoring
        errorMonitoringJob = controllerScope.launch {
            sensorRecorders.values.forEach { sensor ->
                launch {
                    sensor.getErrorFlow().collect { sensorError ->
                        Log.w(TAG, "Sensor error: ${sensorError.sensorId} - ${sensorError.errorMessage}")
                        
                        val controllerError = RecordingControllerError(
                            errorType = "SENSOR_ERROR",
                            message = sensorError.errorMessage,
                            sensorId = sensorError.sensorId,
                            isRecoverable = sensorError.isRecoverable,
                            originalError = sensorError
                        )
                        
                        emitError(controllerError)
                        
                        // Attempt recovery for recoverable errors
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
                
                // Simple recovery: reinitialize the sensor
                val recoverySuccess = sensor.initialize()
                
                if (recoverySuccess) {
                    Log.i(TAG, "Error recovery successful for sensor ${sensor.sensorId}")
                    
                    // Restart recording if session is active
                    if (_isRecording.get() && currentSessionDirectory != null) {
                        sensor.startRecording(currentSessionDirectory!!)
                    }
                } else {
                    Log.w(TAG, "Error recovery failed for sensor ${sensor.sensorId}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during recovery attempt", e)
            }
        }
    }

    private suspend fun emitError(error: RecordingControllerError) {
        _errorFlow.emit(error)
    }
}

/**
 * Recording states for the controller
 */
enum class RecordingState {
    STOPPED,
    STARTING,
    RECORDING,
    STOPPING,
    ERROR
}

/**
 * Error information from the recording controller
 */
data class RecordingControllerError(
    val errorType: String,
    val message: String,
    val sensorId: String? = null,
    val isRecoverable: Boolean = true,
    val timestampNs: Long = System.nanoTime(),
    val originalError: SensorError? = null
)

/**
 * Synchronization event information
 */
data class SyncEvent(
    val markerType: String,
    val timestampNs: Long,
    val metadata: Map<String, String>,
    val successfulSensors: Int,
    val totalSensors: Int
)

/**
 * Overall recording statistics
 */
data class RecordingStatistics(
    val isRecording: Boolean,
    val sessionDurationSeconds: Double,
    val activeSensors: Int,
    val totalSamplesRecorded: Long,
    val totalStorageUsedMB: Double,
    val totalDroppedSamples: Long,
    val sensorStatistics: List<RecordingStats>
)

/**
 * Sensor information for UI display
 */
data class SensorInfo(
    val sensorId: String,
    val sensorType: String,
    val isRecording: Boolean,
    val samplingRate: Double
)