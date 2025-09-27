package com.topdon.tc001.performance

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Simple Performance Benchmark Manager (MVP)
 * 
 * Focused MVP implementation for core performance benchmarking:
 * - GSR sampling rate validation (128Hz target)
 * - RGB frame rate monitoring (30fps target)
 * - Basic synchronization drift measurement (<5ms target)
 */
class SimpleBenchmarkManager {
    
    companion object {
        private const val TAG = "SimpleBenchmark"
        const val GSR_TARGET_RATE = 128.0 // Hz
        const val RGB_TARGET_RATE = 30.0 // fps
        const val MAX_SYNC_DRIFT_MS = 5.0 // ms
        const val GSR_RATE_TOLERANCE = 0.05
        const val RGB_RATE_TOLERANCE = 0.2
    }
    
    // GSR benchmark data
    private val gsrSamples = mutableListOf<Long>()
    private var gsrSessionStart = 0L
    
    // RGB benchmark data
    private val rgbFrames = mutableListOf<Long>()
    private var rgbSessionStart = 0L
    
    // Results storage
    data class BenchmarkResult(
        val type: String,
        val success: Boolean,
        val actualRate: Double,
        val targetRate: Double,
        val summary: String
    )
    
    /**
     * Start GSR sampling rate benchmark
     */
    fun startGSRBenchmark(): Boolean {
        gsrSamples.clear()
        gsrSessionStart = SystemClock.elapsedRealtime()
        Log.i(TAG, "Started GSR benchmark - target: $GSR_TARGET_RATE Hz")
        return true
    }
    
    /**
     * Record GSR sample timestamp
     */
    fun recordGSRSample() {
        if (gsrSessionStart > 0) {
            gsrSamples.add(SystemClock.elapsedRealtime())
        }
    }
    
    /**
     * Stop and evaluate GSR benchmark
     */
    fun stopGSRBenchmark(): BenchmarkResult {
        val endTime = SystemClock.elapsedRealtime()
        val durationMs = if (gsrSessionStart > 0) endTime - gsrSessionStart else 0L
        val sampleCount = gsrSamples.size
        
        val actualRate = if (durationMs > 0) {
            (sampleCount * 1000.0) / durationMs
        } else 0.0
        
        val rateError = abs(actualRate - GSR_TARGET_RATE) / GSR_TARGET_RATE
        val success = rateError <= GSR_RATE_TOLERANCE // 5% tolerance
        
        val summary = if (success) {
            "✅ GSR: ${String.format("%.2f", actualRate)} Hz - PASSED"
        } else {
            "⚠️ GSR: ${String.format("%.2f", actualRate)} Hz - FAILED (target: $GSR_TARGET_RATE Hz)"
        }
        
        Log.i(TAG, summary)
        gsrSessionStart = 0
        
        return BenchmarkResult("GSR", success, actualRate, GSR_TARGET_RATE, summary)
    }
    
    /**
     * Start RGB frame rate benchmark
     */
    fun startRGBBenchmark(): Boolean {
        rgbFrames.clear()
        rgbSessionStart = SystemClock.elapsedRealtime()
        Log.i(TAG, "Started RGB benchmark - target: $RGB_TARGET_RATE fps")
        return true
    }
    
    /**
     * Record RGB frame timestamp
     */
    fun recordRGBFrame() {
        if (rgbSessionStart > 0) {
            rgbFrames.add(SystemClock.elapsedRealtime())
        }
    }
    
    /**
     * Stop and evaluate RGB benchmark
     */
    fun stopRGBBenchmark(): BenchmarkResult {
        val endTime = SystemClock.elapsedRealtime()
        val durationMs = endTime - rgbSessionStart
        val frameCount = rgbFrames.size
        
        val actualRate = if (durationMs > 0) {
            (frameCount * 1000.0) / durationMs
        } else 0.0
        
        val rateError = abs(actualRate - RGB_TARGET_RATE) / RGB_TARGET_RATE
        val success = rateError <= RGB_RATE_TOLERANCE // 20% tolerance for frames
        
        val summary = if (success) {
            "✅ RGB: ${String.format("%.2f", actualRate)} fps - PASSED"
        } else {
            "⚠️ RGB: ${String.format("%.2f", actualRate)} fps - FAILED (target: $RGB_TARGET_RATE fps)"
        }
        
        Log.i(TAG, summary)
        rgbSessionStart = 0
        
        return BenchmarkResult("RGB", success, actualRate, RGB_TARGET_RATE, summary)
    }
    
    /**
     * Check synchronization drift between sensor timestamps
     */
    fun checkSyncDrift(gsrTimestamp: Long, rgbTimestamp: Long, thermalTimestamp: Long? = null): BenchmarkResult {
        val timestamps = listOfNotNull(gsrTimestamp, rgbTimestamp, thermalTimestamp)
        
        if (timestamps.size < 2) {
            return BenchmarkResult("SYNC", false, 0.0, 0.0, "❌ Insufficient timestamps for sync check")
        }
        
        val maxTime = timestamps.maxOrNull() ?: 0L
        val minTime = timestamps.minOrNull() ?: 0L
        val driftMs = (maxTime - minTime) / 1_000_000.0 // Convert nanoseconds to ms
        
        val success = driftMs <= MAX_SYNC_DRIFT_MS
        
        val summary = if (success) {
            "✅ Sync drift: ${String.format("%.2f", driftMs)} ms - PASSED"
        } else {
            "⚠️ Sync drift: ${String.format("%.2f", driftMs)} ms - FAILED (max: $MAX_SYNC_DRIFT_MS ms)"
        }
        
        Log.i(TAG, summary)
        
        return BenchmarkResult("SYNC", success, driftMs, MAX_SYNC_DRIFT_MS, summary)
    }
    
    /**
     * Export benchmark results to simple CSV
     */
    fun exportResults(results: List<BenchmarkResult>, outputDir: File): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFile = File(outputDir, "simple_benchmark_results_$timestamp.csv")
            
            csvFile.printWriter().use { writer ->
                writer.println("# Simple Performance Benchmark Results")
                writer.println("# Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.println("#")
                writer.println("test_type,success,actual_rate,target_rate,summary")
                
                results.forEach { result ->
                    writer.println("${result.type},${result.success},${result.actualRate},${result.targetRate},\"${result.summary}\"")
                }
            }
            
            Log.i(TAG, "Results exported to: ${csvFile.absolutePath}")
            csvFile
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting results", e)
            null
        }
    }
    
    /**
     * Run a quick MVP benchmark test
     */
    suspend fun runQuickBenchmark(): List<BenchmarkResult> {
        val results = mutableListOf<BenchmarkResult>()
        
        Log.i(TAG, "=== Running Quick MVP Benchmark ===")
        
        // Test GSR sampling (simulate 5 seconds)
        startGSRBenchmark()
        repeat(640) { // 128 samples per second * 5 seconds
            recordGSRSample()
            kotlinx.coroutines.delay(8) // ~125Hz simulation
        }
        results.add(stopGSRBenchmark())
        
        // Test RGB frame rate (simulate 3 seconds)  
        startRGBBenchmark()
        repeat(90) { // 30 frames per second * 3 seconds
            recordRGBFrame()
            kotlinx.coroutines.delay(33) // ~30fps simulation
        }
        results.add(stopRGBBenchmark())
        
        // Test sync drift
        val baseTime = System.nanoTime()
        val syncResult = checkSyncDrift(
            baseTime,
            baseTime + 2_000_000, // 2ms drift
            baseTime + 1_000_000  // 1ms drift
        )
        results.add(syncResult)
        
        val passedTests = results.count { it.success }
        val totalTests = results.size
        
        Log.i(TAG, "=== Quick Benchmark Complete: $passedTests/$totalTests passed ===")
        results.forEach { Log.i(TAG, it.summary) }
        
        return results
    }
}