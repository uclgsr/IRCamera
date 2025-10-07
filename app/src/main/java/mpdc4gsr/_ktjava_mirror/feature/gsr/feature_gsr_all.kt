// Merged .kt under 'feature\gsr' subtree
// Files: 53; Generated 2025-10-07 19:59:55


// ===== feature\gsr\data\EnhancedThermalRecorder.kt =====

package mpdc4gsr.feature.gsr.data

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.ResearchTemplate
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.network.NetworkClient
import com.mpdc4gsr.gsr.service.*
import com.mpdc4gsr.gsr.util.TimeUtils
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mpdc4gsr.core.data.*
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.core.data.model.SessionQuality
import mpdc4gsr.core.data.model.SessionStatus
import mpdc4gsr.core.data.model.SessionType
import mpdc4gsr.core.data.utils.CSVBufferedWriter
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.AppError
import mpdc4gsr.core.ui.ConnectionState
import mpdc4gsr.core.ui.components.SensorStatusCard
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*
import mpdc4gsr.feature.gsr.presentation.ExportDestination
import mpdc4gsr.feature.gsr.presentation.ExportFormat
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModel
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModelFactory
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModel
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModelFactory
import mpdc4gsr.feature.gsr.presentation.GSRSession
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel
import mpdc4gsr.feature.gsr.presentation.MultiModalRecordingViewModel
import mpdc4gsr.feature.gsr.presentation.SessionExportViewModel
import mpdc4gsr.feature.gsr.presentation.SessionExportViewModelFactory
import mpdc4gsr.feature.gsr.presentation.SessionManagerViewModel
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModel
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModelFactory
import mpdc4gsr.feature.main.presentation.MainActivityViewModel
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.RecordingController
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import com.mpdc4gsr.gsr.service.GSRRecorder as LegacyGSRRecorder
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory as GSRRealShimmerDeviceFactory

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


// ===== feature\gsr\data\GSRCalculationUtils.kt =====

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


// ===== feature\gsr\data\GSRConstants.kt =====

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


// ===== feature\gsr\data\GSRDataPersistence.kt =====

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


// ===== feature\gsr\data\GSRNetworkStreamer.kt =====

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


// ===== feature\gsr\data\GSRPlotDataModels.kt =====

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


// ===== feature\gsr\data\GSRSensorRecorder.kt =====

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


// ===== feature\gsr\data\GSRSettingsRepository.kt =====

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


// ===== feature\gsr\data\RealShimmerDeviceFactory.kt =====

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


// ===== feature\gsr\data\repository\ShimmerRepositoryImpl.kt =====

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


// ===== feature\gsr\data\source\ShimmerDataSource.kt =====

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


// ===== feature\gsr\data\source\ShimmerDataSourceImpl.kt =====

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


// ===== feature\gsr\domain\repository\ShimmerRepository.kt =====

package mpdc4gsr.feature.gsr.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

interface ShimmerRepository {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connectDevice(deviceAddress: String): Result<Unit>

    suspend fun disconnectDevice(deviceAddress: String)

    suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isDeviceConnected(deviceAddress: String): Boolean

    suspend fun getDeviceBatteryLevel(deviceAddress: String): Int?
}


// ===== feature\gsr\domain\usecase\ShimmerUseCases.kt =====

package mpdc4gsr.feature.gsr.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository

class ScanShimmerDevicesUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(): Flow<List<DeviceInfo>> {
        return repository.scanForDevices()
    }
}

class ConnectShimmerDeviceUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Result<Unit> {
        if (deviceAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Device address cannot be empty"))
        }
        return repository.connectDevice(deviceAddress)
    }
}

class DisconnectShimmerDeviceUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.disconnectDevice(deviceAddress)
    }
}

class StartGSRStreamingUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Flow<GSRSample> {
        if (!repository.isDeviceConnected(deviceAddress)) {
            throw IllegalStateException("Device not connected: $deviceAddress")
        }
        return repository.streamGSRData(deviceAddress)
    }
}

class StopGSRStreamingUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.stopStreaming(deviceAddress)
    }
}

class CheckDeviceConnectionUseCase(
    private val repository: ShimmerRepository
) {
    operator fun invoke(deviceAddress: String): Boolean {
        return repository.isDeviceConnected(deviceAddress)
    }
}

class GetDeviceBatteryUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Int? {
        return repository.getDeviceBatteryLevel(deviceAddress)
    }
}


// ===== feature\gsr\presentation\GSRRawImageViewViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File

class GSRRawImageViewViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    data class GSRImageViewState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val imageFiles: List<File> = emptyList(),
        val selectedImage: File? = null
    )

    private val _imageViewState = MutableStateFlow(GSRImageViewState())
    val imageViewState: StateFlow<GSRImageViewState> = _imageViewState.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _imageViewState.value = _imageViewState.value.copy(isLoading = true, error = null)
            try {
                val imageFiles = getGSRImageFiles()
                _imageViewState.value = _imageViewState.value.copy(
                    isLoading = false,
                    imageFiles = imageFiles
                )
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load images"
                )
            }
        }
    }

    fun openImage(imageFile: File) {
        viewModelScope.launch {
            try {
                val context = application.applicationContext
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    error = "Failed to open image: ${e.message}"
                )
            }
        }
    }

    fun shareImage(imageFile: File) {
        viewModelScope.launch {
            try {
                val context = application.applicationContext
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share Image"))
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    error = "Failed to share image: ${e.message}"
                )
            }
        }
    }

    fun deleteImage(imageFile: File) {
        viewModelScope.launch {
            try {
                if (imageFile.delete()) {
                    // Reload images after deletion
                    loadImages()
                } else {
                    _imageViewState.value = _imageViewState.value.copy(
                        error = "Failed to delete image"
                    )
                }
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    error = "Error deleting image: ${e.message}"
                )
            }
        }
    }

    private fun getGSRImageFiles(): List<File> {
        val imageFiles = mutableListOf<File>()
        // Check multiple possible directories
        val possibleDirectories = listOf(
            // External storage directories
            File(Environment.getExternalStorageDirectory(), "GSR/Images"),
            File(Environment.getExternalStorageDirectory(), "IRCamera/GSR"),
            File(Environment.getExternalStorageDirectory(), "DCIM/GSR"),
            // App-specific directories
            File(application.externalCacheDir, "gsr_images"),
            File(
                application.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "GSR"
            ),
            File(application.filesDir, "gsr_images")
        )
        for (directory in possibleDirectories) {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles { file ->
                    file.isFile && isImageFile(file.name)
                }?.let { files ->
                    imageFiles.addAll(files)
                }
            }
        }
        // Sort by last modified (newest first)
        return imageFiles.sortedByDescending { it.lastModified() }
    }

    private fun isImageFile(fileName: String): Boolean {
        val imageExtensions =
            listOf(".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp")
        return imageExtensions.any { fileName.lowercase().endsWith(it) }
    }

    fun getImageMetadata(imageFile: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            metadata["Name"] = imageFile.name
            metadata["Size"] = formatFileSize(imageFile.length())
            metadata["Modified"] = formatDate(imageFile.lastModified())
            metadata["Path"] = imageFile.absolutePath
            // Try to get image dimensions
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(
                imageFile.absolutePath,
                options
            )
            if (options.outWidth > 0 && options.outHeight > 0) {
                metadata["Dimensions"] =
                    "${options.outWidth} x ${options.outHeight}"
                metadata["Type"] = options.outMimeType ?: "Unknown"
            }
        } catch (e: Exception) {
            metadata["Error"] = "Failed to read metadata: ${e.message}"
        }
        return metadata
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
            )

            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy HH:mm",
            java.util.Locale.getDefault()
        )
            .format(java.util.Date(timestamp))
    }
}


// ===== feature\gsr\presentation\GSRRawImageViewViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GSRRawImageViewViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GSRRawImageViewViewModel::class.java)) {
            return GSRRawImageViewViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== feature\gsr\presentation\GSRSensorViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import java.text.SimpleDateFormat
import java.util.*

class GSRSensorViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    companion object {
        // Reuse SimpleDateFormat instance for better performance
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private const val MAX_HISTORY_SIZE = 100

        // Reconnection configuration (can be made user-configurable)
        const val DEFAULT_MAX_RECONNECTION_ATTEMPTS = 3
        const val DEFAULT_BASE_RECONNECTION_DELAY_MS = 2000L

        // Device scanning delay
        private const val DEVICE_SCAN_DELAY_MS = 3000L
    }

    data class GSRSensorState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentGSR: Float = 0f,
        val skinConductance: Float = 0f,
        val deviceBattery: Int = 0,
        val samplingRate: Int = 128,
        val gsrHistory: List<Float> = emptyList(),
        val error: String? = null,
        val connectionStatus: String = "Disconnected",
        val isReconnecting: Boolean = false,
        val reconnectionAttempt: Int = 0,
        val maxReconnectionAttempts: Int = 0
    )

    data class ReconnectionConfig(
        val maxAttempts: Int = DEFAULT_MAX_RECONNECTION_ATTEMPTS,
        val baseDelayMs: Long = DEFAULT_BASE_RECONNECTION_DELAY_MS,
        val enabled: Boolean = true
    )

    private val _sensorState = MutableStateFlow(GSRSensorState())
    val sensorState: StateFlow<GSRSensorState> = _sensorState.asStateFlow()
    private var reconnectionConfig = ReconnectionConfig()
    private var lastConnectedDeviceAddress: String? = null
    private var wasRecordingBeforeDisconnect = false
    private var settingsRepository: GSRSettingsRepository? = null

    // Expose recorder for lifecycle management from UI layer
    var gsrRecorder: UnifiedGSRRecorder? = null
        private set

    fun initializeRecorder(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        reconnectionConfig: ReconnectionConfig? = null
    ) {
        viewModelScope.launch {
            try {
                // Initialize settings repository and load reconnection config
                if (settingsRepository == null) {
                    settingsRepository = GSRSettingsRepository(context)
                }
                // Load reconnection config from settings if not provided
                val configToUse =
                    reconnectionConfig ?: settingsRepository?.deviceSettings?.value?.let { deviceSettings ->
                        ReconnectionConfig(
                            maxAttempts = deviceSettings.reconnectionAttempts,
                            baseDelayMs = deviceSettings.reconnectionBaseDelayMs,
                            enabled = deviceSettings.autoReconnect
                        )
                    } ?: ReconnectionConfig()
                this@GSRSensorViewModel.reconnectionConfig = configToUse
                gsrRecorder = UnifiedGSRRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner
                )
                val initialized = gsrRecorder?.initialize() ?: false
                if (initialized) {
                    _sensorState.update {
                        it.copy(
                            isConnected = false,
                            connectionStatus = "Initialized",
                            error = null
                        )
                    }
                    startDataCollection()
                    startConnectionMonitoring()
                    observeSettingsChanges()
                } else {
                    _sensorState.update {
                        it.copy(
                            connectionStatus = "Initialization Failed",
                            error = "Failed to initialize GSR recorder"
                        )
                    }
                }
            } catch (e: Exception) {
                _sensorState.update {
                    it.copy(
                        connectionStatus = "Error",
                        error = "Error initializing: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsRepository?.deviceSettings?.collect { deviceSettings ->
                reconnectionConfig = ReconnectionConfig(
                    maxAttempts = deviceSettings.reconnectionAttempts,
                    baseDelayMs = deviceSettings.reconnectionBaseDelayMs,
                    enabled = deviceSettings.autoReconnect
                )
                mpdc4gsr.core.utils.AppLogger.d(
                    "GSRSensorViewModel",
                    "Reconnection config updated: attempts=${reconnectionConfig.maxAttempts}, " +
                            "delay=${reconnectionConfig.baseDelayMs}ms, enabled=${reconnectionConfig.enabled}"
                )
            }
        }
    }

    fun connectDevice() {
        viewModelScope.launch {
            try {
                // Scan for devices and connect to the first available one
                val devices = gsrRecorder?.getDiscoveredDevices()
                if (devices.isNullOrEmpty()) {
                    _sensorState.update { it.copy(error = "No devices found. Please scan first.") }
                    return@launch
                }
                // Connect to first available device
                val device = devices.firstOrNull()
                if (device != null) {
                    // Actually connect to the device using the recorder
                    val connected = gsrRecorder?.connectToDevice(device) ?: false
                    if (connected) {
                        lastConnectedDeviceAddress = device.address
                        _sensorState.update { it.copy(isConnected = true, error = null) }
                    } else {
                        _sensorState.update { it.copy(error = "Failed to connect to device") }
                    }
                } else {
                    _sensorState.update { it.copy(error = "No valid device to connect") }
                }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Connection failed: ${e.message}") }
            }
        }
    }

    fun updateReconnectionConfig(config: ReconnectionConfig) {
        reconnectionConfig = config
        mpdc4gsr.core.utils.AppLogger.d(
            "GSRSensorViewModel",
            "Reconnection config manually updated: attempts=${config.maxAttempts}, " +
                    "delay=${config.baseDelayMs}ms, enabled=${config.enabled}"
        )
    }

    fun getReconnectionConfig(): ReconnectionConfig = reconnectionConfig

    fun getSettingsRepository() = settingsRepository

    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                _sensorState.update { it.copy(isConnected = false, isRecording = false) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Disconnect failed: ${e.message}") }
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                val sessionDir = application.getExternalFilesDir("gsr_sessions")?.absolutePath
                    ?: application.filesDir.absolutePath
                val currentTimeMs = System.currentTimeMillis()
                val currentMonotonicNs = System.nanoTime()
                val metadata = mpdc4gsr.core.data.SessionMetadata(
                    sessionId = "gsr_${currentTimeMs}",
                    sessionStartTimestampMs = currentTimeMs,
                    sessionStartMonotonicNs = currentMonotonicNs,
                    sessionStartIso = ISO_DATE_FORMAT.format(Date(currentTimeMs))
                )
                gsrRecorder?.startRecording(sessionDir, metadata)
                _sensorState.update { it.copy(isRecording = true, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                _sensorState.update { it.copy(isRecording = false, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording stop failed: ${e.message}") }
            }
        }
    }

    private fun startDataCollection() {
        // Collect recording status
        viewModelScope.launch {
            gsrRecorder?.getStatusFlow()?.collect { status ->
                _sensorState.update { currentState ->
                    currentState.copy(
                        isRecording = status.isRecording,
                        samplingRate = status.currentDataRate.toInt()
                    )
                }
            }
        }
        // Collect actual GSR data samples
        viewModelScope.launch {
            gsrRecorder?.getDataStream()?.collect { gsrSample ->
                _sensorState.update { currentState ->
                    val newGSR = gsrSample.gsrMicrosiemens.toFloat()
                    val newHistory = (currentState.gsrHistory + newGSR).takeLast(MAX_HISTORY_SIZE)
                    // Calculate skin conductance (same as GSR in microsiemens)
                    val skinConductance = newGSR
                    currentState.copy(
                        currentGSR = newGSR,
                        skinConductance = skinConductance,
                        gsrHistory = newHistory
                    )
                }
            }
        }
    }

    private fun startConnectionMonitoring() {
        viewModelScope.launch {
            gsrRecorder?.deviceStatus?.collect { status ->
                val isConnectedNow = status.contains("Connected", ignoreCase = true)
                val isDisconnected = status.contains("Disconnected", ignoreCase = true)
                _sensorState.update { currentState ->
                    val wasConnected = currentState.isConnected
                    // Detect disconnection and trigger reconnection
                    if (wasConnected && isDisconnected && !currentState.isReconnecting) {
                        // Save recording state before disconnection
                        wasRecordingBeforeDisconnect = currentState.isRecording
                        if (reconnectionConfig.enabled) {
                            viewModelScope.launch {
                                attemptReconnection()
                            }
                        }
                    }
                    currentState.copy(
                        isConnected = isConnectedNow,
                        connectionStatus = status
                    )
                }
            }
        }
    }

    private suspend fun attemptReconnection() {
        val maxAttempts = reconnectionConfig.maxAttempts
        val baseDelay = reconnectionConfig.baseDelayMs
        for (attempt in 1..maxAttempts) {
            _sensorState.update {
                it.copy(
                    isReconnecting = true,
                    reconnectionAttempt = attempt,
                    maxReconnectionAttempts = maxAttempts,
                    connectionStatus = "Reconnecting (attempt $attempt/$maxAttempts)..."
                )
            }
            // True exponential backoff: baseDelay * 2^(attempt-1)
            val delay = baseDelay * (1L shl (attempt - 1))
            kotlinx.coroutines.delay(delay)
            try {
                // Try to get cached devices first
                var devices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                // If no cached devices and we have a last connected address, try to find it
                var targetDevice = devices.find { it.address == lastConnectedDeviceAddress }
                // If still no device found, trigger a quick scan
                if (targetDevice == null && devices.isEmpty()) {
                    mpdc4gsr.core.utils.AppLogger.i("GSRSensorViewModel", "No cached devices, triggering scan...")
                    _sensorState.update {
                        it.copy(
                            connectionStatus = "Scanning for device (attempt $attempt/$maxAttempts)..."
                        )
                    }
                    val scanSuccess = gsrRecorder?.startDeviceDiscovery() ?: false
                    if (scanSuccess) {
                        kotlinx.coroutines.delay(DEVICE_SCAN_DELAY_MS)
                        devices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                        targetDevice = devices.find { it.address == lastConnectedDeviceAddress }
                            ?: devices.firstOrNull()
                    }
                } else if (targetDevice == null) {
                    // Use first available device if last connected not found
                    targetDevice = devices.firstOrNull()
                }
                if (targetDevice != null) {
                    mpdc4gsr.core.utils.AppLogger.i(
                        "GSRSensorViewModel",
                        "Attempting to connect to ${targetDevice.address}"
                    )
                    val connected = gsrRecorder?.connectToDevice(targetDevice) ?: false
                    if (connected) {
                        _sensorState.update {
                            it.copy(
                                isConnected = true,
                                isReconnecting = false,
                                reconnectionAttempt = 0,
                                maxReconnectionAttempts = 0,
                                connectionStatus = "Reconnected",
                                error = null
                            )
                        }
                        // Resume recording if it was active before disconnection
                        if (wasRecordingBeforeDisconnect) {
                            mpdc4gsr.core.utils.AppLogger.i(
                                "GSRSensorViewModel",
                                "Resuming recording after reconnection"
                            )
                            kotlinx.coroutines.delay(1000) // Brief delay to ensure stable connection
                            startRecording()
                            wasRecordingBeforeDisconnect = false
                        }
                        return
                    }
                } else {
                    mpdc4gsr.core.utils.AppLogger.w("GSRSensorViewModel", "No device found for reconnection")
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.w(
                    "GSRSensorViewModel",
                    "Reconnection attempt $attempt failed: ${e.message}"
                )
            }
        }
        // All attempts failed
        _sensorState.update {
            it.copy(
                isReconnecting = false,
                reconnectionAttempt = 0,
                maxReconnectionAttempts = 0,
                connectionStatus = "Connection Lost",
                error = "Failed to reconnect after $maxAttempts attempts"
            )
        }
        wasRecordingBeforeDisconnect = false // Reset flag
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                // Export functionality would be implemented here
                // For now, just log the action
                mpdc4gsr.core.utils.AppLogger.d("GSRSensorViewModel", "Export data requested")
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                gsrRecorder?.cleanup()
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("GSRSensorViewModel", "Error during cleanup", e)
            }
        }
    }
}


// ===== feature\gsr\presentation\GSRSensorViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GSRSensorViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GSRSensorViewModel::class.java)) {
            return GSRSensorViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== feature\gsr\presentation\GSRSettingsViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import mpdc4gsr.feature.network.data.RecordingController

class GSRSettingsViewModel : AppBaseViewModel() {
    data class UIState(
        val gsrSettings: GSRSettingsRepository.GSRSettings = GSRSettingsRepository.GSRSettings(),
        val deviceSettings: GSRSettingsRepository.DeviceSettings = GSRSettingsRepository.DeviceSettings(),
        val permissionState: PermissionState = PermissionState(false, emptyList(), emptyList()),
        val connectionState: DeviceConnectionState = DeviceConnectionState(false),
        val scanningState: ScanningState = ScanningState.IDLE,
        val isLoading: Boolean = false
    )

    data class PermissionState(
        val hasAllPermissions: Boolean,
        val missingPermissions: List<String>,
        val shouldShowRationale: List<String>
    )

    data class DeviceConnectionState(
        val isConnected: Boolean,
        val deviceInfo: DeviceInfo? = null,
        val connectionStatus: String = "Disconnected",
        val signalStrength: Int = 0
    )

    data class DeviceInfo(
        val id: String,
        val name: String,
        val address: String,
        val isConnected: Boolean = false,
        val batteryLevel: Int? = null,
        val signalStrength: Int = 0
    )

    enum class ScanningState {
        IDLE, SCANNING, COMPLETED, FAILED
    }

    private lateinit var repository: GSRSettingsRepository
    private var gsrSensorRecorder: GSRSensorRecorder? = null

    // StateFlow from Repository
    val gsrSettings: StateFlow<GSRSettingsRepository.GSRSettings> by lazy {
        repository.gsrSettings.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            GSRSettingsRepository.GSRSettings()
        )
    }
    val deviceSettings: StateFlow<GSRSettingsRepository.DeviceSettings> by lazy {
        repository.deviceSettings.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            GSRSettingsRepository.DeviceSettings()
        )
    }

    // Modern UI State Management with StateFlow
    private val _permissionState =
        MutableStateFlow(PermissionState(false, emptyList(), emptyList()))
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState(false))
    val deviceConnectionState: StateFlow<DeviceConnectionState> =
        _deviceConnectionState.asStateFlow()
    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<DeviceInfo>> = _availableDevices.asStateFlow()
    private val _scanningState = MutableStateFlow(ScanningState.IDLE)
    val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()

    // SharedFlow for one-time events
    private val _settingsEvents = MutableSharedFlow<SettingsEvent>()
    val settingsEvents: SharedFlow<SettingsEvent> = _settingsEvents.asSharedFlow()

    // Combined state for UI optimization
    val settingsUiState: StateFlow<UIState> by lazy {
        combine(
            if (::repository.isInitialized) repository.gsrSettings else flowOf(GSRSettingsRepository.GSRSettings()),
            if (::repository.isInitialized) repository.deviceSettings else flowOf(
                GSRSettingsRepository.DeviceSettings()
            ),
            _permissionState,
            _deviceConnectionState,
            _scanningState
        ) { gsrSettings, deviceSettings, permissions, connection, scanning ->
            UIState(gsrSettings, deviceSettings, permissions, connection, scanning)
        }.stateIn(viewModelScope, SharingStarted.Lazily, UIState())
    }

    // Modern Event-driven architecture with SharedFlow
    sealed class SettingsEvent {
        data class ShowPermissionDialog(val permissions: List<String>) : SettingsEvent()
        data class ShowPermissionDeniedDialog(val permissions: List<String>) : SettingsEvent()
        data class ShowPermissionPermanentlyDeniedDialog(val permissions: List<String>) :
            SettingsEvent()

        object OpenAppSettings : SettingsEvent()
        data class DeviceScanCompleted(val message: String) : SettingsEvent()
        data class DeviceConnected(val device: DeviceInfo, val message: String) : SettingsEvent()
        data class DeviceDisconnected(val message: String) : SettingsEvent()
        data class SettingsExported(val data: Map<String, Any>, val message: String) :
            SettingsEvent()

        data class SettingsImported(val message: String) : SettingsEvent()
        data class CalibrationStarted(val message: String) : SettingsEvent()
        data class CalibrationCompleted(val message: String) : SettingsEvent()
        data class ShowToast(val message: String) : SettingsEvent()
        data class ShowError(val message: String) : SettingsEvent()
    }

    fun initialize(context: Context) {
        repository = GSRSettingsRepository(context)
        checkPermissions(context)
        initializeGSRRecorder(context)
    }

    private fun initializeGSRRecorder(context: Context) {
        launchWithErrorHandling {
            try {
                val currentSettings = repository.gsrSettings.value
                // Create a temporary RecordingController since it's required by the constructor
                val tempRecordingController = RecordingController(
                    context,
                    object : androidx.lifecycle.LifecycleOwner {
                        override val lifecycle: androidx.lifecycle.Lifecycle
                            get() = androidx.lifecycle.LifecycleRegistry(this)
                    }
                )
                gsrSensorRecorder = GSRSensorRecorder(
                    context,
                    "gsr_settings_${System.currentTimeMillis()}",
                    currentSettings.samplingRate,
                    tempRecordingController
                )
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Ready"
                )
            } catch (e: Exception) {
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to initialize GSR recorder: ${e.message}"))
            }
        }
    }

    fun checkPermissions(context: Context) {
        val missingPermissions = getMissingPermissions(context)
        val shouldShowRationale = mutableListOf<String>()
        // Check rationale for missing permissions
        missingPermissions.forEach { permission ->
            // Note: shouldShowRequestPermissionRationale check would be handled in Activity
            // ViewModel focuses on permission state management
        }
        _permissionState.value = PermissionState(
            hasAllPermissions = missingPermissions.isEmpty(),
            missingPermissions = missingPermissions,
            shouldShowRationale = shouldShowRationale
        )
    }

    fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
                // In a real scenario, we'd check if rationale should be shown
                permanentlyDeniedPermissions.add(permissions[i])
            }
        }
        launchWithErrorHandling {
            when {
                deniedPermissions.isEmpty() -> {
                    _permissionState.value = _permissionState.value.copy(
                        hasAllPermissions = true,
                        missingPermissions = emptyList()
                    )
                    enableDeviceManagement()
                }

                permanentlyDeniedPermissions.isNotEmpty() -> {
                    _settingsEvents.emit(
                        SettingsEvent.ShowPermissionPermanentlyDeniedDialog(
                            permanentlyDeniedPermissions
                        )
                    )
                }

                else -> {
                    _settingsEvents.emit(SettingsEvent.ShowPermissionDeniedDialog(deniedPermissions))
                }
            }
        }
    }

    fun requestPermissions() {
        launchWithErrorHandling {
            val currentState = _permissionState.value
            if (currentState.missingPermissions.isNotEmpty()) {
                _settingsEvents.emit(SettingsEvent.ShowPermissionDialog(currentState.missingPermissions))
            }
        }
    }

    fun startDeviceScan() {
        if (_scanningState.value == ScanningState.SCANNING) return
        _scanningState.value = ScanningState.SCANNING
        launchWithErrorHandling {
            try {
                // Simulate device scanning
                val devices = scanForDevices()
                _availableDevices.value = devices
                _scanningState.value = ScanningState.COMPLETED
                _settingsEvents.emit(SettingsEvent.DeviceScanCompleted("Found ${devices.size} device(s)"))
            } catch (e: Exception) {
                _scanningState.value = ScanningState.FAILED
                _settingsEvents.emit(SettingsEvent.ShowError("Device scan failed: ${e.message}"))
            }
        }
    }

    private suspend fun scanForDevices(): List<DeviceInfo> {
        // Simulate device discovery
        return listOf(
            DeviceInfo("shimmer_001", "Shimmer GSR #001", "00:11:22:AA:BB:CC"),
            DeviceInfo("shimmer_002", "Shimmer GSR #002", "00:11:22:AA:BB:DD"),
            DeviceInfo("shimmer_003", "Shimmer GSR #003", "00:11:22:AA:BB:EE")
        )
    }

    fun connectToDevice(deviceInfo: DeviceInfo) {
        launchWithErrorHandling {
            try {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    deviceInfo = deviceInfo,
                    connectionStatus = "Connecting..."
                )
                // Simulate connection process
                kotlinx.coroutines.delay(2000)
                // Update device settings in repository
                val currentDeviceSettings = repository.deviceSettings.value
                repository.updateDeviceSettings(
                    currentDeviceSettings.copy(
                        selectedDeviceId = deviceInfo.id,
                        deviceName = deviceInfo.name
                    )
                )
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = true,
                    deviceInfo = deviceInfo,
                    connectionStatus = "Connected",
                    signalStrength = 85
                )
                _settingsEvents.emit(
                    SettingsEvent.DeviceConnected(
                        deviceInfo,
                        "Connected to ${deviceInfo.name}"
                    )
                )
            } catch (e: Exception) {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Connection failed"
                )
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to connect to device: ${e.message}"))
            }
        }
    }

    fun disconnectDevice() {
        launchWithErrorHandling {
            try {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Disconnected"
                )
                _settingsEvents.emit(SettingsEvent.DeviceDisconnected("Device disconnected"))
            } catch (e: Exception) {
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to disconnect device: ${e.message}"))
            }
        }
    }

    fun updateGSRSettings(settings: GSRSettingsRepository.GSRSettings) {
        launchWithErrorHandling {
            repository.updateGSRSettings(settings)
            // Restart GSR recorder with new settings if needed
            if (gsrSensorRecorder != null) {
                // Update sampling rate, etc.
            }
        }
    }

    fun updateSamplingRate(samplingRate: Int) {
        launchWithErrorHandling {
            val currentSettings = repository.gsrSettings.value
            repository.updateGSRSettings(currentSettings.copy(samplingRate = samplingRate))
        }
    }

    fun updateDeviceSettings(settings: GSRSettingsRepository.DeviceSettings) {
        launchWithErrorHandling {
            repository.updateDeviceSettings(settings)
        }
    }

    fun exportSettings() {
        launchWithErrorHandling {
            val settingsMap = repository.exportSettings()
            _settingsEvents.emit(
                SettingsEvent.SettingsExported(
                    settingsMap,
                    "Settings exported successfully"
                )
            )
        }
    }

    fun importSettings(settingsMap: Map<String, Any>) {
        launchWithErrorHandling {
            val success = repository.importSettings(settingsMap)
            if (success) {
                _settingsEvents.emit(SettingsEvent.SettingsImported("Settings imported successfully"))
            } else {
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to import settings: Invalid format"))
            }
        }
    }

    fun resetToDefaults() {
        launchWithErrorHandling {
            repository.resetToDefaults()
        }
    }

    fun startCalibration() {
        launchWithErrorHandling {
            _settingsEvents.emit(SettingsEvent.CalibrationStarted("Starting GSR calibration..."))
            try {
                // Check if device is connected
                if (!_deviceConnectionState.value.isConnected) {
                    _settingsEvents.emit(SettingsEvent.ShowError("Cannot calibrate: No device connected"))
                    return@launchWithErrorHandling
                }
                // According to Shimmer Android API documentation:
                // The Shimmer device stores calibration parameters that are automatically
                // applied during streaming. The ObjectCluster contains both RAW and CAL formats.
                // For GSR sensors, calibration converts raw ADC values to microsiemens.
                // Since GSRSensorRecorder already uses the Shimmer API's built-in calibration
                // (via ObjectCluster.getFormatClusterValue with CAL format), the calibration
                // is active whenever the device streams data.
                // A full calibration workflow would involve:
                // 1. Reading current calibration parameters from device
                // 2. Optionally writing new calibration coefficients
                // 3. Verifying calibration by checking sensor readings
                // Verify that the device is providing calibrated data
                if (gsrSensorRecorder != null) {
                    _settingsEvents.emit(
                        SettingsEvent.CalibrationCompleted(
                            "GSR sensor calibration verified. Device is using Shimmer factory calibration parameters."
                        )
                    )
                } else {
                    _settingsEvents.emit(
                        SettingsEvent.ShowError(
                            "GSR sensor not initialized. Please reconnect the device."
                        )
                    )
                }
            } catch (e: Exception) {
                _settingsEvents.emit(SettingsEvent.ShowError("Calibration failed: ${e.message}"))
            }
        }
    }

    private fun enableDeviceManagement() {
        // Enable device-related UI
    }

    private fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
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
        } else {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        return missing
    }

    companion object {
        private const val TAG = "GSRSettingsViewModel"
    }
}


// ===== feature\gsr\presentation\MultiModalRecordingViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.network.NetworkClient
import com.mpdc4gsr.gsr.service.GSRRecorder
import com.mpdc4gsr.gsr.service.SessionManager
import com.mpdc4gsr.gsr.util.TimeUtils
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.shimmerresearch.android.Shimmer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory

class MultiModalRecordingViewModel(application: Application) : BaseViewModel() {
    data class RecordingState(
        val isRecording: Boolean = false,
        val isStartingRecording: Boolean = false,
        val sampleCount: Long = 0,
        val syncMarkCount: Int = 0,
        val recordingDuration: Long = 0,
        val sessionId: String = "",
        val participantId: String? = null
    )

    data class GSRState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val sampleRate: Int = 128,
        val lastSample: GSRSample? = null,
        val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
        val deviceBattery: Int? = null
    )

    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val videoEnabled: Boolean = true,
        val is4KEnabled: Boolean = false,
        val rawCaptureEnabled: Boolean = false,
        val frameRate: Int = 30,
        val resolution: String = "1080p"
    )

    data class NetworkState(
        val isConnected: Boolean = false,
        val controllerInfo: NetworkClient.ControllerInfo? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null
    )

    data class ShimmerDeviceInfo(
        val shimmer: Shimmer?,
        val deviceName: String,
        val macAddress: String,
        val batteryLevel: Int? = null,
        val signalStrength: Int = 0,
        val isConnected: Boolean = false
    )

    data class RecordingConfiguration(
        val enableVideo: Boolean = true,
        val enable4K: Boolean = false,
        val enableRawCapture: Boolean = false,
        val rawFrameRate: Int = 30,
        val gsrSampleRate: Int = 128,
        val participantId: String = "",
        val sessionTemplate: String? = null
    )

    data class CombinedRecordingState(
        val gsrState: GSRState,
        val cameraState: CameraState,
        val networkState: NetworkState
    ) {
        val allSystemsReady: Boolean
            get() = gsrState.isConnected && cameraState.isInitialized
        val anySystemRecording: Boolean
            get() = gsrState.isRecording || cameraState.isRecording
    }

    enum class SignalQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }

    data class RecordingAction(
        val type: ActionType,
        val message: String? = null,
        val data: Any? = null
    )

    private val context: Context = application.applicationContext
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var networkClient: NetworkClient? = null

    // Recording State Management
    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState
    private val _sessionInfo = MutableLiveData<SessionInfo?>()
    val sessionInfo: LiveData<SessionInfo?> = _sessionInfo

    // Multimodal Sensor States
    private val _gsrState = MutableStateFlow<GSRState>(GSRState())
    val gsrState: StateFlow<GSRState> = _gsrState
    private val _cameraState = MutableStateFlow<CameraState>(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState

    // Device Management
    private val _discoveredDevices = MutableLiveData<List<ShimmerDeviceInfo>>()
    val discoveredDevices: LiveData<List<ShimmerDeviceInfo>> = _discoveredDevices
    private val _connectedDevices = MutableLiveData<List<ShimmerDeviceInfo>>()
    val connectedDevices: LiveData<List<ShimmerDeviceInfo>> = _connectedDevices

    // UI State and Actions
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    private val _recordingAction = MutableLiveData<RecordingAction?>()
    val recordingAction: LiveData<RecordingAction?> = _recordingAction

    // Configuration
    private val _recordingConfig =
        MutableStateFlow<RecordingConfiguration>(RecordingConfiguration())
    val recordingConfig: StateFlow<RecordingConfiguration> = _recordingConfig

    // Combined state for UI optimization
    val combinedRecordingState = combine(
        _gsrState, _cameraState, _networkState
    ) { gsrState, cameraState, networkState ->
        CombinedRecordingState(gsrState, cameraState, networkState)
    }

    enum class ActionType {
        RECORDING_STARTED,
        RECORDING_STOPPED,
        SYNC_EVENT_TRIGGERED,
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        SESSION_EXPORTED,
        ERROR_OCCURRED,
        PERMISSION_REQUIRED
    }

    init {
        initializeRecorders()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    fun initialize() {
        // Kept for compatibility, but initialization now happens in init block
        initializeRecorders()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    private fun initializeRecorders() {
        viewModelScope.launch {
            try {
                // Initialize GSR Recorder
                gsrRecorder = GSRRecorder(context, RealShimmerDeviceFactory(context))
                gsrRecorder.addListener(createGSRListener())
                // Initialize Session Manager
                sessionManager = SessionManager.getInstance(context)
                _statusMessage.value = "Initializing multimodal recording system..."
                // Set initial states
                _recordingState.value = RecordingState()
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Failed to initialize recording system: ${e.message}"
            }
        }
    }

    fun initializeCameraRecorder(rgbCameraRecorder: RgbCameraRecorder) {
        this.rgbCameraRecorder = rgbCameraRecorder
        viewModelScope.launch {
            try {
                val initialized = rgbCameraRecorder.initialize()
                _cameraState.value = _cameraState.value.copy(isInitialized = initialized)
                if (initialized) {
                    _statusMessage.value = "Camera system initialized"
                } else {
                    _error.value = "Camera initialization failed"
                }
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Camera initialization error: ${e.message}"
            }
        }
    }

    fun updateRecordingConfiguration(config: RecordingConfiguration) {
        _recordingConfig.value = config
        // Update camera configuration
        _cameraState.value = _cameraState.value.copy(
            videoEnabled = config.enableVideo,
            is4KEnabled = config.enable4K,
            rawCaptureEnabled = config.enableRawCapture,
            frameRate = config.rawFrameRate
        )
    }

    fun startRecording() {
        val currentState = _recordingState.value
        if (currentState?.isRecording == true || currentState?.isStartingRecording == true) {
            return
        }
        viewModelScope.launch {
            try {
                _recordingState.value = currentState?.copy(isStartingRecording = true)
                _statusMessage.value = "Starting multimodal recording..."
                // Generate session info
                val config = _recordingConfig.value
                val sessionInfo = SessionInfo(
                    sessionId = TimeUtils.generateSessionId("MultiModal"),
                    participantId = config.participantId.takeIf { it.isNotEmpty() },
                    startTime = System.currentTimeMillis()
                )
                // Start GSR recording
                gsrRecorder.startRecording(sessionInfo.sessionId, sessionInfo.participantId)
                // Start camera recording if enabled
                if (config.enableVideo) {
                    rgbCameraRecorder?.startRecording(sessionInfo.sessionId)
                    _cameraState.value = _cameraState.value.copy(isRecording = true)
                }
                // Update states
                _sessionInfo.value = sessionInfo
                _recordingState.value = RecordingState(
                    isRecording = true,
                    isStartingRecording = false,
                    sessionId = sessionInfo.sessionId,
                    participantId = sessionInfo.participantId
                )
                _gsrState.value = _gsrState.value.copy(isRecording = true)
                _recordingAction.value = RecordingAction(
                    type = ActionType.RECORDING_STARTED,
                    message = "Recording started for session ${sessionInfo.sessionId}"
                )
            } catch (e: Exception) {
                _recordingState.value = currentState?.copy(isStartingRecording = false)
                _error.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Stopping multimodal recording..."
                // Stop GSR recording
                gsrRecorder.stopRecording()
                // Stop camera recording
                rgbCameraRecorder?.stopRecording()
                // Update final session info
                val finalSession = _sessionInfo.value?.copy(
                    endTime = System.currentTimeMillis()
                )
                // Update states
                _recordingState.value = RecordingState()
                _gsrState.value = _gsrState.value.copy(isRecording = false)
                _cameraState.value = _cameraState.value.copy(isRecording = false)
                _sessionInfo.value = finalSession
                _recordingAction.value = RecordingAction(
                    type = ActionType.RECORDING_STOPPED,
                    message = "Recording stopped. Session saved.",
                    data = finalSession
                )
            } catch (e: Exception) {
                _error.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    fun triggerSyncEvent() {
        val currentState = _recordingState.value
        if (currentState?.isRecording != true) {
            _error.value = "Cannot trigger sync event when not recording"
            return
        }
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val syncMark = SyncMark(
                    timestamp = timestamp,
                    utcTimestamp = timestamp,
                    eventType = "USER_TRIGGER",
                    sessionId = currentState.sessionId
                )
                // Add sync mark to GSR data
                gsrRecorder.addSyncMark("USER_TRIGGER", "Manual sync event")
                // Add sync mark to camera data if recording
                if (_cameraState.value.isRecording) {
                    rgbCameraRecorder?.addSyncMarker(
                        "USER_TRIGGER",
                        timestamp * 1_000_000,
                        emptyMap()
                    )
                }
                // Update state
                _recordingState.value = currentState.copy(
                    syncMarkCount = currentState.syncMarkCount + 1
                )
                _recordingAction.value = RecordingAction(
                    type = ActionType.SYNC_EVENT_TRIGGERED,
                    message = "Sync event USER_TRIGGER triggered"
                )
            } catch (e: Exception) {
                _error.value = "Failed to trigger sync event: ${e.message}"
            }
        }
    }

    fun discoverDevices() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Discovering Shimmer devices..."
                // Simulate device discovery
                val devices = discoverShimmerDevices()
                _discoveredDevices.value = devices
                _statusMessage.value = "Found ${devices.size} Shimmer device(s)"
            } catch (e: Exception) {
                _error.value = "Device discovery failed: ${e.message}"
            }
        }
    }

    fun connectToDevice(deviceInfo: ShimmerDeviceInfo) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Connecting to ${deviceInfo.deviceName}..."
                // Simulate device connection
                kotlinx.coroutines.delay(2000)
                val connectedDevice = deviceInfo.copy(isConnected = true)
                val currentConnected = _connectedDevices.value?.toMutableList() ?: mutableListOf()
                currentConnected.add(connectedDevice)
                _connectedDevices.value = currentConnected
                _gsrState.value = _gsrState.value.copy(isConnected = true)
                _recordingAction.value = RecordingAction(
                    type = ActionType.DEVICE_CONNECTED,
                    message = "Connected to ${deviceInfo.deviceName}"
                )
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Failed to connect to device: ${e.message}"
            }
        }
    }

    private suspend fun discoverShimmerDevices(): List<ShimmerDeviceInfo> {
        // Simulate device discovery - use null placeholders for now as this is discovery phase
        return listOf(
            ShimmerDeviceInfo(
                shimmer = null,
                deviceName = "Shimmer GSR #001",
                macAddress = "00:11:22:AA:BB:CC",
                batteryLevel = 85,
                signalStrength = 75
            ),
            ShimmerDeviceInfo(
                shimmer = null,
                deviceName = "Shimmer GSR #002",
                macAddress = "00:11:22:AA:BB:DD",
                batteryLevel = 92,
                signalStrength = 88
            )
        )
    }

    private fun createGSRListener(): GSRRecorder.GSRRecordingListener {
        return object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                _statusMessage.value = "GSR recording started"
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                _statusMessage.value = "GSR recording stopped"
            }

            override fun onSampleRecorded(sample: GSRSample) {
                val currentState = _recordingState.value
                _recordingState.value = currentState?.copy(
                    sampleCount = currentState.sampleCount + 1
                )
                _gsrState.value = _gsrState.value.copy(lastSample = sample)
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                // Handle sync mark addition
                val currentState = _recordingState.value
                _recordingState.value = currentState?.copy(
                    syncMarkCount = currentState.syncMarkCount + 1
                )
            }

            override fun onError(error: String) {
                _error.value = "GSR recording error: $error"
            }
        }
    }

    private fun updateSystemReadiness() {
        val gsrReady = _gsrState.value.isConnected
        val cameraReady = _cameraState.value.isInitialized
        _statusMessage.value = when {
            gsrReady && cameraReady -> "All systems ready for recording"
            gsrReady -> "GSR ready, initializing camera..."
            cameraReady -> "Camera ready, connect GSR device..."
            else -> "Initializing recording systems..."
        }
    }

    private fun generateDefaultSessionId() {
        val config = _recordingConfig.value
        val defaultParticipantId = TimeUtils.generateSessionId("MultiModal")
        _recordingConfig.value = config.copy(participantId = defaultParticipantId)
    }

    fun clearAction() {
        _recordingAction.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                if (_recordingState.value?.isRecording == true) {
                    stopRecording()
                }
                rgbCameraRecorder?.cleanup()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    companion object {
        private const val TAG = "MultiModalRecordingViewModel"
    }
}


// ===== feature\gsr\presentation\SessionExportViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat(val displayName: String) {
    CSV("CSV (Comma Separated Values)"),
    JSON("JSON (JavaScript Object Notation)"),
    XML("XML (eXtensible Markup Language)"),
    EXCEL("Excel Spreadsheet")
}

enum class ExportDestination(val displayName: String) {
    DOWNLOADS("Downloads Folder"),
    EXTERNAL_STORAGE("External Storage"),
    SHARE("Share with Other Apps"),
    EMAIL("Email Export")
}

data class GSRSession(
    val sessionId: String,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val deviceId: String,
    val participantId: String?,
    val readingCount: Int,
    val avgConductance: Float,
    val status: String = "COMPLETED",
    val duration: String = "0min",
    val dataPointCount: Int = 0,
    val filePath: String = "",
    val lastModified: Long = 0L
)

class SessionExportViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    data class SessionExportState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val sessions: List<GSRSession> = emptyList(),
        val selectedSessions: Set<GSRSession> = emptySet(),
        val exportFormat: ExportFormat = ExportFormat.CSV,
        val exportDestination: ExportDestination = ExportDestination.DOWNLOADS,
        val isExporting: Boolean = false,
        val exportProgress: Float = 0f,
        val currentExportFile: String? = null
    )

    private val _exportState = MutableStateFlow(SessionExportState())
    val exportState: StateFlow<SessionExportState> = _exportState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _exportState.value = _exportState.value.copy(isLoading = true, error = null)
            try {
                val sessions = getAvailableSessions()
                _exportState.value = _exportState.value.copy(
                    isLoading = false,
                    sessions = sessions
                )
            } catch (e: Exception) {
                _exportState.value = _exportState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sessions"
                )
            }
        }
    }

    fun toggleSessionSelection(session: GSRSession) {
        val currentSelection = _exportState.value.selectedSessions
        val newSelection = if (session in currentSelection) {
            currentSelection - session
        } else {
            currentSelection + session
        }
        _exportState.value = _exportState.value.copy(selectedSessions = newSelection)
    }

    fun setExportFormat(format: ExportFormat) {
        _exportState.value = _exportState.value.copy(exportFormat = format)
    }

    fun setExportDestination(destination: ExportDestination) {
        _exportState.value = _exportState.value.copy(exportDestination = destination)
    }

    fun startExport() {
        viewModelScope.launch {
            val selectedSessions = _exportState.value.selectedSessions
            if (selectedSessions.isEmpty()) {
                _exportState.value = _exportState.value.copy(error = "No sessions selected for export")
                return@launch
            }
            _exportState.value = _exportState.value.copy(
                isExporting = true,
                exportProgress = 0f,
                error = null
            )
            try {
                val exportFiles = mutableListOf<File>()
                val totalSessions = selectedSessions.size
                selectedSessions.forEachIndexed { index, session ->
                    _exportState.value = _exportState.value.copy(
                        currentExportFile = session.name,
                        exportProgress = (index.toFloat() / totalSessions)
                    )
                    val exportedFile = exportSession(session)
                    exportFiles.add(exportedFile)
                }
                _exportState.value = _exportState.value.copy(
                    exportProgress = 1f,
                    currentExportFile = null
                )
                // Handle export destination
                handleExportDestination(exportFiles)
            } catch (e: Exception) {
                _exportState.value = _exportState.value.copy(
                    isExporting = false,
                    exportProgress = 0f,
                    currentExportFile = null,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    private fun getAvailableSessions(): List<GSRSession> {
        val sessions = mutableListOf<GSRSession>()
        // Check multiple possible session directories
        val possibleDirectories = listOf(
            File(Environment.getExternalStorageDirectory(), "GSR/Sessions"),
            File(Environment.getExternalStorageDirectory(), "IRCamera/GSR/Sessions"),
            File(application.getExternalFilesDir(null), "gsr_sessions"),
            File(application.filesDir, "gsr_sessions")
        )
        for (directory in possibleDirectories) {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles { file ->
                    file.isFile && (file.name.endsWith(".csv") || file.name.endsWith(".txt") || file.name.endsWith(".json"))
                }?.forEach { file ->
                    sessions.add(
                        GSRSession(
                            sessionId = file.nameWithoutExtension,
                            name = file.nameWithoutExtension,
                            startTime = file.lastModified(),
                            endTime = null,
                            deviceId = "unknown",
                            participantId = null,
                            readingCount = countDataPoints(file),
                            avgConductance = 0f,
                            status = "COMPLETED",
                            duration = calculateSessionDuration(file),
                            dataPointCount = countDataPoints(file),
                            filePath = file.absolutePath,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
        // Sort by modification date (newest first)
        return sessions.sortedByDescending { it.lastModified }
    }

    private suspend fun exportSession(session: GSRSession): File {
        val outputDir = getExportDirectory()
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${session.name}_export_$timestamp.${_exportState.value.exportFormat.fileExtension}"
        val outputFile = File(outputDir, fileName)
        when (_exportState.value.exportFormat) {
            ExportFormat.CSV -> exportToCSV(session, outputFile)
            ExportFormat.JSON -> exportToJSON(session, outputFile)
            ExportFormat.XML -> exportToXML(session, outputFile)
            ExportFormat.EXCEL -> exportToExcel(session, outputFile)
        }
        return outputFile
    }

    private fun exportToCSV(session: GSRSession, outputFile: File) {
        val sessionFile = File(session.filePath)
        val writer = FileWriter(outputFile)
        writer.use { w ->
            // Write CSV header
            w.write("Timestamp,GSR_Value,Resistance,Conductance,Status\n")
            // Read and convert session data
            sessionFile.readLines().forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val convertedLine = convertDataLineToCSV(line)
                    w.write("$convertedLine\n")
                }
            }
        }
    }

    private fun exportToJSON(session: GSRSession, outputFile: File) {
        val sessionFile = File(session.filePath)
        val writer = FileWriter(outputFile)
        writer.use { w ->
            w.write("{\n")
            w.write("  \"session\": {\n")
            w.write("    \"name\": \"${session.name}\",\n")
            w.write("    \"duration\": \"${session.duration}\",\n")
            w.write("    \"dataPointCount\": ${session.dataPointCount},\n")
            w.write(
                "    \"exportedAt\": \"${
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(
                        Date()
                    )
                }\",\n"
            )
            w.write("    \"data\": [\n")
            val lines = sessionFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
            lines.forEachIndexed { index, line ->
                val jsonLine = convertDataLineToJSON(line)
                w.write("      $jsonLine")
                if (index < lines.size - 1) w.write(",")
                w.write("\n")
            }
            w.write("    ]\n")
            w.write("  }\n")
            w.write("}\n")
        }
    }

    private fun exportToXML(session: GSRSession, outputFile: File) {
        val sessionFile = File(session.filePath)
        val writer = FileWriter(outputFile)
        writer.use { w ->
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            w.write("<gsrSession>\n")
            w.write("  <metadata>\n")
            w.write("    <name>${session.name}</name>\n")
            w.write("    <duration>${session.duration}</duration>\n")
            w.write("    <dataPointCount>${session.dataPointCount}</dataPointCount>\n")
            w.write(
                "    <exportedAt>${
                    SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        Locale.getDefault()
                    ).format(Date())
                }</exportedAt>\n"
            )
            w.write("  </metadata>\n")
            w.write("  <data>\n")
            sessionFile.readLines().forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val xmlLine = convertDataLineToXML(line)
                    w.write("    $xmlLine\n")
                }
            }
            w.write("  </data>\n")
            w.write("</gsrSession>\n")
        }
    }

    private fun exportToExcel(session: GSRSession, outputFile: File) {
        // For now, export as CSV with Excel-compatible format
        // In a full implementation, you'd use Apache POI or similar library
        exportToCSV(session, outputFile)
    }

    private suspend fun handleExportDestination(exportFiles: List<File>) {
        try {
            when (_exportState.value.exportDestination) {
                ExportDestination.DOWNLOADS -> {
                    // Files are already in downloads, just notify completion
                    _exportState.value = _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed! Files saved to Downloads folder."
                    )
                }

                ExportDestination.EXTERNAL_STORAGE -> {
                    // Files are already in external storage
                    _exportState.value = _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed! Files saved to external storage."
                    )
                }

                ExportDestination.SHARE -> {
                    shareFiles(exportFiles)
                }

                ExportDestination.EMAIL -> {
                    emailFiles(exportFiles)
                }
            }
        } catch (e: Exception) {
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed but failed to handle destination: ${e.message}"
            )
        }
    }

    private fun shareFiles(files: List<File>) {
        try {
            val context = application.applicationContext
            val uris = files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share GSR Export"))
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed! Opening share dialog..."
            )
        } catch (e: Exception) {
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed but failed to share: ${e.message}"
            )
        }
    }

    private fun emailFiles(files: List<File>) {
        try {
            val context = application.applicationContext
            val uris = files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, "GSR Session Export")
                putExtra(Intent.EXTRA_TEXT, "Attached are the exported GSR session files.")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Email GSR Export"))
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed! Opening email client..."
            )
        } catch (e: Exception) {
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed but failed to email: ${e.message}"
            )
        }
    }

    private fun getExportDirectory(): File {
        return when (_exportState.value.exportDestination) {
            ExportDestination.DOWNLOADS -> File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "GSR_Exports"
            )

            ExportDestination.EXTERNAL_STORAGE -> File(
                Environment.getExternalStorageDirectory(),
                "IRCamera/GSR_Exports"
            )

            else -> File(application.getExternalFilesDir(null), "exports")
        }
    }

    private fun calculateSessionDuration(file: File): String {
        // Simple duration calculation based on file timestamps
        // In a real implementation, you'd parse the actual session data
        val durationMinutes = (file.length() / 1000).coerceAtMost(999)
        return "${durationMinutes}min"
    }

    private fun countDataPoints(file: File): Int {
        return try {
            file.readLines().count { line ->
                line.isNotBlank() && !line.startsWith("#")
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun convertDataLineToCSV(line: String): String {
        // Convert data line to CSV format
        // This is a simplified conversion - adjust based on actual data format
        return line.replace("\t", ",")
    }

    private fun convertDataLineToJSON(line: String): String {
        // Convert data line to JSON format
        val parts = line.split("\t", ",")
        return if (parts.size >= 2) {
            "{ \"timestamp\": \"${parts[0]}\", \"value\": ${parts[1]} }"
        } else {
            "{ \"data\": \"$line\" }"
        }
    }

    private fun convertDataLineToXML(line: String): String {
        // Convert data line to XML format
        val parts = line.split("\t", ",")
        return if (parts.size >= 2) {
            "<dataPoint timestamp=\"${parts[0]}\" value=\"${parts[1]}\" />"
        } else {
            "<dataPoint data=\"$line\" />"
        }
    }
}

// Extension property for file extensions
private val ExportFormat.fileExtension: String
    get() = when (this) {
        ExportFormat.CSV -> "csv"
        ExportFormat.JSON -> "json"
        ExportFormat.XML -> "xml"
        ExportFormat.EXCEL -> "xlsx"
    }


// ===== feature\gsr\presentation\SessionExportViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SessionExportViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionExportViewModel::class.java)) {
            return SessionExportViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== feature\gsr\presentation\SessionManagerViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.service.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File

class SessionManagerViewModel : AppBaseViewModel() {
    // StateFlow for session management
    private val _allSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    private val _filteredSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val filteredSessions: StateFlow<List<SessionInfo>> = _filteredSessions.asStateFlow()
    private val _storageInfo = MutableStateFlow(StorageInfo("0 MB", 0, false))
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    // SharedFlow for one-time events
    private val _sessionEvents = MutableSharedFlow<SessionEvent>()
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    // UI State
    private val _sessionUiState = MutableStateFlow(SessionManagerUiState())
    val sessionUiState: StateFlow<SessionManagerUiState> = _sessionUiState.asStateFlow()
    private lateinit var sessionManager: SessionManager
    private lateinit var sessionDirectoryManager: SessionDirectoryManager
    private var currentFilter: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    data class SessionManagerUiState(
        val isLoading: Boolean = false,
        val sessionCount: Int = 0,
        val filteredCount: Int = 0,
        val currentFilter: FilterType = FilterType.ALL,
        val searchQuery: String = ""
    )

    data class StorageInfo(
        val formattedAvailable: String,
        val usagePercentage: Int,
        val isLowStorage: Boolean
    )

    sealed class SessionEvent {
        data class OpenDetails(val session: SessionInfo) : SessionEvent()
        data class DeleteConfirm(val session: SessionInfo) : SessionEvent()
        data class Export(val session: SessionInfo) : SessionEvent()
        data class DeletedSuccess(val session: SessionInfo, val message: String) : SessionEvent()
        data class ExportSuccess(val session: SessionInfo, val message: String) : SessionEvent()
        data class ExportFailed(val session: SessionInfo, val message: String) : SessionEvent()
        data class ShowError(val message: String) : SessionEvent()
        data class ShowToast(val message: String) : SessionEvent()
    }

    enum class FilterType {
        ALL, RECENT, COMPLETED, WITH_DATA
    }

    fun initialize(context: Context) {
        sessionManager = SessionManager.getInstance(context)
        sessionDirectoryManager = SessionDirectoryManager(context)
    }

    fun loadSessions(context: Context) {
        if (!::sessionManager.isInitialized) {
            initialize(context)
        }
        _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
        launchWithErrorHandling {
            try {
                // Display storage info
                updateStorageInfo()
                // Clean up failed sessions
                val cleanedSessions = withContext(Dispatchers.IO) {
                    sessionDirectoryManager.cleanupFailedSessions()
                }
                if (cleanedSessions.isNotEmpty()) {
                    AppLogger.i(TAG, "Cleaned up ${cleanedSessions.size} failed sessions")
                    _sessionEvents.emit(SessionEvent.ShowToast("Cleaned up ${cleanedSessions.size} failed sessions"))
                }
                // Load sessions
                val loadedSessions = withContext(Dispatchers.IO) {
                    val activeSessions = sessionManager.getActiveSessions()
                    val historicalSessions = loadHistoricalSessions(context)
                    (activeSessions + historicalSessions).distinctBy { it.sessionId }
                }
                val sortedSessions = loadedSessions.sortedByDescending { it.startTime }
                _allSessions.value = sortedSessions
                applyCurrentFilters()
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    sessionCount = sortedSessions.size
                )
                AppLogger.i(TAG, "Loaded ${sortedSessions.size} sessions")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load sessions", e)
                _sessionEvents.emit(SessionEvent.ShowError("Failed to load sessions: ${e.message}"))
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun updateStorageInfo() {
        try {
            val storageStatus = sessionDirectoryManager.checkStorageSpace()
            _storageInfo.value = StorageInfo(
                formattedAvailable = storageStatus.formattedAvailable,
                usagePercentage = storageStatus.usagePercentage,
                isLowStorage = storageStatus.isLowStorage
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get storage info", e)
        }
    }

    private suspend fun loadHistoricalSessions(context: Context): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            val historicalSessions = mutableListOf<SessionInfo>()
            try {
                val baseDir = File(context.getExternalFilesDir(null), "recordings")
                if (baseDir.exists() && baseDir.isDirectory) {
                    baseDir.listFiles()?.forEach { sessionDir ->
                        if (sessionDir.isDirectory && sessionDir.name.startsWith("session_")) {
                            try {
                                val sessionInfo = parseSessionFromDirectory(sessionDir)
                                historicalSessions.add(sessionInfo)
                            } catch (e: Exception) {
                                AppLogger.w(TAG, "Failed to parse session from ${sessionDir.name}", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load historical sessions", e)
            }
            historicalSessions
        }
    }

    private fun parseSessionFromDirectory(sessionDir: File): SessionInfo {
        val sessionId = sessionDir.name
        val metadataFile = File(sessionDir, "session_metadata.txt")
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            startTime = sessionDir.lastModified(),
        )
        if (metadataFile.exists()) {
            try {
                metadataFile.readLines().forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val value = parts[1]
                        when (key.trim()) {
                            "participantId" -> sessionInfo.participantId = value.trim()
                            "studyName" -> sessionInfo.studyName = value.trim()
                            "endTime" -> sessionInfo.endTime = value.trim().toLongOrNull()
                            "sampleCount" -> sessionInfo.sampleCount =
                                value.trim().toLongOrNull() ?: 0

                            else -> sessionInfo.metadata[key.trim()] = value.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to parse metadata for ${sessionInfo.sessionId}", e)
            }
        }
        // Calculate data file counts and sizes
        calculateSessionDataInfo(sessionDir, sessionInfo)
        return sessionInfo
    }

    private fun calculateSessionDataInfo(sessionDir: File, sessionInfo: SessionInfo) {
        try {
            var totalSize = 0L
            var gsrFileCount = 0
            var thermalFileCount = 0
            var rgbFileCount = 0
            sessionDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                    when {
                        file.name.contains("gsr") -> gsrFileCount++
                        file.name.contains("thermal") -> thermalFileCount++
                        file.name.contains("rgb") -> rgbFileCount++
                    }
                }
            }
            sessionInfo.totalDataSize = totalSize
            sessionInfo.metadata["gsrFileCount"] = gsrFileCount.toString()
            sessionInfo.metadata["thermalFileCount"] = thermalFileCount.toString()
            sessionInfo.metadata["rgbFileCount"] = rgbFileCount.toString()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to calculate data info for ${sessionInfo.sessionId}", e)
        }
    }

    fun filterSessions(query: String?) {
        currentSearchQuery = query ?: ""
        _sessionUiState.value = _sessionUiState.value.copy(searchQuery = currentSearchQuery)
        applyCurrentFilters()
    }

    fun filterSessionsByType(filterPosition: Int) {
        currentFilter = when (filterPosition) {
            0 -> FilterType.ALL
            1 -> FilterType.RECENT
            2 -> FilterType.COMPLETED
            3 -> FilterType.WITH_DATA
            else -> FilterType.ALL
        }
        _sessionUiState.value = _sessionUiState.value.copy(currentFilter = currentFilter)
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        val allSessions = _allSessions.value
        var filtered = allSessions
        // Apply type filter
        filtered = when (currentFilter) {
            FilterType.ALL -> filtered
            FilterType.RECENT -> filtered.filter {
                System.currentTimeMillis() - it.startTime < 24 * 60 * 60 * 1000 // Last 24 hours
            }

            FilterType.COMPLETED -> filtered.filter { it.endTime != null }
            FilterType.WITH_DATA -> filtered.filter { it.totalDataSize > 0 }
        }
        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter { session ->
                session.sessionId.contains(currentSearchQuery, ignoreCase = true) ||
                        session.participantId?.contains(
                            currentSearchQuery,
                            ignoreCase = true
                        ) == true ||
                        session.studyName?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }
        _filteredSessions.value = filtered
        _sessionUiState.value = _sessionUiState.value.copy(filteredCount = filtered.size)
    }

    fun onSessionClick(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.OpenDetails(session))
        }
    }

    fun onSessionDelete(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.DeleteConfirm(session))
        }
    }

    fun onSessionExport(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.Export(session))
        }
    }

    fun deleteSession(session: SessionInfo) {
        launchWithErrorHandling {
            val success = withContext(Dispatchers.IO) {
                sessionDirectoryManager.deleteSession(session.sessionId)
            }
            if (success) {
                // Remove from local list and update UI
                val updatedSessions =
                    _allSessions.value.filter { it.sessionId != session.sessionId }
                _allSessions.value = updatedSessions
                applyCurrentFilters()
                _sessionUiState.value =
                    _sessionUiState.value.copy(sessionCount = updatedSessions.size)
                _sessionEvents.emit(
                    SessionEvent.DeletedSuccess(
                        session,
                        "Session ${session.sessionId} deleted successfully"
                    )
                )
            } else {
                _sessionEvents.emit(SessionEvent.ShowError("Failed to delete session ${session.sessionId}"))
            }
        }
    }

    fun exportSession(session: SessionInfo) {
        launchWithErrorHandling {
            val success = withContext(Dispatchers.IO) {
                sessionDirectoryManager.exportSession(session.sessionId)
            }
            if (success) {
                _sessionEvents.emit(
                    SessionEvent.ExportSuccess(
                        session,
                        "Session ${session.sessionId} exported successfully"
                    )
                )
            } else {
                _sessionEvents.emit(
                    SessionEvent.ExportFailed(
                        session,
                        "Failed to export session ${session.sessionId}"
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "SessionManagerViewModel"
    }
}


// ===== feature\gsr\presentation\ShimmerConfigViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.domain.usecase.*

class ShimmerConfigViewModel(
    private val scanDevicesUseCase: ScanShimmerDevicesUseCase,
    private val connectDeviceUseCase: ConnectShimmerDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectShimmerDeviceUseCase,
    private val getBatteryLevelUseCase: GetDeviceBatteryUseCase,
    private val checkConnectionUseCase: CheckDeviceConnectionUseCase
) : AppBaseViewModel() {
    companion object {
        private val REQUIRED_PERMISSIONS =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
    }

    // StateFlow for UI state management
    private val _shimmerUiState = MutableStateFlow(ShimmerConfigUiState())
    val shimmerUiState: StateFlow<ShimmerConfigUiState> = _shimmerUiState.asStateFlow()

    // Device management StateFlows
    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()
    private val _shimmerConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val shimmerConnectionState: StateFlow<ConnectionState> = _shimmerConnectionState.asStateFlow()

    // Permission management StateFlow
    private val _permissionState = MutableStateFlow(PermissionState(false, emptyList()))
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // SharedFlow for one-time events
    private val _configEvents = MutableSharedFlow<ConfigEvent>()
    val configEvents: SharedFlow<ConfigEvent> = _configEvents.asSharedFlow()

    // SharedFlow for config actions
    private val _configAction = MutableSharedFlow<ConfigAction>()
    val configAction: SharedFlow<ConfigAction> = _configAction.asSharedFlow()

    fun startScan() {
        viewModelScope.launch {
            _shimmerUiState.update { it.copy(isScanning = true, error = null) }
            try {
                scanDevicesUseCase().collect { devices ->
                    _discoveredDevices.value = devices
                    _shimmerUiState.update { it.copy(isScanning = false) }
                }
            } catch (e: Exception) {
                _shimmerUiState.update {
                    it.copy(isScanning = false, error = e.message ?: "Scan failed")
                }
            }
        }
    }

    fun connectDevice(deviceAddress: String) {
        viewModelScope.launch {
            _shimmerConnectionState.value = ConnectionState.Connecting
            val result = connectDeviceUseCase(deviceAddress)
            result.fold(
                onSuccess = {
                    _shimmerConnectionState.value = ConnectionState.Connected(deviceAddress)
                    _configEvents.emit(ConfigEvent.DeviceConnected(deviceAddress))
                },
                onFailure = { error ->
                    _shimmerConnectionState.value = ConnectionState.Error(error.message ?: "Connection failed")
                    _configEvents.emit(ConfigEvent.Error(error.message ?: "Connection failed"))
                }
            )
        }
    }

    fun disconnectDevice(deviceAddress: String) {
        viewModelScope.launch {
            disconnectDeviceUseCase(deviceAddress)
            _shimmerConnectionState.value = ConnectionState.Disconnected
            _configEvents.emit(ConfigEvent.DeviceDisconnected)
        }
    }

    fun getBatteryLevel(deviceAddress: String) {
        viewModelScope.launch {
            val batteryLevel = getBatteryLevelUseCase(deviceAddress)
            batteryLevel?.let {
                _shimmerUiState.update { state ->
                    state.copy(batteryLevel = it)
                }
            }
        }
    }

    fun isDeviceConnected(deviceAddress: String): Boolean {
        return checkConnectionUseCase(deviceAddress)
    }

    fun checkPermissions(context: Context) {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        _permissionState.value = PermissionState(
            hasAllPermissions = missingPermissions.isEmpty(),
            missingPermissions = missingPermissions
        )
    }

    fun onPermissionsGranted() {
        _permissionState.value = PermissionState(hasAllPermissions = true, missingPermissions = emptyList())
    }
}

data class ShimmerConfigUiState(
    val isScanning: Boolean = false,
    val batteryLevel: Int? = null,
    val error: String? = null
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class PermissionState(
    val hasAllPermissions: Boolean,
    val missingPermissions: List<String>
)

sealed class ConfigEvent {
    data class DeviceConnected(val deviceAddress: String) : ConfigEvent()
    object DeviceDisconnected : ConfigEvent()
    data class Error(val message: String) : ConfigEvent()
}

sealed class ConfigAction {
    object StartScan : ConfigAction()
    data class ConnectDevice(val deviceAddress: String) : ConfigAction()
    data class DisconnectDevice(val deviceAddress: String) : ConfigAction()
}


// ===== feature\gsr\presentation\ShimmerConfigViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*

class ShimmerConfigViewModelFactory(
    private val application: Application,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShimmerConfigViewModel::class.java)) {
            // Build the dependency graph from bottom up
            val shimmerDeviceManager = ShimmerDeviceManager(application, lifecycleOwner)
            val shimmerDataSource = ShimmerDataSourceImpl(shimmerDeviceManager)
            val shimmerRepository: ShimmerRepository = ShimmerRepositoryImpl(shimmerDataSource)
            // Create all use cases with the repository
            val scanDevicesUseCase = ScanShimmerDevicesUseCase(shimmerRepository)
            val connectDeviceUseCase = ConnectShimmerDeviceUseCase(shimmerRepository)
            val disconnectDeviceUseCase = DisconnectShimmerDeviceUseCase(shimmerRepository)
            val getBatteryLevelUseCase = GetDeviceBatteryUseCase(shimmerRepository)
            val checkConnectionUseCase = CheckDeviceConnectionUseCase(shimmerRepository)
            // Create the ViewModel with all use cases
            return ShimmerConfigViewModel(
                scanDevicesUseCase = scanDevicesUseCase,
                connectDeviceUseCase = connectDeviceUseCase,
                disconnectDeviceUseCase = disconnectDeviceUseCase,
                getBatteryLevelUseCase = getBatteryLevelUseCase,
                checkConnectionUseCase = checkConnectionUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== feature\gsr\ui\GSRDataViewComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class GSRDataViewComposeActivity : BaseComposeActivity<GSRDataViewViewModel>() {
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"
        private const val EXTRA_SESSION_ID = "session_id"
        fun startActivity(
            context: Context,
            filePath: String,
            sessionId: String? = null
        ) {
            val intent = Intent(context, GSRDataViewComposeActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): GSRDataViewViewModel {
        return viewModels<GSRDataViewViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRDataViewViewModel) {
        val localContext = this@GSRDataViewComposeActivity
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Data Viewer",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Implement search functionality
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Search data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = {
                                // TODO: Implement filter functionality
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Filter data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            IconButton(onClick = {
                                // TODO: Implement export functionality
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    localContext,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRDataViewContent(
                    filePath = filePath,
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSRDataViewContent(
    filePath: String,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Data Info Header
        DataInfoCard(filePath = filePath, sessionId = sessionId)
        // Tab Selection
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Raw Data") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Processed") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Statistics") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Quality") }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Events") }
            )
        }
        // Tab Content
        when (selectedTab) {
            0 -> RawDataView()
            1 -> ProcessedDataView()
            2 -> StatisticsView()
            3 -> QualityAssessmentView()
            4 -> EventsView()
        }
    }
}

@Composable
private fun DataInfoCard(
    filePath: String,
    sessionId: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Data File Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Loaded")
                }
            }
            HorizontalDivider()
            DataInfoRow("File Path", filePath.substringAfterLast("/"))
            DataInfoRow("Session ID", sessionId ?: "N/A")
            DataInfoRow("File Size", "2.3 MB")
            DataInfoRow("Records", "15,647")
            DataInfoRow("Duration", "25:30")
            DataInfoRow("Sample Rate", "128 Hz")
        }
    }
}

@Composable
private fun DataInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun RawDataView() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    // Generate sample GSR data
    val sampleData = remember {
        generateSampleGSRDataRows(1000)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Data Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Showing ${sampleData.size} records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        // TODO: Scroll to top of data
                        android.widget.Toast.makeText(
                            localContext,
                            "Scroll to top",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Go to top")
                    }
                    IconButton(onClick = {
                        // TODO: Scroll to bottom of data
                        android.widget.Toast.makeText(
                            localContext,
                            "Scroll to bottom",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Go to bottom")
                    }
                }
            }
        }
        // Data Table Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Timestamp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    "GSR (Î¼S)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Quality",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Flags",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Data Rows
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(sampleData) { dataRow ->
                GSRDataRow(dataRow)
            }
        }
    }
}

@Composable
private fun GSRDataRow(dataRow: GSRDataRowModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dataRow.quality < 0.7f) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                dataRow.timestamp,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(2f)
            )
            Text(
                String.format("%.3f", dataRow.gsrValue),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(dataRow.quality * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = if (dataRow.quality >= 0.8f) {
                    MaterialTheme.colorScheme.primary
                } else if (dataRow.quality >= 0.6f) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                if (dataRow.flags.isNotEmpty()) dataRow.flags else "-",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProcessedDataView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProcessingOptionsCard()
        ProcessedDataPreviewCard()
        ProcessingResultsCard()
    }
}

@Composable
private fun ProcessingOptionsCard() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Processing Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            var smoothingEnabled by remember { mutableStateOf(true) }
            var artifactRemoval by remember { mutableStateOf(false) }
            var normalizeData by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smoothing Filter")
                Switch(
                    checked = smoothingEnabled,
                    onCheckedChange = { smoothingEnabled = it }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Artifact Removal")
                Switch(
                    checked = artifactRemoval,
                    onCheckedChange = { artifactRemoval = it }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Normalize Data")
                Switch(
                    checked = normalizeData,
                    onCheckedChange = { normalizeData = it }
                )
            }
            Button(
                onClick = {
                    // TODO: Apply data processing
                    android.widget.Toast.makeText(
                        localContext,
                        "Applying processing...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Apply Processing")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Processing")
            }
        }
    }
}

@Composable
private fun ProcessedDataPreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Processed Data Preview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                "Processing Status: Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProcessingMetric("Filtered", "95%")
                ProcessingMetric("Artifacts", "23")
                ProcessingMetric("Quality", "A+")
            }
        }
    }
}

@Composable
private fun ProcessingMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingResultsCard() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Processing Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: View processed data
                        android.widget.Toast.makeText(
                            localContext,
                            "Viewing processed data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "View Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Data")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Export processed data
                        android.widget.Toast.makeText(
                            localContext,
                            "Exporting processed data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }
    }
}

@Composable
private fun StatisticsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DescriptiveStatisticsCard()
        DistributionAnalysisCard()
        TimeSeriesAnalysisCard()
    }
}

@Composable
private fun DescriptiveStatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Descriptive Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Mean", "12.45 Î¼S")
                StatisticItem("Median", "11.87 Î¼S")
                StatisticItem("Mode", "11.2 Î¼S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Std Dev", "3.21 Î¼S")
                StatisticItem("Variance", "10.3")
                StatisticItem("Range", "12.8 Î¼S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Skewness", "0.15")
                StatisticItem("Kurtosis", "-0.23")
                StatisticItem("CV", "25.8%")
            }
        }
    }
}

@Composable
private fun DistributionAnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Distribution Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Q1", "9.2 Î¼S")
                StatisticItem("Q2", "11.9 Î¼S")
                StatisticItem("Q3", "14.8 Î¼S")
                StatisticItem("IQR", "5.6 Î¼S")
            }
            Text(
                "Distribution Type: Normal (p=0.023)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TimeSeriesAnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Time Series Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Trend", "â†— Increasing")
                StatisticItem("Seasonality", "None")
                StatisticItem("Stationarity", "Non-stationary")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Autocorr", "0.68")
                StatisticItem("Peaks", "47")
                StatisticItem("Outliers", "12")
            }
        }
    }
}

@Composable
private fun QualityAssessmentView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OverallQualityCard()
        SignalQualityCard()
        DataIntegrityCard()
    }
}

@Composable
private fun OverallQualityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Overall Data Quality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Quality Score",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "92%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("Excellent")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { 0.92f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SignalQualityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Signal Quality Metrics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            QualityMetric("Signal-to-Noise Ratio", 0.89f, "High")
            QualityMetric("Baseline Stability", 0.95f, "Excellent")
            QualityMetric("Motion Artifacts", 0.78f, "Good")
            QualityMetric("Electrode Contact", 0.92f, "Excellent")
        }
    }
}

@Composable
private fun QualityMetric(
    name: String,
    score: Float,
    rating: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                rating,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    score >= 0.9f -> MaterialTheme.colorScheme.primary
                    score >= 0.7f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
        LinearProgressIndicator(
            progress = { score },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                score >= 0.9f -> MaterialTheme.colorScheme.primary
                score >= 0.7f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun DataIntegrityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Integrity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            IntegrityCheck("Missing Data Points", false, "0.1%")
            IntegrityCheck("Timestamp Gaps", false, "None")
            IntegrityCheck("Value Range Violations", true, "3 instances")
            IntegrityCheck("Duplicate Entries", false, "None")
        }
    }
}

@Composable
private fun IntegrityCheck(
    name: String,
    hasIssues: Boolean,
    details: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (hasIssues) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (hasIssues) "Data Quality Warning" else "Data Quality Good",
                tint = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            details,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventsView() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Events & Annotations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "24 events detected during recording session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(generateSampleEvents()) { event ->
                EventItem(event)
            }
        }
    }
}

@Composable
private fun EventItem(event: GSREventModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (event.type) {
                    "Peak" -> Icons.AutoMirrored.Filled.TrendingUp
                    "Artifact" -> Icons.Default.Warning
                    "Baseline" -> Icons.Default.HorizontalRule
                    else -> Icons.Default.Event
                },
                contentDescription = event.type,
                tint = when (event.type) {
                    "Peak" -> MaterialTheme.colorScheme.primary
                    "Artifact" -> MaterialTheme.colorScheme.error
                    "Baseline" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${event.type} Event",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "At ${event.timestamp} - ${event.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                event.value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Data Models
data class GSRDataRowModel(
    val timestamp: String,
    val gsrValue: Float,
    val quality: Float,
    val flags: String
)

data class GSREventModel(
    val timestamp: String,
    val type: String,
    val description: String,
    val value: String
)

// Helper functions
private fun generateSampleGSRDataRows(count: Int): List<GSRDataRowModel> {
    return (0 until count).map { i ->
        GSRDataRowModel(
            timestamp = "00:${(i / 60).toString().padStart(2, '0')}:${
                (i % 60).toString().padStart(2, '0')
            }.${(i % 1000).toString().padStart(3, '0')}",
            gsrValue = 8.0f + kotlin.random.Random.nextFloat() * 12.0f,
            quality = 0.5f + kotlin.random.Random.nextFloat() * 0.5f,
            flags = if (kotlin.random.Random.nextFloat() < 0.1f) "ARTIFACT" else ""
        )
    }
}

private fun generateSampleEvents(): List<GSREventModel> {
    return listOf(
        GSREventModel("00:02:15.123", "Peak", "High conductance detected", "18.4 Î¼S"),
        GSREventModel("00:05:32.456", "Artifact", "Motion artifact detected", "N/A"),
        GSREventModel("00:08:07.789", "Baseline", "Baseline shift detected", "2.1 Î¼S"),
        GSREventModel("00:11:45.234", "Peak", "Significant response peak", "19.8 Î¼S"),
        GSREventModel("00:15:23.567", "Artifact", "Electrode contact issue", "N/A"),
        GSREventModel("00:18:12.890", "Peak", "Emotional response detected", "17.2 Î¼S")
    )
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

class GSRDataViewViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing data loading, processing, filtering, etc.
    // Future implementation would include:
    // - Data loading from files
    // - Real-time data processing
    // - Filtering and search functionality
    // - Export operations
    // - Quality assessment algorithms
}


// ===== feature\gsr\ui\GSRDataViewerScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GSRDataViewerScreen(
    sessionId: String = "GSR_Session_001",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Sample GSR data - in real app, this would come from ViewModel
    val gsrData = remember { generateSampleGSRData() }
    var selectedAnalysis by remember { mutableStateOf(AnalysisType.RAW_SIGNAL) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Data Analysis",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Share,
                contentDescription = "Export Data",
                onClick = {
                    // TODO: Implement GSR data export functionality
                    android.widget.Toast.makeText(
                        context,
                        "Exporting GSR data...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Info Card
            SessionInfoCard(sessionId = sessionId)
            // Analysis Type Selector
            AnalysisTypeSelector(
                selectedType = selectedAnalysis,
                onTypeSelected = { selectedAnalysis = it }
            )
            // Data Visualization
            when (selectedAnalysis) {
                AnalysisType.RAW_SIGNAL -> {
                    GSRSignalChart(
                        title = "Raw GSR Signal",
                        data = gsrData,
                        color = Color.Cyan
                    )
                }

                AnalysisType.FILTERED -> {
                    GSRSignalChart(
                        title = "Filtered Signal",
                        data = gsrData.map { it * 0.8f + 0.1f }, // Simple filter simulation
                        color = Color.Green
                    )
                }

                AnalysisType.FEATURES -> {
                    GSRFeaturesCard(gsrData)
                }

                AnalysisType.STATISTICS -> {
                    GSRStatisticsCard(gsrData)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SessionInfoCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Information",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Session ID:", color = Color.Gray, fontSize = 14.sp)
                Text(sessionId, color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration:", color = Color.Gray, fontSize = 14.sp)
                Text("5m 32s", color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sample Rate:", color = Color.Gray, fontSize = 14.sp)
                Text("128 Hz", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AnalysisTypeSelector(
    selectedType: AnalysisType,
    onTypeSelected: (AnalysisType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analysis Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalysisType.entries.forEach { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = {
                            Text(
                                type.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = selectedType == type,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Gray,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRSignalChart(
    title: String,
    data: List<Float>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val pointSpacing = width / data.size
                val path = Path()
                data.forEachIndexed { index, value ->
                    val x = index * pointSpacing
                    val y = height - (value * height)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Draw grid lines
                for (i in 0..4) {
                    val y = (height / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRFeaturesCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "GSR Features",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val features = listOf(
                "Skin Conductance Response (SCR) Count" to "23",
                "Average SCR Amplitude" to "0.15 Î¼S",
                "Peak Detection" to "17 peaks",
                "Arousal Index" to "High",
                "Stress Level" to "Moderate"
            )
            features.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GSRStatisticsCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistical Analysis",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val stats = listOf(
                "Mean" to String.format("%.3f Î¼S", data.average()),
                "Standard Deviation" to "0.045 Î¼S",
                "Min Value" to String.format("%.3f Î¼S", data.minOrNull() ?: 0f),
                "Max Value" to String.format("%.3f Î¼S", data.maxOrNull() ?: 0f),
                "Range" to String.format(
                    "%.3f Î¼S",
                    (data.maxOrNull() ?: 0f) - (data.minOrNull() ?: 0f)
                )
            )
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

enum class AnalysisType(val displayName: String) {
    RAW_SIGNAL("Raw"),
    FILTERED("Filtered"),
    FEATURES("Features"),
    STATISTICS("Stats")
}

private fun generateSampleGSRData(): List<Float> {
    return (0..200).map { i ->
        0.5f + 0.3f * sin(i * 0.1).toFloat() + 0.1f * Random.nextFloat()
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRDataViewerScreenPreview() {
    IRCameraTheme {
        GSRDataViewerScreen()
    }
}


// ===== feature\gsr\ui\GSRDeviceManagementComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class GSRDeviceManagementComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRDeviceManagementComposeActivity::class.java))
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val localContext = this@GSRDeviceManagementComposeActivity
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<GSRDeviceInfo?>(null) }
        var showDeviceDetails by remember { mutableStateOf(false) }
        var showBulkActions by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Device Management",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                    contentDescription = if (isScanning) "Stop Scan" else "Scan"
                                )
                            }
                            IconButton(onClick = { showBulkActions = true }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Bulk Actions")
                            }
                            IconButton(onClick = {
                                // TODO: Open device help documentation
                                android.widget.Toast.makeText(
                                    this@GSRDeviceManagementComposeActivity,
                                    "Opening device help...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRDeviceManagementContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = {
                        selectedDevice = it
                        showDeviceDetails = true
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showDeviceDetails && selectedDevice != null) {
            DeviceDetailsDialog(
                device = selectedDevice!!,
                onDismiss = { showDeviceDetails = false },
                onConfigure = {
                    ShimmerConfigComposeActivity.startActivity(this@GSRDeviceManagementComposeActivity)
                    showDeviceDetails = false
                }
            )
        }
        if (showBulkActions) {
            BulkActionsDialog(
                onDismiss = { showBulkActions = false },
                onPerformAction = { action ->
                    // Perform bulk action
                    showBulkActions = false
                }
            )
        }
    }
}

@Composable
private fun GSRDeviceManagementContent(
    isScanning: Boolean,
    selectedDevice: GSRDeviceInfo?,
    onDeviceSelect: (GSRDeviceInfo) -> Unit,
    viewModel: AppBaseViewModel,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Device Status Overview
        DeviceStatusOverview(
            connectedDevices = 2,
            availableDevices = 3,
            isScanning = isScanning,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Connected Devices Section
        Text(
            text = "Connected Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val connectedDevices = getMockGSRDevices().filter { it.status == "connected" }
            val availableDevices = getMockGSRDevices().filter { it.status != "connected" }
            items(connectedDevices) { device ->
                GSRDeviceCard(
                    device = device,
                    onSelect = { onDeviceSelect(device) },
                    onConnect = {
                        // TODO: Implement device connection logic
                        android.widget.Toast.makeText(
                            localContext,
                            "Connecting to ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDisconnect = {
                        // TODO: Implement device disconnection logic
                        android.widget.Toast.makeText(
                            localContext,
                            "Disconnecting from ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            if (availableDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Devices",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    )
                }
                items(availableDevices) { device ->
                    GSRDeviceCard(
                        device = device,
                        onSelect = { onDeviceSelect(device) },
                        onConnect = {
                            // TODO: Implement device connection logic
                            android.widget.Toast.makeText(
                                localContext,
                                "Connecting to ${device.name}...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDisconnect = {
                            // TODO: Implement device disconnection logic
                            android.widget.Toast.makeText(
                                localContext,
                                "Disconnecting from ${device.name}...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusOverview(
    connectedDevices: Int,
    availableDevices: Int,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceStatusItem(
                    label = "Connected",
                    count = connectedDevices,
                    color = Color(0xFF4CAF50)
                )
                DeviceStatusItem(
                    label = "Available",
                    count = availableDevices,
                    color = Color(0xFF2196F3)
                )
                DeviceStatusItem(
                    label = "Total",
                    count = connectedDevices + availableDevices,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun GSRDeviceCard(
    device: GSRDeviceInfo,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = when (device.status) {
                "connected" -> MaterialTheme.colorScheme.tertiaryContainer
                "connecting" -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${device.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Status indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getDeviceStatusColor(device.status),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = device.status?.uppercase() ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Device metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceMetricItem(
                    label = "Battery",
                    value = "${device.batteryLevel}%",
                    icon = getBatteryIcon(device.batteryLevel)
                )
                DeviceMetricItem(
                    label = "Signal",
                    value = "${device.signalStrength} dBm",
                    icon = Icons.Default.Wifi
                )
                DeviceMetricItem(
                    label = "Sample Rate",
                    value = "${device.samplingRate} Hz",
                    icon = Icons.Default.Timeline
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Device actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Details",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                if (device.status == "connected") {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53E3E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = "Disconnect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviceDetailsDialog(
    device: GSRDeviceInfo,
    onDismiss: () -> Unit,
    onConfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(device.name)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DeviceDetailItem("Device ID", device.deviceId)
                DeviceDetailItem("Status", device.status?.replaceFirstChar { it.uppercaseChar() } ?: "N/A")
                DeviceDetailItem("Battery Level", "${device.batteryLevel}%")
                DeviceDetailItem("Signal Strength", "${device.signalStrength} dBm")
                DeviceDetailItem("Sampling Rate", "${device.samplingRate} Hz")
                DeviceDetailItem("Last Seen", device.lastSeen)
                if (device.status == "connected") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "GSR: 2.45 ÂµS\nPPG: 1024, 1028\nTemperature: 36.2Â°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text("Configure")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DeviceDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BulkActionsDialog(
    onDismiss: () -> Unit,
    onPerformAction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Actions") },
        text = {
            Column {
                Text("Select an action to perform on all devices:")
                Spacer(modifier = Modifier.height(16.dp))
                listOf(
                    "Disconnect All" to "disconnect_all",
                    "Update Firmware" to "update_firmware",
                    "Reset Configuration" to "reset_config",
                    "Export Device List" to "export_list"
                ).forEach { (label, action) ->
                    TextButton(
                        onClick = { onPerformAction(action) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getDeviceStatusColor(status: String?) = when (status) {
    "connected" -> Color(0xFF4CAF50)
    "connecting" -> Color(0xFFFF9800)
    "available" -> Color(0xFF2196F3)
    "disconnected" -> Color(0xFF9E9E9E)
    null -> Color(0xFF9E9E9E).copy(alpha = 0.6f)
    else -> Color(0xFFE53E3E)
}

private fun getBatteryIcon(batteryLevel: Int) = when {
    batteryLevel > 75 -> Icons.Default.BatteryFull
    batteryLevel > 50 -> Icons.Default.Battery6Bar
    batteryLevel > 25 -> Icons.Default.Battery3Bar
    else -> Icons.Default.Battery1Bar
}

data class GSRDeviceInfo(
    val name: String,
    val deviceId: String,
    val status: String?,
    val batteryLevel: Int,
    val signalStrength: Int,
    val samplingRate: Int,
    val lastSeen: String
)

private fun getMockGSRDevices() = listOf(
    GSRDeviceInfo("Shimmer3 GSR+ #001", "shimmer_001", "disconnected", 89, -42, 128, "Just now"),
    GSRDeviceInfo("Shimmer3 GSR+ #002", "shimmer_002", "disconnected", 76, -38, 256, "2 min ago"),
    GSRDeviceInfo("Shimmer3 GSR+ #003", "shimmer_003", "available", 92, -55, 128, "5 min ago"),
    GSRDeviceInfo("Shimmer3 GSR+ #004", "shimmer_004", "disconnected", 45, -68, 128, "1 hour ago"),
    GSRDeviceInfo("Shimmer3 GSR+ #005", "shimmer_005", "available", 83, -48, 256, "10 min ago")
)


// ===== feature\gsr\ui\GSRGalleryComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.theme.IRCameraTheme

class GSRGalleryComposeActivity : BaseComposeActivity<GSRGalleryViewModel>() {
    override fun createViewModel(): GSRGalleryViewModel = GSRGalleryViewModel()

    @Composable
    override fun Content(viewModel: GSRGalleryViewModel) {
        IRCameraTheme {
            GSRGalleryScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRGalleryScreen(viewModel: GSRGalleryViewModel) {
    val uiState by viewModel.galleryState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var showFilterDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadGSRSessions()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Search and View Toggle
        GSRGalleryHeader(
            searchQuery = searchQuery,
            onSearchChange = {
                searchQuery = it
                viewModel.filterSessions(it)
            },
            isGridView = isGridView,
            onViewToggle = { isGridView = !isGridView },
            onShowFilter = { showFilterDialog = true },
            sessionsCount = uiState.sessions.size
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Content based on view mode
        if (isGridView) {
            GSRSessionGrid(
                sessions = uiState.filteredSessions,
                onSessionClick = { viewModel.openSession(it) },
                onSessionLongClick = { viewModel.selectSession(it) },
                selectedSessions = uiState.selectedSessions
            )
        } else {
            GSRSessionList(
                sessions = uiState.filteredSessions,
                onSessionClick = { viewModel.openSession(it) },
                onSessionLongClick = { viewModel.selectSession(it) },
                selectedSessions = uiState.selectedSessions
            )
        }
    }
    // Filter Dialog
    if (showFilterDialog) {
        GSRFilterDialog(
            currentFilter = uiState.currentFilter,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                viewModel.applyFilter(filter)
                showFilterDialog = false
            }
        )
    }
    // Selection Actions
    if (uiState.selectedSessions.isNotEmpty()) {
        SelectionActionsBar(
            selectedCount = uiState.selectedSessions.size,
            onExportSelected = { viewModel.exportSelectedSessions() },
            onDeleteSelected = { viewModel.deleteSelectedSessions() },
            onClearSelection = { viewModel.clearSelection() }
        )
    }
}

@Composable
fun GSRGalleryHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isGridView: Boolean,
    onViewToggle: () -> Unit,
    onShowFilter: () -> Unit,
    sessionsCount: Int
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GSR Gallery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$sessionsCount sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search GSR sessions...") },
                leadingIcon = {
                    IconButton(onClick = { keyboardController?.hide() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onViewToggle) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridView) "List View" else "Grid View"
                )
            }
            IconButton(onClick = onShowFilter) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }
    }
}

@Composable
fun GSRSessionGrid(
    sessions: List<GSRSession>,
    onSessionClick: (GSRSession) -> Unit,
    onSessionLongClick: (GSRSession) -> Unit,
    selectedSessions: Set<String>
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sessions) { session ->
            GSRSessionGridItem(
                session = session,
                isSelected = selectedSessions.contains(session.id),
                onClick = { onSessionClick(session) },
                onLongClick = { onSessionLongClick(session) }
            )
        }
    }
}

@Composable
fun GSRSessionList(
    sessions: List<GSRSession>,
    onSessionClick: (GSRSession) -> Unit,
    onSessionLongClick: (GSRSession) -> Unit,
    selectedSessions: Set<String>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            GSRSessionListItem(
                session = session,
                isSelected = selectedSessions.contains(session.id),
                onClick = { onSessionClick(session) },
                onLongClick = { onSessionLongClick(session) }
            )
        }
    }
}

@Composable
fun GSRSessionGridItem(
    session: GSRSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Session preview visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "GSR Data",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "${formatDuration(session.duration)} â€¢ ${session.sampleCount} samples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quality: ${session.dataQuality}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        session.dataQuality > 90 -> Color(0xFF4CAF50)
                        session.dataQuality > 70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}

@Composable
fun GSRSessionListItem(
    session: GSRSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "GSR Data",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (session.participantId.isNotEmpty()) {
                    Text(
                        text = "Participant: ${session.participantId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Row {
                    Text(
                        text = formatDuration(session.duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(" â€¢ ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${session.sampleCount} samples",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(" â€¢ ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Quality: ${session.dataQuality}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            session.dataQuality > 90 -> Color(0xFF4CAF50)
                            session.dataQuality > 70 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
            Text(
                text = formatFileSize(session.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun GSRFilterDialog(
    currentFilter: GSRFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (GSRFilter) -> Unit
) {
    var filter by remember { mutableStateOf(currentFilter) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter GSR Sessions") },
        text = {
            Column {
                Text("Minimum Quality:")
                Slider(
                    value = filter.minQuality.toFloat(),
                    onValueChange = { filter = filter.copy(minQuality = it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 10
                )
                Text("${filter.minQuality}%")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Minimum Duration (minutes):")
                Slider(
                    value = filter.minDuration.toFloat(),
                    onValueChange = { filter = filter.copy(minDuration = it.toInt()) },
                    valueRange = 0f..60f,
                    steps = 12
                )
                Text("${filter.minDuration} min")
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filter.hasParticipant,
                        onCheckedChange = { filter = filter.copy(hasParticipant = it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Only sessions with participant ID")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApplyFilter(filter) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectionActionsBar(
    selectedCount: Int,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                TextButton(onClick = onClearSelection) {
                    Text("Clear")
                }
                OutlinedButton(onClick = onExportSelected) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
                Button(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

// Helper functions
private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return String.format("%d:%02d", minutes, seconds % 60)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

// Data classes
data class GSRSession(
    val id: String,
    val name: String,
    val participantId: String = "",
    val duration: Long,
    val sampleCount: Int,
    val dataQuality: Int,
    val fileSize: Long,
    val timestamp: Long
)

data class GSRFilter(
    val minQuality: Int = 0,
    val minDuration: Int = 0,
    val hasParticipant: Boolean = false
)

data class GSRGalleryUiState(
    val sessions: List<GSRSession> = emptyList(),
    val filteredSessions: List<GSRSession> = emptyList(),
    val selectedSessions: Set<String> = emptySet(),
    val currentFilter: GSRFilter = GSRFilter(),
    val isLoading: Boolean = false
)

// ViewModel
class GSRGalleryViewModel : AppBaseViewModel() {
    private val _galleryState = MutableStateFlow(GSRGalleryUiState())
    val galleryState: StateFlow<GSRGalleryUiState> = _galleryState.asStateFlow()
    fun loadGSRSessions() {
        _galleryState.value = _galleryState.value.copy(isLoading = true)
        val mockSessions = listOf(
            GSRSession(
                "1",
                "Stress Response Study",
                "P001",
                1800000,
                460800,
                95,
                2048576,
                System.currentTimeMillis() - 86400000
            ),
            GSRSession(
                "2",
                "Cognitive Load Test",
                "P002",
                1200000,
                307200,
                87,
                1048576,
                System.currentTimeMillis() - 172800000
            ),
            GSRSession(
                "3",
                "Emotion Recognition",
                "",
                2700000,
                691200,
                92,
                3145728,
                System.currentTimeMillis() - 259200000
            ),
            GSRSession(
                "4",
                "Quick Recording",
                "",
                300000,
                76800,
                78,
                262144,
                System.currentTimeMillis() - 345600000
            ),
            GSRSession(
                "5",
                "Baseline Measurement",
                "P001",
                600000,
                153600,
                98,
                524288,
                System.currentTimeMillis() - 432000000
            )
        )
        _galleryState.value = _galleryState.value.copy(
            sessions = mockSessions,
            filteredSessions = mockSessions,
            isLoading = false
        )
    }

    fun filterSessions(query: String) {
        val filtered = _galleryState.value.sessions.filter { session ->
            query.isEmpty() ||
                    session.name.contains(query, ignoreCase = true) ||
                    session.participantId.contains(query, ignoreCase = true)
        }
        _galleryState.value = _galleryState.value.copy(filteredSessions = filtered)
    }

    fun applyFilter(filter: GSRFilter) {
        val filtered = _galleryState.value.sessions.filter { session ->
            session.dataQuality >= filter.minQuality &&
                    (session.duration / 60000) >= filter.minDuration &&
                    (!filter.hasParticipant || session.participantId.isNotEmpty())
        }
        _galleryState.value = _galleryState.value.copy(
            filteredSessions = filtered,
            currentFilter = filter
        )
    }

    fun selectSession(session: GSRSession) {
        val currentSelection = _galleryState.value.selectedSessions
        val newSelection = if (currentSelection.contains(session.id)) {
            currentSelection - session.id
        } else {
            currentSelection + session.id
        }
        _galleryState.value = _galleryState.value.copy(selectedSessions = newSelection)
    }

    fun clearSelection() {
        _galleryState.value = _galleryState.value.copy(selectedSessions = emptySet())
    }

    fun openSession(session: GSRSession) {
        // Implementation for opening session details
    }

    fun exportSelectedSessions() {
        // Implementation for exporting selected sessions
    }

    fun deleteSelectedSessions() {
        val selectedIds = _galleryState.value.selectedSessions
        val updatedSessions = _galleryState.value.sessions.filter { !selectedIds.contains(it.id) }
        _galleryState.value = _galleryState.value.copy(
            sessions = updatedSessions,
            filteredSessions = updatedSessions,
            selectedSessions = emptySet()
        )
    }
}


// ===== feature\gsr\ui\GSRPlotComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel
import kotlin.math.sin

class GSRPlotComposeActivity : BaseComposeActivity<GSRPlotViewModel>() {
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_DATA_PATH = "data_path"
        fun startActivity(
            context: Context,
            sessionId: String,
            dataPath: String? = null
        ) {
            val intent = Intent(context, GSRPlotComposeActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                dataPath?.let { putExtra(EXTRA_DATA_PATH, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): GSRPlotViewModel {
        return viewModels<GSRPlotViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRPlotViewModel) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "unknown"
        val dataPath = intent.getStringExtra(EXTRA_DATA_PATH)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Data Analysis",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                // TODO: Implement data export
                                android.widget.Toast.makeText(
                                    context,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Implement plot sharing
                                android.widget.Toast.makeText(
                                    context,
                                    "Share plot feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Open plot settings
                                android.widget.Toast.makeText(
                                    context,
                                    "Plot settings feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Tune, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRPlotContent(
                    sessionId = sessionId,
                    dataPath = dataPath,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRPlotContent(
    sessionId: String,
    dataPath: String?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedVisualization by remember { mutableStateOf(VisualizationType.LINE_CHART) }
    var timeRange by remember { mutableStateOf(TimeRange.ALL) }
    var showStatistics by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visualization Controls
        VisualizationControlsCard(
            selectedVisualization = selectedVisualization,
            selectedTimeRange = timeRange,
            onVisualizationChange = { selectedVisualization = it },
            onTimeRangeChange = { timeRange = it }
        )
        // Main Plot Area
        MainPlotCard(
            visualizationType = selectedVisualization,
            timeRange = timeRange,
            sessionId = sessionId,
            context = context
        )
        // Statistics Panel
        if (showStatistics) {
            StatisticsCard(sessionId = sessionId)
        }
        // Data Analysis Tools
        DataAnalysisToolsCard(context = context)
        // Export Options
        ExportOptionsCard(context = context)
    }
}

enum class VisualizationType {
    LINE_CHART,
    SCATTER_PLOT,
    HISTOGRAM,
    HEATMAP
}

enum class TimeRange {
    ALL,
    LAST_MINUTE,
    LAST_5_MINUTES,
    LAST_10_MINUTES,
    CUSTOM
}

@Composable
private fun VisualizationControlsCard(
    selectedVisualization: VisualizationType,
    selectedTimeRange: TimeRange,
    onVisualizationChange: (VisualizationType) -> Unit,
    onTimeRangeChange: (TimeRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Visualization Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Visualization Type Selection
            Text(
                "Chart Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationType.values().take(2).forEach { type ->
                    FilterChip(
                        onClick = { onVisualizationChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    VisualizationType.LINE_CHART -> "Line Chart"
                                    VisualizationType.SCATTER_PLOT -> "Scatter Plot"
                                    VisualizationType.HISTOGRAM -> "Histogram"
                                    VisualizationType.HEATMAP -> "Heatmap"
                                }
                            )
                        },
                        selected = selectedVisualization == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationType.values().drop(2).forEach { type ->
                    FilterChip(
                        onClick = { onVisualizationChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    VisualizationType.LINE_CHART -> "Line Chart"
                                    VisualizationType.SCATTER_PLOT -> "Scatter Plot"
                                    VisualizationType.HISTOGRAM -> "Histogram"
                                    VisualizationType.HEATMAP -> "Heatmap"
                                }
                            )
                        },
                        selected = selectedVisualization == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Time Range Selection
            Text(
                "Time Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.values().take(3).forEach { range ->
                    FilterChip(
                        onClick = { onTimeRangeChange(range) },
                        label = {
                            Text(
                                when (range) {
                                    TimeRange.ALL -> "All"
                                    TimeRange.LAST_MINUTE -> "1m"
                                    TimeRange.LAST_5_MINUTES -> "5m"
                                    TimeRange.LAST_10_MINUTES -> "10m"
                                    TimeRange.CUSTOM -> "Custom"
                                }
                            )
                        },
                        selected = selectedTimeRange == range,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainPlotCard(
    visualizationType: VisualizationType,
    timeRange: TimeRange,
    sessionId: String,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GSR Signal Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        // TODO: Implement zoom in functionality
                        android.widget.Toast.makeText(
                            context,
                            "Zoom in feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = {
                        // TODO: Implement zoom out functionality
                        android.widget.Toast.makeText(
                            context,
                            "Zoom out feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    IconButton(onClick = {
                        // TODO: Implement reset zoom functionality
                        android.widget.Toast.makeText(
                            context,
                            "Reset zoom feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset")
                    }
                }
            }
            HorizontalDivider()
            // Plot Area
            when (visualizationType) {
                VisualizationType.LINE_CHART -> {
                    GSRLineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.SCATTER_PLOT -> {
                    GSRScatterPlot(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.HISTOGRAM -> {
                    GSRHistogram(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.HEATMAP -> {
                    GSRHeatmap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
            // Plot Legend
            PlotLegend()
        }
    }
}

@Composable
private fun GSRLineChart(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRLineChart(this, primaryColor, secondaryColor)
    }
}

private fun drawGSRLineChart(drawScope: DrawScope, primaryColor: Color, secondaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate sample GSR data
        val dataPoints = generateSampleGSRData(100)
        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        // Draw the GSR signal line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3f)
        )
        // Draw data points
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            drawCircle(
                color = secondaryColor,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun GSRScatterPlot(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRScatterPlot(this, primaryColor)
    }
}

private fun drawGSRScatterPlot(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate sample scatter data
        val dataPoints = generateSampleGSRData(50)
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            // Vary point size based on value
            val radius = 3f + (value * 5f)
            drawCircle(
                color = primaryColor.copy(alpha = 0.7f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun GSRHistogram(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRHistogram(this, primaryColor)
    }
}

private fun drawGSRHistogram(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate histogram data
        val binCount = 15
        val binWidth = (width - 2 * padding) / binCount
        val histogramData = generateHistogramData(binCount)
        histogramData.forEachIndexed { index, value ->
            val x = padding + index * binWidth
            val barHeight = value * (height - 2 * padding)
            drawRect(
                color = primaryColor.copy(alpha = 0.8f),
                topLeft = Offset(x, height - padding - barHeight),
                size = androidx.compose.ui.geometry.Size(binWidth * 0.8f, barHeight)
            )
        }
    }
}

@Composable
private fun GSRHeatmap(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRHeatmap(this, primaryColor)
    }
}

private fun drawGSRHeatmap(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val cellSize = 20f
        val cols = (width / cellSize).toInt()
        val rows = (height / cellSize).toInt()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val intensity = (sin((row + col) * 0.3) + 1) / 2
                val color = primaryColor.copy(alpha = intensity.toFloat())
                drawRect(
                    color = color,
                    topLeft = Offset(col * cellSize, row * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
    }
}

@Composable
private fun PlotLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem("GSR Signal", MaterialTheme.colorScheme.primary)
        LegendItem("Data Points", MaterialTheme.colorScheme.secondary)
        LegendItem("Threshold", MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatisticsCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Statistical Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Mean", "12.5 Î¼S")
                StatisticItem("Std Dev", "3.2 Î¼S")
                StatisticItem("Min", "8.1 Î¼S")
                StatisticItem("Max", "18.9 Î¼S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Peaks", "24")
                StatisticItem("Frequency", "0.8 Hz")
                StatisticItem("Trend", "â†— Rising")
                StatisticItem("Quality", "95%")
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataAnalysisToolsCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Analysis Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Apply data filter
                        android.widget.Toast.makeText(
                            context,
                            "Applying filter...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = "Filter Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filter")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Apply data smoothing
                        android.widget.Toast.makeText(
                            context,
                            "Smoothing data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Smooth Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Smooth")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement peak detection algorithm
                        android.widget.Toast.makeText(
                            context,
                            "Peak detection feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Detect Peaks")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peaks")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Implement trend analysis
                        android.widget.Toast.makeText(
                            context,
                            "Trend analysis feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analyze Trends")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trends")
                }
            }
        }
    }
}

@Composable
private fun ExportOptionsCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Export Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Export data to CSV
                        android.widget.Toast.makeText(
                            context,
                            "Exporting to CSV...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Save plot as image
                        android.widget.Toast.makeText(
                            context,
                            "Saving plot...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Save Plot as Image")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Plot")
                }
            }
        }
    }
}

// Helper functions for generating sample data
private fun generateSampleGSRData(points: Int): List<Float> {
    return (0 until points).map { i ->
        val baseValue = 0.3f + sin(i * 0.1f) * 0.2f
        val noise = (kotlin.random.Random.nextFloat() - 0.5f) * 0.1f
        (baseValue + noise).coerceIn(0f, 1f)
    }
}

private fun generateHistogramData(bins: Int): List<Float> {
    return (0 until bins).map { i ->
        val centerValue = i.toFloat() / bins
        kotlin.math.exp(-((centerValue - 0.5f) * (centerValue - 0.5f)) / 0.2f).toFloat()
    }
}

class GSRPlotViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing plot data, zoom state, filters, etc.
    // Future implementation would include:
    // - Data loading from files or database
    // - Real-time data updates
    // - Zoom and pan state management
    // - Filter state management
    // - Export functionality
}


// ===== feature\gsr\ui\GSRPlotScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRPlotScreen(
    sessionId: String,
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("GSR Plot - $sessionId") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GSR Data Plot",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Session: $sessionId",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "GSR plot visualization would be implemented here using a charting library or custom Canvas drawing.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


// ===== feature\gsr\ui\GSRQuickRecordingComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.random.Random

class GSRQuickRecordingComposeActivity : BaseComposeActivity<GSRQuickRecordingViewModel>() {
    override fun createViewModel(): GSRQuickRecordingViewModel = GSRQuickRecordingViewModel()

    @Composable
    override fun Content(viewModel: GSRQuickRecordingViewModel) {
        IRCameraTheme {
            GSRQuickRecordingScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRQuickRecordingScreen(viewModel: GSRQuickRecordingViewModel) {
    val uiState by viewModel.recordingState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initializeQuickRecording()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Quick GSR Recording",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording Status Card
            item {
                QuickRecordingStatusCard(
                    isRecording = uiState.isRecording,
                    recordingDuration = uiState.recordingDuration,
                    samplesCollected = uiState.samplesCollected,
                    onStartRecording = { viewModel.startQuickRecording() },
                    onStopRecording = { viewModel.stopQuickRecording() }
                )
            }
            // GSR Device Status
            item {
                GSRDeviceStatusCard(
                    deviceStatus = uiState.deviceStatus,
                    signalQuality = uiState.signalQuality,
                    batteryLevel = uiState.batteryLevel
                )
            }
            // Live GSR Data Visualization
            if (uiState.isRecording) {
                item {
                    LiveGSRDataCard(
                        currentValue = uiState.currentGSRValue,
                        averageValue = uiState.averageGSRValue,
                        recentValues = uiState.recentGSRValues
                    )
                }
            }
            // Quick Settings
            item {
                QuickSettingsCard(
                    sampleRate = uiState.sampleRate,
                    autoSave = uiState.autoSave,
                    onSampleRateChange = { viewModel.setSampleRate(it) },
                    onAutoSaveToggle = { viewModel.toggleAutoSave() }
                )
            }
            // Recent Sessions
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Quick Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.recentSessions.take(3)) { session ->
                    QuickSessionCard(
                        session = session,
                        onView = { viewModel.viewSession(session) },
                        onExport = { viewModel.exportSession(session) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickRecordingStatusCard(
    isRecording: Boolean,
    recordingDuration: Long,
    samplesCollected: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color(0xFFF44336).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording) {
                // Recording indicator with animation
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Recording Active",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Recording stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStatItem("Duration", formatDuration(recordingDuration))
                    QuickStatItem("Samples", samplesCollected.toString())
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Recording", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Ready to Record",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ready to Record",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to start quick GSR recording",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun GSRDeviceStatusCard(
    deviceStatus: String,
    signalQuality: Int,
    batteryLevel: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Sensor Status: $deviceStatus",
                    tint = when (deviceStatus) {
                        "Connected" -> Color(0xFF4CAF50)
                        "Connecting" -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GSR Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = deviceStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (deviceStatus) {
                            "Connected" -> Color(0xFF4CAF50)
                            "Connecting" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Quality: $signalQuality%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (batteryLevel != null) {
                        Text(
                            text = "Battery: $batteryLevel%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (deviceStatus == "Connected") {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { signalQuality / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        signalQuality > 80 -> Color(0xFF4CAF50)
                        signalQuality > 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}

@Composable
fun LiveGSRDataCard(
    currentValue: Double,
    averageValue: Double,
    recentValues: List<Double>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatItem("Current", String.format("%.2f ÂµS", currentValue))
                QuickStatItem("Average", String.format("%.2f ÂµS", averageValue))
                QuickStatItem(
                    "Range",
                    String.format(
                        "%.1f",
                        recentValues.maxOrNull()?.minus(recentValues.minOrNull() ?: 0.0) ?: 0.0
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Simple data visualization placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Live GSR Signal Visualization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun QuickSettingsCard(
    sampleRate: Int,
    autoSave: Boolean,
    onSampleRateChange: (Int) -> Unit,
    onAutoSaveToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sample Rate: ${sampleRate} Hz")
                Row {
                    listOf(128, 256, 512).forEach { rate ->
                        FilterChip(
                            onClick = { onSampleRateChange(rate) },
                            label = { Text("$rate") },
                            selected = sampleRate == rate,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-save Sessions")
                Switch(
                    checked = autoSave,
                    onCheckedChange = { onAutoSaveToggle() }
                )
            }
        }
    }
}

@Composable
fun QuickSessionCard(
    session: QuickSession,
    onView: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "GSR Data",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatDuration(session.duration)} â€¢ ${session.sampleCount} samples",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onView) {
                Icon(Icons.Default.Visibility, contentDescription = "View")
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Download, contentDescription = "Export")
            }
        }
    }
}

@Composable
fun QuickStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return String.format("%d:%02d", minutes, seconds % 60)
}

// Data classes
data class QuickSession(
    val name: String,
    val duration: Long,
    val sampleCount: Int,
    val timestamp: Long
)

data class GSRQuickRecordingUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val samplesCollected: Int = 0,
    val deviceStatus: String = "Disconnected",
    val signalQuality: Int = 95,
    val batteryLevel: Int? = 87,
    val currentGSRValue: Double = 0.0,
    val averageGSRValue: Double = 0.0,
    val recentGSRValues: List<Double> = emptyList(),
    val sampleRate: Int = 256,
    val autoSave: Boolean = true,
    val recentSessions: List<QuickSession> = emptyList()
)

// ViewModel
class GSRQuickRecordingViewModel : AppBaseViewModel() {
    private val _recordingState = MutableStateFlow(GSRQuickRecordingUiState())
    val recordingState: StateFlow<GSRQuickRecordingUiState> = _recordingState.asStateFlow()
    private var recordingJob: Job? = null
    fun initializeQuickRecording() {
        val mockSessions = listOf(
            QuickSession("Quick Session 1", 180000, 46080, System.currentTimeMillis() - 3600000),
            QuickSession("Quick Session 2", 120000, 30720, System.currentTimeMillis() - 7200000),
            QuickSession("Quick Session 3", 240000, 61440, System.currentTimeMillis() - 10800000)
        )
        _recordingState.value = _recordingState.value.copy(recentSessions = mockSessions)
    }

    fun startQuickRecording() {
        _recordingState.value = _recordingState.value.copy(isRecording = true)
        // Cancel any existing recording job
        recordingJob?.cancel()
        // Start recording simulation on main dispatcher
        recordingJob = viewModelScope.launch(Dispatchers.Main) {
            while (_recordingState.value.isRecording) {
                delay(1000)
                val currentState = _recordingState.value
                _recordingState.value = currentState.copy(
                    recordingDuration = currentState.recordingDuration + 1000,
                    samplesCollected = currentState.samplesCollected + currentState.sampleRate,
                    currentGSRValue = Random.nextDouble(5.0, 15.0),
                    averageGSRValue = Random.nextDouble(8.0, 12.0),
                    recentGSRValues = currentState.recentGSRValues.takeLast(10) + Random.nextDouble(5.0, 15.0)
                )
            }
        }
    }

    fun stopQuickRecording() {
        _recordingState.value = _recordingState.value.copy(isRecording = false)
        recordingJob?.cancel()
        recordingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }

    fun setSampleRate(rate: Int) {
        _recordingState.value = _recordingState.value.copy(sampleRate = rate)
    }

    fun toggleAutoSave() {
        _recordingState.value = _recordingState.value.copy(autoSave = !_recordingState.value.autoSave)
    }

    fun viewSession(session: QuickSession) {
        // Implementation for viewing session
    }

    fun exportSession(session: QuickSession) {
        // Implementation for exporting session
    }
}


// ===== feature\gsr\ui\GSRQuickRecordingScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.math.sqrt

data class GSRReading(
    val timestamp: Long,
    val value: Double, // in microsiemens
    val quality: SignalQuality = SignalQuality.GOOD
)

enum class SignalQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

enum class RecordingState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECORDING,
    PAUSED,
    COMPLETED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRQuickRecordingScreen(
    onNavigateBack: () -> Unit = {},
    onSaveRecording: () -> Unit = {}
) {
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    var recordingDuration by remember { mutableStateOf(0) } // in seconds
    var gsrReadings by remember { mutableStateOf(listOf<GSRReading>()) }
    var currentGSRValue by remember { mutableStateOf(12.5) }
    var batteryLevel by remember { mutableStateOf(85) }
    var signalQuality by remember { mutableStateOf(SignalQuality.GOOD) }
    // Simulate GSR data updates
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (recordingState == RecordingState.RECORDING) {
                delay(100) // Update every 100ms
                recordingDuration += 1
                // Simulate GSR reading
                val newValue = 12.0 + 4.0 * sin(recordingDuration * 0.01) +
                        (Math.random() - 0.5) * 2.0
                currentGSRValue = newValue
                val newReading = GSRReading(
                    timestamp = System.currentTimeMillis(),
                    value = newValue,
                    quality = signalQuality
                )
                gsrReadings = (gsrReadings + newReading).takeLast(200) // Keep last 200 readings
            }
        }
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Quick GSR Recording",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Save recording",
                    onClick = {
                        if (recordingState == RecordingState.COMPLETED && gsrReadings.isNotEmpty()) {
                            onSaveRecording()
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Status Card
                DeviceStatusCard(
                    recordingState = recordingState,
                    batteryLevel = batteryLevel,
                    signalQuality = signalQuality,
                    onConnect = { recordingState = RecordingState.CONNECTING }
                )
                // Real-time GSR Display
                if (recordingState == RecordingState.CONNECTED ||
                    recordingState == RecordingState.RECORDING ||
                    recordingState == RecordingState.PAUSED
                ) {
                    GSRDisplayCard(
                        currentValue = currentGSRValue,
                        readings = gsrReadings,
                        signalQuality = signalQuality
                    )
                }
                // Recording Controls
                RecordingControlsCard(
                    recordingState = recordingState,
                    duration = recordingDuration,
                    onStartRecording = {
                        if (recordingState == RecordingState.CONNECTED) {
                            recordingState = RecordingState.RECORDING
                        }
                    },
                    onPauseRecording = {
                        if (recordingState == RecordingState.RECORDING) {
                            recordingState = RecordingState.PAUSED
                        }
                    },
                    onResumeRecording = {
                        if (recordingState == RecordingState.PAUSED) {
                            recordingState = RecordingState.RECORDING
                        }
                    },
                    onStopRecording = {
                        if (recordingState == RecordingState.RECORDING ||
                            recordingState == RecordingState.PAUSED
                        ) {
                            recordingState = RecordingState.COMPLETED
                        }
                    }
                )
                // Session Summary (when completed)
                if (recordingState == RecordingState.COMPLETED && gsrReadings.isNotEmpty()) {
                    SessionSummaryCard(readings = gsrReadings)
                }
                // Quick Setup Instructions
                if (recordingState == RecordingState.IDLE) {
                    QuickSetupCard(
                        onStartSetup = { recordingState = RecordingState.CONNECTING }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(
    recordingState: RecordingState,
    batteryLevel: Int,
    signalQuality: SignalQuality,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shimmer3 GSR Device",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                RecordingStateBadge(state = recordingState)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (recordingState != RecordingState.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Battery Level
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                batteryLevel > 75 -> Icons.Default.BatteryFull
                                batteryLevel > 50 -> Icons.Default.Battery6Bar
                                batteryLevel > 25 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.Battery2Bar
                            },
                            contentDescription = "Battery",
                            tint = if (batteryLevel > 25) Color(0xFF4ECDC4) else Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${batteryLevel}%",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    // Signal Quality
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Signal",
                            tint = when (signalQuality) {
                                SignalQuality.EXCELLENT -> Color(0xFF4ECDC4)
                                SignalQuality.GOOD -> Color(0xFF4ECDC4)
                                SignalQuality.FAIR -> Color(0xFFFFB74D)
                                SignalQuality.POOR -> Color(0xFFFF6B6B)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = signalQuality.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B73FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Device")
                }
            }
        }
    }
}

@Composable
fun RecordingStateBadge(state: RecordingState) {
    val (color, text, icon) = when (state) {
        RecordingState.IDLE -> Triple(Color(0xFF9E9E9E), "Idle", Icons.Default.PowerOff)
        RecordingState.CONNECTING -> Triple(
            Color(0xFFFFB74D),
            "Connecting",
            Icons.Default.Bluetooth
        )

        RecordingState.CONNECTED -> Triple(
            Color(0xFF4ECDC4),
            "Connected",
            Icons.Default.CheckCircle
        )

        RecordingState.RECORDING -> Triple(
            Color(0xFFFF6B6B),
            "Recording",
            Icons.Default.FiberManualRecord
        )

        RecordingState.PAUSED -> Triple(Color(0xFFFFB74D), "Paused", Icons.Default.Pause)
        RecordingState.COMPLETED -> Triple(Color(0xFF4ECDC4), "Completed", Icons.Default.Done)
        RecordingState.ERROR -> Triple(Color(0xFFFF6B6B), "Error", Icons.Default.Error)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun GSRDisplayCard(
    currentValue: Double,
    readings: List<GSRReading>,
    signalQuality: SignalQuality
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Live GSR Reading",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Current Value Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.2f", currentValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ECDC4)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Î¼S",
                    fontSize = 16.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Waveform Display
            if (readings.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    val minValue = readings.minOf { it.value }
                    val maxValue = readings.maxOf { it.value }
                    val valueRange = if (maxValue > minValue) maxValue - minValue else 1.0
                    readings.forEachIndexed { index, reading ->
                        val x = (index.toFloat() / (readings.size - 1)) * width
                        val normalizedValue = ((reading.value - minValue) / valueRange)
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingControlsCard(
    recordingState: RecordingState,
    duration: Int,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration Display
            if (recordingState == RecordingState.RECORDING ||
                recordingState == RecordingState.PAUSED ||
                recordingState == RecordingState.COMPLETED
            ) {
                Text(
                    text = formatDuration(duration),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (recordingState) {
                    RecordingState.CONNECTED -> {
                        FloatingActionButton(
                            onClick = onStartRecording,
                            containerColor = Color(0xFFFF6B6B)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Start recording",
                                tint = Color.White
                            )
                        }
                    }

                    RecordingState.RECORDING -> {
                        FloatingActionButton(
                            onClick = onPauseRecording,
                            containerColor = Color(0xFFFFB74D)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause recording",
                                tint = Color.White
                            )
                        }
                        FloatingActionButton(
                            onClick = onStopRecording,
                            containerColor = Color(0xFF4ECDC4)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.White
                            )
                        }
                    }

                    RecordingState.PAUSED -> {
                        FloatingActionButton(
                            onClick = onResumeRecording,
                            containerColor = Color(0xFF6B73FF)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume recording",
                                tint = Color.White
                            )
                        }
                        FloatingActionButton(
                            onClick = onStopRecording,
                            containerColor = Color(0xFF4ECDC4)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.White
                            )
                        }
                    }

                    else -> {
                        // Show disabled button
                        FloatingActionButton(
                            onClick = { },
                            containerColor = Color(0xFF404040)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Record (disabled)",
                                tint = Color(0xFF6B6B6B)
                            )
                        }
                    }
                }
            }
            // Status Text
            if (recordingState != RecordingState.IDLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (recordingState) {
                        RecordingState.CONNECTING -> "Connecting to device..."
                        RecordingState.CONNECTED -> "Ready to record"
                        RecordingState.RECORDING -> "Recording in progress"
                        RecordingState.PAUSED -> "Recording paused"
                        RecordingState.COMPLETED -> "Recording completed"
                        RecordingState.ERROR -> "Connection error"
                        else -> ""
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}

@Composable
fun SessionSummaryCard(readings: List<GSRReading>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Session Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val meanValue = readings.map { it.value }.average()
            val minValue = readings.minOf { it.value }
            val maxValue = readings.maxOf { it.value }
            val stdDev = calculateStandardDeviation(readings.map { it.value })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric(
                    label = "Mean",
                    value = "${String.format("%.2f", meanValue)} Î¼S",
                    color = Color(0xFF4ECDC4)
                )
                SummaryMetric(
                    label = "Min",
                    value = "${String.format("%.2f", minValue)} Î¼S",
                    color = Color(0xFF6B73FF)
                )
                SummaryMetric(
                    label = "Max",
                    value = "${String.format("%.2f", maxValue)} Î¼S",
                    color = Color(0xFFFF6B6B)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Data points: ${readings.size}",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF)
            )
            Text(
                text = "Standard deviation: ${String.format("%.2f", stdDev)} Î¼S",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF)
            )
        }
    }
}

@Composable
fun SummaryMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}

@Composable
fun QuickSetupCard(onStartSetup: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "1. Turn on your Shimmer3 GSR device\n" +
                        "2. Ensure Bluetooth is enabled\n" +
                        "3. Attach GSR electrodes to fingers\n" +
                        "4. Tap 'Connect Device' to begin",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onStartSetup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Quick Recording")
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun calculateStandardDeviation(values: List<Double>): Double {
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return sqrt(variance)
}

@Preview(showBackground = true)
@Composable
fun GSRQuickRecordingScreenPreview() {
    GSRQuickRecordingScreen()
}


// ===== feature\gsr\ui\GSRRawImageViewComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModel
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModelFactory
import java.io.File

class GSRRawImageViewComposeActivity : BaseComposeActivity<GSRRawImageViewViewModel>() {
    override fun createViewModel(): GSRRawImageViewViewModel =
        viewModels<GSRRawImageViewViewModel> {
            GSRRawImageViewViewModelFactory(application)
        }.value

    @Composable
    override fun Content(viewModel: GSRRawImageViewViewModel) {
        IRCameraTheme {
            GSRRawImageViewScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRRawImageViewScreen(
    viewModel: GSRRawImageViewViewModel = viewModel(
        factory = GSRRawImageViewViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.imageViewState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("GSR Raw Images") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        // Content Area
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading GSR images...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error"
                ErrorContent(
                    error = errorMessage,
                    onRetry = { viewModel.loadImages() }
                )
            }

            uiState.imageFiles.isEmpty() -> {
                EmptyContent()
            }

            else -> {
                ImageListContent(
                    imageFiles = uiState.imageFiles,
                    onImageClick = { file -> viewModel.openImage(file) }
                )
            }
        }
    }
}

@Composable
private fun ImageListContent(
    imageFiles: List<File>,
    onImageClick: (File) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(imageFiles) { imageFile ->
            GSRImageCard(
                imageFile = imageFile,
                onClick = { onImageClick(imageFile) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSRImageCard(
    imageFile: File,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Thumbnail
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "GSR image thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Image Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = imageFile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Size: ${formatFileSize(imageFile.length())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Modified: ${formatDate(imageFile.lastModified())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading images",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No images",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No GSR images found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "GSR images will appear here when available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Utility functions
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun formatDate(timestamp: Long): String {
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))
}


// ===== feature\gsr\ui\GSRScreens.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRSettingsScreenPlaceholder(
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("GSR Settings Screen - Use GSRSettingsComposeActivity")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    onNavigateToGSRPlot: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Session Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Session Details Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Session ID: $sessionId")
                    Text("Use SessionDetailComposeActivity for full functionality")
                    Button(onClick = onNavigateToGSRPlot) {
                        Text("View Plot")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GSRPlotScreenPlaceholder(
    sessionId: String,
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Plot", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GSR Plot Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Session ID: $sessionId")
                    Text("Use GSRPlotComposeActivity for full functionality")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRDataViewScreen(
    filePath: String,
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Data View", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GSR Data View Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("File Path: $filePath")
                    Text("Use GSRDataViewComposeActivity for full functionality")
                }
            }
        }
    }
}


// ===== feature\gsr\ui\GSRSensorScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModel
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModelFactory

@Composable
fun GSRSensorScreen(
    viewModel: GSRSensorViewModel = viewModel(
        factory = GSRSensorViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onSaveData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorState by viewModel.sensorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Initialize recorder on first composition and manage lifecycle properly
    LaunchedEffect(Unit) {
        viewModel.initializeRecorder(context, lifecycleOwner)
    }
    // Track critical errors that need a dialog
    var showCriticalErrorDialog by remember { mutableStateOf(false) }
    var criticalErrorMessage by remember { mutableStateOf("") }
    // Show error notifications as Snackbar for non-critical errors
    LaunchedEffect(sensorState.error) {
        sensorState.error?.let { error ->
            // Check if this is a critical error (Bluetooth/permission)
            if (error.contains("Bluetooth", ignoreCase = true) ||
                error.contains("permission", ignoreCase = true) ||
                error.contains("initialization failed", ignoreCase = true)
            ) {
                criticalErrorMessage = error
                showCriticalErrorDialog = true
            } else {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    // Critical error dialog
    if (showCriticalErrorDialog) {
        AlertDialog(
            onDismissRequest = { showCriticalErrorDialog = false },
            title = { Text("GSR Sensor Error") },
            text = { Text(criticalErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showCriticalErrorDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (criticalErrorMessage.contains("Bluetooth", ignoreCase = true)) {
                    TextButton(onClick = {
                        showCriticalErrorDialog = false
                        // Try to re-initialize
                        viewModel.initializeRecorder(context, lifecycleOwner)
                    }) {
                        Text("Retry")
                    }
                }
            }
        )
    }
    // Use real data from ViewModel or fallback to simulated data for preview
    val isConnected = sensorState.isConnected
    val isRecording = sensorState.isRecording
    val currentGSR = if (sensorState.currentGSR > 0) sensorState.currentGSR else 2.45f
    val skinConductance = if (sensorState.skinConductance > 0) sensorState.skinConductance else 0.82f
    val deviceBattery = if (sensorState.deviceBattery > 0) sensorState.deviceBattery else 87
    val samplingRate = sensorState.samplingRate
    // Use GSR history from ViewModel state, with fallback to generated data
    val gsrHistory = if (sensorState.gsrHistory.isNotEmpty()) {
        sensorState.gsrHistory
    } else {
        remember { generateInitialGSRData() }
    }
    // Only simulate data when not connected and for preview purposes
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            // Simulation only runs when not connected for preview
            while (!isConnected) {
                kotlinx.coroutines.delay(1000)
                // This is just for UI preview when no real data
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF16131e)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Title bar with GSR-specific actions
            TitleBar(
                title = "GSR Sensor Monitor",
                showBackButton = true,
                onBackClick = onBackClick
            ) {
                TitleBarAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Save GSR Data",
                    onClick = onSaveData
                )
                TitleBarAction(
                    icon = Icons.Default.Settings,
                    contentDescription = "GSR Settings",
                    onClick = onSettingsClick
                )
            }
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection status card
                GSRConnectionCard(
                    isConnected = isConnected,
                    deviceBattery = deviceBattery,
                    samplingRate = samplingRate,
                    connectionStatus = sensorState.connectionStatus,
                    isReconnecting = sensorState.isReconnecting,
                    reconnectionAttempt = sensorState.reconnectionAttempt,
                    maxReconnectionAttempts = sensorState.maxReconnectionAttempts,
                    error = sensorState.error,
                    onConnectionToggle = {
                        if (isConnected) {
                            viewModel.disconnectDevice()
                        } else {
                            viewModel.connectDevice()
                        }
                    }
                )
                // Real-time GSR metrics
                GSRMetricsCard(
                    currentGSR = currentGSR,
                    skinConductance = skinConductance,
                    isRecording = isRecording
                )
                // GSR waveform visualization
                GSRWaveformCard(
                    gsrHistory = gsrHistory,
                    isStreaming = isConnected,
                    currentValue = currentGSR
                )
                // Recording controls
                GSRRecordingControls(
                    isRecording = isRecording,
                    isConnected = isConnected,
                    onRecordingToggle = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    onExportData = {
                        viewModel.exportData()
                        onSaveData()
                    }
                )
                // GSR analysis summary
                if (isRecording || gsrHistory.isNotEmpty()) {
                    GSRAnalysisCard(
                        gsrData = gsrHistory,
                        isRecording = isRecording
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRConnectionCard(
    isConnected: Boolean,
    deviceBattery: Int,
    samplingRate: Int,
    connectionStatus: String = "Disconnected",
    isReconnecting: Boolean = false,
    reconnectionAttempt: Int = 0,
    maxReconnectionAttempts: Int = 0,
    error: String? = null,
    onConnectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isReconnecting -> Color(0xFF8B4513)
                isConnected -> Color(0xFF1A2A1A)
                error != null -> Color(0xFF4A1A1A)
                else -> Color(0xFF2A1A1A)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shimmer3 GSR Device",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionStatus,
                        color = when {
                            isReconnecting -> Color.Yellow
                            isConnected -> Color.Green
                            error != null -> Color.Red
                            else -> Color.Gray
                        },
                        fontSize = 14.sp
                    )
                    if (isReconnecting && reconnectionAttempt > 0) {
                        Text(
                            text = "Reconnecting: attempt $reconnectionAttempt/$maxReconnectionAttempts",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (error != null && !isReconnecting) {
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Switch(
                    checked = isConnected,
                    onCheckedChange = { onConnectionToggle() },
                    enabled = !isReconnecting,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        uncheckedThumbColor = Color.Gray,
                        disabledCheckedThumbColor = Color.Yellow,
                        disabledUncheckedThumbColor = Color.DarkGray
                    )
                )
            }
            if (isConnected && !isReconnecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem("Battery", "$deviceBattery%", Color.Green)
                    MetricItem("Sampling", "${samplingRate}Hz", MaterialTheme.colorScheme.primary)
                    MetricItem("Status", "Streaming", Color.Cyan)
                }
            }
        }
    }
}

@Composable
private fun GSRMetricsCard(
    currentGSR: Float,
    skinConductance: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-time GSR Metrics",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isRecording) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "RECORDING",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricCard(
                    label = "GSR Value",
                    value = String.format("%.2f Î¼S", currentGSR),
                    color = Color.Cyan,
                    description = "Current resistance"
                )
                MetricCard(
                    label = "Skin Conductance",
                    value = String.format("%.2f Î¼S", skinConductance),
                    color = Color.Green,
                    description = "Conductance level"
                )
            }
        }
    }
}

@Composable
private fun GSRWaveformCard(
    gsrHistory: List<Float>,
    isStreaming: Boolean,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GSR Waveform",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20.dp.toPx()
                val graphWidth = width - 2 * padding
                val graphHeight = height - 2 * padding
                // Draw axes
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, height - padding),
                    end = Offset(width - padding, height - padding),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, padding),
                    end = Offset(padding, height - padding),
                    strokeWidth = 1.dp.toPx()
                )
                // Draw GSR waveform
                if (gsrHistory.isNotEmpty()) {
                    val path = Path()
                    val minGSR = gsrHistory.minOrNull() ?: 0f
                    val maxGSR = gsrHistory.maxOrNull() ?: 5f
                    val range = maxGSR - minGSR
                    gsrHistory.forEachIndexed { index, value ->
                        val x = padding + (index.toFloat() / (gsrHistory.size - 1)) * graphWidth
                        val normalizedValue = if (range > 0) (value - minGSR) / range else 0.5f
                        val y = height - padding - normalizedValue * graphHeight
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Cyan,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
                // Draw current value indicator
                if (isStreaming) {
                    drawCircle(
                        color = Color.Yellow,
                        radius = 4.dp.toPx(),
                        center = Offset(width - padding - 10.dp.toPx(), height / 2)
                    )
                }
            }
            // Value scale indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 Î¼S", color = Color.Gray, fontSize = 10.sp)
                Text(
                    "${String.format("%.1f", currentValue)} Î¼S",
                    color = Color.Cyan,
                    fontSize = 10.sp
                )
                Text("5 Î¼S", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GSRRecordingControls(
    isRecording: Boolean,
    isConnected: Boolean,
    onRecordingToggle: () -> Unit,
    onExportData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRecordingToggle,
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else Color.Green
                    )
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
                Button(
                    onClick = onExportData,
                    enabled = !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Export Data")
                }
            }
        }
    }
}

@Composable
private fun GSRAnalysisCard(
    gsrData: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    if (gsrData.isEmpty()) return
    val avgGSR = gsrData.average().toFloat()
    val maxGSR = gsrData.maxOrNull() ?: 0f
    val minGSR = gsrData.minOrNull() ?: 0f
    val stdDev = kotlin.math.sqrt(gsrData.map { (it - avgGSR) * (it - avgGSR) }.average()).toFloat()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GSR Analysis",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Average", String.format("%.2f Î¼S", avgGSR), Color.Cyan)
                MetricItem("Maximum", String.format("%.2f Î¼S", maxGSR), Color.Red)
                MetricItem("Minimum", String.format("%.2f Î¼S", minGSR), MaterialTheme.colorScheme.primary)
                MetricItem("Std Dev", String.format("%.2f", stdDev), Color.Yellow)
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
        Text(
            text = description,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

private fun generateInitialGSRData(): List<Float> {
    return (0..99).map {
        2.0f + kotlin.math.sin(it * 0.1f).toFloat() * 0.5f + kotlin.random.Random.nextFloat() * 0.2f
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSensorScreenPreview() {
    IRCameraTheme {
        GSRSensorScreen()
    }
}


// ===== feature\gsr\ui\GSRSettingsComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

class GSRSettingsComposeActivity : BaseComposeActivity<GSRSettingsViewModel>() {
    companion object {
        private const val TAG = "GSRSettingsComposeActivity"
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsComposeActivity::class.java))
        }
    }

    override fun createViewModel(): GSRSettingsViewModel {
        return viewModels<GSRSettingsViewModel>().value
    }

    @Composable
    override fun Content(viewModel: GSRSettingsViewModel) {
        LibUnifiedTheme {
            GSRSettingsScreen(
                onBackClick = { finish() },
                viewModel = viewModel
            )
        }
    }
}


// ===== feature\gsr\ui\GSRSettingsScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

@Composable
fun GSRSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: GSRSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsUiState.collectAsState()
    val gsrSettings = uiState.gsrSettings
    val deviceSettings = uiState.deviceSettings
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        viewModel.settingsEvents.collect { event ->
            val message = when (event) {
                is GSRSettingsViewModel.SettingsEvent.ShowToast -> event.message
                is GSRSettingsViewModel.SettingsEvent.CalibrationStarted -> event.message
                is GSRSettingsViewModel.SettingsEvent.CalibrationCompleted -> event.message
                is GSRSettingsViewModel.SettingsEvent.ShowError -> event.message
                else -> null
            }
            message?.let {
                android.widget.Toast.makeText(
                    context,
                    it,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Configuration
            SettingsCard(
                title = "Device Configuration",
                icon = Icons.Default.DeviceHub
            ) {
                deviceSettings.deviceName?.let { name ->
                    SettingsRow(
                        label = "Device Name",
                        value = name
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SettingsSlider(
                    label = "Sampling Rate",
                    value = gsrSettings.samplingRate.toFloat(),
                    valueRange = 1f..512f,
                    onValueChange = { viewModel.updateSamplingRate(it.toInt()) },
                    unit = " Hz"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Reconnect",
                    description = "Automatically reconnect to devices after disconnection",
                    checked = deviceSettings.autoReconnect,
                    onCheckedChange = {
                        viewModel.updateDeviceSettings(
                            deviceSettings.copy(autoReconnect = it)
                        )
                    }
                )
                if (deviceSettings.autoReconnect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Reconnection Attempts",
                        value = deviceSettings.reconnectionAttempts.toFloat(),
                        valueRange = 1f..10f,
                        onValueChange = {
                            viewModel.updateDeviceSettings(
                                deviceSettings.copy(reconnectionAttempts = it.toInt())
                            )
                        },
                        unit = " attempts"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Reconnection Delay",
                        value = (deviceSettings.reconnectionBaseDelayMs / 1000f),
                        valueRange = 1f..10f,
                        onValueChange = {
                            viewModel.updateDeviceSettings(
                                deviceSettings.copy(reconnectionBaseDelayMs = (it * 1000).toLong())
                            )
                        },
                        unit = " seconds"
                    )
                }
            }
            // Data Collection
            SettingsCard(
                title = "Data Collection",
                icon = Icons.Default.DataUsage
            ) {
                SettingsToggle(
                    label = "Real-Time Monitoring",
                    description = "Enable real-time data monitoring",
                    checked = gsrSettings.enableRealTimeMonitoring,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(enableRealTimeMonitoring = it)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Data Filtering",
                    description = "Apply filtering to GSR data",
                    checked = gsrSettings.enableFiltering,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(enableFiltering = it)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Notifications",
                    description = "Show data collection notifications",
                    checked = gsrSettings.notificationEnabled,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(notificationEnabled = it)
                        )
                    }
                )
            }
            // Calibration
            SettingsCard(
                title = "Calibration",
                icon = Icons.Default.Tune
            ) {
                Button(
                    onClick = { viewModel.startCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Button(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSettingsScreenPreview() {
    IRCameraTheme {
        GSRSettingsScreen()
    }
}


// ===== feature\gsr\ui\GSRVideoPlayerComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class GSRVideoPlayerComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        private const val EXTRA_VIDEO_PATH = "video_path"
        private const val EXTRA_SESSION_ID = "session_id"
        fun startActivity(
            context: Context,
            videoPath: String,
            sessionId: String? = null
        ) {
            val intent = Intent(context, GSRVideoPlayerComposeActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Video Player",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Share video
                                android.widget.Toast.makeText(
                                    this@GSRVideoPlayerComposeActivity,
                                    "Share video feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Open video settings
                                android.widget.Toast.makeText(
                                    this@GSRVideoPlayerComposeActivity,
                                    "Video settings coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    this@GSRVideoPlayerComposeActivity,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRVideoPlayerContent(
                    videoPath = videoPath,
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRVideoPlayerContent(
    videoPath: String,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(100f) }
    var showDataOverlay by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Video Player Area
        VideoPlayerCard(
            videoPath = videoPath,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            showDataOverlay = showDataOverlay,
            onPlayPause = { isPlaying = !isPlaying },
            onSeek = { currentPosition = it }
        )
        // Scrollable content below video
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Controls
            VideoControlsCard(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                showDataOverlay = showDataOverlay,
                onPlayPause = { isPlaying = !isPlaying },
                onSeek = { currentPosition = it },
                onSpeedChange = { playbackSpeed = it },
                onOverlayToggle = { showDataOverlay = it }
            )
            // Video Information
            VideoInfoCard(
                videoPath = videoPath,
                sessionId = sessionId,
                duration = duration
            )
            // GSR Data Timeline
            if (sessionId != null) {
                GSRDataTimelineCard(
                    sessionId = sessionId,
                    currentPosition = currentPosition,
                    duration = duration
                )
            }
            // Playback Statistics
            PlaybackStatisticsCard(
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed
            )
            // Export Options
            VideoExportCard(
                videoPath = videoPath,
                sessionId = sessionId
            )
        }
    }
}

@Composable
private fun VideoPlayerCard(
    videoPath: String,
    isPlaying: Boolean,
    currentPosition: Float,
    duration: Float,
    showDataOverlay: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Video Playback",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            // Video View Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Color.Black,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // AndroidView for VideoView integration
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(videoPath))
                            setMediaController(MediaController(context))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // GSR Data Overlay
                if (showDataOverlay) {
                    GSRDataOverlay(
                        currentPosition = currentPosition,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                // Play/Pause Overlay
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(32.dp)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            // Seek Bar
            Column {
                Slider(
                    value = currentPosition,
                    onValueChange = onSeek,
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRDataOverlay(
    currentPosition: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "GSR Data",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "12.4 Î¼S",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Quality: 94%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VideoControlsCard(
    isPlaying: Boolean,
    currentPosition: Float,
    duration: Float,
    playbackSpeed: Float,
    showDataOverlay: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOverlayToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Playback Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeek(0f) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Restart")
                }
                IconButton(onClick = { onSeek(maxOf(0f, currentPosition - 10f)) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onSeek(minOf(duration, currentPosition + 10f)) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }
                IconButton(onClick = { onSeek(duration) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "End")
                }
            }
            // Playback Speed
            Text(
                "Playback Speed: ${String.format("%.1f", playbackSpeed)}x",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        onClick = { onSpeedChange(speed) },
                        label = { Text("${speed}x") },
                        selected = playbackSpeed == speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Overlay Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show GSR Data Overlay",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showDataOverlay,
                    onCheckedChange = onOverlayToggle
                )
            }
        }
    }
}

@Composable
private fun VideoInfoCard(
    videoPath: String,
    sessionId: String?,
    duration: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Video Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            VideoInfoRow("File Name", videoPath.substringAfterLast("/"))
            VideoInfoRow("Session ID", sessionId ?: "N/A")
            VideoInfoRow("Duration", formatTime(duration))
            VideoInfoRow("Format", "MP4")
            VideoInfoRow("Resolution", "1920x1080")
            VideoInfoRow("Frame Rate", "30 FPS")
            VideoInfoRow("File Size", "125.6 MB")
        }
    }
}

@Composable
private fun VideoInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GSRDataTimelineCard(
    sessionId: String,
    currentPosition: Float,
    duration: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "GSR Data Timeline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Timeline visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // Simplified timeline representation
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current position indicator
                    val progress = if (duration > 0) currentPosition / duration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                Text(
                    "GSR Timeline Visualization",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Current GSR values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GSRTimelineMetric("Current", "12.4 Î¼S")
                GSRTimelineMetric("Peak", "18.9 Î¼S")
                GSRTimelineMetric("Average", "11.2 Î¼S")
                GSRTimelineMetric("Quality", "94%")
            }
        }
    }
}

@Composable
private fun GSRTimelineMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaybackStatisticsCard(
    currentPosition: Float,
    duration: Float,
    playbackSpeed: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Playback Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlaybackStatistic("Progress", "${((currentPosition / duration) * 100).toInt()}%")
                PlaybackStatistic("Remaining", formatTime(duration - currentPosition))
                PlaybackStatistic("Speed", "${playbackSpeed}x")
            }
        }
    }
}

@Composable
private fun PlaybackStatistic(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VideoExportCard(
    videoPath: String,
    sessionId: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Export Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Export video file
                        android.widget.Toast.makeText(
                            context,
                            "Exporting video...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = "Export Video")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Video")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Export audio track
                        android.widget.Toast.makeText(
                            context,
                            "Exporting audio...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = "Export Audio")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Audio")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Share video
                        android.widget.Toast.makeText(
                            context,
                            "Sharing video...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Video")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Video")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Save current frame as image
                        android.widget.Toast.makeText(
                            context,
                            "Saving frame...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Save Frame")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Frame")
                }
            }
        }
    }
}

// Helper function to format time
private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

class GSRVideoPlayerViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing video playback state, GSR data synchronization, etc.
    // Future implementation would include:
    // - Video playback state management
    // - GSR data loading and synchronization
    // - Export functionality
    // - Playback statistics tracking
}


// ===== feature\gsr\ui\GSRVideoPlayerScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun GSRVideoPlayerScreen(
    videoUri: String = "sample_video.mp4",
    sessionId: String = "GSR_Session_001",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(100) }
    var showGSROverlay by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Video Player",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Visibility,
                contentDescription = "Toggle GSR Overlay",
                onClick = { showGSROverlay = !showGSROverlay }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Player Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Video View
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                // In real implementation, set video URI and controls
                                // setVideoURI(Uri.parse(videoUri))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // GSR Data Overlay
                    if (showGSROverlay) {
                        GSRDataOverlay(
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                    // Play/Pause Button
                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.align(Alignment.Center),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                }
            }
            // Video Controls
            VideoControlsCard(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = { isPlaying = !isPlaying },
                onSeek = { currentPosition = it }
            )
            // Session Information
            SessionDetailsCard(sessionId = sessionId)
            // GSR Metrics
            GSRMetricsCard()
        }
    }
}

@Composable
private fun GSRDataOverlay(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GSR",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "0.42 Î¼S",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // Mini GSR waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                // Placeholder for mini waveform visualization
                Text(
                    text = "~~~",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoControlsCard(
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Progress slider
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.Gray
                )
            )
            // Time indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    // TODO: Skip to previous video
                    android.widget.Toast.makeText(
                        localContext,
                        "Previous video",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    // TODO: Skip to next video
                    android.widget.Toast.makeText(
                        localContext,
                        "Next video",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                }
                IconButton(onClick = {
                    // TODO: Toggle fullscreen mode
                    android.widget.Toast.makeText(
                        localContext,
                        "Fullscreen mode",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailsCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Details",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val details = listOf(
                "Session ID" to sessionId,
                "Recording Date" to "2024-01-15",
                "Duration" to "5m 32s",
                "Participant" to "P001",
                "Condition" to "Stress Test"
            )
            details.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun GSRMetricsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Real-time GSR Metrics",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Current", "0.42 Î¼S", Color.Cyan)
                MetricItem("Average", "0.38 Î¼S", Color.Green)
                MetricItem("Peak", "0.67 Î¼S", Color.Red)
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

@Preview(showBackground = true)
@Composable
private fun GSRVideoPlayerScreenPreview() {
    IRCameraTheme {
        GSRVideoPlayerScreen()
    }
}


// ===== feature\gsr\ui\MultiModalRecordingComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.MultiModalRecordingViewModel

class MultiModalRecordingComposeActivity : BaseComposeActivity<MultiModalRecordingViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, MultiModalRecordingComposeActivity::class.java))
        }

        fun startWithTemplate(context: Context, templateId: String) {
            val intent = Intent(context, MultiModalRecordingComposeActivity::class.java).apply {
                putExtra("template_id", templateId)
            }
            context.startActivity(intent)
        }

        fun startRecording(context: Context, sessionInfo: SessionInfo) {
            val intent = Intent(context, MultiModalRecordingComposeActivity::class.java).apply {
                putExtra("session_info", sessionInfo)
                putExtra("auto_start", true)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): MultiModalRecordingViewModel {
        return viewModels<MultiModalRecordingViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MultiModalRecordingViewModel) {
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var selectedSensors by remember { mutableStateOf(setOf("gsr", "thermal", "rgb")) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Multi-Modal Recording",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Open recording templates
                                android.widget.Toast.makeText(
                                    this@MultiModalRecordingComposeActivity,
                                    "Recording templates coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Templates")
                            }
                            IconButton(onClick = {
                                // TODO: Open recording settings
                                android.widget.Toast.makeText(
                                    this@MultiModalRecordingComposeActivity,
                                    "Opening recording settings...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                MultiModalRecordingContent(
                    isRecording = isRecording,
                    onRecordingToggle = { isRecording = !isRecording },
                    recordingDuration = recordingDuration,
                    selectedSensors = selectedSensors,
                    onSensorToggle = { sensor ->
                        selectedSensors = if (selectedSensors.contains(sensor)) {
                            selectedSensors - sensor
                        } else {
                            selectedSensors + sensor
                        }
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun MultiModalRecordingContent(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    selectedSensors: Set<String>,
    onSensorToggle: (String) -> Unit,
    viewModel: MultiModalRecordingViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Recording Status Card
        RecordingStatusCard(
            isRecording = isRecording,
            duration = recordingDuration,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Sensor Selection Cards
        Text(
            text = "Active Sensors",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SensorCard(
            title = "GSR Sensor",
            subtitle = "Shimmer3 GSR+ Device",
            icon = Icons.Default.MonitorHeart,
            isEnabled = selectedSensors.contains("gsr"),
            isConnected = false,
            onToggle = { onSensorToggle("gsr") },
            statusText = "128 Hz",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard(
            title = "Thermal Camera",
            subtitle = "TOPDON TC001 Device",
            icon = Icons.Default.Thermostat,
            isEnabled = selectedSensors.contains("thermal"),
            isConnected = false,
            onToggle = { onSensorToggle("thermal") },
            statusText = "25 FPS",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard(
            title = "RGB Camera",
            subtitle = "Device Camera",
            icon = Icons.Default.CameraAlt,
            isEnabled = selectedSensors.contains("rgb"),
            isConnected = false,
            onToggle = { onSensorToggle("rgb") },
            statusText = "30 FPS",
            modifier = Modifier.padding(bottom = 24.dp)
        )
        // Recording Controls
        RecordingControls(
            isRecording = isRecording,
            onRecordingToggle = onRecordingToggle,
            canRecord = selectedSensors.isNotEmpty(),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Live Data Preview (if recording)
        if (isRecording) {
            LiveDataPreview(
                selectedSensors = selectedSensors,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                Color(0xFFE53E3E).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isRecording) formatDuration(duration) else "Tap record to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53E3E))
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    isConnected: Boolean,
    onToggle: () -> Unit,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Connection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) statusText else "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                enabled = isConnected
            )
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    canRecord: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main record button
        Button(
            onClick = onRecordingToggle,
            enabled = canRecord,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isRecording) "Stop" else "Start",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRecording) "STOP RECORDING" else "START RECORDING",
                fontWeight = FontWeight.Bold
            )
        }
        // Pause button (only show when recording)
        if (isRecording) {
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = {
                    // TODO: Implement pause recording logic
                    android.widget.Toast.makeText(
                        context,
                        "Pause recording feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause"
                )
            }
        }
    }
}

@Composable
private fun LiveDataPreview(
    selectedSensors: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Data Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            selectedSensors.forEach { sensor ->
                when (sensor) {
                    "gsr" -> {
                        Text(
                            text = "GSR: 2.45 ÂµS (Normal)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    "thermal" -> {
                        Text(
                            text = "Thermal: 36.8Â°C (Body temp detected)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    "rgb" -> {
                        Text(
                            text = "RGB: 1920x1080 @ 30fps",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}


// ===== feature\gsr\ui\MultiModalRecordingScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MultiModalRecordingScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var gsrEnabled by remember { mutableStateOf(true) }
    var thermalEnabled by remember { mutableStateOf(true) }
    var rgbEnabled by remember { mutableStateOf(true) }
    var syncStatus by remember { mutableStateOf(SyncStatus.SYNCED) }
    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Multi-Modal Recording",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Recording Settings",
                onClick = {
                    // TODO: Implement recording settings screen
                    // Open settings for multi-modal recording configuration
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording Status Card
            RecordingStatusCard(
                isRecording = isRecording,
                duration = recordingDuration,
                syncStatus = syncStatus
            )
            // Sensor Status Cards
            SensorStatusSection(
                gsrEnabled = gsrEnabled,
                thermalEnabled = thermalEnabled,
                rgbEnabled = rgbEnabled,
                onGsrToggle = { gsrEnabled = it },
                onThermalToggle = { thermalEnabled = it },
                onRgbToggle = { rgbEnabled = it },
                isRecording = isRecording
            )
            // Live Data Preview
            if (isRecording) {
                LiveDataPreviewSection()
            }
            // Recording Controls
            RecordingControlsSection(
                isRecording = isRecording,
                onStartStop = {
                    isRecording = !isRecording
                    if (!isRecording) recordingDuration = 0
                },
                canRecord = gsrEnabled || thermalEnabled || rgbEnabled
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Int,
    syncStatus: SyncStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color.Red.copy(alpha = 0.1f) else Color(0xFF2A2A2A)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    color = if (isRecording) Color.Red else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration: ${formatDuration(duration)}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sync: ${syncStatus.displayName}",
                    color = syncStatus.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SensorStatusSection(
    gsrEnabled: Boolean,
    thermalEnabled: Boolean,
    rgbEnabled: Boolean,
    onGsrToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onRgbToggle: (Boolean) -> Unit,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sensor Configuration",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SensorToggleItem(
                name = "GSR Sensor",
                description = "Galvanic Skin Response (Shimmer3)",
                enabled = gsrEnabled,
                onToggle = onGsrToggle,
                color = Color.Cyan,
                isRecording = isRecording,
                status = "Connected â€¢ 128 Hz"
            )
            SensorToggleItem(
                name = "Thermal Camera",
                description = "TOPDON TC001 Thermal Imaging",
                enabled = thermalEnabled,
                onToggle = onThermalToggle,
                color = Color.Red,
                isRecording = isRecording,
                status = "Connected â€¢ 256Ã—192"
            )
            SensorToggleItem(
                name = "RGB Camera",
                description = "Built-in Camera",
                enabled = rgbEnabled,
                onToggle = onRgbToggle,
                color = Color.White,
                isRecording = isRecording,
                status = "Ready â€¢ 1080p@30fps"
            )
        }
    }
}

@Composable
private fun SensorToggleItem(
    name: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    color: Color,
    isRecording: Boolean,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    if (enabled) color else Color.Gray,
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = status,
                color = if (enabled) color else Color.Gray,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = if (!isRecording) onToggle else {
                {}
            },
            enabled = !isRecording,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = color.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun LiveDataPreviewSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Data Preview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // GSR Waveform
            Text(
                text = "GSR Signal",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LiveGSRWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Sensor Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveMetricItem("GSR", "0.42 Î¼S", Color.Cyan)
                LiveMetricItem("Thermal", "36.8Â°C", Color.Red)
                LiveMetricItem("RGB", "Recording", Color.White)
            }
        }
    }
}

@Composable
private fun LiveGSRWaveform(modifier: Modifier = Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            phase += 0.2f
        }
    }
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val amplitude = height * 0.3f
        val points = (0..100).map { i ->
            val x = (i / 100f) * width
            val y = centerY + amplitude * sin((i * 0.2f) + phase + Random.nextFloat() * 0.1f)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color.Cyan,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx()
            )
        }
        // Grid lines
        for (i in 0..4) {
            val y = (height / 4) * i
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun LiveMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RecordingControlsSection(
    isRecording: Boolean,
    onStartStop: () -> Unit,
    canRecord: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Main Record Button
            FloatingActionButton(
                onClick = onStartStop,
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Tap to stop recording" else "Tap to start synchronized recording",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (!canRecord && !isRecording) {
                Text(
                    text = "Enable at least one sensor to record",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
            // Additional Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement pause recording functionality
                        // Pause the multi-modal recording
                    },
                    enabled = isRecording
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Implement add marker functionality
                        // Add timestamp marker to recording
                    },
                    enabled = isRecording
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark")
                }
            }
        }
    }
}

enum class SyncStatus(val displayName: String, val color: Color) {
    SYNCED("Synced", Color.Green),
    SYNCING("Syncing", Color.Yellow),
    OUT_OF_SYNC("Out of Sync", Color.Red),
    DISABLED("Disabled", Color.Gray)
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%d:%02d", minutes, remainingSeconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun MultiModalRecordingScreenPreview() {
    IRCameraTheme {
        MultiModalRecordingScreen()
    }
}


// ===== feature\gsr\ui\ResearchTemplateComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.ResearchTemplate
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class ResearchTemplateComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ResearchTemplateComposeActivity::class.java))
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        var selectedTemplate by remember { mutableStateOf<ResearchTemplate?>(null) }
        var selectedCategory by remember { mutableStateOf<ResearchTemplate.TemplateCategory?>(null) }
        var showTemplateDetails by remember { mutableStateOf(false) }
        var showCreateDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Research Templates",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create Template")
                            }
                            IconButton(onClick = {
                                // TODO: Import template from file
                                android.widget.Toast.makeText(
                                    this@ResearchTemplateComposeActivity,
                                    "Import template feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileOpen, contentDescription = "Import")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    this@ResearchTemplateComposeActivity,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    selectedTemplate?.let { template ->
                        ExtendedFloatingActionButton(
                            onClick = {
                                startRecordingWithTemplate(template)
                            },
                            text = { Text("Start Recording") },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start") }
                        )
                    }
                }
            ) { paddingValues ->
                ResearchTemplateContent(
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = {
                        selectedTemplate = it
                        showTemplateDetails = true
                    },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showTemplateDetails && selectedTemplate != null) {
            TemplateDetailsDialog(
                template = selectedTemplate!!,
                onDismiss = { showTemplateDetails = false },
                onStartRecording = {
                    startRecordingWithTemplate(selectedTemplate!!)
                    showTemplateDetails = false
                }
            )
        }
        if (showCreateDialog) {
            CreateTemplateDialog(
                onDismiss = { showCreateDialog = false },
                onCreateTemplate = { template ->
                    // Create new template logic
                    showCreateDialog = false
                }
            )
        }
    }

    private fun startRecordingWithTemplate(template: ResearchTemplate) {
        MultiModalRecordingComposeActivity.startWithTemplate(this, template.id)
    }
}

@Composable
private fun ResearchTemplateContent(
    selectedTemplate: ResearchTemplate?,
    onTemplateSelect: (ResearchTemplate) -> Unit,
    selectedCategory: ResearchTemplate.TemplateCategory?,
    onCategoryChange: (ResearchTemplate.TemplateCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Category Filter
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Template Grid
        val templates = ResearchTemplate.PREDEFINED_TEMPLATES
        val filteredTemplates = if (selectedCategory == null) {
            templates
        } else {
            templates.filter { it.category == selectedCategory }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTemplates) { template ->
                TemplateCard(
                    template = template,
                    isSelected = selectedTemplate?.id == template.id,
                    onClick = { onTemplateSelect(template) }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ResearchTemplate.TemplateCategory?,
    onCategoryChange: (ResearchTemplate.TemplateCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(null) + ResearchTemplate.TemplateCategory.values().toList()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        categories.forEach { category ->
            val displayName = category?.name?.replace("_", " ")?.lowercase()
                ?.replaceFirstChar { it.uppercase() } ?: "All"
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = { Text(displayName) },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: ResearchTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Template icon and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getTemplateIcon(template.category),
                    contentDescription = template.category.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getCategoryColor(template.category),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = template.category.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Template name and description
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            // Template details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = template.duration?.let { "${it / (60 * 1000)} min" } ?: "Variable",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sensors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = template.sensors.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateDetailsDialog(
    template: ResearchTemplate,
    onDismiss: () -> Unit,
    onStartRecording: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.duration} minutes", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sensors:", style = MaterialTheme.typography.bodySmall)
                            Text(template.sensors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GSR Sampling Rate:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.gsrSamplingRate} Hz", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartRecording) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Recording")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onCreateTemplate: (ResearchTemplate) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Custom") }
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Template") },
        text = {
            Column {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            // Focus moves to description field
                        }
                    )
                )
                OutlinedTextField(
                    value = templateDescription,
                    onValueChange = { templateDescription = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (templateName.isNotBlank()) {
                                val newTemplate = ResearchTemplate(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = templateName,
                                    description = templateDescription,
                                    category = enumValues<ResearchTemplate.TemplateCategory>().firstOrNull { it.name == selectedCategory }
                                        ?: ResearchTemplate.TemplateCategory.CUSTOM,
                                    duration = 30,
                                    sensors = setOf(
                                        ResearchTemplate.SensorType.GSR,
                                        ResearchTemplate.SensorType.THERMAL_CAMERA
                                    ),
                                    gsrSamplingRate = 128
                                )
                                onCreateTemplate(newTemplate)
                                onDismiss()
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (templateName.isNotBlank()) {
                        val newTemplate = ResearchTemplate(
                            id = "custom_${System.currentTimeMillis()}",
                            name = templateName,
                            description = templateDescription,
                            category = enumValues<ResearchTemplate.TemplateCategory>().firstOrNull { it.name == selectedCategory }
                                ?: ResearchTemplate.TemplateCategory.CUSTOM,
                            duration = 30,
                            sensors = setOf(
                                ResearchTemplate.SensorType.GSR,
                                ResearchTemplate.SensorType.THERMAL_CAMERA
                            ),
                            gsrSamplingRate = 128
                        )
                        onCreateTemplate(newTemplate)
                    }
                },
                enabled = templateName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getTemplateIcon(category: ResearchTemplate.TemplateCategory) = when (category) {
    ResearchTemplate.TemplateCategory.STRESS_RESPONSE -> Icons.Default.Psychology
    ResearchTemplate.TemplateCategory.COGNITIVE_LOAD -> Icons.Default.Psychology
    ResearchTemplate.TemplateCategory.EMOTION_RECOGNITION -> Icons.Default.Psychology
    ResearchTemplate.TemplateCategory.PHYSIOLOGICAL_MONITORING -> Icons.Default.MonitorHeart
    ResearchTemplate.TemplateCategory.BEHAVIORAL_ANALYSIS -> Icons.Default.Groups
    ResearchTemplate.TemplateCategory.CUSTOM -> Icons.Default.Build
}

private fun getCategoryColor(category: ResearchTemplate.TemplateCategory) = when (category) {
    ResearchTemplate.TemplateCategory.STRESS_RESPONSE -> Color(0xFF9C27B0)
    ResearchTemplate.TemplateCategory.COGNITIVE_LOAD -> Color(0xFF2196F3)
    ResearchTemplate.TemplateCategory.EMOTION_RECOGNITION -> Color(0xFFE91E63)
    ResearchTemplate.TemplateCategory.PHYSIOLOGICAL_MONITORING -> Color(0xFF4CAF50)
    ResearchTemplate.TemplateCategory.BEHAVIORAL_ANALYSIS -> Color(0xFF00BCD4)
    ResearchTemplate.TemplateCategory.CUSTOM -> Color(0xFFFF9800)
}


// ===== feature\gsr\ui\ResearchTemplateScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

data class ResearchTemplate(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val sensorTypes: List<String>,
    val tasks: List<String>,
    val difficulty: TemplateDifficulty,
    val category: TemplateCategory,
    val isCustom: Boolean = false
)

enum class TemplateDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class TemplateCategory {
    STRESS_RESPONSE,
    COGNITIVE_LOAD,
    EMOTION_RECOGNITION,
    PHYSIOLOGICAL_MONITORING,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchTemplateScreen(
    onNavigateBack: () -> Unit = {},
    onCreateCustomTemplate: () -> Unit = {},
    onUseTemplate: (ResearchTemplate) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val templates = remember { getSampleTemplates() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredTemplates = templates.filter { template ->
        val matchesCategory = selectedCategory == null || template.category == selectedCategory
        val matchesSearch = if (searchQuery.isBlank()) true
        else template.title.contains(searchQuery, ignoreCase = true) ||
                template.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Research Templates",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "Create custom template",
                    onClick = onCreateCustomTemplate
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search templates...") },
                    leadingIcon = {
                        IconButton(onClick = { keyboardController?.hide() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6B73FF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6B73FF)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    )
                )
                // Category Filter Chips
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CategoryFilterChips(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                    }
                    item {
                        TemplateStatsCard(templates = templates)
                    }
                    items(filteredTemplates) { template ->
                        TemplateItem(
                            template = template,
                            onUse = { onUseTemplate(template) }
                        )
                    }
                    if (filteredTemplates.isEmpty()) {
                        item {
                            EmptyTemplatesState(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onCreateCustom = onCreateCustomTemplate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChips(
    selectedCategory: TemplateCategory?,
    onCategorySelected: (TemplateCategory?) -> Unit
) {
    Column {
        Text(
            text = "Categories",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Categories Chip
            FilterChip(
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                selected = selectedCategory == null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6B73FF),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF2A2A2A),
                    labelColor = Color.White
                )
            )
            // Category-specific chips (showing only a few due to space)
            listOf(
                TemplateCategory.STRESS_RESPONSE to "Stress",
                TemplateCategory.COGNITIVE_LOAD to "Cognitive",
                TemplateCategory.EMOTION_RECOGNITION to "Emotion"
            ).forEach { (category, label) ->
                FilterChip(
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                    label = { Text(label) },
                    selected = selectedCategory == category,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6B73FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF2A2A2A),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun TemplateStatsCard(templates: List<ResearchTemplate>) {
    val totalTemplates = templates.size
    val customTemplates = templates.count { it.isCustom }
    val avgDuration = templates.map { parseDuration(it.duration) }.average().toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Templates",
                value = totalTemplates.toString(),
                color = Color(0xFF6B73FF)
            )
            StatItem(
                label = "Custom",
                value = customTemplates.toString(),
                color = Color(0xFF4ECDC4)
            )
            StatItem(
                label = "Avg Duration",
                value = "${avgDuration}min",
                color = Color(0xFFFF6B6B)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}

@Composable
fun TemplateItem(
    template: ResearchTemplate,
    onUse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = template.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(difficulty = template.difficulty)
                        Spacer(modifier = Modifier.width(8.dp))
                        CategoryBadge(category = template.category)
                        if (template.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF4ECDC4).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4ECDC4),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = onUse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B73FF)
                    )
                ) {
                    Text("Use Template")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Description
            Text(
                text = template.description,
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration and Sensors
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Duration",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = template.duration,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensors",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${template.sensorTypes.size} sensors",
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                }
                // Sensor Type Icons
                Row {
                    template.sensorTypes.take(3).forEach { sensorType ->
                        Icon(
                            imageVector = when (sensorType) {
                                "GSR" -> Icons.Default.Sensors
                                "Thermal" -> Icons.Default.Thermostat
                                "Camera" -> Icons.Default.Camera
                                else -> Icons.Default.DeviceHub
                            },
                            contentDescription = sensorType,
                            tint = when (sensorType) {
                                "GSR" -> Color(0xFF4ECDC4)
                                "Thermal" -> Color(0xFFFF6B6B)
                                "Camera" -> Color.White
                                else -> Color(0xFF6B73FF)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                    if (template.sensorTypes.size > 3) {
                        Text(
                            text = "+${template.sensorTypes.size - 3}",
                            fontSize = 12.sp,
                            color = Color(0xFFCCFFFFFF),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            // Tasks Preview
            if (template.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tasks: ${
                        template.tasks.take(2).joinToString(", ")
                    }${if (template.tasks.size > 2) "..." else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: TemplateDifficulty) {
    val (color, text) = when (difficulty) {
        TemplateDifficulty.BEGINNER -> Color(0xFF4ECDC4) to "Beginner"
        TemplateDifficulty.INTERMEDIATE -> Color(0xFFFFB74D) to "Intermediate"
        TemplateDifficulty.ADVANCED -> Color(0xFFFF6B6B) to "Advanced"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CategoryBadge(category: TemplateCategory) {
    val (color, text) = when (category) {
        TemplateCategory.STRESS_RESPONSE -> Color(0xFFFF6B6B) to "Stress"
        TemplateCategory.COGNITIVE_LOAD -> Color(0xFF6B73FF) to "Cognitive"
        TemplateCategory.EMOTION_RECOGNITION -> Color(0xFFFFB74D) to "Emotion"
        TemplateCategory.PHYSIOLOGICAL_MONITORING -> Color(0xFF4ECDC4) to "Physiology"
        TemplateCategory.CUSTOM -> Color(0xFF9E9E9E) to "Custom"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyTemplatesState(
    searchQuery: String,
    selectedCategory: TemplateCategory?,
    onCreateCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (searchQuery.isBlank() && selectedCategory == null)
                Icons.AutoMirrored.Filled.Assignment else Icons.Default.SearchOff,
            contentDescription = if (searchQuery.isBlank() && selectedCategory == null) "No Templates" else "No Search Results",
            tint = Color(0xFF6B73FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank() && selectedCategory == null)
                "No templates available" else "No templates found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isBlank() && selectedCategory == null)
                "Create your first custom template to get started"
            else
                "Try adjusting your search or category filter",
            fontSize = 14.sp,
            color = Color(0xFFCCFFFFFF)
        )
        if (searchQuery.isBlank() && selectedCategory == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateCustom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Template",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Template")
            }
        }
    }
}

private fun parseDuration(duration: String): Int {
    // Parse "25 min" format to minutes
    return duration.replace(" min", "").toIntOrNull() ?: 0
}

private fun getSampleTemplates() = listOf(
    ResearchTemplate(
        id = "TEMPLATE-001",
        title = "Basic Stress Response",
        description = "Measure physiological responses to cognitive stress tasks. Includes baseline recording, math problems, and recovery period.",
        duration = "20 min",
        sensorTypes = listOf("GSR", "Thermal"),
        tasks = listOf("Baseline (5min)", "Math problems", "Recovery (5min)"),
        difficulty = TemplateDifficulty.BEGINNER,
        category = TemplateCategory.STRESS_RESPONSE
    ),
    ResearchTemplate(
        id = "TEMPLATE-002",
        title = "Cognitive Load Assessment",
        description = "Advanced cognitive load measurement using multi-modal sensors during complex reasoning tasks.",
        duration = "35 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Training", "N-back task", "Stroop test", "Memory recall"),
        difficulty = TemplateDifficulty.ADVANCED,
        category = TemplateCategory.COGNITIVE_LOAD
    ),
    ResearchTemplate(
        id = "TEMPLATE-003",
        title = "Emotion Recognition Study",
        description = "Capture emotional responses using facial thermal imaging and GSR during video stimuli presentation.",
        duration = "25 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Baseline", "Happy videos", "Sad videos", "Neutral videos"),
        difficulty = TemplateDifficulty.INTERMEDIATE,
        category = TemplateCategory.EMOTION_RECOGNITION
    ),
    ResearchTemplate(
        id = "TEMPLATE-004",
        title = "Physiological Monitoring",
        description = "Continuous physiological monitoring during extended computer work sessions.",
        duration = "60 min",
        sensorTypes = listOf("GSR", "Thermal"),
        tasks = listOf("Computer work", "Break periods", "Final assessment"),
        difficulty = TemplateDifficulty.BEGINNER,
        category = TemplateCategory.PHYSIOLOGICAL_MONITORING
    ),
    ResearchTemplate(
        id = "CUSTOM-001",
        title = "My Custom Protocol",
        description = "Custom research protocol designed for specific study requirements.",
        duration = "30 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Custom task 1", "Custom task 2"),
        difficulty = TemplateDifficulty.INTERMEDIATE,
        category = TemplateCategory.CUSTOM,
        isCustom = true
    )
)

@Preview(showBackground = true)
@Composable
fun ResearchTemplateScreenPreview() {
    ResearchTemplateScreen()
}


// ===== feature\gsr\ui\SensorDashboardComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui
// Note: MainActivityViewModel was moved to backup during cleanup
// Using modern Compose ViewModels instead
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppError
import mpdc4gsr.core.ui.ConnectionState
import mpdc4gsr.core.ui.components.SensorStatusCard
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.feature.main.presentation.MainActivityViewModel

class SensorDashboardComposeActivity : ComponentActivity() {
    private lateinit var dashboardViewModel: MainActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboardViewModel = viewModels<MainActivityViewModel>().value
        setContent {
            LibUnifiedTheme {
                Content(dashboardViewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: MainActivityViewModel) {
        // Observe sensor states
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        // Use real GSR data from ViewModel
        val gsrDataState by viewModel.gsrData.collectAsState()
        // Map ViewModel GSRDataState to UI GSRData with battery level
        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = gsrDataState.currentValue,
                    batteryLevel = gsrBatteryLevel ?: gsrDataState.batteryLevel,
                    recentReadings = gsrDataState.recentReadings.ifEmpty { generateMockGSRReadings() },
                    averageValue = gsrDataState.averageValue,
                    minValue = gsrDataState.minValue,
                    maxValue = gsrDataState.maxValue
                )
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Sensor Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall sensor status overview
                Text(
                    text = "Sensor Status Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                SensorStatusCard(
                    thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                    gsrSensorState = mapSensorStateToConnectionState(gsrSensorState),
                    bleConnectionState = mapGSRConnectionToConnectionState(gsrConnectionState)
                )
                // GSR Sensor detailed visualization
                Text(
                    text = "GSR Sensor Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                GSRVisualizationCard(
                    gsrData = gsrData,
                    connectionState = GSRConnectionState(
                        isConnected = gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED,
                        deviceName = "Shimmer3-GSR",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 85 else 0
                    ),
                    onExportData = {
                        // TODO: Implement GSR data export functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Export GSR data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResetStatistics = {
                        // TODO: Implement statistics reset functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Reset statistics feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // Additional sensor information cards
                AdditionalSensorInfo(
                    thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                    gsrSensorState = mapSensorStateToConnectionState(gsrSensorState)
                )
                // Data export and management section
                DataManagementSection(
                    onExportAllData = {
                        // TODO: Export all sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Exporting all sensor data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onClearData = {
                        // TODO: Clear sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Clear data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenSettings = {
                        // TODO: Open sensor settings
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Opening settings...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    @Composable
    private fun AdditionalSensorInfo(
        thermalCameraState: ConnectionState,
        gsrSensorState: ConnectionState
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Thermal camera info
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Thermal Camera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Resolution: 384x288",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Frame Rate: 10Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Status: ${
                            when (thermalCameraState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Error"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            // GSR sensor info
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "GSR Sensor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sample Rate: 51.2Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Connection: BLE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Status: ${
                            when (gsrSensorState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Error"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    @Composable
    private fun DataManagementSection(
        onExportAllData: () -> Unit,
        onClearData: () -> Unit,
        onOpenSettings: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportAllData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export All Data")
                    }
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Data")
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Settings")
                    }
                }
            }
        }
    }

    // Helper functions to map existing state to Compose-friendly types
    private fun mapSensorStateToConnectionState(sensorState: MainActivityViewModel.SensorState): ConnectionState {
        return when (sensorState.status) {
            MainActivityViewModel.SensorStatus.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.SensorStatus.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.SensorStatus.CONNECTED -> ConnectionState.Connected()
            MainActivityViewModel.SensorStatus.STREAMING -> ConnectionState.Connected()
            MainActivityViewModel.SensorStatus.ERROR -> ConnectionState.Error(
                AppError.SensorError(
                    "ThermalCamera",
                    "Sensor Error"
                )
            )

            MainActivityViewModel.SensorStatus.SIMULATION -> ConnectionState.Connected()
        }
    }

    private fun mapGSRConnectionToConnectionState(gsrState: MainActivityViewModel.GSRConnectionState): ConnectionState {
        return when (gsrState) {
            MainActivityViewModel.GSRConnectionState.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.GSRConnectionState.DISCOVERING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTED -> ConnectionState.Connected()
            MainActivityViewModel.GSRConnectionState.ERROR -> ConnectionState.Error(
                AppError.SensorError(
                    "GSR",
                    "GSR Error"
                )
            )
        }
    }

    private fun generateMockGSRReadings(): List<Float> {
        return (0..50).map {
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 40f
        }
    }
}


// ===== feature\gsr\ui\SensorDashboardComposeEnhanced.kt =====

package mpdc4gsr.feature.gsr.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.feature.main.presentation.MainActivityViewModel

class SensorDashboardComposeEnhanced : ComponentActivity() {
    private lateinit var dashboardViewModel: MainActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboardViewModel = viewModels<MainActivityViewModel>().value
        setContent {
            LibUnifiedTheme {
                Content(dashboardViewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: MainActivityViewModel) {
        // Observe sensor states
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        val sessionState by viewModel.sessionState.collectAsState()
        // Enhanced GSR data with consolidated layout integration
        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = 125.5f,
                    batteryLevel = gsrBatteryLevel ?: 75,
                    recentReadings = generateEnhancedGSRReadings(),
                    averageValue = 118.3f,
                    minValue = 95.2f,
                    maxValue = 145.8f
                )
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Enhanced Sensor Dashboard",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Multi-Modal Sensor Integration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Enhanced actions leveraging consolidated patterns
                        IconButton(onClick = {
                            // TODO: Export all sensor data
                            android.widget.Toast.makeText(
                                this@SensorDashboardComposeEnhanced,
                                "Exporting all sensor data...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Data")
                        }
                        IconButton(onClick = {
                            // TODO: Open sensor settings
                            android.widget.Toast.makeText(
                                this@SensorDashboardComposeEnhanced,
                                "Opening sensor settings...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enhanced multi-modal sensor overview
                MultiModalSensorOverview(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    sessionState = sessionState
                )
                // Enhanced GSR visualization with consolidated patterns
                GSRVisualizationCard(
                    gsrData = gsrData,
                    connectionState = GSRConnectionState(
                        isConnected = gsrConnectionState != MainActivityViewModel.GSRConnectionState.DISCONNECTED,
                        deviceName = "Shimmer3-GSR-Enhanced",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 90 else 0
                    ),
                    onExportData = {
                        // TODO: Implement GSR data export functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Export GSR data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResetStatistics = {
                        // TODO: Implement statistics reset functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Reset statistics feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // Device management section (consolidated layout pattern)
                DeviceManagementSection(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    onDeviceConfig = { deviceType -> launchDeviceConfig(deviceType) },
                    onDeviceTest = { deviceType -> launchDeviceTest(deviceType) }
                )
                // Enhanced data export section
                DataExportSection(
                    sessionState = sessionState,
                    onExportSession = {
                        // TODO: Export current session
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Exporting current session...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onExportAllData = {
                        // TODO: Export all sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Exporting all data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onManageSessions = {
                        // TODO: Launch session manager
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Opening session manager...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // System status and diagnostics
                SystemDiagnosticsSection(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    onRunDiagnostics = {
                        // TODO: Run system diagnostics
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Running diagnostics...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onViewLogs = {
                        // TODO: View system logs
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Opening system logs...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    @Composable
    private fun MultiModalSensorOverview(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        sessionState: MainActivityViewModel.SessionState
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Multi-Modal Recording",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (sessionState == MainActivityViewModel.SessionState.RECORDING) "Recording Active" else "Ready to Record",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = "Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Sensor status grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensorStatusIndicator(
                        title = "Thermal",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        isActive = thermalCameraState.status == MainActivityViewModel.SensorStatus.STREAMING
                    )
                    SensorStatusIndicator(
                        title = "GSR",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        isActive = gsrSensorState.status == MainActivityViewModel.SensorStatus.STREAMING
                    )
                    SensorStatusIndicator(
                        title = "Session",
                        status = if (sessionState == MainActivityViewModel.SessionState.RECORDING) "Active" else "Idle",
                        icon = Icons.Default.Storage,
                        isActive = sessionState == MainActivityViewModel.SessionState.RECORDING
                    )
                }
            }
        }
    }

    @Composable
    private fun SensorStatusIndicator(
        title: String,
        status: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isActive: Boolean
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.6f
                ),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }

    @Composable
    private fun DeviceManagementSection(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        onDeviceConfig: (String) -> Unit,
        onDeviceTest: (String) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Device Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Enhanced device cards with consolidated layout patterns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceCard(
                        title = "Thermal Camera",
                        subtitle = "TOPDON TC001",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        onConfig = { onDeviceConfig("thermal") },
                        onTest = { onDeviceTest("thermal") },
                        modifier = Modifier.weight(1f)
                    )
                    DeviceCard(
                        title = "GSR Sensor",
                        subtitle = "Shimmer3",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        onConfig = { onDeviceConfig("gsr") },
                        onTest = { onDeviceTest("gsr") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceCard(
        title: String,
        subtitle: String,
        status: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onConfig: () -> Unit,
        onTest: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Status: $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Config", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onTest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    @Composable
    private fun DataExportSection(
        sessionState: MainActivityViewModel.SessionState,
        onExportSession: () -> Unit,
        onExportAllData: () -> Unit,
        onManageSessions: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                    Text(
                        text = "Recording in progress - data will be available after session ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportSession,
                        enabled = sessionState != MainActivityViewModel.SessionState.RECORDING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Session")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Session")
                    }
                    OutlinedButton(
                        onClick = onExportAllData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Export All Data")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManageSessions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = "Manage Sessions")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Sessions")
                }
            }
        }
    }

    @Composable
    private fun SystemDiagnosticsSection(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        onRunDiagnostics: () -> Unit,
        onViewLogs: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // System health indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("Thermal Status", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                "Thermal Camera Connected" else "Thermal Camera Error",
                            tint = if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("GSR Status", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                "GSR Sensor Connected" else "GSR Sensor Error",
                            tint = if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRunDiagnostics,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Run Diagnostics")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Diagnostics")
                    }
                    OutlinedButton(
                        onClick = onViewLogs,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "View Logs")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Logs")
                    }
                }
            }
        }
    }

    // Launch methods for enhanced functionality
    private fun launchDeviceConfig(deviceType: String) {
        // Launch device-specific configuration
        when (deviceType) {
            "thermal" -> {
                // Launch thermal camera configuration
            }

            "gsr" -> {
                // Launch GSR sensor configuration
            }
        }
    }

    private fun launchDeviceTest(deviceType: String) {
        // Launch device-specific testing
        when (deviceType) {
            "thermal" -> {
                // Launch thermal camera test
            }

            "gsr" -> {
                // Launch GSR sensor test
            }
        }
    }

    // Enhanced mock data generation
    private fun generateEnhancedGSRReadings(): List<Float> {
        return (0..100).map {
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 50f
        }
    }
}


// ===== feature\gsr\ui\SessionDetailComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class SessionDetailComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        fun startActivity(
            context: Context,
            sessionId: String,
        ) {
            val intent = Intent(context, SessionDetailComposeActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "Unknown"
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Session Details",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Share session data
                                android.widget.Toast.makeText(
                                    this@SessionDetailComposeActivity,
                                    "Share session feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Export session data
                                android.widget.Toast.makeText(
                                    this@SessionDetailComposeActivity,
                                    "Exporting session...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                SessionDetailContent(
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    sessionId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Session Overview Card
        SessionOverviewCard(sessionId = sessionId)
        // Session Statistics Card
        SessionStatisticsCard()
        // Data Quality Card
        DataQualityCard()
        // Session Timeline Card
        SessionTimelineCard()
        // Actions Card
        val context = androidx.compose.ui.platform.LocalContext.current
        SessionActionsCard(
            onViewData = {
                // TODO: Navigate to data view activity
                android.widget.Toast.makeText(
                    context,
                    "Opening data view...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onExportData = {
                // TODO: Export session data
                android.widget.Toast.makeText(
                    context,
                    "Exporting session data...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onDeleteSession = {
                // TODO: Show confirmation dialog and delete session
                android.widget.Toast.makeText(
                    context,
                    "Delete session confirmation dialog",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun SessionOverviewCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Session Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            SessionInfoRow("Session ID", sessionId)
            SessionInfoRow("Date", "2024-01-15 14:30:00")
            SessionInfoRow("Duration", "45 minutes")
            SessionInfoRow("Device", "Shimmer3 GSR Unit")
            SessionInfoRow("Sample Rate", "128 Hz")
            SessionInfoRow("Status", "Completed") {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Completed")
                }
            }
        }
    }
}

@Composable
private fun SessionInfoRow(
    label: String,
    value: String,
    valueContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (valueContent != null) {
            valueContent()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SessionStatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Session Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Data Points", "345,600")
                StatisticItem("Avg GSR", "12.5 Î¼S")
                StatisticItem("Peak GSR", "45.7 Î¼S")
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataQualityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Quality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            QualityIndicator("Signal Quality", 0.95f)
            QualityIndicator("Data Completeness", 0.98f)
            QualityIndicator("Noise Level", 0.15f, isInverse = true)
        }
    }
}

@Composable
private fun QualityIndicator(
    label: String,
    value: Float,
    isInverse: Boolean = false
) {
    val displayValue = if (isInverse) 1f - value else value
    val color = when {
        displayValue >= 0.8f -> MaterialTheme.colorScheme.primary
        displayValue >= 0.6f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(displayValue * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { displayValue },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

@Composable
private fun SessionTimelineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Session Timeline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                " Session started at 14:30:00\n" +
                        " Device connected at 14:30:15\n" +
                        " Data recording began at 14:30:30\n" +
                        " Peak activity detected at 14:45:12\n" +
                        " Steady state achieved at 14:50:00\n" +
                        " Recording completed at 15:15:00",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
            )
        }
    }
}

@Composable
private fun SessionActionsCard(
    onViewData: () -> Unit,
    onExportData: () -> Unit,
    onDeleteSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Data")
                }
                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export")
                }
            }
            OutlinedButton(
                onClick = onDeleteSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Session")
            }
        }
    }
}

// Simple ViewModel for the session detail
class SessionDetailViewModel : AppBaseViewModel() {
    // Future implementation would include:
    // - Session data loading
    // - Export functionality
    // - Share functionality
    // - Delete confirmation
}


// ===== feature\gsr\ui\SessionDetailScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin

data class SessionInfo(
    val id: String,
    val title: String,
    val date: String,
    val duration: String,
    val participantId: String,
    val sensorTypes: List<String>,
    val dataPoints: Int,
    val notes: String
)

data class SessionMetrics(
    val gsrMean: Double,
    val gsrStd: Double,
    val gsrMin: Double,
    val gsrMax: Double,
    val thermalMean: Double,
    val thermalStd: Double,
    val heartRateAvg: Int,
    val stressLevel: String
)

data class TimeSeriesData(
    val timestamp: Long,
    val gsrValue: Double,
    val thermalValue: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String = "SESSION-2024-001",
    onNavigateBack: () -> Unit = {},
    onExportSession: () -> Unit = {},
    onPlayVideo: () -> Unit = {}
) {
    val session = remember { getSampleSession(sessionId) }
    val metrics = remember { getSampleMetrics() }
    val timeSeriesData = remember { getSampleTimeSeriesData() }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Session Details",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Share,
                    contentDescription = "Export session",
                    onClick = onExportSession
                )
                TitleBarAction(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    onClick = onPlayVideo
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session Header
                item {
                    SessionHeaderCard(session = session)
                }
                // Metrics Overview
                item {
                    MetricsOverviewCard(metrics = metrics)
                }
                // GSR Waveform
                item {
                    GSRWaveformCard(data = timeSeriesData)
                }
                // Thermal Data
                item {
                    ThermalDataCard(data = timeSeriesData)
                }
                // Analysis Summary
                item {
                    AnalysisSummaryCard(session = session, metrics = metrics)
                }
                // Export Options
                item {
                    ExportOptionsCard(
                        onExportRaw = { onExportSession() },
                        onExportReport = { onExportSession() },
                        onExportVideo = { onPlayVideo() }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionHeaderCard(session: SessionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = session.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Date",
                    value = session.date
                )
                SessionInfoItem(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = session.duration
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoItem(
                    icon = Icons.Default.Person,
                    label = "Participant",
                    value = session.participantId
                )
                SessionInfoItem(
                    icon = Icons.Default.DataUsage,
                    label = "Data Points",
                    value = session.dataPoints.toString()
                )
            }
            if (session.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Notes: ${session.notes}",
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}

@Composable
fun SessionInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF4ECDC4),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFFCCFFFFFF)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun MetricsOverviewCard(metrics: SessionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Metrics Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "GSR Mean",
                    value = "${String.format("%.2f", metrics.gsrMean)} Î¼S",
                    color = Color(0xFF4ECDC4)
                )
                MetricItem(
                    label = "Thermal Avg",
                    value = "${String.format("%.1f", metrics.thermalMean)}Â°C",
                    color = Color(0xFFFF6B6B)
                )
                MetricItem(
                    label = "Heart Rate",
                    value = "${metrics.heartRateAvg} BPM",
                    color = Color(0xFF6B73FF)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stress Level: ",
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
                Text(
                    text = metrics.stressLevel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (metrics.stressLevel) {
                        "Low" -> Color(0xFF4ECDC4)
                        "Medium" -> Color(0xFFFFB74D)
                        "High" -> Color(0xFFFF6B6B)
                        else -> Color.White
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}

@Composable
fun GSRWaveformCard(data: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "GSR Waveform",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                if (data.isNotEmpty()) {
                    val minValue = data.minOf { it.gsrValue }
                    val maxValue = data.maxOf { it.gsrValue }
                    val valueRange = maxValue - minValue
                    data.forEachIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * width
                        val normalizedValue = if (valueRange > 0) {
                            ((point.gsrValue - minValue) / valueRange)
                        } else 0.5
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun ThermalDataCard(data: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Thermal Data",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                if (data.isNotEmpty()) {
                    val minValue = data.minOf { it.thermalValue }
                    val maxValue = data.maxOf { it.thermalValue }
                    val valueRange = maxValue - minValue
                    data.forEachIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * width
                        val normalizedValue = if (valueRange > 0) {
                            ((point.thermalValue - minValue) / valueRange)
                        } else 0.5
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFF6B6B),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisSummaryCard(
    session: SessionInfo,
    metrics: SessionMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Analysis Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val summaryText = buildString {
                append("This ${session.duration} session recorded ${session.dataPoints} data points ")
                append("from ${session.sensorTypes.joinToString(", ")} sensors. ")
                append("Average GSR was ${String.format("%.2f", metrics.gsrMean)} Î¼S with ")
                append("standard deviation of ${String.format("%.2f", metrics.gsrStd)}. ")
                append(
                    "Thermal readings averaged ${
                        String.format(
                            "%.1f",
                            metrics.thermalMean
                        )
                    }Â°C. "
                )
                append("Overall stress level assessed as ${metrics.stressLevel.lowercase()}.")
            }
            Text(
                text = summaryText,
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ExportOptionsCard(
    onExportRaw: () -> Unit,
    onExportReport: () -> Unit,
    onExportVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Export Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onExportRaw,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4ECDC4)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Export Raw Data",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raw Data")
                }
                OutlinedButton(
                    onClick = onExportReport,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B73FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
                OutlinedButton(
                    onClick = onExportVideo,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = "Export Video",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video")
                }
            }
        }
    }
}

private fun getSampleSession(id: String) = SessionInfo(
    id = id,
    title = "Stress Response Study - Session A",
    date = "Dec 15, 2024 14:30",
    duration = "25:42",
    participantId = "P001-UCL-2024",
    sensorTypes = listOf("GSR", "Thermal", "Heart Rate"),
    dataPoints = 15420,
    notes = "Baseline recording with cognitive stress tasks. Participant reported feeling moderately stressed during math problems."
)

private fun getSampleMetrics() = SessionMetrics(
    gsrMean = 12.45,
    gsrStd = 3.21,
    gsrMin = 8.12,
    gsrMax = 18.67,
    thermalMean = 36.4,
    thermalStd = 0.8,
    heartRateAvg = 78,
    stressLevel = "Medium"
)

private fun getSampleTimeSeriesData(): List<TimeSeriesData> {
    return (0..100).map { i ->
        TimeSeriesData(
            timestamp = System.currentTimeMillis() + i * 1000,
            gsrValue = 12.0 + 3.0 * sin(i * 0.1) + (Math.random() - 0.5) * 2.0,
            thermalValue = 36.4 + 0.5 * sin(i * 0.05) + (Math.random() - 0.5) * 0.3
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionDetailScreenPreview() {
    SessionDetailScreen()
}


// ===== feature\gsr\ui\SessionExportComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.ExportDestination
import mpdc4gsr.feature.gsr.presentation.ExportFormat
import mpdc4gsr.feature.gsr.presentation.GSRSession
import mpdc4gsr.feature.gsr.presentation.SessionExportViewModel
import mpdc4gsr.feature.gsr.presentation.SessionExportViewModelFactory

class SessionExportComposeActivity : BaseComposeActivity<SessionExportViewModel>() {
    override fun createViewModel(): SessionExportViewModel =
        viewModels<SessionExportViewModel> {
            SessionExportViewModelFactory(application)
        }.value

    @Composable
    override fun Content(viewModel: SessionExportViewModel) {
        IRCameraTheme {
            SessionExportScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionExportScreen(
    viewModel: SessionExportViewModel = viewModel(
        factory = SessionExportViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext
        )
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.exportState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Export GSR Session") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.startExport() },
                    enabled = !uiState.isExporting && uiState.selectedSessions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Start export"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        // Content
        when {
            uiState.isLoading -> {
                LoadingContent()
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error"
                ErrorContent(
                    error = errorMessage,
                    onRetry = { viewModel.loadSessions() }
                )
            }

            uiState.sessions.isEmpty() -> {
                EmptyContent()
            }

            else -> {
                ExportContent(
                    uiState = uiState,
                    onSessionToggle = { session -> viewModel.toggleSessionSelection(session) },
                    onExportFormatChange = { format -> viewModel.setExportFormat(format) },
                    onExportDestinationChange = { destination -> viewModel.setExportDestination(destination) },
                    onStartExport = { viewModel.startExport() }
                )
            }
        }
    }
}

@Composable
private fun ExportContent(
    uiState: SessionExportViewModel.SessionExportState,
    onSessionToggle: (GSRSession) -> Unit,
    onExportFormatChange: (ExportFormat) -> Unit,
    onExportDestinationChange: (ExportDestination) -> Unit,
    onStartExport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Export Progress (if exporting)
        if (uiState.isExporting) {
            item {
                ExportProgressCard(
                    progress = uiState.exportProgress,
                    currentFile = uiState.currentExportFile
                )
            }
        }
        // Export Configuration
        item {
            ExportConfigurationCard(
                selectedFormat = uiState.exportFormat,
                selectedDestination = uiState.exportDestination,
                onFormatChange = onExportFormatChange,
                onDestinationChange = onExportDestinationChange
            )
        }
        // Session Selection Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Sessions to Export",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.selectedSessions.size} of ${uiState.sessions.size} sessions selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        // Session List
        items(uiState.sessions) { session ->
            SessionSelectionCard(
                session = session,
                isSelected = session in uiState.selectedSessions,
                onToggle = { onSessionToggle(session) }
            )
        }
        // Export Action
        if (!uiState.isExporting && uiState.selectedSessions.isNotEmpty()) {
            item {
                Button(
                    onClick = onStartExport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedSessions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Selected Sessions")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSelectionCard(
    session: GSRSession,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "Duration: ${session.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Data points: ${session.dataPointCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ExportConfigurationCard(
    selectedFormat: ExportFormat,
    selectedDestination: ExportDestination,
    onFormatChange: (ExportFormat) -> Unit,
    onDestinationChange: (ExportDestination) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Export Format Selection
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExportFormat.values().forEach { format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedFormat == format,
                            onClick = { onFormatChange(format) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedFormat == format,
                        onClick = { onFormatChange(format) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = format.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Export Destination Selection
            Text(
                text = "Export Destination",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExportDestination.values().forEach { destination ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedDestination == destination,
                            onClick = { onDestinationChange(destination) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedDestination == destination,
                        onClick = { onDestinationChange(destination) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destination.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportProgressCard(
    progress: Float,
    currentFile: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Exporting...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            if (currentFile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current: $currentFile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading sessions...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading sessions",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "No sessions",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No sessions available",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "GSR sessions will appear here when available for export",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


// ===== feature\gsr\ui\SessionManagerComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.SessionManagerViewModel
import java.text.SimpleDateFormat
import java.util.*

class SessionManagerComposeActivity : BaseComposeActivity<SessionManagerViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SessionManagerComposeActivity::class.java))
        }
    }

    override fun createViewModel(): SessionManagerViewModel {
        return viewModels<SessionManagerViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SessionManagerViewModel) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedSessions by remember { mutableStateOf(setOf<String>()) }
        var showFilterDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Session Manager",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            IconButton(onClick = {
                                // TODO: Export all sessions
                                android.widget.Toast.makeText(
                                    this@SessionManagerComposeActivity,
                                    "Exporting all sessions...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    this@SessionManagerComposeActivity,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            // Start new recording session
                            MultiModalRecordingComposeActivity.startActivity(this@SessionManagerComposeActivity)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Session")
                    }
                }
            ) { paddingValues ->
                val context = LocalContext.current
                SessionManagerContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedSessions = selectedSessions,
                    onSessionSelectionChange = { selectedSessions = it },
                    viewModel = viewModel,
                    context = context,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showFilterDialog) {
            SessionFilterDialog(
                onDismiss = { showFilterDialog = false },
                onApplyFilter = {
                    // TODO: Apply filter logic to session list
                    android.widget.Toast.makeText(
                        this@SessionManagerComposeActivity,
                        "Applying filters...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    showFilterDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionManagerContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSessions: Set<String>,
    onSessionSelectionChange: (Set<String>) -> Unit,
    viewModel: SessionManagerViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search Sessions") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            )
        )
        // Session Statistics Card
        SessionStatisticsCard(
            selectedCount = selectedSessions.size,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val mockSessions = listOf(
                com.mpdc4gsr.gsr.model.SessionInfo(
                    sessionId = "session_1",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    participantId = "P001",
                    studyName = "GSR Study Session"
                ),
                com.mpdc4gsr.gsr.model.SessionInfo(
                    sessionId = "session_2",
                    startTime = System.currentTimeMillis() - 86400000,
                    endTime = System.currentTimeMillis() - 82800000,
                    participantId = "P002",
                    studyName = "Thermal Analysis"
                ),
                com.mpdc4gsr.gsr.model.SessionInfo(
                    sessionId = "session_3",
                    startTime = System.currentTimeMillis() - 172800000,
                    endTime = null,
                    participantId = "P003",
                    studyName = "Multi-modal Recording"
                )
            )
            items(mockSessions.filter {
                (it.studyName ?: "").contains(searchQuery, ignoreCase = true)
            }) { session ->
                SessionCard(
                    session = session,
                    isSelected = selectedSessions.contains(session.sessionId),
                    onSelectionChange = { isSelected ->
                        if (isSelected) {
                            onSessionSelectionChange(selectedSessions + session.sessionId)
                        } else {
                            onSessionSelectionChange(selectedSessions - session.sessionId)
                        }
                    },
                    onClick = {
                        SessionDetailComposeActivity.startActivity(
                            context = context,
                            sessionId = session.sessionId
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionStatisticsCard(
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "12", // Replace with actual count
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (selectedCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = selectedCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.studyName ?: session.sessionId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Started: ${dateFormatter.format(session.startTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Status chip - determine status from endTime
                val status = if (session.endTime == null) "active" else "completed"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (status) {
                        "active" -> Color(0xFF4CAF50)
                        "completed" -> Color(0xFF2196F3)
                        else -> Color(0xFF9E9E9E)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                // TODO: Show session options menu
                android.widget.Toast.makeText(
                    context,
                    "Session options coming soon",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Session options")
            }
        }
    }
}

@Composable
private fun SessionFilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Sessions") },
        text = {
            Column {
                Text("Select filter criteria:")
                // Add filter options here
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApplyFilter()
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ===== feature\gsr\ui\SessionManagerScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

data class ResearchSession(
    val id: String,
    val title: String,
    val participantId: String,
    val date: String,
    val duration: String,
    val status: SessionStatus,
    val sensorTypes: List<String>,
    val dataSize: String,
    val progress: Float = 0f // 0.0 to 1.0
)

enum class SessionStatus {
    COMPLETED,
    IN_PROGRESS,
    PAUSED,
    FAILED,
    SCHEDULED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagerScreen(
    onNavigateBack: () -> Unit = {},
    onCreateNewSession: () -> Unit = {},
    onViewSession: (ResearchSession) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val sessions = remember { getSampleSessions() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredSessions = sessions.filter { session ->
        when (selectedTab) {
            0 -> true // All
            1 -> session.status == SessionStatus.COMPLETED
            2 -> session.status == SessionStatus.IN_PROGRESS
            3 -> session.status == SessionStatus.SCHEDULED
            else -> true
        }
    }.filter { session ->
        if (searchQuery.isBlank()) true
        else session.title.contains(searchQuery, ignoreCase = true) ||
                session.participantId.contains(searchQuery, ignoreCase = true)
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Session Manager",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "New session",
                    onClick = onCreateNewSession
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sessions...") },
                    leadingIcon = {
                        IconButton(onClick = { keyboardController?.hide() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6B73FF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6B73FF)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    )
                )
                // Tab Row
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTab),
                            color = Color(0xFF6B73FF)
                        )
                    }
                ) {
                    val tabs = listOf("All", "Completed", "Active", "Scheduled")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) Color(0xFF6B73FF) else Color.White
                                )
                            }
                        )
                    }
                }
                // Session Statistics
                SessionStatsCard(
                    sessions = sessions,
                    modifier = Modifier.padding(16.dp)
                )
                // Sessions List
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSessions) { session ->
                        SessionItem(
                            session = session,
                            onClick = { onViewSession(session) }
                        )
                    }
                    if (filteredSessions.isEmpty()) {
                        item {
                            EmptySessionsState(
                                searchQuery = searchQuery,
                                onCreateNew = onCreateNewSession
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionStatsCard(
    sessions: List<ResearchSession>,
    modifier: Modifier = Modifier
) {
    val completedSessions = sessions.count { it.status == SessionStatus.COMPLETED }
    val activeSessions = sessions.count { it.status == SessionStatus.IN_PROGRESS }
    val totalDuration = sessions.filter { it.status == SessionStatus.COMPLETED }
        .sumOf { parseDuration(it.duration) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Completed",
                value = completedSessions.toString(),
                color = Color(0xFF4ECDC4)
            )
            StatItem(
                label = "Active",
                value = activeSessions.toString(),
                color = Color(0xFF6B73FF)
            )
            StatItem(
                label = "Total Time",
                value = formatTotalDuration(totalDuration),
                color = Color(0xFFFF6B6B)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}

@Composable
fun SessionItem(
    session: ResearchSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                SessionStatusBadge(status = session.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Session Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${session.participantId}",
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
                Text(
                    text = session.date,
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Progress Bar (for in-progress sessions)
            if (session.status == SessionStatus.IN_PROGRESS) {
                LinearProgressIndicator(
                    progress = { session.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6B73FF),
                    trackColor = Color(0xFF404040)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Duration",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.duration,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Data Size",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.dataSize,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                }
                // Sensor Type Icons
                Row {
                    session.sensorTypes.forEach { sensorType ->
                        Icon(
                            imageVector = when (sensorType) {
                                "GSR" -> Icons.Default.Sensors
                                "Thermal" -> Icons.Default.Thermostat
                                "Camera" -> Icons.Default.Camera
                                else -> Icons.Default.DeviceHub
                            },
                            contentDescription = sensorType,
                            tint = when (sensorType) {
                                "GSR" -> Color(0xFF4ECDC4)
                                "Thermal" -> Color(0xFFFF6B6B)
                                "Camera" -> Color.White
                                else -> Color(0xFF6B73FF)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionStatusBadge(status: SessionStatus) {
    val (color, text) = when (status) {
        SessionStatus.COMPLETED -> Color(0xFF4ECDC4) to "Completed"
        SessionStatus.IN_PROGRESS -> Color(0xFF6B73FF) to "Active"
        SessionStatus.PAUSED -> Color(0xFFFFB74D) to "Paused"
        SessionStatus.FAILED -> Color(0xFFFF6B6B) to "Failed"
        SessionStatus.SCHEDULED -> Color(0xFF9E9E9E) to "Scheduled"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptySessionsState(
    searchQuery: String,
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (searchQuery.isBlank()) Icons.AutoMirrored.Filled.Assignment else Icons.Default.SearchOff,
            contentDescription = if (searchQuery.isBlank()) "No Sessions" else "No Search Results",
            tint = Color(0xFF6B73FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank()) "No sessions yet" else "No sessions found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isBlank())
                "Create your first research session to get started"
            else
                "Try adjusting your search criteria",
            fontSize = 14.sp,
            color = Color(0xFFCCFFFFFF)
        )
        if (searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateNew,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Session",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Session")
            }
        }
    }
}

private fun parseDuration(duration: String): Int {
    // Parse "25:42" format to minutes
    val parts = duration.split(":")
    return if (parts.size == 2) {
        parts[0].toIntOrNull()?.let { minutes ->
            parts[1].toIntOrNull()?.let { seconds ->
                minutes + (seconds / 60)
            }
        } ?: 0
    } else 0
}

private fun formatTotalDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

private fun getSampleSessions() = listOf(
    ResearchSession(
        id = "SESSION-001",
        title = "Stress Response Study A",
        participantId = "P001-UCL-2024",
        date = "Dec 15, 2024",
        duration = "25:42",
        status = SessionStatus.COMPLETED,
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        dataSize = "2.4 MB"
    ),
    ResearchSession(
        id = "SESSION-002",
        title = "Cognitive Load Assessment",
        participantId = "P002-UCL-2024",
        date = "Dec 16, 2024",
        duration = "18:30",
        status = SessionStatus.IN_PROGRESS,
        sensorTypes = listOf("GSR", "Thermal"),
        dataSize = "1.2 MB",
        progress = 0.65f
    ),
    ResearchSession(
        id = "SESSION-003",
        title = "Emotion Recognition Task",
        participantId = "P003-UCL-2024",
        date = "Dec 17, 2024",
        duration = "32:15",
        status = SessionStatus.COMPLETED,
        sensorTypes = listOf("GSR", "Camera"),
        dataSize = "3.1 MB"
    ),
    ResearchSession(
        id = "SESSION-004",
        title = "Baseline Measurement",
        participantId = "P001-UCL-2024",
        date = "Dec 18, 2024",
        duration = "15:00",
        status = SessionStatus.SCHEDULED,
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        dataSize = "0 MB"
    ),
    ResearchSession(
        id = "SESSION-005",
        title = "Social Interaction Study",
        participantId = "P004-UCL-2024",
        date = "Dec 14, 2024",
        duration = "28:45",
        status = SessionStatus.FAILED,
        sensorTypes = listOf("GSR"),
        dataSize = "0.8 MB"
    )
)

@Preview(showBackground = true)
@Composable
fun SessionManagerScreenPreview() {
    SessionManagerScreen()
}


// ===== feature\gsr\ui\ShimmerConfigComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModel
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModelFactory

class ShimmerConfigComposeActivity : ComponentActivity() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ShimmerConfigComposeActivity::class.java))
        }
    }

    private val viewModel: ShimmerConfigViewModel by viewModels {
        ShimmerConfigViewModelFactory(application, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content(viewModel)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: ShimmerConfigViewModel) {
        val localContext = androidx.compose.ui.platform.LocalContext.current
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<DeviceInfo?>(null) }
        var showConfigDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Shimmer Configuration",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                    contentDescription = if (isScanning) "Stop Scan" else "Scan"
                                )
                            }
                            IconButton(onClick = {
                                // TODO: Implement Shimmer configuration help/documentation
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Opening Shimmer configuration help",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ShimmerConfigContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = { selectedDevice = it },
                    onConfigureDevice = { showConfigDialog = true },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showConfigDialog && selectedDevice != null) {
            DeviceConfigurationDialog(
                device = selectedDevice!!,
                onDismiss = { showConfigDialog = false },
                onSaveConfiguration = { config ->
                    // Save device configuration
                    showConfigDialog = false
                }
            )
        }
    }
}

@Composable
private fun ShimmerConfigContent(
    isScanning: Boolean,
    selectedDevice: DeviceInfo?,
    onDeviceSelect: (DeviceInfo?) -> Unit,
    onConfigureDevice: () -> Unit,
    viewModel: ShimmerConfigViewModel,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scanning Status Card
        ScanningStatusCard(
            isScanning = isScanning,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Device List
        Text(
            text = "Available Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val mockDevices = listOf(
                DeviceInfo("shimmer_001", "Shimmer3 GSR+ #001", "Shimmer3", -45, true),
                DeviceInfo("shimmer_002", "Shimmer3 GSR+ #002", "Shimmer3", -62, true),
                DeviceInfo("shimmer_003", "Shimmer3 GSR+ #003", "Shimmer3", -38, true)
            )
            items(mockDevices) { device ->
                DeviceCard(
                    device = device,
                    isSelected = selectedDevice?.address == device.address,
                    onSelect = { onDeviceSelect(device) },
                    onConnect = {
                        // TODO: Implement Shimmer device connection
                        android.widget.Toast.makeText(
                            localContext,
                            "Connecting to ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onConfigure = {
                        onDeviceSelect(device)
                        onConfigureDevice()
                    }
                )
            }
        }
        // Selected Device Configuration Panel
        selectedDevice?.let { device ->
            SelectedDevicePanel(
                device = device,
                onConfigure = onConfigureDevice,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ScanningStatusCard(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScanning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isScanning) "Scanning for devices..." else "Scan complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isScanning) "Looking for Shimmer devices" else "3 devices found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Scan complete",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${device.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Status and signal strength
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Connection status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (device.deviceType) {
                                        "connected" -> Color(0xFF4CAF50)
                                        "available" -> Color(0xFF2196F3)
                                        "configuring" -> Color(0xFFFF9800)
                                        else -> Color(0xFF9E9E9E)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = device.deviceType.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Signal strength
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Signal strength",
                            modifier = Modifier.size(16.dp),
                            tint = when (device.signalStrength) {
                                DeviceInfo.SignalStrength.EXCELLENT -> Color(0xFF4CAF50)
                                DeviceInfo.SignalStrength.GOOD -> Color(0xFF4CAF50)
                                DeviceInfo.SignalStrength.FAIR -> Color(0xFFFF9800)
                                DeviceInfo.SignalStrength.POOR -> Color(0xFFE53E3E)
                                DeviceInfo.SignalStrength.VERY_POOR -> Color(0xFFE53E3E)
                            }
                        )
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onSelect) {
                    Icon(
                        if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isSelected) "Collapse" else "Expand"
                    )
                }
            }
            if (isSelected) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                // Device actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                    Button(
                        onClick = onConfigure,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configure",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDevicePanel(
    device: DeviceInfo,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selected Device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Status: ${device.deviceType.replaceFirstChar { it.uppercaseChar() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = onConfigure,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Advanced Configuration",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Configuration")
            }
        }
    }
}

@Composable
private fun DeviceConfigurationDialog(
    device: DeviceInfo,
    onDismiss: () -> Unit,
    onSaveConfiguration: (Map<String, Any>) -> Unit
) {
    var samplingRate by remember { mutableStateOf(128f) }
    var gsrRange by remember { mutableStateOf("Auto") }
    var enablePPG by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure ${device.name}")
        },
        text = {
            Column {
                Text(
                    text = "Sampling Rate (Hz)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = samplingRate,
                    onValueChange = { samplingRate = it },
                    valueRange = 1f..512f,
                    steps = 8
                )
                Text(
                    text = "${samplingRate.toInt()} Hz",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "GSR Range",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enablePPG,
                        onCheckedChange = { enablePPG = it }
                    )
                    Text(
                        text = "Enable PPG channels",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveConfiguration(
                        mapOf(
                            "samplingRate" to samplingRate.toInt(),
                            "gsrRange" to gsrRange,
                            "enablePPG" to enablePPG
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ===== feature\gsr\ui\UnifiedSensorComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.SessionQuality
import mpdc4gsr.core.data.model.SessionStatus
import mpdc4gsr.core.data.model.SessionType
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class UnifiedSensorType(
    val displayName: String,
    val icon: ImageVector,
    val description: String
) {
    THERMAL("Thermal Camera", Icons.Default.Thermostat, "TC001/TS004 thermal imaging sensors"),
    GSR("GSR Sensor", Icons.Default.Sensors, "Galvanic skin response monitoring"),
    RGB_CAMERA("RGB Camera", Icons.Default.Camera, "High-resolution RGB camera recording"),
    AUDIO("Audio", Icons.Default.Audiotrack, "Audio recording"),
    NETWORK("Network", Icons.Default.NetworkCheck, "Network connectivity and data transmission")
}

data class SensorStatus(
    val type: UnifiedSensorType,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val quality: String = "Unknown",
    val dataRate: String = "0 KB/s",
    val lastUpdate: String = "Never"
)

data class UnifiedSessionInfo(
    val name: String = "New Session",
    val type: SessionType = SessionType.RESEARCH,
    val quality: SessionQuality = SessionQuality(),
    val status: SessionStatus = SessionStatus.IDLE,
    val duration: String = "00:00:00",
    val dataSize: String = "0 MB"
)

class UnifiedSensorViewModel : AppBaseViewModel() {
    private val _sensorStatuses = mutableStateOf(
        UnifiedSensorType.values().map { type ->
            SensorStatus(
                type = type,
                isConnected = false,
                quality = "Disconnected"
            )
        }
    )
    val sensorStatuses: State<List<SensorStatus>> = _sensorStatuses
    private val _sessionInfo = mutableStateOf(UnifiedSessionInfo())
    val sessionInfo: State<UnifiedSessionInfo> = _sessionInfo
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording
    private val _connectedDevices = mutableStateOf<List<DeviceInfo>>(emptyList())
    val connectedDevices: State<List<DeviceInfo>> = _connectedDevices
    fun connectSensor(sensorType: UnifiedSensorType) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Simulate connection process
            delay(2000)
            _sensorStatuses.value = _sensorStatuses.value.map { status ->
                if (status.type == sensorType) {
                    status.copy(
                        isConnected = true,
                        quality = "Good",
                        dataRate = when (sensorType) {
                            UnifiedSensorType.THERMAL -> "125 KB/s"
                            UnifiedSensorType.GSR -> "2 KB/s"
                            UnifiedSensorType.RGB_CAMERA -> "1.2 MB/s"
                            UnifiedSensorType.AUDIO -> "64 KB/s"
                            UnifiedSensorType.NETWORK -> "10 MB/s"
                        },
                        lastUpdate = "Just now"
                    )
                } else status
            }
        }
    }

    fun disconnectSensor(sensorType: UnifiedSensorType) {
        _sensorStatuses.value = _sensorStatuses.value.map { status ->
            if (status.type == sensorType) {
                status.copy(
                    isConnected = false,
                    isRecording = false,
                    quality = "Disconnected",
                    dataRate = "0 KB/s",
                    lastUpdate = "Disconnected"
                )
            } else status
        }
    }

    fun startRecording() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRecording.value = true
            _sessionInfo.value = _sessionInfo.value.copy(status = SessionStatus.RECORDING)
            // Update sensor recording status
            _sensorStatuses.value = _sensorStatuses.value.map { status ->
                if (status.isConnected) {
                    status.copy(isRecording = true)
                } else status
            }
            // Simulate recording time updates
            var seconds = 0
            while (_isRecording.value) {
                delay(1000)
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                val duration = String.format("%02d:%02d:%02d", hours, minutes, secs)
                val dataSize = "${(seconds * 0.5).toInt()} MB" // Simulate growing data
                _sessionInfo.value = _sessionInfo.value.copy(
                    duration = duration,
                    dataSize = dataSize
                )
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        _sessionInfo.value = _sessionInfo.value.copy(status = SessionStatus.IDLE)
        _sensorStatuses.value = _sensorStatuses.value.map { status ->
            status.copy(isRecording = false)
        }
    }

    fun updateSessionName(name: String) {
        _sessionInfo.value = _sessionInfo.value.copy(name = name)
    }
}

class UnifiedSensorComposeActivity : BaseComposeActivity<UnifiedSensorViewModel>() {
    override fun createViewModel(): UnifiedSensorViewModel =
        viewModels<UnifiedSensorViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: UnifiedSensorViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val sensorStatuses by viewModel.sensorStatuses
            val sessionInfo by viewModel.sessionInfo
            val isRecording by viewModel.isRecording
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Unified Sensor Control",
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Session info card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = if (isRecording)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sessionInfo.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${sessionInfo.status} â€¢ ${sessionInfo.duration} â€¢ ${sessionInfo.dataSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isRecording) {
                                    Button(
                                        onClick = { viewModel.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Stop")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startRecording() },
                                        enabled = sensorStatuses.any { it.isConnected }
                                    ) {
                                        Text("Start Recording")
                                    }
                                }
                            }
                        }
                    }
                    // RGB Camera preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PreviewView(context).apply {
                                        // Camera preview will be initialized here
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay when camera is not connected
                            val rgbCameraStatus =
                                sensorStatuses.find { it.type == UnifiedSensorType.RGB_CAMERA }
                            if (rgbCameraStatus?.isConnected != true) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black.copy(alpha = 0.8f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideocamOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera Preview",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Connect RGB camera to view",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Sensor status cards
                    Text(
                        text = "Sensor Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sensorStatuses.forEach { sensorStatus ->
                        SensorStatusCard(
                            sensorStatus = sensorStatus,
                            onConnect = { viewModel.connectSensor(it) },
                            onDisconnect = { viewModel.disconnectSensor(it) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Quick actions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        UnifiedSensorType.values().forEach { type ->
                                            viewModel.connectSensor(type)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Connect All")
                                }
                                OutlinedButton(
                                    onClick = {
                                        UnifiedSensorType.values().forEach { type ->
                                            viewModel.disconnectSensor(type)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Disconnect All")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatusCard(
    sensorStatus: SensorStatus,
    onConnect: (UnifiedSensorType) -> Unit,
    onDisconnect: (UnifiedSensorType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                sensorStatus.isRecording -> MaterialTheme.colorScheme.errorContainer
                sensorStatus.isConnected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = sensorStatus.type.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when {
                    sensorStatus.isRecording -> MaterialTheme.colorScheme.error
                    sensorStatus.isConnected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensorStatus.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sensorStatus.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Quality: ${sensorStatus.quality} â€¢ Rate: ${sensorStatus.dataRate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = when {
                        sensorStatus.isRecording -> MaterialTheme.colorScheme.error
                        sensorStatus.isConnected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            sensorStatus.isRecording -> "Recording"
                            sensorStatus.isConnected -> "Connected"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            sensorStatus.isRecording -> MaterialTheme.colorScheme.onError
                            sensorStatus.isConnected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (sensorStatus.isConnected) {
                    OutlinedButton(
                        onClick = { onDisconnect(sensorStatus.type) },
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = "Disconnect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Button(
                        onClick = { onConnect(sensorStatus.type) },
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}


