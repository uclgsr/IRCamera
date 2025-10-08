// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\test\java\mpdc4gsr\core' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\test\java\mpdc4gsr\core\app_src_test_java_mpdc4gsr_core_all.kt =====

// Merged .kt under 'app\src\test\java\mpdc4gsr\core' subtree
// Files: 2; Generated 2025-10-07 23:07:39


// ===== app\src\test\java\mpdc4gsr\core\monitoring\PerformanceMetricsTest.kt =====

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


// ===== app\src\test\java\mpdc4gsr\core\monitoring\TelemetryManagerTest.kt =====

package mpdc4gsr.core.monitoring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TelemetryManagerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TelemetryManager.initialize(context)
    }

    @Test
    fun testInitialize() {
        // Should not throw exception when initialized multiple times
        TelemetryManager.initialize(context)
    }

    @Test
    fun testSetUserId() {
        // Should not throw exception
        TelemetryManager.setUserId("test_user_123")
    }

    @Test
    fun testClearUserId() {
        TelemetryManager.setUserId("test_user_123")
        TelemetryManager.clearUserId()
        // Should not throw exception
    }

    @Test
    fun testTrackEvent() {
        TelemetryManager.trackEvent("test_event")
    }

    @Test
    fun testTrackEventWithParams() {
        val params = mapOf(
            "param1" to "value1",
            "param2" to 123,
            "param3" to true
        )
        TelemetryManager.trackEvent("test_event_with_params", params)
    }

    @Test
    fun testTrackScreenView() {
        TelemetryManager.trackScreenView("TestScreen", "TestScreenActivity")
    }

    @Test
    fun testTrackError() {
        TelemetryManager.trackError("Test error message")
    }

    @Test
    fun testTrackErrorWithException() {
        val exception = RuntimeException("Test exception")
        TelemetryManager.trackError("Error occurred", exception, fatal = false)
    }

    @Test
    fun testTrackFatalError() {
        val exception = RuntimeException("Fatal exception")
        TelemetryManager.trackError("Fatal error occurred", exception, fatal = true)
    }

    @Test
    fun testLogMetric() {
        TelemetryManager.logMetric("test_metric", 123)
    }

    @Test
    fun testLogMetricWithUnit() {
        TelemetryManager.logMetric("test_metric_with_unit", 456, "ms")
    }

    @Test
    fun testTrackRecordingSession() {
        TelemetryManager.trackRecordingSession(
            recordingId = "rec_123",
            durationMs = 60000,
            success = true
        )
    }

    @Test
    fun testTrackFeatureUsage() {
        TelemetryManager.trackFeatureUsage("camera", "start_recording")
    }

    @Test
    fun testTrackNetworkRequest() {
        TelemetryManager.trackNetworkRequest(
            endpoint = "/api/upload",
            method = "POST",
            statusCode = 200,
            durationMs = 150
        )
    }

    @Test
    fun testTrackPermissionRequest() {
        TelemetryManager.trackPermissionRequest("android.permission.CAMERA", granted = true)
    }

    @Test
    fun testSetProperty() {
        TelemetryManager.setProperty("test_property", "test_value")
        // Should not throw exception
    }

    @Test
    fun testRemoveProperty() {
        TelemetryManager.setProperty("test_property", "test_value")
        TelemetryManager.removeProperty("test_property")
        // Should not throw exception
    }

    @Test
    fun testTrackExecutionTime() {
        var executed = false
        val result = trackExecutionTime("test_operation") {
            Thread.sleep(50)
            executed = true
            "result"
        }
        assert(executed)
        assert(result == "result")
    }
}


// ===== FROM: _ktjava_mirror\app\src\test\java\mpdc4gsr\core\monitoring\app_src_test_java_mpdc4gsr_core_monitoring_all.kt =====

// Merged .kt under 'app\src\test\java\mpdc4gsr\core\monitoring' subtree
// Files: 2; Generated 2025-10-07 23:07:39


// ===== app\src\test\java\mpdc4gsr\core\monitoring\PerformanceMetricsTest.kt =====

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


// ===== app\src\test\java\mpdc4gsr\core\monitoring\TelemetryManagerTest.kt =====

package mpdc4gsr.core.monitoring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TelemetryManagerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TelemetryManager.initialize(context)
    }

    @Test
    fun testInitialize() {
        // Should not throw exception when initialized multiple times
        TelemetryManager.initialize(context)
    }

    @Test
    fun testSetUserId() {
        // Should not throw exception
        TelemetryManager.setUserId("test_user_123")
    }

    @Test
    fun testClearUserId() {
        TelemetryManager.setUserId("test_user_123")
        TelemetryManager.clearUserId()
        // Should not throw exception
    }

    @Test
    fun testTrackEvent() {
        TelemetryManager.trackEvent("test_event")
    }

    @Test
    fun testTrackEventWithParams() {
        val params = mapOf(
            "param1" to "value1",
            "param2" to 123,
            "param3" to true
        )
        TelemetryManager.trackEvent("test_event_with_params", params)
    }

    @Test
    fun testTrackScreenView() {
        TelemetryManager.trackScreenView("TestScreen", "TestScreenActivity")
    }

    @Test
    fun testTrackError() {
        TelemetryManager.trackError("Test error message")
    }

    @Test
    fun testTrackErrorWithException() {
        val exception = RuntimeException("Test exception")
        TelemetryManager.trackError("Error occurred", exception, fatal = false)
    }

    @Test
    fun testTrackFatalError() {
        val exception = RuntimeException("Fatal exception")
        TelemetryManager.trackError("Fatal error occurred", exception, fatal = true)
    }

    @Test
    fun testLogMetric() {
        TelemetryManager.logMetric("test_metric", 123)
    }

    @Test
    fun testLogMetricWithUnit() {
        TelemetryManager.logMetric("test_metric_with_unit", 456, "ms")
    }

    @Test
    fun testTrackRecordingSession() {
        TelemetryManager.trackRecordingSession(
            recordingId = "rec_123",
            durationMs = 60000,
            success = true
        )
    }

    @Test
    fun testTrackFeatureUsage() {
        TelemetryManager.trackFeatureUsage("camera", "start_recording")
    }

    @Test
    fun testTrackNetworkRequest() {
        TelemetryManager.trackNetworkRequest(
            endpoint = "/api/upload",
            method = "POST",
            statusCode = 200,
            durationMs = 150
        )
    }

    @Test
    fun testTrackPermissionRequest() {
        TelemetryManager.trackPermissionRequest("android.permission.CAMERA", granted = true)
    }

    @Test
    fun testSetProperty() {
        TelemetryManager.setProperty("test_property", "test_value")
        // Should not throw exception
    }

    @Test
    fun testRemoveProperty() {
        TelemetryManager.setProperty("test_property", "test_value")
        TelemetryManager.removeProperty("test_property")
        // Should not throw exception
    }

    @Test
    fun testTrackExecutionTime() {
        var executed = false
        val result = trackExecutionTime("test_operation") {
            Thread.sleep(50)
            executed = true
            "result"
        }
        assert(executed)
        assert(result == "result")
    }
}