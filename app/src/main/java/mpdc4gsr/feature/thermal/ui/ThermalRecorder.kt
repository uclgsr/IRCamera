package mpdc4gsr.feature.thermal.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.data.TimestampManager
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

class ThermalRecorder(private val context: Context) {

    companion object {
        private const val TAG = "ThermalRecorder"
        private const val CSV_HEADER =
            "timestamp_ns,frame_sequence,min_temp_c,avg_temp_c,max_temp_c,pixel_count"
    }

    private var isRecording = AtomicBoolean(false)
    private var frameSequence = AtomicLong(0)
    private var sessionDirectory: File? = null
    private var csvWriter: FileWriter? = null
    private var saveImages = false
    private var sessionMetadata: SessionMetadata? = null

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var thermalSettings: mpdc4gsr.feature.thermal.data.ThermalSettingsRepository.ThermalSettings? = null

    interface ThermalFrameCallback {
        fun onFrameReceived(frameData: ByteArray, width: Int, height: Int, timestamp: Long)
    }

    data class ThermalFrameStats(
        val timestampNs: Long,
        val frameSequence: Long,
        val minTemp: Float,
        val avgTemp: Float,
        val maxTemp: Float,
        val pixelCount: Int
    )

    private var frameListener: ThermalFrameListener? = null

    interface ThermalFrameListener {
        fun onFrameProcessed(stats: ThermalFrameStats)
        fun onError(error: String)
    }

    fun setFrameListener(listener: ThermalFrameListener) {
        this.frameListener = listener
    }

    suspend fun startRecording(
        sessionDir: String,
        sessionMetadata: SessionMetadata,
        saveImages: Boolean = false
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (isRecording.get()) {
                AppLogger.w(TAG, "Thermal recording already in progress")
                return@withContext false
            }

            try {
                val thermalSettingsRepo = mpdc4gsr.feature.thermal.data.ThermalSettingsRepository.getInstance(context)
                thermalSettings = thermalSettingsRepo.getSettings()

                val effectiveSaveImages = saveImages || (thermalSettings?.saveRawImages ?: false)

                Log.i(
                    TAG,
                    "Thermal settings loaded: frameRate=${thermalSettings?.frameRate}fps, saveImages=$effectiveSaveImages, palette=${thermalSettings?.palette}"
                )

                this@ThermalRecorder.saveImages = effectiveSaveImages
                this@ThermalRecorder.sessionMetadata = sessionMetadata
                sessionDirectory = File(sessionDir)

                sessionDirectory?.let { dir ->
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                }

                val csvFile = File(sessionDirectory, "thermal_stats_${sessionMetadata.sessionId}.csv")
                
                // Open writer for the entire recording session
                csvWriter = FileWriter(csvFile, false).apply {
                    write(sessionMetadata.createTimingHeader())
                    write("# THERMAL FRAME DATA - Temperatures in Celsius\n")
                    write("# Frame timestamps include:\n")
                    write("#   timestamp_wall_ms: Wall clock time (UTC)\n")
                    write("#   timestamp_relative_ms: Milliseconds since session start (monotonic)\n")
                    write("#   timestamp_monotonic_ns: Raw monotonic nanoseconds for precise intervals\n")
                    write("#   synchronized_timestamp_ms: PC-synchronized timestamp (includes clock offset from time sync)\n")
                    write("#\n")
                    write("timestamp_wall_ms,timestamp_relative_ms,timestamp_monotonic_ns,synchronized_timestamp_ms,frame_sequence,min_temp_c,avg_temp_c,max_temp_c,pixel_count\n")
                    flush()
                }

                frameSequence.set(0)

                sessionMetadata.addSyncEvent(
                    "THERMAL_RECORDING_START", mapOf(
                        "sensor_type" to "thermal_topdon",
                        "sensor_id" to "thermal_topdon_tc001",
                        "save_images" to saveImages.toString(),
                        "sync_verification" to "enabled"
                    )
                )

                isRecording.set(true)
                AppLogger.i(TAG, "Thermal recording started with session timing: ${csvFile.absolutePath}")
                AppLogger.i(TAG, "Session start: ${sessionMetadata.sessionStartIso}")
                AppLogger.i(TAG, "Thermal recording SessionSync event logged for alignment verification")

                if (saveImages) {
                    sessionDirectory?.let { dir ->
                        Log.i(
                            TAG,
                            "Thermal frame images will be saved to: ${dir.absolutePath}"
                        )
                    }
                }

                return@withContext true

            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start thermal recording", e)
                frameListener?.onError("Failed to start thermal recording: ${e.message}")
                return@withContext false
            }
        }

    suspend fun startRecording(sessionDir: String, saveImages: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            if (isRecording.get()) {
                AppLogger.w(TAG, "Thermal recording already in progress")
                return@withContext false
            }

            try {
                this@ThermalRecorder.saveImages = saveImages
                sessionDirectory = File(sessionDir)

                sessionDirectory?.let { dir ->
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                }

                val timestamp =
                    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val csvFile = File(sessionDirectory, "thermal_stats_$timestamp.csv")
                
                // Open writer for the entire recording session
                csvWriter = FileWriter(csvFile, false).apply {
                    write("# Legacy thermal recording - no session synchronization metadata\n")
                    write(CSV_HEADER)
                    write("\n")
                    flush()
                }

                frameSequence.set(0)

                isRecording.set(true)
                AppLogger.i(TAG, "Thermal recording started (legacy mode): ${csvFile.absolutePath}")

                if (saveImages) {
                    sessionDirectory?.let { dir ->
                        Log.i(
                            TAG,
                            "Thermal frame images will be saved to: ${dir.absolutePath}"
                        )
                    }
                }

                return@withContext true

            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start thermal recording", e)
                frameListener?.onError("Failed to start thermal recording: ${e.message}")
                return@withContext false
            }
        }

    suspend fun stopRecording(): Boolean = withContext(Dispatchers.IO) {
        if (!isRecording.get()) {
            AppLogger.w(TAG, "No thermal recording in progress")
            return@withContext false
        }

        try {
            isRecording.set(false)
            
            // Close the writer properly
            csvWriter?.flush()
            csvWriter?.close()
            csvWriter = null

            AppLogger.i(TAG, "Thermal recording stopped. Processed ${frameSequence.get()} frames")
            return@withContext true

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping thermal recording", e)
            return@withContext false
        }
    }

    fun processFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
        timestampNs: Long = TimestampManager.getCurrentTimestampNanos()
    ) {
        if (!isRecording.get()) {
            return
        }

        recordingScope.launch {
            try {
                val stats = calculateFrameStats(frameData, width, height, timestampNs)
                logFrameStats(stats)

                if (saveImages) {
                    saveFrameImage(frameData, width, height, stats.frameSequence)
                }

                frameListener?.onFrameProcessed(stats)

            } catch (e: Exception) {
                AppLogger.e(TAG, "Error processing thermal frame", e)
                frameListener?.onError("Error processing frame: ${e.message}")
            }
        }
    }

    fun processFrameFromIntensity(
        intensityData: ByteArray,
        width: Int,
        height: Int,
        minTempRange: Float = -20f,
        maxTempRange: Float = 400f,
        timestampNs: Long = TimestampManager.getCurrentTimestampNanos()
    ) {
        if (!isRecording.get()) {
            return
        }

        recordingScope.launch {
            try {

                val tempData = FloatArray(width * height)
                val tempRange = maxTempRange - minTempRange

                for (i in intensityData.indices) {
                    val intensity = intensityData[i].toInt() and 0xFF
                    tempData[i] = minTempRange + (intensity / 255.0f) * tempRange
                }

                val stats = calculateFrameStatsFromFloat(tempData, width, height, timestampNs)
                logFrameStats(stats)

                if (saveImages) {
                    saveFrameImageFromIntensity(intensityData, width, height, stats.frameSequence)
                }

                frameListener?.onFrameProcessed(stats)

            } catch (e: Exception) {
                AppLogger.e(TAG, "Error processing thermal frame from intensity", e)
                frameListener?.onError("Error processing frame from intensity: ${e.message}")
            }
        }
    }

    private fun calculateFrameStats(
        frameData: ByteArray,
        width: Int,
        height: Int,
        timestampNs: Long
    ): ThermalFrameStats {

        val pixelCount = width * height
        val expectedSize = pixelCount * 4

        if (frameData.size < expectedSize) {
            AppLogger.w(TAG, "Frame data size mismatch: expected $expectedSize, got ${frameData.size}")
        }

        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var totalTemp = 0.0f
        var validPixels = 0

        for (i in 0 until min(pixelCount, frameData.size / 4)) {
            val byteIndex = i * 4
            val temp = ByteBuffer.wrap(frameData, byteIndex, 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).float

            if (!temp.isNaN() && !temp.isInfinite()) {
                minTemp = min(minTemp, temp)
                maxTemp = max(maxTemp, temp)
                totalTemp += temp
                validPixels++
            }
        }

        val avgTemp = if (validPixels > 0) totalTemp / validPixels else 0.0f
        val sequence = frameSequence.incrementAndGet()

        return ThermalFrameStats(
            timestampNs = timestampNs,
            frameSequence = sequence,
            minTemp = if (minTemp == Float.MAX_VALUE) 0.0f else minTemp,
            avgTemp = avgTemp,
            maxTemp = if (maxTemp == Float.MIN_VALUE) 0.0f else maxTemp,
            pixelCount = validPixels
        )
    }

    private fun calculateFrameStatsFromFloat(
        tempData: FloatArray,
        width: Int,
        height: Int,
        timestampNs: Long
    ): ThermalFrameStats {
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var totalTemp = 0.0f
        var validPixels = 0

        for (temp in tempData) {
            if (!temp.isNaN() && !temp.isInfinite()) {
                minTemp = min(minTemp, temp)
                maxTemp = max(maxTemp, temp)
                totalTemp += temp
                validPixels++
            }
        }

        val avgTemp = if (validPixels > 0) totalTemp / validPixels else 0.0f
        val sequence = frameSequence.incrementAndGet()

        return ThermalFrameStats(
            timestampNs = timestampNs,
            frameSequence = sequence,
            minTemp = if (minTemp == Float.MAX_VALUE) 0.0f else minTemp,
            avgTemp = avgTemp,
            maxTemp = if (maxTemp == Float.MIN_VALUE) 0.0f else maxTemp,
            pixelCount = validPixels
        )
    }

    private suspend fun logFrameStats(stats: ThermalFrameStats) = withContext(Dispatchers.IO) {
        try {
            csvWriter?.let { writer ->
                val csvLine = sessionMetadata?.let { sm ->
                    val wallClockMs = sm.monotonicToWallClock(stats.timestampNs)
                    val relativeMs = (stats.timestampNs - sm.sessionStartMonotonicNs) / 1_000_000L
                    val synchronizedTimestampMs = TimestampManager.getSynchronizedTimestampMs()

                    StringBuilder().apply {
                        append(wallClockMs)
                        append(',')
                        append(relativeMs)
                        append(',')
                        append(stats.timestampNs)
                        append(',')
                        append(synchronizedTimestampMs)
                        append(',')
                        append(stats.frameSequence)
                        append(',')
                        append("%.3f".format(Locale.US, stats.minTemp))
                        append(',')
                        append("%.3f".format(Locale.US, stats.avgTemp))
                        append(',')
                        append("%.3f".format(Locale.US, stats.maxTemp))
                        append(',')
                        append(stats.pixelCount)
                    }.toString()
                } ?: StringBuilder().apply {
                    append(stats.timestampNs)
                    append(',')
                    append(stats.frameSequence)
                    append(',')
                    append("%.3f".format(Locale.US, stats.minTemp))
                    append(',')
                    append("%.3f".format(Locale.US, stats.avgTemp))
                    append(',')
                    append("%.3f".format(Locale.US, stats.maxTemp))
                    append(',')
                    append(stats.pixelCount)
                }.toString()

                writer.write(csvLine)
                writer.write("\n")
                writer.flush()

                Log.d(
                    TAG,
                    "Frame ${stats.frameSequence}: T=${stats.minTemp}°C to ${stats.maxTemp}°C (avg=${stats.avgTemp}°C)"
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error writing thermal stats to CSV", e)
        }
    }

    private suspend fun saveFrameImage(
        frameData: ByteArray,
        width: Int,
        height: Int,
        frameSequence: Long
    ) = withContext(Dispatchers.IO) {
        try {
            sessionDirectory?.let { dir ->
                val imageFile =
                    File(
                        dir,
                        "thermal_frame_${frameSequence}_${TimestampManager.getCurrentTimestampNanos()}.raw"
                    )

                FileOutputStream(imageFile).use { output ->
                    output.write(frameData)
                }

                AppLogger.d(TAG, "Saved thermal frame image: ${imageFile.name}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error saving thermal frame image", e)
        }
    }

    private suspend fun saveFrameImageFromIntensity(
        intensityData: ByteArray,
        width: Int,
        height: Int,
        frameSequence: Long
    ) = withContext(Dispatchers.IO) {
        try {
            sessionDirectory?.let { dir ->

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val pixels = IntArray(width * height)
                for (i in intensityData.indices) {
                    val intensity = intensityData[i].toInt() and 0xFF

                    val color =
                        (0xFF000000.toInt()) or (intensity shl 16) or (intensity shl 8) or intensity
                    pixels[i] = color
                }

                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

                val imageFile =
                    File(
                        dir,
                        "thermal_frame_${frameSequence}_${TimestampManager.getCurrentTimestampNanos()}.png"
                    )
                FileOutputStream(imageFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                bitmap.recycle()
                AppLogger.d(TAG, "Saved thermal frame PNG: ${imageFile.name}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error saving thermal frame PNG", e)
        }
    }

    fun isRecording(): Boolean = isRecording.get()

    fun getFrameCount(): Long = frameSequence.get()
}
