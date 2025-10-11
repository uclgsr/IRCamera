package mpdc4gsr.gsr.session

import android.content.Context
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.DeviceType
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.model.SessionStateStore
import mpdc4gsr.gsr.model.TimelineEstimate
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SessionControllerTest {

    private val dispatcher = StandardTestDispatcher()
    private val context: Context = mock()
    private val stateStore = SessionStateStore()
    private var nowNanos = 0L
    private val timelineClock = TimelineClock(dispatcher) { nowNanos }
    private val controller = SessionController(context, stateStore, timelineClock, dispatcher)
    private val scope = TestScope(dispatcher)

    @After
    fun tearDown() {
        controller.close()
    }

    @Test
    fun `begin and end session manage snapshot lifecycle`() = scope.runTest {
        controller.beginSession("session-1", "Test Session", 120L)
        advanceUntilIdle()

        val snapshot = stateStore.sessionSnapshot.value
        assertNotNull(snapshot)
        assertEquals("session-1", snapshot.sessionId)
        assertEquals("Test Session", snapshot.label)
        assertEquals(120L, snapshot.plannedDurationSeconds)
        assertFalse(snapshot.isRecording)
        assertEquals(RecorderKind.values().size, snapshot.recorderStates.size)

        controller.endSession()
        advanceUntilIdle()

        assertNull(stateStore.sessionSnapshot.value)
        assertTrue(stateStore.deviceTelemetry.value.isEmpty())
    }

    @Test
    fun `commands update recorder state and faults`() = scope.runTest {
        controller.beginSession("session-2", "Commands", null)
        advanceUntilIdle()

        controller.applyCommand(
            SessionCommand.StartRecording(
                sessionId = "session-2",
                modalities = setOf(RecorderKind.GSR),
                scheduledTimeMillis = 10L,
            ),
        )
        controller.applyCommand(
            SessionCommand.UpdateRecorderState(RecorderKind.GSR, RecorderState.RECORDING),
        )
        controller.applyCommand(
            SessionCommand.StimulusMarker("marker-1", scheduledTimeMillis = 20L),
        )
        controller.applyCommand(
            SessionCommand.SetSimulationMode(true),
        )
        advanceUntilIdle()

        val snapshot = requireNotNull(stateStore.sessionSnapshot.value)
        assertTrue(snapshot.isRecording)
        assertEquals(RecorderState.RECORDING, snapshot.recorderStates[RecorderKind.GSR])
        assertTrue(snapshot.faults.any { it.message.contains("marker-1") })
        assertTrue(snapshot.faults.any { it.message.contains("Simulation mode enabled") })

        controller.applyCommand(
            SessionCommand.StopRecording(sessionId = "session-2", scheduledTimeMillis = 30L),
        )
        advanceUntilIdle()

        val updated = requireNotNull(stateStore.sessionSnapshot.value)
        assertFalse(updated.isRecording)
    }

    @Test
    fun `device snapshot and updates replace descriptors`() = scope.runTest {
        controller.beginSession("session-3", "Devices", null)
        advanceUntilIdle()

        val device =
            DeviceDescriptor(
                id = "dev-1",
                displayName = "Device 1",
                type = DeviceType.SHIMMER_GSR_SENSOR,
                connectionState = ConnectionState.DISCOVERED,
                batteryPercent = 85,
                supportsThermal = false,
                supportsRgb = false,
                supportsAudio = true,
                shimmerMacAddress = "00:11:22",
                lastHeartbeat = Instant.now(),
                timeOffsetMillis = 1.2,
            )
        controller.applyCommand(SessionCommand.ApplyDeviceSnapshot(listOf(device)))
        advanceUntilIdle()

        val snapshot = requireNotNull(stateStore.sessionSnapshot.value)
        assertEquals(1, snapshot.connectedDevices.size)

        controller.updateDeviceState("dev-1", ConnectionState.CONNECTED)
        val updated = requireNotNull(stateStore.sessionSnapshot.value)
        assertEquals(ConnectionState.CONNECTED, updated.connectedDevices.first().connectionState)
    }

    @Test
    fun `timeline estimate is propagated to snapshot`() = scope.runTest {
        controller.beginSession("session-4", "Timeline", null)
        advanceUntilIdle()

        val newEstimate =
            TimelineEstimate(
                referenceEpochMillis = 123L,
                offsetMillis = 12.0,
                roundTripMillis = 1.5,
                driftPpm = 4.0,
                accuracyMillis = 0.4,
                lastUpdated = Instant.now(),
            )
        controller.updateTimelineEstimate(newEstimate)
        advanceUntilIdle()

        val snapshot = requireNotNull(stateStore.sessionSnapshot.value)
        assertEquals(newEstimate.referenceEpochMillis, snapshot.globalTimeline.referenceEpochMillis)
        assertEquals(newEstimate.offsetMillis, snapshot.globalTimeline.offsetMillis)
    }
}

