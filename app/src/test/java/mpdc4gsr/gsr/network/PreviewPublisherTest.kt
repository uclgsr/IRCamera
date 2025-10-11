package mpdc4gsr.gsr.network

import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mpdc4gsr.gsr.model.TelemetryState
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewPublisherTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetryState = MutableStateFlow<Map<String, TelemetryState>>(emptyMap())
    private val commandClient: CommandClient = mock()
    private val publisher = PreviewPublisher(commandClient, telemetryState, dispatcher)

    @After
    fun tearDown() {
        publisher.close()
    }

    @Test
    fun `start periodically publishes telemetry payload`() = runTest(dispatcher) {
        telemetryState.value =
            mapOf(
                "device-1" to
                    TelemetryState(
                        gsrMicrosiemens = 12.5f,
                        skinTemperatureCelsius = 32.4f,
                        thermalSpotCelsius = 37.2f,
                        audioLevelDb = -10f,
                        frameRate = 24.0,
                        droppedFrames = 1,
                        batteryPercent = 88,
                        rssi = -54,
                        ispActive = true,
                    ),
            )

        publisher.start()
        advanceTimeBy(500L)
        advanceUntilIdle()

        val captor = argumentCaptor<CommandEnvelope>()
        verify(commandClient).sendCommand(captor.capture())

        val envelope = captor.firstValue
        assertEquals("telemetry_update", envelope.type)
        val devicePayload = envelope.payload["device-1"]?.jsonObject ?: error("Missing device payload")
        assertEquals(12.5f, devicePayload.requireFloat("gsr_microsiemens"))
        assertEquals(1, devicePayload.requireInt("dropped_frames"))
        assertEquals(true, devicePayload.requireBoolean("isp_active"))
    }

    private fun JsonObject.requireFloat(key: String): Float =
        this[key]?.jsonPrimitive?.content?.toFloat() ?: error("Missing float $key")

    private fun JsonObject.requireInt(key: String): Int =
        this[key]?.jsonPrimitive?.content?.toIntOrNull() ?: error("Missing int $key")

    private fun JsonObject.requireBoolean(key: String): Boolean =
        this[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: error("Missing boolean $key")
}
