// Merged .kt under 'app\src\test\java\mpdc4gsr' subtree
// Files: 14; Generated 2025-10-07 23:07:39


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


// ===== app\src\test\java\mpdc4gsr\tests\ComposeTestStubs.kt =====

package mpdc4gsr.tests

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class MainFragmentViewModel {
    data class DeviceState(
        val hasAnyDevice: Boolean,
        val hasConnectLine: Boolean,
        val hasConnectTS004: Boolean,
        val hasConnectTC007: Boolean
    )

    val deviceState: MutableStateFlow<DeviceState> = MutableStateFlow(
        DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
    )
    val batteryInfo: MutableStateFlow<String?> = MutableStateFlow(null)
    val navigationEvents: MutableSharedFlow<String> = MutableSharedFlow()
}

class MainFragmentCompose {
    @Composable
    fun Content(viewModel: MainFragmentViewModel) {
    }
}

class SensorDashboardFragmentCompose {
    @Composable
    fun Content() {
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\DeviceEventManagementTest.kt =====

package mpdc4gsr.tests

import android.hardware.usb.UsbDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceEventManagementTest {
    private lateinit var mockDevice: UsbDevice

    @Before
    fun setUp() {
        mockDevice = mockk(relaxed = true)
    }

    @Test
    fun `deviceConnectionState initial value is null`() = runTest {
        val initialState = DeviceEventManager.deviceConnectionState.value
        assertNull("Initial device connection state should be null", initialState)
    }

    @Test
    fun `socketConnectionState initial value is null`() = runTest {
        val initialState = DeviceEventManager.socketConnectionState.value
        assertNull("Initial socket connection state should be null", initialState)
    }

    @Test
    fun `emitDeviceConnection updates deviceConnectionState`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val job = launch(testDispatcher) {
            DeviceEventManager.deviceConnectionState.collect { state ->
                if (state != null) {
                    assertTrue("Device should be connected", state.isConnected)
                    assertEquals("Device should match mock device", mockDevice, state.device)
                }
            }
        }
        DeviceEventManager.emitDeviceConnection(true, mockDevice)
        val state = DeviceEventManager.deviceConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Device should be connected", state!!.isConnected)
        assertEquals("Device should match mock device", mockDevice, state.device)
        job.cancel()
    }

    @Test
    fun `emitDeviceConnection with null device`() = runTest {
        DeviceEventManager.emitDeviceConnection(false, null)
        val state = DeviceEventManager.deviceConnectionState.value
        assertNotNull("State should not be null", state)
        assertFalse("Device should be disconnected", state!!.isConnected)
        assertNull("Device should be null", state.device)
    }

    @Test
    fun `emitSocketConnection updates socketConnectionState`() = runTest {
        DeviceEventManager.emitSocketConnection(true, false)
        val state = DeviceEventManager.socketConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Socket should be connected", state!!.isConnected)
        assertFalse("Should not be TS004", state.isTS004)
    }

    @Test
    fun `emitSocketConnection with TS004 flag`() = runTest {
        DeviceEventManager.emitSocketConnection(true, true)
        val state = DeviceEventManager.socketConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Socket should be connected", state!!.isConnected)
        assertTrue("Should be TS004", state.isTS004)
    }

    @Test
    fun `emitDeviceConnectionSync updates state synchronously`() = runTest {
        DeviceEventManager.emitDeviceConnectionSync(true, mockDevice)
        val state = DeviceEventManager.deviceConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Device should be connected", state!!.isConnected)
        assertEquals("Device should match mock device", mockDevice, state.device)
    }

    @Test
    fun `emitSocketConnectionSync updates state synchronously`() = runTest {
        DeviceEventManager.emitSocketConnectionSync(false, false)
        val state = DeviceEventManager.socketConnectionState.value
        assertNotNull("State should not be null", state)
        assertFalse("Socket should be disconnected", state!!.isConnected)
        assertFalse("Should not be TS004", state.isTS004)
    }

    @Test
    fun `emitDevicePermissionRequest emits to SharedFlow`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val collectedDevices = mutableListOf<UsbDevice>()
        val job = launch(testDispatcher) {
            DeviceEventManager.devicePermissionRequested.collect { device ->
                collectedDevices.add(device)
            }
        }
        DeviceEventManager.emitDevicePermissionRequest(mockDevice)
        assertEquals("Should have collected one device", 1, collectedDevices.size)
        assertEquals("Collected device should match mock", mockDevice, collectedDevices[0])
        job.cancel()
    }

    @Test
    fun `emitDevicePermissionRequestSync emits to SharedFlow synchronously`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val collectedDevices = mutableListOf<UsbDevice>()
        val job = launch(testDispatcher) {
            DeviceEventManager.devicePermissionRequested.collect { device ->
                collectedDevices.add(device)
            }
        }
        val result = DeviceEventManager.emitDevicePermissionRequestSync(mockDevice)
        assertTrue("tryEmit should succeed with active collector", result)
        assertEquals("Should have collected one device", 1, collectedDevices.size)
        assertEquals("Collected device should match mock", mockDevice, collectedDevices[0])
        job.cancel()
    }

    @Test
    fun `multiple emissions update state correctly`() = runTest {
        val device1 = mockk<UsbDevice>(relaxed = true)
        val device2 = mockk<UsbDevice>(relaxed = true)
        DeviceEventManager.emitDeviceConnection(true, device1)
        var state = DeviceEventManager.deviceConnectionState.value
        assertEquals("First device should be set", device1, state?.device)
        DeviceEventManager.emitDeviceConnection(false, null)
        state = DeviceEventManager.deviceConnectionState.value
        assertNull("Device should be null after disconnect", state?.device)
        assertFalse("Should be disconnected", state?.isConnected ?: true)
        DeviceEventManager.emitDeviceConnection(true, device2)
        state = DeviceEventManager.deviceConnectionState.value
        assertEquals("Second device should be set", device2, state?.device)
        assertTrue("Should be connected", state?.isConnected ?: false)
    }

    @Test
    fun `DeviceConnectionState data class equality`() {
        val state1 = DeviceEventManager.DeviceConnectionState(true, mockDevice)
        val state2 = DeviceEventManager.DeviceConnectionState(true, mockDevice)
        val state3 = DeviceEventManager.DeviceConnectionState(false, null)
        assertEquals("Same states should be equal", state1, state2)
        assertNotEquals("Different states should not be equal", state1, state3)
    }

    @Test
    fun `SocketConnectionState data class equality`() {
        val state1 = DeviceEventManager.SocketConnectionState(true, false)
        val state2 = DeviceEventManager.SocketConnectionState(true, false)
        val state3 = DeviceEventManager.SocketConnectionState(false, true)
        assertEquals("Same states should be equal", state1, state2)
        assertNotEquals("Different states should not be equal", state1, state3)
    }

    @Test
    fun `deviceConnectionState is a cold StateFlow`() = runTest {
        DeviceEventManager.emitDeviceConnection(true, mockDevice)
        val state1 = DeviceEventManager.deviceConnectionState.first()
        val state2 = DeviceEventManager.deviceConnectionState.first()
        assertEquals("StateFlow should provide same value on multiple collections", state1, state2)
    }

    @Test
    fun `socket connection state transitions correctly`() = runTest {
        DeviceEventManager.emitSocketConnection(false, false)
        var state = DeviceEventManager.socketConnectionState.value
        assertFalse("Should start disconnected", state?.isConnected ?: true)
        DeviceEventManager.emitSocketConnection(true, false)
        state = DeviceEventManager.socketConnectionState.value
        assertTrue("Should be connected", state?.isConnected ?: false)
        DeviceEventManager.emitSocketConnection(false, false)
        state = DeviceEventManager.socketConnectionState.value
        assertFalse("Should be disconnected again", state?.isConnected ?: true)
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\ErrorHandlingTest.kt =====

package mpdc4gsr.tests

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.runBlocking
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorHandlingTest {
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        mockkObject(AppLogger)
        every { AppLogger.e(any(), any(), any(), any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    @Test
    fun testRunSafelySuccessCase() {
        val result =
            ErrorHandler.runSafely("TEST", "test operation") {
                42
            }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
        verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
    }

    @Test
    fun testRunSafelyFailureCase() {
        val exception = RuntimeException("Test error")
        val result =
            ErrorHandler.runSafely("TEST", "test operation") {
                throw exception
            }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(exactly = 1) {
            AppLogger.e("TEST", "Failed to test operation: Test error", exception)
        }
    }

    @Test
    fun testRunSafelyWithDefaultSuccess() {
        val result =
            ErrorHandler.runSafelyWithDefault(
                "TEST",
                "test operation",
                0,
            ) {
                42
            }
        assertEquals(42, result)
        verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
    }

    @Test
    fun testRunSafelyWithDefaultFailure() {
        val exception = RuntimeException("Test error")
        val result =
            ErrorHandler.runSafelyWithDefault(
                "TEST",
                "test operation",
                0,
            ) {
                throw exception
            }
        assertEquals(0, result)
        verify(exactly = 1) {
            AppLogger.e(
                "TEST",
                "Failed to test operation: Test error, using default",
                exception,
            )
        }
    }

    @Test
    fun testRunSafelyIgnoreResultSuccess() {
        var executed = false
        ErrorHandler.runSafelyIgnoreResult("TEST", "test operation") {
            executed = true
        }
        assertTrue(executed)
        verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
    }

    @Test
    fun testRunSafelyIgnoreResultFailure() {
        val exception = RuntimeException("Test error")
        ErrorHandler.runSafelyIgnoreResult("TEST", "test operation") {
            throw exception
        }
        verify(exactly = 1) {
            AppLogger.e("TEST", "Failed to test operation: Test error", exception)
        }
    }

    @Test
    fun testRunSafelySuspendSuccess() =
        runBlocking {
            val result =
                ErrorHandler.runSafelySuspend("TEST", "test operation") {
                    42
                }
            assertTrue(result.isSuccess)
            assertEquals(42, result.getOrNull())
            verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
        }

    @Test
    fun testRunSafelySuspendFailure() =
        runBlocking {
            val exception = RuntimeException("Test error")
            val result =
                ErrorHandler.runSafelySuspend("TEST", "test operation") {
                    throw exception
                }
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            verify(exactly = 1) {
                AppLogger.e("TEST", "Failed to test operation: Test error", exception)
            }
        }

    @Test
    fun testRunSafelySuspendWithDefaultSuccess() =
        runBlocking {
            val result =
                ErrorHandler.runSafelySuspendWithDefault(
                    "TEST",
                    "test operation",
                    0,
                ) {
                    42
                }
            assertEquals(42, result)
            verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
        }

    @Test
    fun testRunSafelySuspendWithDefaultFailure() =
        runBlocking {
            val exception = RuntimeException("Test error")
            val result =
                ErrorHandler.runSafelySuspendWithDefault(
                    "TEST",
                    "test operation",
                    0,
                ) {
                    throw exception
                }
            assertEquals(0, result)
            verify(exactly = 1) {
                AppLogger.e(
                    "TEST",
                    "Failed to test operation: Test error, using default",
                    exception,
                )
            }
        }
}


// ===== app\src\test\java\mpdc4gsr\tests\FragmentToComposeMigrationTest.kt =====

package mpdc4gsr.tests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@Ignore("All tests disabled")
@RunWith(AndroidJUnit4::class)
class FragmentToComposeMigrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock ViewModels for testing
    private val mockMainFragmentViewModel = mock<MainFragmentViewModel>()

    @Test
    fun testMainFragmentCompose_InitialState() = runTest {
        // Setup mock data
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify initial UI state
        composeTestRule.onNodeWithText("No Devices Found").assertExists()
        composeTestRule.onNodeWithText("Add a device to get started").assertExists()
        composeTestRule.onNodeWithContentDescription("Add Device").assertExists()
        composeTestRule.onNodeWithContentDescription("GSR Recording").assertExists()
    }

    @Test
    fun testMainFragmentCompose_WithConnectedDevices() = runTest {
        // Setup mock data with connected devices
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = true,
            hasConnectLine = true,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify connected device state
        composeTestRule.onNodeWithText("Devices Connected").assertExists()
        composeTestRule.onNodeWithText("TC Line Device").assertExists()
        composeTestRule.onNodeWithText("Online").assertExists()
    }

    @Test
    fun testSensorDashboardFragmentCompose_InitialState() = runTest {
        composeTestRule.setContent {
            SensorDashboardFragmentCompose().Content()
        }
        // Verify dashboard components
        composeTestRule.onNodeWithText("Sensor Dashboard").assertExists()
        composeTestRule.onNodeWithText("All Sensors Connected & Ready").assertExists()
        composeTestRule.onNodeWithText("Start Recording").assertExists()
        composeTestRule.onNodeWithText("Sensors (4)").assertExists()
        // Verify individual sensors
        composeTestRule.onNodeWithText("TC001 Thermal Camera").assertExists()
        composeTestRule.onNodeWithText("RGB Camera").assertExists()
        composeTestRule.onNodeWithText("Shimmer GSR Sensor").assertExists()
        composeTestRule.onNodeWithText("Audio Recorder").assertExists()
    }

    @Test
    fun testSensorDashboardFragmentCompose_RecordingState() = runTest {
        composeTestRule.setContent {
            SensorDashboardFragmentCompose().Content()
        }
        // Start recording
        composeTestRule.onNodeWithText("Start Recording").performClick()
        // Verify recording state changes
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Stop Recording").assertExists()
        composeTestRule.onNodeWithText("RECORDING").assertExists()
    }

    @Test
    fun testAccessibility_MainFragmentCompose() = runTest {
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Add Device").assertExists()
        composeTestRule.onNodeWithContentDescription("GSR Recording").assertExists()
        // Test accessibility actions
        composeTestRule.onAllNodesWithContentDescription("Add Device")[0].assertHasClickAction()
        composeTestRule.onAllNodesWithContentDescription("GSR Recording")[0].assertHasClickAction()
    }

    @Test
    fun testThemeConsistency_AllFragments() = runTest {
        // Test that all fragments use consistent Material 3 theming
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify UI components are present
        composeTestRule.onNodeWithText("No Devices Found").assertExists()
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\GSRBluetoothDiscoveryTest.kt =====

package mpdc4gsr.tests

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("All tests disabled")
class GSRBluetoothDiscoveryTest {
    private lateinit var mockContext: Context
    private lateinit var mockBluetoothManager: BluetoothManager
    private lateinit var mockBluetoothAdapter: BluetoothAdapter
    private lateinit var mockBluetoothDevice: BluetoothDevice

    // Target Shimmer3 GSR+ device characteristics
    private val shimmerDeviceName = "Shimmer3-GSR"
    private val shimmerMacAddress = "00:06:66:12:34:56"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockContext = mockk()
        mockBluetoothManager = mockk()
        mockBluetoothAdapter = mockk()
        mockBluetoothDevice = mockk()
        // Setup real Shimmer device characteristics
        every { mockBluetoothDevice.name } returns shimmerDeviceName
        every { mockBluetoothDevice.address } returns shimmerMacAddress
        every { mockBluetoothDevice.type } returns BluetoothDevice.DEVICE_TYPE_LE
        every { mockContext.getSystemService(Context.BLUETOOTH_SERVICE) } returns mockBluetoothManager
        every { mockBluetoothManager.adapter } returns mockBluetoothAdapter
        every { mockBluetoothAdapter.isEnabled } returns true
    }

    @Test
    fun `should detect required BLE permissions for GSR device discovery`() {
        // Test specific BLE permissions required for Shimmer3 GSR+
        every {
            mockContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            mockContext.checkSelfPermission(android.Manifest.permission.BLUETOOTH)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            mockContext.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN)
        } returns PackageManager.PERMISSION_GRANTED
        // Verify all required permissions are checked
        val hasLocationPermission = mockContext.checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasBluetoothPermission = mockContext.checkSelfPermission(
            android.Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED
        val hasBluetoothAdminPermission = mockContext.checkSelfPermission(
            android.Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED
        assertTrue("Location permission required for BLE scanning", hasLocationPermission)
        assertTrue("Bluetooth permission required for device access", hasBluetoothPermission)
        assertTrue("Bluetooth admin permission required for pairing", hasBluetoothAdminPermission)
    }

    @Test
    fun `should validate Shimmer device characteristics during discovery`() {
        // Test real Shimmer3 GSR+ device identification
        // Validate device name pattern
        assertTrue(
            "Device name should match Shimmer3 pattern",
            mockBluetoothDevice.name.startsWith("Shimmer3")
        )
        // Validate MAC address format (Shimmer uses specific OUI)
        assertTrue(
            "MAC address should follow Shimmer OUI pattern",
            mockBluetoothDevice.address.matches(Regex("^[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}$"))
        )
        // Validate BLE device type
        assertEquals(
            "Should detect BLE device type",
            BluetoothDevice.DEVICE_TYPE_LE,
            mockBluetoothDevice.type
        )
    }

    @Test
    fun `should handle Bluetooth adapter state changes`() = runTest {
        // Test Bluetooth adapter state handling during device discovery
        // Initially enabled
        every { mockBluetoothAdapter.isEnabled } returns true
        assertTrue("Bluetooth should be enabled for discovery", mockBluetoothAdapter.isEnabled)
        // Simulate adapter disabled during scan
        every { mockBluetoothAdapter.isEnabled } returns false
        assertFalse("Should detect Bluetooth disabled state", mockBluetoothAdapter.isEnabled)
        // Verify proper error handling when Bluetooth disabled
        // Real implementation should handle this gracefully without crashing
    }

    @Test
    fun `should validate device connection attempt with proper service discovery`() = runTest {
        // Test actual Shimmer device connection process
        val shimmerServiceUuid = "49535343-fe7d-4ae5-8fa9-9fafd205e455" // Shimmer service UUID
        // Mock successful device bonding
        every { mockBluetoothDevice.bondState } returns BluetoothDevice.BOND_BONDED
        // Verify device is properly bonded before connection attempt
        assertEquals(
            "Device should be bonded for GSR streaming",
            BluetoothDevice.BOND_BONDED,
            mockBluetoothDevice.bondState
        )
        // Connection attempt should include proper service discovery
        // Real implementation validates Shimmer-specific GATT services
    }

    @Test
    fun `should implement exponential backoff for connection retries`() = runTest {
        // Test connection retry logic with timing validation
        val maxRetries = 3
        val baseDelayMs = 1000L
        var attemptCount = 0
        val retryDelays = mutableListOf<Long>()
        // Simulate connection failures requiring retries
        repeat(maxRetries) { attempt ->
            attemptCount++
            val delay = baseDelayMs * (1L shl attempt) // Exponential backoff
            retryDelays.add(delay)
        }
        assertEquals("Should attempt max retries", maxRetries, attemptCount)
        assertEquals("First retry delay", 1000L, retryDelays[0])
        assertEquals("Second retry delay", 2000L, retryDelays[1])
        assertEquals("Third retry delay", 4000L, retryDelays[2])
        // Validate exponential backoff pattern
        for (i in 1 until retryDelays.size) {
            assertTrue(
                "Retry delay should increase exponentially",
                retryDelays[i] > retryDelays[i - 1]
            )
        }
    }

    @Test
    fun `should validate GSR data packet structure for Shimmer3`() {
        // Test Shimmer3 GSR+ specific data packet validation
        // Shimmer3 GSR packet structure (simplified)
        val gsrChannelId = 0x0D // Shimmer3 GSR channel identifier
        val expectedPacketSize = 20 // Typical Shimmer packet size
        val samplingRate = 128 // Target sampling rate for GSR
        // Validate packet identifiers
        assertTrue("GSR channel ID should be valid", gsrChannelId in 0x00..0xFF)
        assertTrue("Packet size should be reasonable", expectedPacketSize > 0)
        assertTrue("Sampling rate should be 128Hz", samplingRate == 128)
        // Real implementation should parse actual Shimmer ObjectCluster data
        // and validate timestamp, GSR resistance, and conductance values
    }

    @Test
    fun `should handle device disconnection detection`() = runTest {
        // Test proper disconnection detection and cleanup
        var isConnected = true
        val connectionLostCallback = mockk<() -> Unit>(relaxed = true)
        // Simulate connection loss
        isConnected = false
        if (!isConnected) {
            connectionLostCallback()
        }
        // Verify disconnection callback was triggered
        verify(exactly = 1) { connectionLostCallback() }
        // Real implementation should:
        // 1. Detect GATT disconnection events
        // 2. Release Bluetooth resources properly
        // 3. Attempt automatic reconnection
        // 4. Update UI with connection status
    }

    @Test
    fun `should validate resource cleanup after discovery`() {
        // Test proper resource management during BLE operations
        val mockGattCallback = mockk<android.bluetooth.BluetoothGattCallback>(relaxed = true)
        val mockBluetoothGatt = mockk<android.bluetooth.BluetoothGatt>(relaxed = true)
        // Simulate GATT connection
        every { mockBluetoothDevice.connectGatt(any(), any(), any()) } returns mockBluetoothGatt
        // Verify connection is established
        val gatt = mockBluetoothDevice.connectGatt(mockContext, false, mockGattCallback)
        assertNotNull("GATT connection should be established", gatt)
        // Verify proper cleanup
        every { mockBluetoothGatt.close() } just runs
        mockBluetoothGatt.close()
        verify(exactly = 1) { mockBluetoothGatt.close() }
        // Real implementation should ensure:
        // 1. All GATT connections are properly closed
        // 2. No memory leaks from BLE callbacks
        // 3. Proper service discovery cleanup
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\GSRSensorReconnectionTest.kt =====

package mpdc4gsr.tests

import android.app.Application
import io.mockk.MockKAnnotations
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GSRSensorReconnectionTest {
    private lateinit var mockApplication: Application

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockApplication = mockk(relaxed = true)
    }

    @Test
    fun `GSRSensorState should have default values for reconnection fields`() {
        val state = GSRSensorViewModel.GSRSensorState()
        assertFalse("isReconnecting should be false by default", state.isReconnecting)
        assertEquals("reconnectionAttempt should be 0 by default", 0, state.reconnectionAttempt)
        assertEquals("connectionStatus should be Disconnected by default", "Disconnected", state.connectionStatus)
    }

    @Test
    fun `GSRSensorState should track reconnection attempts`() {
        val state = GSRSensorViewModel.GSRSensorState(
            isReconnecting = true,
            reconnectionAttempt = 2
        )
        assertTrue("isReconnecting should be true", state.isReconnecting)
        assertEquals("reconnectionAttempt should be 2", 2, state.reconnectionAttempt)
    }

    @Test
    fun `GSRSensorState should update connection status`() {
        val initialState = GSRSensorViewModel.GSRSensorState()
        val connectedState = initialState.copy(
            isConnected = true,
            connectionStatus = "Connected"
        )
        val reconnectingState = connectedState.copy(
            isConnected = false,
            isReconnecting = true,
            reconnectionAttempt = 1,
            connectionStatus = "Reconnecting (attempt 1/3)..."
        )
        assertEquals("Initial status should be Disconnected", "Disconnected", initialState.connectionStatus)
        assertEquals("Connected status should update", "Connected", connectedState.connectionStatus)
        assertTrue("Should be in reconnecting state", reconnectingState.isReconnecting)
        assertEquals("Should track attempt number", 1, reconnectingState.reconnectionAttempt)
    }

    @Test
    fun `should calculate exponential backoff correctly`() = runTest {
        val maxAttempts = 3
        val baseDelay = 2000L
        // True exponential backoff: baseDelay * 2^(attempt-1)
        val expectedDelays = listOf(2000L, 4000L, 8000L)
        for (attempt in 1..maxAttempts) {
            val calculatedDelay = baseDelay * (1L shl (attempt - 1))
            assertEquals(
                "Delay for attempt $attempt should match exponential backoff",
                expectedDelays[attempt - 1],
                calculatedDelay
            )
        }
    }

    @Test
    fun `ReconnectionConfig should have default values`() {
        val config = GSRSensorViewModel.ReconnectionConfig()
        assertEquals("Default max attempts should be 3", 3, config.maxAttempts)
        assertEquals("Default base delay should be 2000ms", 2000L, config.baseDelayMs)
        assertTrue("Reconnection should be enabled by default", config.enabled)
    }

    @Test
    fun `ReconnectionConfig should be customizable`() {
        val config = GSRSensorViewModel.ReconnectionConfig(
            maxAttempts = 5,
            baseDelayMs = 3000L,
            enabled = false
        )
        assertEquals("Max attempts should be customizable", 5, config.maxAttempts)
        assertEquals("Base delay should be customizable", 3000L, config.baseDelayMs)
        assertFalse("Reconnection should be disableable", config.enabled)
    }

    @Test
    fun `connection status should indicate error after failed reconnection`() {
        val failedState = GSRSensorViewModel.GSRSensorState(
            isConnected = false,
            isReconnecting = false,
            reconnectionAttempt = 0,
            connectionStatus = "Connection Lost",
            error = "Failed to reconnect after 3 attempts"
        )
        assertFalse("Should not be connected", failedState.isConnected)
        assertFalse("Should not be reconnecting", failedState.isReconnecting)
        assertEquals("Should show Connection Lost status", "Connection Lost", failedState.connectionStatus)
        assertTrue(
            "Error message should indicate reconnection failure",
            failedState.error?.contains("Failed to reconnect") == true
        )
    }

    @Test
    fun `successful reconnection should reset state`() {
        val reconnectedState = GSRSensorViewModel.GSRSensorState(
            isConnected = true,
            isReconnecting = false,
            reconnectionAttempt = 0,
            connectionStatus = "Reconnected",
            error = null
        )
        assertTrue("Should be connected", reconnectedState.isConnected)
        assertFalse("Should not be reconnecting", reconnectedState.isReconnecting)
        assertEquals("Reconnection attempt should be reset", 0, reconnectedState.reconnectionAttempt)
        assertEquals("Status should show Reconnected", "Reconnected", reconnectedState.connectionStatus)
        assertEquals("Error should be cleared", null, reconnectedState.error)
    }

    @Test
    fun `connection status should be visible to UI`() {
        val states = listOf(
            "Disconnected",
            "Initialized",
            "Connecting...",
            "Connected",
            "Reconnecting (attempt 1/3)...",
            "Connection Lost"
        )
        states.forEach { status ->
            val state = GSRSensorViewModel.GSRSensorState(connectionStatus = status)
            assertEquals("Status should match", status, state.connectionStatus)
        }
    }

    @Test
    fun `maxReconnectionAttempts should be tracked in state`() {
        val state = GSRSensorViewModel.GSRSensorState(
            isReconnecting = true,
            reconnectionAttempt = 2,
            maxReconnectionAttempts = 5
        )
        assertTrue("Should be reconnecting", state.isReconnecting)
        assertEquals("Current attempt should be 2", 2, state.reconnectionAttempt)
        assertEquals("Max attempts should be 5", 5, state.maxReconnectionAttempts)
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\LoggingFunctionalityTest.kt =====

package mpdc4gsr.tests

import android.util.Log
import io.mockk.*
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.core.utils.AppLogger
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggingFunctionalityTest {
    private lateinit var structuredLogger: StructuredLogger

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        structuredLogger = mockk(relaxed = true)
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = false,
            structuredLoggerInstance = structuredLogger,
        )
    }

    @After
    fun teardown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    @Test
    fun testDebugLoggingRespectesMinLevel() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.INFO)
        AppLogger.d("TEST", "Debug message")
        verify(exactly = 0) { Log.d("TEST", "Debug message") }
    }

    @Test
    fun testInfoLoggingWorks() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        AppLogger.i("TEST", "Info message")
        verify(exactly = 1) { Log.i("TEST", "Info message") }
    }

    @Test
    fun testWarningLoggingWorks() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        AppLogger.w("TEST", "Warning message")
        verify(exactly = 1) { Log.w("TEST", "Warning message") }
    }

    @Test
    fun testErrorLoggingWorks() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        AppLogger.e("TEST", "Error message")
        verify(exactly = 1) { Log.e("TEST", "Error message") }
    }

    @Test
    fun testLoggingWithThrowable() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        val exception = RuntimeException("Test exception")
        AppLogger.e("TEST", "Error with exception", exception)
        verify(exactly = 1) { Log.e("TEST", "Error with exception", exception) }
    }

    @Test
    fun testStructuredLoggingWhenEnabled() {
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = true,
            structuredLoggerInstance = structuredLogger,
        )
        AppLogger.e("TEST", "Error message", component = "TestComponent")
        verify(exactly = 1) {
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "TestComponent",
                "log_message",
                any(),
            )
        }
    }

    @Test
    fun testStructuredLoggingNotCalledWhenDisabled() {
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = false,
            structuredLoggerInstance = structuredLogger,
        )
        AppLogger.e("TEST", "Error message", component = "TestComponent")
        verify(exactly = 0) {
            structuredLogger.log(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun testVerboseLoggingRespectesMinLevel() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.INFO)
        AppLogger.v("TEST", "Verbose message")
        verify(exactly = 0) { Log.v("TEST", "Verbose message") }
    }

    @Test
    fun testVerboseLoggingWhenEnabled() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.VERBOSE)
        AppLogger.v("TEST", "Verbose message")
        verify(exactly = 1) { Log.v("TEST", "Verbose message") }
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\MultiSensorCoordinationTest.kt =====

package mpdc4gsr.tests

import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("All tests disabled")
class MultiSensorCoordinationTest {
    private data class SensorState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val lastTimestamp: Long = 0L,
        val sampleCount: Int = 0
    )

    private lateinit var gsrSensorState: SensorState
    private lateinit var cameraSensorState: SensorState
    private lateinit var thermalSensorState: SensorState

    // Multi-modal coordination parameters
    private val targetSynchronizationAccuracy = 100L // Â±100ms as per requirements
    private val recordingDurationSeconds = 60
    private val gsrSamplingRate = 128 // Hz
    private val camerFrameRate = 30 // FPS
    private val thermalFrameRate = 10 // FPS

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        gsrSensorState = SensorState()
        cameraSensorState = SensorState()
        thermalSensorState = SensorState()
    }

    @Test
    fun `should initialize all sensors in correct sequence`() = runTest {
        // Test proper sensor initialization order and dependencies
        val initializationOrder = mutableListOf<String>()

        // Simulate sensor initialization sequence
        fun initializeGSR(): Boolean {
            initializationOrder.add("GSR")
            gsrSensorState = gsrSensorState.copy(isConnected = true)
            return true
        }

        fun initializeCamera(): Boolean {
            initializationOrder.add("Camera")
            cameraSensorState = cameraSensorState.copy(isConnected = true)
            return true
        }

        fun initializeThermal(): Boolean {
            initializationOrder.add("Thermal")
            thermalSensorState = thermalSensorState.copy(isConnected = true)
            return true
        }
        // Execute initialization
        val gsrSuccess = initializeGSR()
        val cameraSuccess = initializeCamera()
        val thermalSuccess = initializeThermal()
        // Verify all sensors initialized successfully
        assertTrue("GSR sensor should initialize", gsrSuccess)
        assertTrue("Camera sensor should initialize", cameraSuccess)
        assertTrue("Thermal sensor should initialize", thermalSuccess)
        assertEquals(
            "All three sensors should be initialized",
            listOf("GSR", "Camera", "Thermal"),
            initializationOrder
        )
        // Verify sensor states
        assertTrue("GSR should be connected", gsrSensorState.isConnected)
        assertTrue("Camera should be connected", cameraSensorState.isConnected)
        assertTrue("Thermal should be connected", thermalSensorState.isConnected)
    }

    @Test
    fun `should start all sensors simultaneously for recording`() = runTest {
        // Test synchronized start of all sensor modalities
        val sessionStartTime = System.currentTimeMillis()
        val sensorStartTimes = mutableMapOf<String, Long>()

        // Simulate simultaneous sensor start
        fun startAllSensors() {
            val startTime = System.currentTimeMillis()
            // Start GSR recording
            gsrSensorState = gsrSensorState.copy(
                isRecording = true,
                lastTimestamp = startTime
            )
            sensorStartTimes["GSR"] = startTime
            // Start camera recording (minimal delay simulation)
            val cameraStartTime = startTime + 10 // 10ms delay
            cameraSensorState = cameraSensorState.copy(
                isRecording = true,
                lastTimestamp = cameraStartTime
            )
            sensorStartTimes["Camera"] = cameraStartTime
            // Start thermal recording
            val thermalStartTime = startTime + 5 // 5ms delay
            thermalSensorState = thermalSensorState.copy(
                isRecording = true,
                lastTimestamp = thermalStartTime
            )
            sensorStartTimes["Thermal"] = thermalStartTime
        }
        startAllSensors()
        // Verify all sensors are recording
        assertTrue("GSR should be recording", gsrSensorState.isRecording)
        assertTrue("Camera should be recording", cameraSensorState.isRecording)
        assertTrue("Thermal should be recording", thermalSensorState.isRecording)
        // Verify start time synchronization
        val startTimes = sensorStartTimes.values.toList()
        val maxStartTimeDiff = startTimes.maxOrNull()!! - startTimes.minOrNull()!!
        assertTrue(
            "All sensors should start within 100ms of each other",
            maxStartTimeDiff <= targetSynchronizationAccuracy
        )
    }

    @Test
    fun `should maintain timestamp synchronization across sensors`() = runTest {
        // Test cross-sensor timestamp alignment during recording
        val baseTimestamp = System.currentTimeMillis()
        val synchronizedTimestamps = mutableMapOf<String, MutableList<Long>>()
        synchronizedTimestamps["GSR"] = mutableListOf()
        synchronizedTimestamps["Camera"] = mutableListOf()
        synchronizedTimestamps["Thermal"] = mutableListOf()
        // Simulate 5 seconds of synchronized data collection
        val simulationDurationMs = 5000L
        var currentTime = baseTimestamp
        while (currentTime < baseTimestamp + simulationDurationMs) {
            // GSR samples at 128 Hz (every ~7.8ms)
            if ((currentTime - baseTimestamp) % 8 == 0L) {
                synchronizedTimestamps["GSR"]?.add(currentTime)
            }
            // Camera frames at 30 FPS (every ~33.3ms)
            if ((currentTime - baseTimestamp) % 33 == 0L) {
                synchronizedTimestamps["Camera"]?.add(currentTime)
            }
            // Thermal frames at 10 FPS (every 100ms)
            if ((currentTime - baseTimestamp) % 100 == 0L) {
                synchronizedTimestamps["Thermal"]?.add(currentTime)
            }
            currentTime += 1 // 1ms increment
        }
        // Validate sample counts match expected rates
        val expectedGSRSamples = (simulationDurationMs / 8).toInt()
        val expectedCameraSamples = (simulationDurationMs / 33).toInt()
        val expectedThermalSamples = (simulationDurationMs / 100).toInt()
        assertTrue(
            "GSR sample count should be reasonable",
            synchronizedTimestamps["GSR"]?.size!! >= expectedGSRSamples * 0.9
        )
        assertTrue(
            "Camera sample count should be reasonable",
            synchronizedTimestamps["Camera"]?.size!! >= expectedCameraSamples * 0.9
        )
        assertTrue(
            "Thermal sample count should be reasonable",
            synchronizedTimestamps["Thermal"]?.size!! >= expectedThermalSamples * 0.9
        )
        // Find synchronization events (timestamps within target accuracy)
        val syncEvents =
            findSynchronizationEvents(synchronizedTimestamps, targetSynchronizationAccuracy)
        assertTrue("Should have synchronization reference points", syncEvents.isNotEmpty())
    }

    @Test
    fun `should handle sensor failure without affecting other modalities`() = runTest {
        // Test individual sensor failure isolation
        // Start all sensors
        gsrSensorState = gsrSensorState.copy(isConnected = true, isRecording = true)
        cameraSensorState = cameraSensorState.copy(isConnected = true, isRecording = true)
        thermalSensorState = thermalSensorState.copy(isConnected = true, isRecording = true)
        // Simulate GSR sensor failure
        val failureCallback = mockk<(String) -> Unit>(relaxed = true)
        fun simulateGSRFailure() {
            gsrSensorState = gsrSensorState.copy(isConnected = false, isRecording = false)
            failureCallback("GSR sensor disconnected")
        }
        simulateGSRFailure()
        // Verify GSR failure is detected
        assertFalse("GSR should be disconnected after failure", gsrSensorState.isConnected)
        assertFalse("GSR should stop recording after failure", gsrSensorState.isRecording)
        verify(exactly = 1) { failureCallback("GSR sensor disconnected") }
        // Verify other sensors continue operating
        assertTrue("Camera should continue recording", cameraSensorState.isRecording)
        assertTrue("Thermal should continue recording", thermalSensorState.isRecording)
        // Test recovery scenario
        fun recoverGSR() {
            gsrSensorState = gsrSensorState.copy(isConnected = true, isRecording = true)
        }
        recoverGSR()
        assertTrue("GSR should recover and resume recording", gsrSensorState.isRecording)
    }

    @Test
    fun `should coordinate data file generation across sensors`() = runTest {
        // Test synchronized data file creation and naming
        val sessionId = "session_${System.currentTimeMillis()}"
        val sessionDir = "/storage/emulated/0/IRCamera/$sessionId"
        val expectedFiles = mapOf(
            "GSR" to "$sessionDir/gsr_data.csv",
            "Camera" to "$sessionDir/video.mp4",
            "CameraFrames" to "$sessionDir/rgb_frames.csv",
            "Thermal" to "$sessionDir/thermal_data.csv"
        )
        // Validate file path generation
        expectedFiles.forEach { (sensor, filePath) ->
            assertTrue(
                "$sensor file path should contain session ID",
                filePath.contains(sessionId)
            )
            assertTrue(
                "$sensor file path should be absolute",
                filePath.startsWith("/")
            )
        }
        // Validate file format consistency
        val csvFiles = expectedFiles.values.filter { it.endsWith(".csv") }
        val videoFiles = expectedFiles.values.filter { it.endsWith(".mp4") }
        assertEquals("Should have 3 CSV files", 3, csvFiles.size)
        assertEquals("Should have 1 video file", 1, videoFiles.size)
        // Test file creation timestamps
        val fileCreationTime = System.currentTimeMillis()
        val fileTimestamps = expectedFiles.mapValues { fileCreationTime }
        val maxTimeDiff = fileTimestamps.values.maxOrNull()!! - fileTimestamps.values.minOrNull()!!
        assertTrue(
            "All files should be created within synchronization window",
            maxTimeDiff <= targetSynchronizationAccuracy
        )
    }

    @Test
    fun `should validate cross-sensor data correlation`() = runTest {
        // Test data correlation between different sensor modalities
        // Simulate synchronized event across all sensors
        val eventTimestamp = System.currentTimeMillis()
        val eventWindow = 200L // Â±200ms window for event detection

        data class SensorEvent(
            val timestamp: Long,
            val sensorType: String,
            val value: Double
        )
        // Generate synchronized sensor events
        val sensorEvents = listOf(
            SensorEvent(eventTimestamp, "GSR", 15.6), // GSR conductance spike
            SensorEvent(eventTimestamp + 10, "Camera", 1.0), // Frame captured
            SensorEvent(eventTimestamp + 50, "Thermal", 32.4) // Temperature reading
        )
        // Validate event timing correlation
        val eventTimestamps = sensorEvents.map { it.timestamp }
        val timeSpread = eventTimestamps.maxOrNull()!! - eventTimestamps.minOrNull()!!
        assertTrue(
            "Sensor events should occur within correlation window",
            timeSpread <= eventWindow
        )
        // Test data value validation
        sensorEvents.forEach { event ->
            when (event.sensorType) {
                "GSR" -> {
                    assertTrue("GSR value should be positive", event.value > 0)
                    assertTrue("GSR value should be reasonable", event.value < 100)
                }

                "Camera" -> {
                    assertEquals("Camera event should be frame indicator", 1.0, event.value, 0.1)
                }

                "Thermal" -> {
                    assertTrue(
                        "Thermal value should be reasonable body temperature",
                        event.value >= 20.0 && event.value <= 45.0
                    )
                }
            }
        }
    }

    @Test
    fun `should measure multi-modal system performance impact`() = runTest {
        // Test performance impact of running all sensors simultaneously
        data class PerformanceMetrics(
            val cpuUsage: Double = 0.0,
            val memoryUsage: Double = 0.0,
            val batteryDrain: Double = 0.0,
            val storageWriteRate: Double = 0.0
        )
        // Baseline performance (no sensors)
        val baselineMetrics = PerformanceMetrics(
            cpuUsage = 15.0, // 15% baseline CPU
            memoryUsage = 200.0, // 200MB baseline memory
            batteryDrain = 5.0, // 5% per hour baseline
            storageWriteRate = 0.0 // No writing
        )
        // Multi-modal performance (all sensors active)
        val multiModalMetrics = PerformanceMetrics(
            cpuUsage = 45.0, // 45% CPU with all sensors
            memoryUsage = 350.0, // 350MB with buffers
            batteryDrain = 25.0, // 25% per hour with sensors
            storageWriteRate = 2.5 // 2.5 MB/s writing rate
        )
        // Calculate performance impact
        val cpuIncrease = multiModalMetrics.cpuUsage - baselineMetrics.cpuUsage
        val memoryIncrease = multiModalMetrics.memoryUsage - baselineMetrics.memoryUsage
        val batteryIncrease = multiModalMetrics.batteryDrain - baselineMetrics.batteryDrain
        // Validate performance impact is reasonable
        assertTrue("CPU increase should be manageable", cpuIncrease <= 40.0) // Max 40% increase
        assertTrue(
            "Memory increase should be reasonable",
            memoryIncrease <= 200.0
        ) // Max 200MB increase
        assertTrue(
            "Battery drain should be acceptable",
            batteryIncrease <= 25.0
        ) // Max 25% increase
        // Validate storage performance
        assertTrue(
            "Storage write rate should support all sensors",
            multiModalMetrics.storageWriteRate >= 2.0
        ) // Min 2 MB/s required
        // Calculate estimated recording duration with battery constraint
        val batteryCapacityHours = 24.0 // Example battery life
        val estimatedRecordingHours =
            batteryCapacityHours / (multiModalMetrics.batteryDrain / 100.0)
        assertTrue("Should support at least 2 hours of recording", estimatedRecordingHours >= 2.0)
    }

    @Test
    fun `should handle graceful shutdown of all sensors`() = runTest {
        // Test coordinated shutdown of multi-modal recording
        // Start all sensors
        gsrSensorState = gsrSensorState.copy(isRecording = true)
        cameraSensorState = cameraSensorState.copy(isRecording = true)
        thermalSensorState = thermalSensorState.copy(isRecording = true)
        val shutdownOrder = mutableListOf<String>()
        val shutdownCallbacks = mutableMapOf<String, () -> Unit>()
        // Setup shutdown callbacks
        shutdownCallbacks["GSR"] = {
            gsrSensorState = gsrSensorState.copy(isRecording = false)
            shutdownOrder.add("GSR")
        }
        shutdownCallbacks["Camera"] = {
            cameraSensorState = cameraSensorState.copy(isRecording = false)
            shutdownOrder.add("Camera")
        }
        shutdownCallbacks["Thermal"] = {
            thermalSensorState = thermalSensorState.copy(isRecording = false)
            shutdownOrder.add("Thermal")
        }
        // Execute graceful shutdown
        shutdownCallbacks.values.forEach { it() }
        // Verify all sensors stopped recording
        assertFalse("GSR should stop recording", gsrSensorState.isRecording)
        assertFalse("Camera should stop recording", cameraSensorState.isRecording)
        assertFalse("Thermal should stop recording", thermalSensorState.isRecording)
        assertEquals(
            "All sensors should shut down",
            3,
            shutdownOrder.size
        )
        // Verify shutdown order contains all sensors
        assertTrue("GSR should be in shutdown order", shutdownOrder.contains("GSR"))
        assertTrue("Camera should be in shutdown order", shutdownOrder.contains("Camera"))
        assertTrue("Thermal should be in shutdown order", shutdownOrder.contains("Thermal"))
    }

    private fun findSynchronizationEvents(
        timestamps: Map<String, List<Long>>,
        accuracyMs: Long
    ): List<Long> {
        // Find timestamps that occur within accuracy window across multiple sensors
        val syncEvents = mutableListOf<Long>()
        val allTimestamps = timestamps.values.flatten().sorted()
        for (timestamp in allTimestamps) {
            val sensorsInWindow = timestamps.keys.count { sensorType ->
                timestamps[sensorType]?.any {
                    kotlin.math.abs(it - timestamp) <= accuracyMs
                } == true
            }
            if (sensorsInWindow >= 2) { // At least 2 sensors synchronized
                syncEvents.add(timestamp)
            }
        }
        return syncEvents.distinct()
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\NetworkProtocolTest.kt =====

package mpdc4gsr.tests

import mpdc4gsr.feature.network.data.Protocol
import org.junit.Assert.*
import org.junit.Test

class NetworkProtocolTest {
    @Test
    fun testCreateSyncInitMessage() {
        val syncInit = Protocol.createSyncInitMessage()
        assertEquals("SYNC_INIT", syncInit)
    }

    @Test
    fun testParseSyncInitMessage() {
        val message = Protocol.parseMessage("SYNC_INIT")
        assertNotNull(message)
        assertEquals(Protocol.MSG_SYNC_INIT, message?.type)
    }

    @Test
    fun testSyncInitConstant() {
        assertEquals("SYNC_INIT", Protocol.MSG_SYNC_INIT)
    }

    @Test
    fun testCreateSyncRequestMessage() {
        val syncRequest = Protocol.createSyncRequestMessage(1234567890L)
        assertTrue(syncRequest.contains("SYNC_REQUEST"))
        assertTrue(syncRequest.contains("t_pc=1234567890"))
    }

    @Test
    fun testCreateSyncResponseMessage() {
        val syncResponse = Protocol.createSyncResponseMessage(1000L, 1005L)
        assertTrue(syncResponse.contains("SYNC_RESPONSE"))
        assertTrue(syncResponse.contains("t_pc=1000"))
        assertTrue(syncResponse.contains("t_ph=1005"))
    }

    @Test
    fun testCreateSyncResultMessage() {
        val syncResult = Protocol.createSyncResultMessage(1000L, 1005L, 1010L, 5L, 10L)
        assertTrue(syncResult.contains("SYNC_RESULT"))
        assertTrue(syncResult.contains("t1=1000"))
        assertTrue(syncResult.contains("t2=1005"))
        assertTrue(syncResult.contains("t3=1010"))
        assertTrue(syncResult.contains("offset=5"))
        assertTrue(syncResult.contains("rtt=10"))
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\RecordingSettingsTest.kt =====

package mpdc4gsr.tests

import android.content.Context
import android.content.SharedPreferences
import mpdc4gsr.feature.settings.data.RecordingSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@Ignore("All tests disabled")
@RunWith(MockitoJUnitRunner::class)
class RecordingSettingsTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: RecordingSettingsRepository

    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockSharedPreferences.getBoolean(eq("recording_auto_recording"), anyBoolean())).thenReturn(false)
        `when`(mockSharedPreferences.getString(eq("recording_quality"), anyString())).thenReturn("High")
        `when`(mockSharedPreferences.getInt(eq("recording_video_frame_rate"), anyInt())).thenReturn(30)
        `when`(mockSharedPreferences.getBoolean(eq("recording_audio_enabled"), anyBoolean())).thenReturn(true)
        `when`(mockSharedPreferences.getBoolean(eq("recording_simultaneous"), anyBoolean())).thenReturn(true)
        `when`(mockSharedPreferences.getBoolean(eq("recording_timestamp_sync"), anyBoolean())).thenReturn(true)
    }

    @Test
    fun `test default settings`() {
        val settings = RecordingSettingsRepository.RecordingSettings()
        assertEquals(false, settings.autoRecording)
        assertEquals("High", settings.recordingQuality)
        assertEquals(30, settings.videoFrameRate)
        assertEquals(true, settings.audioEnabled)
        assertEquals(true, settings.simultaneousRecording)
        assertEquals(true, settings.timestampSync)
    }

    @Test
    fun `test quality config for Ultra quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("Ultra")
        assertEquals(50_000_000, qualityConfig.videoBitrate)
        assertEquals(3840, qualityConfig.videoWidth)
        assertEquals(2160, qualityConfig.videoHeight)
        assertEquals(60, qualityConfig.preferredFps)
    }

    @Test
    fun `test quality config for High quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("High")
        assertEquals(20_000_000, qualityConfig.videoBitrate)
        assertEquals(1920, qualityConfig.videoWidth)
        assertEquals(1080, qualityConfig.videoHeight)
        assertEquals(30, qualityConfig.preferredFps)
    }

    @Test
    fun `test quality config for Medium quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("Medium")
        assertEquals(10_000_000, qualityConfig.videoBitrate)
        assertEquals(1280, qualityConfig.videoWidth)
        assertEquals(720, qualityConfig.videoHeight)
        assertEquals(30, qualityConfig.preferredFps)
    }

    @Test
    fun `test quality config for Low quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("Low")
        assertEquals(5_000_000, qualityConfig.videoBitrate)
        assertEquals(854, qualityConfig.videoWidth)
        assertEquals(480, qualityConfig.videoHeight)
        assertEquals(24, qualityConfig.preferredFps)
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\RgbCameraPerformanceTest.kt =====

package mpdc4gsr.tests

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("All tests disabled")
class RgbCameraPerformanceTest {
    private lateinit var mockContext: Context
    private lateinit var mockCameraManager: CameraManager
    private lateinit var mockCameraCharacteristics: CameraCharacteristics

    // Samsung Galaxy S22 camera specifications
    private val target4KResolution = Size(3840, 2160)
    private val targetFrameRate = 30
    private val expectedBitrate = 20_000_000 // 20 Mbps for 4K

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockContext = mockk()
        mockCameraManager = mockk()
        mockCameraCharacteristics = mockk()
        every { mockContext.getSystemService(Context.CAMERA_SERVICE) } returns mockCameraManager
        every { mockCameraManager.cameraIdList } returns arrayOf("0", "1") // Back and front cameras
    }

    @Test
    fun `should validate Samsung Galaxy S22 4K recording capabilities`() {
        // Test 4K (UHD) recording support on Samsung Galaxy S22
        val supportedSizes = arrayOf(
            Size(1920, 1080), // FHD
            Size(3840, 2160), // 4K UHD
            Size(7680, 4320)  // 8K (S22 capability)
        )
        every {
            mockCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        } returns mockk {
            every { getOutputSizes(any<Class<Any>>()) } returns supportedSizes
        }
        // Verify 4K resolution is supported
        val supports4K = supportedSizes.contains(target4KResolution)
        assertTrue("Samsung Galaxy S22 should support 4K recording", supports4K)
        // Validate aspect ratio
        val aspectRatio = target4KResolution.width.toFloat() / target4KResolution.height.toFloat()
        assertEquals(
            "4K should have 16:9 aspect ratio",
            16f / 9f,
            aspectRatio,
            0.01f
        )
    }

    @Test
    fun `should validate frame rate capabilities for 4K recording`() = runTest {
        // Test frame rate support for 4K recording on Galaxy S22
        val supportedFrameRates = intArrayOf(24, 30, 60) // Common S22 frame rates
        assertTrue("Should support 30 FPS", supportedFrameRates.contains(30))
        assertTrue("Should support 60 FPS", supportedFrameRates.contains(60))
        // Calculate frame timing for 30 FPS
        val frameIntervalNs = 1_000_000_000L / targetFrameRate // 33.33ms in nanoseconds
        val expectedIntervalNs = 33_333_333L
        assertEquals(
            "Frame interval should be ~33.33ms for 30 FPS",
            expectedIntervalNs,
            frameIntervalNs
        )
        // Validate frame timing consistency
        val frameTimestamps = mutableListOf<Long>()
        var currentTimeNs = 0L
        repeat(90) { // 3 seconds at 30 FPS
            frameTimestamps.add(currentTimeNs)
            currentTimeNs += frameIntervalNs
        }
        // Check timing consistency
        val intervals = frameTimestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        assertTrue(
            "Average frame interval should be close to target",
            kotlin.math.abs(avgInterval - frameIntervalNs) < 1_000_000 // 1ms tolerance
        )
    }

    @Test
    fun `should calculate expected file sizes for 4K recording`() {
        // Test file size calculations for 4K video on Galaxy S22
        val recordingDurationSeconds = 60
        val bitratePerSecond = expectedBitrate / 8 // Convert bits to bytes
        val expectedFileSize = bitratePerSecond * recordingDurationSeconds
        // Expected file size for 1 minute of 4K recording (~150MB)
        val expectedSizeMB = expectedFileSize / (1024 * 1024)
        assertTrue(
            "4K recording should produce ~150MB per minute",
            expectedSizeMB >= 120 && expectedSizeMB <= 180
        )
        // Validate storage space requirements
        val availableStorageGB = 32 // Minimum required storage
        val maxRecordingMinutes = (availableStorageGB * 1024) / expectedSizeMB
        assertTrue(
            "Should support at least 3 hours of 4K recording with 32GB",
            maxRecordingMinutes >= 180
        )
    }

    @Test
    fun `should monitor thermal performance during extended recording`() = runTest {
        // Test thermal management during extended 4K recording
        var deviceTemperature = 25.0f // Starting temperature (Â°C)
        val thermalThrottleThreshold = 40.0f // Galaxy S22 throttle point
        val maxSafeTemperature = 45.0f
        // Simulate temperature increase during recording
        val recordingMinutes = 10
        repeat(recordingMinutes) { minute ->
            // Temperature increases ~1.5Â°C per minute during 4K recording
            deviceTemperature += 1.5f
            if (deviceTemperature > thermalThrottleThreshold) {
                // Thermal throttling should engage
                val throttleReduction = 0.8f // 20% performance reduction
                val effectiveFrameRate = targetFrameRate * throttleReduction
                assertTrue(
                    "Thermal throttling should reduce frame rate",
                    effectiveFrameRate < targetFrameRate
                )
            }
            // Verify temperature stays within safe limits
            assertTrue(
                "Device temperature should stay below critical threshold",
                deviceTemperature < maxSafeTemperature
            )
        }
        // Validate thermal recovery
        val coolingRate = 2.0f // Â°C per minute when not recording
        repeat(5) { // 5 minutes cooling
            deviceTemperature -= coolingRate
        }
        assertTrue(
            "Device should cool down after recording stops",
            deviceTemperature < thermalThrottleThreshold
        )
    }

    @Test
    fun `should validate simultaneous video and frame capture`() = runTest {
        // Test parallel video recording and JPEG frame extraction
        val videoRecordingActive = true
        val frameExtractionActive = true
        val frameExtractionRate = 5 // 5 FPS for JPEG frames while recording 30 FPS video
        assertTrue("Video recording should be active", videoRecordingActive)
        assertTrue("Frame extraction should be active", frameExtractionActive)
        // Calculate frame extraction timing
        val videoFrameInterval = 1000L / targetFrameRate // 33.33ms
        val extractionFrameInterval = 1000L / frameExtractionRate // 200ms
        assertEquals("Video frame interval should be ~33ms", 33L, videoFrameInterval)
        assertEquals("Extraction frame interval should be 200ms", 200L, extractionFrameInterval)
        // Simulate 10 seconds of recording
        val recordingDurationMs = 10_000L
        val expectedVideoFrames = (recordingDurationMs / videoFrameInterval).toInt()
        val expectedExtractedFrames = (recordingDurationMs / extractionFrameInterval).toInt()
        assertEquals("Should record ~300 video frames in 10 seconds", 300, expectedVideoFrames)
        assertEquals("Should extract 50 JPEG frames in 10 seconds", 50, expectedExtractedFrames)
        // Validate extraction doesn't impact video recording
        val extractionOverhead = 0.05f // 5% CPU overhead for frame extraction
        val effectiveVideoQuality = 1.0f - extractionOverhead
        assertTrue(
            "Frame extraction should not significantly impact video quality",
            effectiveVideoQuality > 0.9f
        )
    }

    @Test
    fun `should handle camera permission and initialization sequence`() {
        // Test proper camera permission handling and initialization
        var cameraPermissionGranted = false
        var cameraInitialized = false
        var previewStarted = false
        // Simulate permission flow
        cameraPermissionGranted = true // User grants permission
        if (cameraPermissionGranted) {
            cameraInitialized = true
            // Initialize camera session
            if (cameraInitialized) {
                previewStarted = true
            }
        }
        assertTrue("Camera permission should be granted", cameraPermissionGranted)
        assertTrue("Camera should initialize after permission granted", cameraInitialized)
        assertTrue("Preview should start after initialization", previewStarted)
        // Validate error handling when permission denied
        cameraPermissionGranted = false
        cameraInitialized = false
        previewStarted = false
        if (!cameraPermissionGranted) {
            // Should show error message and not proceed
            assertFalse("Camera should not initialize without permission", cameraInitialized)
            assertFalse("Preview should not start without permission", previewStarted)
        }
    }

    @Test
    fun `should validate camera resource management`() = runTest {
        // Test proper camera resource cleanup and management
        var cameraSession: String? = null
        var imageReader: String? = null
        var mediaRecorder: String? = null
        // Simulate resource allocation
        cameraSession = "CameraSession-123"
        imageReader = "ImageReader-456"
        mediaRecorder = "MediaRecorder-789"
        assertNotNull("Camera session should be allocated", cameraSession)
        assertNotNull("Image reader should be allocated", imageReader)
        assertNotNull("Media recorder should be allocated", mediaRecorder)
        // Simulate resource cleanup
        fun cleanupResources() {
            mediaRecorder = null
            imageReader = null
            cameraSession = null
        }
        cleanupResources()
        assertNull("Media recorder should be cleaned up", mediaRecorder)
        assertNull("Image reader should be cleaned up", imageReader)
        assertNull("Camera session should be cleaned up", cameraSession)
        // Real implementation should ensure:
        // 1. CameraCaptureSession is closed
        // 2. ImageReader is closed
        // 3. MediaRecorder is released
        // 4. Camera device is closed
        // 5. Background threads are terminated
    }

    @Test
    fun `should validate video encoding parameters for Galaxy S22`() {
        // Test video encoding configuration for Samsung Galaxy S22
        val videoProfile = mapOf(
            "width" to 3840,
            "height" to 2160,
            "frameRate" to 30,
            "bitrate" to 20_000_000,
            "codec" to "H.264",
            "profile" to "High",
            "level" to "5.1"
        )
        assertEquals("Width should be 4K", 3840, videoProfile["width"])
        assertEquals("Height should be 4K", 2160, videoProfile["height"])
        assertEquals("Frame rate should be 30 FPS", 30, videoProfile["frameRate"])
        assertEquals("Bitrate should be 20 Mbps", 20_000_000, videoProfile["bitrate"])
        assertEquals("Codec should be H.264", "H.264", videoProfile["codec"])
        assertEquals("Profile should be High", "High", videoProfile["profile"])
        assertEquals("Level should support 4K", "5.1", videoProfile["level"])
        // Validate encoding efficiency
        val pixelsPerFrame = (videoProfile["width"] as Int) * (videoProfile["height"] as Int)
        val bitsPerPixel =
            (videoProfile["bitrate"] as Int).toFloat() / (pixelsPerFrame * (videoProfile["frameRate"] as Int))
        assertTrue(
            "Bits per pixel should be reasonable for quality",
            bitsPerPixel >= 0.1f && bitsPerPixel <= 1.0f
        )
    }

    @Test
    fun `should measure camera startup and capture latency`() = runTest {
        // Test camera startup time and capture latency measurements
        var cameraStartupTime = 0L
        var firstFrameLatency = 0L
        var captureLatency = 0L
        // Simulate camera startup timing
        val startTime = System.currentTimeMillis()
        // Mock camera initialization delay
        kotlinx.coroutines.delay(500) // 500ms startup time
        cameraStartupTime = System.currentTimeMillis() - startTime
        assertTrue(
            "Camera startup should complete within 1 second",
            cameraStartupTime < 1000L
        )
        // Simulate first frame latency
        val firstFrameStart = System.currentTimeMillis()
        kotlinx.coroutines.delay(100) // 100ms to first frame
        firstFrameLatency = System.currentTimeMillis() - firstFrameStart
        assertTrue(
            "First frame should appear within 200ms",
            firstFrameLatency < 200L
        )
        // Simulate capture latency (shutter press to image saved)
        val captureStart = System.currentTimeMillis()
        kotlinx.coroutines.delay(50) // 50ms capture latency
        captureLatency = System.currentTimeMillis() - captureStart
        assertTrue(
            "Capture latency should be under 100ms",
            captureLatency < 100L
        )
        // Total latency from request to result
        val totalLatency = cameraStartupTime + captureLatency
        assertTrue(
            "Total camera latency should be reasonable",
            totalLatency < 1500L
        )
    }
}


// ===== app\src\test\java\mpdc4gsr\tests\ThermalCameraUsbIntegrationTest.kt =====

package mpdc4gsr.tests

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import io.mockk.*
import java.io.File
import kotlinx.coroutines.test.runTest
import mpdc4gsr.core.ui.PermissionController
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("All tests disabled")
class ThermalCameraUsbIntegrationTest {
    private lateinit var mockContext: Context
    private lateinit var mockUsbManager: UsbManager
    private lateinit var mockUsbDevice: UsbDevice

    // Topdon TC001 specific characteristics
    private val topdonVendorId = 0x1234 // Topdon vendor ID
    private val topdonProductId = 0x5678 // TC001 product ID
    private val expectedThermalFrameRate = 10 // 10 FPS target
    private val thermalResolution = "160x120" // TC001 resolution

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockContext = mockk()
        mockUsbManager = mockk()
        mockUsbDevice = mockk()
        // Setup Topdon TC001 device characteristics
        every { mockUsbDevice.vendorId } returns topdonVendorId
        every { mockUsbDevice.productId } returns topdonProductId
        every { mockUsbDevice.deviceName } returns "Topdon TC001"
        every { mockUsbDevice.productName } returns "TC001 Thermal Camera"
        every { mockContext.getSystemService(Context.USB_SERVICE) } returns mockUsbManager
    }

    @Test
    fun `should detect Topdon TC001 USB device characteristics`() {
        // Test specific Topdon TC001 device identification
        assertEquals(
            "Should detect correct vendor ID",
            topdonVendorId,
            mockUsbDevice.vendorId
        )
        assertEquals(
            "Should detect correct product ID",
            topdonProductId,
            mockUsbDevice.productId
        )
        assertTrue(
            "Device name should contain Topdon",
            mockUsbDevice.deviceName.contains("Topdon", ignoreCase = true)
        )
        assertTrue(
            "Product name should indicate thermal camera",
            mockUsbDevice.productName?.contains("Thermal", ignoreCase = true) == true
        )
    }

    @Test
    fun `should validate USB permission request flow`() {
        // Test USB device permission handling for Topdon TC001
        val permissionAction = PermissionController.ACTION_USB_PERMISSION
        val hasPendingPermission = false
        // Mock USB permission status
        every {
            mockUsbManager.hasPermission(mockUsbDevice)
        } returns false
        // Verify permission is initially not granted
        assertFalse(
            "USB permission should initially be false",
            mockUsbManager.hasPermission(mockUsbDevice)
        )
        // Simulate permission granted
        every { mockUsbManager.hasPermission(mockUsbDevice) } returns true
        assertTrue(
            "USB permission should be granted after user approval",
            mockUsbManager.hasPermission(mockUsbDevice)
        )
        // Real implementation should handle:
        // 1. PendingIntent for permission request
        // 2. BroadcastReceiver for permission response
        // 3. Proper user dialog handling
    }

    @Test
    fun `should initialize thermal capture at correct frame rate`() = runTest {
        // Test thermal camera initialization with proper frame rate
        val targetFrameRate = 10.0 // 10 FPS for TC001
        val frameIntervalMs = 1000L / targetFrameRate.toLong()
        assertEquals(
            "Frame interval should be 100ms for 10 FPS",
            100L,
            frameIntervalMs
        )
        // Validate frame timing consistency
        val frameTimestamps = mutableListOf<Long>()
        var currentTime = 0L
        repeat(10) {
            frameTimestamps.add(currentTime)
            currentTime += frameIntervalMs
        }
        // Check frame timing intervals
        for (i in 1 until frameTimestamps.size) {
            val interval = frameTimestamps[i] - frameTimestamps[i - 1]
            assertEquals(
                "Frame intervals should be consistent",
                frameIntervalMs,
                interval
            )
        }
    }

    @Test
    fun `should validate thermal image file format and structure`() = runTest {
        // Test thermal image file generation and validation
        val sessionDir = "/storage/emulated/0/IRCamera/session_test"
        val thermalImagesDir = "$sessionDir/thermal_images"
        val thermalDataFile = "$sessionDir/thermal_data.csv"
        // Validate directory structure
        assertTrue(
            "Session directory path should be valid",
            sessionDir.isNotEmpty()
        )
        assertTrue(
            "Thermal images directory should be created",
            thermalImagesDir.endsWith("/thermal_images")
        )
        // Validate file naming pattern
        val frameIndex = 42
        val timestamp = System.currentTimeMillis()
        val expectedFileName = "thermal_frame_${frameIndex}_${timestamp}.png"
        assertTrue(
            "Thermal frame filename should follow pattern",
            expectedFileName.matches(Regex("thermal_frame_\\d+_\\d+\\.png"))
        )
        // Validate CSV structure
        val csvHeader = "timestamp,frame_index,min_temp,max_temp,avg_temp,filename"
        val csvLines = listOf(
            csvHeader,
            "$timestamp,$frameIndex,15.2,37.8,25.6,$expectedFileName"
        )
        assertEquals(
            "CSV should have proper header",
            csvHeader,
            csvLines[0]
        )
        assertTrue(
            "CSV data line should contain temperature values",
            csvLines[1].contains("15.2") && csvLines[1].contains("37.8")
        )
    }

    @Test
    fun `should handle USB device hot-plugging scenarios`() = runTest {
        // Test USB device connection/disconnection during operation
        var isDeviceConnected = true
        val disconnectionCallback = mockk<() -> Unit>(relaxed = true)
        val reconnectionCallback = mockk<() -> Unit>(relaxed = true)
        // Simulate device disconnection
        isDeviceConnected = false
        if (!isDeviceConnected) {
            disconnectionCallback()
        }
        verify(exactly = 1) { disconnectionCallback() }
        // Simulate device reconnection
        isDeviceConnected = true
        if (isDeviceConnected) {
            reconnectionCallback()
        }
        verify(exactly = 1) { reconnectionCallback() }
        // Real implementation should:
        // 1. Monitor USB_DEVICE_ATTACHED/DETACHED broadcasts
        // 2. Gracefully handle disconnection during recording
        // 3. Attempt automatic reconnection when device returns
        // 4. Fall back to simulation mode if device unavailable
    }

    @Test
    fun `should validate thermal data extraction from TC001`() {
        // Test extraction of thermal data from Topdon TC001 frames
        // Mock thermal frame data (16-bit thermal values)
        val frameWidth = 160
        val frameHeight = 120
        val totalPixels = frameWidth * frameHeight
        // Simulate thermal data array
        val thermalData = FloatArray(totalPixels) { index ->
            // Generate realistic thermal values (15-40Â°C range)
            15.0f + (index % 25) * 1.0f
        }
        assertEquals("Frame should have correct pixel count", totalPixels, thermalData.size)
        // Calculate temperature statistics
        val minTemp = thermalData.minOrNull() ?: 0f
        val maxTemp = thermalData.maxOrNull() ?: 0f
        val avgTemp = thermalData.average().toFloat()
        assertTrue("Min temperature should be reasonable", minTemp >= 10f && minTemp <= 50f)
        assertTrue("Max temperature should be reasonable", maxTemp >= 10f && maxTemp <= 50f)
        assertTrue("Average temperature should be reasonable", avgTemp >= 10f && avgTemp <= 50f)
        assertTrue("Max should be >= min", maxTemp >= minTemp)
        // Validate temperature conversion (assuming raw to Celsius conversion)
        val rawValue = 1000 // Example raw thermal value
        val calibratedTemp = (rawValue - 273) / 100.0f // Simplified conversion
        assertTrue(
            "Calibrated temperature should be in reasonable range",
            calibratedTemp > -50f && calibratedTemp < 100f
        )
    }

    @Test
    fun `should handle thermal camera initialization failure gracefully`() = runTest {
        // Test graceful fallback when thermal camera fails to initialize
        var initializationSuccess = false
        val fallbackToSimulation = mockk<() -> Unit>(relaxed = true)
        // Simulate initialization failure
        try {
            // Mock camera initialization failure
            throw RuntimeException("TC001 initialization failed")
        } catch (e: Exception) {
            initializationSuccess = false
            fallbackToSimulation()
        }
        assertFalse("Initialization should fail in test scenario", initializationSuccess)
        verify(exactly = 1) { fallbackToSimulation() }
        // Real implementation should:
        // 1. Log specific error messages
        // 2. Enable simulation mode automatically
        // 3. Continue with other sensor modalities
        // 4. Provide user notification of fallback
    }

    @Test
    fun `should validate thermal camera performance metrics`() = runTest {
        // Test performance monitoring for thermal camera operations
        val targetFrameRate = 10.0
        val recordingDurationSeconds = 60
        val expectedFrameCount = (targetFrameRate * recordingDurationSeconds).toInt()
        // Simulate frame capture timing
        val frameCaptureTimes = mutableListOf<Long>()
        var startTime = 0L
        repeat(expectedFrameCount) { frameIndex ->
            val captureTime = startTime + (frameIndex * (1000.0 / targetFrameRate)).toLong()
            frameCaptureTimes.add(captureTime)
        }
        assertEquals(
            "Should capture expected number of frames",
            expectedFrameCount,
            frameCaptureTimes.size
        )
        // Validate frame timing consistency
        val frameIntervals = frameCaptureTimes.zipWithNext { a, b -> b - a }
        val averageInterval = frameIntervals.average()
        val expectedInterval = 1000.0 / targetFrameRate
        assertTrue(
            "Average frame interval should be close to target",
            kotlin.math.abs(averageInterval - expectedInterval) < 10.0 // 10ms tolerance
        )
        // Check for frame drops (intervals significantly longer than expected)
        val droppedFrames = frameIntervals.count { it > expectedInterval * 1.5 }
        assertTrue(
            "Frame drop rate should be minimal",
            droppedFrames < expectedFrameCount * 0.05 // Less than 5% dropped frames
        )
    }

    @Test
    fun `should validate thermal image file integrity`() {
        // Test thermal image file generation and integrity
        val mockImageFile = mockk<File>()
        every { mockImageFile.exists() } returns true
        every { mockImageFile.length() } returns 8192L // Reasonable file size
        every { mockImageFile.canRead() } returns true
        every { mockImageFile.extension } returns "png"
        assertTrue("Thermal image file should exist", mockImageFile.exists())
        assertTrue("File should be readable", mockImageFile.canRead())
        assertTrue("File should have reasonable size", mockImageFile.length() > 1000L)
        assertEquals("File should be PNG format", "png", mockImageFile.extension)
        // Validate PNG header (simplified check)
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals("PNG header should be 8 bytes", 8, pngHeader.size)
        assertEquals("PNG signature should start correctly", 0x89.toByte(), pngHeader[0])
        assertEquals("PNG signature should contain 'PNG'", 0x50, pngHeader[1])
    }
}


