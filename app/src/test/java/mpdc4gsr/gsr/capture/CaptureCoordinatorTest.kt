package mpdc4gsr.gsr.capture

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mpdc4gsr.gsr.createRecordingContext
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.recording.Recorder
import mpdc4gsr.gsr.recording.RecordingContext
import mpdc4gsr.gsr.session.SessionCommand
import mpdc4gsr.gsr.session.SessionController
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `prepare creates recorders and updates session state`() = scope.runTest {
        val recorders =
            mapOf(
                RecorderKind.GSR to FakeRecorder(RecorderKind.GSR),
                RecorderKind.AUDIO to FakeRecorder(RecorderKind.AUDIO),
            )
        val recorderFactory = mockk<RecorderFactory>()
        every { recorderFactory.create(any()) } answers { recorders.getValue(firstArg()) }

        val commands = mutableListOf<SessionCommand>()
        val sessionController = mockk<SessionController>()
        coEvery { sessionController.applyCommand(capture(commands)) } returns Unit

        val coordinator = CaptureCoordinator(recorderFactory, sessionController, dispatcher)
        val tempDir = createTempDirectory("capture-coordinator").toFile()
        val context =
            createRecordingContext(
                sessionDirectory = tempDir,
                enabledModalities = recorders.keys,
            )

        coordinator.prepare(context)
        advanceUntilIdle()

        recorders.values.forEach { recorder ->
            assertEquals(listOf(context), recorder.preparedContexts)
        }

        val stateCommands = commands.filterIsInstance<SessionCommand.UpdateRecorderState>()
        assertEquals(recorders.size * 2, stateCommands.size)
        assertEquals(
            setOf(RecorderState.PREPARING, RecorderState.IDLE),
            stateCommands.map { it.state }.toSet(),
        )

        tempDir.deleteRecursively()
        coordinator.close()
    }

    @Test
    fun `start and stop propagate to recorders and session controller`() = scope.runTest {
        val recorders =
            mapOf(
                RecorderKind.GSR to FakeRecorder(RecorderKind.GSR),
                RecorderKind.AUDIO to FakeRecorder(RecorderKind.AUDIO),
            )
        val recorderFactory = mockk<RecorderFactory>()
        every { recorderFactory.create(any()) } answers { recorders.getValue(firstArg()) }

        val commands = mutableListOf<SessionCommand>()
        val sessionController = mockk<SessionController>()
        coEvery { sessionController.applyCommand(capture(commands)) } returns Unit

        val coordinator = CaptureCoordinator(recorderFactory, sessionController, dispatcher)
        val tempDir = createTempDirectory("capture-coordinator-start-stop").toFile()
        val context =
            createRecordingContext(
                sessionDirectory = tempDir,
                enabledModalities = recorders.keys,
            )

        coordinator.prepare(context)
        coordinator.startRecording(context)
        advanceUntilIdle()

        recorders.values.forEach { recorder ->
            assertEquals(1, recorder.startCount)
        }
        val startStates =
            commands.filterIsInstance<SessionCommand.UpdateRecorderState>()
                .map { it.state }
        assertTrue(startStates.contains(RecorderState.RECORDING))

        coordinator.stopRecording()
        advanceUntilIdle()

        recorders.values.forEach { recorder ->
            assertEquals(1, recorder.stopCount)
        }
        val stopStates =
            commands.filterIsInstance<SessionCommand.UpdateRecorderState>()
                .map { it.state }
        assertTrue(stopStates.contains(RecorderState.STOPPING))
        assertTrue(stopStates.contains(RecorderState.IDLE))

        tempDir.deleteRecursively()
        coordinator.close()
    }

    @Test
    fun `close cancels scope and closes recorders`() = scope.runTest {
        val recorder = FakeRecorder(RecorderKind.GSR)
        val recorderFactory = mockk<RecorderFactory>()
        every { recorderFactory.create(RecorderKind.GSR) } returns recorder
        val sessionController = mockk<SessionController>()
        coEvery { sessionController.applyCommand(any()) } returns Unit

        val coordinator = CaptureCoordinator(recorderFactory, sessionController, dispatcher)
        val tempDir = createTempDirectory("capture-coordinator-close").toFile()
        val context =
            createRecordingContext(
                sessionDirectory = tempDir,
                enabledModalities = setOf(RecorderKind.GSR),
            )

        coordinator.prepare(context)
        coordinator.close()

        assertTrue(recorder.closed.get())
        tempDir.deleteRecursively()
    }

    private class FakeRecorder(
        override val kind: RecorderKind,
    ) : Recorder {
        private val _state = MutableStateFlow(RecorderState.IDLE)
        override val state = _state

        val preparedContexts = mutableListOf<RecordingContext>()
        var startCount = 0
        var stopCount = 0
        val closed = AtomicBoolean(false)

        override suspend fun prepare(context: RecordingContext) {
            preparedContexts += context
            _state.value = RecorderState.PREPARING
        }

        override suspend fun start(context: RecordingContext) {
            startCount++
            _state.value = RecorderState.RECORDING
        }

        override suspend fun stop() {
            stopCount++
            _state.value = RecorderState.IDLE
        }

        override fun close() {
            closed.set(true)
        }
    }
}
