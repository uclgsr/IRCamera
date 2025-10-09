package mpdc4gsr.core.data

import android.util.Log
import kotlinx.coroutines.*
import mpdc4gsr.core.sensors.gsr.model.GSRSample
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LSLGSROutlet(
    private val streamName: String = "GSR-Shimmer3",
    private val deviceId: String = "shimmer-default",
    private val sessionId: String? = null,
    private val serverPort: Int = 9001
) {
    companion object {
        private const val TAG = "LSLGSROutlet"
        private const val STREAM_TYPE = "GSR"
        private const val CHANNEL_COUNT = 4 // Raw GSR, Calibrated GSR, PPG, Timestamp
        private const val SAMPLE_RATE = 128.0
        private const val CHANNEL_FORMAT = "float32"
        private const val BUFFER_SIZE = 1000
        private const val BATCH_SIZE = 10
        private const val QUALITY_HISTORY_SIZE = 100
        private const val MIN_QUALITY_THRESHOLD = 0.7
    }

    data class LSLStreamInfo(
        val name: String,
        val type: String,
        val channelCount: Int,
        val sampleRate: Double,
        val channelFormat: String,
        val sourceId: String,
        val hostname: String = "IRCamera-Android",
        val sessionId: String? = null
    )

    inner class LSLStreamOutlet(private val streamInfo: LSLStreamInfo) {
        private val isActive = AtomicBoolean(false)
        private var startTime = 0L
        private val sampleCount = AtomicLong(0)
        private var serverSocket: ServerSocket? = null
        private val connectedClients = mutableSetOf<Socket>()
        private val networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var serverJob: Job? = null
        fun open(): Boolean {
            return try {
                startTime = System.currentTimeMillis()
                // Start TCP server for LSL streaming
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(serverPort))
                }
                isActive.set(true)
                // Start accepting client connections
                serverJob = networkScope.launch {
                    acceptConnections()
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        private suspend fun acceptConnections() {
            while (isActive.get()) {
                try {
                    val clientSocket = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    }
                    clientSocket?.let { socket ->
                        synchronized(connectedClients) {
                            connectedClients.add(socket)
                        }
                        // Send stream info to new client
                        sendStreamInfo(socket)
                        // Handle client disconnection monitoring
                        networkScope.launch {
                            try {
                                socket.inputStream.read() // Block until client disconnects
                            } catch (e: Exception) {
                                // Client disconnected
                            } finally {
                                synchronized(connectedClients) {
                                    connectedClients.remove(socket)
                                }
                                socket.close()
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive.get()) {
                        delay(1000) // Brief pause before retrying
                    }
                }
            }
        }

        private fun sendStreamInfo(socket: Socket) {
            try {
                val writer = PrintWriter(socket.outputStream, true)
                val streamInfoJson = JSONObject().apply {
                    put("type", "stream_info")
                    put("name", streamInfo.name)
                    put("stream_type", streamInfo.type)
                    put("channel_count", streamInfo.channelCount)
                    put("sample_rate", streamInfo.sampleRate)
                    put("channel_format", streamInfo.channelFormat)
                    put("source_id", streamInfo.sourceId)
                    put("hostname", streamInfo.hostname)
                    put("session_id", streamInfo.sessionId)
                    put("channels", JSONArray().apply {
                        put("GSR_Raw")
                        put("GSR_Calibrated")
                        put("PPG")
                        put("Timestamp")
                    })
                }
                writer.println(streamInfoJson.toString())
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("LSLGSROutlet", "Unexpected Exception in LSLGSROutlet catch block", e)
            }
        }

        fun pushSample(sample: FloatArray): Boolean {
            if (!isActive.get() || sample.size != streamInfo.channelCount) {
                return false
            }
            return try {
                val timestamp = System.currentTimeMillis()
                val lslSample = JSONObject().apply {
                    put("type", "sample")
                    put("timestamp", timestamp)
                    put("sample_count", sampleCount.incrementAndGet())
                    put("data", JSONArray().apply {
                        sample.forEach { put(it) }
                    })
                }
                // Send to all connected clients
                synchronized(connectedClients) {
                    val iterator = connectedClients.iterator()
                    while (iterator.hasNext()) {
                        val client = iterator.next()
                        try {
                            val writer = PrintWriter(client.outputStream, true)
                            writer.println(lslSample.toString())
                        } catch (e: Exception) {
                            // Remove disconnected client
                            iterator.remove()
                            client.close()
                        }
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        fun pushChunk(samples: Array<FloatArray>): Boolean {
            if (!isActive.get()) {
                return false
            }
            return try {
                val timestamp = System.currentTimeMillis()
                val lslChunk = JSONObject().apply {
                    put("type", "chunk")
                    put("timestamp", timestamp)
                    put("sample_count", samples.size)
                    put("data", JSONArray().apply {
                        samples.forEach { sample ->
                            val sampleArray = JSONArray()
                            sample.forEach { sampleArray.put(it) }
                            put(sampleArray)
                        }
                    })
                }
                // Send to all connected clients
                synchronized(connectedClients) {
                    val iterator = connectedClients.iterator()
                    while (iterator.hasNext()) {
                        val client = iterator.next()
                        try {
                            val writer = PrintWriter(client.outputStream, true)
                            writer.println(lslChunk.toString())
                        } catch (e: Exception) {
                            // Remove disconnected client
                            iterator.remove()
                            client.close()
                        }
                    }
                }
                sampleCount.addAndGet(samples.size.toLong())
                true
            } catch (e: Exception) {
                false
            }
        }

        fun close() {
            isActive.set(false)
            // Close all client connections
            synchronized(connectedClients) {
                connectedClients.forEach { it.close() }
                connectedClients.clear()
            }
            // Close server socket
            serverSocket?.close()
            serverJob?.cancel()
        }

        fun getSampleCount(): Long = sampleCount.get()
        fun getUptimeMs(): Long = System.currentTimeMillis() - startTime
        fun getConnectedClients(): Int = synchronized(connectedClients) { connectedClients.size }
    }

    // Stream configuration
    private val streamInfo = LSLStreamInfo(
        name = streamName,
        type = STREAM_TYPE,
        channelCount = CHANNEL_COUNT,
        sampleRate = SAMPLE_RATE,
        channelFormat = CHANNEL_FORMAT,
        sourceId = deviceId,
        sessionId = sessionId
    )
    private var outlet: LSLStreamOutlet? = null
    private val sampleBuffer = ConcurrentLinkedQueue<GSRSample>()
    private val qualityHistory = ConcurrentLinkedQueue<Double>()
    private val isStreaming = AtomicBoolean(false)
    private val samplesSent = AtomicLong(0)
    private val networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamingJob: Job? = null
    fun startStreaming(): Boolean {
        return try {
            outlet = LSLStreamOutlet(streamInfo)
            if (outlet?.open() == true) {
                isStreaming.set(true)
                // Start streaming loop
                streamingJob = networkScope.launch {
                    streamingLoop()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun streamingLoop() {
        while (isStreaming.get()) {
            try {
                val samplesToSend = mutableListOf<GSRSample>()
                // Collect batch of samples
                repeat(BATCH_SIZE) {
                    sampleBuffer.poll()?.let { samplesToSend.add(it) }
                }
                if (samplesToSend.isNotEmpty()) {
                    // Convert GSR samples to LSL format
                    val lslSamples = samplesToSend.map { sample ->
                        floatArrayOf(
                            sample.gsrRaw.toFloat(),
                            sample.gsrMicrosiemens.toFloat(),
                            sample.ppgRaw.toFloat(),
                            sample.timestamp.toFloat()
                        )
                    }.toTypedArray()
                    // Send chunk to LSL
                    outlet?.pushChunk(lslSamples)?.let { success ->
                        if (success) {
                            samplesSent.addAndGet(samplesToSend.size.toLong())
                        }
                    }
                }
                delay(50) // 20 Hz streaming rate
            } catch (e: Exception) {
                delay(1000)
            }
        }
    }

    fun pushSample(sample: GSRSample) {
        if (isStreaming.get()) {
            // Add to buffer for batch processing
            sampleBuffer.offer(sample)
            // Keep buffer size manageable
            while (sampleBuffer.size > BUFFER_SIZE) {
                sampleBuffer.poll()
            }
            // Update quality metrics
            updateQualityMetrics(sample)
        }
    }

    private fun updateQualityMetrics(sample: GSRSample) {
        // Simple quality metric based on signal stability
        val quality = if (sample.gsrMicrosiemens in 0.1..10.0) 1.0 else 0.0
        qualityHistory.offer(quality)
        while (qualityHistory.size > QUALITY_HISTORY_SIZE) {
            qualityHistory.poll()
        }
    }

    fun stopStreaming() {
        isStreaming.set(false)
        streamingJob?.cancel()
        outlet?.close()
        outlet = null
        sampleBuffer.clear()
        qualityHistory.clear()
    }

    fun getStreamingStatistics(): Map<String, Any> {
        return mapOf(
            "is_streaming" to isStreaming.get(),
            "samples_sent" to samplesSent.get(),
            "buffer_size" to sampleBuffer.size,
            "connected_clients" to (outlet?.getConnectedClients() ?: 0),
            "uptime_ms" to (outlet?.getUptimeMs() ?: 0),
            "quality_score" to getAverageQuality()
        )
    }

    private fun getAverageQuality(): Double {
        return if (qualityHistory.isEmpty()) {
            0.0
        } else {
            qualityHistory.average()
        }
    }

    fun isStreamingActive(): Boolean = isStreaming.get()
    fun getSamplesSent(): Long = samplesSent.get()
    fun getBufferSize(): Int = sampleBuffer.size
}
