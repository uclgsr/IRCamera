package com.topdon.tc001.controller

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@RunWith(RobolectricTestRunner::class)
class RecordingControllerTest {
    @Mock
    private lateinit var mockLifecycleOwner: LifecycleOwner

    @Mock
    private lateinit var mockRgbSensor: SensorRecorder

    @Mock
    private lateinit var mockThermalSensor: SensorRecorder

    @Mock
    private lateinit var mockGsrSensor: SensorRecorder

    private lateinit var context: Context
    private lateinit var recordingController: RecordingController
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        testScope = TestScope()

        // Configure mock sensors
        whenever(mockRgbSensor.sensorId).thenReturn("rgb_camera_1")
        whenever(mockRgbSensor.sensorType).thenReturn("RGB Camera")
        whenever(mockRgbSensor.samplingRate).thenReturn(30.0)
        whenever(mockRgbSensor.isRecording).thenReturn(false)
        whenever(mockRgbSensor.getErrorFlow()).thenReturn(emptyFlow())
        whenever(mockRgbSensor.getRecordingStats()).thenReturn(createMockStats("rgb_camera_1", "RGB Camera"))

        whenever(mockThermalSensor.sensorId).thenReturn("thermal_camera_1")
        whenever(mockThermalSensor.sensorType).thenReturn("Thermal Camera")
        whenever(mockThermalSensor.samplingRate).thenReturn(9.0)
        whenever(mockThermalSensor.isRecording).thenReturn(false)
        whenever(mockThermalSensor.getErrorFlow()).thenReturn(emptyFlow())
        whenever(mockThermalSensor.getRecordingStats()).thenReturn(createMockStats("thermal_camera_1", "Thermal Camera"))

        whenever(mockGsrSensor.sensorId).thenReturn("gsr_shimmer_1")
        whenever(mockGsrSensor.sensorType).thenReturn("GSR Sensor")
        whenever(mockGsrSensor.samplingRate).thenReturn(128.0)
        whenever(mockGsrSensor.isRecording).thenReturn(false)
        whenever(mockGsrSensor.getErrorFlow()).thenReturn(emptyFlow())
        whenever(mockGsrSensor.getRecordingStats()).thenReturn(createMockStats("gsr_shimmer_1", "GSR Sensor"))

        recordingController = RecordingController(context, mockLifecycleOwner)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    private fun createMockStats(
        sensorId: String,
        sensorType: String,
    ): RecordingStats {
        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = 1000L,
            totalSamplesRecorded = 100L,
            averageDataRate = 10.0,
            droppedSamples = 0L,
            storageUsedMB = 1.0,
            syncMarkersCount = 1,
            lastSampleTimestampNs = System.nanoTime(),
        )
    }

    @Test
    fun `test all sensors start successfully`() =
        testScope.runTest {
            // Arrange - all sensors initialize and start successfully
            whenever(mockRgbSensor.initialize()).thenReturn(true)
            whenever(mockThermalSensor.initialize()).thenReturn(true)
            whenever(mockGsrSensor.initialize()).thenReturn(true)

            whenever(mockRgbSensor.startRecording(any())).thenReturn(true)
            whenever(mockThermalSensor.startRecording(any())).thenReturn(true)
            whenever(mockGsrSensor.startRecording(any())).thenReturn(true)

            // Manually add sensors to simulate successful initialization
            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            sensorMap["thermal_camera_1"] = mockThermalSensor
            sensorMap["gsr_shimmer_1"] = mockGsrSensor

            // Act
            val result = recordingController.startRecording("/tmp/session1")

            // Assert
            assertTrue(result, "All sensors should start successfully")
            assertTrue(recordingController.isRecording, "Recording should be active")

            val summary = recordingController.getSensorStatusSummary()
            assertEquals(3, summary.totalSensorsInitialized)
            assertEquals(3, summary.totalSensorsRecording)
            assertEquals("All sensors recording", summary.statusMessage)
        }

    @Test
    fun `test partial sensor start - RGB and Thermal succeed, GSR fails`() =
        testScope.runTest {
            // Arrange - GSR fails to start, others succeed
            whenever(mockRgbSensor.initialize()).thenReturn(true)
            whenever(mockThermalSensor.initialize()).thenReturn(true)
            whenever(mockGsrSensor.initialize()).thenReturn(true)

            whenever(mockRgbSensor.startRecording(any())).thenReturn(true)
            whenever(mockThermalSensor.startRecording(any())).thenReturn(true)
            whenever(mockGsrSensor.startRecording(any())).thenReturn(false) // GSR fails

            whenever(mockRgbSensor.isRecording).thenReturn(true)
            whenever(mockThermalSensor.isRecording).thenReturn(true)
            whenever(mockGsrSensor.isRecording).thenReturn(false)

            // Manually add sensors to simulate successful initialization
            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            sensorMap["thermal_camera_1"] = mockThermalSensor
            sensorMap["gsr_shimmer_1"] = mockGsrSensor

            // Act
            val result = recordingController.startRecording("/tmp/session2")

            // Assert
            assertTrue(result, "Recording should succeed with partial sensors")
            assertTrue(recordingController.isRecording, "Recording should be active")

            val summary = recordingController.getSensorStatusSummary()
            assertEquals(3, summary.totalSensorsInitialized)
            assertEquals(2, summary.totalSensorsRecording) // Only RGB and Thermal
            assertTrue(summary.hasPartialRecording)
            assertEquals("Partial recording: 2/3 sensors active", summary.statusMessage)
        }

    @Test
    fun `test sensor start with exception - should not crash session`() =
        testScope.runTest {
            // Arrange - GSR throws exception, others succeed
            whenever(mockRgbSensor.initialize()).thenReturn(true)
            whenever(mockThermalSensor.initialize()).thenReturn(true)
            whenever(mockGsrSensor.initialize()).thenReturn(true)

            whenever(mockRgbSensor.startRecording(any())).thenReturn(true)
            whenever(mockThermalSensor.startRecording(any())).thenReturn(true)
            whenever(mockGsrSensor.startRecording(any())).thenThrow(RuntimeException("GSR connection failed"))

            whenever(mockRgbSensor.isRecording).thenReturn(true)
            whenever(mockThermalSensor.isRecording).thenReturn(true)
            whenever(mockGsrSensor.isRecording).thenReturn(false)

            // Manually add sensors to simulate successful initialization
            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            sensorMap["thermal_camera_1"] = mockThermalSensor
            sensorMap["gsr_shimmer_1"] = mockGsrSensor

            // Act
            val result = recordingController.startRecording("/tmp/session3")

            // Assert
            assertTrue(result, "Recording should succeed despite GSR exception")
            assertTrue(recordingController.isRecording, "Recording should be active")

            val summary = recordingController.getSensorStatusSummary()
            assertEquals(3, summary.totalSensorsInitialized)
            assertEquals(2, summary.totalSensorsRecording) // Only RGB and Thermal
        }

    @Test
    fun `test all sensors fail - session should not start`() =
        testScope.runTest {
            // Arrange - all sensors fail to start
            whenever(mockRgbSensor.initialize()).thenReturn(true)
            whenever(mockThermalSensor.initialize()).thenReturn(true)
            whenever(mockGsrSensor.initialize()).thenReturn(true)

            whenever(mockRgbSensor.startRecording(any())).thenReturn(false)
            whenever(mockThermalSensor.startRecording(any())).thenReturn(false)
            whenever(mockGsrSensor.startRecording(any())).thenReturn(false)

            // Manually add sensors to simulate successful initialization
            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            sensorMap["thermal_camera_1"] = mockThermalSensor
            sensorMap["gsr_shimmer_1"] = mockGsrSensor

            // Act
            val result = recordingController.startRecording("/tmp/session4")

            // Assert
            assertFalse(result, "Recording should fail when all sensors fail")
            assertFalse(recordingController.isRecording, "Recording should not be active")

            val summary = recordingController.getSensorStatusSummary()
            assertEquals(3, summary.totalSensorsInitialized)
            assertEquals(0, summary.totalSensorsRecording)
        }

    @Test
    fun `test sensor status summary reflects reality`() =
        testScope.runTest {
            // Arrange - only RGB initializes successfully
            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            // Thermal and GSR not added to simulate initialization failure

            // Act
            val summary = recordingController.getSensorStatusSummary()

            // Assert
            assertEquals(3, summary.totalSensorsConfigured) // Total expected
            assertEquals(1, summary.totalSensorsInitialized) // Only RGB
            assertEquals(0, summary.totalSensorsRecording) // None recording yet
            assertTrue(summary.hasFailedSensors) // 2 out of 3 failed to initialize
            assertEquals("Sensors ready but not recording", summary.statusMessage)
        }

    @Test
    fun `test sensor restart during active session`() =
        testScope.runTest {
            // Arrange - Set up an active recording session with one failed sensor
            whenever(mockRgbSensor.initialize()).thenReturn(true)
            whenever(mockThermalSensor.initialize()).thenReturn(true)
            whenever(mockGsrSensor.initialize()).thenReturn(true)

            whenever(mockRgbSensor.startRecording(any())).thenReturn(true)
            whenever(mockThermalSensor.startRecording(any())).thenReturn(true)
            whenever(mockGsrSensor.startRecording(any())).thenReturn(false) // Initially fails

            whenever(mockRgbSensor.isRecording).thenReturn(true)
            whenever(mockThermalSensor.isRecording).thenReturn(true)
            whenever(mockGsrSensor.isRecording).thenReturn(false) // Not recording initially

            // Set up active session
            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            sensorMap["thermal_camera_1"] = mockThermalSensor
            sensorMap["gsr_shimmer_1"] = mockGsrSensor

            // Start recording session
            val sessionStarted = recordingController.startRecording("/tmp/session_restart")
            assertTrue(sessionStarted, "Session should start with 2/3 sensors")

            // Now simulate GSR sensor recovery - it can now start successfully
            whenever(mockGsrSensor.startRecording(any())).thenReturn(true)
            whenever(mockGsrSensor.isRecording).thenReturn(true)

            // Act - attempt to restart the failed GSR sensor
            val restartSuccess = recordingController.attemptSensorRestart("gsr_shimmer_1")

            // Assert
            assertTrue(restartSuccess, "GSR sensor should restart successfully")
            verify(mockGsrSensor).initialize() // Should reinitialize
            verify(mockGsrSensor, times(2)).startRecording(any()) // Should attempt start twice (initial + restart)
        }

    @Test
    fun `test status report generation`() =
        testScope.runTest {
            // Arrange - mixed sensor states
            whenever(mockRgbSensor.isRecording).thenReturn(true)
            whenever(mockThermalSensor.isRecording).thenReturn(false)

            val field = RecordingController::class.java.getDeclaredField("sensorRecorders")
            field.isAccessible = true
            val sensorMap = field.get(recordingController) as MutableMap<String, SensorRecorder>
            sensorMap["rgb_camera_1"] = mockRgbSensor
            sensorMap["thermal_camera_1"] = mockThermalSensor

            // Act
            val statusReport = recordingController.getStatusReport()

            // Assert
            assertTrue(statusReport.contains("Recording Controller Status"))
            assertTrue(statusReport.contains("RGB Camera"))
            assertTrue(statusReport.contains("Thermal Camera"))
            assertTrue(statusReport.contains("Individual Sensors:"))
        }
}
