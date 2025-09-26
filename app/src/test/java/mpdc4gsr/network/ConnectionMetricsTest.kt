package mpdc4gsr.network
import org.junit.Assert.*

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ConnectionMetrics functionality
 */
class ConnectionMetricsTest {

    private lateinit var connectionMetrics: ConnectionMetrics

    @Before
    fun setup() {
        connectionMetrics = ConnectionMetrics()
    }

    @Test
    fun testConnectionDurationTracking() {
        connectionMetrics.recordConnectionStart()

        // Simulate some time passing
        Thread.sleep(100)

        val duration = connectionMetrics.getConnectionDuration()
        assertTrue(duration >= 100, "Duration should be at least 100ms")
    }

    @Test
    fun testMessageCounters() {
        connectionMetrics.recordMessageSent()
        connectionMetrics.recordMessageSent()
        connectionMetrics.recordMessageReceived()

        runBlocking {
            val metrics = connectionMetrics.getMetricsSummary()
            assertEquals(2L, metrics["messages_sent"])
            assertEquals(1L, metrics["messages_received"])
        }
    }

    @Test
    fun testLatencyCalculation() = runBlocking {
        connectionMetrics.recordPingSent()

        // Simulate small delay
        Thread.sleep(50)

        connectionMetrics.recordPongReceived()

        val latency = connectionMetrics.getLatestLatency()
        assertTrue(latency >= 50, "Latency should be at least 50ms")
    }

    @Test
    fun testConnectionQualityScore() = runBlocking {
        // Test with good metrics
        val score = connectionMetrics.getConnectionQualityScore()
        assertEquals(100, score, "Initial quality score should be 100")

        // Test with reconnect attempts
        connectionMetrics.recordReconnectAttempt()
        val scoreAfterReconnect = connectionMetrics.getConnectionQualityScore()
        assertTrue(scoreAfterReconnect < 100, "Score should decrease after reconnect attempt")
    }

    @Test
    fun testMetricsReset() = runBlocking {
        connectionMetrics.recordMessageSent()
        connectionMetrics.recordReconnectAttempt()

        connectionMetrics.reset()

        val metrics = connectionMetrics.getMetricsSummary()
        assertEquals(0L, metrics["messages_sent"])
        assertEquals(0L, metrics["reconnect_attempts"])
    }
}