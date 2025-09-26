package mpdc4gsr.sensors.thermal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

/**
 * Adaptive Thermal Frame Streaming Optimization
 *
 * Implements dynamic frame interval adjustment based on network conditions
 * to balance bandwidth usage and latency for thermal camera streaming.
 *
 * Addresses TODO: "Optimize thermal frame streaming by making frame interval
 * adaptive or configurable to balance bandwidth and latency"
 */
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
    private var networkClient: mpdc4gsr.network.NetworkClient? = null

    fun setNetworkClient(client: mpdc4gsr.network.NetworkClient?) {
        networkClient = client"set" else "cleared"} for thermal streaming")
    }

    fun initialize() {        startNetworkMonitoring()    }


    fun startStreaming() {
        if (isStreamingEnabled) {            return
        }

        isStreamingEnabled = true
        currentFrameCount = 0    }


    fun stopStreaming() {
        if (!isStreamingEnabled) {
            return
        }

        isStreamingEnabled = false
        adaptationJob?.cancel()
        frameBuffer.clear()

        logFinalStatistics()    }


    fun processFrame(frameData: ThermalFrameData): Boolean {
        if (!isStreamingEnabled) {
            return false
        }

        totalFramesGenerated++
        currentFrameCount++


        val shouldStream = (currentFrameCount % streamingFrameInterval == 0)

        if (shouldStream) {
            return attemptFrameStreaming(frameData)
        } else {")
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
                    streamed = true")
                } else {

                    frameBuffer.offerFirst(frame)
                    break
                }
            }
        }

        return streamed
    }


    private fun handleBufferOverflow() {        var droppedCount = 0
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
        }    }

    /**
     * Stream individual frame (placeholder for actual streaming logic)
     */
    private fun streamFrame(frame: ThermalFrameData): Boolean {
        return try {

            val startTime = System.currentTimeMillis()

            // Send thermal frame via network client using existing sendMessage API
            try {
                val frameJson = JSONObject(frame.toNetworkMessage())
                networkClient?.let { client ->
                    // Use coroutine scope since sendMessage is suspend function
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val success = client.sendMessage(frameJson)
                            if (success) {                            } else {                            }
                        } catch (e: Exception) {                        }
                    }
                } ?: run {
                    // Fallback to simulation if no network client available
                    simulateNetworkSend(frame)
                }
            } catch (e: Exception) {                simulateNetworkSend(frame)
            }

            val endTime = System.currentTimeMillis()
            val latency = endTime - startTime


            recordNetworkPerformance(latency, isPacketLost = false)")
            true

        } catch (e: Exception) {            recordNetworkPerformance(1000L, isPacketLost = true)
            false
        }
    }

    /**
     * Simulate network streaming (replace with actual implementation)
     */
    private fun simulateNetworkSend(frame: ThermalFrameData) {

        val simulatedLatency = (50..200).random()
        Thread.sleep(simulatedLatency.toLong())


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
                } catch (e: Exception) {                }
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

        if (oldInterval != streamingFrameInterval) {}%)"
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
        val stats = getStreamingStatistics()}%, " +
                    "Latency: ${stats["average_latency_ms"]}ms, " +
                    "Quality: ${stats["network_quality"]}"
        )
    }


    private fun logFinalStatistics() {
        val stats = getStreamingStatistics()}%")}%")    }

    /**
     * Convert thermal frame data to network message format
     */
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
        streamingScope.cancel()    }
}