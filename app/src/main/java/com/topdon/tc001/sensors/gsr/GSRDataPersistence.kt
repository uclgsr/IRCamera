package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.util.Log
import com.topdon.tc001.sensors.TimestampManager
import com.topdon.tc001.sensors.TimestampRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced GSR data persistence with proper timestamping and cross-sensor alignment
 * Provides research-grade CSV output with multiple timestamp formats for compatibility
 */
class GSRDataPersistence(
    private val context: Context,
    private val sessionId: String,
) {
    companion object {
        private const val TAG = "GSRDataPersistence"
        private const val BATCH_SIZE = 100
        private const val FLUSH_INTERVAL_MS = 1000L
    }

    private val dataQueue = ConcurrentLinkedQueue<GSRDataRecord>()
    private val writeMutex = Mutex()
    private val isWriting = AtomicBoolean(false)
    private val samplesWritten = AtomicLong(0)

    private var csvFile: File? = null
    private var csvWriter: FileWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Initialize data persistence for GSR recording session
     */
    suspend fun initialize(): Boolean {
        return try {
            val sessionDir = createSessionDirectory()
            csvFile = createCsvFile(sessionDir)
            csvWriter = FileWriter(csvFile!!, true)

            // Write CSV header with comprehensive timestamp information
            writeCsvHeader()

            Log.i(TAG, "GSR data persistence initialized for session: $sessionId")
            Log.i(TAG, "CSV file: ${csvFile!!.absolutePath}")

            // Start background batch writer
            startBatchWriter()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GSR data persistence", e)
            false
        }
    }

    /**
     * Create session directory with proper structure
     */
    private fun createSessionDirectory(): File {
        val baseDir = File(context.getExternalFilesDir(null), "GSR_Sessions")
        val sessionDir = File(baseDir, sessionId)

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        return sessionDir
    }

    /**
     * Create CSV file with timestamp-based naming
     */
    private fun createCsvFile(sessionDir: File): File {
        val timestamp = dateFormat.format(Date())
        val filename = "gsr_data_${sessionId}_$timestamp.csv"
        return File(sessionDir, filename)
    }

    /**
     * Write comprehensive CSV header with all timestamp formats
     */
    private suspend fun writeCsvHeader() {
        writeMutex.withLock {
            try {
                val header =
                    buildString {
                        // Timestamp columns (multiple formats for compatibility)
                        append(TimestampRecord.getCsvHeader())
                        append(",")

                        // GSR sensor data columns
                        append("gsr_raw_value,gsr_microsiemens,gsr_resistance_kohm,")

                        // PPG sensor data columns (if available)
                        append("ppg_raw_value,ppg_filtered,heart_rate_bpm,")

                        // Device and quality metrics
                        append("device_id,battery_level,signal_quality,")
                        append("sampling_rate_hz,packet_sequence,")

                        // Session and participant info
                        append("session_id,participant_id,recording_mode")
                    }

                csvWriter?.write(header)
                csvWriter?.write("\n")
                csvWriter?.flush()

                Log.i(TAG, "CSV header written with ${header.split(",").size} columns")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write CSV header", e)
                throw e
            }
        }
    }

    /**
     * Queue GSR data record for asynchronous writing
     */
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

    /**
     * Start background batch writer for efficient data persistence
     */
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

    /**
     * Write batch of data records to CSV file
     */
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
                    val csvLine = record.toCsvLine()
                    csvWriter?.write(csvLine)
                    csvWriter?.write("\n")
                }

                csvWriter?.flush()
                samplesWritten.addAndGet(batch.size.toLong())

                Log.d(TAG, "Wrote batch of ${batch.size} GSR samples. Total: ${samplesWritten.get()}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write GSR data batch", e)
            }
        }
    }

    /**
     * Start data persistence (called when recording starts)
     */
    fun startPersistence() {
        isWriting.set(true)
        TimestampManager.startSession()
        Log.i(TAG, "GSR data persistence started")
    }

    /**
     * Stop data persistence and flush remaining data
     */
    suspend fun stopPersistence() {
        isWriting.set(false)

        // Write remaining data
        while (dataQueue.isNotEmpty()) {
            writeBatch()
        }

        TimestampManager.endSession()

        Log.i(TAG, "GSR data persistence stopped. Total samples written: ${samplesWritten.get()}")
    }

    /**
     * Cleanup resources
     */
    suspend fun cleanup() {
        stopPersistence()

        writeMutex.withLock {
            try {
                csvWriter?.close()
                csvWriter = null
                Log.i(TAG, "GSR data persistence cleanup completed")
            } catch (e: IOException) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    /**
     * Get recording statistics
     */
    fun getStatistics(): GSRPersistenceStats {
        return GSRPersistenceStats(
            samplesWritten = samplesWritten.get(),
            pendingSamples = dataQueue.size,
            csvFilePath = csvFile?.absolutePath ?: "",
            sessionId = sessionId,
            isActive = isWriting.get(),
        )
    }
}

/**
 * GSR data record with comprehensive information for research analysis
 */
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
     * Convert record to CSV line format
     */
    fun toCsvLine(): String {
        return buildString {
            // Timestamp data
            append(timestamp.toCsvFormat())
            append(",")

            // GSR sensor data
            append("$gsrRawValue,$gsrMicrosiemens,$gsrResistanceKohm,")

            // PPG sensor data
            append("$ppgRawValue,$ppgFiltered,$heartRateBpm,")

            // Device and quality metrics
            append("$deviceId,$batteryLevel,$signalQuality,")
            append("$samplingRateHz,$packetSequence,")

            // Session information
            append("$sessionId,$participantId,$recordingMode")
        }
    }
}

/**
 * GSR sample data structure for sensor readings
 */
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

/**
 * Statistics for GSR data persistence monitoring
 */
data class GSRPersistenceStats(
    val samplesWritten: Long,
    val pendingSamples: Int,
    val csvFilePath: String,
    val sessionId: String,
    val isActive: Boolean,
)
