package com.topdon.tc001.performance

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Performance Benchmark Manager
 * 
 * Implements comprehensive performance benchmarking and evaluation metrics
 * for the multi-modal recording system as specified in issue #6.
 * 
 * Measures:
 * - Data throughput and sampling rates
 * - Latency and synchronization accuracy
 * - Network performance
 * - System resource utilization
 */
class PerformanceBenchmarkManager {
    
    companion object {
        private const val TAG = "PerformanceBenchmark"
        
        // Benchmark thresholds and targets
        const val GSR_TARGET_SAMPLING_RATE = 128.0 // Hz
        const val RGB_TARGET_FRAME_RATE = 30.0 // fps
        const val THERMAL_TARGET_FRAME_RATE = 10.0 // fps
        const val ACCEPTABLE_SAMPLING_VARIANCE = 0.05 // 5%
        const val ACCEPTABLE_SYNC_DRIFT_MS = 5.0 // 5ms
        const val ACCEPTABLE_NETWORK_LATENCY_MS = 50.0 // 50ms
    }
    
    private val benchmarkResults = ConcurrentHashMap<String, BenchmarkResult>()
    private val activeBenchmarks = ConcurrentHashMap<String, BenchmarkSession>()
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    
    /**
     * Data structures for benchmark results
     */
    data class BenchmarkResult(
        val benchmarkId: String,
        val benchmarkType: BenchmarkType,
        val startTime: Long,
        val endTime: Long,
        val success: Boolean,
        val metrics: Map<String, Double>,
        val summary: String,
        val details: Map<String, Any> = emptyMap()
    )
    
    data class BenchmarkSession(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long? = null,
        val sampleTimestamps: MutableList<Long> = mutableListOf(),
        val metrics: MutableMap<String, Double> = mutableMapOf(),
        val isActive: Boolean = true
    )
    
    data class PerformanceMetric(
        val name: String,
        val value: Double,
        val unit: String,
        val timestamp: Long,
        val isWithinThreshold: Boolean,
        val threshold: Double? = null
    )
    
    enum class BenchmarkType {
        GSR_SAMPLING_RATE,
        RGB_FRAME_RATE,
        THERMAL_FRAME_RATE,
        NETWORK_THROUGHPUT,
        END_TO_END_LATENCY,
        TIMESTAMP_SYNC_ACCURACY,
        SYSTEM_RESOURCE_USAGE
    }
    
    /**
     * Start GSR sampling rate benchmark
     */
    fun startGSRSamplingRateBenchmark(sessionId: String): String {
        val benchmarkId = "gsr_sampling_${System.currentTimeMillis()}"
        val session = BenchmarkSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis()
        )
        
        activeBenchmarks[benchmarkId] = session
        Log.i(TAG, "Started GSR sampling rate benchmark: $benchmarkId")
        
        return benchmarkId
    }
    
    /**
     * Record GSR sample for benchmarking
     */
    fun recordGSRSample(benchmarkId: String, timestamp: Long) {
        activeBenchmarks[benchmarkId]?.let { session ->
            session.sampleTimestamps.add(timestamp)
            
            // Calculate current sampling rate if we have enough samples
            if (session.sampleTimestamps.size > 10) {
                val recentSamples = session.sampleTimestamps.takeLast(100)
                val timeSpanMs = recentSamples.last() - recentSamples.first()
                val currentSamplingRate = (recentSamples.size - 1) * 1000.0 / timeSpanMs
                
                session.metrics["current_sampling_rate"] = currentSamplingRate
                session.metrics["sample_count"] = session.sampleTimestamps.size.toDouble()
                
                // Log periodic updates
                if (session.sampleTimestamps.size % 128 == 0) {
                    Log.d(TAG, "GSR sampling rate: ${String.format("%.2f", currentSamplingRate)} Hz (target: $GSR_TARGET_SAMPLING_RATE Hz)")
                }
            }
        }
    }
    
    /**
     * Finalize GSR sampling rate benchmark
     */
    fun finalizeGSRSamplingRateBenchmark(benchmarkId: String): BenchmarkResult {
        val session = activeBenchmarks[benchmarkId] ?: throw IllegalArgumentException("Benchmark not found: $benchmarkId")
        
        val endTime = System.currentTimeMillis()
        val durationMs = endTime - session.startTime
        val totalSamples = session.sampleTimestamps.size
        
        // Calculate metrics
        val avgSamplingRate = if (durationMs > 0) totalSamples * 1000.0 / durationMs else 0.0
        val variance = calculateSamplingRateVariance(session.sampleTimestamps)
        val dropRate = calculateSampleDropRate(session.sampleTimestamps, durationMs)
        
        val metrics = mapOf(
            "average_sampling_rate_hz" to avgSamplingRate,
            "target_sampling_rate_hz" to GSR_TARGET_SAMPLING_RATE,
            "sampling_rate_variance" to variance,
            "sample_drop_rate_percent" to dropRate,
            "total_samples" to totalSamples.toDouble(),
            "duration_seconds" to durationMs / 1000.0,
            "deviation_from_target_percent" to abs(avgSamplingRate - GSR_TARGET_SAMPLING_RATE) / GSR_TARGET_SAMPLING_RATE * 100
        )
        
        val success = abs(avgSamplingRate - GSR_TARGET_SAMPLING_RATE) / GSR_TARGET_SAMPLING_RATE <= ACCEPTABLE_SAMPLING_VARIANCE
        
        val summary = if (success) {
            "✅ GSR sampling rate validation PASSED - achieved ${String.format("%.2f", avgSamplingRate)} Hz (target: $GSR_TARGET_SAMPLING_RATE Hz)"
        } else {
            "⚠️ GSR sampling rate validation WARNING - deviation ${String.format("%.1f", metrics["deviation_from_target_percent"]!!)}% from target"
        }
        
        val result = BenchmarkResult(
            benchmarkId = benchmarkId,
            benchmarkType = BenchmarkType.GSR_SAMPLING_RATE,
            startTime = session.startTime,
            endTime = endTime,
            success = success,
            metrics = metrics,
            summary = summary,
            details = mapOf(
                "sample_timestamps" to session.sampleTimestamps,
                "variance_analysis" to calculateVarianceAnalysis(session.sampleTimestamps)
            )
        )
        
        benchmarkResults[benchmarkId] = result
        activeBenchmarks.remove(benchmarkId)
        
        Log.i(TAG, "GSR sampling rate benchmark completed: $summary")
        return result
    }
    
    /**
     * Start RGB frame rate benchmark
     */
    fun startRGBFrameRateBenchmark(sessionId: String): String {
        val benchmarkId = "rgb_framerate_${System.currentTimeMillis()}"
        val session = BenchmarkSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis()
        )
        
        activeBenchmarks[benchmarkId] = session
        Log.i(TAG, "Started RGB frame rate benchmark: $benchmarkId")
        
        return benchmarkId
    }
    
    /**
     * Record RGB frame for benchmarking
     */
    fun recordRGBFrame(benchmarkId: String, frameTimestamp: Long, frameSize: Long = 0) {
        activeBenchmarks[benchmarkId]?.let { session ->
            session.sampleTimestamps.add(frameTimestamp)
            
            // Calculate current frame rate if we have enough frames
            if (session.sampleTimestamps.size > 5) {
                val recentFrames = session.sampleTimestamps.takeLast(150) // Last 5 seconds at 30fps
                if (recentFrames.size >= 2) {
                    val timeSpanMs = recentFrames.last() - recentFrames.first()
                    val currentFrameRate = (recentFrames.size - 1) * 1000.0 / timeSpanMs
                    
                    session.metrics["current_frame_rate"] = currentFrameRate
                    session.metrics["frame_count"] = session.sampleTimestamps.size.toDouble()
                    if (frameSize > 0) {
                        session.metrics["avg_frame_size_bytes"] = frameSize.toDouble()
                    }
                }
            }
        }
    }
    
    /**
     * Finalize RGB frame rate benchmark
     */
    fun finalizeRGBFrameRateBenchmark(benchmarkId: String, resolution: String = "unknown", targetFps: Double = RGB_TARGET_FRAME_RATE): BenchmarkResult {
        val session = activeBenchmarks[benchmarkId] ?: throw IllegalArgumentException("Benchmark not found: $benchmarkId")
        
        val endTime = System.currentTimeMillis()
        val durationMs = endTime - session.startTime
        val totalFrames = session.sampleTimestamps.size
        
        // Calculate metrics
        val avgFrameRate = if (durationMs > 0) totalFrames * 1000.0 / durationMs else 0.0
        val frameRateVariance = calculateSamplingRateVariance(session.sampleTimestamps)
        val dropRate = calculateFrameDropRate(session.sampleTimestamps, durationMs, targetFps)
        
        val metrics = mapOf(
            "average_frame_rate_fps" to avgFrameRate,
            "target_frame_rate_fps" to targetFps,
            "frame_rate_variance" to frameRateVariance,
            "frame_drop_rate_percent" to dropRate,
            "total_frames" to totalFrames.toDouble(),
            "duration_seconds" to durationMs / 1000.0,
            "deviation_from_target_percent" to abs(avgFrameRate - targetFps) / targetFps * 100
        )
        
        val success = abs(avgFrameRate - targetFps) / targetFps <= 0.2 // 20% tolerance for frame rate
        
        val summary = if (success) {
            "✅ RGB frame rate validation PASSED - achieved ${String.format("%.2f", avgFrameRate)} fps @ $resolution (target: ${String.format("%.0f", targetFps)} fps)"
        } else {
            "⚠️ RGB frame rate validation WARNING - deviation ${String.format("%.1f", metrics["deviation_from_target_percent"]!!)}% from target"
        }
        
        val result = BenchmarkResult(
            benchmarkId = benchmarkId,
            benchmarkType = BenchmarkType.RGB_FRAME_RATE,
            startTime = session.startTime,
            endTime = endTime,
            success = success,
            metrics = metrics,
            summary = summary,
            details = mapOf(
                "frame_timestamps" to session.sampleTimestamps,
                "resolution" to resolution,
                "variance_analysis" to calculateVarianceAnalysis(session.sampleTimestamps)
            )
        )
        
        benchmarkResults[benchmarkId] = result
        activeBenchmarks.remove(benchmarkId)
        
        Log.i(TAG, "RGB frame rate benchmark completed: $summary")
        return result
    }
    
    /**
     * Start network throughput benchmark
     */
    fun startNetworkThroughputBenchmark(sessionId: String): String {
        val benchmarkId = "network_throughput_${System.currentTimeMillis()}"
        val session = BenchmarkSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis()
        )
        
        session.metrics["bytes_sent"] = 0.0
        session.metrics["bytes_received"] = 0.0
        session.metrics["packets_sent"] = 0.0
        session.metrics["packets_received"] = 0.0
        
        activeBenchmarks[benchmarkId] = session
        Log.i(TAG, "Started network throughput benchmark: $benchmarkId")
        
        return benchmarkId
    }
    
    /**
     * Record network activity
     */
    fun recordNetworkActivity(benchmarkId: String, bytesSent: Long, bytesReceived: Long = 0, packetsSent: Int = 1, packetsReceived: Int = 0) {
        activeBenchmarks[benchmarkId]?.let { session ->
            session.metrics["bytes_sent"] = (session.metrics["bytes_sent"] ?: 0.0) + bytesSent
            session.metrics["bytes_received"] = (session.metrics["bytes_received"] ?: 0.0) + bytesReceived
            session.metrics["packets_sent"] = (session.metrics["packets_sent"] ?: 0.0) + packetsSent
            session.metrics["packets_received"] = (session.metrics["packets_received"] ?: 0.0) + packetsReceived
            
            session.sampleTimestamps.add(System.currentTimeMillis())
        }
    }
    
    /**
     * Finalize network throughput benchmark
     */
    fun finalizeNetworkThroughputBenchmark(benchmarkId: String): BenchmarkResult {
        val session = activeBenchmarks[benchmarkId] ?: throw IllegalArgumentException("Benchmark not found: $benchmarkId")
        
        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - session.startTime) / 1000.0
        
        val totalBytesSent = session.metrics["bytes_sent"] ?: 0.0
        val totalBytesReceived = session.metrics["bytes_received"] ?: 0.0
        val totalPacketsSent = session.metrics["packets_sent"] ?: 0.0
        val totalPacketsReceived = session.metrics["packets_received"] ?: 0.0
        
        val uploadThroughputKBps = if (durationSeconds > 0) totalBytesSent / 1024 / durationSeconds else 0.0
        val downloadThroughputKBps = if (durationSeconds > 0) totalBytesReceived / 1024 / durationSeconds else 0.0
        val totalThroughputKBps = uploadThroughputKBps + downloadThroughputKBps
        
        val metrics = mapOf(
            "upload_throughput_kbps" to uploadThroughputKBps,
            "download_throughput_kbps" to downloadThroughputKBps,
            "total_throughput_kbps" to totalThroughputKBps,
            "total_bytes_sent" to totalBytesSent,
            "total_bytes_received" to totalBytesReceived,
            "total_packets_sent" to totalPacketsSent,
            "total_packets_received" to totalPacketsReceived,
            "duration_seconds" to durationSeconds,
            "avg_packet_size_bytes" to if (totalPacketsSent > 0) totalBytesSent / totalPacketsSent else 0.0
        )
        
        val success = true // Network throughput is measured for information, not pass/fail
        
        val summary = "📊 Network throughput: ${String.format("%.1f", totalThroughputKBps)} KB/s " +
                     "(↑${String.format("%.1f", uploadThroughputKBps)} KB/s, ↓${String.format("%.1f", downloadThroughputKBps)} KB/s)"
        
        val result = BenchmarkResult(
            benchmarkId = benchmarkId,
            benchmarkType = BenchmarkType.NETWORK_THROUGHPUT,
            startTime = session.startTime,
            endTime = endTime,
            success = success,
            metrics = metrics,
            summary = summary
        )
        
        benchmarkResults[benchmarkId] = result
        activeBenchmarks.remove(benchmarkId)
        
        Log.i(TAG, "Network throughput benchmark completed: $summary")
        return result
    }
    
    /**
     * Measure timestamp synchronization accuracy
     */
    fun measureTimestampSyncAccuracy(sensorTimestamps: Map<String, Long>): BenchmarkResult {
        val benchmarkId = "sync_accuracy_${System.currentTimeMillis()}"
        val currentTime = System.currentTimeMillis()
        
        if (sensorTimestamps.size < 2) {
            return BenchmarkResult(
                benchmarkId = benchmarkId,
                benchmarkType = BenchmarkType.TIMESTAMP_SYNC_ACCURACY,
                startTime = currentTime,
                endTime = currentTime,
                success = false,
                metrics = emptyMap(),
                summary = "❌ Insufficient sensors for sync accuracy measurement"
            )
        }
        
        val timestamps = sensorTimestamps.values.map { it / 1_000_000.0 } // Convert to ms
        val maxTime = timestamps.maxOrNull() ?: 0.0
        val minTime = timestamps.minOrNull() ?: 0.0
        val drift = maxTime - minTime
        val avgTime = timestamps.average()
        val variance = timestamps.map { (it - avgTime) * (it - avgTime) }.average()
        val stdDev = sqrt(variance)
        
        val metrics = mapOf(
            "max_drift_ms" to drift,
            "avg_timestamp_ms" to avgTime,
            "timestamp_variance_ms2" to variance,
            "timestamp_stddev_ms" to stdDev,
            "sensor_count" to sensorTimestamps.size.toDouble()
        )
        
        val success = drift <= ACCEPTABLE_SYNC_DRIFT_MS
        
        val summary = if (success) {
            "✅ Timestamp synchronization PASSED - max drift: ${String.format("%.2f", drift)} ms"
        } else {
            "⚠️ Timestamp synchronization WARNING - drift ${String.format("%.2f", drift)} ms exceeds ${ACCEPTABLE_SYNC_DRIFT_MS} ms threshold"
        }
        
        val result = BenchmarkResult(
            benchmarkId = benchmarkId,
            benchmarkType = BenchmarkType.TIMESTAMP_SYNC_ACCURACY,
            startTime = currentTime,
            endTime = currentTime,
            success = success,
            metrics = metrics,
            summary = summary,
            details = mapOf(
                "sensor_timestamps" to sensorTimestamps,
                "timestamp_analysis" to mapOf(
                    "max_ms" to maxTime,
                    "min_ms" to minTime,
                    "range_ms" to drift,
                    "sensors" to sensorTimestamps.keys.toList()
                )
            )
        )
        
        benchmarkResults[benchmarkId] = result
        Log.i(TAG, "Timestamp sync accuracy measured: $summary")
        
        return result
    }
    
    /**
     * Calculate sampling rate variance
     */
    private fun calculateSamplingRateVariance(timestamps: List<Long>): Double {
        if (timestamps.size < 3) return 0.0
        
        val intervals = timestamps.zipWithNext { a, b -> b - a }.map { it.toDouble() }
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval) * (it - avgInterval) }.average()
        
        return variance / (avgInterval * avgInterval) // Normalized variance
    }
    
    /**
     * Calculate sample drop rate
     */
    private fun calculateSampleDropRate(timestamps: List<Long>, durationMs: Long): Double {
        if (durationMs == 0L) return 0.0
        
        val expectedSamples = (durationMs / 1000.0) * GSR_TARGET_SAMPLING_RATE
        val actualSamples = timestamps.size.toDouble()
        val dropRate = if (expectedSamples > 0) (expectedSamples - actualSamples) / expectedSamples * 100 else 0.0
        
        return maxOf(0.0, dropRate) // Don't report negative drop rates
    }
    
    /**
     * Calculate frame drop rate
     */
    private fun calculateFrameDropRate(timestamps: List<Long>, durationMs: Long, targetFps: Double): Double {
        if (durationMs == 0L) return 0.0
        
        val expectedFrames = (durationMs / 1000.0) * targetFps
        val actualFrames = timestamps.size.toDouble()
        val dropRate = if (expectedFrames > 0) (expectedFrames - actualFrames) / expectedFrames * 100 else 0.0
        
        return maxOf(0.0, dropRate) // Don't report negative drop rates
    }
    
    /**
     * Calculate detailed variance analysis
     */
    private fun calculateVarianceAnalysis(timestamps: List<Long>): Map<String, Double> {
        if (timestamps.size < 3) return emptyMap()
        
        val intervals = timestamps.zipWithNext { a, b -> b - a }.map { it.toDouble() }
        val avgInterval = intervals.average()
        val minInterval = intervals.minOrNull() ?: 0.0
        val maxInterval = intervals.maxOrNull() ?: 0.0
        val variance = intervals.map { (it - avgInterval) * (it - avgInterval) }.average()
        val stdDev = sqrt(variance)
        
        return mapOf(
            "avg_interval_ms" to avgInterval,
            "min_interval_ms" to minInterval,
            "max_interval_ms" to maxInterval,
            "interval_variance_ms2" to variance,
            "interval_stddev_ms" to stdDev,
            "coefficient_of_variation" to if (avgInterval > 0) stdDev / avgInterval else 0.0
        )
    }
    
    /**
     * Export all benchmark results to CSV
     */
    fun exportBenchmarkResults(outputDirectory: File): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(outputDirectory, "performance_benchmark_results_$timestamp.csv")
        
        outputFile.printWriter().use { writer ->
            writer.println("# Performance Benchmark Results Export")
            writer.println("# Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            writer.println("# Total Benchmarks: ${benchmarkResults.size}")
            writer.println("#")
            
            // Header
            writer.println("benchmark_id,benchmark_type,start_time,end_time,duration_ms,success,summary," +
                          "metric_name,metric_value,metric_unit")
            
            // Export each benchmark result with its metrics
            benchmarkResults.values.forEach { result ->
                val duration = result.endTime - result.startTime
                
                result.metrics.forEach { (metricName, metricValue) ->
                    writer.println("${result.benchmarkId},${result.benchmarkType}," +
                                  "${result.startTime},${result.endTime},$duration,${result.success}," +
                                  "\"${result.summary}\",\"$metricName\",$metricValue,\"\"")
                }
                
                // If no metrics, still export the benchmark
                if (result.metrics.isEmpty()) {
                    writer.println("${result.benchmarkId},${result.benchmarkType}," +
                                  "${result.startTime},${result.endTime},$duration,${result.success}," +
                                  "\"${result.summary}\",\"\",\"\",\"\"")
                }
            }
        }
        
        Log.i(TAG, "Benchmark results exported to: ${outputFile.absolutePath}")
        return outputFile
    }
    
    /**
     * Generate performance summary report
     */
    fun generatePerformanceSummary(): String {
        val summary = StringBuilder()
        summary.appendLine("=== PERFORMANCE BENCHMARK SUMMARY ===")
        summary.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        summary.appendLine("Total Benchmarks: ${benchmarkResults.size}")
        summary.appendLine("")
        
        // Group by benchmark type
        val resultsByType = benchmarkResults.values.groupBy { it.benchmarkType }
        
        resultsByType.forEach { (type, results) ->
            summary.appendLine("${type.name} (${results.size} tests):")
            
            val successCount = results.count { it.success }
            val successRate = if (results.isNotEmpty()) successCount * 100.0 / results.size else 0.0
            
            summary.appendLine("  Success Rate: $successCount/${results.size} (${String.format("%.1f", successRate)}%)")
            
            results.forEach { result ->
                summary.appendLine("  ${if (result.success) "✅" else "⚠️"} ${result.summary}")
            }
            summary.appendLine("")
        }
        
        // Overall system performance assessment
        val overallSuccess = benchmarkResults.values.count { it.success }
        val overallTotal = benchmarkResults.values.size
        val overallSuccessRate = if (overallTotal > 0) overallSuccess * 100.0 / overallTotal else 0.0
        
        summary.appendLine("=== OVERALL SYSTEM PERFORMANCE ===")
        summary.appendLine("Success Rate: $overallSuccess/$overallTotal (${String.format("%.1f", overallSuccessRate)}%)")
        
        when {
            overallSuccessRate >= 90 -> summary.appendLine("🟢 EXCELLENT - System meets performance requirements")
            overallSuccessRate >= 75 -> summary.appendLine("🟡 GOOD - System performance acceptable with minor issues")
            overallSuccessRate >= 50 -> summary.appendLine("🟠 FAIR - System performance needs improvement")
            else -> summary.appendLine("🔴 POOR - System performance requires significant optimization")
        }
        
        val summaryText = summary.toString()
        Log.i(TAG, "Performance summary generated:\n$summaryText")
        
        return summaryText
    }
    
    /**
     * Clear all benchmark results
     */
    fun clearResults() {
        benchmarkResults.clear()
        activeBenchmarks.clear()
        performanceMetrics.clear()
        Log.i(TAG, "All benchmark results cleared")
    }
    
    /**
     * Get all benchmark results
     */
    fun getAllResults(): Map<String, BenchmarkResult> = benchmarkResults.toMap()
    
    /**
     * Get results by type
     */
    fun getResultsByType(type: BenchmarkType): List<BenchmarkResult> {
        return benchmarkResults.values.filter { it.benchmarkType == type }
    }
}