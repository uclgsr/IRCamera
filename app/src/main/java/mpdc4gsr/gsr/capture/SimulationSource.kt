package mpdc4gsr.gsr.capture

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import mpdc4gsr.gsr.model.GsrSample

/**
 * Generates deterministic pseudo-random data for simulation mode (FR1). Emits samples at 128 Hz so
 * the rest of the pipeline can be exercised without hardware.
 */
class SimulationSource(
    private val dispatcher: CoroutineDispatcher,
) : AutoCloseable {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var job: Job? = null
    private val random = Random(0x53212025)

    fun start(sessionId: String, emit: suspend (GsrSample) -> Unit) {
        stop()
        job =
            scope.launch {
                var sequence = 0L
                var phase = 0.0
                val startTime = System.currentTimeMillis()
                while (isActive) {
                    val timestamp = System.currentTimeMillis()
                    val elapsed = (timestamp - startTime) / 1000.0
                    val tonic = 10f + 2f * sin(phase).toFloat()
                    val phasic = (random.nextDouble() - 0.5).toFloat()
                    val microsiemens = tonic + phasic
                    val sample =
                        GsrSample(
                            deviceId = "simulated-$sessionId",
                            timestampMillis = timestamp,
                            gsrMicrosiemens = microsiemens.toDouble(),
                            resistanceOhms = if (microsiemens > 0f) 1_000_000.0 / microsiemens.toDouble() else null,
                            skinTemperatureCelsius = 32.0 + sin(elapsed),
                            sequenceNumber = sequence++,
                        )
                    emit(sample)
                    phase += 2 * PI / 128.0
                    delay(1000L / 128)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    override fun close() {
        stop()
        scope.cancel()
    }
}
