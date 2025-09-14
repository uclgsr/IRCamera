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

    suspend fun initialize(): Boolean {
        return try {
            val sessionDir = createSessionDirectory()
            csvFile = createCsvFile(sessionDir)
            csvWriter = FileWriter(csvFile!!, true)

            writeCsvHeader()

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
        val baseDir = File(context.getExternalFilesDir(null), "GSR_Sessions")
        val sessionDir = File(baseDir, sessionId)

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        return sessionDir
    }

    private fun createCsvFile(sessionDir: File): File {
        val timestamp = dateFormat.format(Date())
        val filename = "gsr_data_${sessionId}_$timestamp.csv"
        return File(sessionDir, filename)
    }

    private suspend fun writeCsvHeader() {
        writeMutex.withLock {
            try {
                val header =
                    buildString {

                        append(TimestampRecord.getCsvHeader())
                        append(",")

                        append("gsr_raw_value,gsr_microsiemens,gsr_resistance_kohm,")

                        append("ppg_raw_value,ppg_filtered,heart_rate_bpm,")

                        append("device_id,battery_level,signal_quality,")
                        append("sampling_rate_hz,packet_sequence,")

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
                batch.forEach { record ->
                    val csvLine = record.toCsvLine()
                    csvWriter?.write(csvLine)
                    csvWriter?.write("\n")
                }

                csvWriter?.flush()
                samplesWritten.addAndGet(batch.size.toLong())

                Log.d(
                    TAG,
                    "Wrote batch of ${batch.size} GSR samples. Total: ${samplesWritten.get()}"
                )
            } catch (e: IOException) {
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
                csvWriter?.close()
                csvWriter = null
                Log.i(TAG, "GSR data persistence cleanup completed")
            } catch (e: IOException) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

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
)
