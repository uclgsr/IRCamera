package com.topdon.tc001.camera

import android.content.Context
import android.util.Log
import android.view.TextureView
import com.topdon.tc001.camera.ui.SensorSelectionDialog
import com.topdon.tc001.gsr.EnhancedThermalRecorder
import com.topdon.gsr.util.TimeUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Advanced Multi-Modal Recorder with True Parallel Recording
 * 
 * Features:
 * - Truly parallel recording starts for all selected sensors
 * - Flexible sensor combination selection (any combination of Thermal/RGB/GSR)
 * - Samsung S22 ground truth timing with sub-millisecond precision
 * - Comprehensive error handling and sensor availability detection
 * - Research-grade synchronization with unified timestamps
 */
class ParallelMultiModalRecorder(
    private val context: Context,
    private val thermalRecorder: EnhancedThermalRecorder,
    private val rgbTextureView: TextureView
) {
    companion object {
        private const val TAG = "ParallelRecorder"
    }

    // Recording components
    private var rgbCameraRecorder: RGBCameraRecorder? = null
    private val recordingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Recording state
    private var currentSessionId: String? = null
    private val isRecording = AtomicBoolean(false)
    private val synchronizedStartTime = AtomicLong(0)
    private var selectedSensors: Set<SensorSelectionDialog.SensorType> = emptySet()
    
    // Recording results
    data class ParallelRecordingSession(
        val sessionId: String,
        val selectedSensors: Set<SensorSelectionDialog.SensorType>,
        val startTimestamp: Long,
        val endTimestamp: Long? = null,
        val thermalVideoFile: File? = null,
        val rgbVideoFile: File? = null,
        val gsrDataFile: File? = null,
        val syncMarksFile: File? = null,
        val sessionMetadata: File? = null,
        val recordingDuration: Long = 0,
        val sensorStatus: Map<SensorSelectionDialog.SensorType, String> = emptyMap()
    )

    // Event callbacks
    var onRecordingStarted: ((ParallelRecordingSession) -> Unit)? = null
    var onRecordingStopped: ((ParallelRecordingSession) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSensorStatusChanged: ((SensorSelectionDialog.SensorType, String) -> Unit)? = null

    /**
     * Initialize all recording components
     */
    fun initialize() {
        rgbCameraRecorder = RGBCameraRecorder(context, rgbTextureView).apply {
            initialize()
            
            onRecordingStarted = {
                onSensorStatusChanged?.invoke(SensorSelectionDialog.SensorType.RGB, "Recording")
                Log.d(TAG, "RGB recording started in parallel session")
            }
            
            onRecordingStopped = { videoFile ->
                onSensorStatusChanged?.invoke(SensorSelectionDialog.SensorType.RGB, "Completed")
                Log.d(TAG, "RGB recording stopped: ${videoFile?.absolutePath}")
            }
            
            onError = { error ->
                onSensorStatusChanged?.invoke(SensorSelectionDialog.SensorType.RGB, "Error: $error")
                Log.e(TAG, "RGB camera error in parallel session: $error")
            }
        }
        
        Log.i(TAG, "Parallel multi-modal recorder initialized")
    }

    /**
     * Start parallel multi-modal recording with selected sensors
     */
    fun startParallelRecording(
        selectedSensors: Set<SensorSelectionDialog.SensorType>,
        sessionId: String? = null,
        rgbSettings: RGBCameraRecorder.RecordingSettings = RGBCameraRecorder.RecordingSettings()
    ): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording")
            return false
        }

        if (selectedSensors.isEmpty()) {
            Log.w(TAG, "No sensors selected for recording")
            onError?.invoke("No sensors selected for recording")
            return false
        }

        try {
            // Generate unified session with Samsung S22 ground truth timing
            val unifiedSessionId = sessionId ?: TimeUtil.generateSessionId("Parallel")
            val synchronizedTimestamp = TimeUtil.getSynchronizedTimestamp()
            
            currentSessionId = unifiedSessionId
            this.selectedSensors = selectedSensors
            synchronizedStartTime.set(synchronizedTimestamp)
            
            Log.i(TAG, "Starting parallel recording with sensors: $selectedSensors")
            Log.i(TAG, "Unified session ID: $unifiedSessionId, Ground truth timestamp: $synchronizedTimestamp")

            // Start parallel recording using coroutines for true simultaneity
            recordingScope.launch {
                val startJobs = mutableListOf<Deferred<Pair<SensorSelectionDialog.SensorType, Boolean>>>()
                
                // Launch all selected sensors in parallel
                selectedSensors.forEach { sensor ->
                    val job = async {
                        when (sensor) {
                            SensorSelectionDialog.SensorType.THERMAL -> {
                                // Thermal recording via GSR recorder (includes thermal support)
                                val success = thermalRecorder.startRecording(unifiedSessionId, null, true)
                                if (success) {
                                    // Add initial sync marker with precise timing
                                    delay(50) // Small delay to ensure recording is active
                                    thermalRecorder.triggerSyncEvent("PARALLEL_THERMAL_START", mapOf(
                                        "sync_timestamp" to synchronizedTimestamp.toString(),
                                        "selected_sensors" to selectedSensors.map { it.displayName }.joinToString(","),
                                        "recording_mode" to "parallel_multimodal"
                                    ))
                                }
                                Pair(sensor, success)
                            }
                            
                            SensorSelectionDialog.SensorType.RGB -> {
                                // RGB camera recording
                                rgbCameraRecorder?.updateSettings(rgbSettings)
                                val success = rgbCameraRecorder?.startRecording(unifiedSessionId) ?: false
                                Pair(sensor, success)
                            }
                            
                            SensorSelectionDialog.SensorType.GSR -> {
                                // GSR recording (handled by thermal recorder)
                                // This is already covered by THERMAL sensor
                                Pair(sensor, selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL))
                            }
                        }
                    }
                    startJobs.add(job)
                }
                
                // Wait for all sensors to start and collect results
                val results = startJobs.awaitAll()
                val failedSensors = results.filter { !it.second }.map { it.first }
                val successfulSensors = results.filter { it.second }.map { it.first }.toSet()
                
                withContext(Dispatchers.Main) {
                    if (successfulSensors.isEmpty()) {
                        // All sensors failed to start
                        Log.e(TAG, "All sensors failed to start: $failedSensors")
                        onError?.invoke("Failed to start any sensors: ${failedSensors.map { it.displayName }.joinToString(", ")}")
                        cleanup()
                        return@withContext
                    }
                    
                    if (failedSensors.isNotEmpty()) {
                        // Some sensors failed, but continue with successful ones
                        Log.w(TAG, "Some sensors failed to start: $failedSensors")
                        Log.i(TAG, "Continuing with successful sensors: $successfulSensors")
                    }
                    
                    isRecording.set(true)
                    
                    // Update sensor status
                    successfulSensors.forEach { sensor ->
                        onSensorStatusChanged?.invoke(sensor, "Recording")
                    }
                    failedSensors.forEach { sensor ->
                        onSensorStatusChanged?.invoke(sensor, "Failed")
                    }
                    
                    // Add comprehensive sync event marking parallel start completion
                    if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
                        thermalRecorder.triggerSyncEvent("PARALLEL_RECORDING_STARTED", mapOf(
                            "sync_timestamp" to synchronizedTimestamp.toString(),
                            "selected_sensors" to selectedSensors.map { it.displayName }.joinToString(","),
                            "successful_sensors" to successfulSensors.map { it.displayName }.joinToString(","),
                            "failed_sensors" to failedSensors.map { it.displayName }.joinToString(","),
                            "unified_time_base" to "samsung_s22_ground_truth",
                            "recording_mode" to "parallel_multimodal"
                        ))
                    }
                    
                    // Create session data
                    val session = ParallelRecordingSession(
                        sessionId = unifiedSessionId,
                        selectedSensors = selectedSensors,
                        startTimestamp = synchronizedTimestamp,
                        rgbVideoFile = if (successfulSensors.contains(SensorSelectionDialog.SensorType.RGB)) 
                            rgbCameraRecorder?.getCurrentVideoFile() else null,
                        sensorStatus = successfulSensors.associateWith { "Recording" } + 
                                      failedSensors.associateWith { "Failed" }
                    )
                    
                    onRecordingStarted?.invoke(session)
                    
                    Log.i(TAG, "Parallel multi-modal recording started successfully")
                    Log.i(TAG, "Active sensors: ${successfulSensors.map { it.displayName }.joinToString(", ")}")
                }
            }
            
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start parallel recording", e)
            onError?.invoke("Failed to start parallel recording: ${e.message}")
            cleanup()
            return false
        }
    }

    /**
     * Stop parallel multi-modal recording
     */
    fun stopParallelRecording(): ParallelRecordingSession? {
        if (!isRecording.get() || currentSessionId == null) {
            Log.w(TAG, "Not currently recording")
            return null
        }

        try {
            val stopTimestamp = TimeUtil.getSynchronizedTimestamp()
            val sessionId = currentSessionId!!
            val startTime = synchronizedStartTime.get()
            val recordingDuration = stopTimestamp - startTime

            Log.i(TAG, "Stopping parallel multi-modal recording")
            Log.i(TAG, "Recording duration: ${recordingDuration}ms")

            // Add sync event before stopping
            if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
                thermalRecorder.triggerSyncEvent("PARALLEL_RECORDING_STOPPING", mapOf(
                    "sync_timestamp" to stopTimestamp.toString(),
                    "session_duration" to recordingDuration.toString(),
                    "stop_reason" to "user_initiated"
                ))
            }

            // Stop all active recording components in parallel
            recordingScope.launch {
                val stopJobs = mutableListOf<Deferred<Unit>>()
                
                selectedSensors.forEach { sensor ->
                    val job = async {
                        when (sensor) {
                            SensorSelectionDialog.SensorType.THERMAL, 
                            SensorSelectionDialog.SensorType.GSR -> {
                                // Stop thermal/GSR recording
                                thermalRecorder.stopRecording()
                                Unit
                            }
                            
                            SensorSelectionDialog.SensorType.RGB -> {
                                // Stop RGB recording
                                rgbCameraRecorder?.stopRecording()
                                Unit
                            }
                        }
                    }
                    stopJobs.add(job)
                }
                
                // Wait for all to stop
                stopJobs.awaitAll()
                
                withContext(Dispatchers.Main) {
                    isRecording.set(false)
                    
                    // Create final session with all output files
                    val sessionDir = thermalRecorder.getSessionDirectory()
                    val finalSession = ParallelRecordingSession(
                        sessionId = sessionId,
                        selectedSensors = selectedSensors,
                        startTimestamp = startTime,
                        endTimestamp = stopTimestamp,
                        recordingDuration = recordingDuration,
                        thermalVideoFile = if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
                            // The thermal video file would be created by the existing thermal recording system
                            null // Will be set by the thermal recording system
                        } else null,
                        rgbVideoFile = if (selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) 
                            rgbCameraRecorder?.getCurrentVideoFile() else null,
                        gsrDataFile = if (selectedSensors.contains(SensorSelectionDialog.SensorType.GSR)) 
                            sessionDir?.let { File(it, "signals.csv") } else null,
                        syncMarksFile = sessionDir?.let { File(it, "sync_marks.csv") },
                        sessionMetadata = sessionDir?.let { File(it, "session_metadata.json") },
                        sensorStatus = selectedSensors.associateWith { "Completed" }
                    )
                    
                    currentSessionId = null
                    selectedSensors = emptySet()
                    
                    onRecordingStopped?.invoke(finalSession)
                    
                    Log.i(TAG, "Parallel multi-modal recording completed")
                    Log.i(TAG, "Session files saved to: ${sessionDir?.absolutePath}")
                }
            }
            
            // Return preliminary session info (final session will be provided via callback)
            return ParallelRecordingSession(
                sessionId = sessionId,
                selectedSensors = selectedSensors,
                startTimestamp = startTime,
                endTimestamp = stopTimestamp,
                recordingDuration = recordingDuration
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop parallel recording", e)
            onError?.invoke("Failed to stop parallel recording: ${e.message}")
            cleanup()
            return null
        }
    }

    /**
     * Add synchronized event marker across all active recording streams
     */
    fun addParallelSyncEvent(eventName: String, metadata: Map<String, String> = emptyMap()) {
        if (!isRecording.get()) return

        val timestamp = TimeUtil.getSynchronizedTimestamp()
        val eventData = metadata.toMutableMap().apply {
            put("sync_timestamp", timestamp.toString())
            put("event_name", eventName)
            put("session_id", currentSessionId ?: "unknown")
            put("active_sensors", selectedSensors.map { it.displayName }.joinToString(","))
            put("timing_source", "samsung_s22_ground_truth")
        }

        // Add sync event to GSR/thermal recorder if active
        if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
            thermalRecorder.triggerSyncEvent("PARALLEL_CROSS_MODAL_$eventName", eventData)
        }
        
        Log.d(TAG, "Added parallel synchronized event: $eventName at timestamp $timestamp")
    }

    /**
     * Switch RGB camera (front/back) during recording
     */
    fun switchRGBCamera(): RGBCameraRecorder.CameraFacing? {
        if (!selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
            Log.w(TAG, "RGB sensor not active, cannot switch camera")
            return null
        }
        
        val newFacing = rgbCameraRecorder?.switchCamera()
        
        if (isRecording.get()) {
            addParallelSyncEvent("RGB_CAMERA_SWITCHED", mapOf(
                "new_camera_facing" to (newFacing?.displayName ?: "unknown")
            ))
        }
        
        return newFacing
    }

    /**
     * Update RGB recording settings during recording
     */
    fun updateRGBSettings(settings: RGBCameraRecorder.RecordingSettings) {
        if (!selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
            Log.w(TAG, "RGB sensor not active, cannot update settings")
            return
        }
        
        rgbCameraRecorder?.updateSettings(settings)
        
        if (isRecording.get()) {
            addParallelSyncEvent("RGB_SETTINGS_CHANGED", mapOf(
                "resolution" to settings.resolution.displayName,
                "frame_rate" to settings.frameRate.toString(),
                "stabilization" to settings.enableStabilization.toString()
            ))
        }
    }

    /**
     * Get current recording state
     */
    fun isRecording() = isRecording.get()
    fun getCurrentSessionId() = currentSessionId
    fun getSelectedSensors() = selectedSensors.toSet()
    fun getSessionDirectory(): File? = thermalRecorder.getSessionDirectory()

    /**
     * Get RGB camera information (only available if RGB sensor is selected)
     */
    fun getCurrentRGBSettings() = if (selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
        rgbCameraRecorder?.getCurrentSettings()
    } else null
    
    fun getRGBCameraFacing() = if (selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
        rgbCameraRecorder?.getCurrentCameraFacing()
    } else null
    
    fun getAvailableRGBCameras() = rgbCameraRecorder?.getAvailableCameraFacing() ?: emptyList()
    fun getSupportedRGBResolutions() = rgbCameraRecorder?.getSupportedResolutions() ?: emptyList()

    /**
     * Cleanup all resources
     */
    fun cleanup() {
        if (isRecording.get()) {
            stopParallelRecording()
        }
        
        recordingScope.cancel()
        rgbCameraRecorder?.cleanup()
        thermalRecorder.cleanup()
        
        currentSessionId = null
        selectedSensors = emptySet()
        isRecording.set(false)
        synchronizedStartTime.set(0)
        
        Log.i(TAG, "Parallel multi-modal recorder cleaned up")
    }
}