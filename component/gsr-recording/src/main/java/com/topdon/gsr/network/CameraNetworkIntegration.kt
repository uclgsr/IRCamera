package com.topdon.gsr.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class CameraNetworkIntegration(
    private val context: Context,
    private val networkClient: NetworkClient,
    private val qosManager: QualityOfServiceManager,
) {
    companion object {
        private const val TAG = "CameraNetworkIntegration"
        private const val RGB_STREAM_ID = "rgb_camera"
        private const val THERMAL_STREAM_ID = "thermal_camera"
        private const val MAX_FRAME_QUEUE_SIZE = 30
        private const val FRAME_DROP_THRESHOLD = 0.8f 
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
        val sessionId: String,
    )

    data class ThermalFrame(
        val frameId: Long,
        val timestamp: Long,
        val width: Int,
        val height: Int,
        val thermalData: FloatArray,
        val minTemp: Float,
        val maxTemp: Float,
        val sessionId: String,
    )

    data class StreamMetrics(
        val streamId: String,
        val isActive: Boolean,
        val frameRate: Float,
        val totalFrames: Long,
        val droppedFrames: Long,
        val queueSize: Int,
        val avgLatency: Long,
    )

    suspend fun initializeCameraStreaming(sessionId: String) =
        withContext(Dispatchers.IO) {
            currentSessionId = sessionId

            Log.d(TAG, "Initialized camera streaming for session: $sessionId")

            
            val initMessage =
                JSONObject().apply {
                    put("type", "camera_stream_init")
                    put("session_id", sessionId)
                    put(
                        "streams",
                        org.json.JSONArray().apply {
                            put(RGB_STREAM_ID)
                            put(THERMAL_STREAM_ID)
                        },
                    )
                    put("timestamp", System.currentTimeMillis())
                }

            networkClient.sendMessage(initMessage)
        }

    suspend fun startRgbStreaming() =
        withContext(Dispatchers.IO) {
            if (isRgbStreamingActive.getAndSet(true)) {
                Log.w(TAG, "RGB streaming already active")
                return@withContext
            }

            Log.d(TAG, "Starting RGB camera streaming")

            rgbStreamingJob =
                streamingScope.launch {
                    while (isRgbStreamingActive.get()) {
                        processRgbFrameQueue()
                        delay(16L) 
                    }
                }

            
            val startMessage =
                JSONObject().apply {
                    put("type", "stream_started")
                    put("stream_id", RGB_STREAM_ID)
                    put("session_id", currentSessionId)
                    put("timestamp", System.currentTimeMillis())
                }

            networkClient.sendMessage(startMessage)
        }

    suspend fun startThermalStreaming() =
        withContext(Dispatchers.IO) {
            if (isThermalStreamingActive.getAndSet(true)) {
                Log.w(TAG, "Thermal streaming already active")
                return@withContext
            }

            Log.d(TAG, "Starting thermal camera streaming")

            thermalStreamingJob =
                streamingScope.launch {
                    while (isThermalStreamingActive.get()) {
                        processThermalFrameQueue()
                        delay(33L) 
                    }
                }

            
            val startMessage =
                JSONObject().apply {
                    put("type", "stream_started")
                    put("stream_id", THERMAL_STREAM_ID)
                    put("session_id", currentSessionId)
                    put("timestamp", System.currentTimeMillis())
                }

            networkClient.sendMessage(startMessage)
        }

    fun processRgbFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
        format: String,
    ) {
        if (!isRgbStreamingActive.get()) return

        val frameId = rgbFrameCount.incrementAndGet()
        val timestamp = System.currentTimeMillis()

        try {
            
            if (rgbFrameQueue.size >= MAX_FRAME_QUEUE_SIZE * FRAME_DROP_THRESHOLD) {
                
                rgbFrameQueue.poll()?.let {
                    droppedFrameCount.incrementAndGet()
                    Log.v(TAG, "Dropped RGB frame due to queue overflow")
                }
            }

            
            val rgbFrame =
                RgbFrame(
                    frameId = frameId,
                    timestamp = timestamp,
                    width = width,
                    height = height,
                    imageData = frameData,
                    format = format,
                    quality = determineJpegQuality(),
                    sessionId = currentSessionId ?: "unknown",
                )

            rgbFrameQueue.offer(rgbFrame)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing RGB frame", e)
        }
    }

    fun processThermalFrame(
        thermalData: FloatArray,
        width: Int,
        height: Int,
        minTemp: Float,
        maxTemp: Float,
    ) {
        if (!isThermalStreamingActive.get()) return

        val frameId = thermalFrameCount.incrementAndGet()
        val timestamp = System.currentTimeMillis()

        try {
            
            if (thermalFrameQueue.size >= MAX_FRAME_QUEUE_SIZE * FRAME_DROP_THRESHOLD) {
                
                thermalFrameQueue.poll()?.let {
                    droppedFrameCount.incrementAndGet()
                    Log.v(TAG, "Dropped thermal frame due to queue overflow")
                }
            }

            val thermalFrame =
                ThermalFrame(
                    frameId = frameId,
                    timestamp = timestamp,
                    width = width,
                    height = height,
                    thermalData = thermalData,
                    minTemp = minTemp,
                    maxTemp = maxTemp,
                    sessionId = currentSessionId ?: "unknown",
                )

            thermalFrameQueue.offer(thermalFrame)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing thermal frame", e)
        }
    }

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

    private suspend fun processRgbFrameQueue() {
        val frame = rgbFrameQueue.poll() ?: return

        try {
            
            val frameMessage =
                JSONObject().apply {
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

            
            qosManager.queueData(
                data = frame.imageData,
                dataType = QualityOfServiceManager.DataType.VIDEO_METADATA,
                priority = QualityOfServiceManager.Priority.HIGH,
                sessionId = frame.sessionId,
                metadata =
                    mapOf(
                        "stream_id" to RGB_STREAM_ID,
                        "frame_id" to frame.frameId.toString(),
                        "timestamp" to frame.timestamp.toString(),
                    ),
            )

            
            networkClient.sendMessage(frameMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending RGB frame", e)
        }
    }

    private fun compressThermalData(thermalData: FloatArray): ByteArray {
        return try {
            
            val byteBuffer = ByteBuffer.allocate(thermalData.size * 4)
            for (value in thermalData) {
                byteBuffer.putFloat(value)
            }
            val thermalBytes = byteBuffer.array()

            
            val compressed = ByteArrayOutputStream()
            var i = 0
            while (i < thermalBytes.size) {
                val currentByte = thermalBytes[i]
                var count = 1

                
                while (i + count < thermalBytes.size &&
                    thermalBytes[i + count] == currentByte &&
                    count < 255
                ) {
                    count++
                }

                if (count > 3) {
                    
                    compressed.write(0xFF) 
                    compressed.write(count)
                    compressed.write(currentByte.toInt())
                    i += count
                } else {
                    
                    for (j in 0 until count) {
                        if (currentByte.toInt() == 0xFF) {
                            
                            compressed.write(0xFF)
                            compressed.write(0x00)
                        } else {
                            compressed.write(currentByte.toInt())
                        }
                    }
                    i += count
                }
            }
            compressed.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to compress thermal data, using float-to-byte conversion", e)
            
            val byteBuffer = ByteBuffer.allocate(thermalData.size * 4)
            for (value in thermalData) {
                byteBuffer.putFloat(value)
            }
            byteBuffer.array()
        }
    }

    private suspend fun processThermalFrameQueue() {
        val frame = thermalFrameQueue.poll() ?: return

        try {
            
            val compressedThermalData = compressThermalData(frame.thermalData)

            
            val frameMessage =
                JSONObject().apply {
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

            
            qosManager.queueData(
                data = compressedThermalData,
                dataType = QualityOfServiceManager.DataType.THERMAL,
                priority = QualityOfServiceManager.Priority.NORMAL,
                sessionId = frame.sessionId,
                metadata =
                    mapOf(
                        "stream_id" to THERMAL_STREAM_ID,
                        "frame_id" to frame.frameId.toString(),
                        "timestamp" to frame.timestamp.toString(),
                    ),
            )

            
            networkClient.sendMessage(frameMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending thermal frame", e)
        }
    }

    private fun serializeThermalData(thermalData: FloatArray): ByteArray {
        
        val byteBuffer = ByteBuffer.allocate(thermalData.size * 4)
        thermalData.forEach { temp ->
            byteBuffer.putFloat(temp)
        }

        return byteBuffer.array()
    }

    suspend fun stopRgbStreaming() =
        withContext(Dispatchers.IO) {
            if (!isRgbStreamingActive.getAndSet(false)) {
                Log.w(TAG, "RGB streaming not active")
                return@withContext
            }

            rgbStreamingJob?.cancel()
            rgbFrameQueue.clear()

            Log.d(TAG, "RGB streaming stopped")

            
            val stopMessage =
                JSONObject().apply {
                    put("type", "stream_stopped")
                    put("stream_id", RGB_STREAM_ID)
                    put("session_id", currentSessionId)
                    put("timestamp", System.currentTimeMillis())
                }

            networkClient.sendMessage(stopMessage)
        }

    suspend fun stopThermalStreaming() =
        withContext(Dispatchers.IO) {
            if (!isThermalStreamingActive.getAndSet(false)) {
                Log.w(TAG, "Thermal streaming not active")
                return@withContext
            }

            thermalStreamingJob?.cancel()
            thermalFrameQueue.clear()

            Log.d(TAG, "Thermal streaming stopped")

            
            val stopMessage =
                JSONObject().apply {
                    put("type", "stream_stopped")
                    put("stream_id", THERMAL_STREAM_ID)
                    put("session_id", currentSessionId)
                    put("timestamp", System.currentTimeMillis())
                }

            networkClient.sendMessage(stopMessage)
        }

    fun getStreamingMetrics(): List<StreamMetrics> {
        val metrics = mutableListOf<StreamMetrics>()

        
        metrics.add(
            StreamMetrics(
                streamId = RGB_STREAM_ID,
                isActive = isRgbStreamingActive.get(),
                frameRate = calculateFrameRate(rgbFrameCount.get()),
                totalFrames = rgbFrameCount.get(),
                droppedFrames = droppedFrameCount.get(),
                queueSize = rgbFrameQueue.size,
                avgLatency = 0L, 
            ),
        )

        
        metrics.add(
            StreamMetrics(
                streamId = THERMAL_STREAM_ID,
                isActive = isThermalStreamingActive.get(),
                frameRate = calculateFrameRate(thermalFrameCount.get()),
                totalFrames = thermalFrameCount.get(),
                droppedFrames = droppedFrameCount.get(),
                queueSize = thermalFrameQueue.size,
                avgLatency = 0L, 
            ),
        )

        return metrics
    }

    private fun calculateFrameRate(frameCount: Long): Float {
        
        // For now, return a placeholder value
        return if (frameCount > 0) 30.0f else 0.0f
    }

    suspend fun stopAllStreaming() =
        withContext(Dispatchers.IO) {
            stopRgbStreaming()
            stopThermalStreaming()

            rgbFrameQueue.clear()
            thermalFrameQueue.clear()

            streamingJob.cancel()

            Log.d(TAG, "All camera streaming stopped")
        }
}
