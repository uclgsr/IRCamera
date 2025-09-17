package com.topdon.tc001.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.topdon.lib.core.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * PreviewStreamer handles streaming of live preview data to PC Controller.
 *
 * Provides low-bandwidth preview streaming including:
 * - Downscaled camera frames (RGB and thermal)
 * - Real-time sensor readings (GSR)
 * - Status updates
 */
class PreviewStreamer(
    private val networkServer: NetworkServer
) {
    companion object {
        private const val TAG = "PreviewStreamer"
        private const val DEFAULT_FRAME_INTERVAL_MS = 1000L // 1 FPS
        private const val DEFAULT_SENSOR_INTERVAL_MS = 1000L // 1 Hz
        private const val DEFAULT_PREVIEW_WIDTH = 320
        private const val DEFAULT_PREVIEW_HEIGHT = 240
        private const val DEFAULT_JPEG_QUALITY = 70
    }

    private val isStreaming = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var frameStreamingJob: Job? = null
    private var sensorStreamingJob: Job? = null

    // Configuration
    private var frameIntervalMs = DEFAULT_FRAME_INTERVAL_MS
    private var sensorIntervalMs = DEFAULT_SENSOR_INTERVAL_MS
    private var previewWidth = DEFAULT_PREVIEW_WIDTH
    private var previewHeight = DEFAULT_PREVIEW_HEIGHT
    private var jpegQuality = DEFAULT_JPEG_QUALITY

    // Current data references
    private val currentRgbFrame = AtomicReference<Bitmap?>()
    private val currentThermalFrame = AtomicReference<Bitmap?>()
    private val currentGsrValue = AtomicReference<Float?>()
    private val currentRecordingStatus = AtomicReference<String>("IDLE")

    /**
     * Start preview streaming to connected PC client
     */
    suspend fun startStreaming(): Boolean {
        if (isStreaming.get()) {
            Log.w(TAG, "Preview streaming already active")
            return true
        }

        if (!networkServer.isClientConnected()) {
            Log.w(TAG, "No PC client connected, cannot start streaming")
            return false
        }

        Log.i(TAG, "Starting preview streaming to PC")
        isStreaming.set(true)

        // Start frame streaming
        frameStreamingJob = scope.launch {
            streamFrames()
        }

        // Start sensor data streaming
        sensorStreamingJob = scope.launch {
            streamSensorData()
        }

        return true
    }

    /**
     * Stop preview streaming
     */
    suspend fun stopStreaming() {
        if (!isStreaming.get()) {
            return
        }

        Log.i(TAG, "Stopping preview streaming")
        isStreaming.set(false)

        frameStreamingJob?.cancel()
        sensorStreamingJob?.cancel()

        frameStreamingJob = null
        sensorStreamingJob = null
    }

    /**
     * Update RGB camera frame for streaming
     */
    fun updateRgbFrame(bitmap: Bitmap?) {
        currentRgbFrame.set(bitmap)
    }

    /**
     * Update thermal camera frame for streaming
     */
    fun updateThermalFrame(bitmap: Bitmap?) {
        currentThermalFrame.set(bitmap)
    }

    /**
     * Update GSR sensor value for streaming
     */
    fun updateGsrValue(gsrValue: Float) {
        currentGsrValue.set(gsrValue)
    }

    /**
     * Update recording status for streaming
     */
    fun updateRecordingStatus(status: String) {
        currentRecordingStatus.set(status)
    }

    /**
     * Configure streaming parameters
     */
    fun configure(
        frameIntervalMs: Long = DEFAULT_FRAME_INTERVAL_MS,
        sensorIntervalMs: Long = DEFAULT_SENSOR_INTERVAL_MS,
        previewWidth: Int = DEFAULT_PREVIEW_WIDTH,
        previewHeight: Int = DEFAULT_PREVIEW_HEIGHT,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY
    ) {
        this.frameIntervalMs = frameIntervalMs
        this.sensorIntervalMs = sensorIntervalMs
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        this.jpegQuality = jpegQuality

        Log.i(
            TAG,
            "Preview streaming configured: ${frameIntervalMs}ms frames, ${sensorIntervalMs}ms sensors, ${previewWidth}x${previewHeight}@${jpegQuality}%"
        )
    }

    private suspend fun streamFrames() {
        Log.i(TAG, "Frame streaming started")

        while (isActive && isStreaming.get()) {
            try {
                // Stream RGB frame if available
                currentRgbFrame.get()?.let { rgbBitmap ->
                    streamFrame("rgb", rgbBitmap)
                }

                // Stream thermal frame if available
                currentThermalFrame.get()?.let { thermalBitmap ->
                    streamFrame("thermal", thermalBitmap)
                }

                delay(frameIntervalMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error in frame streaming", e)
                if (isActive) delay(1000) // Wait before retrying
            }
        }

        Log.i(TAG, "Frame streaming stopped")
    }

    private suspend fun streamSensorData() {
        Log.i(TAG, "Sensor data streaming started")

        while (isActive && isStreaming.get()) {
            try {
                val gsrValue = currentGsrValue.get()
                val recordingStatus = currentRecordingStatus.get()

                val sensorMessage = JSONObject().apply {
                    put("message_type", "sensor_data")
                    put("timestamp_ns", System.nanoTime())
                    put("data", JSONObject().apply {
                        if (gsrValue != null) {
                            put("gsr_microsiemens", gsrValue)
                        }
                        put("recording_status", recordingStatus)
                        put("client_count", if (networkServer.isClientConnected()) 1 else 0)
                    })
                }

                networkServer.sendMessage(sensorMessage)

                delay(sensorIntervalMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error in sensor data streaming", e)
                if (isActive) delay(1000) // Wait before retrying
            }
        }

        Log.i(TAG, "Sensor data streaming stopped")
    }

    private suspend fun streamFrame(frameType: String, bitmap: Bitmap) {
        try {
            // Scale bitmap to preview dimensions
            val scaledBitmap = BitmapUtils.scaleWithWH(
                bitmap,
                previewWidth.toDouble(),
                previewHeight.toDouble()
            )

            // Convert to JPEG bytes
            val jpegBytes = BitmapUtils.bitmapToBytes(scaledBitmap, jpegQuality)
            if (jpegBytes == null) {
                Log.w(TAG, "Failed to convert $frameType frame to JPEG")
                return
            }

            // Encode as base64 for JSON transmission
            val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            val frameMessage = JSONObject().apply {
                put("message_type", "preview_frame")
                put("timestamp_ns", System.nanoTime())
                put("frame_type", frameType)
                put("width", scaledBitmap.width)
                put("height", scaledBitmap.height)
                put("format", "jpeg")
                put("quality", jpegQuality)
                put("data_base64", base64Data)
                put("data_size_bytes", jpegBytes.size)
            }

            networkServer.sendMessage(frameMessage)

            Log.d(
                TAG,
                "Streamed $frameType frame: ${scaledBitmap.width}x${scaledBitmap.height}, ${jpegBytes.size} bytes"
            )

            // Clean up scaled bitmap if it's different from original
            if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error streaming $frameType frame", e)
        }
    }

    /**
     * Get current streaming status
     */
    fun isStreaming(): Boolean = isStreaming.get()

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.launch {
            stopStreaming()
        }
        scope.cancel()
        Log.i(TAG, "PreviewStreamer cleaned up")
    }
}
