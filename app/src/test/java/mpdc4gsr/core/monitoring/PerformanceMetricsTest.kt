package mpdc4gsr.core.monitoring
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformanceMetricsTest {
    @Before
    fun setUp() {
        PerformanceMetrics.reset()
    }
    @After
    fun tearDown() {
        PerformanceMetrics.reset()
    }
    @Test
    fun testInitialize() {
        PerformanceMetrics.initialize()
        // Should not throw exception
        PerformanceMetrics.initialize()
    }
    @Test
    fun testStartAndEndMeasurement() {
        PerformanceMetrics.startMeasurement("test_operation")
        Thread.sleep(100)
        val duration = PerformanceMetrics.endMeasurement("test_operation")
        assertTrue("Duration should be at least 100ms", duration >= 100)
        assertTrue("Duration should be less than 200ms", duration < 200)
    }
    @Test
    fun testEndMeasurementWithoutStart() {
        val duration = PerformanceMetrics.endMeasurement("non_existent")
        assertEquals(-1, duration)
    }
    @Test
    fun testIncrementCounter() {
        PerformanceMetrics.incrementCounter("test_counter")
        PerformanceMetrics.incrementCounter("test_counter")
        PerformanceMetrics.incrementCounter("test_counter")
        assertEquals(3, PerformanceMetrics.getCounter("test_counter"))
    }
    @Test
    fun testGetCounterNonExistent() {
        assertEquals(0, PerformanceMetrics.getCounter("non_existent"))
    }
    @Test
    fun testRecordFrameTime() {
        // Normal frame (< 16ms)
        PerformanceMetrics.recordFrameTime(10_000_000)
        // Janky frame (> 16ms)
        PerformanceMetrics.recordFrameTime(20_000_000)
        // Another janky frame
        PerformanceMetrics.recordFrameTime(30_000_000)
        assertEquals(3, PerformanceMetrics.getCounter("total_frames"))
        assertEquals(2, PerformanceMetrics.getCounter("janky_frames"))
    }
    @Test
    fun testGetJankyFramePercentage() {
        // 2 janky frames out of 10 total
        repeat(8) { PerformanceMetrics.recordFrameTime(10_000_000) }
        repeat(2) { PerformanceMetrics.recordFrameTime(20_000_000) }
        val percentage = PerformanceMetrics.getJankyFramePercentage()
        assertEquals(20.0f, percentage, 0.01f)
    }
    @Test
    fun testGetJankyFramePercentageNoFrames() {
        val percentage = PerformanceMetrics.getJankyFramePercentage()
        assertEquals(0.0f, percentage, 0.01f)
    }
    @Test
    fun testMeasureTimeFunction() {
        var executed = false
        val result = measureTime("test_function") {
            Thread.sleep(50)
            executed = true
            "result"
        }
        assertTrue(executed)
        assertEquals("result", result)
    }
    @Test
    fun testMeasureTimeFunctionWithException() {
        try {
            measureTime("test_exception") {
                throw RuntimeException("Test exception")
            }
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
    }
    @Test
    fun testReset() {
        PerformanceMetrics.incrementCounter("test_counter")
        PerformanceMetrics.startMeasurement("test_op")
        PerformanceMetrics.reset()
        assertEquals(0, PerformanceMetrics.getCounter("test_counter"))
        assertEquals(-1, PerformanceMetrics.endMeasurement("test_op"))
    }
}
