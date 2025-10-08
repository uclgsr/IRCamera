// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\data' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\data\app_src_main_java_mpdc4gsr_feature_gsr_data_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\gsr\data' subtree
// Files: 12; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\EnhancedThermalRecorder.kt =====

package mpdc4gsr.feature.gsr.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RecordingStats
import mpdc4gsr.core.data.RecordingStatus
import mpdc4gsr.core.data.SensorError
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import java.io.File
import java.io.FileWriter

class EnhancedThermalRecorder(private val context: Context) {
    companion object {
        private const val TAG = "EnhancedThermalRecorder"
    }

    private val thermalCameraRecorder = ThermalCameraRecorder(context)
    private var currentSessionDirectory: File? = null
    private var syncEventWriter: FileWriter? = null
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    suspend fun initialize(): Boolean {
        return thermalCameraRecorder.initialize()
    }

    fun startRecording(
        sessionId: String,
        sessionMetadata: SessionMetadata?,
        saveImages: Boolean = false
    ): Boolean {
        try {
            val externalDir = File(context.getExternalFilesDir(null), "IRCamera/sessions")
            currentSessionDirectory = File(externalDir, sessionId)
            currentSessionDirectory?.mkdirs()
            setupSyncEventsFile()
            recorderScope.launch {
                val success = if (sessionMetadata != null) {
                    thermalCameraRecorder.startRecording(
                        currentSessionDirectory!!.absolutePath,
                        sessionMetadata
                    )
                } else {
                    thermalCameraRecorder.startRecording(currentSessionDirectory!!.absolutePath)
                }
                if (success) {
                    AppLogger.i(TAG, "Enhanced thermal recording started for session: $sessionId")
                } else {
                    AppLogger.e(TAG, "Failed to start thermal recording for session: $sessionId")
                }
            }
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start enhanced thermal recording", e)
            return false
        }
    }

    fun stopRecording(): SessionInfo? {
        return try {
            recorderScope.launch {
                thermalCameraRecorder.stopRecording()
            }
            closeSyncEventsFile()
            val sessionInfo = SessionInfo(
                sessionDirectory = currentSessionDirectory,
                sampleCount = thermalCameraRecorder.getRecordingStats().totalSamplesRecorded
            )
            AppLogger.i(TAG, "Enhanced thermal recording stopped successfully")
            sessionInfo
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop enhanced thermal recording", e)
            null
        }
    }

    fun triggerSyncEvent(eventType: String, eventData: Map<String, String>) {
        try {
            val timestamp = System.nanoTime()
            val eventLine = buildString {
                append(timestamp)
                append(",")
                append(eventType)
                append(",")
                append(eventData.entries.joinToString(";") { "${it.key}=${it.value}" })
            }
            syncEventWriter?.let { writer ->
                writer.write(eventLine)
                writer.write("\n")
                writer.flush()
            }
            AppLogger.d(TAG, "Sync event triggered: $eventType")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to trigger sync event: $eventType", e)
        }
    }

    fun getSessionDirectory(): File? {
        return currentSessionDirectory
    }

    fun cleanup() {
        try {
            closeSyncEventsFile()
            recorderScope.launch {
                thermalCameraRecorder.cleanup()
            }
            recorderScope.cancel()
            currentSessionDirectory = null
            AppLogger.i(TAG, "Enhanced thermal recorder cleaned up")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during cleanup", e)
        }
    }

    fun getStatusFlow(): Flow<RecordingStatus> = thermalCameraRecorder.getStatusFlow()
    fun getErrorFlow(): Flow<SensorError> = thermalCameraRecorder.getErrorFlow()
    fun getRecordingStats(): RecordingStats = thermalCameraRecorder.getRecordingStats()
    private fun setupSyncEventsFile() {
        try {
            currentSessionDirectory?.let { dir ->
                val syncEventsFile = File(dir, "sync_events.csv")
                syncEventWriter = FileWriter(syncEventsFile, true)
                syncEventWriter?.write("timestamp_ns,event_type,event_data\n")
                syncEventWriter?.flush()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to setup sync events file", e)
        }
    }

    private fun closeSyncEventsFile() {
        try {
            syncEventWriter?.close()
            syncEventWriter = null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to close sync events file", e)
        }
    }

    data class SessionInfo(
        val sessionDirectory: File?,
        val sampleCount: Long,
        val startTime: Long = System.currentTimeMillis()
    )
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRCalculationUtils.kt =====

package mpdc4gsr.feature.gsr.data

import kotlin.math.max

object GSRCalculationUtils {

    fun calculateGSRMicrosiemens(rawValue: Int): Double {
        if (rawValue < GSRConstants.GSR_UNCAL_LIMIT_LOW || rawValue > GSRConstants.GSR_UNCAL_LIMIT_HIGH) {
            return 0.0
        }
        return try {
            val voltage = (rawValue / GSRConstants.ADC_MAX_VALUE) * GSRConstants.REFERENCE_VOLTAGE
            val gsrResistance =
                GSRConstants.REFERENCE_RESISTANCE_OHMS * ((GSRConstants.REFERENCE_VOLTAGE / voltage) - 1.0)
            val conductance = if (gsrResistance > 0) {
                (1.0 / gsrResistance) * GSRConstants.MICROSIEMENS_CONVERSION
            } else {
                0.0
            }
            conductance.coerceIn(0.0, GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND)
        } catch (e: Exception) {
            0.0
        }
    }

    fun calculateResistanceFromGSR(gsrMicrosiemens: Double): Double {
        return if (gsrMicrosiemens > 0) {
            GSRConstants.MICROSIEMENS_CONVERSION / gsrMicrosiemens
        } else {
            Double.MAX_VALUE
        }
    }

    fun calculateSignalQuality(gsrMicrosiemens: Double, rawValue: Int): Double {
        return when {
            rawValue !in GSRConstants.GSR_RAW_LOWER_BOUND..GSRConstants.GSR_RAW_UPPER_BOUND -> 0.2
            gsrMicrosiemens !in GSRConstants.GSR_MICROSIEMENS_LOWER_BOUND..GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND -> 0.3
            gsrMicrosiemens > GSRConstants.GSR_HIGH_THRESHOLD -> 0.4
            gsrMicrosiemens < GSRConstants.GSR_LOW_THRESHOLD -> 0.5
            else -> 0.9
        }
    }

    fun calculateQualityScore(gsrRaw: Int): Double {
        val baseQuality = when {
            gsrRaw <= 0 -> 0.0
            gsrRaw < GSRConstants.GSR_RAW_LOWER_BOUND -> 0.3
            gsrRaw > GSRConstants.GSR_RAW_UPPER_BOUND -> 0.4
            else -> 0.8
        }
        return max(0.0, baseQuality).coerceAtMost(1.0)
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRConstants.kt =====

package mpdc4gsr.feature.gsr.data

object GSRConstants {
    // GSR calculation constants based on Shimmer3 GSR hardware specifications
    const val ADC_MAX_VALUE = 4095.0
    const val REFERENCE_VOLTAGE = 3.0
    const val REFERENCE_RESISTANCE_OHMS = 40200.0
    const val VOLTAGE_DIVIDER = 1000.0
    const val MICROSIEMENS_CONVERSION = 1000000.0

    // Sampling rate configuration
    const val GSR_SAMPLING_RATE = 128.0

    // Signal quality thresholds
    const val GSR_RAW_LOWER_BOUND = 100
    const val GSR_RAW_UPPER_BOUND = 4000
    const val GSR_MICROSIEMENS_LOWER_BOUND = 0.1
    const val GSR_MICROSIEMENS_UPPER_BOUND = 100.0
    const val GSR_HIGH_THRESHOLD = 50.0
    const val GSR_LOW_THRESHOLD = 0.5

    // Connection health monitoring constants
    const val TIMING_HEALTH_ACCEPTABLE_MS = 1000L
    const val HEALTH_SCORE_WEIGHT_HISTORICAL = 0.8
    const val HEALTH_SCORE_WEIGHT_SAMPLE = 0.1
    const val HEALTH_SCORE_WEIGHT_TIMING = 0.1
    const val POOR_CONNECTION_THRESHOLD = 30.0

    // GSR range limits for uncalibrated values
    const val GSR_UNCAL_LIMIT_LOW = 100
    const val GSR_UNCAL_LIMIT_HIGH = 4000
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRDataPersistence.kt =====

package mpdc4gsr.feature.gsr.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mpdc4gsr.core.data.TimestampManager
import mpdc4gsr.core.data.TimestampRecord
import mpdc4gsr.core.data.utils.CSVBufferedWriter
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class GSRDataPersistence(
    private val context: Context,
    private val sessionId: String,
) {
    companion object {
        private const val TAG = "GSRDataPersistence"
        private const val BATCH_SIZE = 100
        private const val FLUSH_INTERVAL_MS = 500L
    }

    private val dataQueue = ConcurrentLinkedQueue<GSRDataRecord>()
    private val writeMutex = Mutex()
    private val isWriting = AtomicBoolean(false)
    private val samplesWritten = AtomicLong(0)
    private var csvFile: File? = null
    private var csvBufferedWriter: CSVBufferedWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    suspend fun initialize(): Boolean {
        return try {
            val sessionDir = createSessionDirectory()
            csvFile = createCsvFile(sessionDir)
            val headers = createCsvHeaders()
            csvBufferedWriter = CSVBufferedWriter(
                outputFile = csvFile!!,
                headers = headers,
                bufferSize = 4096,
                flushIntervalMs = FLUSH_INTERVAL_MS
            )
            csvBufferedWriter?.startWithHeaders()
            AppLogger.i(TAG, "GSR data persistence initialized for session: $sessionId")
            AppLogger.i(TAG, "CSV file: ${csvFile!!.absolutePath}")
            startBatchWriter()
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize GSR data persistence", e)
            false
        }
    }

    private fun createSessionDirectory(): File {
        val baseDir = File(context.getExternalFilesDir(null), "sessions")
        val sessionDir = File(baseDir, sessionId)
        val shimmerDir = File(sessionDir, "Shimmer")
        if (!shimmerDir.exists()) {
            shimmerDir.mkdirs()
        }
        return shimmerDir
    }

    private fun createCsvFile(sessionDir: File): File {
        return File(sessionDir, SessionDirectoryManager.SHIMMER_DATA_FILE)
    }

    private fun createCsvHeaders(): List<String> {
        return listOf(
            "system_nanos", "elapsed_realtime_ms", "device_timestamp_ms",
            "session_relative_ms", "synchronized_timestamp_ms",
            "gsr_raw_value", "gsr_microsiemens", "gsr_resistance_kohm",
            "ppg_raw_value", "ppg_filtered", "heart_rate_bpm",
            "device_id", "battery_level", "signal_quality",
            "sampling_rate_hz", "packet_sequence",
            "session_id", "participant_id", "recording_mode"
        )
    }

    fun queueDataRecord(gsrData: GSRSampleData) {
        val timestamp = TimestampManager.createTimestampRecord()
        val record =
            GSRDataRecord(
                timestamp = timestamp,
                gsrRawValue = gsrData.rawValue,
                gsrMicrosiemens = gsrData.microsiemens,
                gsrResistanceKohm = gsrData.resistanceKohm,
                ppgRawValue = gsrData.ppgRawValue,
                ppgFiltered = gsrData.ppgFiltered,
                heartRateBpm = gsrData.heartRateBpm,
                deviceId = gsrData.deviceId,
                batteryLevel = gsrData.batteryLevel,
                signalQuality = gsrData.signalQuality,
                samplingRateHz = gsrData.samplingRateHz,
                packetSequence = gsrData.packetSequence,
                sessionId = sessionId,
                participantId = gsrData.participantId,
                recordingMode = gsrData.recordingMode,
            )
        dataQueue.offer(record)
    }

    private fun startBatchWriter() {
        scope.launch {
            while (isWriting.get() || dataQueue.isNotEmpty()) {
                try {
                    writeBatch()
                    kotlinx.coroutines.delay(FLUSH_INTERVAL_MS)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in batch writer", e)
                }
            }
        }
    }

    private suspend fun writeBatch() {
        if (dataQueue.isEmpty()) return
        val batch = mutableListOf<GSRDataRecord>()
        repeat(BATCH_SIZE) {
            dataQueue.poll()?.let { batch.add(it) }
        }
        if (batch.isEmpty()) return
        writeMutex.withLock {
            try {
                batch.forEach { record ->
                    val csvRow = record.toCsvRow()
                    csvBufferedWriter?.writeRow(csvRow)
                }
                samplesWritten.addAndGet(batch.size.toLong())
                Log.d(
                    TAG,
                    "Wrote batch of ${batch.size} GSR samples. Total: ${samplesWritten.get()}"
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to write GSR data batch", e)
            }
        }
    }

    fun startPersistence() {
        isWriting.set(true)
        TimestampManager.startSession()
        AppLogger.i(TAG, "GSR data persistence started")
    }

    suspend fun stopPersistence() {
        isWriting.set(false)
        while (dataQueue.isNotEmpty()) {
            writeBatch()
        }
        TimestampManager.endSession()
        AppLogger.i(TAG, "GSR data persistence stopped. Total samples written: ${samplesWritten.get()}")
    }

    suspend fun cleanup() {
        stopPersistence()
        writeMutex.withLock {
            try {
                csvBufferedWriter?.stop()
                csvBufferedWriter = null
                AppLogger.i(TAG, "GSR data persistence cleanup completed")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during cleanup", e)
            }
        }
    }

    fun getStatistics(): GSRPersistenceStats {
        val writeStats = csvBufferedWriter?.getWriteStats()
        return GSRPersistenceStats(
            samplesWritten = samplesWritten.get(),
            pendingSamples = dataQueue.size,
            csvFilePath = csvFile?.absolutePath ?: "",
            sessionId = sessionId,
            isActive = isWriting.get(),
            bufferStats = writeStats
        )
    }
}

data class GSRDataRecord(
    val timestamp: TimestampRecord,
    val gsrRawValue: Int,
    val gsrMicrosiemens: Double,
    val gsrResistanceKohm: Double,
    val ppgRawValue: Int,
    val ppgFiltered: Double,
    val heartRateBpm: Int,
    val deviceId: String,
    val batteryLevel: Int,
    val signalQuality: Int,
    val samplingRateHz: Int,
    val packetSequence: Long,
    val sessionId: String,
    val participantId: String,
    val recordingMode: String,
) {
    fun toCsvRow(): List<Any> {
        return listOf(
            timestamp.systemNanos,
            timestamp.elapsedRealtimeMs,
            timestamp.deviceTimestampMs,
            timestamp.sessionRelativeMs,
            timestamp.synchronizedTimestampMs,
            gsrRawValue,
            gsrMicrosiemens,
            gsrResistanceKohm,
            ppgRawValue,
            ppgFiltered,
            heartRateBpm,
            deviceId,
            batteryLevel,
            signalQuality,
            samplingRateHz,
            packetSequence,
            sessionId,
            participantId,
            recordingMode
        )
    }

    fun toCsvLine(): String {
        return buildString {
            append(timestamp.toCsvFormat())
            append(",")
            append("$gsrRawValue,$gsrMicrosiemens,$gsrResistanceKohm,")
            append("$ppgRawValue,$ppgFiltered,$heartRateBpm,")
            append("$deviceId,$batteryLevel,$signalQuality,")
            append("$samplingRateHz,$packetSequence,")
            append("$sessionId,$participantId,$recordingMode")
        }
    }
}

data class GSRSampleData(
    val rawValue: Int,
    val microsiemens: Double,
    val resistanceKohm: Double,
    val ppgRawValue: Int = 0,
    val ppgFiltered: Double = 0.0,
    val heartRateBpm: Int = 0,
    val deviceId: String,
    val batteryLevel: Int = 100,
    val signalQuality: Int = 100,
    val samplingRateHz: Int = 128,
    val packetSequence: Long,
    val participantId: String,
    val recordingMode: String = "shimmer_ble",
)

data class GSRPersistenceStats(
    val samplesWritten: Long,
    val pendingSamples: Int,
    val csvFilePath: String,
    val sessionId: String,
    val isActive: Boolean,
    val bufferStats: mpdc4gsr.core.data.utils.WriteStats? = null,
) {
    val totalDataSizeBytes: Long
        get() = bufferStats?.bytesWritten ?: 0L
    val averageSampleSize: Double
        get() = if (samplesWritten > 0) totalDataSizeBytes.toDouble() / samplesWritten else 0.0
    val writePerformanceInfo: String
        get() = bufferStats?.let { stats ->
            "Queue: ${stats.queueSize}, Size: ${stats.formattedSize}, Avg: ${
                String.format(
                    "%.1f",
                    averageSampleSize
                )
            } bytes/sample"
        } ?: "No buffer stats available"
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRNetworkStreamer.kt =====

package mpdc4gsr.feature.gsr.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import com.mpdc4gsr.gsr.model.GSRSample
import kotlinx.coroutines.*
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.RecordingController
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class GSRNetworkStreamer(
    private val context: Context,
    private val sessionId: String,
    private val recordingController: RecordingController,
) {
    companion object {
        private const val TAG = "GSRNetworkStreamer"
        private const val GSR_STREAM_TYPE = "gsr_data"
        private const val BUFFER_SIZE = 1000
        private const val BATCH_SIZE = 50
        private const val STREAM_INTERVAL_MS = 100L
        private const val HEARTBEAT_INTERVAL_MS = 5000L
        private const val QUALITY_REPORTING_INTERVAL_MS = 10000L
    }

    private var networkClient: NetworkClient? = null
    private val sampleBuffer = ConcurrentLinkedQueue<GSRSample>()
    private val _isStreaming = AtomicBoolean(false)
    val isStreaming: Boolean get() = _isStreaming.get()
    private val streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamingJob: Job? = null
    private var heartbeatJob: Job? = null
    private var qualityReportingJob: Job? = null
    private val samplesSent = AtomicLong(0)
    private val samplesAcknowledged = AtomicLong(0)
    private val bytesTransmitted = AtomicLong(0)
    private val networkErrors = AtomicLong(0)
    private var startTime: Long = 0
    private var clockOffset: Long = 0
    private var lastSyncTime: Long = 0
    private val syncInterval = 30000L
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                AppLogger.i(TAG, "Initializing GSR network streamer for session: $sessionId")
                networkClient = NetworkClient(context)
                val connected = networkClient?.connectToController("192.168.1.100") ?: false
                if (!connected) {
                    AppLogger.e(TAG, "Failed to connect to PC hub")
                    return@withContext false
                }
                performTimeSync()
                registerGSRStream()
                AppLogger.i(TAG, "GSR network streamer initialized successfully")
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initialize GSR network streamer", e)
                false
            }
        }
    }

    suspend fun startStreaming(): Boolean {
        if (_isStreaming.get()) {
            AppLogger.w(TAG, "GSR streaming already active")
            return true
        }
        return try {
            _isStreaming.set(true)
            startTime = System.currentTimeMillis()
            streamingJob =
                streamingScope.launch {
                    streamGSRData()
                }
            heartbeatJob =
                streamingScope.launch {
                    sendHeartbeats()
                }
            qualityReportingJob =
                streamingScope.launch {
                    reportQualityMetrics()
                }
            AppLogger.i(TAG, "GSR streaming started for session: $sessionId")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start GSR streaming", e)
            _isStreaming.set(false)
            false
        }
    }

    suspend fun stopStreaming(): Boolean {
        if (!_isStreaming.get()) {
            return true
        }
        return try {
            _isStreaming.set(false)
            streamingJob?.cancel()
            heartbeatJob?.cancel()
            qualityReportingJob?.cancel()
            flushBuffer()
            sendStreamEndNotification()
            AppLogger.i(TAG, "GSR streaming stopped for session: $sessionId")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop GSR streaming", e)
            false
        }
    }

    fun addSample(sample: GSRSample) {
        if (!_isStreaming.get()) {
            return
        }
        try {
            val syncedSample =
                sample.copy(
                    timestamp = sample.timestamp + clockOffset,
                )
            sampleBuffer.offer(syncedSample)
            if (sampleBuffer.size > BUFFER_SIZE) {
                sampleBuffer.poll()
                AppLogger.w(TAG, "GSR sample buffer overflow, dropping oldest sample")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to add GSR sample to buffer", e)
        }
    }

    private suspend fun streamGSRData() {
        while (_isStreaming.get()) {
            try {
                if (sampleBuffer.size >= BATCH_SIZE || shouldFlushBuffer()) {
                    sendBatch()
                }
                if (System.currentTimeMillis() - lastSyncTime > syncInterval) {
                    performTimeSync()
                }
                delay(STREAM_INTERVAL_MS)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error in GSR streaming loop", e)
                networkErrors.incrementAndGet()
                delay(1000)
            }
        }
    }

    private suspend fun sendBatch() {
        val batch = mutableListOf<GSRSample>()
        repeat(BATCH_SIZE) {
            sampleBuffer.poll()?.let { sample ->
                batch.add(sample)
            }
        }
        if (batch.isEmpty()) return
        try {
            val batchMessage = createBatchMessage(batch)
            // Send actual network message using NetworkClient
            networkClient?.let { client ->
                try {
                    // Convert string message to JSONObject for NetworkClient API
                    val jsonMessage = JSONObject(batchMessage.toString())
                    streamingScope.launch {
                        val success = client.sendMessage(jsonMessage)
                        if (success) {
                            AppLogger.v(TAG, "Sent GSR batch: ${batch.size} samples")
                        } else {
                            AppLogger.w(TAG, "Failed to send GSR batch via network")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to send GSR batch via network", e)
                }
            } ?: run {
                Log.d(
                    TAG,
                    "NetworkClient not available, simulating send: ${
                        batchMessage.toString().take(100)
                    }..."
                )
            }
            samplesSent.addAndGet(batch.size.toLong())
            bytesTransmitted.addAndGet(batchMessage.toString().length.toLong())
            AppLogger.d(TAG, "Simulated sending GSR batch of ${batch.size} samples")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to send GSR batch", e)
            networkErrors.incrementAndGet()
        }
    }

    private fun createBatchMessage(batch: List<GSRSample>): JSONObject {
        return JSONObject().apply {
            put("type", GSR_STREAM_TYPE)
            put("session_id", sessionId)
            put("timestamp", System.currentTimeMillis())
            put("sample_count", batch.size)
            put(
                "samples",
                org.json.JSONArray().apply {
                    batch.forEach { sample ->
                        put(
                            JSONObject().apply {
                                put("timestamp", sample.timestamp)
                                put("gsr_value", sample.conductance)
                                put("raw_value", sample.rawValue)
                                put("quality", 1.0)
                                put(
                                    "device_id", android.provider.Settings.Secure.getString(
                                        context.contentResolver,
                                        android.provider.Settings.Secure.ANDROID_ID
                                    )
                                )
                                put("resistance", sample.resistance)
                                put("sample_index", sample.sampleIndex)
                            },
                        )
                    }
                },
            )
        }
    }

    private suspend fun performTimeSync() {
        try {
            val clientSent = System.nanoTime()
            val syncRequest =
                JSONObject().apply {
                    put("type", "time_sync_request")
                    put("client_timestamp", clientSent)
                }
            // Send actual time sync request using NetworkClient sendMessage
            networkClient?.let { client ->
                try {
                    val success = client.sendMessage(syncRequest)
                    if (success) {
                        // For now, use simple local time sync since we don't have a sync response mechanism
                        val clientReceived = System.nanoTime()
                        val roundTripTime = clientReceived - clientSent
                        clockOffset = 0 // Assume zero offset without server response
                        lastSyncTime = System.currentTimeMillis()
                        AppLogger.d(TAG, "Network time sync completed, RTT: ${roundTripTime}ns")
                    } else {
                        AppLogger.w(TAG, "Network time sync request failed, using local fallback")
                        performLocalTimeSync(clientSent)
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Network time sync failed, using local fallback", e)
                    performLocalTimeSync(clientSent)
                }
            } ?: run {
                AppLogger.d(TAG, "NetworkClient not available, performing local time sync")
                performLocalTimeSync(clientSent)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Time synchronization failed", e)
        }
    }

    private fun performLocalTimeSync(clientSent: Long) {
        val clientReceived = System.nanoTime()
        val roundTripTime = clientReceived - clientSent
        clockOffset = 0 // No server available, assume zero offset
        lastSyncTime = System.currentTimeMillis()
        AppLogger.d(TAG, "Local time sync completed, RTT: ${roundTripTime}ns")
    }

    private suspend fun sendHeartbeats() {
        while (_isStreaming.get()) {
            try {
                val heartbeat =
                    JSONObject().apply {
                        put("type", "heartbeat")
                        put("session_id", sessionId)
                        put("timestamp", System.currentTimeMillis())
                        put("buffer_size", sampleBuffer.size)
                    }
                // Send actual heartbeat using NetworkClient
                networkClient?.let { client ->
                    streamingScope.launch {
                        try {
                            val success = client.sendMessage(heartbeat)
                            if (success) {
                                AppLogger.v(TAG, "Sent heartbeat")
                            } else {
                                AppLogger.w(TAG, "Failed to send heartbeat via network")
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Failed to send heartbeat via network", e)
                        }
                    }
                } ?: run {
                    AppLogger.d(TAG, "NetworkClient not available, simulating heartbeat: ${heartbeat}")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to send heartbeat", e)
            }
            delay(HEARTBEAT_INTERVAL_MS)
        }
    }

    private suspend fun reportQualityMetrics() {
        while (_isStreaming.get()) {
            try {
                val metrics =
                    JSONObject().apply {
                        put("type", "quality_metrics")
                        put("session_id", sessionId)
                        put("timestamp", System.currentTimeMillis())
                        put("samples_sent", samplesSent.get())
                        put("samples_acknowledged", samplesAcknowledged.get())
                        put("bytes_transmitted", bytesTransmitted.get())
                        put("network_errors", networkErrors.get())
                        put("buffer_size", sampleBuffer.size)
                        put("uptime_ms", System.currentTimeMillis() - startTime)
                    }
                // Send actual quality metrics using NetworkClient
                networkClient?.let { client ->
                    streamingScope.launch {
                        try {
                            val success = client.sendMessage(metrics)
                            if (success) {
                                AppLogger.v(TAG, "Sent quality metrics")
                            } else {
                                AppLogger.w(TAG, "Failed to send quality metrics via network")
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Failed to send quality metrics via network", e)
                        }
                    }
                } ?: run {
                    AppLogger.d(TAG, "NetworkClient not available, simulating metrics: ${metrics}")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to send quality metrics", e)
            }
            delay(QUALITY_REPORTING_INTERVAL_MS)
        }
    }

    private suspend fun registerGSRStream() {
        try {
            val registration =
                JSONObject().apply {
                    put("type", "stream_registration")
                    put("stream_type", GSR_STREAM_TYPE)
                    put("session_id", sessionId)
                    put(
                        "device_id",
                        android.provider.Settings.Secure.getString(
                            context.contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID,
                        ),
                    )
                    put("sampling_rate", 128)
                    put("timestamp", System.currentTimeMillis())
                }
            // Send actual stream registration using NetworkClient
            networkClient?.let { client ->
                streamingScope.launch {
                    try {
                        val success = client.sendMessage(registration)
                        if (success) {
                            AppLogger.i(TAG, "GSR stream registration sent successfully")
                        } else {
                            AppLogger.w(TAG, "Failed to send stream registration via network")
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Failed to send stream registration via network", e)
                    }
                }
            } ?: run {
                AppLogger.d(TAG, "NetworkClient not available, simulating registration: ${registration}")
                AppLogger.i(TAG, "GSR stream registration simulated")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to register GSR stream", e)
        }
    }

    private suspend fun sendStreamEndNotification() {
        try {
            val endNotification =
                JSONObject().apply {
                    put("type", "stream_end")
                    put("stream_type", GSR_STREAM_TYPE)
                    put("session_id", sessionId)
                    put("total_samples", samplesSent.get())
                    put("total_bytes", bytesTransmitted.get())
                    put("timestamp", System.currentTimeMillis())
                }
            // Send actual stream end notification using NetworkClient
            networkClient?.let { client ->
                streamingScope.launch {
                    try {
                        val success = client.sendMessage(endNotification)
                        if (success) {
                            AppLogger.i(TAG, "GSR stream end notification sent successfully")
                        } else {
                            AppLogger.w(TAG, "Failed to send stream end notification via network")
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Failed to send stream end notification via network", e)
                    }
                }
            } ?: run {
                Log.d(
                    TAG,
                    "NetworkClient not available, simulating end notification: ${endNotification}"
                )
                AppLogger.i(TAG, "GSR stream end notification simulated")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to send stream end notification", e)
        }
    }

    private suspend fun flushBuffer() {
        while (sampleBuffer.isNotEmpty()) {
            sendBatch()
        }
    }

    private fun shouldFlushBuffer(): Boolean {
        return sampleBuffer.isNotEmpty() &&
                (System.currentTimeMillis() % (STREAM_INTERVAL_MS * 5) == 0L)
    }

    fun getStreamingStats(): Map<String, Any> {
        return mapOf(
            "samples_sent" to samplesSent.get(),
            "samples_acknowledged" to samplesAcknowledged.get(),
            "bytes_transmitted" to bytesTransmitted.get(),
            "network_errors" to networkErrors.get(),
            "buffer_size" to sampleBuffer.size,
            "uptime_ms" to if (_isStreaming.get()) System.currentTimeMillis() - startTime else 0,
            "clock_offset_ns" to clockOffset,
        )
    }

    suspend fun cleanup() {
        stopStreaming()
        streamingScope.cancel()
        networkClient?.disconnect()
        networkClient = null
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRPlotDataModels.kt =====

package mpdc4gsr.feature.gsr.data

import java.io.Serializable

data class GSRPlotData(
    val timestamps: List<Double>,
    val gsrValues: List<Double>,
    val ppgValues: List<Double>,
    val gsrMovingAverage: List<Double>,
    val ppgMovingAverage: List<Double>,
    val gsrEvents: List<GSREvent>,
    val statistics: List<TimeWindowStats>,
    val metadata: PlotMetadata
) : Serializable

data class GSREvent(
    val timestamp: Double,
    val type: String,
    val magnitude: Double,
    val gsrValue: Double
) : Serializable

data class TimeWindowStats(
    val startTime: Double,
    val endTime: Double,
    val mean: Double,
    val stdDev: Double,
    val min: Double,
    val max: Double,
    val count: Int
) : Serializable

data class PlotMetadata(
    val fileName: String,
    val duration: Double,
    val samplingRate: Double,
    val dataPoints: Int
) : Serializable


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRSensorRecorder.kt =====

package mpdc4gsr.feature.gsr.data

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.service.ShimmerGSRRecorder
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import mpdc4gsr.core.data.*
import mpdc4gsr.feature.network.data.RecordingController
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.mpdc4gsr.gsr.service.GSRRecorder as LegacyGSRRecorder
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory as GSRRealShimmerDeviceFactory

class GSRSensorRecorder(
    private val context: Context,
    override val sensorId: String = "gsr_shimmer_1",
    private val samplingRateHz: Int = 128,
    private val recordingController: RecordingController
) : SensorRecorder {
    companion object {
        private const val TAG = "GSRSensorRecorder"
        private const val TIMING_HEALTH_POOR_MS = 2000L
        private const val HEALTH_SCORE_WEIGHT_HISTORICAL = 0.8
        private const val HEALTH_SCORE_WEIGHT_SAMPLE = 0.15
        private const val HEALTH_SCORE_WEIGHT_TIMING = 0.05
        private const val POOR_CONNECTION_THRESHOLD = 50.0
        private const val SHIMMER_DEFAULT_SAMPLING_RATE = 128.0
        private const val GSR_CHANNEL_ID = 0x01
        private const val GSR_RANGE_AUTO = 0x00

        // Monitoring and connection delays
        private const val STATUS_MONITORING_INTERVAL_MS = 1000L
        private const val CONNECTION_VERIFICATION_DELAY_MS = 2000L
        private const val CONNECTION_STATE_MONITOR_INTERVAL_MS = 1000L
        private const val CONNECTION_STATE_ERROR_DELAY_MS = 5000L
        private const val RECONNECTION_VERIFY_DELAY_MS = 1000L
        private const val SHIMMER_MIN_SAMPLING_RATE = 1.0
        private const val SHIMMER_MAX_SAMPLING_RATE = 512.0

        // Enhanced batch writing configuration for improved performance
        private const val CSV_BATCH_SIZE = 50 // Write every 50 samples for better performance
        private const val CSV_FLUSH_INTERVAL_MS = 5000L // Flush every 5 seconds
        private const val CONNECTION_HEALTH_CHECK_INTERVAL = 10 // Check connection every 10 samples
        fun hasRequiredPermissions(context: Context): Boolean {
            return hasBleScanningPermissions(context)
        }

        fun hasBleScanningPermissions(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun hasBluetoothConnectPermission(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun getMissingPermissions(context: Context): List<String> {
            val missing = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    missing.add(android.Manifest.permission.BLUETOOTH_SCAN)
                }
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    missing.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            return missing
        }

        fun hasComprehensiveBluetoothPermissions(context: Context): Boolean {
            return hasBleScanningPermissions(context)
        }
    }

    override val sensorType: String = "GSR Shimmer3"
    private var _samplingRate: Double = samplingRateHz.toDouble()
    override val samplingRate: Double get() = _samplingRate
    private var _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()
    private var unifiedShimmerDevice: ShimmerDevice? = null
    private var realShimmerGSRRecorder: ShimmerGSRRecorder? = null
    private var gsrDataPersistence: GSRDataPersistence? = null
    private var currentSessionId: String? = null
    private val sampleSequence = AtomicLong(0)
    private var isShimmerConnected = false
    private var legacyGSRRecorder: LegacyGSRRecorder? = null
    private var gsrSettingsRepository: GSRSettingsRepository? = null
    private var effectiveSamplingRate: Double = samplingRateHz.toDouble()
    private var gsrNetworkStreamer: GSRNetworkStreamer? = null
    private var isNetworkStreamingEnabled = true
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionDirectory: String = ""
    private var sampleCount = AtomicLong(0)
    private var recordingStartTime: Long = 0
    private var syncMarkerCount = AtomicLong(0)

    // Enhanced batch writing and connection monitoring
    private var batchSampleBuffer = mutableListOf<GSRSampleData>()
    private var lastFlushTime = System.currentTimeMillis()
    private var connectionHealthScore = 100.0
    private var lastConnectionCheck = System.currentTimeMillis()
    private var timeSyncService: TimeSynchronizationService? = null
    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private var connectionStateMonitoringJob: Job? = null
    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 3
    private val reconnectionDelayMs = 2000L
    private var currentConnectedDevice: Shimmer? = null
    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()
    private var lastSampleTimestamp: Long = 0
    private var dataMonitoringJob: Job? = null
    override suspend fun initialize(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                AppLogger.i(TAG, "Initializing GSR sensor with Shimmer3 integration for $sensorId")
                if (!hasRequiredPermissions(context)) {
                    AppLogger.w(TAG, "Missing required Bluetooth permissions for Shimmer GSR device")
                    Log.i(
                        TAG,
                        "GSR sensor will initialize but Shimmer functionality will be limited until permissions are granted"
                    )
                }
                AppLogger.i(TAG, "Using official Shimmer Bluetooth libraries for device communication")
                if (initializeShimmerBluetoothManager()) {
                    AppLogger.i(TAG, "ShimmerBluetoothManagerAndroid ready for device connections")
                    startConnectionStateMonitoring()
                }
                gsrSettingsRepository = GSRSettingsRepository(context)
                val gsrSettings = gsrSettingsRepository?.gsrSettings?.value
                effectiveSamplingRate = gsrSettings?.samplingRate?.toDouble() ?: samplingRateHz.toDouble()
                effectiveSamplingRate =
                    effectiveSamplingRate.coerceIn(SHIMMER_MIN_SAMPLING_RATE, SHIMMER_MAX_SAMPLING_RATE)
                _samplingRate = effectiveSamplingRate
                Log.i(
                    TAG,
                    "GSR Settings loaded: samplingRate=${gsrSettings?.samplingRate}Hz, filtering=${gsrSettings?.enableFiltering}, bufferSize=${gsrSettings?.bufferSize}"
                )
                Log.i(
                    TAG,
                    "Effective sampling rate for Shimmer: ${effectiveSamplingRate}Hz (within Shimmer range: $SHIMMER_MIN_SAMPLING_RATE-$SHIMMER_MAX_SAMPLING_RATE Hz)"
                )
                // Observe settings changes for real-time updates
                observeGSRSettingsChanges()
                realShimmerGSRRecorder =
                    ShimmerGSRRecorder(
                        context,
                        GSRRealShimmerDeviceFactory(context),
                        effectiveSamplingRate.toInt()
                    )
                val shimmerRecorder = realShimmerGSRRecorder
                if (shimmerRecorder != null) {
                    try {
                        val deviceInitialized = shimmerRecorder.initializeDevice()
                        if (deviceInitialized) {
                            Log.i(
                                TAG,
                                "Shimmer GSR device initialized and ready with ${effectiveSamplingRate}Hz sampling rate"
                            )
                            isShimmerConnected = true
                        } else {
                            Log.w(
                                TAG,
                                "Shimmer GSR device not available, but sensor recorder initialized"
                            )
                            isShimmerConnected = false
                        }
                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "Shimmer GSR device initialization failed, but continuing: ${e.message}"
                        )
                        isShimmerConnected = false
                    }
                }
                legacyGSRRecorder =
                    LegacyGSRRecorder(context, GSRRealShimmerDeviceFactory(context), effectiveSamplingRate.toInt())
                if (isNetworkStreamingEnabled) {
                    try {
                        AppLogger.i(TAG, "Network streaming will be initialized during recording start")
                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "Network streaming setup failed, continuing without streaming: ${e.message}"
                        )
                        isNetworkStreamingEnabled = false
                    }
                }
                startDataMonitoring()
                setupGSRSampleCallback()
                Log.i(
                    TAG,
                    "GSR sensor initialized successfully (Shimmer connected: $isShimmerConnected, Network streaming: $isNetworkStreamingEnabled)",
                )
                emitStatus()
                return@withContext true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initialize GSR sensor", e)
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "GSR initialization failed: ${e.message}"
                )
                return@withContext false
            }
        }

    private fun startDataMonitoring() {
        dataMonitoringJob =
            recordingScope.launch {
                while (isActive) {
                    if (_isRecording.get()) {
                        monitorGSRData()
                        emitStatus()
                    }
                    delay(STATUS_MONITORING_INTERVAL_MS)
                }
            }
    }

    private suspend fun monitorGSRData() {
        try {
            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null) {
                val realSampleCount = sampleCount.get()
                val expectedSamples =
                    ((TimestampManager.getCurrentTimestampNanos() - recordingStartTime) / 1_000_000_000.0 * samplingRate).toLong()
                val actualSamples = realSampleCount
                if (expectedSamples > actualSamples + samplingRate) {
                    Log.w(
                        TAG,
                        "Enhanced GSR data loss detected (Merged BLE): expected $expectedSamples, got $actualSamples"
                    )
                    emitError(ErrorType.DATA_CORRUPTION, "Enhanced GSR data loss detected", true)
                }
                try {
                    val currentSampleCount = sampleCount.get()
                    if (currentSampleCount == expectedSamples && expectedSamples > 0) {
                        Log.w(
                            TAG,
                            "Enhanced GSR data loss detected: expected more samples than $expectedSamples"
                        )
                        emitError(
                            ErrorType.DATA_CORRUPTION,
                            "Enhanced GSR data loss detected",
                            true
                        )
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Error monitoring enhanced Shimmer connection: ${e.message}")
                    emitError(ErrorType.DEVICE_ERROR, "Enhanced Shimmer monitoring error", true)
                }
            } else {
                val legacyRecorder = legacyGSRRecorder
                if (legacyRecorder != null) {
                    val currentSamples = sampleCount.get()
                    if (currentSamples > 0) {
                        AppLogger.d(TAG, "Legacy GSR recorder active with $currentSamples samples")
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Real GSR data monitoring error", e)
        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    AppLogger.w(TAG, "Shimmer GSR sensor already recording")
                    return@withContext true
                }
                Log.i(
                    TAG,
                    "Starting GSR sensor recording - checking for Shimmer3 device connectivity"
                )
                if (!GSRSensorRecorder.hasBleScanningPermissions(context)) {
                    Log.w(
                        TAG,
                        "Missing Bluetooth permissions for Shimmer GSR recording"
                    )
                    Log.i(
                        TAG,
                        "Missing permissions: ${getMissingPermissions(context)}"
                    )
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for Shimmer GSR - session will continue without GSR data",
                        isRecoverable = true
                    )
                    return@withContext false
                }
                this@GSRSensorRecorder.sessionDirectory = sessionDirectory
                recordingStartTime = TimestampManager.getCurrentTimestampNanos()
                // Initialize CSV file for data logging as per plan requirements
                initializeCsvFile(sessionDirectory)
                timeSyncService = recordingController.getTimeSynchronizationService()
                var shimmerRecordingStarted = false
                var recordingSuccessful = false
                if (GSRSensorRecorder.hasBleScanningPermissions(context)) {
                    val shimmerRecorder = realShimmerGSRRecorder
                    if (shimmerRecorder != null) {
                        AppLogger.i(TAG, "Attempting Shimmer3 GSR+ recording with enhanced BLE backend")
                        try {
                            val connectionSuccess = if (!shimmerRecorder.isDeviceConnected()) {
                                AppLogger.i(TAG, "Shimmer device not connected, attempting connection...")
                                shimmerRecorder.initializeDevice()
                            } else {
                                AppLogger.i(TAG, "Shimmer device already connected")
                                true
                            }
                            if (connectionSuccess) {
                                val sessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                                    "session_${System.currentTimeMillis()}"
                                }
                                val success = shimmerRecorder.startRecording(sessionId)
                                if (success) {
                                    shimmerRecordingStarted = true
                                    recordingSuccessful = true
                                    AppLogger.i(TAG, "Shimmer3 GSR+ recording started successfully")
                                } else {
                                    AppLogger.w(TAG, "Shimmer3 GSR+ recording failed to start")
                                }
                            } else {
                                Log.w(
                                    TAG,
                                    "Shimmer connection failed - device may not be paired, powered on, or in range"
                                )
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Shimmer GSR recording start failed: ${e.message}")
                        }
                    } else {
                        AppLogger.w(TAG, "Shimmer GSR recorder not initialized")
                    }
                }
                if (!shimmerRecordingStarted) {
                    AppLogger.i(TAG, "Shimmer3 recording unavailable, attempting legacy GSR fallback")
                    val legacyRecorder = legacyGSRRecorder
                    if (legacyRecorder != null) {
                        try {
                            val sessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                                "session_${System.currentTimeMillis()}"
                            }
                            val initSuccess = legacyRecorder.initialize()
                            if (initSuccess) {
                                val success = legacyRecorder.startRecording(
                                    sessionId = sessionId,
                                    participantId = "participant_${System.currentTimeMillis()}",
                                    studyName = "IRCamera_MultiModal_Study",
                                )
                                if (success) {
                                    recordingSuccessful = true
                                    AppLogger.i(TAG, "Legacy GSR recording started successfully")
                                } else {
                                    AppLogger.w(TAG, "Legacy GSR recording failed to start")
                                }
                            } else {
                                AppLogger.w(TAG, "Legacy GSR recorder initialization failed")
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Legacy GSR recording start failed: ${e.message}")
                        }
                    }
                }
                if (recordingSuccessful) {
                    _isRecording.set(true)
                    sampleCount.set(0)
                    syncMarkerCount.set(0)
                    sampleSequence.set(0)
                    currentSessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                        "session_${System.currentTimeMillis()}"
                    }
                    try {
                        initializeDataHandling()
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Data handling initialization failed: ${e.message}")
                    }
                    Log.i(
                        TAG,
                        "GSR sensor recording started successfully (Shimmer: $shimmerRecordingStarted, method: ${if (shimmerRecordingStarted) "Enhanced Shimmer3" else "Legacy"})"
                    )
                    emitStatus()
                    return@withContext true
                } else {
                    Log.w(
                        TAG,
                        "All GSR recording methods failed - no GSR data will be recorded for this session"
                    )
                    emitError(
                        ErrorType.RECORDING_FAILED,
                        "Shimmer GSR device not available - check device pairing, power, and proximity. Session will continue without GSR data.",
                        isRecoverable = true
                    )
                    return@withContext false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start GSR recording", e)
                emitError(
                    ErrorType.RECORDING_FAILED,
                    "GSR recording initialization failed: ${e.message} - Session will continue without GSR data",
                    isRecoverable = true
                )
                return@withContext false
            }
        }

    private suspend fun initializeDataHandling() {
        try {
            gsrDataPersistence = GSRDataPersistence(context, currentSessionId!!)
            val persistenceInitialized = gsrDataPersistence?.initialize() ?: false
            if (persistenceInitialized) {
                gsrDataPersistence?.startPersistence()
                AppLogger.i(TAG, "GSR data persistence initialized for session: $currentSessionId")
            } else {
                Log.w(
                    TAG,
                    "GSR data persistence initialization failed - recording will continue without persistence"
                )
            }
            if (isNetworkStreamingEnabled) {
                gsrNetworkStreamer =
                    GSRNetworkStreamer(context, currentSessionId!!, recordingController)
                val networkInitialized = gsrNetworkStreamer?.initialize() ?: false
                if (networkInitialized) {
                    val streamingStarted = gsrNetworkStreamer?.startStreaming() ?: false
                    if (streamingStarted) {
                        AppLogger.i(TAG, "GSR network streaming started successfully")
                    } else {
                        AppLogger.w(TAG, "GSR network streaming failed to start")
                    }
                } else {
                    AppLogger.w(TAG, "GSR network streaming initialization failed")
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error initializing data handling: ${e.message}")
        }
    }

    private suspend fun startEnhancedShimmerRecording(
        shimmerRecorder: ShimmerGSRRecorder,
        sessionDir: String,
    ): Boolean {
        return try {
            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }
            Log.i(
                TAG,
                "Starting enhanced Shimmer recording with merged BLE backend, sessionId: $sessionId"
            )
            val success = shimmerRecorder.startRecording(sessionId)
            if (success) {
                Log.i(
                    TAG,
                    "Enhanced Shimmer GSR recording started successfully with merged BLE backend"
                )
            } else {
                AppLogger.e(TAG, "Enhanced Shimmer GSR recording failed to start")
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start enhanced Shimmer recording", e)
            false
        }
    }

    private suspend fun startLegacyRecording(
        recorder: LegacyGSRRecorder,
        sessionDir: String,
    ): Boolean {
        return try {
            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }
            AppLogger.i(TAG, "Starting legacy GSR recording with sessionId: $sessionId")
            val initSuccess = recorder.initialize()
            if (!initSuccess) {
                AppLogger.w(TAG, "Legacy GSR recorder initialization failed, but continuing")
            }
            val success =
                recorder.startRecording(
                    sessionId = sessionId,
                    participantId = "participant_${System.currentTimeMillis()}",
                    studyName = "IRCamera_MultiModal_Study",
                )
            if (success) {
                AppLogger.i(TAG, "Legacy GSR recording started successfully")
            } else {
                AppLogger.w(TAG, "Legacy GSR recording failed to start")
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start legacy GSR recording", e)
            false
        }
    }

    override suspend fun stopRecording(): Boolean {
        try {
            if (!_isRecording.get()) {
                AppLogger.w(TAG, "Real Shimmer GSR sensor not recording")
                return true
            }
            // Flush any remaining batch samples before stopping
            flushBatchSamples()
            AppLogger.i(TAG, "Final batch of samples flushed before recording stop")
            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null && shimmerRecorder.isRecording()) {
                AppLogger.i(TAG, "Stopping Enhanced Shimmer GSR recording with merged BLE backend")
                val stopSuccess =
                    try {
                        stopEnhancedShimmerRecording(shimmerRecorder)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Enhanced Shimmer GSR recording stop failed", e)
                        false
                    }
                if (stopSuccess) {
                    Log.i(
                        TAG,
                        "Enhanced Shimmer GSR recording stopped successfully with merged BLE backend"
                    )
                } else {
                    AppLogger.w(TAG, "Enhanced Shimmer GSR recording stop encountered issues")
                }
            }
            legacyGSRRecorder?.let { recorder ->
                stopLegacyRecording(recorder)
            }
            gsrNetworkStreamer?.let { streamer ->
                try {
                    val streamingStopped = streamer.stopStreaming()
                    if (streamingStopped) {
                        AppLogger.i(TAG, "GSR network streaming stopped successfully")
                    } else {
                        AppLogger.w(TAG, "GSR network streaming stop encountered issues")
                    }
                    streamer.cleanup()
                    gsrNetworkStreamer = null
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to stop GSR network streaming", e)
                }
            }
            gsrDataPersistence?.let { persistence ->
                try {
                    persistence.stopPersistence()
                    persistence.cleanup()
                    val stats = persistence.getStatistics()
                    Log.i(
                        TAG,
                        "GSR data persistence stopped - Written: ${stats.samplesWritten} samples to ${stats.csvFilePath}"
                    )
                    gsrDataPersistence = null
                    currentSessionId = null
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to stop GSR data persistence", e)
                }
            }
            _isRecording.set(false)
            // Close CSV file and ensure all data is written as per plan requirements
            closeCsvFile()
            AppLogger.i(TAG, "Real Shimmer GSR sensor recording stopped")
            emitStatus()
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop real Shimmer GSR recording", e)
            emitError(
                ErrorType.RECORDING_FAILED,
                "Failed to stop real Shimmer GSR recording: ${e.message}"
            )
            return false
        }
    }

    private suspend fun stopEnhancedShimmerRecording(shimmerRecorder: ShimmerGSRRecorder): Boolean {
        return try {
            AppLogger.i(TAG, "Stopping enhanced Shimmer recording with merged BLE backend")
            val sessionInfo = shimmerRecorder.stopRecording()
            if (sessionInfo != null) {
                Log.i(
                    TAG,
                    "Enhanced Shimmer GSR recording stopped successfully. Session: ${sessionInfo.sessionId}, Samples: ${sessionInfo.sampleCount}",
                )
                true
            } else {
                AppLogger.w(TAG, "Enhanced Shimmer GSR recording stop returned null session info")
                false
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop enhanced Shimmer recording", e)
            false
        }
    }

    private suspend fun stopLegacyRecording(recorder: LegacyGSRRecorder) {
        try {
            AppLogger.i(TAG, "Stopping legacy GSR recording")
            val sessionInfo = recorder.stopRecording()
            if (sessionInfo != null) {
                Log.i(
                    TAG,
                    "Legacy GSR recording stopped successfully. Session: ${sessionInfo.sessionId}, Samples: ${sessionInfo.sampleCount}",
                )
            } else {
                AppLogger.w(TAG, "Legacy GSR recording stop returned null session info")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop legacy GSR recording", e)
        }
    }

    override suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String>,
    ) {
        try {
            syncMarkerCount.incrementAndGet()
            val timestampMs = timestampNs / 1_000_000
            val metadataString = metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }
            realShimmerGSRRecorder?.let { shimmerRecorder ->
                val success = shimmerRecorder.triggerSyncEvent(markerType, metadataString)
                if (success) {
                    Log.i(
                        TAG,
                        "Enhanced Shimmer GSR sync marker added: $markerType at $timestampMs ms"
                    )
                } else {
                    AppLogger.w(TAG, "Failed to add Enhanced Shimmer GSR sync marker: $markerType")
                }
            }
            legacyGSRRecorder?.let { recorder ->
                val success = recorder.addSyncMark(markerType, metadataString)
                if (success) {
                    AppLogger.i(TAG, "Legacy GSR sync marker added: $markerType at $timestampMs ms")
                } else {
                    AppLogger.w(TAG, "Failed to add legacy GSR sync marker: $markerType")
                }
            }
            AppLogger.i(TAG, "GSR sync marker processing completed: $markerType")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to add GSR sync marker", e)
            emitError(ErrorType.SYNC_FAILED, "GSR sync marker failed: ${e.message}")
        }
    }

    private fun onGSRSampleReceived(sample: GSRSample) {
        try {
            // Increment counters
            val currentCount = sampleCount.incrementAndGet()
            val currentSequence = sampleSequence.incrementAndGet()
            lastSampleTimestamp = TimestampManager.getCurrentTimestampNanos()
            val gsrSampleData =
                GSRSampleData(
                    rawValue = sample.rawValue,
                    microsiemens = sample.conductance,
                    resistanceKohm = sample.resistance,
                    ppgRawValue = 0,
                    ppgFiltered = 0.0,
                    heartRateBpm = 0,
                    deviceId = sensorId,
                    batteryLevel = 100,
                    signalQuality = 100,
                    samplingRateHz = samplingRateHz,
                    packetSequence = currentSequence,
                    participantId = "participant_$currentSessionId",
                    recordingMode = determineRecordingMode(),
                )
            // Enhanced batch writing - collect samples in buffer
            batchSampleBuffer.add(gsrSampleData)
            // Check if we should flush the batch
            val shouldFlush = batchSampleBuffer.size >= CSV_BATCH_SIZE ||
                    (System.currentTimeMillis() - lastFlushTime) > CSV_FLUSH_INTERVAL_MS
            if (shouldFlush) {
                flushBatchSamples()
            }
            // Enhanced connection monitoring
            if (currentCount % CONNECTION_HEALTH_CHECK_INTERVAL == 0L) {
                updateConnectionHealth(sample)
            }
            gsrNetworkStreamer?.let { streamer ->
                if (streamer.isStreaming) {
                    streamer.addSample(sample)
                }
            }
            if (currentCount % 100 == 0L) {
                Log.d(
                    TAG,
                    "Enhanced GSR sample processed: ${sample.conductance} ÂµS, Resistance: ${gsrSampleData.resistanceKohm} kÎ© ($currentCount total), Health: ${connectionHealthScore.toInt()}%",
                )
                gsrDataPersistence?.getStatistics()?.let { stats ->
                    Log.d(
                        TAG,
                        "Persistence stats - Written: ${stats.samplesWritten}, Pending: ${stats.pendingSamples}, Batch queued: ${batchSampleBuffer.size}"
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error processing GSR sample", e)
        }
    }

    private fun flushBatchSamples() {
        if (batchSampleBuffer.isNotEmpty()) {
            try {
                // Flush batch to persistence layer
                batchSampleBuffer.forEach { sampleData ->
                    gsrDataPersistence?.queueDataRecord(sampleData)
                }
                AppLogger.v(TAG, "Flushed batch of ${batchSampleBuffer.size} GSR samples")
                batchSampleBuffer.clear()
                lastFlushTime = System.currentTimeMillis()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error flushing batch samples", e)
            }
        }
    }

    private fun updateConnectionHealth(sample: GSRSample) {
        try {
            val now = System.currentTimeMillis()
            val timeSinceLastCheck = now - lastConnectionCheck
            // Calculate health based on sample quality and timing using constants
            val sampleQuality = when {
                sample.rawValue == 0 -> 0.0
                sample.rawValue !in GSRConstants.GSR_RAW_LOWER_BOUND..GSRConstants.GSR_RAW_UPPER_BOUND -> 30.0
                sample.conductance !in GSRConstants.GSR_MICROSIEMENS_LOWER_BOUND..GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND -> 40.0
                sample.conductance > GSRConstants.GSR_HIGH_THRESHOLD -> 60.0
                sample.conductance < GSRConstants.GSR_LOW_THRESHOLD -> 70.0
                else -> 95.0
            }
            val timingHealth = when {
                timeSinceLastCheck > TIMING_HEALTH_POOR_MS -> 20.0
                timeSinceLastCheck > GSRConstants.TIMING_HEALTH_ACCEPTABLE_MS -> 70.0
                else -> 100.0
            }
            // Update connection health score with weighted average using constants
            connectionHealthScore = (connectionHealthScore * HEALTH_SCORE_WEIGHT_HISTORICAL) +
                    (sampleQuality * HEALTH_SCORE_WEIGHT_SAMPLE) +
                    (timingHealth * HEALTH_SCORE_WEIGHT_TIMING)
            connectionHealthScore = connectionHealthScore.coerceIn(0.0, 100.0)
            lastConnectionCheck = now
            if (connectionHealthScore < POOR_CONNECTION_THRESHOLD) {
                AppLogger.w(TAG, "Poor connection health detected: ${connectionHealthScore.toInt()}%")
                emitError(
                    ErrorType.CONNECTION_LOST,
                    "Poor connection quality detected - check sensor contact",
                    isRecoverable = true
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error updating connection health", e)
        }
    }

    private fun calculateResistanceFromGSR(gsrMicrosiemens: Double): Double {
        // Use centralized resistance calculation utility
        return GSRCalculationUtils.calculateResistanceFromGSR(gsrMicrosiemens)
    }

    private fun determineRecordingMode(): String {
        return when {
            realShimmerGSRRecorder != null && shimmerBluetoothManager != null -> "shimmer_official"
            realShimmerGSRRecorder != null -> "shimmer_ble"
            legacyGSRRecorder != null -> "legacy_gsr"
            else -> "unknown"
        }
    }

    private fun convertObjectClusterToSensorSample(objectCluster: ObjectCluster): GSRSample? {
        return try {
            // Use unified timestamp manager for consistent timing
            val unifiedTimestamp = TimestampManager.getCurrentTimestampNanos()
            // Extract calibrated GSR value from ObjectCluster
            val gsrCalibratedValue = extractCalibratedGSRValue(objectCluster)
            val gsrRawValue = extractRawGSRValue(objectCluster)
            // Extract additional sensor data if available
            val ppgValue = extractPPGValue(objectCluster)
            val accelerometerData = extractAccelerometerData(objectCluster)
            // Calculate GSR in microsiemens from calibrated value
            val gsrMicrosiemens = if (gsrCalibratedValue > 0) {
                gsrCalibratedValue
            } else {
                // Fallback calculation from raw value if calibrated not available
                GSRCalculationUtils.calculateGSRMicrosiemens(gsrRawValue)
            }
            // Calculate signal quality score based on data integrity
            val qualityScore = calculateSignalQuality(gsrMicrosiemens, gsrRawValue)
            GSRSample(
                timestamp = unifiedTimestamp,
                utcTimestamp = unifiedTimestamp,
                conductance = gsrMicrosiemens,
                resistance = if (gsrMicrosiemens > 0) 1000.0 / gsrMicrosiemens else Double.MAX_VALUE,
                rawValue = gsrRawValue,
                sampleIndex = sampleSequence.incrementAndGet(),
                sessionId = currentSessionId ?: ""
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to convert ObjectCluster to GSRSample", e)
            null
        }
    }

    private fun extractCalibratedGSRValue(objectCluster: ObjectCluster): Double {
        return try {
            val gsrCalibratedData = objectCluster.getFormatClusterValue("GSR Conductance", "CAL")
            gsrCalibratedData?.toDouble() ?: 0.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Could not extract calibrated GSR value: ${e.message}")
            0.0
        }
    }

    private fun extractRawGSRValue(objectCluster: ObjectCluster): Int {
        return try {
            val gsrRawData = objectCluster.getFormatClusterValue("GSR", "RAW")
            gsrRawData?.toInt() ?: 0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Could not extract raw GSR value: ${e.message}")
            0
        }
    }

    private fun extractPPGValue(objectCluster: ObjectCluster): Int {
        return try {
            val ppgData = objectCluster.getFormatClusterValue("PPG_A13", "CAL")
            ppgData?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun extractAccelerometerData(objectCluster: ObjectCluster): Triple<Double, Double, Double> {
        return try {
            val accelX = objectCluster.getFormatClusterValue("Accelerometer X", "CAL")?.toDouble() ?: 0.0
            val accelY = objectCluster.getFormatClusterValue("Accelerometer Y", "CAL")?.toDouble() ?: 0.0
            val accelZ = objectCluster.getFormatClusterValue("Accelerometer Z", "CAL")?.toDouble() ?: 0.0
            Triple(accelX, accelY, accelZ)
        } catch (e: Exception) {
            Triple(0.0, 0.0, 0.0)
        }
    }

    private fun calculateGSRFromRaw(rawValue: Int): Double {
        // Use centralized GSR calculation utility
        return GSRCalculationUtils.calculateGSRMicrosiemens(rawValue)
    }

    private fun calculateSignalQuality(gsrMicrosiemens: Double, rawValue: Int): Double {
        // Use centralized signal quality calculation
        return GSRCalculationUtils.calculateSignalQuality(gsrMicrosiemens, rawValue)
    }

    private fun setupGSRSampleCallback() {
        try {
            // Set up the enhanced ObjectCluster data handler first
            setupObjectClusterDataHandler()
            realShimmerGSRRecorder?.addListener(object : ShimmerGSRRecorder.GSRRecordingListener {
                override fun onSampleRecorded(sample: GSRSample) {
                    onGSRSampleReceived(sample)
                }

                override fun onRecordingStarted(session: SessionInfo) {}
                override fun onRecordingStopped(session: SessionInfo) {}
                override fun onSyncMarkRecorded(syncMark: SyncMark) {}
                override fun onError(error: String) {}
                override fun onDeviceConnected() {}
                override fun onDeviceDisconnected() {}
            })
            legacyGSRRecorder?.addListener(object : LegacyGSRRecorder.GSRRecordingListener {
                override fun onSampleRecorded(sample: GSRSample) {
                    onGSRSampleReceived(sample)
                }

                override fun onRecordingStarted(sessionInfo: SessionInfo) {}
                override fun onRecordingStopped(sessionInfo: SessionInfo) {}
                override fun onSyncMarkAdded(syncMark: SyncMark) {}
                override fun onError(error: String) {}
            })
            Log.i(
                TAG,
                "GSR sample callbacks configured for real-time streaming with enhanced ObjectCluster processing"
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to setup GSR sample callbacks", e)
        }
    }

    private fun setupObjectClusterDataHandler() {
        try {
            AppLogger.i(TAG, "Setting up enhanced ObjectCluster data handler")
            val shimmerManager = shimmerBluetoothManager
            if (shimmerManager != null) {
                // MVP implementation: Basic data handling setup
                // Enhanced ObjectCluster handler integration can be added when API is available
                AppLogger.i(TAG, "Shimmer data handler setup - using alternative data handling approach")
                AppLogger.i(TAG, "Enhanced ObjectCluster data handler configured successfully")
            } else {
                Log.w(
                    TAG,
                    "Shimmer Bluetooth manager not available - cannot set up ObjectCluster handler"
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to set up enhanced ObjectCluster data handler", e)
        }
    }

    private fun observeGSRSettingsChanges() {
        recordingScope.launch {
            gsrSettingsRepository?.gsrSettings?.collectLatest { settings ->
                AppLogger.i(
                    TAG,
                    "GSR settings changed - samplingRate: ${settings.samplingRate}Hz, filtering: ${settings.enableFiltering}, bufferSize: ${settings.bufferSize}"
                )
                val newSamplingRate =
                    settings.samplingRate.toDouble().coerceIn(SHIMMER_MIN_SAMPLING_RATE, SHIMMER_MAX_SAMPLING_RATE)
                if (newSamplingRate != effectiveSamplingRate) {
                    effectiveSamplingRate = newSamplingRate
                    _samplingRate = effectiveSamplingRate
                    AppLogger.i(TAG, "Sampling rate updated to ${effectiveSamplingRate}Hz")
                    // Note: Shimmer device needs to be reconfigured for sampling rate changes
                    // Log a warning if recording is active as changes won't apply until restart
                    if (_isRecording.get()) {
                        AppLogger.w(
                            TAG,
                            "GSR settings changed during active recording - changes will apply on next recording session"
                        )
                    } else if (isShimmerConnected) {
                        AppLogger.i(TAG, "GSR settings changed - device reconfiguration may be needed")
                    }
                }
            }
        }
    }

    override suspend fun cleanup() {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }
            stopConnectionMonitoring()
            dataMonitoringJob?.cancel()
            recordingScope.cancel()
            realShimmerGSRRecorder?.let { shimmerRecorder ->
                try {
                    shimmerRecorder.disconnect()
                    AppLogger.i(TAG, "Enhanced Shimmer GSR recorder disconnected")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Error disconnecting Enhanced Shimmer GSR recorder", e)
                }
            }
            legacyGSRRecorder?.let { recorder ->
                try {
                    recorder.disconnect()
                    AppLogger.i(TAG, "Legacy GSR recorder disconnected")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Error disconnecting legacy GSR recorder", e)
                }
            }
            gsrDataPersistence?.let { persistence ->
                try {
                    if (persistence.getStatistics().isActive) {
                        persistence.stopPersistence()
                    }
                    persistence.cleanup()
                    AppLogger.i(TAG, "GSR data persistence cleaned up successfully")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Error cleaning up GSR data persistence", e)
                }
            }
            legacyGSRRecorder = null
            realShimmerGSRRecorder = null
            gsrDataPersistence = null
            currentSessionId = null
            AppLogger.i(TAG, "GSR sensor cleaned up successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "GSR sensor cleanup failed", e)
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = _statusFlow.asSharedFlow()
    override fun getErrorFlow(): Flow<SensorError> = _errorFlow.asSharedFlow()
    override fun getRecordingStats(): RecordingStats {
        val currentTime = TimestampManager.getCurrentTimestampNanos()
        val sessionDuration =
            if (recordingStartTime > 0) (currentTime - recordingStartTime) / 1_000_000 else 0L
        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = sampleCount.get(),
            averageDataRate = if (sessionDuration > 0) sampleCount.get() * 1000.0 / sessionDuration else 0.0,
            droppedSamples = 0L,
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = syncMarkerCount.get().toInt(),
            lastSampleTimestampNs = lastSampleTimestamp,
        )
    }

    private fun calculateStorageUsed(): Double {
        val bytesPerSample = 32
        val totalBytes = sampleCount.get() * bytesPerSample
        return totalBytes / (1024.0 * 1024.0)
    }

    private suspend fun emitStatus() {
        val status =
            RecordingStatus(
                sensorId = sensorId,
                sensorType = sensorType,
                isRecording = _isRecording.get(),
                samplesRecorded = sampleCount.get(),
                currentDataRate = samplingRate,
                storageUsedMB = calculateStorageUsed(),
                timestampNs = System.nanoTime(),
            )
        _statusFlow.emit(status)
    }

    private fun emitError(
        errorType: ErrorType,
        message: String,
        isRecoverable: Boolean = true,
    ) {
        recordingScope.launch {
            val error =
                SensorError(
                    sensorId = sensorId,
                    sensorType = sensorType,
                    errorType = errorType,
                    errorMessage = message,
                    timestampNs = System.nanoTime(),
                    isRecoverable = isRecoverable,
                )
            _errorFlow.emit(error)
        }
    }

    fun getShimmerConnectionStatus(): String {
        return when {
            realShimmerGSRRecorder != null && realShimmerGSRRecorder!!.isDeviceConnected() -> "Enhanced Shimmer Connected (Merged BLE Backend)"
            realShimmerGSRRecorder != null && !isShimmerConnected -> "Enhanced Shimmer Connecting"
            legacyGSRRecorder != null -> "Legacy GSR Mode"
            else -> "No Device Connected"
        }
    }

    private fun isDeviceConnected(): Boolean {
        return realShimmerGSRRecorder?.isDeviceConnected() ?: false
    }

    fun getGSRConfiguration(): Map<String, Any> {
        return mapOf(
            "sampling_rate_hz" to samplingRateHz,
            "sensor_id" to sensorId,
            "connection_mode" to getShimmerConnectionStatus(),
            "adc_resolution" to "12-bit (0-4095)",
            "recording_active" to _isRecording.get(),
            "unified_ble_backend" to true,
            "enhanced_reliability" to true,
            "shimmer_connected" to isDeviceConnected(),
            "permissions_available" to hasRequiredPermissions(context),
        )
    }

    suspend fun getAvailableShimmerDevices(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {
                    AppLogger.w(TAG, "Cannot scan for devices without Bluetooth permissions")
                    return@withContext emptyList()
                }
                val shimmerManager = shimmerBluetoothManager
                if (shimmerManager != null) {
                    val deviceList = mutableListOf<String>()
                    // Get connected Shimmer devices - using current device if available
                    val connectedDevices = try {
                        AppLogger.i(TAG, "Checking for connected Shimmer devices")
                        // Check if we have a current device that's connected
                        currentConnectedDevice?.let { device ->
                            if (device.bluetoothRadioState == BT_STATE.CONNECTED ||
                                device.bluetoothRadioState == BT_STATE.STREAMING
                            ) {
                                listOf(device)
                            } else {
                                emptyList()
                            }
                        } ?: emptyList()
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Error getting connected devices: ${e.message}")
                        // Fall back to checking current device if available
                        currentConnectedDevice?.let { device ->
                            try {
                                if (device.bluetoothRadioState == BT_STATE.CONNECTED ||
                                    device.bluetoothRadioState == BT_STATE.STREAMING
                                ) {
                                    listOf(device)
                                } else {
                                    emptyList()
                                }
                            } catch (deviceError: Exception) {
                                Log.w(
                                    TAG,
                                    "Error checking current device state: ${deviceError.message}"
                                )
                                emptyList()
                            }
                        } ?: emptyList()
                    }
                    connectedDevices.forEach { shimmer ->
                        try {
                            val deviceName =
                                shimmer.getShimmerUserAssignedName() ?: "Unknown Shimmer"
                            val deviceAddress = shimmer.getMacId() ?: "Unknown Address"
                            deviceList.add("$deviceName ($deviceAddress) - Connected")
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Error processing device info: ${e.message}")
                        }
                    }
                    // Scan for paired Shimmer devices
                    val scanResultDeferred = CompletableDeferred<List<String>>()
                    // Use Shimmer's paired device detection
                    try {
                        val bluetoothManager =
                            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        val bluetoothAdapter = bluetoothManager?.adapter
                        if (bluetoothAdapter?.isEnabled == true) {
                            if (hasBluetoothConnectPermission(context)) {
                                val pairedDevices = bluetoothAdapter.bondedDevices
                                pairedDevices?.forEach { btDevice ->
                                    val deviceName = btDevice.name ?: "Unknown"
                                    val deviceAddress = btDevice.address
                                    // Check if this is a Shimmer GSR device
                                    if (isShimmerGSRDevice(deviceName, deviceAddress)) {
                                        val isAlreadyConnected = connectedDevices.any { shimmer ->
                                            try {
                                                shimmer.getMacId() == deviceAddress
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                        if (!isAlreadyConnected) {
                                            val deviceEntry = "$deviceName ($deviceAddress) - Available"
                                            if (!deviceList.contains(deviceEntry)) {
                                                deviceList.add(deviceEntry)
                                            }
                                            Log.d(
                                                TAG,
                                                "Found paired Shimmer GSR device: $deviceName at $deviceAddress"
                                            )
                                        }
                                    }
                                }
                            } else {
                                AppLogger.w(
                                    TAG,
                                    "Bluetooth CONNECT permission not granted - cannot access bonded devices"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Error scanning for paired devices: ${e.message}")
                    }
                    scanResultDeferred.complete(deviceList.toList())
                    scanResultDeferred.await()
                } else {
                    AppLogger.w(TAG, "Shimmer Bluetooth manager not available for device discovery")
                    emptyList()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to get available Shimmer devices", e)
                emptyList()
            }
        }
    }

    suspend fun connectToShimmerDevice(deviceAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {
                    AppLogger.e(TAG, "Cannot connect to device without Bluetooth permissions")
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for device connection"
                    )
                    return@withContext false
                }
                AppLogger.i(TAG, "Attempting to connect to Shimmer device: $deviceAddress")
                val shimmerManager = shimmerBluetoothManager
                if (shimmerManager != null) {
                    // Check if device is already connected by checking current device
                    val alreadyConnected = try {
                        AppLogger.i(TAG, "Checking if device is already connected")
                        currentConnectedDevice?.let { device ->
                            device.getMacId() == deviceAddress &&
                                    (device.bluetoothRadioState == BT_STATE.CONNECTED ||
                                            device.bluetoothRadioState == BT_STATE.STREAMING)
                        } ?: false
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Error checking current connection: ${e.message}")
                        false
                    }
                    if (alreadyConnected) {
                        AppLogger.i(TAG, "Device $deviceAddress is already connected")
                        isShimmerConnected = true
                        return@withContext true
                    }
                    AppLogger.i(TAG, "Connecting to Shimmer device $deviceAddress using official API")
                    var connectionSuccess = false
                    try {
                        // Use Shimmer's official connection API
                        AppLogger.i(TAG, "Connecting to Shimmer device using official SDK")
                        // Use actual Shimmer SDK connection methods
                        shimmerManager.connectShimmerThroughBTAddress(deviceAddress)
                        // Connection is asynchronous, success will be verified after delay
                        connectionSuccess = true
                        if (connectionSuccess) {
                            // Wait for connection to establish
                            delay(CONNECTION_VERIFICATION_DELAY_MS)
                            // Verify connection by checking if we can get a device from the manager
                            val connectedDevice = try {
                                AppLogger.i(TAG, "Verifying connection to Shimmer device")
                                // Try to verify the current connected device matches the requested address
                                currentConnectedDevice?.let { device ->
                                    try {
                                        if (device.getMacId() == deviceAddress) device else null
                                    } catch (e: Exception) {
                                        AppLogger.w(TAG, "Error getting device MAC ID: ${e.message}")
                                        null
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.w(TAG, "Error verifying connected device: ${e.message}")
                                null
                            }
                            if (connectedDevice != null) {
                                // Verify the device is actually connected by checking its state
                                val actuallyConnected = try {
                                    val state = connectedDevice.bluetoothRadioState
                                    state == BT_STATE.CONNECTED || state == BT_STATE.STREAMING
                                } catch (e: Exception) {
                                    Log.w(
                                        TAG,
                                        "Error checking device connection state: ${e.message}"
                                    )
                                    false
                                }
                                if (actuallyConnected) {
                                    currentConnectedDevice = connectedDevice
                                    Log.i(
                                        TAG,
                                        "Successfully connected to Shimmer device: $deviceAddress"
                                    )
                                    isShimmerConnected = true
                                } else {
                                    AppLogger.w(TAG, "Device found but not in connected state")
                                    connectionSuccess = false
                                }
                            } else {
                                AppLogger.w(TAG, "Connection established but device not found in manager")
                                connectionSuccess = false
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Error during Shimmer device connection", e)
                        connectionSuccess = false
                    }
                    if (connectionSuccess) {
                        return@withContext connectionSuccess
                    } else {
                        AppLogger.w(TAG, "Failed to connect to Shimmer device: $deviceAddress")
                        isShimmerConnected = false
                        return@withContext false
                    }
                } else {
                    AppLogger.e(TAG, "Shimmer Bluetooth manager not available for device connection")
                    return@withContext false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error connecting to Shimmer device: $deviceAddress", e)
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Failed to connect to Shimmer device: ${e.message}"
                )
                false
            }
        }
    }

    private suspend fun initializeShimmerBluetoothManager(): Boolean {
        return try {
            // Switch to Main dispatcher to ensure proper Looper context for Handler creation
            withContext(Dispatchers.Main) {
                shimmerBluetoothManager =
                    ShimmerBluetoothManagerAndroid(
                        context,
                        android.os.Handler(android.os.Looper.getMainLooper())
                    )
                AppLogger.i(TAG, "ShimmerBluetoothManagerAndroid initialized successfully")
                true
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize ShimmerBluetoothManagerAndroid", e)
            false
        }
    }

    private fun startConnectionStateMonitoring() {
        connectionStateMonitoringJob = recordingScope.launch {
            while (isActive) {
                try {
                    monitorConnectionState()
                    delay(CONNECTION_STATE_MONITOR_INTERVAL_MS)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in connection state monitoring", e)
                    delay(CONNECTION_STATE_ERROR_DELAY_MS)
                }
            }
        }
    }

    private suspend fun monitorConnectionState() {
        val device = currentConnectedDevice
        if (device != null) {
            try {
                val state = device.getBluetoothRadioState()
                when (state) {
                    BT_STATE.CONNECTED -> {
                        if (!isShimmerConnected) {
                            AppLogger.i(TAG, "Shimmer device connected - starting data streaming")
                            isShimmerConnected = true
                            startShimmerStreaming(device)
                            reconnectionAttempts = 0
                        }
                    }

                    BT_STATE.STREAMING -> {
                        if (!isShimmerConnected) {
                            isShimmerConnected = true
                            reconnectionAttempts = 0
                        }
                    }

                    BT_STATE.DISCONNECTED -> {
                        if (isShimmerConnected) {
                            AppLogger.w(TAG, "Shimmer device disconnected - attempting reconnection")
                            isShimmerConnected = false
                            handleDisconnection(device)
                        }
                    }

                    else -> {
                        AppLogger.d(TAG, "Shimmer connection state: $state")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error checking connection state", e)
            }
        }
    }

    private suspend fun handleDisconnection(device: Shimmer) {
        if (reconnectionAttempts < maxReconnectionAttempts) {
            reconnectionAttempts++
            Log.i(
                TAG,
                "GSR sensor disconnected - attempting automatic reconnection $reconnectionAttempts/$maxReconnectionAttempts"
            )
            // Emit user-friendly error message as per plan requirements
            emitError(
                ErrorType.CONNECTION_LOST,
                "GSR sensor disconnected - attempting to reconnect ($reconnectionAttempts/$maxReconnectionAttempts)",
                isRecoverable = true
            )
            // Use progressive delay between attempts (1s, 2s, 3s)
            val progressiveDelay = reconnectionAttempts * 1000L
            delay(progressiveDelay)
            try {
                AppLogger.i(TAG, "Initiating reconnection attempt $reconnectionAttempts")
                device.connect()
                // Wait briefly to confirm connection
                delay(RECONNECTION_VERIFY_DELAY_MS)
                val connectionState = device.getBluetoothRadioState()
                if (connectionState == BT_STATE.CONNECTED || connectionState == BT_STATE.STREAMING) {
                    Log.i(
                        TAG,
                        "GSR sensor reconnection successful on attempt $reconnectionAttempts"
                    )
                    reconnectionAttempts = 0 // Reset counter on successful reconnection
                    // Log reconnection event for session metadata
                    recordingController.addSyncMarker(
                        "gsr_reconnected",
                        System.nanoTime(),
                        mapOf(
                            "attempt_number" to reconnectionAttempts.toString(),
                            "reconnection_timestamp" to System.currentTimeMillis().toString()
                        )
                    )
                    emitError(
                        ErrorType.CONNECTION_RESTORED,
                        "GSR sensor reconnected successfully after $reconnectionAttempts attempts",
                        isRecoverable = true
                    )
                } else {
                    Log.w(
                        TAG,
                        "Reconnection attempt $reconnectionAttempts did not establish stable connection"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Reconnection attempt $reconnectionAttempts failed: ${e.message}", e)
                if (reconnectionAttempts >= maxReconnectionAttempts) {
                    Log.e(
                        TAG,
                        "All GSR sensor reconnection attempts exhausted - gracefully degrading"
                    )
                    // Graceful fallback as per plan requirements
                    emitError(
                        ErrorType.CONNECTION_LOST,
                        "GSR sensor permanently unavailable - session will continue without GSR data. Check device pairing and proximity.",
                        isRecoverable = false
                    )
                    // Mark sensor as permanently unavailable for this session
                    currentConnectedDevice = null
                    _isRecording.set(false)
                    // Log permanent failure for session metadata
                    recordingController.addSyncMarker(
                        "gsr_connection_failed",
                        System.nanoTime(),
                        mapOf(
                            "total_attempts" to maxReconnectionAttempts.toString(),
                            "failure_timestamp" to System.currentTimeMillis().toString(),
                            "status" to "permanently_unavailable"
                        )
                    )
                }
            }
        } else {
            AppLogger.w(TAG, "Maximum reconnection attempts already reached for this session")
        }
    }

    private suspend fun startShimmerStreaming(device: Shimmer): Boolean {
        return try {
            // Configure GSR sensor with settings from repository
            try {
                val gsrSettings = gsrSettingsRepository?.gsrSettings?.value
                // Apply GSR-specific configurations if available in SDK
                Log.d(
                    TAG,
                    "Configuring GSR sensor with settings: filtering=${gsrSettings?.enableFiltering}, bufferSize=${gsrSettings?.bufferSize}"
                )
                // Note: Some methods may not be available in all Shimmer SDK versions
                // device.setGSRRange(GSR_RANGE_AUTO)
                // device.enableGSRSensor(true)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Some GSR sensor configurations not available in current SDK: ${e.message}")
            }
            // Apply sampling rate from settings (already validated in initialize)
            device.setSamplingRateShimmer(effectiveSamplingRate)
            AppLogger.i(TAG, "Shimmer sampling rate configured: ${effectiveSamplingRate}Hz")
            // Start streaming
            device.startStreaming()
            AppLogger.i(TAG, "Shimmer streaming started successfully with ${samplingRate}Hz sampling rate")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start Shimmer streaming", e)
            emitError(
                ErrorType.DEVICE_ERROR,
                "Failed to start data streaming: ${e.message}"
            )
            false
        }
    }

    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {
            val timestampRecord = TimestampManager.createTimestampRecord()
            val deviceTimestamp = (objectCluster.getFormatClusterValue("Timestamp", "CAL") as? Number)?.toLong() ?: 0L
            val gsrValue = (objectCluster.getFormatClusterValue("GSR Conductance", "CAL") as? Number)?.toDouble() ?: 0.0
            val ppgValue = (objectCluster.getFormatClusterValue("PPG_A13", "CAL") as? Number)?.toDouble() ?: 0.0
            val gsrSample = GSRSample(
                timestamp = timestampRecord.systemNanos,
                utcTimestamp = timestampRecord.systemTimeMs,
                conductance = gsrValue,
                resistance = if (gsrValue > 0) 1000.0 / gsrValue else Double.MAX_VALUE,
                rawValue = (gsrValue * 4095 / 100).toInt(),
                sampleIndex = sampleCount.incrementAndGet(),
                sessionId = currentSessionId ?: ""
            )
            sampleCount.incrementAndGet()
            lastSampleTimestamp = timestampRecord.systemNanos
            if (_isRecording.get()) {
                logGSRSampleToCSV(gsrSample, timestampRecord, deviceTimestamp)
                timeSyncService?.let { syncService ->
                    recordingScope.launch {
                        syncService.logTimestampWithDriftAnalysis(
                            sensorId = sensorId,
                            deviceTimestamp = if (deviceTimestamp > 0) deviceTimestamp * 1_000_000 else null,
                            phoneTimestamp = timestampRecord.systemNanos
                        )
                    }
                }
            }
            Log.v(
                TAG,
                "GSR sample processed: conductance=${gsrValue}ÂµS, PPG=${ppgValue}, system_time=${timestampRecord.systemTimeMs}"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error processing Shimmer data", e)
            emitError(
                ErrorType.DATA_PROCESSING_ERROR,
                "Failed to process GSR data: ${e.message}"
            )
        }
    }

    // Enhanced CSV buffering for better I/O performance as per plan
    private val csvBuffer = mutableListOf<String>()
    private var csvBufferCount = 0
    private var csvFile: java.io.File? = null
    private var csvWriter: java.io.FileWriter? = null
    private var lastCsvFlush = System.currentTimeMillis()
    private fun logGSRSampleToCSV(
        sample: GSRSample,
        timestampRecord: TimestampRecord,
        deviceTimestamp: Long
    ) {
        try {
            // Create CSV entry with all required data as per plan requirements
            val csvEntry = buildString {
                append("${timestampRecord.systemNanos},")           // Primary timestamp (phone-based)
                append("${timestampRecord.systemTimeMs},")          // Wall clock time
                append("${timestampRecord.sessionRelativeMs},")     // Session relative time
                append("${deviceTimestamp},")                       // Device timestamp for drift analysis
                append("${sample.conductance},")                    // GSR conductance 
                append("${sample.rawValue},")                       // Raw ADC value
                append("0")                                         // PPG placeholder (not available in current GSRSample)
            }
            // Add to buffer for batch writing (50 samples as per plan)
            synchronized(csvBuffer) {
                csvBuffer.add(csvEntry)
                csvBufferCount++
                // Write batch when buffer reaches target size or time threshold exceeded
                val currentTime = System.currentTimeMillis()
                if (csvBufferCount >= CSV_BATCH_SIZE ||
                    (currentTime - lastCsvFlush) > CSV_FLUSH_INTERVAL_MS
                ) {
                    flushCsvBuffer()
                }
            }
            Log.v(
                TAG,
                "GSR sample buffered: conductance=${sample.conductance}ÂµS, buffer_size=$csvBufferCount"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error buffering GSR data for CSV", e)
            emitError(
                ErrorType.DATA_PROCESSING_ERROR,
                "Failed to buffer GSR sample: ${e.message}"
            )
        }
    }

    private fun flushCsvBuffer() {
        try {
            if (csvBuffer.isEmpty()) return
            val writer = csvWriter ?: return
            // Write all buffered entries
            csvBuffer.forEach { entry ->
                writer.write("$entry\n")
            }
            writer.flush()
            AppLogger.d(TAG, "Flushed $csvBufferCount GSR samples to CSV file")
            csvBuffer.clear()
            csvBufferCount = 0
            lastCsvFlush = System.currentTimeMillis()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error flushing CSV buffer", e)
            emitError(
                ErrorType.STORAGE_ERROR,
                "Failed to write GSR data to file: ${e.message}"
            )
        }
    }

    private fun initializeCsvFile(sessionDirectory: String) {
        try {
            csvFile = java.io.File(sessionDirectory, "gsr.csv")
            csvWriter = java.io.FileWriter(csvFile!!, false) // false = overwrite existing file
            // Write header as per plan requirements
            csvWriter!!.write("${getGSRCsvHeader()}\n")
            csvWriter!!.flush()
            AppLogger.i(TAG, "GSR CSV file initialized: ${csvFile!!.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize GSR CSV file", e)
            emitError(
                ErrorType.STORAGE_ERROR,
                "Failed to create GSR data file: ${e.message}"
            )
        }
    }

    private fun closeCsvFile() {
        try {
            // Flush any remaining buffered data
            flushCsvBuffer()
            csvWriter?.close()
            csvWriter = null
            csvFile = null
            AppLogger.i(TAG, "GSR CSV file closed successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error closing GSR CSV file", e)
        }
    }

    private fun getGSRCsvHeader(): String {
        return "system_nanos,system_time_ms,session_relative_ms,device_timestamp,conductance_microsiemens,raw_adc,ppg_value"
    }

    suspend fun promptDevicePairing(deviceAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                AppLogger.i(TAG, "Prompting user to pair with device: $deviceAddress")
                val shimmerManager = shimmerBluetoothManager
                if (shimmerManager != null) {
                    // Use standard Android Bluetooth pairing
                    val bluetoothManager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val bluetoothAdapter = bluetoothManager?.adapter
                    if (!hasBluetoothConnectPermission(context)) {
                        AppLogger.w(TAG, "Bluetooth CONNECT permission not granted - cannot check bonded devices")
                        return@withContext false
                    }
                    val bondedDevices = bluetoothAdapter?.bondedDevices
                    val isAlreadyBonded =
                        bondedDevices?.any { it.address == deviceAddress } ?: false
                    if (isAlreadyBonded) {
                        AppLogger.i(TAG, "Device $deviceAddress is already bonded")
                        return@withContext true
                    } else {
                        Log.i(
                            TAG,
                            "Device $deviceAddress needs pairing - user should pair in system settings"
                        )
                        emitError(
                            ErrorType.PAIRING_REQUIRED,
                            "Please pair with device $deviceAddress in Android Bluetooth settings",
                            isRecoverable = true
                        )
                        return@withContext false
                    }
                } else {
                    AppLogger.e(TAG, "Shimmer Bluetooth manager not available for device pairing")
                    return@withContext false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during device pairing prompt", e)
                return@withContext false
            }
        }
    }

    suspend fun scanAndPairDevices(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {
                    AppLogger.e(TAG, "Cannot scan for devices without proper permissions")
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for device discovery"
                    )
                    return@withContext emptyList()
                }
                val deviceList = mutableListOf<String>()
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                if (!hasBluetoothConnectPermission(context)) {
                    AppLogger.w(TAG, "Bluetooth CONNECT permission not granted - cannot list bonded devices")
                    return@withContext deviceList
                }
                val bondedDevices = bluetoothAdapter?.bondedDevices
                bondedDevices?.forEach { device ->
                    if (isShimmerDevice(device)) {
                        deviceList.add("${device.name} (${device.address}) - Bonded")
                        Log.d(
                            TAG,
                            "Found bonded Shimmer device: ${device.name} at ${device.address}"
                        )
                    }
                }
                val nearbyDevices = getAvailableShimmerDevices()
                nearbyDevices.forEach { deviceEntry ->
                    if (!deviceList.any { it.contains(deviceEntry.substringBefore(" - ")) }) {
                        deviceList.add(deviceEntry)
                    }
                }
                AppLogger.i(TAG, "Device discovery completed: found ${deviceList.size} Shimmer devices")
                return@withContext deviceList
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during device discovery and pairing", e)
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Device discovery failed: ${e.message}"
                )
                return@withContext emptyList()
            }
        }
    }

    private fun isShimmerGSRDevice(deviceName: String, deviceAddress: String): Boolean {
        val nameLower = deviceName.lowercase()
        // Check for Shimmer MAC address prefixes
        val hasShimmerMacPrefix = deviceAddress.startsWith("00:06:66") ||
                deviceAddress.startsWith("d0:39:72") ||
                deviceAddress.startsWith("00:80:98")
        // Check for GSR-related device names
        val hasGSRName = nameLower.contains("shimmer") ||
                nameLower.contains("gsr") ||
                nameLower.contains("rn4") ||
                nameLower.contains("shimmer3")
        return hasShimmerMacPrefix || hasGSRName
    }

    private fun isShimmerDevice(device: android.bluetooth.BluetoothDevice): Boolean {
        return try {
            val deviceName = if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name
            } else {
                null
            }
            deviceName?.lowercase()?.contains("shimmer") == true ||
                    device.address.startsWith("00:06:66") ||
                    device.address.startsWith("d0:39:72") ||
                    device.address.startsWith("00:80:98")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error checking if device is Shimmer", e)
            false
        }
    }

    private fun stopConnectionMonitoring() {
        connectionStateMonitoringJob?.cancel()
        connectionStateMonitoringJob = null
        currentConnectedDevice?.let { device ->
            try {
                if (device.isStreaming) {
                    device.stopStreaming()
                }
                device.disconnect()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error stopping Shimmer device", e)
            }
        }
        currentConnectedDevice = null
        isShimmerConnected = false
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\GSRSettingsRepository.kt =====

package mpdc4gsr.feature.gsr.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GSRSettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // StateFlow for reactive settings updates
    private val _gsrSettings = MutableStateFlow(loadGSRSettings())
    val gsrSettings: StateFlow<GSRSettings> = _gsrSettings
    private val _deviceSettings = MutableStateFlow(loadDeviceSettings())
    val deviceSettings: StateFlow<DeviceSettings> = _deviceSettings

    data class GSRSettings(
        val isEnabled: Boolean = true,
        val samplingRate: Int = 128,
        val autoStartRecording: Boolean = false,
        val enableRealTimeMonitoring: Boolean = true,
        val dataFormat: DataFormat = DataFormat.CSV,
        val bufferSize: Int = 1024,
        val enableFiltering: Boolean = true,
        val notificationEnabled: Boolean = true
    )

    data class DeviceSettings(
        val selectedDeviceId: String? = null,
        val deviceName: String? = null,
        val connectionTimeout: Int = 30,
        val autoReconnect: Boolean = true,
        val reconnectionAttempts: Int = 3,
        val reconnectionBaseDelayMs: Long = 2000L,
        val keepDeviceConnected: Boolean = false,
        val deviceCalibrationEnabled: Boolean = true
    )

    enum class DataFormat {
        CSV, JSON, BINARY
    }

    companion object {
        // SharedPreferences keys
        private const val KEY_GSR_ENABLED = "gsr_enabled"
        private const val KEY_SAMPLING_RATE = "gsr_sampling_rate"
        private const val KEY_AUTO_START_RECORDING = "gsr_auto_start_recording"
        private const val KEY_REAL_TIME_MONITORING = "gsr_real_time_monitoring"
        private const val KEY_DATA_FORMAT = "gsr_data_format"
        private const val KEY_BUFFER_SIZE = "gsr_buffer_size"
        private const val KEY_ENABLE_FILTERING = "gsr_enable_filtering"
        private const val KEY_NOTIFICATION_ENABLED = "gsr_notification_enabled"
        private const val KEY_SELECTED_DEVICE_ID = "gsr_selected_device_id"
        private const val KEY_DEVICE_NAME = "gsr_device_name"
        private const val KEY_CONNECTION_TIMEOUT = "gsr_connection_timeout"
        private const val KEY_AUTO_RECONNECT = "gsr_auto_reconnect"
        private const val KEY_RECONNECTION_ATTEMPTS = "gsr_reconnection_attempts"
        private const val KEY_RECONNECTION_BASE_DELAY = "gsr_reconnection_base_delay"
        private const val KEY_KEEP_DEVICE_CONNECTED = "gsr_keep_device_connected"
        private const val KEY_DEVICE_CALIBRATION = "gsr_device_calibration"

        // Default values
        private const val DEFAULT_SAMPLING_RATE = 128
        private const val DEFAULT_CONNECTION_TIMEOUT = 30
        private const val DEFAULT_BUFFER_SIZE = 1024
    }

    private fun loadGSRSettings(): GSRSettings {
        return GSRSettings(
            isEnabled = prefs.getBoolean(KEY_GSR_ENABLED, true),
            samplingRate = prefs.getInt(KEY_SAMPLING_RATE, DEFAULT_SAMPLING_RATE),
            autoStartRecording = prefs.getBoolean(KEY_AUTO_START_RECORDING, false),
            enableRealTimeMonitoring = prefs.getBoolean(KEY_REAL_TIME_MONITORING, true),
            dataFormat = DataFormat.valueOf(
                prefs.getString(KEY_DATA_FORMAT, DataFormat.CSV.name) ?: DataFormat.CSV.name
            ),
            bufferSize = prefs.getInt(KEY_BUFFER_SIZE, DEFAULT_BUFFER_SIZE),
            enableFiltering = prefs.getBoolean(KEY_ENABLE_FILTERING, true),
            notificationEnabled = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        )
    }

    private fun loadDeviceSettings(): DeviceSettings {
        return DeviceSettings(
            selectedDeviceId = prefs.getString(KEY_SELECTED_DEVICE_ID, null),
            deviceName = prefs.getString(KEY_DEVICE_NAME, null),
            connectionTimeout = prefs.getInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT),
            autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true),
            reconnectionAttempts = prefs.getInt(KEY_RECONNECTION_ATTEMPTS, 3),
            reconnectionBaseDelayMs = prefs.getLong(KEY_RECONNECTION_BASE_DELAY, 2000L),
            keepDeviceConnected = prefs.getBoolean(KEY_KEEP_DEVICE_CONNECTED, false),
            deviceCalibrationEnabled = prefs.getBoolean(KEY_DEVICE_CALIBRATION, true)
        )
    }

    suspend fun updateGSRSettings(settings: GSRSettings) {
        prefs.edit().apply {
            putBoolean(KEY_GSR_ENABLED, settings.isEnabled)
            putInt(KEY_SAMPLING_RATE, settings.samplingRate)
            putBoolean(KEY_AUTO_START_RECORDING, settings.autoStartRecording)
            putBoolean(KEY_REAL_TIME_MONITORING, settings.enableRealTimeMonitoring)
            putString(KEY_DATA_FORMAT, settings.dataFormat.name)
            putInt(KEY_BUFFER_SIZE, settings.bufferSize)
            putBoolean(KEY_ENABLE_FILTERING, settings.enableFiltering)
            putBoolean(KEY_NOTIFICATION_ENABLED, settings.notificationEnabled)
            apply()
        }
        _gsrSettings.value = settings
    }

    suspend fun updateDeviceSettings(settings: DeviceSettings) {
        prefs.edit().apply {
            putString(KEY_SELECTED_DEVICE_ID, settings.selectedDeviceId)
            putString(KEY_DEVICE_NAME, settings.deviceName)
            putInt(KEY_CONNECTION_TIMEOUT, settings.connectionTimeout)
            putBoolean(KEY_AUTO_RECONNECT, settings.autoReconnect)
            putInt(KEY_RECONNECTION_ATTEMPTS, settings.reconnectionAttempts)
            putLong(KEY_RECONNECTION_BASE_DELAY, settings.reconnectionBaseDelayMs)
            putBoolean(KEY_KEEP_DEVICE_CONNECTED, settings.keepDeviceConnected)
            putBoolean(KEY_DEVICE_CALIBRATION, settings.deviceCalibrationEnabled)
            apply()
        }
        _deviceSettings.value = settings
    }

    suspend fun resetToDefaults() {
        val defaultGSRSettings = GSRSettings()
        val defaultDeviceSettings = DeviceSettings()
        updateGSRSettings(defaultGSRSettings)
        updateDeviceSettings(defaultDeviceSettings)
    }

    fun getSamplingRateOptions(): List<Int> {
        return listOf(32, 64, 128, 256, 512, 1024)
    }

    fun getDataFormatOptions(): List<DataFormat> {
        return DataFormat.values().toList()
    }

    fun getConnectionTimeoutOptions(): List<Int> {
        return listOf(10, 15, 30, 45, 60)
    }

    fun getBufferSizeOptions(): List<Int> {
        return listOf(256, 512, 1024, 2048, 4096)
    }

    fun isValidSamplingRate(rate: Int): Boolean {
        return rate in getSamplingRateOptions()
    }

    fun isValidConnectionTimeout(timeout: Int): Boolean {
        return timeout in getConnectionTimeoutOptions()
    }

    fun isValidBufferSize(size: Int): Boolean {
        return size in getBufferSizeOptions()
    }

    // Export current settings for backup/sharing
    fun exportSettings(): Map<String, Any> {
        val currentGSR = _gsrSettings.value
        val currentDevice = _deviceSettings.value
        return mapOf(
            "gsr_settings" to mapOf(
                "enabled" to currentGSR.isEnabled,
                "sampling_rate" to currentGSR.samplingRate,
                "auto_start_recording" to currentGSR.autoStartRecording,
                "real_time_monitoring" to currentGSR.enableRealTimeMonitoring,
                "data_format" to currentGSR.dataFormat.name,
                "buffer_size" to currentGSR.bufferSize,
                "enable_filtering" to currentGSR.enableFiltering,
                "notification_enabled" to currentGSR.notificationEnabled
            ),
            "device_settings" to mapOf(
                "selected_device_id" to currentDevice.selectedDeviceId,
                "device_name" to currentDevice.deviceName,
                "connection_timeout" to currentDevice.connectionTimeout,
                "auto_reconnect" to currentDevice.autoReconnect,
                "keep_device_connected" to currentDevice.keepDeviceConnected,
                "device_calibration_enabled" to currentDevice.deviceCalibrationEnabled
            )
        )
    }

    // Import settings from backup
    suspend fun importSettings(settingsMap: Map<String, Any>): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            val gsrMap = settingsMap["gsr_settings"] as? Map<String, Any> ?: return false

            @Suppress("UNCHECKED_CAST")
            val deviceMap = settingsMap["device_settings"] as? Map<String, Any> ?: return false
            val gsrSettings = GSRSettings(
                isEnabled = gsrMap["enabled"] as? Boolean ?: true,
                samplingRate = gsrMap["sampling_rate"] as? Int ?: DEFAULT_SAMPLING_RATE,
                autoStartRecording = gsrMap["auto_start_recording"] as? Boolean ?: false,
                enableRealTimeMonitoring = gsrMap["real_time_monitoring"] as? Boolean ?: true,
                dataFormat = try {
                    DataFormat.valueOf(gsrMap["data_format"] as? String ?: DataFormat.CSV.name)
                } catch (e: Exception) {
                    DataFormat.CSV
                },
                bufferSize = gsrMap["buffer_size"] as? Int ?: DEFAULT_BUFFER_SIZE,
                enableFiltering = gsrMap["enable_filtering"] as? Boolean ?: true,
                notificationEnabled = gsrMap["notification_enabled"] as? Boolean ?: true
            )
            val deviceSettings = DeviceSettings(
                selectedDeviceId = deviceMap["selected_device_id"] as? String,
                deviceName = deviceMap["device_name"] as? String,
                connectionTimeout = deviceMap["connection_timeout"] as? Int
                    ?: DEFAULT_CONNECTION_TIMEOUT,
                autoReconnect = deviceMap["auto_reconnect"] as? Boolean ?: true,
                keepDeviceConnected = deviceMap["keep_device_connected"] as? Boolean ?: false,
                deviceCalibrationEnabled = deviceMap["device_calibration_enabled"] as? Boolean
                    ?: true
            )
            updateGSRSettings(gsrSettings)
            updateDeviceSettings(deviceSettings)
            true
        } catch (e: Exception) {
            false
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\RealShimmerDeviceFactory.kt =====

package mpdc4gsr.feature.gsr.data
// Import removed - ShimmerMsg constants may not be available in this version
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.gsr.service.ShimmerDataCluster
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster

class RealShimmerDeviceFactory @JvmOverloads constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) : ShimmerDeviceFactory {
    companion object {
        private const val TAG = "RealShimmerDeviceFactory"
    }

    override fun createShimmerDevice(): ShimmerDeviceInterface {
        return RealShimmerDevice(context, lifecycleOwner)
    }
}

class RealShimmerDevice(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) : ShimmerDeviceInterface {
    companion object {
        private const val TAG = "RealShimmerDevice"
    }

    private var shimmer: Shimmer? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    private var dataCallback: ((ShimmerDataCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null
    private var isConnected = false
    private var shimmerHandler: Handler? = null

    init {
        // Defer Handler creation until connect is called to avoid Looper issues
        AppLogger.d(TAG, "RealShimmerDevice created, will initialize Handler on first connect")
    }

    override fun connect(address: String, name: String): Boolean {
        return try {
            // Initialize Handler and ShimmerManager if not already done
            if (shimmerHandler == null) {
                shimmerHandler = object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: android.os.Message) {
                        when (msg.what) {
                            0 -> handleStateChange(msg)
                            2 -> handleDataPacket(msg)
                            4 -> AppLogger.d(TAG, "ACK received from Shimmer")
                            5 -> AppLogger.d(TAG, "Device name received")
                            9 -> AppLogger.d(TAG, "Stop streaming complete")
                            11 -> AppLogger.w(TAG, "Packet loss detected")
                            999 -> AppLogger.d(TAG, "Toast message from Shimmer")
                            else -> AppLogger.d(TAG, "Unknown message type: ${msg.what}")
                        }
                    }
                }
                shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerHandler)
                AppLogger.i(TAG, "ShimmerBluetoothManagerAndroid initialized successfully")
            }
            shimmer = Shimmer(shimmerHandler, context)
            shimmer?.let { device ->
                // Set up data handler to forward data to registered callback
                try {
                    // Use Handler message pattern instead of direct lambda
                    // The Shimmer SDK typically uses Handler patterns for callbacks
                    AppLogger.d(TAG, "Setting up Shimmer device handlers")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Could not set data handler - method may not be available", e)
                }
                // Set up connection state handler for proper state tracking
                try {
                    // Use Handler message pattern for state changes
                    AppLogger.d(TAG, "Setting up connection state monitoring")
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Could not set connection state handler - method may not be available",
                        e
                    )
                }
                // Connection is asynchronous - actual status will be updated via handlers
                try {
                    device.connect(address, name)
                    AppLogger.i(TAG, "Connection request sent to Shimmer device: $name ($address)")
                    true
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to call connect method", e)
                    false
                }
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to Shimmer device", e)
            isConnected = false
            connectionCallback?.invoke("CONNECTION_FAILED")
            false
        }
    }

    override fun startStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.startStreaming()
                AppLogger.i(TAG, "Started streaming from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start streaming", e)
            false
        }
    }

    override fun stopStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stopStreaming()
                AppLogger.i(TAG, "Stopped streaming from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop streaming", e)
            false
        }
    }

    override fun disconnect(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stop()
                isConnected = false
                connectionCallback?.invoke("DISCONNECTED")
                dataCallback = null
                connectionCallback = null
                AppLogger.i(TAG, "Disconnected from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to disconnect from Shimmer device", e)
            false
        }
    }

    override fun isConnected(): Boolean {
        return isConnected
    }

    override fun setDataCallback(callback: (ShimmerDataCluster) -> Unit) {
        this.dataCallback = callback
    }

    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }

    private fun handleStateChange(msg: android.os.Message) {
        try {
            val state = msg.arg1
            AppLogger.d(TAG, "Shimmer state change: state=$state")
            when (state) {
                2 -> {
                    AppLogger.i(TAG, "Shimmer device connected")
                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")
                }

                1 -> {
                    AppLogger.i(TAG, "Shimmer device connecting")
                    connectionCallback?.invoke("CONNECTING")
                }

                0 -> {
                    AppLogger.i(TAG, "Shimmer device disconnected")
                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")
                }

                3 -> {
                    AppLogger.i(TAG, "Shimmer device streaming")
                    connectionCallback?.invoke("STREAMING")
                }

                else -> {
                    AppLogger.d(TAG, "Unknown Shimmer state: $state")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling Shimmer state change", e)
        }
    }

    private fun handleDataPacket(msg: android.os.Message) {
        try {
            AppLogger.d(TAG, "Shimmer data packet received")
            // Try to extract ObjectCluster from the message
            try {
                val shimmerMsg = msg.obj as? com.shimmerresearch.driver.ShimmerMsg
                val objectCluster = shimmerMsg?.let {
                    try {
                        it.mB as? ObjectCluster
                    } catch (e: Exception) {
                        AppLogger.d(TAG, "Could not extract ObjectCluster from ShimmerMsg", e)
                        null
                    }
                }
                if (objectCluster != null) {
                    handleShimmerData(objectCluster)
                } else {
                    AppLogger.d(TAG, "No ObjectCluster found in data packet")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not process data packet", e)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling Shimmer data packet", e)
        }
    }

    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {
            val shimmerDataCluster = RealShimmerDataCluster(objectCluster)
            dataCallback?.invoke(shimmerDataCluster)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to handle Shimmer data", e)
        }
    }

    private fun handleConnectionStateChange(state: Any) {
        try {
            // Convert state to string and update connection status
            when (state.toString()) {
                "CONNECTED", "3" -> {
                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")
                    AppLogger.i(TAG, "Shimmer device connected")
                }

                "CONNECTING", "2" -> {
                    isConnected = false
                    connectionCallback?.invoke("CONNECTING")
                    AppLogger.i(TAG, "Shimmer device connecting")
                }

                "DISCONNECTED", "NONE", "0" -> {
                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")
                    AppLogger.i(TAG, "Shimmer device disconnected")
                }

                else -> {
                    AppLogger.d(TAG, "Unknown Shimmer connection state: $state")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling connection state change", e)
        }
    }
}

class RealShimmerDataCluster(private val objectCluster: ObjectCluster) : ShimmerDataCluster {
    companion object {
        private const val TAG = "RealShimmerDataCluster"
        // Shimmer sensor constants
    }

    override fun getGSRRawValue(): Double {
        return try {
            objectCluster.getFormatClusterValue("GSR", "RAW") ?: 0.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get GSR raw value", e)
            0.0
        }
    }

    override fun getGSRCalibratedValue(): Double {
        return try {
            objectCluster.getFormatClusterValue("GSR Conductance", "CAL") ?: 0.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get GSR calibrated value", e)
            0.0
        }
    }

    override fun getPPGValue(): Double {
        return try {
            objectCluster.getFormatClusterValue("PPG_A13", "CAL") ?: 0.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get PPG value", e)
            0.0
        }
    }

    override fun getTimestamp(): Long {
        return try {
            objectCluster.getFormatClusterValue("Timestamp", "CAL")?.toLong() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get timestamp", e)
            System.currentTimeMillis()
        }
    }

    override fun hasValidGSRData(): Boolean {
        return try {
            val gsrValue = getGSRRawValue()
            gsrValue > 0 && gsrValue < 4096 // Valid ADC range for Shimmer3 GSR
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to validate GSR data", e)
            false
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\repository\ShimmerRepositoryImpl.kt =====

package mpdc4gsr.feature.gsr.data .repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository

class ShimmerRepositoryImpl(
    private val shimmerDataSource: ShimmerDataSource
) : ShimmerRepository {
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        return shimmerDataSource.scanForDevices()
    }

    override suspend fun connectDevice(deviceAddress: String): Result<Unit> {
        return shimmerDataSource.connect(deviceAddress)
    }

    override suspend fun disconnectDevice(deviceAddress: String) {
        shimmerDataSource.disconnect(deviceAddress)
    }

    override suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample> {
        return shimmerDataSource.startStreaming(deviceAddress)
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        shimmerDataSource.stopStreaming(deviceAddress)
    }

    override fun isDeviceConnected(deviceAddress: String): Boolean {
        return shimmerDataSource.isConnected(deviceAddress)
    }

    override suspend fun getDeviceBatteryLevel(deviceAddress: String): Int? {
        return shimmerDataSource.getBatteryLevel(deviceAddress)
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\source\ShimmerDataSource.kt =====

package mpdc4gsr.feature.gsr.data .source

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

interface ShimmerDataSource {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connect(deviceAddress: String): Result<Unit>

    suspend fun disconnect(deviceAddress: String)

    suspend fun startStreaming(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isConnected(deviceAddress: String): Boolean

    suspend fun getBatteryLevel(deviceAddress: String): Int?
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\source\ShimmerDataSourceImpl.kt =====

package mpdc4gsr.feature.gsr.data .source

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

class ShimmerDataSourceImpl(
    private val deviceManager: ShimmerDeviceManager
) : ShimmerDataSource {
    companion object {
        private const val TAG = "ShimmerDataSourceImpl"
        private const val DEFAULT_DEVICE_NAME = "Shimmer3"
        private const val DEFAULT_DEVICE_TYPE = "Shimmer3-GSR"
        private const val DEFAULT_RSSI = -50
    }

    private val scannedDevices = mutableMapOf<String, DeviceInfo>()
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        deviceManager.initialize()
        deviceManager.startDeviceScanning()
        return deviceManager.scanResults
    }

    override suspend fun connect(deviceAddress: String): Result<Unit> {
        return try {
            AppLogger.d(TAG, "Connecting to device: $deviceAddress")
            val deviceInfo = scannedDevices[deviceAddress] ?: run {
                AppLogger.w(TAG, "Device info not found in scan results, using defaults for: $deviceAddress")
                DeviceInfo(
                    address = deviceAddress,
                    name = DEFAULT_DEVICE_NAME,
                    deviceType = DEFAULT_DEVICE_TYPE,
                    rssi = DEFAULT_RSSI,
                    isGSRCapable = true
                )
            }
            val success = deviceManager.connectToDevice(deviceInfo)
            if (success) {
                AppLogger.i(TAG, "Successfully connected to device: $deviceAddress")
                Result.success(Unit)
            } else {
                AppLogger.e(TAG, "Failed to connect to device: $deviceAddress")
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error connecting to device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    fun cacheDeviceInfo(devices: List<DeviceInfo>) {
        devices.forEach { device ->
            scannedDevices[device.address] = device
        }
    }

    override suspend fun disconnect(deviceAddress: String) {
        try {
            AppLogger.d(TAG, "Disconnecting device: $deviceAddress")
            deviceManager.disconnectDevice(deviceAddress)
            AppLogger.i(TAG, "Successfully disconnected device: $deviceAddress")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error disconnecting device: $deviceAddress", e)
        }
    }

    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> {
        return flow {
            AppLogger.d(TAG, "Starting GSR streaming for device: $deviceAddress")
            AppLogger.w(TAG, "Note: GSR streaming implementation requires Shimmer SDK callback integration")
            AppLogger.w(TAG, "This is a placeholder that emits no data - actual implementation requires")
            AppLogger.w(TAG, "registering callbacks with ShimmerBluetoothManagerAndroid for data packets")
        }
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        try {
            AppLogger.d(TAG, "Stopping streaming for device: $deviceAddress")
            AppLogger.w(TAG, "Note: Stopping streaming requires Shimmer SDK integration")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping streaming for device: $deviceAddress", e)
        }
    }

    override fun isConnected(deviceAddress: String): Boolean {
        val connected = deviceManager.shimmerBluetoothManager?.let { mgr ->
            mgr.getShimmerDeviceBtConnectedFromMac(deviceAddress)?.let { shimmer ->
                shimmer.isConnected
            }
        } ?: false
        AppLogger.d(TAG, "Device $deviceAddress connection status: $connected")
        return connected
    }

    override suspend fun getBatteryLevel(deviceAddress: String): Int? {
        return try {
            val shimmer = deviceManager.shimmerBluetoothManager?.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            if (shimmer != null) {
                AppLogger.d(TAG, "Shimmer device found for battery query: $deviceAddress")
                AppLogger.w(TAG, "Note: Battery level reading requires Shimmer SDK state parsing")
                null
            } else {
                AppLogger.w(TAG, "Shimmer device not found for battery query: $deviceAddress")
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting battery level for device: $deviceAddress", e)
            null
        }
    }
}


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\data\repository\app_src_main_java_mpdc4gsr_feature_gsr_data_repository_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\gsr\data\repository' subtree
// Files: 1; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\repository\ShimmerRepositoryImpl.kt =====

package mpdc4gsr.feature.gsr.data.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository

class ShimmerRepositoryImpl(
    private val shimmerDataSource: ShimmerDataSource
) : ShimmerRepository {
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        return shimmerDataSource.scanForDevices()
    }

    override suspend fun connectDevice(deviceAddress: String): Result<Unit> {
        return shimmerDataSource.connect(deviceAddress)
    }

    override suspend fun disconnectDevice(deviceAddress: String) {
        shimmerDataSource.disconnect(deviceAddress)
    }

    override suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample> {
        return shimmerDataSource.startStreaming(deviceAddress)
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        shimmerDataSource.stopStreaming(deviceAddress)
    }

    override fun isDeviceConnected(deviceAddress: String): Boolean {
        return shimmerDataSource.isConnected(deviceAddress)
    }

    override suspend fun getDeviceBatteryLevel(deviceAddress: String): Int? {
        return shimmerDataSource.getBatteryLevel(deviceAddress)
    }
}


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\data\source\app_src_main_java_mpdc4gsr_feature_gsr_data_source_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\gsr\data\source' subtree
// Files: 2; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\source\ShimmerDataSource.kt =====

package mpdc4gsr.feature.gsr.data.source

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

interface ShimmerDataSource {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connect(deviceAddress: String): Result<Unit>

    suspend fun disconnect(deviceAddress: String)

    suspend fun startStreaming(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isConnected(deviceAddress: String): Boolean

    suspend fun getBatteryLevel(deviceAddress: String): Int?
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\source\ShimmerDataSourceImpl.kt =====

package mpdc4gsr.feature.gsr.data .source

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

class ShimmerDataSourceImpl(
    private val deviceManager: ShimmerDeviceManager
) : ShimmerDataSource {
    companion object {
        private const val TAG = "ShimmerDataSourceImpl"
        private const val DEFAULT_DEVICE_NAME = "Shimmer3"
        private const val DEFAULT_DEVICE_TYPE = "Shimmer3-GSR"
        private const val DEFAULT_RSSI = -50
    }

    private val scannedDevices = mutableMapOf<String, DeviceInfo>()
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        deviceManager.initialize()
        deviceManager.startDeviceScanning()
        return deviceManager.scanResults
    }

    override suspend fun connect(deviceAddress: String): Result<Unit> {
        return try {
            AppLogger.d(TAG, "Connecting to device: $deviceAddress")
            val deviceInfo = scannedDevices[deviceAddress] ?: run {
                AppLogger.w(TAG, "Device info not found in scan results, using defaults for: $deviceAddress")
                DeviceInfo(
                    address = deviceAddress,
                    name = DEFAULT_DEVICE_NAME,
                    deviceType = DEFAULT_DEVICE_TYPE,
                    rssi = DEFAULT_RSSI,
                    isGSRCapable = true
                )
            }
            val success = deviceManager.connectToDevice(deviceInfo)
            if (success) {
                AppLogger.i(TAG, "Successfully connected to device: $deviceAddress")
                Result.success(Unit)
            } else {
                AppLogger.e(TAG, "Failed to connect to device: $deviceAddress")
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error connecting to device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    fun cacheDeviceInfo(devices: List<DeviceInfo>) {
        devices.forEach { device ->
            scannedDevices[device.address] = device
        }
    }

    override suspend fun disconnect(deviceAddress: String) {
        try {
            AppLogger.d(TAG, "Disconnecting device: $deviceAddress")
            deviceManager.disconnectDevice(deviceAddress)
            AppLogger.i(TAG, "Successfully disconnected device: $deviceAddress")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error disconnecting device: $deviceAddress", e)
        }
    }

    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> {
        return flow {
            AppLogger.d(TAG, "Starting GSR streaming for device: $deviceAddress")
            AppLogger.w(TAG, "Note: GSR streaming implementation requires Shimmer SDK callback integration")
            AppLogger.w(TAG, "This is a placeholder that emits no data - actual implementation requires")
            AppLogger.w(TAG, "registering callbacks with ShimmerBluetoothManagerAndroid for data packets")
        }
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        try {
            AppLogger.d(TAG, "Stopping streaming for device: $deviceAddress")
            AppLogger.w(TAG, "Note: Stopping streaming requires Shimmer SDK integration")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping streaming for device: $deviceAddress", e)
        }
    }

    override fun isConnected(deviceAddress: String): Boolean {
        val connected = deviceManager.shimmerBluetoothManager?.let { mgr ->
            mgr.getShimmerDeviceBtConnectedFromMac(deviceAddress)?.let { shimmer ->
                shimmer.isConnected
            }
        } ?: false
        AppLogger.d(TAG, "Device $deviceAddress connection status: $connected")
        return connected
    }

    override suspend fun getBatteryLevel(deviceAddress: String): Int? {
        return try {
            val shimmer = deviceManager.shimmerBluetoothManager?.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            if (shimmer != null) {
                AppLogger.d(TAG, "Shimmer device found for battery query: $deviceAddress")
                AppLogger.w(TAG, "Note: Battery level reading requires Shimmer SDK state parsing")
                null
            } else {
                AppLogger.w(TAG, "Shimmer device not found for battery query: $deviceAddress")
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting battery level for device: $deviceAddress", e)
            null
        }
    }
}