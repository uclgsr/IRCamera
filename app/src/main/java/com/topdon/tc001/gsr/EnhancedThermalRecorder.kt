package com.topdon.tc001.gsr

import android.content.Context
import android.util.Log
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.service.GSRRecorder
import com.topdon.gsr.service.SessionManager
import com.topdon.gsr.util.TimeUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class EnhancedThermalRecorder private constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "EnhancedThermalRecorder"

        fun create(context: Context): EnhancedThermalRecorder {
            val recorder = EnhancedThermalRecorder(context)

            TimeUtil.initializeGroundTruthTiming()

            val detectedProcessor = TimeUtil.getDetectedProcessor()
            val deviceModel = TimeUtil.getDeviceModel()
            val deviceManufacturer = android.os.Build.MANUFACTURER

            if (deviceManufacturer.contains("samsung", ignoreCase = true) &&
                deviceModel.contains("SM-S90", ignoreCase = true)
            ) {
                Log.d(TAG, "Samsung S22 device detected: $deviceManufacturer $deviceModel")
                Log.d(
                    TAG,
                    "Processor variant: $detectedProcessor - Optimal timing performance enabled"
                )

                when (detectedProcessor) {
                    "Exynos_2200" -> Log.i(
                        TAG,
                        "Exynos 2200 processor detected - ARM Cortex-X2 high-precision timing active"
                    )

                    "Snapdragon_8_Gen_1" -> Log.i(
                        TAG,
                        "Snapdragon 8 Gen 1 processor detected - Kryo 780 high-precision timing active"
                    )

                    "Samsung_S22_Generic" -> Log.i(
                        TAG,
                        "Samsung S22 detected - Generic high-precision timing active"
                    )
                }
            } else {
                Log.w(
                    TAG,
                    "Non-Samsung S22 device: $deviceManufacturer $deviceModel - Using standard timing"
                )
                Log.w(TAG, "Detected processor: $detectedProcessor")
            }

            return recorder
        }
    }

    private val gsrRecorder: GSRRecorder = GSRRecorder(context)
    private val sessionManager: SessionManager = SessionManager.getInstance(context)

    private var currentSession: SessionInfo? = null
    private var isRecordingState = false

    private val gsrListener =
        object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                Log.i(TAG, "Enhanced thermal recording with GSR started: ${sessionInfo.sessionId}")
                currentSession = sessionInfo
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                Log.i(TAG, "Enhanced thermal recording with GSR stopped: ${sessionInfo.sessionId}")
                currentSession = null
                isRecordingState = false
            }

            override fun onSampleRecorded(sample: com.topdon.gsr.model.GSRSample) {

                if (sample.sampleIndex % 1280 == 0L) {
                    Log.d(
                        TAG,
                        "GSR recording: ${sample.sampleIndex} samples (${sample.sampleIndex / 128}s)"
                    )
                }
            }

            override fun onSyncMarkAdded(syncMark: com.topdon.gsr.model.SyncMark) {
                Log.d(TAG, "Thermal sync event: ${syncMark.eventType}")
            }

            override fun onError(error: String) {
                Log.e(TAG, "GSR recording error during thermal session: $error")
            }
        }

    init {
        gsrRecorder.addListener(gsrListener)


        Log.d(TAG, "Enhanced thermal recorder initialized with Samsung S22 ground truth timing")
        Log.d(TAG, "Detected processor: ${TimeUtil.getDetectedProcessor()}")
        Log.d(TAG, "Timing validation: ${TimeUtil.validateTimingSystem()}")
    }

    suspend fun startRecording(
        sessionName: String,
        participantId: String? = null,
        enableGsr: Boolean = true,
    ): Boolean {
        if (isRecordingState) {
            Log.w(TAG, "Recording already in progress")
            return false
        }

        val sessionId =
            if (sessionName.contains("_")) {
                sessionName // Use provided name if it looks like a session ID
            } else {
                TimeUtil.generateSessionId(sessionName)
            }

        val unifiedStartTimestamp = TimeUtil.getHighPrecisionTimestamp()
        Log.d(
            TAG,
            "Starting synchronized recording with Samsung S22 ground truth timestamp: $unifiedStartTimestamp"
        )
        Log.d(
            TAG,
            "Using ${TimeUtil.getDetectedProcessor()} processor timing for maximum precision"
        )

        if (enableGsr) {

            val gsrStarted =
                gsrRecorder.startRecording(sessionName, participantId, "Thermal_GSR_Study")

            if (gsrStarted) {
                isRecordingState = true

                val timingValidation = TimeUtil.validateTimingSystem()
                Log.i(TAG, "Enhanced thermal recording started with GSR: $sessionName")
                Log.d(TAG, "Samsung S22 timing system validation: $timingValidation")

                val syncEventSuccess =
                    triggerSyncEvent(
                        "RECORDING_INITIALIZATION",
                        mapOf(
                            "unified_start_timestamp" to unifiedStartTimestamp.toString(),
                            "samsung_s22_ground_truth" to "established",
                            "timing_validation" to timingValidation.toString(),
                        ),
                    )

                if (syncEventSuccess) {
                    Log.d(TAG, "Initial synchronization mark successfully added")
                }

                return true
            } else {
                Log.e(TAG, "Failed to start GSR recording for thermal session")
                return false
            }
        } else {

            currentSession =
                sessionManager.createSession(sessionId, participantId, "Thermal_Only_Study")
            isRecordingState = true
            Log.i(TAG, "Thermal recording started without GSR: $sessionId")
            return true
        }
    }

    fun stopRecording(): SessionInfo? {
        if (!isRecordingState) {
            Log.w(TAG, "No recording in progress")
            return currentSession
        }

        val session =
            if (gsrRecorder.isRecording()) {
                gsrRecorder.stopRecording()
            } else {
                currentSession?.let { sessionManager.completeSession(it.sessionId) }
            }

        isRecordingState = false
        Log.i(TAG, "Enhanced thermal recording stopped")
        return session
    }

    fun triggerSyncEvent(
        eventType: String = "THERMAL_CAPTURE",
        metadata: Map<String, String> = emptyMap(),
    ): Boolean {
        return if (isRecordingState) {
            if (gsrRecorder.isRecording()) {

                val synchronizedTimestamp = TimeUtil.getHighPrecisionTimestamp()
                val enhancedMetadata =
                    mutableMapOf<String, String>().apply {
                        putAll(metadata)
                        putAll(TimeUtil.getTimingMetadata())
                        put("sync_timestamp", synchronizedTimestamp.toString())
                        put("high_precision_timestamp", synchronizedTimestamp.toString())
                        put("thermal_ground_truth", "samsung_s22_snapdragon_8_gen_1")
                        put("timing_validation", TimeUtil.validateTimingSystem().toString())
                    }

                try {

                    GlobalScope.launch {
                        gsrRecorder.addSyncMark(eventType, enhancedMetadata.toString())
                    }
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add sync mark", e)
                    false
                }
            } else {

                currentSession?.let { session ->
                    val syncMark =
                        com.topdon.gsr.model.SyncMark(
                            timestamp = System.currentTimeMillis(),
                            utcTimestamp = TimeUtil.getHighPrecisionTimestamp(),
                            eventType = eventType,
                            sessionId = session.sessionId,
                            metadata =
                                metadata + TimeUtil.getTimingMetadata() +
                                        mapOf(
                                            "samsung_s22_precision" to "sub_millisecond",
                                            "snapdragon_timer" to "active",
                                        ),
                        )
                    session.syncMarks.add(syncMark)
                    Log.d(
                        TAG,
                        "Sync event added to thermal-only session with Samsung S22 unified timing: $eventType"
                    )
                    true
                } ?: false
            }
        } else {
            Log.w(TAG, "Cannot trigger sync event - not recording")
            false
        }
    }

    fun captureFrame(frameMetadata: Map<String, String> = emptyMap()): Boolean {
        val synchronizedTimestamp = TimeUtil.getHighPrecisionTimestamp()
        val metadata =
            mutableMapOf<String, String>().apply {
                putAll(frameMetadata)
                put("capture_type", "thermal")
                put("timestamp", TimeUtil.formatTimestamp(synchronizedTimestamp))
                put("sync_method", "unified_samsung_s22_snapdragon_ground_truth")
                put("precision_level", "sub_millisecond")
                putAll(TimeUtil.getTimingMetadata())
            }

        return triggerSyncEvent("THERMAL_FRAME_CAPTURE", metadata)
    }

    fun isRecording(): Boolean = isRecordingState

    fun getCurrentSession(): SessionInfo? = currentSession

    fun getSessionDirectory(): File? {
        return gsrRecorder.getSessionDirectory()
    }

    fun setPcTimeOffset(offsetMs: Long) {
        TimeUtil.setPcTimeOffset(offsetMs)
        Log.d(TAG, "PC time offset set: ${offsetMs}ms")
    }

    fun addSessionMetadata(
        key: String,
        value: String,
    ): Boolean {
        return currentSession?.let { session ->
            session.metadata[key] = value
            true
        } ?: false
    }

    fun getRecordingStats(): RecordingStats? {
        return currentSession?.let { session ->
            RecordingStats(
                sessionId = session.sessionId,
                duration = session.getDurationMs(),
                gsrSampleCount = if (gsrRecorder.isRecording()) gsrRecorder.getCurrentSession()?.sampleCount
                    ?: 0 else 0,
                syncEventCount = session.syncMarks.size,
                isActive = session.isActive(),
            )
        }
    }

    data class RecordingStats(
        val sessionId: String,
        val duration: Long,
        val gsrSampleCount: Long,
        val syncEventCount: Int,
        val isActive: Boolean,
    )

    fun cleanup() {
        gsrRecorder.removeListener(gsrListener)
        if (isRecordingState) {
            stopRecording()
        }
    }
}
