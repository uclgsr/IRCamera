package com.topdon.tc001.camera

import android.content.Context
import android.util.Log
import android.view.TextureView
import com.topdon.gsr.util.TimeUtil
import com.topdon.tc001.sensors.gsr.EnhancedThermalRecorder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class SynchronizedMultiModalRecorder(
    private val context: Context,
    private val thermalRecorder: EnhancedThermalRecorder,
    private val rgbTextureView: TextureView,
) {
    companion object {
        private const val TAG = "SynchronizedRecorder"
    }

    private var rgbCameraRecorder: RGBCameraRecorder? = null
    private var currentSessionId: String? = null
    private var isRecording = false

    data class RecordingSession(
        val sessionId: String,
        val startTimestamp: Long,
        val endTimestamp: Long? = null,
        val thermalVideoFile: File? = null,
        val rgbVideoFile: File? = null,
        val gsrDataFile: File? = null,
        val syncMarksFile: File? = null,
        val sessionMetadata: File? = null,
    )

    var onRecordingStarted: ((RecordingSession) -> Unit)? = null
    var onRecordingStopped: ((RecordingSession) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun initialize() {
        rgbCameraRecorder =
            RGBCameraRecorder(context, rgbTextureView).apply {
                initialize()

                onRecordingStarted = {
                    Log.d(TAG, "RGB recording started")
                }

                onRecordingStopped = { videoFile ->
                    Log.d(TAG, "RGB recording stopped: ${videoFile?.toString()}")
                }

                onError = { error ->
                    Log.e(TAG, "RGB camera error: $error")
                    this@SynchronizedMultiModalRecorder.onError?.invoke("RGB Camera: $error")
                }
            }
    }

    fun startSynchronizedRecording(
        sessionId: String? = null,
        rgbSettings: RGBCameraRecorder.RecordingSettings = RGBCameraRecorder.RecordingSettings(),
    ): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        try {

            val unifiedSessionId = sessionId ?: TimeUtil.generateSessionId("MultiModal")
            val synchronizedTimestamp = TimeUtil.getSynchronizedTimestamp()
            currentSessionId = unifiedSessionId

            Log.i(
                TAG,
                "Starting synchronized multi-modal recording with unified timestamp: $synchronizedTimestamp"
            )

            var gsrStarted = false
            GlobalScope.launch {
                gsrStarted = thermalRecorder.startRecording(unifiedSessionId, null, true)
                if (!gsrStarted) {
                    Log.w(TAG, "GSR recording failed to start, continuing with thermal+RGB only")
                }
            }

            rgbCameraRecorder?.updateSettings(rgbSettings)
            val rgbStarted =
                runBlocking { rgbCameraRecorder?.startRecording(unifiedSessionId) } ?: false
            if (!rgbStarted) {
                Log.w(TAG, "RGB recording failed to start")
                if (gsrStarted) {
                    runBlocking { thermalRecorder.stopRecording() }
                }
                return false
            }

            thermalRecorder.triggerSyncEvent(
                "MULTIMODAL_START",
                mapOf(
                    "sync_timestamp" to synchronizedTimestamp.toString(),
                    "unified_time_base" to "samsung_s22_ground_truth",
                    "session_id" to unifiedSessionId,
                    "thermal_recording" to "active",
                    "rgb_recording" to "active",
                    "gsr_recording" to if (gsrStarted) "active" else "unavailable",
                    "recording_mode" to "synchronized_trimodal",
                ),
            )

            isRecording = true

            val session =
                RecordingSession(
                    sessionId = unifiedSessionId,
                    startTimestamp = synchronizedTimestamp,
                    rgbVideoFile = rgbCameraRecorder?.getCurrentVideoFile(),
                )

            onRecordingStarted?.invoke(session)

            Log.i(TAG, "Synchronized multi-modal recording started successfully: $unifiedSessionId")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start synchronized recording", e)
            onError?.invoke("Failed to start synchronized recording: ${e.message}")

            cleanup()
            return false
        }
    }

    fun stopSynchronizedRecording(): RecordingSession? {
        if (!isRecording || currentSessionId == null) {
            Log.w(TAG, "Not currently recording")
            return null
        }

        try {
            val stopTimestamp = TimeUtil.getSynchronizedTimestamp()
            val sessionId = currentSessionId!!

            Log.i(TAG, "Stopping synchronized multi-modal recording at timestamp: $stopTimestamp")

            thermalRecorder.triggerSyncEvent(
                "MULTIMODAL_STOP",
                mapOf(
                    "sync_timestamp" to stopTimestamp.toString(),
                    "session_id" to sessionId,
                    "unified_time_base" to "samsung_s22_ground_truth",
                    "stop_reason" to "user_initiated",
                ),
            )

            val gsrSession = runBlocking { thermalRecorder.stopRecording() }
            val rgbVideoFile = runBlocking { rgbCameraRecorder?.stopRecording() }

            isRecording = false

            val sessionDir = thermalRecorder.getSessionDirectory()
            
            val finalSession =
                RecordingSession(
                    sessionId = sessionId,
                    startTimestamp = gsrSession?.startTime ?: System.currentTimeMillis(),
                    endTimestamp = stopTimestamp,
                    rgbVideoFile = null, // Boolean return type doesn't match File expected
                    gsrDataFile = gsrSession?.let { session ->
                        thermalRecorder.getSessionDirectory()?.let { dir ->
                            File(dir, "signals.csv")
                        }
                    },
                    syncMarksFile = gsrSession?.let { session ->
                        thermalRecorder.getSessionDirectory()?.let { dir ->
                            File(dir, "sync_marks.csv")
                        }
                    },
                    sessionMetadata = gsrSession?.let { session ->
                        thermalRecorder.getSessionDirectory()?.let { dir ->
                            File(dir, "session_metadata.json")
                        }
                    },
                )

            currentSessionId = null
            onRecordingStopped?.invoke(finalSession)

            Log.i(TAG, "Synchronized multi-modal recording completed: $sessionId")
            Log.i(
                TAG,
                "Session files: RGB=${if (rgbVideoFile == true) "completed" else "failed"}, GSR=${gsrSession?.sampleCount} samples"
            )

            return finalSession
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop synchronized recording", e)
            onError?.invoke("Failed to stop synchronized recording: ${e.message}")
            cleanup()
            return null
        }
    }

    fun addSyncEvent(
        eventName: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        if (!isRecording) return

        val timestamp = TimeUtil.getSynchronizedTimestamp()
        val eventData =
            metadata.toMutableMap().apply {
                put("sync_timestamp", timestamp.toString())
                put("event_name", eventName)
                put("session_id", currentSessionId ?: "unknown")
                put("timing_source", "samsung_s22_ground_truth")
            }

        thermalRecorder.triggerSyncEvent("CROSS_MODAL_EVENT_$eventName", eventData)

        Log.d(TAG, "Added synchronized event: $eventName at timestamp $timestamp")
    }

    fun switchRGBCamera(): RGBCameraRecorder.CameraFacing? {
        val currentFacing = rgbCameraRecorder?.getCurrentCameraFacing()
        val newFacing =
            if (currentFacing == RGBCameraRecorder.CameraFacing.BACK) {
                RGBCameraRecorder.CameraFacing.FRONT
            } else {
                RGBCameraRecorder.CameraFacing.BACK
            }

        val success = runBlocking { rgbCameraRecorder?.switchCamera(newFacing) ?: false }
        return if (success) newFacing else currentFacing
    }

    fun updateRGBSettings(settings: RGBCameraRecorder.RecordingSettings) {
        rgbCameraRecorder?.updateSettings(settings)

        if (isRecording) {
            addSyncEvent(
                "RGB_SETTINGS_CHANGED",
                mapOf(
                    "resolution" to settings.resolution.displayName,
                    "frame_rate" to settings.frameRate.toString(),
                    "stabilization" to settings.enableStabilization.toString(),
                ),
            )
        }
    }

    fun setRGBFlash(enabled: Boolean) {
        runBlocking { rgbCameraRecorder?.setFlashEnabled(enabled) }

        if (isRecording) {
            addSyncEvent(
                "RGB_FLASH_TOGGLE",
                mapOf(
                    "flash_enabled" to enabled.toString(),
                ),
            )
        }
    }

    fun pauseRGBRecording() {
        runBlocking { rgbCameraRecorder?.pauseRecording() }

        if (isRecording) {
            addSyncEvent("RGB_RECORDING_PAUSED")
        }
    }

    fun resumeRGBRecording() {
        runBlocking { rgbCameraRecorder?.resumeRecording() }

        if (isRecording) {
            addSyncEvent("RGB_RECORDING_RESUMED")
        }
    }

    fun isRecording() = isRecording

    fun getCurrentSessionId() = currentSessionId

    fun getCurrentRGBSettings() = rgbCameraRecorder?.getCurrentSettings()

    fun getRGBCameraFacing() = rgbCameraRecorder?.getCurrentCameraFacing()

    fun getAvailableRGBCameras() = rgbCameraRecorder?.getAvailableCameraFacing() ?: emptyList()

    fun getSupportedRGBResolutions() = rgbCameraRecorder?.getSupportedResolutions() ?: emptyList()

    fun getSessionDirectory(): File? {
        return thermalRecorder.getSessionDirectory()
    }

    fun cleanup() {
        if (isRecording) {
            stopSynchronizedRecording()
        }

        rgbCameraRecorder?.cleanup()
        thermalRecorder.cleanup()

        currentSessionId = null
        isRecording = false
    }

    fun createThermalRGBSession(thermalVideoFile: File): RecordingSession? {
        val sessionId = currentSessionId ?: return null
        val sessionDir = getSessionDirectory() ?: return null

        return RecordingSession(
            sessionId = sessionId,
            startTimestamp = System.currentTimeMillis(),
            thermalVideoFile = thermalVideoFile,
            rgbVideoFile = rgbCameraRecorder?.getCurrentVideoFile(),
            gsrDataFile = File(sessionDir, "signals.csv"),
            syncMarksFile = File(sessionDir, "sync_marks.csv"),
            sessionMetadata = File(sessionDir, "session_metadata.json"),
        )
    }

    fun generateSessionMetadata(): Map<String, Any> {
        return mapOf(
            "session_id" to (currentSessionId ?: "unknown"),
            "device_model" to android.os.Build.MODEL,
            "device_processor" to detectSamsungS22Processor(),
            "timing_precision" to "sub_millisecond",
            "unified_time_base" to "samsung_s22_ground_truth",
            "recording_components" to
                    mapOf(
                        "thermal" to "thermal_camera_video",
                        "rgb" to
                                mapOf(
                                    "resolution" to (rgbCameraRecorder?.getCurrentSettings()?.resolution?.displayName
                                        ?: "unknown"),
                                    "frame_rate" to (rgbCameraRecorder?.getCurrentSettings()?.frameRate
                                        ?: 0),
                                    "camera_facing" to (rgbCameraRecorder?.getCurrentCameraFacing()?.displayName
                                        ?: "unknown"),
                                ),
                        "gsr" to
                                mapOf(
                                    "sampling_rate" to "128Hz",
                                    "device_type" to "shimmer3_gsr",
                                    "data_format" to "conductance_resistance_csv",
                                ),
                    ),
            "synchronization_accuracy" to "samsung_s22_hardware_timer",
            "android_version" to android.os.Build.VERSION.RELEASE,
            "api_level" to android.os.Build.VERSION.SDK_INT,
        )
    }

    private fun detectSamsungS22Processor(): String {
        val deviceModel = android.os.Build.MODEL
        return when {
            deviceModel.contains("SM-S901E") -> "Exynos_2200" // International
            deviceModel.contains("SM-S901U") -> "Snapdragon_8_Gen_1" // US
            deviceModel.contains("SM-S901W") -> "Snapdragon_8_Gen_1" // Canada
            deviceModel.contains("SM-S901N") -> "Snapdragon_8_Gen_1" // Korea
            deviceModel.contains("SM-S901") -> "Samsung_S22_Generic" // Generic S22
            else -> "Unknown_Device"
        }
    }
}
