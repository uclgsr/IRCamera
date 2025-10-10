package mpdc4gsr.gsr.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.gsr.model.TimelineEstimate
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Maintains a high-resolution estimate of the PC orchestrator timeline. The estimate combines
 * network-derived offset/drift information with the Android device's monotonic clock to achieve
 * millisecond-level accuracy (FR3, NFR2).
 */
class TimelineClock(
    private val dispatcher: CoroutineDispatcher,
    private val nowProvider: () -> Long = { System.nanoTime() },
) : AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val referenceNanos = nowProvider()

    private val _timeline = MutableStateFlow(
        TimelineEstimate(
            referenceEpochMillis = System.currentTimeMillis(),
            offsetMillis = 0.0,
            roundTripMillis = Double.MAX_VALUE,
            driftPpm = 0.0,
            accuracyMillis = Double.MAX_VALUE,
            lastUpdated = Instant.now(),
        ),
    )
    val timeline: StateFlow<TimelineEstimate> = _timeline

    fun nowInstant(): Instant {
        val estimate = _timeline.value
        val elapsedNanos = nowProvider() - referenceNanos
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos.toLong())
        val driftCorrection = estimate.driftPpm / 1_000_000.0 * elapsedMillis
        val corrected =
            estimate.referenceEpochMillis +
                elapsedMillis +
                estimate.offsetMillis +
                driftCorrection
        return Instant.ofEpochMilli(corrected.toLong())
    }

    suspend fun updateEstimate(estimate: TimelineEstimate) {
        scope.launch {
            val smoothed = smoothEstimate(_timeline.value, estimate)
            _timeline.value = smoothed
        }.join()
    }

    private fun smoothEstimate(
        current: TimelineEstimate,
        incoming: TimelineEstimate,
    ): TimelineEstimate {
        if (current.accuracyMillis == Double.MAX_VALUE) return incoming
        val alpha = when {
            incoming.accuracyMillis < 1.0 -> 0.75
            incoming.accuracyMillis < 5.0 -> 0.6
            incoming.accuracyMillis < 10.0 -> 0.45
            else -> 0.2
        }
        val offset =
            (1 - alpha) * current.offsetMillis +
                alpha * incoming.offsetMillis
        val drift =
            (1 - alpha) * current.driftPpm +
                alpha * incoming.driftPpm
        val accuracy = minOf(current.accuracyMillis, incoming.accuracyMillis)
        val roundTrip = minOf(current.roundTripMillis, incoming.roundTripMillis)
        return incoming.copy(
            offsetMillis = offset,
            driftPpm = drift,
            accuracyMillis = accuracy,
            roundTripMillis = roundTrip,
            lastUpdated = Instant.now(),
        )
    }

    override fun close() {
        scope.cancel()
    }
}
