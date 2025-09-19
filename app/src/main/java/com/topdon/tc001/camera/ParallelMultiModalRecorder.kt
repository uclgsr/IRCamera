package com.topdon.tc001.camera

import android.content.Context
import android.util.Log
import android.view.TextureView
import com.topdon.gsr.util.TimeUtil
import com.topdon.tc001.camera.ui.SensorSelectionDialog
import com.topdon.tc001.gsr.EnhancedThermalRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ParallelMultiModalRecorder(
    private val context: Context,
    private val thermalRecorder: EnhancedThermalRecorder,
    private val rgbTextureView: TextureView,
) {
    companion object {
        private const val TAG = "ParallelRecorder"
    }

    private var rgbCameraRecorder: RGBCameraRecorder? = null
    private val recordingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var currentSessionId: String? = null
    private val isRecording = AtomicBoolean(false)
    private val synchronizedStartTime = AtomicLong(0)
    private var selectedSensors: Set<SensorSelectionDialog.SensorType> = emptySet()

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
        val sensorStatus: Map<SensorSelectionDialog.SensorType, String> = emptyMap(),
    )

    var onRecordingStarted: ((ParallelRecordingSession) -> Unit)? = null
    var onRecordingStopped: ((ParallelRecordingSession) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSensorStatusChanged: ((SensorSelectionDialog.SensorType, String) -> Unit)? = null

    fun initialize() {
        rgbCameraRecorder =
            RGBCameraRecorder(context, rgbTextureView).apply {
                initialize()

                onRecordingStarted = {
                    onSensorStatusChanged?.invoke(SensorSelectionDialog.SensorType.RGB, "Recording")
                    Log.d(TAG, "RGB recording started in parallel session")
                }

                onRecordingStopped = { videoFile ->
                    onSensorStatusChanged?.invoke(SensorSelectionDialog.SensorType.RGB, "Completed")
                    Log.d(TAG, "RGB recording stopped: ${videoFile?.toString()}")
                }

                onError = { error ->
                    onSensorStatusChanged?.invoke(
                        SensorSelectionDialog.SensorType.RGB,
                        "Error: $error"
                    )
                    Log.e(TAG, "RGB camera error in parallel session: $error")
                }
            }

        Log.i(TAG, "Parallel multi-modal recorder initialized")
    }

    fun startParallelRecording(
        selectedSensors: Set<SensorSelectionDialog.SensorType>,
        sessionId: String? = null,
        rgbSettings: RGBCameraRecorder.RecordingSettings = RGBCameraRecorder.RecordingSettings(),
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

            val unifiedSessionId = sessionId ?: TimeUtil.generateSessionId("Parallel")
            val synchronizedTimestamp = TimeUtil.getSynchronizedTimestamp()

            currentSessionId = unifiedSessionId
            this.selectedSensors = selectedSensors
            synchronizedStartTime.set(synchronizedTimestamp)

            Log.i(TAG, "Starting parallel recording with sensors: $selectedSensors")
            Log.i(
                TAG,
                "Unified session ID: $unifiedSessionId, Ground truth timestamp: $synchronizedTimestamp"
            )

            recordingScope.launch {
                val startJobs =
                    mutableListOf<Deferred<Pair<SensorSelectionDialog.SensorType, Boolean>>>()

                selectedSensors.forEach { sensor ->
                    val job =
                        async {
                            when (sensor) {
                                SensorSelectionDialog.SensorType.THERMAL -> {

                                    val success =
                                        thermalRecorder.startRecording(unifiedSessionId, null, true)
                                    if (success) {

                                        delay(50) // Small delay to ensure recording is active
                                        thermalRecorder.triggerSyncEvent(
                                            "PARALLEL_THERMAL_START",
                                            mapOf(
                                                "sync_timestamp" to synchronizedTimestamp.toString(),
                                                "selected_sensors" to selectedSensors.map { it.displayName }
                                                    .joinToString(","),
                                                "recording_mode" to "parallel_multimodal",
                                            ),
                                        )
                                    }
                                    Pair(sensor, success)
                                }

                                SensorSelectionDialog.SensorType.RGB -> {

                                    rgbCameraRecorder?.updateSettings(rgbSettings)
                                    val success =
                                        rgbCameraRecorder?.startRecording(unifiedSessionId) ?: false
                                    Pair(sensor, success)
                                }

                                SensorSelectionDialog.SensorType.GSR -> {


                                    Pair(
                                        sensor,
                                        selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)
                                    )
                                }
                            }
                        }
                    startJobs.add(job)
                }

                val results = startJobs.awaitAll()
                val failedSensors = results.filter { !it.second }.map { it.first }
                val successfulSensors = results.filter { it.second }.map { it.first }.toSet()

                withContext(Dispatchers.Main) {
                    if (successfulSensors.isEmpty()) {

                        Log.e(TAG, "All sensors failed to start: $failedSensors")
                        onError?.invoke(
                            "Failed to start any sensors: ${
                                failedSensors.map { it.displayName }.joinToString(", ")
                            }"
                        )
                        cleanup()
                        return@withContext
                    }

                    if (failedSensors.isNotEmpty()) {

                        Log.w(TAG, "Some sensors failed to start: $failedSensors")
                        Log.i(TAG, "Continuing with successful sensors: $successfulSensors")
                    }

                    isRecording.set(true)

                    successfulSensors.forEach { sensor ->
                        onSensorStatusChanged?.invoke(sensor, "Recording")
                    }
                    failedSensors.forEach { sensor ->
                        onSensorStatusChanged?.invoke(sensor, "Failed")
                    }

                    if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
                        thermalRecorder.triggerSyncEvent(
                            "PARALLEL_RECORDING_STARTED",
                            mapOf(
                                "sync_timestamp" to synchronizedTimestamp.toString(),
                                "selected_sensors" to selectedSensors.map { it.displayName }
                                    .joinToString(","),
                                "successful_sensors" to successfulSensors.map { it.displayName }
                                    .joinToString(","),
                                "failed_sensors" to failedSensors.map { it.displayName }
                                    .joinToString(","),
                                "unified_time_base" to "samsung_s22_ground_truth",
                                "recording_mode" to "parallel_multimodal",
                            ),
                        )
                    }

                    val session =
                        ParallelRecordingSession(
                            sessionId = unifiedSessionId,
                            selectedSensors = selectedSensors,
                            startTimestamp = synchronizedTimestamp,
                            rgbVideoFile =
                                if (successfulSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
                                    rgbCameraRecorder?.getCurrentVideoFile()
                                } else {
                                    null
                                },
                            sensorStatus =
                                successfulSensors.associateWith { "Recording" } +
                                        failedSensors.associateWith { "Failed" },
                        )

                    onRecordingStarted?.invoke(session)

                    Log.i(TAG, "Parallel multi-modal recording started successfully")
                    Log.i(
                        TAG,
                        "Active sensors: ${
                            successfulSensors.map { it.displayName }.joinToString(", ")
                        }"
                    )
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

            if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
                thermalRecorder.triggerSyncEvent(
                    "PARALLEL_RECORDING_STOPPING",
                    mapOf(
                        "sync_timestamp" to stopTimestamp.toString(),
                        "session_duration" to recordingDuration.toString(),
                        "stop_reason" to "user_initiated",
                    ),
                )
            }

            recordingScope.launch {
                val stopJobs = mutableListOf<Deferred<Unit>>()

                selectedSensors.forEach { sensor ->
                    val job =
                        async {
                            when (sensor) {
                                SensorSelectionDialog.SensorType.THERMAL,
                                SensorSelectionDialog.SensorType.GSR,
                                    -> {

                                    thermalRecorder.stopRecording()
                                    Unit
                                }

                                SensorSelectionDialog.SensorType.RGB -> {

                                    rgbCameraRecorder?.stopRecording()
                                    Unit
                                }
                            }
                        }
                    stopJobs.add(job)
                }

                stopJobs.awaitAll()

                withContext(Dispatchers.Main) {
                    isRecording.set(false)

                    val sessionDir = thermalRecorder.getSessionDirectory()
                    val finalSession =
                        ParallelRecordingSession(
                            sessionId = sessionId,
                            selectedSensors = selectedSensors,
                            startTimestamp = startTime,
                            endTimestamp = stopTimestamp,
                            recordingDuration = recordingDuration,
                            thermalVideoFile =
                                if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {

                                    null // Will be set by the thermal recording system
                                } else {
                                    null
                                },
                            rgbVideoFile =
                                if (selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
                                    rgbCameraRecorder?.getCurrentVideoFile()
                                } else {
                                    null
                                },
                            gsrDataFile =
                                if (selectedSensors.contains(SensorSelectionDialog.SensorType.GSR)) {
                                    sessionDir?.let { dir -> File(dir, "signals.csv") }
                                } else {
                                    null
                                },
                            syncMarksFile = sessionDir?.let { dir -> File(dir, "sync_marks.csv") },
                            sessionMetadata = sessionDir?.let { dir -> File(dir, "session_metadata.json") },
                            sensorStatus = selectedSensors.associateWith { "Completed" },
                        )

                    currentSessionId = null
                    selectedSensors = emptySet()

                    onRecordingStopped?.invoke(finalSession)

                    Log.i(TAG, "Parallel multi-modal recording completed")
                    Log.i(TAG, "Session files saved to: ${sessionDir?.absolutePath}")
                }
            }

            return ParallelRecordingSession(
                sessionId = sessionId,
                selectedSensors = selectedSensors,
                startTimestamp = startTime,
                endTimestamp = stopTimestamp,
                recordingDuration = recordingDuration,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop parallel recording", e)
            onError?.invoke("Failed to stop parallel recording: ${e.message}")
            cleanup()
            return null
        }
    }

    fun addParallelSyncEvent(
        eventName: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        if (!isRecording.get()) return

        val timestamp = TimeUtil.getSynchronizedTimestamp()
        val eventData =
            metadata.toMutableMap().apply {
                put("sync_timestamp", timestamp.toString())
                put("event_name", eventName)
                put("session_id", currentSessionId ?: "unknown")
                put("active_sensors", selectedSensors.map { it.displayName }.joinToString(","))
                put("timing_source", "samsung_s22_ground_truth")
            }

        if (selectedSensors.contains(SensorSelectionDialog.SensorType.THERMAL)) {
            thermalRecorder.triggerSyncEvent("PARALLEL_CROSS_MODAL_$eventName", eventData)
        }

        Log.d(TAG, "Added parallel synchronized event: $eventName at timestamp $timestamp")
    }

    fun switchRGBCamera(): RGBCameraRecorder.CameraFacing? {
        if (!selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
            Log.w(TAG, "RGB sensor not active, cannot switch camera")
            return null
        }

        val currentFacing = rgbCameraRecorder?.getCurrentCameraFacing()
        val newFacing =
            if (currentFacing == RGBCameraRecorder.CameraFacing.BACK) {
                RGBCameraRecorder.CameraFacing.FRONT
            } else {
                RGBCameraRecorder.CameraFacing.BACK
            }

        val success = runBlocking { rgbCameraRecorder?.switchCamera(newFacing) ?: false }
        val resultFacing = if (success) newFacing else currentFacing

        if (isRecording.get()) {
            addParallelSyncEvent(
                "RGB_CAMERA_SWITCHED",
                mapOf(
                    "new_camera_facing" to (resultFacing?.displayName ?: "unknown"),
                ),
            )
        }

        return resultFacing
    }

    fun updateRGBSettings(settings: RGBCameraRecorder.RecordingSettings) {
        if (!selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
            Log.w(TAG, "RGB sensor not active, cannot update settings")
            return
        }

        rgbCameraRecorder?.updateSettings(settings)

        if (isRecording.get()) {
            addParallelSyncEvent(
                "RGB_SETTINGS_CHANGED",
                mapOf(
                    "resolution" to settings.resolution.displayName,
                    "frame_rate" to settings.frameRate.toString(),
                    "stabilization" to settings.enableStabilization.toString(),
                ),
            )
        }
    }

    fun isRecording() = isRecording.get()

    fun getCurrentSessionId() = currentSessionId

    fun getSelectedSensors() = selectedSensors.toSet()

    fun getSessionDirectory(): File? = thermalRecorder.getSessionDirectory()

    fun getCurrentRGBSettings() =
        if (selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
            rgbCameraRecorder?.getCurrentSettings()
        } else {
            null
        }

    fun getRGBCameraFacing() =
        if (selectedSensors.contains(SensorSelectionDialog.SensorType.RGB)) {
            rgbCameraRecorder?.getCurrentCameraFacing()
        } else {
            null
        }

    fun getAvailableRGBCameras() = rgbCameraRecorder?.getAvailableCameraFacing() ?: emptyList()

    fun getSupportedRGBResolutions() = rgbCameraRecorder?.getSupportedResolutions() ?: emptyList()

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
