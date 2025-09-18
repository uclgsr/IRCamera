package com.topdon.tc001.performance

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * Thermal Camera Performance Monitor
 * 
 * Monitors thermal camera performance metrics including:
 * - Frame rate measurement and validation
 * - Frame processing time analysis
 * - Temperature accuracy assessment
 * - Resource utilization tracking
 */
class ThermalPerformanceMonitor {
    
    companion object {
        private const val TAG = "ThermalPerformance"
        private const val TARGET_THERMAL_FPS = 10.0 // Hz
        private const val ACCEPTABLE_FPS_VARIANCE = 0.15 // 15%
        private const val FRAME_RATE_WINDOW_SIZE = 50 // frames for rolling average
    }
    
    private val frameTimestamps = ConcurrentLinkedQueue<Long>()
    private val frameProcessingTimes = ConcurrentLinkedQueue<Long>()
    private val temperatureReadings = ConcurrentLinkedQueue<ThermalFrame>()
    
    private val framesProcessed = AtomicLong(0)
    private val droppedFrames = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private val sessionStartTime = AtomicLong(0)
    
    private var isMonitoring = false
    
    data class ThermalFrame(
        val timestamp: Long,
        val minTemp: Float,
        val maxTemp: Float,
        val avgTemp: Float,
        val processingTimeMs: Long,
        val frameSize: Int,
        val qualityScore: Float = 1.0f
    )
    
    data class ThermalPerformanceMetrics(
        val averageFrameRate: Double,
        val frameRateVariance: Double,
        val averageProcessingTime: Double,
        val frameDropRate: Double,
        val temperatureRange: Pair<Float, Float>,
        val averageTemperature: Float,
        val thermalAccuracy: Float,
        val totalFramesProcessed: Long,
        val sessionDurationSeconds: Double,
        val isWithinTargetFps: Boolean
    )
    
    /**
     * Start monitoring thermal performance
     */
    fun startMonitoring() {
        isMonitoring = true
        sessionStartTime.set(System.currentTimeMillis())
        clearMetrics()
        Log.i(TAG, "Thermal performance monitoring started")
    }
    
    /**
     * Stop monitoring thermal performance
     */
    fun stopMonitoring(): ThermalPerformanceMetrics {
        isMonitoring = false
        val metrics = calculatePerformanceMetrics()
        Log.i(TAG, "Thermal performance monitoring stopped")
        logPerformanceSummary(metrics)
        return metrics
    }
    
    /**
     * Record a thermal frame for performance analysis
     */
    fun recordFrame(
        timestamp: Long,
        minTemp: Float,
        maxTemp: Float,
        avgTemp: Float,
        processingStartTime: Long,
        frameSize: Int,
        qualityScore: Float = 1.0f
    ) {
        if (!isMonitoring) return
        
        val processingTime = System.currentTimeMillis() - processingStartTime
        val frame = ThermalFrame(
            timestamp = timestamp,
            minTemp = minTemp,
            maxTemp = maxTemp,
            avgTemp = avgTemp,
            processingTimeMs = processingTime,
            frameSize = frameSize,
            qualityScore = qualityScore
        )
        
        // Record timing data
        frameTimestamps.offer(timestamp)
        frameProcessingTimes.offer(processingTime)
        temperatureReadings.offer(frame)
        
        // Maintain window size
        maintainWindowSize()
        
        val frameCount = framesProcessed.incrementAndGet()
        lastFrameTime.set(timestamp)
        
        // Detect dropped frames
        if (frameCount > 1) {
            val expectedInterval = 1000.0 / TARGET_THERMAL_FPS // ms
            val actualInterval = timestamp - (frameTimestamps.toList().getOrNull(frameTimestamps.size - 2) ?: timestamp)
            
            if (actualInterval > expectedInterval * 1.5) {
                val estimatedDropped = ((actualInterval - expectedInterval) / expectedInterval).toInt()
                droppedFrames.addAndGet(estimatedDropped.toLong())
                Log.w(TAG, "Detected $estimatedDropped dropped thermal frame(s) - interval: ${actualInterval}ms")
            }
        }
        
        // Log periodic updates
        if (frameCount % 50 == 0L) {
            val currentFps = calculateCurrentFrameRate()
            Log.d(TAG, "Thermal frames: $frameCount, FPS: ${String.format("%.2f", currentFps)}, " +
                      "Processing: ${processingTime}ms, Temp: ${String.format("%.1f", avgTemp)}°C")
        }
    }
    
    /**
     * Calculate current frame rate based on recent frames
     */
    fun calculateCurrentFrameRate(): Double {
        val timestamps = frameTimestamps.toList()
        if (timestamps.size < 2) return 0.0
        
        val recentFrames = timestamps.takeLast(FRAME_RATE_WINDOW_SIZE)
        if (recentFrames.size < 2) return 0.0
        
        val timeSpanMs = recentFrames.last() - recentFrames.first()
        return if (timeSpanMs > 0) (recentFrames.size - 1) * 1000.0 / timeSpanMs else 0.0
    }
    
    /**
     * Get current performance metrics without stopping monitoring
     */
    fun getCurrentMetrics(): ThermalPerformanceMetrics {
        return calculatePerformanceMetrics()
    }
    
    /**
     * Calculate comprehensive performance metrics
     */
    private fun calculatePerformanceMetrics(): ThermalPerformanceMetrics {
        val timestamps = frameTimestamps.toList()
        val processingTimes = frameProcessingTimes.toList()
        val thermalFrames = temperatureReadings.toList()
        
        val sessionDurationMs = if (sessionStartTime.get() > 0) {
            System.currentTimeMillis() - sessionStartTime.get()
        } else 0
        val sessionDurationSeconds = sessionDurationMs / 1000.0
        
        // Frame rate analysis
        val averageFrameRate = if (sessionDurationSeconds > 0 && timestamps.isNotEmpty()) {
            timestamps.size / sessionDurationSeconds
        } else 0.0
        
        val frameRateVariance = calculateFrameRateVariance(timestamps)
        
        // Processing time analysis
        val averageProcessingTime = if (processingTimes.isNotEmpty()) {
            processingTimes.average()
        } else 0.0
        
        // Temperature analysis
        val temperatures = thermalFrames.map { it.avgTemp }
        val temperatureRange = if (temperatures.isNotEmpty()) {
            Pair(temperatures.minOrNull() ?: 0f, temperatures.maxOrNull() ?: 0f)
        } else Pair(0f, 0f)
        
        val averageTemperature = temperatures.average().toFloat()
        
        // Calculate thermal accuracy (simplified - could be enhanced with calibration data)
        val thermalAccuracy = calculateThermalAccuracy(thermalFrames)
        
        // Frame drop analysis
        val totalExpectedFrames = if (sessionDurationSeconds > 0) {
            (sessionDurationSeconds * TARGET_THERMAL_FPS).toLong()
        } else timestamps.size.toLong()
        
        val actualFrames = framesProcessed.get()
        val frameDropRate = if (totalExpectedFrames > 0) {
            (totalExpectedFrames - actualFrames).toDouble() / totalExpectedFrames * 100
        } else 0.0
        
        val isWithinTargetFps = abs(averageFrameRate - TARGET_THERMAL_FPS) / TARGET_THERMAL_FPS <= ACCEPTABLE_FPS_VARIANCE
        
        return ThermalPerformanceMetrics(
            averageFrameRate = averageFrameRate,
            frameRateVariance = frameRateVariance,
            averageProcessingTime = averageProcessingTime,
            frameDropRate = maxOf(0.0, frameDropRate),
            temperatureRange = temperatureRange,
            averageTemperature = averageTemperature,
            thermalAccuracy = thermalAccuracy,
            totalFramesProcessed = actualFrames,
            sessionDurationSeconds = sessionDurationSeconds,
            isWithinTargetFps = isWithinTargetFps
        )
    }
    
    /**
     * Calculate frame rate variance
     */
    private fun calculateFrameRateVariance(timestamps: List<Long>): Double {
        if (timestamps.size < 3) return 0.0
        
        val intervals = timestamps.zipWithNext { a, b -> (b - a).toDouble() }
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval) * (it - avgInterval) }.average()
        
        return variance / (avgInterval * avgInterval) // Normalized variance
    }
    
    /**
     * Calculate thermal accuracy based on consistency and expected ranges
     */
    private fun calculateThermalAccuracy(frames: List<ThermalFrame>): Float {
        if (frames.isEmpty()) return 0f
        
        // Simple accuracy metric based on:
        // 1. Temperature consistency (low variance indicates good accuracy)
        // 2. Reasonable temperature ranges
        // 3. Quality scores from thermal processing
        
        val temperatures = frames.map { it.avgTemp }
        val tempVariance = if (temperatures.size > 1) {
            val avgTemp = temperatures.average()
            temperatures.map { (it - avgTemp) * (it - avgTemp) }.average()
        } else 0.0
        
        val qualityScores = frames.map { it.qualityScore }
        val avgQualityScore = qualityScores.average()
        
        // Normalize temperature variance (lower variance = higher accuracy)
        val temperatureStability = kotlin.math.exp(-tempVariance / 100.0) // Decay function for variance
        
        // Combine quality score and temperature stability
        val accuracy = (avgQualityScore * 0.6 + temperatureStability * 0.4).toFloat()
        
        return kotlin.math.min(1.0f, kotlin.math.max(0.0f, accuracy))
    }
    
    /**
     * Maintain window size for performance metrics
     */
    private fun maintainWindowSize() {
        val maxWindowSize = FRAME_RATE_WINDOW_SIZE * 2
        
        while (frameTimestamps.size > maxWindowSize) {
            frameTimestamps.poll()
        }
        
        while (frameProcessingTimes.size > maxWindowSize) {
            frameProcessingTimes.poll()
        }
        
        while (temperatureReadings.size > maxWindowSize) {
            temperatureReadings.poll()
        }
    }
    
    /**
     * Clear all metrics
     */
    private fun clearMetrics() {
        frameTimestamps.clear()
        frameProcessingTimes.clear()
        temperatureReadings.clear()
        framesProcessed.set(0)
        droppedFrames.set(0)
        lastFrameTime.set(0)
    }
    
    /**
     * Log performance summary
     */
    private fun logPerformanceSummary(metrics: ThermalPerformanceMetrics) {
        Log.i(TAG, "=== THERMAL PERFORMANCE SUMMARY ===")
        Log.i(TAG, "Session Duration: ${String.format("%.2f", metrics.sessionDurationSeconds)}s")
        Log.i(TAG, "Total Frames: ${metrics.totalFramesProcessed}")
        Log.i(TAG, "Average FPS: ${String.format("%.2f", metrics.averageFrameRate)} (target: $TARGET_THERMAL_FPS)")
        Log.i(TAG, "Frame Rate Variance: ${String.format("%.4f", metrics.frameRateVariance)}")
        Log.i(TAG, "Average Processing Time: ${String.format("%.2f", metrics.averageProcessingTime)}ms")
        Log.i(TAG, "Frame Drop Rate: ${String.format("%.2f", metrics.frameDropRate)}%")
        Log.i(TAG, "Temperature Range: ${String.format("%.1f", metrics.temperatureRange.first)}°C - ${String.format("%.1f", metrics.temperatureRange.second)}°C")
        Log.i(TAG, "Average Temperature: ${String.format("%.1f", metrics.averageTemperature)}°C")
        Log.i(TAG, "Thermal Accuracy: ${String.format("%.3f", metrics.thermalAccuracy)}")
        
        if (metrics.isWithinTargetFps) {
            Log.i(TAG, "✅ Thermal frame rate validation PASSED")
        } else {
            Log.w(TAG, "⚠️ Thermal frame rate validation WARNING - deviation from target")
        }
    }
    
    /**
     * Export thermal performance data to CSV
     */
    fun exportPerformanceData(outputFile: java.io.File): Boolean {
        return try {
            val frames = temperatureReadings.toList()
            val metrics = getCurrentMetrics()
            
            outputFile.printWriter().use { writer ->
                writer.println("# Thermal Performance Data Export")
                writer.println("# Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                writer.println("# Session Duration: ${String.format("%.2f", metrics.sessionDurationSeconds)}s")
                writer.println("# Average FPS: ${String.format("%.2f", metrics.averageFrameRate)}")
                writer.println("# Total Frames: ${metrics.totalFramesProcessed}")
                writer.println("#")
                
                writer.println("timestamp_ms,min_temp_c,max_temp_c,avg_temp_c,processing_time_ms,frame_size_bytes,quality_score")
                
                frames.forEach { frame ->
                    writer.println("${frame.timestamp},${frame.minTemp},${frame.maxTemp},${frame.avgTemp},${frame.processingTimeMs},${frame.frameSize},${frame.qualityScore}")
                }
            }
            
            Log.i(TAG, "Thermal performance data exported to: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting thermal performance data", e)
            false
        }
    }
    
    /**
     * Get diagnostic information for troubleshooting
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        val metrics = getCurrentMetrics()
        
        return mapOf(
            "is_monitoring" to isMonitoring,
            "session_start_time" to sessionStartTime.get(),
            "frames_processed" to framesProcessed.get(),
            "dropped_frames" to droppedFrames.get(),
            "current_fps" to calculateCurrentFrameRate(),
            "buffer_sizes" to mapOf(
                "timestamps" to frameTimestamps.size,
                "processing_times" to frameProcessingTimes.size,
                "thermal_frames" to temperatureReadings.size
            ),
            "performance_metrics" to metrics
        )
    }
}