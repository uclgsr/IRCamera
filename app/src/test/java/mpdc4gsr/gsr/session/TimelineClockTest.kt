package mpdc4gsr.gsr.session

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mpdc4gsr.gsr.model.TimelineEstimate
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineClockTest {

    private val dispatcher = StandardTestDispatcher()
    private var nowNanos = 0L
    private val clock = TimelineClock(dispatcher) { nowNanos }

    @Test
    fun `nowInstant applies offset and drift corrections`() = runTest(dispatcher) {
        val reference = Instant.parse("2024-01-01T00:00:00Z")
        val estimate =
            TimelineEstimate(
                referenceEpochMillis = reference.toEpochMilli(),
                offsetMillis = 100.0,
                roundTripMillis = 5.0,
                driftPpm = 200.0,
                accuracyMillis = 0.5,
                lastUpdated = reference,
            )

        clock.updateEstimate(estimate)
        advanceUntilIdle()

        nowNanos = 2_000_000_000L // 2 seconds after reference
        val instant = clock.nowInstant()

        val expectedMillis =
            reference.toEpochMilli() + 2_000 + 100 +
                (estimate.driftPpm / 1_000_000.0 * 2_000)

        assertEquals(expectedMillis.toLong(), instant.toEpochMilli())
    }

    @Test
    fun `updateEstimate smooths incoming values based on accuracy`() = runTest(dispatcher) {
        val initial =
            TimelineEstimate(
                referenceEpochMillis = 0L,
                offsetMillis = 120.0,
                roundTripMillis = 12.0,
                driftPpm = 40.0,
                accuracyMillis = 5.0,
                lastUpdated = Instant.EPOCH,
            )
        clock.updateEstimate(initial)
        advanceUntilIdle()

        val incoming =
            TimelineEstimate(
                referenceEpochMillis = 10L,
                offsetMillis = 20.0,
                roundTripMillis = 3.0,
                driftPpm = 8.0,
                accuracyMillis = 0.8,
                lastUpdated = Instant.EPOCH.plusMillis(10),
            )
        clock.updateEstimate(incoming)
        advanceUntilIdle()

        val smoothed = clock.timeline.value
        assertTrue(smoothed.offsetMillis in 35.0..45.0)
        assertTrue(smoothed.driftPpm in 18.0..22.0)
        assertEquals(3.0, smoothed.roundTripMillis)
        assertEquals(0.8, smoothed.accuracyMillis)
    }
}

