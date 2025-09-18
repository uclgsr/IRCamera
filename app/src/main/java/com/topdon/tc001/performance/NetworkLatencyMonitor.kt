package com.topdon.tc001.performance

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Network Latency and Throughput Monitor
 * 
 * Measures network performance metrics for live streaming including:
 * - End-to-end latency measurement
 * - Network throughput monitoring
 * - Packet loss detection
 * - Connection stability analysis
 */
class NetworkLatencyMonitor {
    
    companion object {
        private const val TAG = "NetworkLatency"
        private const val PING_INTERVAL_MS = 1000L
        private const val LATENCY_TIMEOUT_MS = 5000L
        private const val ACCEPTABLE_LATENCY_MS = 50.0
        private const val ACCEPTABLE_PACKET_LOSS_PERCENT = 5.0
        private const val THROUGHPUT_MEASUREMENT_WINDOW = 10000L // 10 seconds
    }
    
    private val latencyMeasurements = ConcurrentLinkedQueue<LatencyMeasurement>()
    private val throughputMeasurements = ConcurrentLinkedQueue<ThroughputMeasurement>()
    private val networkEvents = ConcurrentLinkedQueue<NetworkEvent>()
    
    private val packetsSent = AtomicLong(0)
    private val packetsReceived = AtomicLong(0)
    private val bytesSent = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)
    private val sessionStartTime = AtomicLong(0)
    private val lastMeasurementTime = AtomicLong(0)
    
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    
    data class LatencyMeasurement(
        val timestamp: Long,
        val targetHost: String,
        val targetPort: Int,
        val latencyMs: Double,
        val success: Boolean,
        val errorMessage: String? = null
    )
    
    data class ThroughputMeasurement(
        val timestamp: Long,
        val uploadBytesPerSecond: Double,
        val downloadBytesPerSecond: Double,
        val totalBytesPerSecond: Double,
        val packetsSentPerSecond: Double,
        val packetsReceivedPerSecond: Double
    )
    
    data class NetworkEvent(
        val timestamp: Long,
        val eventType: NetworkEventType,
        val description: String,
        val severity: NetworkEventSeverity
    )
    
    enum class NetworkEventType {
        CONNECTION_ESTABLISHED,
        CONNECTION_LOST,
        HIGH_LATENCY_DETECTED,
        PACKET_LOSS_DETECTED,
        THROUGHPUT_DROP_DETECTED,
        TIMEOUT_OCCURRED,
        ERROR_OCCURRED
    }
    
    enum class NetworkEventSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    data class NetworkPerformanceMetrics(
        val averageLatencyMs: Double,
        val latencyVariance: Double,
        val minLatencyMs: Double,
        val maxLatencyMs: Double,
        val packetLossPercent: Double,
        val averageUploadThroughputKBps: Double,
        val averageDownloadThroughputKBps: Double,
        val totalThroughputKBps: Double,
        val connectionStabilityScore: Double,
        val totalMeasurements: Int,
        val successfulMeasurements: Int,
        val sessionDurationSeconds: Double,
        val networkEvents: List<NetworkEvent>
    )
    
    /**
     * Start network performance monitoring
     */
    fun startMonitoring(targetHosts: List<Pair<String, Int>> = listOf("8.8.8.8" to 53)) {
        if (isMonitoring) return
        
        isMonitoring = true
        sessionStartTime.set(System.currentTimeMillis())
        clearMetrics()
        
        Log.i(TAG, "Network performance monitoring started")
        
        // Start periodic latency measurements
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    targetHosts.forEach { (host, port) ->
                        measureLatency(host, port)
                    }
                    delay(PING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Error in monitoring loop", e)
                    recordNetworkEvent(
                        NetworkEventType.ERROR_OCCURRED,
                        "Monitoring loop error: ${e.message}",
                        NetworkEventSeverity.WARNING
                    )
                }
            }
        }
    }
    
    /**
     * Stop network performance monitoring
     */
    fun stopMonitoring(): NetworkPerformanceMetrics {
        isMonitoring = false
        monitoringJob?.cancel()
        
        val metrics = calculateNetworkMetrics()
        Log.i(TAG, "Network performance monitoring stopped")
        logNetworkSummary(metrics)
        
        return metrics
    }
    
    /**
     * Measure latency to a specific host
     */
    fun measureLatency(host: String, port: Int): LatencyMeasurement {
        val startTime = System.nanoTime()
        val timestamp = System.currentTimeMillis()
        
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), LATENCY_TIMEOUT_MS.toInt())
                val endTime = System.nanoTime()
                val latencyMs = (endTime - startTime) / 1_000_000.0
                
                packetsSent.incrementAndGet()
                packetsReceived.incrementAndGet()
                
                val measurement = LatencyMeasurement(
                    timestamp = timestamp,
                    targetHost = host,
                    targetPort = port,
                    latencyMs = latencyMs,
                    success = true
                )
                
                latencyMeasurements.offer(measurement)
                
                // Check for high latency
                if (latencyMs > ACCEPTABLE_LATENCY_MS) {
                    recordNetworkEvent(
                        NetworkEventType.HIGH_LATENCY_DETECTED,
                        "High latency detected: ${String.format("%.2f", latencyMs)}ms to $host:$port",
                        if (latencyMs > ACCEPTABLE_LATENCY_MS * 2) NetworkEventSeverity.ERROR else NetworkEventSeverity.WARNING
                    )
                }
                
                maintainWindowSize()
                measurement
            }
        } catch (e: Exception) {
            packetsSent.incrementAndGet()
            
            val measurement = LatencyMeasurement(
                timestamp = timestamp,
                targetHost = host,
                targetPort = port,
                latencyMs = LATENCY_TIMEOUT_MS.toDouble(),
                success = false,
                errorMessage = e.message
            )
            
            latencyMeasurements.offer(measurement)
            
            recordNetworkEvent(
                NetworkEventType.TIMEOUT_OCCURRED,
                "Failed to connect to $host:$port - ${e.message}",
                NetworkEventSeverity.ERROR
            )
            
            maintainWindowSize()
            measurement
        }
    }
    
    /**
     * Record network throughput data
     */
    fun recordThroughput(bytesSent: Long, bytesReceived: Long, packetsSent: Int = 1, packetsReceived: Int = 1) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastMeasurementTime.getAndSet(currentTime)
        
        this.bytesSent.addAndGet(bytesSent)
        this.bytesReceived.addAndGet(bytesReceived)
        this.packetsSent.addAndGet(packetsSent.toLong())
        this.packetsReceived.addAndGet(packetsReceived.toLong())
        
        // Calculate throughput if we have a previous measurement
        if (lastTime > 0) {
            val timeDeltaSeconds = (currentTime - lastTime) / 1000.0
            if (timeDeltaSeconds > 0) {
                val uploadBytesPerSecond = bytesSent / timeDeltaSeconds
                val downloadBytesPerSecond = bytesReceived / timeDeltaSeconds
                val totalBytesPerSecond = uploadBytesPerSecond + downloadBytesPerSecond
                val packetsSentPerSecond = packetsSent / timeDeltaSeconds
                val packetsReceivedPerSecond = packetsReceived / timeDeltaSeconds
                
                val measurement = ThroughputMeasurement(
                    timestamp = currentTime,
                    uploadBytesPerSecond = uploadBytesPerSecond,
                    downloadBytesPerSecond = downloadBytesPerSecond,
                    totalBytesPerSecond = totalBytesPerSecond,
                    packetsSentPerSecond = packetsSentPerSecond,
                    packetsReceivedPerSecond = packetsReceivedPerSecond
                )
                
                throughputMeasurements.offer(measurement)
                maintainWindowSize()
                
                // Log significant throughput measurements
                if (totalBytesPerSecond > 1024) { // > 1KB/s
                    Log.d(TAG, "Throughput: ↑${String.format("%.1f", uploadBytesPerSecond / 1024)} KB/s, " +
                              "↓${String.format("%.1f", downloadBytesPerSecond / 1024)} KB/s")
                }
            }
        }
    }
    
    /**
     * Record significant network events
     */
    private fun recordNetworkEvent(
        eventType: NetworkEventType,
        description: String,
        severity: NetworkEventSeverity
    ) {
        val event = NetworkEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            description = description,
            severity = severity
        )
        
        networkEvents.offer(event)
        
        when (severity) {
            NetworkEventSeverity.INFO -> Log.i(TAG, "Network Event: $description")
            NetworkEventSeverity.WARNING -> Log.w(TAG, "Network Warning: $description")
            NetworkEventSeverity.ERROR -> Log.e(TAG, "Network Error: $description")
            NetworkEventSeverity.CRITICAL -> Log.e(TAG, "Network Critical: $description")
        }
        
        maintainWindowSize()
    }
    
    /**
     * Get current network performance metrics
     */
    fun getCurrentMetrics(): NetworkPerformanceMetrics {
        return calculateNetworkMetrics()
    }
    
    /**
     * Calculate comprehensive network performance metrics
     */
    private fun calculateNetworkMetrics(): NetworkPerformanceMetrics {
        val latencies = latencyMeasurements.toList()
        val throughputs = throughputMeasurements.toList()
        val events = networkEvents.toList()
        
        val sessionDurationMs = if (sessionStartTime.get() > 0) {
            System.currentTimeMillis() - sessionStartTime.get()
        } else 0
        val sessionDurationSeconds = sessionDurationMs / 1000.0
        
        // Latency analysis
        val successfulLatencies = latencies.filter { it.success }.map { it.latencyMs }
        val averageLatencyMs = successfulLatencies.average().takeIf { !it.isNaN() } ?: 0.0
        val minLatencyMs = successfulLatencies.minOrNull() ?: 0.0
        val maxLatencyMs = successfulLatencies.maxOrNull() ?: 0.0
        
        val latencyVariance = if (successfulLatencies.size > 1) {
            val mean = averageLatencyMs
            successfulLatencies.map { (it - mean) * (it - mean) }.average()
        } else 0.0
        
        // Packet loss analysis
        val totalPacketsSent = packetsSent.get()
        val totalPacketsReceived = packetsReceived.get()
        val packetLossPercent = if (totalPacketsSent > 0) {
            ((totalPacketsSent - totalPacketsReceived).toDouble() / totalPacketsSent) * 100
        } else 0.0
        
        // Throughput analysis
        val averageUploadThroughput = throughputs.map { it.uploadBytesPerSecond }.average().takeIf { !it.isNaN() } ?: 0.0
        val averageDownloadThroughput = throughputs.map { it.downloadBytesPerSecond }.average().takeIf { !it.isNaN() } ?: 0.0
        val totalThroughput = averageUploadThroughput + averageDownloadThroughput
        
        // Connection stability score (based on successful connections and low variance)
        val successRate = if (latencies.isNotEmpty()) {
            latencies.count { it.success }.toDouble() / latencies.size
        } else 1.0
        
        val latencyStability = if (averageLatencyMs > 0) {
            kotlin.math.exp(-sqrt(latencyVariance) / averageLatencyMs)
        } else 1.0
        
        val connectionStabilityScore = (successRate * 0.7 + latencyStability * 0.3).coerceIn(0.0, 1.0)
        
        return NetworkPerformanceMetrics(
            averageLatencyMs = averageLatencyMs,
            latencyVariance = latencyVariance,
            minLatencyMs = minLatencyMs,
            maxLatencyMs = maxLatencyMs,
            packetLossPercent = maxOf(0.0, packetLossPercent),
            averageUploadThroughputKBps = averageUploadThroughput / 1024,
            averageDownloadThroughputKBps = averageDownloadThroughput / 1024,
            totalThroughputKBps = totalThroughput / 1024,
            connectionStabilityScore = connectionStabilityScore,
            totalMeasurements = latencies.size,
            successfulMeasurements = successfulLatencies.size,
            sessionDurationSeconds = sessionDurationSeconds,
            networkEvents = events
        )
    }
    
    /**
     * Maintain window size for measurements
     */
    private fun maintainWindowSize() {
        val maxWindowSize = 1000
        
        while (latencyMeasurements.size > maxWindowSize) {
            latencyMeasurements.poll()
        }
        
        while (throughputMeasurements.size > maxWindowSize) {
            throughputMeasurements.poll()
        }
        
        while (networkEvents.size > maxWindowSize) {
            networkEvents.poll()
        }
    }
    
    /**
     * Clear all metrics
     */
    private fun clearMetrics() {
        latencyMeasurements.clear()
        throughputMeasurements.clear()
        networkEvents.clear()
        packetsSent.set(0)
        packetsReceived.set(0)
        bytesSent.set(0)
        bytesReceived.set(0)
        lastMeasurementTime.set(0)
    }
    
    /**
     * Log network performance summary
     */
    private fun logNetworkSummary(metrics: NetworkPerformanceMetrics) {
        Log.i(TAG, "=== NETWORK PERFORMANCE SUMMARY ===")
        Log.i(TAG, "Session Duration: ${String.format("%.2f", metrics.sessionDurationSeconds)}s")
        Log.i(TAG, "Total Measurements: ${metrics.totalMeasurements}")
        Log.i(TAG, "Successful Measurements: ${metrics.successfulMeasurements}")
        Log.i(TAG, "Success Rate: ${String.format("%.1f", (metrics.successfulMeasurements.toDouble() / metrics.totalMeasurements) * 100)}%")
        Log.i(TAG, "Average Latency: ${String.format("%.2f", metrics.averageLatencyMs)}ms")
        Log.i(TAG, "Latency Range: ${String.format("%.2f", metrics.minLatencyMs)}ms - ${String.format("%.2f", metrics.maxLatencyMs)}ms")
        Log.i(TAG, "Latency Variance: ${String.format("%.2f", sqrt(metrics.latencyVariance))}ms")
        Log.i(TAG, "Packet Loss: ${String.format("%.2f", metrics.packetLossPercent)}%")
        Log.i(TAG, "Upload Throughput: ${String.format("%.1f", metrics.averageUploadThroughputKBps)} KB/s")
        Log.i(TAG, "Download Throughput: ${String.format("%.1f", metrics.averageDownloadThroughputKBps)} KB/s")
        Log.i(TAG, "Total Throughput: ${String.format("%.1f", metrics.totalThroughputKBps)} KB/s")
        Log.i(TAG, "Connection Stability: ${String.format("%.3f", metrics.connectionStabilityScore)}")
        
        // Performance assessment
        val latencyOk = metrics.averageLatencyMs <= ACCEPTABLE_LATENCY_MS
        val packetLossOk = metrics.packetLossPercent <= ACCEPTABLE_PACKET_LOSS_PERCENT
        val stabilityOk = metrics.connectionStabilityScore >= 0.8
        
        if (latencyOk && packetLossOk && stabilityOk) {
            Log.i(TAG, "✅ Network performance validation PASSED")
        } else {
            val issues = mutableListOf<String>()
            if (!latencyOk) issues.add("high latency (${String.format("%.2f", metrics.averageLatencyMs)}ms)")
            if (!packetLossOk) issues.add("packet loss (${String.format("%.2f", metrics.packetLossPercent)}%)")
            if (!stabilityOk) issues.add("connection instability (${String.format("%.3f", metrics.connectionStabilityScore)})")
            
            Log.w(TAG, "⚠️ Network performance issues detected: ${issues.joinToString(", ")}")
        }
        
        // Log significant network events
        val criticalEvents = metrics.networkEvents.filter { it.severity == NetworkEventSeverity.CRITICAL }
        val errorEvents = metrics.networkEvents.filter { it.severity == NetworkEventSeverity.ERROR }
        val warningEvents = metrics.networkEvents.filter { it.severity == NetworkEventSeverity.WARNING }
        
        if (criticalEvents.isNotEmpty() || errorEvents.isNotEmpty()) {
            Log.w(TAG, "Network Events - Critical: ${criticalEvents.size}, Errors: ${errorEvents.size}, Warnings: ${warningEvents.size}")
        }
    }
    
    /**
     * Export network performance data to CSV
     */
    fun exportNetworkData(outputFile: File): Boolean {
        return try {
            val latencies = latencyMeasurements.toList()
            val throughputs = throughputMeasurements.toList()
            val events = networkEvents.toList()
            val metrics = getCurrentMetrics()
            
            outputFile.printWriter().use { writer ->
                writer.println("# Network Performance Data Export")
                writer.println("# Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                writer.println("# Session Duration: ${String.format("%.2f", metrics.sessionDurationSeconds)}s")
                writer.println("# Average Latency: ${String.format("%.2f", metrics.averageLatencyMs)}ms")
                writer.println("# Packet Loss: ${String.format("%.2f", metrics.packetLossPercent)}%")
                writer.println("# Total Throughput: ${String.format("%.1f", metrics.totalThroughputKBps)} KB/s")
                writer.println("#")
                
                // Export latency measurements
                writer.println("# Latency Measurements")
                writer.println("timestamp_ms,target_host,target_port,latency_ms,success,error_message")
                latencies.forEach { latency ->
                    writer.println("${latency.timestamp},${latency.targetHost},${latency.targetPort},${latency.latencyMs},${latency.success},\"${latency.errorMessage ?: ""}\"")
                }
                
                writer.println()
                writer.println("# Throughput Measurements")
                writer.println("timestamp_ms,upload_bytes_per_sec,download_bytes_per_sec,total_bytes_per_sec,packets_sent_per_sec,packets_received_per_sec")
                throughputs.forEach { throughput ->
                    writer.println("${throughput.timestamp},${throughput.uploadBytesPerSecond},${throughput.downloadBytesPerSecond},${throughput.totalBytesPerSecond},${throughput.packetsSentPerSecond},${throughput.packetsReceivedPerSecond}")
                }
                
                writer.println()
                writer.println("# Network Events")
                writer.println("timestamp_ms,event_type,severity,description")
                events.forEach { event ->
                    writer.println("${event.timestamp},${event.eventType},${event.severity},\"${event.description}\"")
                }
            }
            
            Log.i(TAG, "Network performance data exported to: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting network performance data", e)
            false
        }
    }
    
    /**
     * Get diagnostic information
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        val metrics = getCurrentMetrics()
        
        return mapOf(
            "is_monitoring" to isMonitoring,
            "session_start_time" to sessionStartTime.get(),
            "packets_sent" to packetsSent.get(),
            "packets_received" to packetsReceived.get(),
            "bytes_sent" to bytesSent.get(),
            "bytes_received" to bytesReceived.get(),
            "buffer_sizes" to mapOf(
                "latency_measurements" to latencyMeasurements.size,
                "throughput_measurements" to throughputMeasurements.size,
                "network_events" to networkEvents.size
            ),
            "performance_metrics" to metrics
        )
    }
}