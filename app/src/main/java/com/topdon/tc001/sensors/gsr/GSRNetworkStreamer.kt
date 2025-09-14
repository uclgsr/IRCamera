package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.util.Log
import com.topdon.gsr.model.GSRSample
import com.topdon.tc001.network.EnhancedNetworkClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class GSRNetworkStreamer(
    private val context: Context,
    private val sessionId: String,
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

    private var networkClient: EnhancedNetworkClient? = null
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

    private var clockOffset: Long = 0 // Nanoseconds
    private var lastSyncTime: Long = 0
    private val syncInterval = 30000L // 30 seconds

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing GSR network streamer for session: $sessionId")

                networkClient =
                    EnhancedNetworkClient(context).apply {

                        setConnectionTimeout(10000)
                        setHeartbeatInterval(HEARTBEAT_INTERVAL_MS)
                        setCompressionEnabled(true)
                    }

                val connected = networkClient?.connect() ?: false
                if (!connected) {
                    Log.e(TAG, "Failed to connect to PC hub")
                    return@withContext false
                }

                performTimeSync()

                registerGSRStream()

                Log.i(TAG, "GSR network streamer initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GSR network streamer", e)
                false
            }
        }
    }

    suspend fun startStreaming(): Boolean {
        if (_isStreaming.get()) {
            Log.w(TAG, "GSR streaming already active")
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

            Log.i(TAG, "GSR streaming started for session: $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GSR streaming", e)
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

            Log.i(TAG, "GSR streaming stopped for session: $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop GSR streaming", e)
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
                sampleBuffer.poll() // Remove oldest sample
                Log.w(TAG, "GSR sample buffer overflow, dropping oldest sample")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add GSR sample to buffer", e)
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
                Log.e(TAG, "Error in GSR streaming loop", e)
                networkErrors.incrementAndGet()
                delay(1000) // Error recovery delay
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
            val sent = networkClient?.sendMessage(batchMessage) ?: false

            if (sent) {
                samplesSent.addAndGet(batch.size.toLong())
                bytesTransmitted.addAndGet(batchMessage.toString().length.toLong())
                Log.d(TAG, "Sent GSR batch of ${batch.size} samples")
            } else {
                Log.w(TAG, "Failed to send GSR batch")
                networkErrors.incrementAndGet()

                batch.forEach { sample ->
                    sampleBuffer.offer(sample)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send GSR batch", e)
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
                                put("gsr_value", sample.gsrValue)
                                put("raw_value", sample.rawValue)
                                put("quality", sample.quality)
                                put("device_id", sample.deviceId)
                            },
                        )
                    }
                },
            )
        }
    }

    private suspend fun performTimeSync() {
        try {
            val syncRequest =
                JSONObject().apply {
                    put("type", "time_sync_request")
                    put("client_timestamp", System.nanoTime())
                }

            val response = networkClient?.sendRequestWithResponse(syncRequest)

            response?.let { resp ->
                val clientSent = resp.getLong("client_timestamp")
                val serverTime = resp.getLong("server_timestamp")
                val clientReceived = System.nanoTime()

                val roundTripTime = clientReceived - clientSent
                clockOffset = serverTime - (clientSent + roundTripTime / 2)
                lastSyncTime = System.currentTimeMillis()

                Log.d(TAG, "Time sync completed, offset: ${clockOffset}ns, RTT: ${roundTripTime}ns")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Time synchronization failed", e)
        }
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

                networkClient?.sendMessage(heartbeat)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send heartbeat", e)
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

                networkClient?.sendMessage(metrics)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send quality metrics", e)
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
                    put("sampling_rate", 128) // Hz
                    put("timestamp", System.currentTimeMillis())
                }

            networkClient?.sendMessage(registration)
            Log.i(TAG, "GSR stream registered with PC hub")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register GSR stream", e)
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

            networkClient?.sendMessage(endNotification)
            Log.i(TAG, "GSR stream end notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send stream end notification", e)
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
