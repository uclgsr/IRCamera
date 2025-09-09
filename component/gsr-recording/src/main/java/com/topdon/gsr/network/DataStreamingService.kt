package com.topdon.gsr.network

import android.content.Context
import android.util.Log
import com.topdon.gsr.model.GSRSample
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time data streaming service for sending sensor data to PC Controller
 * Handles buffering, batching, and reliable delivery of sensor measurements
 */
class DataStreamingService(
    private val context: Context,
    private val networkClient: NetworkClient
) {
    companion object {
        private const val TAG = "DataStreamingService"
        private const val BATCH_SIZE = 50 // Number of samples per batch
        private const val BATCH_TIMEOUT_MS = 100L // Maximum time to wait for batch completion
        private const val MAX_QUEUE_SIZE = 5000 // Maximum queue size to prevent memory issues
        private const val RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 500L
    }

    private val streamingJob = SupervisorJob()
    private val streamingScope = CoroutineScope(Dispatchers.IO + streamingJob)
    
    private val gsrQueue = ConcurrentLinkedQueue<GSRSample>()
    private val thermalQueue = ConcurrentLinkedQueue<ThermalSample>()
    private val videoMetadataQueue = ConcurrentLinkedQueue<VideoMetadata>()
    
    private val isStreaming = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    
    private var batchingJob: Job? = null
    private var currentSessionId: String? = null

    data class ThermalSample(
        val timestamp: Long,
        val frameIndex: Long,
        val temperature: Float,
        val x: Int,
        val y: Int,
        val sessionId: String
    )

    data class VideoMetadata(
        val timestamp: Long,
        val frameIndex: Long,
        val frameSize: Int,
        val sessionId: String,
        val cameraType: String // "rgb" or "thermal"
    )

    interface StreamingEventListener {
        fun onStreamingStarted(sessionId: String)
        fun onStreamingStopped(sessionId: String)
        fun onBatchSent(batchSize: Int, dataType: String)
        fun onStreamingError(error: String)
        fun onQueueFull(dataType: String, droppedSamples: Int)
    }

    private var eventListener: StreamingEventListener? = null

    fun setEventListener(listener: StreamingEventListener?) {
        eventListener = listener
    }

    /**
     * Start real-time data streaming for a session
     */
    suspend fun startStreaming(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        if (isStreaming.get()) {
            Log.w(TAG, "Data streaming already active")
            return@withContext false
        }

        if (!networkClient.isConnected()) {
            Log.w(TAG, "Cannot start streaming - not connected to PC Controller")
            return@withContext false
        }

        try {
            currentSessionId = sessionId
            isStreaming.set(true)
            isConnected.set(true)

            // Clear any existing queued data
            clearQueues()

            // Start the batching and sending process
            startBatchingProcess()

            // Notify PC Controller that streaming started
            val success = networkClient.startDataStreaming()
            if (success) {
                eventListener?.onStreamingStarted(sessionId)
                Log.i(TAG, "Data streaming started for session: $sessionId")
                true
            } else {
                stopStreaming()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start data streaming", e)
            eventListener?.onStreamingError("Failed to start: ${e.message}")
            false
        }
    }

    /**
     * Stop real-time data streaming
     */
    suspend fun stopStreaming(): Boolean = withContext(Dispatchers.IO) {
        if (!isStreaming.get()) {
            Log.w(TAG, "Data streaming not active")
            return@withContext false
        }

        try {
            isStreaming.set(false)
            
            // Stop batching process
            batchingJob?.cancel()
            batchingJob = null

            // Send any remaining batched data
            sendRemainingData()

            // Notify PC Controller that streaming stopped
            val success = networkClient.stopDataStreaming()
            
            val sessionId = currentSessionId
            currentSessionId = null
            
            if (sessionId != null) {
                eventListener?.onStreamingStopped(sessionId)
            }

            Log.i(TAG, "Data streaming stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping data streaming", e)
            false
        }
    }

    /**
     * Queue GSR sample for streaming
     */
    fun queueGSRSample(sample: GSRSample) {
        if (!isStreaming.get()) return

        if (gsrQueue.size >= MAX_QUEUE_SIZE) {
            // Drop oldest samples to prevent memory overflow
            val dropped = minOf(BATCH_SIZE, gsrQueue.size / 2)
            repeat(dropped) { gsrQueue.poll() }
            eventListener?.onQueueFull("GSR", dropped)
            Log.w(TAG, "GSR queue full, dropped $dropped samples")
        }

        gsrQueue.offer(sample)
    }

    /**
     * Queue thermal sample for streaming
     */
    fun queueThermalSample(sample: ThermalSample) {
        if (!isStreaming.get()) return

        if (thermalQueue.size >= MAX_QUEUE_SIZE) {
            val dropped = minOf(BATCH_SIZE, thermalQueue.size / 2)
            repeat(dropped) { thermalQueue.poll() }
            eventListener?.onQueueFull("Thermal", dropped)
            Log.w(TAG, "Thermal queue full, dropped $dropped samples")
        }

        thermalQueue.offer(sample)
    }

    /**
     * Queue video metadata for streaming
     */
    fun queueVideoMetadata(metadata: VideoMetadata) {
        if (!isStreaming.get()) return

        if (videoMetadataQueue.size >= MAX_QUEUE_SIZE) {
            val dropped = minOf(BATCH_SIZE, videoMetadataQueue.size / 2)
            repeat(dropped) { videoMetadataQueue.poll() }
            eventListener?.onQueueFull("VideoMetadata", dropped)
            Log.w(TAG, "Video metadata queue full, dropped $dropped samples")
        }

        videoMetadataQueue.offer(metadata)
    }

    private fun startBatchingProcess() {
        batchingJob = streamingScope.launch {
            while (isStreaming.get() && isActive) {
                try {
                    // Process GSR batches
                    if (gsrQueue.size >= BATCH_SIZE) {
                        sendGSRBatch()
                    }

                    // Process thermal batches
                    if (thermalQueue.size >= BATCH_SIZE) {
                        sendThermalBatch()
                    }

                    // Process video metadata batches
                    if (videoMetadataQueue.size >= BATCH_SIZE) {
                        sendVideoMetadataBatch()
                    }

                    // Timeout-based batching for partial batches
                    delay(BATCH_TIMEOUT_MS)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error in batching process", e)
                        eventListener?.onStreamingError("Batching error: ${e.message}")
                        delay(1000) // Wait before retrying
                    }
                }
            }
        }
    }

    private suspend fun sendGSRBatch() {
        val batch = mutableListOf<GSRSample>()
        repeat(minOf(BATCH_SIZE, gsrQueue.size)) {
            gsrQueue.poll()?.let { batch.add(it) }
        }

        if (batch.isNotEmpty()) {
            val batchData = createGSRBatchJson(batch)
            if (sendBatchWithRetry(batchData, "gsr")) {
                eventListener?.onBatchSent(batch.size, "GSR")
            }
        }
    }

    private suspend fun sendThermalBatch() {
        val batch = mutableListOf<ThermalSample>()
        repeat(minOf(BATCH_SIZE, thermalQueue.size)) {
            thermalQueue.poll()?.let { batch.add(it) }
        }

        if (batch.isNotEmpty()) {
            val batchData = createThermalBatchJson(batch)
            if (sendBatchWithRetry(batchData, "thermal")) {
                eventListener?.onBatchSent(batch.size, "Thermal")
            }
        }
    }

    private suspend fun sendVideoMetadataBatch() {
        val batch = mutableListOf<VideoMetadata>()
        repeat(minOf(BATCH_SIZE, videoMetadataQueue.size)) {
            videoMetadataQueue.poll()?.let { batch.add(it) }
        }

        if (batch.isNotEmpty()) {
            val batchData = createVideoMetadataBatchJson(batch)
            if (sendBatchWithRetry(batchData, "video_metadata")) {
                eventListener?.onBatchSent(batch.size, "VideoMetadata")
            }
        }
    }

    private suspend fun sendBatchWithRetry(batchData: JSONObject, dataType: String): Boolean {
        repeat(RETRY_ATTEMPTS) { attempt ->
            try {
                val success = networkClient.sendMeasurementData(
                    currentSessionId ?: "unknown",
                    batchData
                )
                if (success) {
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Batch send attempt ${attempt + 1} failed for $dataType", e)
                if (attempt < RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        Log.e(TAG, "Failed to send $dataType batch after $RETRY_ATTEMPTS attempts")
        eventListener?.onStreamingError("Failed to send $dataType batch")
        return false
    }

    private fun createGSRBatchJson(samples: List<GSRSample>): JSONObject {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val sampleJson = JSONObject().apply {
                put("timestamp", sample.timestamp)
                put("utc_timestamp", sample.utcTimestamp)
                put("sample_index", sample.sampleIndex)
                put("conductance", sample.conductance)
                put("resistance", sample.resistance)
                put("raw_value", sample.rawValue)
                put("session_id", sample.sessionId)
            }
            samplesArray.put(sampleJson)
        }

        return JSONObject().apply {
            put("data_type", "gsr_batch")
            put("batch_size", samples.size)
            put("samples", samplesArray)
            put("synchronized_timestamp", networkClient.getSynchronizedTimestamp())
        }
    }

    private fun createThermalBatchJson(samples: List<ThermalSample>): JSONObject {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val sampleJson = JSONObject().apply {
                put("timestamp", sample.timestamp)
                put("frame_index", sample.frameIndex)
                put("temperature", sample.temperature)
                put("x", sample.x)
                put("y", sample.y)
                put("session_id", sample.sessionId)
            }
            samplesArray.put(sampleJson)
        }

        return JSONObject().apply {
            put("data_type", "thermal_batch")
            put("batch_size", samples.size)
            put("samples", samplesArray)
            put("synchronized_timestamp", networkClient.getSynchronizedTimestamp())
        }
    }

    private fun createVideoMetadataBatchJson(samples: List<VideoMetadata>): JSONObject {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val sampleJson = JSONObject().apply {
                put("timestamp", sample.timestamp)
                put("frame_index", sample.frameIndex)
                put("frame_size", sample.frameSize)
                put("session_id", sample.sessionId)
                put("camera_type", sample.cameraType)
            }
            samplesArray.put(sampleJson)
        }

        return JSONObject().apply {
            put("data_type", "video_metadata_batch")
            put("batch_size", samples.size)
            put("samples", samplesArray)
            put("synchronized_timestamp", networkClient.getSynchronizedTimestamp())
        }
    }

    private suspend fun sendRemainingData() {
        // Send any remaining GSR data
        while (gsrQueue.isNotEmpty()) {
            sendGSRBatch()
        }

        // Send any remaining thermal data
        while (thermalQueue.isNotEmpty()) {
            sendThermalBatch()
        }

        // Send any remaining video metadata
        while (videoMetadataQueue.isNotEmpty()) {
            sendVideoMetadataBatch()
        }
    }

    private fun clearQueues() {
        gsrQueue.clear()
        thermalQueue.clear()
        videoMetadataQueue.clear()
    }

    /**
     * Get current queue sizes for monitoring
     */
    fun getQueueSizes(): Map<String, Int> {
        return mapOf(
            "gsr" to gsrQueue.size,
            "thermal" to thermalQueue.size,
            "video_metadata" to videoMetadataQueue.size
        )
    }

    /**
     * Check if streaming is active
     */
    fun isStreamingActive(): Boolean = isStreaming.get()

    /**
     * Clean up resources
     */
    suspend fun cleanup() {
        // Stop streaming before cancelling jobs to ensure proper data flush
        stopStreaming()
        
        // Cancel all jobs after streaming is stopped
        streamingJob.cancel()
        clearQueues()
        eventListener = null
    }
}