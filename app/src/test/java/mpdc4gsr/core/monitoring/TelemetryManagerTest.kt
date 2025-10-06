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
