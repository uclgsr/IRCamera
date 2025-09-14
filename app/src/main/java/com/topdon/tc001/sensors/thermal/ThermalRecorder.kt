package com.topdon.tc001.sensors.thermal

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

/**
 * ThermalRecorder handles thermal camera integration as specified in the requirements:
 * - Integrates with existing UVC camera flow via frame callbacks
 * - Computes thermal statistics (min/avg/max temperature) per frame
 * - Logs thermal data to CSV with nanosecond timestamps
 * - Optionally saves thermal frame images
 */
class ThermalRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "ThermalRecorder"
        private const val CSV_HEADER = "timestamp_ns,frame_sequence,min_temp_c,avg_temp_c,max_temp_c,pixel_count"
    }
    
    private var isRecording = AtomicBoolean(false)
    private var frameSequence = AtomicLong(0)
    private var sessionDirectory: File? = null
    private var csvWriter: FileWriter? = null
    private var saveImages = false
    
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Frame callback interface to integrate with existing UVC camera
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
    
    /**
     * Start thermal recording to the specified session directory
     */
    suspend fun startRecording(sessionDir: String, saveImages: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (isRecording.get()) {
            Log.w(TAG, "Thermal recording already in progress")
            return@withContext false
        }
        
        try {
            this@ThermalRecorder.saveImages = saveImages
            sessionDirectory = File(sessionDir)
            
            if (!sessionDirectory!!.exists()) {
                sessionDirectory!!.mkdirs()
            }
            
            // Create thermal stats CSV file
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val csvFile = File(sessionDirectory, "thermal_stats_$timestamp.csv")
            csvWriter = FileWriter(csvFile, false)
            
            // Write CSV header
            csvWriter?.write(CSV_HEADER)
            csvWriter?.write("\n")
            csvWriter?.flush()
            
            // Reset counters
            frameSequence.set(0)
            
            isRecording.set(true)
            Log.i(TAG, "Thermal recording started: ${csvFile.absolutePath}")
            
            if (saveImages) {
                Log.i(TAG, "Thermal frame images will be saved to: ${sessionDirectory!!.absolutePath}")
            }
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start thermal recording", e)
            frameListener?.onError("Failed to start thermal recording: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Stop thermal recording
     */
    suspend fun stopRecording(): Boolean = withContext(Dispatchers.IO) {
        if (!isRecording.get()) {
            Log.w(TAG, "No thermal recording in progress")
            return@withContext false
        }
        
        try {
            isRecording.set(false)
            
            // Close CSV writer
            csvWriter?.flush()
            csvWriter?.close()
            csvWriter = null
            
            Log.i(TAG, "Thermal recording stopped. Processed ${frameSequence.get()} frames")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping thermal recording", e)
            return@withContext false
        }
    }
    
    /**
     * Process thermal frame data - this method should be called from the UVC camera callback
     * Assumes thermal data is provided as temperature values in Celsius
     */
    fun processFrame(frameData: ByteArray, width: Int, height: Int, timestampNs: Long = System.nanoTime()) {
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
                Log.e(TAG, "Error processing thermal frame", e)
                frameListener?.onError("Error processing frame: ${e.message}")
            }
        }
    }
    
    /**
     * Process thermal frame from raw pixel intensities (0-255) with temperature mapping
     * For cases where thermal camera provides raw intensity data that needs conversion
     */
    fun processFrameFromIntensity(
        intensityData: ByteArray, 
        width: Int, 
        height: Int, 
        minTempRange: Float = -20f, 
        maxTempRange: Float = 400f,
        timestampNs: Long = System.nanoTime()
    ) {
        if (!isRecording.get()) {
            return
        }
        
        recordingScope.launch {
            try {
                // Convert intensity data to temperature values
                val tempData = FloatArray(width * height)
                val tempRange = maxTempRange - minTempRange
                
                for (i in intensityData.indices) {
                    val intensity = intensityData[i].toInt() and 0xFF // Convert to unsigned byte
                    tempData[i] = minTempRange + (intensity / 255.0f) * tempRange
                }
                
                val stats = calculateFrameStatsFromFloat(tempData, width, height, timestampNs)
                logFrameStats(stats)
                
                if (saveImages) {
                    saveFrameImageFromIntensity(intensityData, width, height, stats.frameSequence)
                }
                
                frameListener?.onFrameProcessed(stats)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing thermal frame from intensity", e)
                frameListener?.onError("Error processing frame from intensity: ${e.message}")
            }
        }
    }
    
    /**
     * Calculate frame statistics from temperature data (assuming float values in Celsius)
     */
    private fun calculateFrameStats(frameData: ByteArray, width: Int, height: Int, timestampNs: Long): ThermalFrameStats {
        // Assume frameData contains float temperature values (4 bytes per pixel)
        val pixelCount = width * height
        val expectedSize = pixelCount * 4 // 4 bytes per float
        
        if (frameData.size < expectedSize) {
            Log.w(TAG, "Frame data size mismatch: expected $expectedSize, got ${frameData.size}")
        }
        
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var totalTemp = 0.0f
        var validPixels = 0
        
        // Convert bytes to floats and calculate statistics
        for (i in 0 until min(pixelCount, frameData.size / 4)) {
            val byteIndex = i * 4
            val temp = ByteBuffer.wrap(frameData, byteIndex, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).float
            
            // Skip invalid temperature readings
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
    
    /**
     * Calculate frame statistics from float array
     */
    private fun calculateFrameStatsFromFloat(tempData: FloatArray, width: Int, height: Int, timestampNs: Long): ThermalFrameStats {
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
    
    /**
     * Log frame statistics to CSV
     */
    private suspend fun logFrameStats(stats: ThermalFrameStats) = withContext(Dispatchers.IO) {
        try {
            csvWriter?.let { writer ->
                val csvLine = StringBuilder().apply {
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
                
                Log.d(TAG, "Frame ${stats.frameSequence}: T=${stats.minTemp}°C to ${stats.maxTemp}°C (avg=${stats.avgTemp}°C)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing thermal stats to CSV", e)
        }
    }
    
    /**
     * Save thermal frame as image file
     */
    private suspend fun saveFrameImage(frameData: ByteArray, width: Int, height: Int, frameSequence: Long) = withContext(Dispatchers.IO) {
        try {
            sessionDirectory?.let { dir ->
                val imageFile = File(dir, "thermal_frame_${frameSequence}_${System.nanoTime()}.raw")
                
                FileOutputStream(imageFile).use { output ->
                    output.write(frameData)
                }
                
                Log.d(TAG, "Saved thermal frame image: ${imageFile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving thermal frame image", e)
        }
    }
    
    /**
     * Save thermal frame from intensity data as PNG
     */
    private suspend fun saveFrameImageFromIntensity(intensityData: ByteArray, width: Int, height: Int, frameSequence: Long) = withContext(Dispatchers.IO) {
        try {
            sessionDirectory?.let { dir ->
                // Create bitmap from intensity data
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                val pixels = IntArray(width * height)
                for (i in intensityData.indices) {
                    val intensity = intensityData[i].toInt() and 0xFF
                    // Create grayscale color
                    val color = (0xFF000000.toInt()) or (intensity shl 16) or (intensity shl 8) or intensity
                    pixels[i] = color
                }
                
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                
                val imageFile = File(dir, "thermal_frame_${frameSequence}_${System.nanoTime()}.png")
                FileOutputStream(imageFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                
                bitmap.recycle()
                Log.d(TAG, "Saved thermal frame PNG: ${imageFile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving thermal frame PNG", e)
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording.get()
    
    /**
     * Get frame count
     */
    fun getFrameCount(): Long = frameSequence.get()
}
