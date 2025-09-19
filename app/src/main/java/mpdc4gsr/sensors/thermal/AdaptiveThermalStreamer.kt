package mpdc4gsr.sensors.thermal

import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.min
import kotlin.math.max

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
        
        // Streaming interval bounds
        private const val MIN_INTERVAL = 1  // Stream every frame
        private const val MAX_INTERVAL = 5  // Stream every 5th frame
        
        // Network performance thresholds (milliseconds)
        private const val EXCELLENT_LATENCY = 50
        private const val GOOD_LATENCY = 100
        private const val FAIR_LATENCY = 200
        
        // Buffer management
        private const val MAX_BUFFER_SIZE = 10
        private const val OVERFLOW_DROP_COUNT = 3
        
        // Adaptation timing
        private const val ADAPTATION_INTERVAL_MS = 5000L
        private const val NETWORK_SAMPLE_SIZE = 10
    }
    
    private var streamingFrameInterval = 2 // Start with moderate interval
    private var currentFrameCount = 0
    private var isStreamingEnabled = false
    
    // Network performance tracking
    private val latencyMeasurements = LinkedList<Long>()
    private val packetLossMeasurements = LinkedList<Double>()
    private var averageLatency = 100L
    private var packetLossRate = 0.0
    
    // Frame buffer management
    private val frameBuffer = LinkedList<ThermalFrameData>()
    private var totalFramesGenerated = 0L
    private var framesStreamed = 0L
    private var framesDropped = 0L
    
    // Coroutine management
    private var adaptationJob: Job? = null
    private val streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Data class for thermal frame information
     */
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
    
    /**
     * Network performance metrics
     */
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
    
    /**
     * Initialize adaptive streaming with network monitoring
     */
    fun initialize() {
        Log.i(TAG, "Initializing adaptive thermal streamer")
        
        // Start network performance monitoring
        startNetworkMonitoring()
        
        Log.i(TAG, "Adaptive thermal streamer initialized with interval: $streamingFrameInterval")
    }
    
    /**
     * Start adaptive streaming
     */
    fun startStreaming() {
        if (isStreamingEnabled) {
            Log.w(TAG, "Streaming already enabled")
            return
        }
        
        isStreamingEnabled = true
        currentFrameCount = 0
        
        Log.i(TAG, "Started adaptive thermal streaming")
    }
    
    /**
     * Stop streaming and cleanup
     */
    fun stopStreaming() {
        if (!isStreamingEnabled) {
            return
        }
        
        isStreamingEnabled = false
        adaptationJob?.cancel()
        frameBuffer.clear()
        
        logFinalStatistics()
        Log.i(TAG, "Stopped adaptive thermal streaming")
    }
    
    /**
     * Process new thermal frame for potential streaming
     */
    fun processFrame(frameData: ThermalFrameData): Boolean {
        if (!isStreamingEnabled) {
            return false
        }
        
        totalFramesGenerated++
        currentFrameCount++
        
        // Check if this frame should be streamed based on current interval
        val shouldStream = (currentFrameCount % streamingFrameInterval == 0)
        
        if (shouldStream) {
            return attemptFrameStreaming(frameData)
        } else {
            // Log dropped frame for statistics
            Log.v(TAG, "Frame ${frameData.frameIndex} skipped (interval: $streamingFrameInterval)")
            return false
        }
    }
    
    /**
     * Attempt to stream frame with buffer management
     */
    private fun attemptFrameStreaming(frameData: ThermalFrameData): Boolean {
        // Buffer management - prevent overflow
        if (frameBuffer.size >= MAX_BUFFER_SIZE) {
            handleBufferOverflow()
        }
        
        // Add frame to buffer
        frameBuffer.offer(frameData)
        
        // Attempt immediate streaming
        return processBufferedFrames()
    }
    
    /**
     * Process buffered frames for streaming
     */
    private fun processBufferedFrames(): Boolean {
        var streamed = false
        
        while (frameBuffer.isNotEmpty()) {
            val frame = frameBuffer.poll()
            if (frame != null) {
                if (streamFrame(frame)) {
                    framesStreamed++
                    streamed = true
                    
                    Log.v(TAG, "Streamed frame ${frame.frameIndex} (buffer size: ${frameBuffer.size})")
                } else {
                    // Failed to stream - add back to front of buffer for retry
                    frameBuffer.offerFirst(frame)
                    break
                }
            }
        }
        
        return streamed
    }
    
    /**
     * Handle buffer overflow by dropping low priority frames
     */
    private fun handleBufferOverflow() {
        Log.w(TAG, "Frame buffer overflow, dropping frames")
        
        var droppedCount = 0
        val iterator = frameBuffer.iterator()
        
        // Drop oldest low priority frames first
        while (iterator.hasNext() && droppedCount < OVERFLOW_DROP_COUNT) {
            val frame = iterator.next()
            if (frame.priority == ThermalFrameData.FramePriority.LOW || 
                frame.priority == ThermalFrameData.FramePriority.NORMAL) {
                iterator.remove()
                droppedCount++
                framesDropped++
            }
        }
        
        Log.w(TAG, "Dropped $droppedCount frames due to buffer overflow")
    }
    
    /**
     * Stream individual frame (placeholder for actual streaming logic)
     */
    private fun streamFrame(frame: ThermalFrameData): Boolean {
        return try {
            // Simulate network streaming with latency measurement
            val startTime = System.currentTimeMillis()
            
            // TODO: Replace with actual network streaming implementation
            // This would typically send frame.jpegData to PC controller
            simulateNetworkSend(frame)
            
            val endTime = System.currentTimeMillis()
            val latency = endTime - startTime
            
            // Record network performance
            recordNetworkPerformance(latency, isPacketLost = false)
            
            Log.v(TAG, "Frame ${frame.frameIndex} streamed successfully (latency: ${latency}ms)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stream frame ${frame.frameIndex}: ${e.message}")
            recordNetworkPerformance(1000L, isPacketLost = true)
            false
        }
    }
    
    /**
     * Simulate network streaming (replace with actual implementation)
     */
    private fun simulateNetworkSend(frame: ThermalFrameData) {
        // Simulate variable network latency
        val simulatedLatency = (50..200).random()
        Thread.sleep(simulatedLatency.toLong())
        
        // Simulate occasional packet loss
        if (Math.random() < 0.02) { // 2% packet loss simulation
            throw RuntimeException("Simulated packet loss")
        }
    }
    
    /**
     * Record network performance metrics
     */
    private fun recordNetworkPerformance(latency: Long, isPacketLost: Boolean) {
        // Add latency measurement
        latencyMeasurements.offer(latency)
        if (latencyMeasurements.size > NETWORK_SAMPLE_SIZE) {
            latencyMeasurements.poll()
        }
        
        // Add packet loss measurement
        packetLossMeasurements.offer(if (isPacketLost) 1.0 else 0.0)
        if (packetLossMeasurements.size > NETWORK_SAMPLE_SIZE) {
            packetLossMeasurements.poll()
        }
        
        // Update averages
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
    
    /**
     * Start network performance monitoring and adaptation
     */
    private fun startNetworkMonitoring() {
        adaptationJob = streamingScope.launch {
            while (isActive && isStreamingEnabled) {
                try {
                    delay(ADAPTATION_INTERVAL_MS)
                    updateStreamingInterval()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in network adaptation: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Update streaming interval based on network performance
     */
    private fun updateStreamingInterval() {
        val oldInterval = streamingFrameInterval
        
        // Calculate new interval based on network conditions
        val newInterval = when {
            averageLatency <= EXCELLENT_LATENCY && packetLossRate < 0.01 -> {
                MIN_INTERVAL // Stream every frame for excellent network
            }
            averageLatency <= GOOD_LATENCY && packetLossRate < 0.02 -> {
                2 // Stream every 2nd frame for good network
            }
            averageLatency <= FAIR_LATENCY && packetLossRate < 0.05 -> {
                3 // Stream every 3rd frame for fair network
            }
            else -> {
                MAX_INTERVAL // Conservative streaming for poor network
            }
        }
        
        // Apply interval with bounds checking
        streamingFrameInterval = max(MIN_INTERVAL, min(MAX_INTERVAL, newInterval))
        
        if (oldInterval != streamingFrameInterval) {
            Log.i(TAG, "Streaming interval updated: $oldInterval -> $streamingFrameInterval " +
                    "(latency: ${averageLatency}ms, loss: ${String.format("%.1f", packetLossRate * 100)}%)")
        }
        
        // Log performance statistics
        logPerformanceStatistics()
    }
    
    /**
     * Get current network performance metrics
     */
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
    
    /**
     * Calculate estimated bandwidth based on frame streaming
     */
    private fun calculateEstimatedBandwidth(): Long {
        // Estimate based on frames streamed and average frame size
        val averageFrameSize = 50 * 1024L // Assume ~50KB per JPEG frame
        val streamingRate = if (streamingFrameInterval > 0) {
            (25.0 / streamingFrameInterval) // Base frame rate / interval
        } else {
            0.0
        }
        
        return (streamingRate * averageFrameSize).toLong()
    }
    
    /**
     * Get current streaming statistics
     */
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
    
    /**
     * Log performance statistics
     */
    private fun logPerformanceStatistics() {
        val stats = getStreamingStatistics()
        
        Log.d(TAG, "Streaming Performance - Interval: ${stats["streaming_interval"]}, " +
                "Efficiency: ${String.format("%.1f", stats["streaming_efficiency"])}%, " +
                "Latency: ${stats["average_latency_ms"]}ms, " +
                "Quality: ${stats["network_quality"]}")
    }
    
    /**
     * Log final statistics on shutdown
     */
    private fun logFinalStatistics() {
        val stats = getStreamingStatistics()
        
        Log.i(TAG, "Final Streaming Statistics:")
        Log.i(TAG, "  Total frames generated: ${stats["total_frames_generated"]}")
        Log.i(TAG, "  Frames streamed: ${stats["frames_streamed"]}")
        Log.i(TAG, "  Frames dropped: ${stats["frames_dropped"]}")
        Log.i(TAG, "  Streaming efficiency: ${String.format("%.1f", stats["streaming_efficiency"])}%")
        Log.i(TAG, "  Average latency: ${stats["average_latency_ms"]}ms")
        Log.i(TAG, "  Packet loss rate: ${String.format("%.2f", stats["packet_loss_rate"] as Double * 100)}%")
        Log.i(TAG, "  Final network quality: ${stats["network_quality"]}")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopStreaming()
        streamingScope.cancel()
        Log.i(TAG, "Adaptive thermal streamer cleaned up")
    }
}