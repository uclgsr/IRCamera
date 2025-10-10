package mpdc4gsr.feature.gsr.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.sensors.gsr.model.GSRSample
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.RecordingController
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class GSRNetworkStreamer(
    private val context: Context,
    private val sessionId: String,
    private val recordingController: RecordingController,
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

    private var networkClient: NetworkClient? = null
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
    private var clockOffset: Long = 0
    private var lastSyncTime: Long = 0
    private val syncInterval = 30000L

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                networkClient = NetworkClient(context)
                val connected = networkClient?.connectToController("192.168.1.100") ?: false
                if (!connected) {
                    return@withContext false
                }
                performTimeSync()
                registerGSRStream()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun startStreaming(): Boolean {
        if (_isStreaming.get()) {
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
            true
        } catch (e: Exception) {
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
            true
        } catch (e: Exception) {
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
                sampleBuffer.poll()
            }
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
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
                networkErrors.incrementAndGet()
                delay(1000)
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
            // Send actual network message using NetworkClient
            networkClient?.let { client ->
                streamingScope.launch {
                    try {
                        val success = client.sendMessage(batchMessage.toString())
                        if (success) {
                        }
                    } catch (e: Exception) {
                        mpdc4gsr.core.utils.AppLogger
                            .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
                    }
                }
            }
            samplesSent.addAndGet(batch.size.toLong())
            bytesTransmitted.addAndGet(batchMessage.toString().length.toLong())
        } catch (e: Exception) {
            networkErrors.incrementAndGet()
        }
    }

    private fun createBatchMessage(batch: List<GSRSample>): JSONObject =
        JSONObject().apply {
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
                                put("timestamp_ns", sample.timestamp)
                                put("timestamp_iso", sample.timestampIso)
                                put("gsr_microsiemens", sample.gsrMicrosiemens)
                                put("gsr_raw", sample.gsrRaw)
                                put("ppg_raw", sample.ppgRaw)
                                put("quality", sample.qualityScore)
                                put("connection_rssi", sample.connectionRssi)
                                put("resistance_ohms", sample.resistanceOhms)
                                put(
                                    "device_id",
                                    android.provider.Settings.Secure.getString(
                                        context.contentResolver,
                                        android.provider.Settings.Secure.ANDROID_ID,
                                    ),
                                )
                            },
                        )
                    }
                },
            )
        }

    private suspend fun performTimeSync() {
        try {
            val clientSent = System.nanoTime()
            val syncRequest =
                JSONObject().apply {
                    put("type", "time_sync_request")
                    put("client_timestamp", clientSent)
                }
            // Send actual time sync request using NetworkClient sendMessage
            networkClient?.let { client ->
                try {
                    val success = client.sendMessage(syncRequest)
                    if (success) {
                        // For now, use simple local time sync since we don't have a sync response mechanism
                        val clientReceived = System.nanoTime()
                        val roundTripTime = clientReceived - clientSent
                        clockOffset = 0 // Assume zero offset without server response
                        lastSyncTime = System.currentTimeMillis()
                    } else {
                        performLocalTimeSync(clientSent)
                    }
                } catch (e: Exception) {
                    performLocalTimeSync(clientSent)
                }
            } ?: run {
                performLocalTimeSync(clientSent)
            }
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
        }
    }

    private fun performLocalTimeSync(clientSent: Long) {
        val clientReceived = System.nanoTime()
        val roundTripTime = clientReceived - clientSent
        clockOffset = 0 // No server available, assume zero offset
        lastSyncTime = System.currentTimeMillis()
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
                // Send actual heartbeat using NetworkClient
                networkClient?.let { client ->
                    streamingScope.launch {
                        try {
                            val success = client.sendMessage(heartbeat)
                            if (success) {
                            }
                        } catch (e: Exception) {
                            mpdc4gsr.core.utils.AppLogger.e(
                                "GSRNetworkStreamer",
                                "Unexpected Exception in GSRNetworkStreamer catch block",
                                e,
                            )
                        }
                    }
                } ?: run {
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger
                    .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
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
                // Send actual quality metrics using NetworkClient
                networkClient?.let { client ->
                    streamingScope.launch {
                        try {
                            val success = client.sendMessage(metrics)
                            if (success) {
                            }
                        } catch (e: Exception) {
                            mpdc4gsr.core.utils.AppLogger.e(
                                "GSRNetworkStreamer",
                                "Unexpected Exception in GSRNetworkStreamer catch block",
                                e,
                            )
                        }
                    }
                } ?: run {
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger
                    .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
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
                    put("sampling_rate", 128)
                    put("timestamp", System.currentTimeMillis())
                }
            // Send actual stream registration using NetworkClient
            networkClient?.let { client ->
                streamingScope.launch {
                    try {
                        val success = client.sendMessage(registration)
                        if (success) {
                        } else {
                        }
                    } catch (e: Exception) {
                        mpdc4gsr.core.utils.AppLogger
                            .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
                    }
                }
            } ?: run {
            }
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
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
            // Send actual stream end notification using NetworkClient
            networkClient?.let { client ->
                streamingScope.launch {
                    try {
                        val success = client.sendMessage(endNotification)
                        if (success) {
                        } else {
                        }
                    } catch (e: Exception) {
                        mpdc4gsr.core.utils.AppLogger
                            .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
                    }
                }
            } ?: run {
            }
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("GSRNetworkStreamer", "Unexpected Exception in GSRNetworkStreamer catch block", e)
        }
    }

    private suspend fun flushBuffer() {
        while (sampleBuffer.isNotEmpty()) {
            sendBatch()
        }
    }

    private fun shouldFlushBuffer(): Boolean =
        sampleBuffer.isNotEmpty() &&
                (System.currentTimeMillis() % (STREAM_INTERVAL_MS * 5) == 0L)

    fun getStreamingStats(): Map<String, Any> =
        mapOf(
            "samples_sent" to samplesSent.get(),
            "samples_acknowledged" to samplesAcknowledged.get(),
            "bytes_transmitted" to bytesTransmitted.get(),
            "network_errors" to networkErrors.get(),
            "buffer_size" to sampleBuffer.size,
            "uptime_ms" to if (_isStreaming.get()) System.currentTimeMillis() - startTime else 0,
            "clock_offset_ns" to clockOffset,
        )

    suspend fun cleanup() {
        stopStreaming()
        streamingScope.cancel()
        networkClient?.disconnect()
        networkClient = null
    }
}
