package mpdc4gsr.controller

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import io.mockk.*
import com.kotlinx.coroutines.*
import com.kotlinx.coroutines.flow.emptyFlow
import com.kotlinx.coroutines.test.*
import com.mpdc4gsr.sensors.RecordingStats
import com.mpdc4gsr.sensors.SensorRecorder
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File


@OptIn(ExperimentalCoroutinesApi::class)
class RecordingControllerTest {

    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var recordingController: RecordingController
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

        // Create mock sensors with default behaviors
        mockRgbSensor = createMockSensor("RGB", "camera")
        mockThermalSensor = createMockSensor("Thermal", "thermal")
        mockGsrSensor = createMockSensor("GSR", "gsr")

        recordingController = RecordingController(context, lifecycleOwner)
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
            every { getStatusFlow() } returns emptyFlow()
            every { getErrorFlow() } returns emptyFlow()
            every { getRecordingStats() } returns RecordingStats(
                sensorId = id,
                sensorType = type,
                sessionDurationMs = 0,
                totalSamplesRecorded = 0,
                averageDataRate = 0.0,
                droppedSamples = 0,
                storageUsedMB = 0.0,
                syncMarkersCount = 0,
                lastSampleTimestampNs = 0
            )
        }
    }

    @Test
    fun `test sensor registration works correctly`() = testScope.runTest {

        recordingController.registerSensor("RGB", mockRgbSensor)
        recordingController.registerSensor("Thermal", mockThermalSensor)
        recordingController.registerSensor("GSR", mockGsrSensor)


        val availableSensors = recordingController.getAvailableSensors()
        assertEquals("Should have 3 registered sensors", 3, availableSensors.size)
        assertTrue("RGB should be registered", availableSensors.any { it.sensorId == "RGB" })
        assertTrue(
            "Thermal should be registered",
            availableSensors.any { it.sensorId == "Thermal" })
        assertTrue("GSR should be registered", availableSensors.any { it.sensorId == "GSR" })
    }

    @Test
    fun `test unregister sensor removes sensor correctly`() = testScope.runTest {

        recordingController.registerSensor("RGB", mockRgbSensor)
        recordingController.registerSensor("Thermal", mockThermalSensor)


        recordingController.unregisterSensor("Thermal")


        val availableSensors = recordingController.getAvailableSensors()
        assertEquals("Should have 1 sensor after unregistering", 1, availableSensors.size)
        assertEquals("Remaining sensor should be RGB", "RGB", availableSensors[0].sensorId)
    }

    @Test
    fun `test partial session start succeeds with some sensors failing`() = testScope.runTest {


        coEvery { mockRgbSensor.startRecording(any()) } returns true
        coEvery { mockThermalSensor.startRecording(any()) } returns false
        coEvery { mockGsrSensor.startRecording(any()) } returns true


        recordingController.registerSensor("RGB", mockRgbSensor)
        recordingController.registerSensor("Thermal", mockThermalSensor)
        recordingController.registerSensor("GSR", mockGsrSensor)


        val sessionDir = createTempDirectory()


        val result = recordingController.startSession(sessionDir.absolutePath)

        assertTrue("Session should start successfully with partial sensors", result)
        assertTrue("Recording should be active", recordingController.isRecording)


        coVerify { mockRgbSensor.startRecording(any()) }
        coVerify { mockThermalSensor.startRecording(any()) }
        coVerify { mockGsrSensor.startRecording(any()) }


        sessionDir.deleteRecursively()
    }

    @Test
    fun `test session fails if all sensors fail to start`() = testScope.runTest {

        coEvery { mockRgbSensor.startRecording(any()) } returns false
        coEvery { mockThermalSensor.startRecording(any()) } returns false
        coEvery { mockGsrSensor.startRecording(any()) } returns false

        recordingController.registerSensor("RGB", mockRgbSensor)
        recordingController.registerSensor("Thermal", mockThermalSensor)
        recordingController.registerSensor("GSR", mockGsrSensor)

        val sessionDir = createTempDirectory()


        val result = recordingController.startSession(sessionDir.absolutePath)

        assertFalse("Session should fail when all sensors fail", result)
        assertFalse("Recording should not be active", recordingController.isRecording)

        sessionDir.deleteRecursively()
    }

    @Test
    fun `test session succeeds with single working sensor`() = testScope.runTest {

        coEvery { mockRgbSensor.startRecording(any()) } returns true

        recordingController.registerSensor("RGB", mockRgbSensor)

        val sessionDir = createTempDirectory()


        val result = recordingController.startSession(sessionDir.absolutePath)

        assertTrue("Session should start with single sensor", result)
        assertTrue("Recording should be active", recordingController.isRecording)

        sessionDir.deleteRecursively()
    }

    @Test
    fun `test session metadata files are created`() = testScope.runTest {

        coEvery { mockRgbSensor.startRecording(any()) } returns true

        recordingController.registerSensor("RGB", mockRgbSensor)

        val sessionDir = createTempDirectory()


        recordingController.startSession(sessionDir.absolutePath)


        val metadataFile = File(sessionDir, "session_metadata.json")
        assertTrue("Session metadata file should be created", metadataFile.exists())


        val metadataContent = metadataFile.readText()
        assertTrue("Metadata should contain session_id", metadataContent.contains("session_id"))
        assertTrue(
            "Metadata should contain start timestamps",
            metadataContent.contains("start_timestamp")
        )
        assertTrue(
            "Metadata should contain available sensors",
            metadataContent.contains("available_sensors")
        )

        sessionDir.deleteRecursively()
    }

    @Test
    fun `test backward compatibility with legacy methods`() = testScope.runTest {

        coEvery { mockRgbSensor.startRecording(any()) } returns true
        coEvery { mockRgbSensor.stopRecording() } returns true

        recordingController.registerSensor("RGB", mockRgbSensor)

        val sessionDir = createTempDirectory()


        val startResult = recordingController.startRecording(sessionDir.absolutePath)
        assertTrue("Legacy startRecording should work", startResult)

        val stopResult = recordingController.stopRecording()
        assertTrue("Legacy stopRecording should work", stopResult)

        sessionDir.deleteRecursively()
    }

    @Test
    fun `test sensor subdirectory creation`() = testScope.runTest {

        coEvery { mockRgbSensor.startRecording(any()) } returns true
        coEvery { mockGsrSensor.startRecording(any()) } returns true

        recordingController.registerSensor("RGB", mockRgbSensor)
        recordingController.registerSensor("GSR", mockGsrSensor)

        val sessionDir = createTempDirectory()

        recordingController.startSession(sessionDir.absolutePath)


        assertTrue("RGB subdirectory should exist", File(sessionDir, "rgb").exists())
        assertTrue("GSR subdirectory should exist", File(sessionDir, "gsr").exists())

        sessionDir.deleteRecursively()
    }

    @Test
    fun `test clean session restart capability`() = testScope.runTest {

        coEvery { mockRgbSensor.startRecording(any()) } returns true
        coEvery { mockRgbSensor.stopRecording() } returns true

        recordingController.registerSensor("RGB", mockRgbSensor)

        val sessionDir1 = createTempDirectory()
        val sessionDir2 = createTempDirectory()


        assertTrue(
            "First session should start",
            recordingController.startSession(sessionDir1.absolutePath)
        )
        assertTrue("First session should stop", recordingController.stopSession())


        assertTrue(
            "Second session should start after first completed",
            recordingController.startSession(sessionDir2.absolutePath)
        )
        assertTrue("Second session should stop", recordingController.stopSession())

        sessionDir1.deleteRecursively()
        sessionDir2.deleteRecursively()
    }

    private fun createTempDirectory(): File {
        val tempDir = File.createTempFile("test_session", "")
        tempDir.delete()
        tempDir.mkdirs()
        return tempDir
    }
}
