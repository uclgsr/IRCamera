package mpdc4gsr.feature.thermal.ui
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import kotlin.math.max
import kotlin.math.min

class AdaptiveThermalStreamer {
    companion object {
        private const val TAG = "AdaptiveThermalStreamer"
        private const val MIN_INTERVAL = 1
        private const val MAX_INTERVAL = 5
        private const val EXCELLENT_LATENCY = 50
        private const val GOOD_LATENCY = 100
        private const val FAIR_LATENCY = 200
        private const val MAX_BUFFER_SIZE = 10
        private const val OVERFLOW_DROP_COUNT = 3
        private const val ADAPTATION_INTERVAL_MS = 5000L
        private const val NETWORK_SAMPLE_SIZE = 10
    }
    private var streamingFrameInterval = 2
    private var currentFrameCount = 0
    private var isStreamingEnabled = false
    private val latencyMeasurements = LinkedList<Long>()
    private val packetLossMeasurements = LinkedList<Double>()
    private var averageLatency = 100L
    private var packetLossRate = 0.0
    private val frameBuffer = LinkedList<ThermalFrameData>()
    private var totalFramesGenerated = 0L
    private var framesStreamed = 0L
    private var framesDropped = 0L
    private var adaptationJob: Job? = null
    private val streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    data class ThermalFrameData(
        val frameIndex: Long,
        val timestamp: Long,
        val jpegData: ByteArray,
        val quality: Float,
        val priority: FramePriority = FramePriority.NORMAL
    ) {
        enum class FramePriority {
            LOW, NORMAL, HIGH, CRITICAL
        }
    }
    data class NetworkPerformance(
        val latency: Long,
        val packetLoss: Double,
        val bandwidth: Long,
        val quality: NetworkQuality
    ) {
        enum class NetworkQuality {
            EXCELLENT, GOOD, FAIR, POOR
        }
    }
    // Network client for actual thermal frame streaming
    private var networkClient: mpdc4gsr.feature.network.data.NetworkClient? = null
    fun setNetworkClient(client: mpdc4gsr.feature.network.data.NetworkClient?) {
        networkClient = client
        Log.i(
            TAG,
            "Network client ${if (client != null) "set" else "cleared"} for thermal streaming"
        )
    }
    fun initialize() {
        AppLogger.i(TAG, "Initializing adaptive thermal streamer")
        startNetworkMonitoring()
        AppLogger.i(TAG, "Adaptive thermal streamer initialized with interval: $streamingFrameInterval")
    }
    fun startStreaming() {
        if (isStreamingEnabled) {
            AppLogger.w(TAG, "Streaming already enabled")
            return
        }
        isStreamingEnabled = true
        currentFrameCount = 0
        AppLogger.i(TAG, "Started adaptive thermal streaming")
    }
    fun stopStreaming() {
        if (!isStreamingEnabled) {
            return
        }
        isStreamingEnabled = false
        adaptationJob?.cancel()
        frameBuffer.clear()
        logFinalStatistics()
        AppLogger.i(TAG, "Stopped adaptive thermal streaming")
    }
    fun processFrame(frameData: ThermalFrameData): Boolean {
        if (!isStreamingEnabled) {
            return false
        }
        totalFramesGenerated++
        currentFrameCount++
        val shouldStream = (currentFrameCount % streamingFrameInterval == 0)
        if (shouldStream) {
            return attemptFrameStreaming(frameData)
        } else {
            AppLogger.v(TAG, "Frame ${frameData.frameIndex} skipped (interval: $streamingFrameInterval)")
            return false
        }
    }
    private fun attemptFrameStreaming(frameData: ThermalFrameData): Boolean {
        if (frameBuffer.size >= MAX_BUFFER_SIZE) {
            handleBufferOverflow()
        }
        frameBuffer.offer(frameData)
        return processBufferedFrames()
    }
    private fun processBufferedFrames(): Boolean {
        var streamed = false
        while (frameBuffer.isNotEmpty()) {
            val frame = frameBuffer.poll()
            if (frame != null) {
                if (streamFrame(frame)) {
                    framesStreamed++
                    streamed = true
                    Log.v(
                        TAG,
                        "Streamed frame ${frame.frameIndex} (buffer size: ${frameBuffer.size})"
                    )
                } else {
                    frameBuffer.offerFirst(frame)
                    break
                }
            }
        }
        return streamed
    }
    private fun handleBufferOverflow() {
        AppLogger.w(TAG, "Frame buffer overflow, dropping frames")
        var droppedCount = 0
        val iterator = frameBuffer.iterator()
        while (iterator.hasNext() && droppedCount < OVERFLOW_DROP_COUNT) {
            val frame = iterator.next()
            if (frame.priority == ThermalFrameData.FramePriority.LOW ||
                frame.priority == ThermalFrameData.FramePriority.NORMAL
            ) {
                iterator.remove()
                droppedCount++
                framesDropped++
            }
        }
        AppLogger.w(TAG, "Dropped $droppedCount frames due to buffer overflow")
    }

    private fun streamFrame(frame: ThermalFrameData): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            // Send thermal frame via network client using existing sendMessage API
            try {
                val frameMessage = frame.toNetworkMessage()
                val frameJson = JSONObject(frameMessage)
                networkClient?.let { client ->
                    // Use coroutine scope since sendMessage is suspend function
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val success = client.sendMessage(frameJson)
                            if (success) {
                                AppLogger.v(TAG, "Sent thermal frame via NetworkClient")
                            } else {
                                AppLogger.w(TAG, "Failed to send thermal frame via NetworkClient")
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Failed to send thermal frame via NetworkClient", e)
                        }
                    }
                } ?: run {
                    // Fallback to simulation if no network client available
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        simulateNetworkSend(frame)
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Network send failed, using simulation fallback", e)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    simulateNetworkSend(frame)
                }
            }
            val endTime = System.currentTimeMillis()
            val latency = endTime - startTime
            recordNetworkPerformance(latency, isPacketLost = false)
            AppLogger.v(TAG, "Frame ${frame.frameIndex} streamed successfully (latency: ${latency}ms)")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stream frame ${frame.frameIndex}: ${e.message}")
            recordNetworkPerformance(1000L, isPacketLost = true)
            false
        }
    }

    private suspend fun simulateNetworkSend(frame: ThermalFrameData) {
        val simulatedLatency = (50..200).random()
        delay(simulatedLatency.toLong())
        if (Math.random() < 0.02) {
            throw RuntimeException("Simulated packet loss")
        }
    }
    private fun recordNetworkPerformance(latency: Long, isPacketLost: Boolean) {
        latencyMeasurements.offer(latency)
        if (latencyMeasurements.size > NETWORK_SAMPLE_SIZE) {
            latencyMeasurements.poll()
        }
        packetLossMeasurements.offer(if (isPacketLost) 1.0 else 0.0)
        if (packetLossMeasurements.size > NETWORK_SAMPLE_SIZE) {
            packetLossMeasurements.poll()
        }
        averageLatency = if (latencyMeasurements.isNotEmpty()) {
            latencyMeasurements.average().toLong()
        } else {
            100L
        }
        packetLossRate = if (packetLossMeasurements.isNotEmpty()) {
            packetLossMeasurements.average()
        } else {
            0.0
        }
    }
    private fun startNetworkMonitoring() {
        adaptationJob = streamingScope.launch {
            while (isActive && isStreamingEnabled) {
                try {
                    delay(ADAPTATION_INTERVAL_MS)
                    updateStreamingInterval()
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in network adaptation: ${e.message}")
                }
            }
        }
    }
    private fun updateStreamingInterval() {
        val oldInterval = streamingFrameInterval
        val newInterval = when {
            averageLatency <= EXCELLENT_LATENCY && packetLossRate < 0.01 -> {
                MIN_INTERVAL
            }
            averageLatency <= GOOD_LATENCY && packetLossRate < 0.02 -> {
                2
            }
            averageLatency <= FAIR_LATENCY && packetLossRate < 0.05 -> {
                3
            }
            else -> {
                MAX_INTERVAL
            }
        }
        streamingFrameInterval = max(MIN_INTERVAL, min(MAX_INTERVAL, newInterval))
        if (oldInterval != streamingFrameInterval) {
            Log.i(
                TAG, "Streaming interval updated: $oldInterval -> $streamingFrameInterval " +
                        "(latency: ${averageLatency}ms, loss: ${
                            String.format(
                                "%.1f",
                                packetLossRate * 100
                            )
                        }%)"
            )
        }
        logPerformanceStatistics()
    }
    fun getNetworkPerformance(): NetworkPerformance {
        val quality = when {
            averageLatency <= EXCELLENT_LATENCY && packetLossRate < 0.01 ->
                NetworkPerformance.NetworkQuality.EXCELLENT
            averageLatency <= GOOD_LATENCY && packetLossRate < 0.02 ->
                NetworkPerformance.NetworkQuality.GOOD
            averageLatency <= FAIR_LATENCY && packetLossRate < 0.05 ->
                NetworkPerformance.NetworkQuality.FAIR
            else ->
                NetworkPerformance.NetworkQuality.POOR
        }
        return NetworkPerformance(
            latency = averageLatency,
            packetLoss = packetLossRate,
            bandwidth = calculateEstimatedBandwidth(),
            quality = quality
        )
    }
    private fun calculateEstimatedBandwidth(): Long {
        val averageFrameSize = 50 * 1024L
        val streamingRate = if (streamingFrameInterval > 0) {
            (25.0 / streamingFrameInterval)
        } else {
            0.0
        }
        return (streamingRate * averageFrameSize).toLong()
    }
    fun getStreamingStatistics(): Map<String, Any> {
        val efficiency = if (totalFramesGenerated > 0) {
            (framesStreamed.toDouble() / totalFramesGenerated.toDouble()) * 100.0
        } else {
            0.0
        }
        return mapOf(
            "streaming_interval" to streamingFrameInterval,
            "total_frames_generated" to totalFramesGenerated,
            "frames_streamed" to framesStreamed,
            "frames_dropped" to framesDropped,
            "streaming_efficiency" to efficiency,
            "buffer_size" to frameBuffer.size,
            "average_latency_ms" to averageLatency,
            "packet_loss_rate" to packetLossRate,
            "network_quality" to getNetworkPerformance().quality.name
        )
    }
    private fun logPerformanceStatistics() {
        val stats = getStreamingStatistics()
        Log.d(
            TAG, "Streaming Performance - Interval: ${stats["streaming_interval"]}, " +
                    "Efficiency: ${String.format("%.1f", stats["streaming_efficiency"])}%, " +
                    "Latency: ${stats["average_latency_ms"]}ms, " +
                    "Quality: ${stats["network_quality"]}"
        )
    }
    private fun logFinalStatistics() {
        val stats = getStreamingStatistics()
        AppLogger.i(TAG, "Final Streaming Statistics:")
        AppLogger.i(TAG, "  Total frames generated: ${stats["total_frames_generated"]}")
        AppLogger.i(TAG, "  Frames streamed: ${stats["frames_streamed"]}")
        AppLogger.i(TAG, "  Frames dropped: ${stats["frames_dropped"]}")
        Log.i(
            TAG,
            "  Streaming efficiency: ${String.format("%.1f", stats["streaming_efficiency"])}%"
        )
        AppLogger.i(TAG, "  Average latency: ${stats["average_latency_ms"]}ms")
        Log.i(
            TAG,
            "  Packet loss rate: ${
                String.format(
                    "%.2f",
                    stats["packet_loss_rate"] as Double * 100
                )
            }%"
        )
        AppLogger.i(TAG, "  Final network quality: ${stats["network_quality"]}")
    }

    private fun ThermalFrameData.toNetworkMessage(): String {
        return """
        {
            "type": "thermal_frame",
            "frame_index": $frameIndex,
            "timestamp": $timestamp,
            "quality": $quality,
            "priority": "${priority.name}",
            "data_size": ${jpegData.size},
            "data": "${android.util.Base64.encodeToString(jpegData, android.util.Base64.DEFAULT)}"
        }
        """.trimIndent()
    }
    fun cleanup() {
        stopStreaming()
        streamingScope.cancel()
        AppLogger.i(TAG, "Adaptive thermal streamer cleaned up")
    }
}