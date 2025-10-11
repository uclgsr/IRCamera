package mpdc4gsr.core.common.telemetry

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class PerformanceMetricsTest {

    @Before
    fun setUp() {
        PerformanceMetrics.reset()
    }

    @Test
    fun `endMeasurement returns elapsed duration`() {
        PerformanceMetrics.startMeasurement("load_data")
        Thread.sleep(10)
        val duration = PerformanceMetrics.endMeasurement("load_data")

        assertTrue(duration >= 0)
    }

    @Test
    fun `recordFrameTime tracks janky frames`() {
        PerformanceMetrics.recordFrameTime(frameTimeNanos = 20_000_000) // 20ms -> slow
        PerformanceMetrics.recordFrameTime(frameTimeNanos = 40_000_000) // 40ms -> critical
        PerformanceMetrics.recordFrameTime(frameTimeNanos = 10_000_000) // smooth

        assertEquals(3, PerformanceMetrics.getCounter("total_frames"))
        assertEquals(2, PerformanceMetrics.getCounter("janky_frames"))
        assertTrue(PerformanceMetrics.getJankyFramePercentage() > 60f)
    }

    @Test
    fun `measureTime helper wraps return value`() {
        val result =
            measureTime("computation") {
                2 + 2
            }

        assertEquals(4, result)
    }
}

