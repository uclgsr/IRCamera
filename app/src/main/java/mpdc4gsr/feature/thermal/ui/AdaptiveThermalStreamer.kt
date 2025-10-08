package mpdc4gsr.feature.thermal.ui

import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import kotlin.math.max
import kotlin.math.min

class AdaptiveThermalStreamer {
    companion object {
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
            TAG,
            "Network client ${if (client != null) "set" else "cleared"} for thermal streaming"
        )
    }

    fun initialize() {
        startNetworkMonitoring()
    }

    fun startStreaming() {
        if (isStreamingEnabled) {
            return
        }
        isStreamingEnabled = true
        currentFrameCount = 0
    }

    fun stopStreaming() {
        if (!isStreamingEnabled) {
            return
        }
        isStreamingEnabled = false
        adaptationJob?.cancel()
        frameBuffer.clear()
        logFinalStatistics()
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
    }

    private fun streamFrame(frame: ThermalFrameData): Boolean {
        return (
            val startTime = System.currentTimeMillis()
            // Send thermal frame via network client using existing sendMessage API
                val frameMessage = frame.toNetworkMessage()
                val frameJson = JSONObject(frameMessage)
                networkClient?.let { client ->
                    // Use coroutine scope since sendMessage is suspend function
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val success = client.sendMessage(frameJson)
                            if (success) {
                            } else {
                            }
                        }
                    }
                } ?: run {
                    // Fallback to simulation if no network client available
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        simulateNetworkSend(frame)
                    }
                }
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    simulateNetworkSend(frame)
                }
            }
            val endTime = System.currentTimeMillis()
            val latency = endTime - startTime
            recordNetworkPerformance(latency, isPacketLost = false)
            true
            recordNetworkPerformance(1000L, isPacketLost = true)
            false
        }
    }

    private suspend fun simulateNetworkSend(frame: ThermalFrameData) {
        val simulatedLatency = (50..200).random()
        delay(simulatedLatency.toLong())
        if (Math.random() < 0.02) {
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
                    delay(ADAPTATION_INTERVAL_MS)
                    updateStreamingInterval()
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
            TAG, "Streaming Performance - Interval: ${stats["streaming_interval"]}, " +
                    "Efficiency: ${String.format("%.1f", stats["streaming_efficiency"])}%, " +
                    "Latency: ${stats["average_latency_ms"]}ms, " +
                    "Quality: ${stats["network_quality"]}"
        )
    }

    private fun logFinalStatistics() {
        val stats = getStreamingStatistics()
            TAG,
            "  Streaming efficiency: ${String.format("%.1f", stats["streaming_efficiency"])}%"
        )
            TAG,
            "  Packet loss rate: ${
                String.format(
                    "%.2f",
                    stats["packet_loss_rate"] as Double * 100
                )
            }%"
        )
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
    }
}