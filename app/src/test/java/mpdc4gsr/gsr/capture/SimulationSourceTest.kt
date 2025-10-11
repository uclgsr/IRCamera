package mpdc4gsr.gsr.capture

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mpdc4gsr.gsr.model.GsrSample
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulationSourceTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private val emittedSamples = mutableListOf<GsrSample>()
    private val source = SimulationSource(dispatcher)

    @After
    fun tearDown() {
        source.close()
        emittedSamples.clear()
    }

    @Test
    fun `start emits deterministic samples until stopped`() = scope.runTest {
        source.start("session-id") { sample ->
            emittedSamples += sample
            if (emittedSamples.size >= 4) {
                source.stop()
            }
        }

        advanceTimeBy(2000L)
        advanceUntilIdle()

        assertTrue(emittedSamples.isNotEmpty(), "Expected simulation source to emit samples")
        assertEquals("simulated-session-id", emittedSamples.first().deviceId)
        emittedSamples.zipWithNext { current, next ->
            assertEquals(
                current.sequenceNumber + 1,
                next.sequenceNumber,
                "Sequence numbers should increase monotonically",
            )
        }
    }
}
