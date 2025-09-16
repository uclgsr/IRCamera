package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.util.Log
import com.topdon.tc001.sensors.TimestampManager
import com.topdon.tc001.sensors.TimestampRecord
import com.topdon.tc001.util.CSVBufferedWriter
import com.topdon.tc001.util.SessionDirectoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        private const val FLUSH_INTERVAL_MS = 500L // Reduced for better GSR data integrity
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
            
            // Create CSV headers for buffered writer
            val headers = createCsvHeaders()
            
            // Initialize buffered CSV writer with optimized settings for GSR data
            csvBufferedWriter = CSVBufferedWriter(
                outputFile = csvFile!!,
                headers = headers,
                bufferSize = 4096,  // 4KB buffer for GSR data
                flushIntervalMs = FLUSH_INTERVAL_MS
            )
            
            csvBufferedWriter?.startWithHeaders()

            Log.i(TAG, "GSR data persistence initialized for session: $sessionId")
            Log.i(TAG, "CSV file: ${csvFile!!.absolutePath}")

            startBatchWriter()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GSR data persistence", e)
            false
        }
    }

    private fun createSessionDirectory(): File {
        // Use standard directory structure for consistency
        val baseDir = File(context.getExternalFilesDir(null), "sessions")
        val sessionDir = File(baseDir, sessionId)
        val shimmerDir = File(sessionDir, "Shimmer")

        if (!shimmerDir.exists()) {
            shimmerDir.mkdirs()
        }

        return shimmerDir
    }

    private fun createCsvFile(sessionDir: File): File {
        // Use standard file name from SessionDirectoryManager
        return File(sessionDir, SessionDirectoryManager.SHIMMER_DATA_FILE)
    }

    private fun createCsvHeaders(): List<String> {
        return listOf(
            // Timestamp fields (matching TimestampRecord properties)
            "system_nanos", "elapsed_realtime_ms", "device_timestamp_ms", 
            "session_relative_ms", "synchronized_timestamp_ms",
            
            // GSR data fields
            "gsr_raw_value", "gsr_microsiemens", "gsr_resistance_kohm",
            
            // PPG data fields  
            "ppg_raw_value", "ppg_filtered", "heart_rate_bpm",
            
            // Device info fields
            "device_id", "battery_level", "signal_quality",
            "sampling_rate_hz", "packet_sequence",
            
            // Session info fields
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
                    Log.e(TAG, "Error in batch writer", e)
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
                // Use buffered writer for better performance
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
                Log.e(TAG, "Failed to write GSR data batch", e)
            }
        }
    }

    fun startPersistence() {
        isWriting.set(true)
        TimestampManager.startSession()
        Log.i(TAG, "GSR data persistence started")
    }

    suspend fun stopPersistence() {
        isWriting.set(false)

        // Write any remaining data
        while (dataQueue.isNotEmpty()) {
            writeBatch()
        }

        TimestampManager.endSession()

        Log.i(TAG, "GSR data persistence stopped. Total samples written: ${samplesWritten.get()}")
    }

    suspend fun cleanup() {
        stopPersistence()

        writeMutex.withLock {
            try {
                // Stop buffered writer properly
                csvBufferedWriter?.stop()
                csvBufferedWriter = null
                Log.i(TAG, "GSR data persistence cleanup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
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
    /**
     * Convert to CSV row format for BufferedDataWriter
     */
    fun toCsvRow(): List<Any> {
        return listOf(
            // Timestamp fields (using actual TimestampRecord properties)
            timestamp.systemNanos,
            timestamp.elapsedRealtimeMs,
            timestamp.deviceTimestampMs,
            timestamp.sessionRelativeMs,
            timestamp.synchronizedTimestampMs,
            
            // GSR data fields
            gsrRawValue,
            gsrMicrosiemens,
            gsrResistanceKohm,
            
            // PPG data fields  
            ppgRawValue,
            ppgFiltered,
            heartRateBpm,
            
            // Device info fields
            deviceId,
            batteryLevel,
            signalQuality,
            samplingRateHz,
            packetSequence,
            
            // Session info fields
            sessionId,
            participantId,
            recordingMode
        )
    }

    /**
     * Legacy CSV line format for backward compatibility
     */
    fun toCsvLine(): String {
        return buildString {
            // Timestamp fields
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
    val bufferStats: com.topdon.tc001.util.WriteStats? = null, // Enhanced with buffer statistics
) {
    val totalDataSizeBytes: Long
        get() = bufferStats?.bytesWritten ?: 0L
        
    val averageSampleSize: Double
        get() = if (samplesWritten > 0) totalDataSizeBytes.toDouble() / samplesWritten else 0.0
        
    val writePerformanceInfo: String
        get() = bufferStats?.let { stats ->
            "Queue: ${stats.queueSize}, Size: ${stats.formattedSize}, Avg: ${String.format("%.1f", averageSampleSize)} bytes/sample"
        } ?: "No buffer stats available"
}
