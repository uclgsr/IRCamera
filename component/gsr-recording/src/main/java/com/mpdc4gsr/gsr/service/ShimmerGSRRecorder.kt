package com.mpdc4gsr.gsr.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.util.TimeUtils
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ShimmerGSRRecorder(
    private val context: Context,
    private val shimmerDeviceFactory: ShimmerDeviceFactory,
    private val samplingRateHz: Int = 128,
    private val recordingMode: RecordingMode = RecordingMode.STREAMING,
) {
    enum class RecordingMode {
        STREAMING,
        LOGGING,
        LOG_AND_STREAM
    }

    companion object {
        private const val TAG = "ShimmerGSRRecorder"
        private const val SESSIONS_DIR = "IRCamera_Sessions"
        private const val SIGNALS_FILENAME = "signals.csv"
        private const val SYNC_MARKS_FILENAME = "sync_marks.csv"
        private const val SESSION_METADATA_FILENAME = "session_metadata.json"
        private const val ADC_12BIT_MAX = 4095
        private const val GSR_SENSOR_BIT = 0x08.toByte()
        private const val GSR_RANGE_AUTO = 0x00.toByte()
        private const val TIMESTAMP_CHANNEL_BIT = 0x01.toByte()
        private const val SENSOR_GSR_BIT = 0x10L
        private const val SENSOR_TIMESTAMP_BIT = 0x08L
        private val SIGNALS_HEADER =
            arrayOf(
                "timestamp_ms",
                "utc_timestamp_ms",
                "conductance_us",
                "resistance_kohms",
                "raw_value",
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

    private val isRecording = AtomicBoolean(false)
    private val sampleIndex = AtomicLong(0)
    private val isDeviceConnected = AtomicBoolean(false)
    private var shimmerDevice: ShimmerDeviceInterface? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var currentSession: SessionInfo? = null
    private var sessionDirectory: File? = null
    private var signalsWriter: CSVWriter? = null
    private var syncMarksWriter: CSVWriter? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<GSRRecordingListener>()
    private val shimmerAPIBridge = ShimmerApiBridge.getInstance()

    interface GSRRecordingListener {
        fun onRecordingStarted(session: SessionInfo)
        fun onRecordingStopped(session: SessionInfo)
        fun onSampleRecorded(sample: GSRSample)
        fun onSyncMarkRecorded(syncMark: SyncMark)
        fun onError(error: String)
        fun onDeviceConnected()
        fun onDeviceDisconnected()
    }

    fun addListener(listener: GSRRecordingListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GSRRecordingListener) {
        listeners.remove(listener)
    }

    suspend fun initializeDevice(deviceAddress: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothAdapter = bluetoothManager.adapter
                if (bluetoothAdapter?.isEnabled != true) {
                    Log.w(TAG, "Bluetooth is not enabled")
                    notifyError("Bluetooth is not enabled")
                    return@withContext false
                }
                shimmerDevice = shimmerDeviceFactory.createShimmerDevice()
                Log.i(TAG, "Shimmer API Bridge: ${shimmerAPIBridge.getProcessingInfo()}")
                Log.i(
                    TAG,
                    "Official processing available: ${shimmerAPIBridge.isOfficialProcessingAvailable()}"
                )
                shimmerDevice?.let { device ->
                    device.setDataCallback { dataCluster ->
                        handleShimmerData(dataCluster)
                    }
                    device.setConnectionCallback { connectionState ->
                        when (connectionState) {
                            "CONNECTED" -> {
                                isDeviceConnected.set(true)
                                Log.i(TAG, "Shimmer device connected")
                                listeners.forEach { it.onDeviceConnected() }
                            }

                            "DISCONNECTED" -> {
                                isDeviceConnected.set(false)
                                Log.w(TAG, "Shimmer device disconnected")
                                listeners.forEach { it.onDeviceDisconnected() }
                            }

                            else -> {
                                Log.d(TAG, "Shimmer connection state: $connectionState")
                            }
                        }
                    }
                    try {
                        Log.d(TAG, "Using official Shimmer API configuration")
                    } catch (e: Exception) {
                        Log.w(TAG, "Enhanced configuration failed, using defaults: ${e.message}")
                    }
                    if (deviceAddress != null) {
                        device.connect(deviceAddress, "default")
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                            notifyError("BLUETOOTH_CONNECT permission not granted")
                            return@withContext false
                        }
                        val pairedDevices = bluetoothAdapter?.bondedDevices
                        val shimmerDevice =
                            pairedDevices?.find {
                                it.name?.contains("Shimmer", ignoreCase = true) == true
                            }
                        if (shimmerDevice != null) {
                            device.connect(shimmerDevice.address, "default")
                        } else {
                            Log.w(TAG, "No paired Shimmer devices found")
                            notifyError("No paired Shimmer devices found")
                            return@withContext false
                        }
                    }
                    var attempts = 0
                    while (!isDeviceConnected.get() && attempts < 50) {
                        delay(200)
                        attempts++
                    }
                    if (isDeviceConnected.get()) {
                        Log.i(TAG, "Shimmer device connected successfully")
                        listeners.forEach { it.onDeviceConnected() }
                        return@withContext true
                    } else {
                        Log.w(TAG, "Failed to connect to Shimmer device")
                        notifyError("Failed to connect to Shimmer device")
                        return@withContext false
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Shimmer device", e)
                notifyError("Error initializing device: ${e.message}")
                false
            }
        }

    suspend fun startRecording(sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (isRecording.get()) {
                Log.w(TAG, "Recording already in progress")
                return@withContext false
            }
            if (!isDeviceConnected.get()) {
                Log.w(TAG, "Shimmer device not connected")
                notifyError("Shimmer device not connected")
                return@withContext false
            }
            try {
                sessionDirectory = createSessionDirectory(sessionId)
                if (sessionDirectory == null) {
                    notifyError("Failed to create session directory")
                    return@withContext false
                }
                if (!initializeCsvWriters()) {
                    notifyError("Failed to initialize CSV writers")
                    return@withContext false
                }
                currentSession =
                    SessionInfo(
                        sessionId = sessionId,
                        startTime = System.currentTimeMillis(),
                        participantId = null,
                        studyName = "Shimmer3_GSR_Study",
                    )
                sampleIndex.set(0)
                isRecording.set(true)
                shimmerDevice?.startStreaming()
                currentSession?.let { session ->
                    listeners.forEach { it.onRecordingStarted(session) }
                }
                Log.i(
                    TAG,
                    "Shimmer GSR recording started: sessionId=$sessionId, samplingRate=${samplingRateHz}Hz"
                )
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                cleanup()
                notifyError("Failed to start recording: ${e.message}")
                return@withContext false
            }
        }

    fun stopRecording(): SessionInfo? {
        if (!isRecording.get()) {
            Log.w(TAG, "No recording in progress")
            return currentSession
        }
        isRecording.set(false)
        shimmerDevice?.stopStreaming()
        currentSession?.let { session ->
            session.endTime = System.currentTimeMillis()
            session.sampleCount = sampleIndex.get()
            saveSessionMetadata(session)
            listeners.forEach { it.onRecordingStopped(session) }
            Log.i(
                TAG,
                "Shimmer GSR recording stopped: sessionId=${session.sessionId}, samples=${session.sampleCount}"
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
                listeners.forEach { it.onSyncMarkRecorded(syncMark) }
                Log.d(TAG, "Sync event recorded: $eventType")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording sync event", e)
            notifyError("Error recording sync event: ${e.message}")
        }
        return false
    }

    private fun handleShimmerData(dataCluster: ShimmerDataCluster) {
        if (!isRecording.get()) return
        try {
            val currentTime = System.currentTimeMillis()
            val utcTime = TimeUtils.getUtcTimestamp()
            val currentIndex = sampleIndex.getAndIncrement()
            currentSession?.let { session ->
                val rawGSRValue = dataCluster.getGSRRawValue()
                val sample =
                    shimmerAPIBridge.processGSRData(
                        rawValue = rawGSRValue,
                        timestamp = currentTime,
                        sessionId = session.sessionId,
                    ).copy(
                        utcTimestamp = utcTime,
                        sampleIndex = currentIndex,
                    )
                signalsWriter?.writeNext(sample.toCsvRow())
                if (currentIndex % 10 == 0L) {
                    signalsWriter?.flush()
                }
                listeners.forEach { it.onSampleRecorded(sample) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Shimmer data", e)
            notifyError("Error processing Shimmer data: ${e.message}")
        }
    }

    private fun createGSRConfiguration(): ByteArray {
        try {
            val config = ByteArray(12)
            val samplingRateConfig =
                when (samplingRateHz) {
                    128 -> 0x04.toByte()
                    256 -> 0x03.toByte()
                    512 -> 0x02.toByte()
                    1024 -> 0x01.toByte()
                    else -> 0x04.toByte()
                }
            config[0] = samplingRateConfig
            config[1] = 0x08.toByte()
            config[3] = TIMESTAMP_CHANNEL_BIT
            Log.d(
                TAG,
                "Created enhanced GSR configuration: ${samplingRateHz}Hz sampling, auto-range GSR, timestamp enabled"
            )
            return config
        } catch (e: Exception) {
            Log.w(TAG, "Using default GSR configuration due to error", e)
            return ByteArray(12) { if (it == 1) 0x08.toByte() else 0x00.toByte() }
        }
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
            signalsWriter = null
            syncMarksWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
    }

    private fun notifyError(message: String) {
        listeners.forEach { it.onError(message) }
    }

    fun disconnect() {
        if (isRecording.get()) {
            stopRecording()
        }
        shimmerDevice?.disconnect()
        isDeviceConnected.set(false)
        listeners.forEach { it.onDeviceDisconnected() }
        Log.i(TAG, "Shimmer device disconnected")
    }

    fun isRecording(): Boolean = isRecording.get()
    fun isDeviceConnected(): Boolean = isDeviceConnected.get()
    private fun startShimmerLogging() {
        try {
            // Shimmer internal logging is not implemented in this wrapper
            Log.i(TAG, "Shimmer internal logging not supported in this implementation")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start Shimmer logging: ${e.message}")
        }
    }

    private fun stopShimmerLogging() {
        try {
            // Shimmer internal logging is not implemented in this wrapper
            Log.i(TAG, "Shimmer internal logging not supported in this implementation")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop Shimmer logging: ${e.message}")
        }
    }

    fun getRecordingMode(): RecordingMode = recordingMode
}
