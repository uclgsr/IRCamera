package mpdc4gsr.integration

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator
import mpdc4gsr.gsr.capture.CaptureCoordinator
import mpdc4gsr.gsr.capture.RecorderFactory
import mpdc4gsr.gsr.capture.SimulationSource
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.GsrSample
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.SessionStateStore
import mpdc4gsr.gsr.recording.RecordingContext
import mpdc4gsr.gsr.session.SessionController
import mpdc4gsr.gsr.session.TimelineClock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the capture coordinator against a simulated Shimmer data stream end-to-end. Files are
 * written to a temporary directory so the test can assert that the recorder produces CSV output.
 */
@RunWith(AndroidJUnit4::class)
class SimulatedCaptureInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context by lazy { instrumentation.targetContext }
    private val dispatcher = Dispatchers.Default

    private lateinit var tempDir: File
    private lateinit var shimmerSamples: MutableSharedFlow<GsrSample>
    private lateinit var shimmerController: ShimmerDeviceController
    private lateinit var simulationSource: SimulationSource
    private lateinit var timelineClock: TimelineClock
    private lateinit var sessionStore: SessionStateStore
    private lateinit var sessionController: SessionController
    private lateinit var captureCoordinator: CaptureCoordinator

    private class ResumedLifecycleOwner : LifecycleOwner {
        private val registry =
            LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }

        override val lifecycle: Lifecycle
            get() = registry
    }

    @Before
    fun setUp() {
        tempDir = File(context.cacheDir, "simulated-integration").apply {
            deleteRecursively()
            mkdirs()
        }

        shimmerSamples = MutableSharedFlow(extraBufferCapacity = 32)

        shimmerController =
            mockk(relaxed = true) {
                every { samples } returns shimmerSamples
            }

        simulationSource = SimulationSource(dispatcher)
        timelineClock = TimelineClock(dispatcher)
        sessionStore = SessionStateStore()
        sessionController = SessionController(context, sessionStore, timelineClock, dispatcher)

        val thermalCoordinator = mockk<ThermalCaptureCoordinator>(relaxed = true)

        val recorderFactory =
            RecorderFactory(
                appContext = context,
                lifecycleOwnerProvider = { ResumedLifecycleOwner() },
                ioDispatcher = dispatcher,
                shimmerController = shimmerController,
                simulationSource = simulationSource,
                timelineClock = timelineClock,
                thermalCoordinator = thermalCoordinator,
            )

        captureCoordinator =
            CaptureCoordinator(
                recorderFactory = recorderFactory,
                sessionController = sessionController,
                dispatcher = dispatcher,
            )
    }

    @After
    fun tearDown() {
        captureCoordinator.close()
        sessionController.close()
        timelineClock.close()
        simulationSource.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun simulatedGsrRecordingProducesCsvOutput() =
        runBlocking {
            val recordingContext =
                RecordingContext(
                    appContext = context,
                    sessionId = "instrumented-sim",
                    sessionDirectory = tempDir,
                    clock = timelineClock,
                    enabledModalities = setOf(RecorderKind.GSR),
                    simulationEnabled = true,
                )

            captureCoordinator.prepare(recordingContext)
            captureCoordinator.startRecording(recordingContext)

            val sample =
                GsrSample(
                    deviceId = "shimmer-sim",
                    timestampMillis = System.currentTimeMillis(),
                    gsrMicrosiemens = 12.34,
                    resistanceOhms = 81_000.0,
                    skinTemperatureCelsius = 32.5,
                    sequenceNumber = 1L,
                )

            shimmerSamples.emit(sample)

            val expectedFile = File(tempDir, "gsr/${sample.deviceId}_${recordingContext.sessionId}.csv")

            withTimeout(5_000) {
                while (!expectedFile.exists()) {
                    delay(100)
                }
            }

            captureCoordinator.stopRecording()

            assertTrue("Expected CSV output for simulated session", expectedFile.exists())
            val contents = expectedFile.readText()
            assertTrue("CSV should contain simulated device samples", "shimmer-sim" in contents)

            verify(exactly = 1) { shimmerController.enableSimulation(recordingContext.sessionId) }
        }
}
