package com.mpdc4gsr.gsr.service
import android.content.Context
import android.os.Environment
import android.util.Log
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.util.TimeUtils
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
class GSRRecorder(
    private val context: Context,
    private val shimmerDeviceFactory: ShimmerDeviceFactory,
    private val samplingRateHz: Int = 128,
) {
    private val shimmerRecorder = ShimmerGSRRecorder(context, shimmerDeviceFactory, samplingRateHz)
    private val useShimmerDevice = true
    companion object {
        private const val TAG = "GSRRecorder"
        private const val SESSIONS_DIR = "IRCamera_Sessions"
        private const val SIGNALS_FILENAME = "signals.csv"
        private const val SYNC_MARKS_FILENAME = "sync_marks.csv"
        private const val SESSION_METADATA_FILENAME = "session_metadata.json"
        private val SIGNALS_HEADER =
            arrayOf(
                "timestamp_ms",
                "utc_timestamp_ms",
                "conductance_us",
                "resistance_kohms",
                "sample_index",
                "session_id",
            )
        private val SYNC_MARKS_HEADER =
            arrayOf(
                "timestamp_ms",
                "utc_timestamp_ms",
                "event_type",
                "session_id",
                "metadata",
            )
    }
    private val sampleIntervalMs = 1000L / samplingRateHz
    private val isRecording = AtomicBoolean(false)
    private val sampleIndex = AtomicLong(0)
    private var currentSession: SessionInfo? = null
    private var sessionDirectory: File? = null
    private var recordingJob: Job? = null
    private var signalsWriter: CSVWriter? = null
    private var syncMarksWriter: CSVWriter? = null
    private val listeners = mutableListOf<GSRRecordingListener>()
    interface GSRRecordingListener {
        fun onRecordingStarted(sessionInfo: SessionInfo)
        fun onRecordingStopped(sessionInfo: SessionInfo)
        fun onSampleRecorded(sample: GSRSample)
        fun onSyncMarkAdded(syncMark: SyncMark)
        fun onError(error: String)
    }
    fun addListener(listener: GSRRecordingListener) {
        listeners.add(listener)
    }
    fun removeListener(listener: GSRRecordingListener) {
        listeners.remove(listener)
    }
    suspend fun initialize(): Boolean {
        return if (useShimmerDevice) {
            Log.i(TAG, "Attempting to initialize Shimmer3 GSR device...")
            val success = shimmerRecorder.initializeDevice()
            if (success) {
                Log.i(TAG, "Shimmer3 device initialized successfully")
                setupShimmerListeners()
                true
            } else {
                Log.w(TAG, "Failed to initialize Shimmer3 device, will use simulated data")
                false
            }
        } else {
            Log.i(TAG, "Using simulated GSR data mode")
            true
        }
    }
    private fun setupShimmerListeners() {
        shimmerRecorder.addListener(
            object : ShimmerGSRRecorder.GSRRecordingListener {
                override fun onRecordingStarted(session: SessionInfo) {
                    listeners.forEach { it.onRecordingStarted(session) }
                }
                override fun onRecordingStopped(session: SessionInfo) {
                    listeners.forEach { it.onRecordingStopped(session) }
                }
                override fun onSampleRecorded(sample: GSRSample) {
                    listeners.forEach { it.onSampleRecorded(sample) }
                }
                override fun onSyncMarkRecorded(syncMark: SyncMark) {
                    listeners.forEach { it.onSyncMarkAdded(syncMark) }
                }
                override fun onError(error: String) {
                    listeners.forEach { it.onError(error) }
                }
                override fun onDeviceConnected() {
                    Log.i(TAG, "Shimmer3 GSR device connected")
                }
                override fun onDeviceDisconnected() {
                    Log.w(TAG, "Shimmer3 GSR device disconnected")
                }
            },
        )
    }
    suspend fun startRecording(
        sessionId: String,
        participantId: String? = null,
        studyName: String? = null,
    ): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "Recording already in progress")
            return false
        }
        return if (useShimmerDevice) {
            shimmerRecorder.startRecording(sessionId)
        } else {
            startSimulatedRecording(sessionId, participantId, studyName)
        }
    }
    private suspend fun startSimulatedRecording(
        sessionId: String,
        participantId: String?,
        studyName: String?,
    ): Boolean {
        try {
            sessionDirectory = createSessionDirectory(sessionId)
            if (sessionDirectory == null) {
                notifyError("Failed to create session directory")
                return false
            }
            if (!initializeCsvWriters()) {
                notifyError("Failed to initialize CSV writers")
                return false
            }
            currentSession =
                SessionInfo(
                    sessionId = sessionId,
                    startTime = System.currentTimeMillis(),
                    participantId = participantId,
                    studyName = studyName ?: "GSR_Study",
                )
            sampleIndex.set(0)
            isRecording.set(true)
            recordingJob =
                CoroutineScope(Dispatchers.IO).launch {
                    generateSimulatedGSRData()
                }
            currentSession?.let { session ->
                listeners.forEach { it.onRecordingStarted(session) }
            }
            Log.i(
                TAG,
                "Simulated GSR recording started: sessionId=$sessionId, samplingRate=${samplingRateHz}Hz"
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start simulated recording", e)
            cleanup()
            notifyError("Failed to start recording: ${e.message}")
            return false
        }
    }
    private suspend fun generateSimulatedGSRData() {
        val baseTime = System.currentTimeMillis()
        while (isRecording.get()) {
            try {
                val currentTime = System.currentTimeMillis()
                val utcTime = TimeUtils.getUtcTimestamp()
                val currentIndex = sampleIndex.getAndIncrement()
                val elapsedMs = currentTime - baseTime
                currentSession?.let { session ->
                    val timeOffset = currentIndex * sampleIntervalMs
                    val baseFreq = timeOffset / 10000.0
                    val breathingFreq = timeOffset / 2000.0
                    val noiseFreq = timeOffset / 500.0
                    val conductance =
                        20.0 +
                                Math.sin(baseFreq) * 10.0 +
                                Math.sin(breathingFreq) * 3.0 +
                                Math.sin(noiseFreq) * 1.0 +
                                Math.random() * 2.0
                    val finalConductance = Math.max(5.0, Math.min(50.0, conductance))
                    val resistance = 1.0 / (finalConductance / 1000000.0)
                    val sample =
                        GSRSample(
                            timestamp = currentTime,
                            utcTimestamp = utcTime,
                            conductance = finalConductance,
                            resistance = resistance,
                            sampleIndex = currentIndex,
                            sessionId = session.sessionId,
                        )
                    signalsWriter?.writeNext(sample.toCsvRow())
                    if (currentIndex % 10 == 0L) {
                        signalsWriter?.flush()
                    }
                    listeners.forEach { it.onSampleRecorded(sample) }
                }
                delay(sampleIntervalMs)
            } catch (e: Exception) {
                if (coroutineContext.isActive) {
                    Log.e(TAG, "Error in simulated data generation", e)
                    notifyError("Data generation error: ${e.message}")
                }
            }
        }
    }
    fun stopRecording(): SessionInfo? {
        if (!isRecording.get()) {
            Log.w(TAG, "No recording in progress")
            return currentSession
        }
        isRecording.set(false)
        return if (useShimmerDevice) {
            shimmerRecorder.stopRecording()
        } else {
            stopSimulatedRecording()
        }
    }
    private fun stopSimulatedRecording(): SessionInfo? {
        recordingJob?.cancel()
        recordingJob = null
        currentSession?.let { session ->
            session.endTime = System.currentTimeMillis()
            session.sampleCount = sampleIndex.get()
            saveSessionMetadata(session)
            listeners.forEach { it.onRecordingStopped(session) }
            Log.i(
                TAG,
                "Simulated GSR recording stopped: sessionId=${session.sessionId}, samples=${session.sampleCount}"
            )
        }
        cleanup()
        val completedSession = currentSession
        currentSession = null
        return completedSession
    }
    fun triggerSyncEvent(
        eventType: String,
        metadata: String = "",
    ): Boolean {
        if (!isRecording.get()) return false
        return if (useShimmerDevice) {
            shimmerRecorder.triggerSyncEvent(eventType, metadata)
        } else {
            triggerSimulatedSyncEvent(eventType, metadata)
        }
    }
    private fun triggerSimulatedSyncEvent(
        eventType: String,
        metadata: String,
    ): Boolean {
        try {
            currentSession?.let { session ->
                val syncMark =
                    SyncMark(
                        timestamp = System.currentTimeMillis(),
                        utcTimestamp = TimeUtils.getUtcTimestamp(),
                        eventType = eventType,
                        sessionId = session.sessionId,
                        metadata = if (metadata.isNotEmpty()) mapOf("data" to metadata) else emptyMap(),
                    )
                syncMarksWriter?.writeNext(syncMark.toCsvRow())
                syncMarksWriter?.flush()
                listeners.forEach { it.onSyncMarkAdded(syncMark) }
                Log.d(TAG, "Sync event recorded: $eventType")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording sync event", e)
            notifyError("Error recording sync event: ${e.message}")
        }
        return false
    }
    private fun createSessionDirectory(sessionId: String): File? {
        return try {
            val externalStorage = Environment.getExternalStorageDirectory()
            val sessionsDir = File(externalStorage, SESSIONS_DIR)
            val sessionDir = File(sessionsDir, sessionId)
            if (!sessionDir.exists() && !sessionDir.mkdirs()) {
                Log.e(TAG, "Failed to create session directory: ${sessionDir.absolutePath}")
                return null
            }
            Log.d(TAG, "Created session directory: ${sessionDir.absolutePath}")
            sessionDir
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session directory", e)
            null
        }
    }
    private fun initializeCsvWriters(): Boolean {
        return try {
            sessionDirectory?.let { dir ->
                val signalsFile = File(dir, SIGNALS_FILENAME)
                signalsWriter =
                    CSVWriter(FileWriter(signalsFile)).apply {
                        writeNext(SIGNALS_HEADER)
                        flush()
                    }
                val syncMarksFile = File(dir, SYNC_MARKS_FILENAME)
                syncMarksWriter =
                    CSVWriter(FileWriter(syncMarksFile)).apply {
                        writeNext(SYNC_MARKS_HEADER)
                        flush()
                    }
                true
            } ?: false
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize CSV writers", e)
            false
        }
    }
    private fun saveSessionMetadata(session: SessionInfo) {
        try {
            sessionDirectory?.let { dir ->
                val metadataFile = File(dir, SESSION_METADATA_FILENAME)
                val gson = com.google.gson.Gson()
                val json = gson.toJson(session)
                metadataFile.writeText(json)
                Log.d(TAG, "Session metadata saved")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session metadata", e)
        }
    }
    private fun cleanup() {
        try {
            signalsWriter?.close()
            syncMarksWriter?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing CSV writers", e)
        } finally {
            signalsWriter = null
            syncMarksWriter = null
        }
    }
    private fun notifyError(error: String) {
        Log.e(TAG, error)
        listeners.forEach { it.onError(error) }
    }
    fun disconnect() {
        if (useShimmerDevice) {
            shimmerRecorder.disconnect()
        }
        if (isRecording.get()) {
            stopRecording()
        }
    }
    fun isDeviceConnected(): Boolean {
        return if (useShimmerDevice) {
            shimmerRecorder.isDeviceConnected()
        } else {
            true
        }
    }
    fun isRecording(): Boolean {
        return isRecording.get()
    }
    fun getCurrentSession(): SessionInfo? {
        return currentSession
    }
    fun getSessionDirectory(): File? {
        return sessionDirectory
    }
    suspend fun addSyncMark(
        eventType: String,
        metadata: String = "",
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isRecording.get()) {
                Log.w(TAG, "Cannot add sync mark - recording not active")
                return@withContext false
            }
            return@withContext if (useShimmerDevice) {
                shimmerRecorder.triggerSyncEvent(eventType, metadata)
            } else {
                triggerSimulatedSyncEvent(eventType, metadata)
            }
        }
}
