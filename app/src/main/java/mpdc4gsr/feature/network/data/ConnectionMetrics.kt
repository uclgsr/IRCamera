package mpdc4gsr.feature.network.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mpdc4gsr.core.utils.AppLogger
import java.util.concurrent.atomic.AtomicLong

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
    private val totalBytesSent = AtomicLong(0)
    private val totalBytesReceived = AtomicLong(0)
    private val latencyHistory = mutableListOf<Long>()
    private val bandwidthHistory = mutableListOf<BandwidthSample>()
    private val maxHistorySize = 100

    data class BandwidthSample(
        val timestamp: Long,
        val bytesSent: Long,
        val bytesReceived: Long,
        val intervalMs: Long
    )

    fun recordConnectionStart() {
        connectionStartTime.set(System.currentTimeMillis())
        AppLogger.d(TAG, "Connection metrics started")
    }

    fun recordConnectionEnd() {
        val duration = getConnectionDuration()
        AppLogger.d(TAG, "Connection ended after ${duration}ms")
    }

    fun recordMessageSent(messageSize: Int = 0) {
        totalMessagesSent.incrementAndGet()
        if (messageSize > 0) {
            totalBytesSent.addAndGet(messageSize.toLong())
        }
    }

    fun recordMessageReceived(messageSize: Int = 0) {
        totalMessagesReceived.incrementAndGet()
        if (messageSize > 0) {
            totalBytesReceived.addAndGet(messageSize.toLong())
        }
    }

    fun recordPingSent() {
        lastPingTime.set(System.currentTimeMillis())
    }

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
            AppLogger.d(TAG, "Ping latency: ${latency}ms")
        }
    }

    fun recordReconnectAttempt() {
        totalReconnectAttempts.incrementAndGet()
    }

    fun getConnectionDuration(): Long {
        val startTime = connectionStartTime.get()
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }
    }

    suspend fun getAverageLatency(): Long = mutex.withLock {
        if (latencyHistory.isEmpty()) {
            -1L
        } else {
            latencyHistory.sum() / latencyHistory.size
        }
    }

    suspend fun getLatestLatency(): Long = mutex.withLock {
        latencyHistory.lastOrNull() ?: -1L
    }

    suspend fun getMetricsSummary(): Map<String, Any> = mutex.withLock {
        mapOf(
            "connection_duration_ms" to getConnectionDuration(),
            "messages_sent" to totalMessagesSent.get(),
            "messages_received" to totalMessagesReceived.get(),
            "bytes_sent" to totalBytesSent.get(),
            "bytes_received" to totalBytesReceived.get(),
            "reconnect_attempts" to totalReconnectAttempts.get(),
            "average_latency_ms" to getAverageLatency(),
            "latest_latency_ms" to getLatestLatency(),
            "total_pings" to latencyHistory.size,
            "connection_uptime_hours" to (getConnectionDuration() / (1000.0 * 60.0 * 60.0)),
            "average_send_bandwidth_bps" to getAverageSendBandwidth(),
            "average_receive_bandwidth_bps" to getAverageReceiveBandwidth(),
            "total_data_transferred_mb" to ((totalBytesSent.get() + totalBytesReceived.get()) / (1024.0 * 1024.0))
        )
    }

    suspend fun reset() = mutex.withLock {
        connectionStartTime.set(0)
        lastPingTime.set(0)
        totalMessagesSent.set(0)
        totalMessagesReceived.set(0)
        totalReconnectAttempts.set(0)
        totalBytesSent.set(0)
        totalBytesReceived.set(0)
        latencyHistory.clear()
        bandwidthHistory.clear()
        AppLogger.d(TAG, "Connection metrics reset")
    }

    private suspend fun getAverageSendBandwidth(): Double = mutex.withLock {
        val duration = getConnectionDuration()
        if (duration > 0) {
            totalBytesSent.get() * 1000.0 / duration
        } else {
            0.0
        }
    }

    private suspend fun getAverageReceiveBandwidth(): Double = mutex.withLock {
        val duration = getConnectionDuration()
        if (duration > 0) {
            totalBytesReceived.get() * 1000.0 / duration
        } else {
            0.0
        }
    }

    suspend fun recordBandwidthSample() = mutex.withLock {
        val now = System.currentTimeMillis()
        val sample = BandwidthSample(
            timestamp = now,
            bytesSent = totalBytesSent.get(),
            bytesReceived = totalBytesReceived.get(),
            intervalMs = if (bandwidthHistory.isNotEmpty()) {
                now - bandwidthHistory.last().timestamp
            } else {
                1000L // Default 1 second interval
            }
        )
        bandwidthHistory.add(sample)
        if (bandwidthHistory.size > maxHistorySize) {
            bandwidthHistory.removeAt(0)
        }
    }

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