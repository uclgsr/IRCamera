package mpdc4gsr.network

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/**
 * Connection quality metrics tracking for network connections
 */
class ConnectionMetrics {
    companion object {
        private const val TAG = "ConnectionMetrics"
        private const val PING_TIMEOUT_MS = 5000L
    }
    
    private val mutex = Mutex()
    private val connectionStartTime = AtomicLong(0)
    private val lastPingTime = AtomicLong(0)
    private val totalMessagesSent = AtomicLong(0)
    private val totalMessagesReceived = AtomicLong(0)
    private val totalReconnectAttempts = AtomicLong(0)
    
    private val latencyHistory = mutableListOf<Long>()
    private val maxHistorySize = 100
    
    /**
     * Record connection establishment
     */
    fun recordConnectionStart() {
        connectionStartTime.set(System.currentTimeMillis())
        Log.d(TAG, "Connection metrics started")
    }
    
    /**
     * Record connection end
     */
    fun recordConnectionEnd() {
        val duration = getConnectionDuration()
        Log.d(TAG, "Connection ended after ${duration}ms")
    }
    
    /**
     * Record message sent
     */
    fun recordMessageSent() {
        totalMessagesSent.incrementAndGet()
    }
    
    /**
     * Record message received
     */
    fun recordMessageReceived() {
        totalMessagesReceived.incrementAndGet()
    }
    
    /**
     * Record ping sent
     */
    fun recordPingSent() {
        lastPingTime.set(System.currentTimeMillis())
    }
    
    /**
     * Record pong received and calculate latency
     */
    suspend fun recordPongReceived() {
        val pingTime = lastPingTime.get()
        if (pingTime > 0) {
            val latency = System.currentTimeMillis() - pingTime
            mutex.withLock {
                latencyHistory.add(latency)
                if (latencyHistory.size > maxHistorySize) {
                    latencyHistory.removeAt(0)
                }
            }
            Log.d(TAG, "Ping latency: ${latency}ms")
        }
    }
    
    /**
     * Record reconnection attempt
     */
    fun recordReconnectAttempt() {
        totalReconnectAttempts.incrementAndGet()
    }
    
    /**
     * Get current connection duration in milliseconds
     */
    fun getConnectionDuration(): Long {
        val startTime = connectionStartTime.get()
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }
    }
    
    /**
     * Get average latency in milliseconds
     */
    suspend fun getAverageLatency(): Long = mutex.withLock {
        if (latencyHistory.isEmpty()) {
            -1L
        } else {
            latencyHistory.sum() / latencyHistory.size
        }
    }
    
    /**
     * Get latest latency in milliseconds
     */
    suspend fun getLatestLatency(): Long = mutex.withLock {
        latencyHistory.lastOrNull() ?: -1L
    }
    
    /**
     * Get connection quality metrics summary
     */
    suspend fun getMetricsSummary(): Map<String, Any> = mutex.withLock {
        mapOf(
            "connection_duration_ms" to getConnectionDuration(),
            "messages_sent" to totalMessagesSent.get(),
            "messages_received" to totalMessagesReceived.get(),
            "reconnect_attempts" to totalReconnectAttempts.get(),
            "average_latency_ms" to getAverageLatency(),
            "latest_latency_ms" to getLatestLatency(),
            "total_pings" to latencyHistory.size,
            "connection_uptime_hours" to (getConnectionDuration() / (1000.0 * 60.0 * 60.0))
        )
    }
    
    /**
     * Reset all metrics
     */
    suspend fun reset() = mutex.withLock {
        connectionStartTime.set(0)
        lastPingTime.set(0)
        totalMessagesSent.set(0)
        totalMessagesReceived.set(0)
        totalReconnectAttempts.set(0)
        latencyHistory.clear()
        Log.d(TAG, "Connection metrics reset")
    }
    
    /**
     * Get connection quality score (0-100)
     */
    suspend fun getConnectionQualityScore(): Int = mutex.withLock {
        var score = 100
        
        // Reduce score based on latency
        val avgLatency = getAverageLatency()
        if (avgLatency > 0) {
            when {
                avgLatency > 1000 -> score -= 40  // Very high latency
                avgLatency > 500 -> score -= 25   // High latency
                avgLatency > 200 -> score -= 15   // Moderate latency
                avgLatency > 100 -> score -= 5    // Slight latency
            }
        }
        
        // Reduce score based on reconnection attempts
        val reconnects = totalReconnectAttempts.get()
        if (reconnects > 0) {
            score -= (reconnects * 10).toInt().coerceAtMost(30)
        }
        
        // Ensure score is in valid range
        score.coerceIn(0, 100)
    }
}