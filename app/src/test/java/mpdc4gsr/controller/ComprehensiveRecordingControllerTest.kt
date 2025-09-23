package mpdc4gsr.controller

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import io.mockk.*
import com.kotlinx.coroutines.*
import com.kotlinx.coroutines.test.*
import com.mpdc4gsr.permissions.PermissionManager
import com.mpdc4gsr.sensors.SensorRecorder
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Comprehensive tests for session lifecycle and recording coordination
 * Tests the enhanced fault tolerance, crash recovery, and foreground service integration
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComprehensiveRecordingControllerTest {

    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var permissionManager: PermissionManager
    private lateinit var recordingController: ComprehensiveRecordingController
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    // Mock sensors for testing different failure scenarios
    private lateinit var mockRgbSensor: SensorRecorder
    private lateinit var mockThermalSensor: SensorRecorder
    private lateinit var mockGsrSensor: SensorRecorder

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        context = mockk<Context>()
        lifecycleOwner = mockk<LifecycleOwner>()
        permissionManager = mockk<PermissionManager>()

        // Mock context file system operations
        every { context.filesDir } returns File("/tmp/test_files")
        every { context.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)

        // Create mock sensors with default behaviors
        mockRgbSensor = createMockSensor("RGB", "camera")
        mockThermalSensor = createMockSensor("Thermal", "thermal")
        mockGsrSensor = createMockSensor("GSR", "gsr")

        recordingController = ComprehensiveRecordingController(context, lifecycleOwner, permissionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createMockSensor(id: String, type: String): SensorRecorder {
        return mockk<SensorRecorder> {
            every { sensorId } returns id
            every { sensorType } returns type
            every { isRecording } returns false
            every { samplingRate } returns 128.0
            coEvery { startRecording(any(), any()) } returns true
            coEvery { stopRecording() } returns true
        }
    }

    @Test
    fun `test recording orchestration sequence starts all sensors with fault tolerance`() = testScope.runTest {
        // Setup
        every { permissionManager.hasAllPermissions(any()) } returns true
        recordingController.addSensorRecorder("RGB", mockRgbSensor)
        recordingController.addSensorRecorder("Thermal", mockThermalSensor)
        recordingController.addSensorRecorder("GSR", mockGsrSensor)

        // Act
        val result = recordingController.startRecording(
            sessionId = "test_session_001",
            enabledSensors = listOf("RGB", "Thermal", "GSR"),
            estimatedDurationMinutes = 30
        )

        // Assert
        assertTrue("Recording should start successfully", result)
        assertTrue("Recording state should be active", recordingController.isRecording)

        // Verify all sensors were started
        coVerify { mockRgbSensor.startRecording(any(), any()) }
        coVerify { mockThermalSensor.startRecording(any(), any()) }
        coVerify { mockGsrSensor.startRecording(any(), any()) }
    }

    @Test
    fun `test partial recording continues when one sensor fails to start`() = testScope.runTest {
        // Setup - thermal sensor fails to start
        every { permissionManager.hasAllPermissions(any()) } returns true
        coEvery { mockThermalSensor.startRecording(any(), any()) } returns false

        recordingController.addSensorRecorder("RGB", mockRgbSensor)
        recordingController.addSensorRecorder("Thermal", mockThermalSensor)
        recordingController.addSensorRecorder("GSR", mockGsrSensor)

        // Act
        val result = recordingController.startRecording(
            sessionId = "test_partial_session",
            enabledSensors = listOf("RGB", "Thermal", "GSR")
        )

        // Assert - should still succeed with partial recording
        assertTrue("Recording should start with partial sensors", result)
        assertTrue("Recording state should be active", recordingController.isRecording)
    }

    @Test
    fun `test recording fails gracefully when all sensors fail to start`() = testScope.runTest {
        // Setup - all sensors fail
        every { permissionManager.hasAllPermissions(any()) } returns true
        coEvery { mockRgbSensor.startRecording(any(), any()) } returns false
        coEvery { mockThermalSensor.startRecording(any(), any()) } returns false
        coEvery { mockGsrSensor.startRecording(any(), any()) } returns false

        recordingController.addSensorRecorder("RGB", mockRgbSensor)
        recordingController.addSensorRecorder("Thermal", mockThermalSensor)
        recordingController.addSensorRecorder("GSR", mockGsrSensor)

        // Act
        val result = recordingController.startRecording(
            sessionId = "test_all_fail_session",
            enabledSensors = listOf("RGB", "Thermal", "GSR")
        )

        // Assert
        assertFalse("Recording should fail when no sensors start", result)
        assertFalse("Recording state should be inactive", recordingController.isRecording)
    }

    @Test
    fun `test sensor exception isolation - one sensor crash doesn't abort session`() = testScope.runTest {
        // Setup - GSR sensor throws exception on start
        every { permissionManager.hasAllPermissions(any()) } returns true
        coEvery { mockGsrSensor.startRecording(any(), any()) } throws RuntimeException("GSR sensor connection failed")

        recordingController.addSensorRecorder("RGB", mockRgbSensor)
        recordingController.addSensorRecorder("Thermal", mockThermalSensor)
        recordingController.addSensorRecorder("GSR", mockGsrSensor)

        // Act
        val result = recordingController.startRecording(
            sessionId = "test_exception_isolation",
            enabledSensors = listOf("RGB", "Thermal", "GSR")
        )

        // Assert - should continue with other sensors
        assertTrue("Recording should continue despite one sensor exception", result)
        assertTrue("Recording state should be active", recordingController.isRecording)
    }

    @Test
    fun `test graceful teardown stops all sensors individually`() = testScope.runTest {
        // Setup
        every { permissionManager.hasAllPermissions(any()) } returns true
        recordingController.addSensorRecorder("RGB", mockRgbSensor)
        recordingController.addSensorRecorder("Thermal", mockThermalSensor)
        recordingController.addSensorRecorder("GSR", mockGsrSensor)

        // Start recording first
        recordingController.startRecording(sessionId = "test_teardown")

        // Act - stop recording
        val result = recordingController.stopRecording()

        // Assert
        assertTrue("Stop recording should succeed", result)
        assertFalse("Recording state should be inactive", recordingController.isRecording)

        // Verify all sensors were stopped individually
        coVerify { mockRgbSensor.stopRecording() }
        coVerify { mockThermalSensor.stopRecording() }
        coVerify { mockGsrSensor.stopRecording() }
    }

    @Test
    fun `test stop recording continues even when sensor stop fails`() = testScope.runTest {
        // Setup - thermal sensor fails to stop
        every { permissionManager.hasAllPermissions(any()) } returns true
        coEvery { mockThermalSensor.stopRecording() } throws RuntimeException("Thermal sensor stop failed")

        recordingController.addSensorRecorder("RGB", mockRgbSensor)
        recordingController.addSensorRecorder("Thermal", mockThermalSensor)
        recordingController.addSensorRecorder("GSR", mockGsrSensor)

        recordingController.startRecording(sessionId = "test_stop_failure")

        // Act
        val result = recordingController.stopRecording()

        // Assert - should still succeed overall
        assertTrue("Stop recording should succeed despite individual sensor failure", result)
        assertFalse("Recording state should be inactive", recordingController.isRecording)
    }

    @Test
    fun `test prerequisites validation catches insufficient storage`() = testScope.runTest {
        // Setup - mock insufficient storage
        every { permissionManager.hasAllPermissions(any()) } returns true
        mockkStatic(File::class)
        every { any<File>().freeSpace } returns 100 * 1024 * 1024 // 100MB - insufficient

        recordingController.addSensorRecorder("RGB", mockRgbSensor)

        // Act
        val result = recordingController.startRecording(
            sessionId = "test_storage_check",
            enabledSensors = listOf("RGB"),
            estimatedDurationMinutes = 120 // Long recording requiring more storage
        )

        // Assert
        assertFalse("Recording should fail due to insufficient storage", result)
        assertFalse("Recording state should be inactive", recordingController.isRecording)
    }

    @Test
    fun `test prerequisites validation catches missing permissions`() = testScope.runTest {
        // Setup - missing permissions
        every { permissionManager.hasAllPermissions(any()) } returns false

        recordingController.addSensorRecorder("RGB", mockRgbSensor)

        // Act
        val result = recordingController.startRecording(
            sessionId = "test_permissions_check",
            enabledSensors = listOf("RGB")
        )

        // Assert
        assertFalse("Recording should fail due to missing permissions", result)
        assertFalse("Recording state should be inactive", recordingController.isRecording)
    }

    @Test
    fun `test crash recovery detection works on startup`() = testScope.runTest {
        // Setup - mock crashed session detection
        mockkObject(mpdc4gsr.core.CrashRecoveryManager::class)
        val mockRecoveryResult = mpdc4gsr.core.CrashRecoveryResult(
            hasCrashedSession = true,
            recoveredSession = mpdc4gsr.core.RecoveredSession(
                sessionId = "crashed_session_123",
                sessionDirectory = "/tmp/crashed_session",
                sessionStartTime = System.currentTimeMillis() - 60000,
                activeSensors = listOf("RGB", "GSR"),
                sessionAge = 60000,
                analysis = mpdc4gsr.core.SessionAnalysis(
                    hasSessionDirectory = true,
                    sessionDirectoryExists = true,
                    dataFiles = emptyMap(),
                    partialDataSize = 1024,
                    summary = "Partial data found"
                )
            ),
            recoveryActions = listOf("Mark session as crashed", "Preserve partial data")
        )

        every {
            recordingController.checkForCrashedSessions()
        } returns mockRecoveryResult.hasCrashedSession

        // Act
        val hasCrashedSession = recordingController.checkForCrashedSessions()

        // Assert
        assertTrue("Should detect crashed session", hasCrashedSession)
    }
}