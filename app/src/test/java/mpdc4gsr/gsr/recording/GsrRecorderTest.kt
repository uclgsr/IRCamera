package mpdc4gsr.gsr.recording

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mpdc4gsr.gsr.createRecordingContext
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.GsrSample
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.session.TimelineClock
import mpdc4gsr.gsr.capture.SimulationSource
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GsrRecorderTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private lateinit var tempDir: java.io.File

    @Before
    fun setUp() {
        tempDir = createTempDirectory("gsr-recorder").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `start collects samples, writes csv, and updates state`() = scope.runTest {
        val shimmerSamples = MutableSharedFlow<GsrSample>(extraBufferCapacity = 8)
        val shimmerController =
            mockk<ShimmerDeviceController>(relaxed = true) {
                every { samples } returns shimmerSamples
            }
        val simulationSource = mockk<SimulationSource>(relaxed = true)
        val timelineClock = mockk<TimelineClock>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        val recorder =
            GsrRecorder(
                context = context,
                dispatcher = dispatcher,
                shimmerController = shimmerController,
                simulationSource = simulationSource,
                timelineClock = timelineClock,
            )

        val recordingContext =
            createRecordingContext(
                sessionDirectory = tempDir,
                sessionId = "session-123",
                timelineClock = timelineClock,
                enabledModalities = setOf(RecorderKind.GSR),
                simulationEnabled = true,
            )

        recorder.prepare(recordingContext)
        assertEquals(RecorderState.PREPARING, recorder.state.value)

        recorder.start(recordingContext)
        assertEquals(RecorderState.RECORDING, recorder.state.value)
        verify(exactly = 1) { shimmerController.enableSimulation("session-123") }

        val collected = mutableListOf<GsrSample>()
        val job =
            launch {
                recorder.latestSample.collect {
                    collected += it
                    this@launch.cancel()
                }
            }

        val sample =
            GsrSample(
                deviceId = "AA:BB:CC",
                timestampMillis = 1_000L,
                gsrMicrosiemens = 12.3456,
                resistanceOhms = 456.0,
                skinTemperatureCelsius = 33.2,
                sequenceNumber = 10,
            )

        shimmerSamples.emit(sample)
        advanceUntilIdle()

        job.join()
        assertEquals(listOf(sample), collected)

        val csvFile = tempDir.resolve("gsr/AA:BB:CC_session-123.csv")
        assertTrue(csvFile.exists(), "CSV file expected to be created for device")
        val lines = csvFile.readLines()
        assertEquals("timestamp_ms,device_id,microsiemens,resistance_ohms,skin_temp_c,sequence", lines.first())
        assertTrue(lines.last().contains("12.345600"))

        recorder.stop()
        advanceUntilIdle()

        verify { simulationSource.stop() }
        assertEquals(RecorderState.IDLE, recorder.state.value)

        recorder.close()
    }
}
