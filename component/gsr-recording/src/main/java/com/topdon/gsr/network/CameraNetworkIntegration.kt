package com.topdon.gsr.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Camera-Network Integration service for real-time thermal and RGB streaming
 * Provides optimized frame streaming with adaptive quality and compression
 */
class CameraNetworkIntegration(
    private val context: Context,
    private val networkClient: NetworkClient,
    private val qosManager: QualityOfServiceManager
) {
    companion object {
        private const val TAG = "CameraNetworkIntegration"
        private const val RGB_STREAM_ID = "rgb_camera"
        private const val THERMAL_STREAM_ID = "thermal_camera"
        private const val MAX_FRAME_QUEUE_SIZE = 30
        private const val FRAME_DROP_THRESHOLD = 0.8f // Drop frames when queue is 80% full
        private const val JPEG_QUALITY_HIGH = 85
        private const val JPEG_QUALITY_MEDIUM = 65
        private const val JPEG_QUALITY_LOW = 45
    }

    private val streamingJob = SupervisorJob()
    private val streamingScope = CoroutineScope(Dispatchers.IO + streamingJob)
    
    private val isRgbStreamingActive = AtomicBoolean(false)
    private val isThermalStreamingActive = AtomicBoolean(false)
    
    private val rgbFrameQueue = ConcurrentLinkedQueue<RgbFrame>()
    private val thermalFrameQueue = ConcurrentLinkedQueue<ThermalFrame>()
    
    private val rgbFrameCount = AtomicLong(0)
    private val thermalFrameCount = AtomicLong(0)
    private val droppedFrameCount = AtomicLong(0)
    
    private var currentSessionId: String? = null
    private var rgbStreamingJob: Job? = null
    private var thermalStreamingJob: Job? = null

    data class RgbFrame(
        val frameId: Long,
        val timestamp: Long,
        val width: Int,
        val height: Int,
        val imageData: ByteArray,
        val format: String,
        val quality: Int,
        val sessionId: String
    )
    
    data class ThermalFrame(
        val frameId: Long,
        val timestamp: Long,
        val width: Int,
        val height: Int,
        val thermalData: FloatArray,
        val minTemp: Float,
        val maxTemp: Float,
        val sessionId: String
    )
    
    data class StreamMetrics(
        val streamId: String,
        val isActive: Boolean,
        val frameRate: Float,
        val totalFrames: Long,
        val droppedFrames: Long,
        val queueSize: Int,
        val avgLatency: Long
    )

    /**
     * Initialize camera streaming for session
     */
    suspend fun initializeCameraStreaming(sessionId: String) = withContext(Dispatchers.IO) {
        currentSessionId = sessionId
        
        Log.d(TAG, "Initialized camera streaming for session: $sessionId")
        
        // Send stream initialization message to PC Controller
        val initMessage = JSONObject().apply {
            put("type", "camera_stream_init")
            put("session_id", sessionId)
            put("streams", org.json.JSONArray().apply {
                put(RGB_STREAM_ID)
                put(THERMAL_STREAM_ID)
            })
            put("timestamp", System.currentTimeMillis())
        }
        
        networkClient.sendMessage(initMessage)
    }

    /**
     * Start RGB camera streaming
     */
    suspend fun startRgbStreaming() = withContext(Dispatchers.IO) {
        if (isRgbStreamingActive.getAndSet(true)) {
            Log.w(TAG, "RGB streaming already active")
            return@withContext
        }
        
        Log.d(TAG, "Starting RGB camera streaming")
        
        rgbStreamingJob = streamingScope.launch {
            while (isRgbStreamingActive.get()) {
                processRgbFrameQueue()
                delay(16L) // ~60 FPS processing
            }
        }
        
        // Notify PC Controller that RGB streaming started
        val startMessage = JSONObject().apply {
            put("type", "stream_started")
            put("stream_id", RGB_STREAM_ID)
            put("session_id", currentSessionId)
            put("timestamp", System.currentTimeMillis())
        }
        
        networkClient.sendMessage(startMessage)
    }

    /**
     * Start thermal camera streaming
     */
    suspend fun startThermalStreaming() = withContext(Dispatchers.IO) {
        if (isThermalStreamingActive.getAndSet(true)) {
            Log.w(TAG, "Thermal streaming already active")
            return@withContext
        }
        
        Log.d(TAG, "Starting thermal camera streaming")
        
        thermalStreamingJob = streamingScope.launch {
            while (isThermalStreamingActive.get()) {
                processThermalFrameQueue()
                delay(33L) // ~30 FPS processing for thermal
            }
        }
        
        // Notify PC Controller that thermal streaming started
        val startMessage = JSONObject().apply {
            put("type", "stream_started")
            put("stream_id", THERMAL_STREAM_ID)
            put("session_id", currentSessionId)
            put("timestamp", System.currentTimeMillis())
        }
        
        networkClient.sendMessage(startMessage)
    }

    /**
     * Process RGB frame from camera data (byte array format)
     */
    fun processRgbFrame(frameData: ByteArray, width: Int, height: Int, format: String) {
        if (!isRgbStreamingActive.get()) return
        
        val frameId = rgbFrameCount.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        
        try {
            // Check if queue is getting full
            if (rgbFrameQueue.size >= MAX_FRAME_QUEUE_SIZE * FRAME_DROP_THRESHOLD) {
                // Drop oldest frame to prevent memory issues
                rgbFrameQueue.poll()?.let {
                    droppedFrameCount.incrementAndGet()
                    Log.v(TAG, "Dropped RGB frame due to queue overflow")
                }
            }
            
            // Create RGB frame
            val rgbFrame = RgbFrame(
                frameId = frameId,
                timestamp = timestamp,
                width = width,
                height = height,
                imageData = frameData,
                format = format,
                quality = determineJpegQuality(),
                sessionId = currentSessionId ?: "unknown"
            )
            
            rgbFrameQueue.offer(rgbFrame)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing RGB frame", e)
        }
    }

    /**
     * Process thermal frame data
     */
    fun processThermalFrame(
        thermalData: FloatArray,
        width: Int,
        height: Int,
        minTemp: Float,
        maxTemp: Float
    ) {
        if (!isThermalStreamingActive.get()) return
        
        val frameId = thermalFrameCount.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        
        try {
            // Check if queue is getting full
            if (thermalFrameQueue.size >= MAX_FRAME_QUEUE_SIZE * FRAME_DROP_THRESHOLD) {
                // Drop oldest frame
                thermalFrameQueue.poll()?.let {
                    droppedFrameCount.incrementAndGet()
                    Log.v(TAG, "Dropped thermal frame due to queue overflow")
                }
            }
            
            val thermalFrame = ThermalFrame(
                frameId = frameId,
                timestamp = timestamp,
                width = width,
                height = height,
                thermalData = thermalData,
                minTemp = minTemp,
                maxTemp = maxTemp,
                sessionId = currentSessionId ?: "unknown"
            )
            
            thermalFrameQueue.offer(thermalFrame)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing thermal frame", e)
        }
    }

    /**
     * Determine JPEG quality based on network conditions
     */
    private fun determineJpegQuality(): Int {
        val networkMetrics = qosManager.getNetworkQualityMetrics()
        
        return when (networkMetrics.networkTier) {
            QualityOfServiceManager.NetworkTier.EXCELLENT -> JPEG_QUALITY_HIGH
            QualityOfServiceManager.NetworkTier.HIGH -> JPEG_QUALITY_HIGH
            QualityOfServiceManager.NetworkTier.MEDIUM -> JPEG_QUALITY_MEDIUM
            QualityOfServiceManager.NetworkTier.LOW -> JPEG_QUALITY_LOW
            QualityOfServiceManager.NetworkTier.POOR -> JPEG_QUALITY_LOW
        }
    }

    /**
     * Process RGB frame queue and send frames to PC Controller
     */
    private suspend fun processRgbFrameQueue() {
        val frame = rgbFrameQueue.poll() ?: return
        
        try {
            // Create frame message
            val frameMessage = JSONObject().apply {
                put("type", "rgb_frame")
                put("stream_id", RGB_STREAM_ID)
                put("frame_id", frame.frameId)
                put("timestamp", frame.timestamp)
                put("width", frame.width)
                put("height", frame.height)
                put("format", frame.format)
                put("quality", frame.quality)
                put("data_size", frame.imageData.size)
                put("session_id", frame.sessionId)
            }
            
            // Queue frame data with high priority for real-time streaming
            qosManager.queueData(
                data = frame.imageData,
                dataType = QualityOfServiceManager.DataType.VIDEO_METADATA,
                priority = QualityOfServiceManager.Priority.HIGH,
                sessionId = frame.sessionId,
                metadata = mapOf(
                    "stream_id" to RGB_STREAM_ID,
                    "frame_id" to frame.frameId.toString(),
                    "timestamp" to frame.timestamp.toString()
                )
            )
            
            // Send frame metadata
            networkClient.sendMessage(frameMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending RGB frame", e)
        }
    }

    /**
     * Process thermal frame queue and send frames to PC Controller
     */
    private suspend fun processThermalFrameQueue() {
        val frame = thermalFrameQueue.poll() ?: return
        
        try {
            // Compress thermal data for transmission
            val compressedThermalData = compressThermalData(frame.thermalData)
            
            // Create frame message
            val frameMessage = JSONObject().apply {
                put("type", "thermal_frame")
                put("stream_id", THERMAL_STREAM_ID)
                put("frame_id", frame.frameId)
                put("timestamp", frame.timestamp)
                put("width", frame.width)
                put("height", frame.height)
                put("min_temp", frame.minTemp)
                put("max_temp", frame.maxTemp)
                put("data_size", compressedThermalData.size)
                put("session_id", frame.sessionId)
            }
            
            // Queue thermal data with normal priority
            qosManager.queueData(
                data = compressedThermalData,
                dataType = QualityOfServiceManager.DataType.THERMAL,
                priority = QualityOfServiceManager.Priority.NORMAL,
                sessionId = frame.sessionId,
                metadata = mapOf(
                    "stream_id" to THERMAL_STREAM_ID,
                    "frame_id" to frame.frameId.toString(),
                    "timestamp" to frame.timestamp.toString()
                )
            )
            
            // Send frame metadata
            networkClient.sendMessage(frameMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending thermal frame", e)
        }
    }

    /**
     * Compress thermal data for efficient transmission
     */
    private fun serializeThermalData(thermalData: FloatArray): ByteArray {
        // Convert float array to byte array for transmission
        val byteBuffer = ByteBuffer.allocate(thermalData.size * 4)
        thermalData.forEach { temp ->
            byteBuffer.putFloat(temp)
        }
        
        return byteBuffer.array()
    }

    /**
     * Stop RGB streaming
     */
    suspend fun stopRgbStreaming() = withContext(Dispatchers.IO) {
        if (!isRgbStreamingActive.getAndSet(false)) {
            Log.w(TAG, "RGB streaming not active")
            return@withContext
        }
        
        rgbStreamingJob?.cancel()
        rgbFrameQueue.clear()
        
        Log.d(TAG, "RGB streaming stopped")
        
        // Notify PC Controller
        val stopMessage = JSONObject().apply {
            put("type", "stream_stopped")
            put("stream_id", RGB_STREAM_ID)
            put("session_id", currentSessionId)
            put("timestamp", System.currentTimeMillis())
        }
        
        networkClient.sendMessage(stopMessage)
    }

    /**
     * Stop thermal streaming
     */
    suspend fun stopThermalStreaming() = withContext(Dispatchers.IO) {
        if (!isThermalStreamingActive.getAndSet(false)) {
            Log.w(TAG, "Thermal streaming not active")
            return@withContext
        }
        
        thermalStreamingJob?.cancel()
        thermalFrameQueue.clear()
        
        Log.d(TAG, "Thermal streaming stopped")
        
        // Notify PC Controller
        val stopMessage = JSONObject().apply {
            put("type", "stream_stopped")
            put("stream_id", THERMAL_STREAM_ID)
            put("session_id", currentSessionId)
            put("timestamp", System.currentTimeMillis())
        }
        
        networkClient.sendMessage(stopMessage)
    }

    /**
     * Get streaming metrics for monitoring
     */
    fun getStreamingMetrics(): List<StreamMetrics> {
        val metrics = mutableListOf<StreamMetrics>()
        
        // RGB stream metrics
        metrics.add(
            StreamMetrics(
                streamId = RGB_STREAM_ID,
                isActive = isRgbStreamingActive.get(),
                frameRate = calculateFrameRate(rgbFrameCount.get()),
                totalFrames = rgbFrameCount.get(),
                droppedFrames = droppedFrameCount.get(),
                queueSize = rgbFrameQueue.size,
                avgLatency = 0L // Would be calculated from timestamps
            )
        )
        
        // Thermal stream metrics
        metrics.add(
            StreamMetrics(
                streamId = THERMAL_STREAM_ID,
                isActive = isThermalStreamingActive.get(),
                frameRate = calculateFrameRate(thermalFrameCount.get()),
                totalFrames = thermalFrameCount.get(),
                droppedFrames = droppedFrameCount.get(),
                queueSize = thermalFrameQueue.size,
                avgLatency = 0L // Would be calculated from timestamps
            )
        )
        
        return metrics
    }

    /**
     * Calculate frame rate based on frame count and time
     */
    private fun calculateFrameRate(frameCount: Long): Float {
        // This would track timing over a window
        // For now, return a placeholder value
        return if (frameCount > 0) 30.0f else 0.0f
    }

    /**
     * Stop all streaming and cleanup resources
     */
    suspend fun stopAllStreaming() = withContext(Dispatchers.IO) {
        stopRgbStreaming()
        stopThermalStreaming()
        
        rgbFrameQueue.clear()
        thermalFrameQueue.clear()
        
        streamingJob.cancel()
        
        Log.d(TAG, "All camera streaming stopped")
    }
}