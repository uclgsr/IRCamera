package mpdc4gsr.gsr.network

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mpdc4gsr.gsr.model.TimelineEstimate
import mpdc4gsr.gsr.session.SessionController
import mpdc4gsr.gsr.session.TimelineClock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import kotlin.math.abs

/**
 * Maintains continuous time synchronisation with the PC orchestrator. Combines UDP-based ping
 * exchange (NTP-style) with periodic HTTPS calibration payloads. The resulting estimate is pushed
 * into [TimelineClock] via [SessionController].
 */
class TimeSyncClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val endpoint: String,
    private val dispatcher: CoroutineDispatcher,
    private val sessionController: SessionController,
    private val clock: TimelineClock,
) : AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var udpJob: Job? = null
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        udpJob = scope.launch { runUdpLoop() }
        scope.launch { pollCalibration() }
    }

    override fun close() {
        scope.cancel()
        started = false
    }

    private suspend fun runUdpLoop() = withContext(dispatcher) {
        val address = InetSocketAddress(parseHost(endpoint), parsePort(endpoint))
        DatagramSocket().use { socket ->
            socket.soTimeout = UDP_TIMEOUT_MILLIS
            val buffer = ByteArray(32)
            while (isActive) {
                try {
                    val t0 = System.nanoTime()
                    val packet = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
                    packet.putLong(t0)
                    val outgoing = DatagramPacket(packet.array(), packet.position(), address)
                    socket.send(outgoing)

                    val incomingPacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(incomingPacket)
                    val t3 = System.nanoTime()

                    val response = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN)
                    val t1 = response.long
                    val t2 = response.long
                    val offset = ((t1 - t0) + (t2 - t3)) / 2.0 / 1_000_000.0
                    val roundTrip = (t3 - t0 - (t2 - t1)) / 1_000_000.0
                    val drift = ((t2 - t1) - (t3 - t0)) / (t3 - t0).toDouble() * 1_000_000.0

                    val estimate =
                        TimelineEstimate(
                            referenceEpochMillis = System.currentTimeMillis(),
                            offsetMillis = offset,
                            roundTripMillis = roundTrip,
                            driftPpm = drift,
                            accuracyMillis = abs(offset) + roundTrip / 2.0,
                            lastUpdated = Instant.now(),
                        )
                    sessionController.updateTimelineEstimate(estimate)
                } catch (ex: Exception) {
                    Log.w(TAG, "UDP sync failed", ex)
                    delay(UDP_RETRY_DELAY_MILLIS)
                }
                delay(UDP_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun pollCalibration() {
        while (scope.isActive) {
            try {
                val request =
                    Request.Builder()
                        .url("$endpoint/time/calibration")
                        .build()
                val response = okHttpClient.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) {
                        Log.w(TAG, "Calibration endpoint failure: ${it.code}")
                    } else {
                        val body = it.body?.string()
                        if (body != null) {
                            val calibration = json.decodeFromString(TimeCalibration.serializer(), body)
                            val estimate =
                                TimelineEstimate(
                                    referenceEpochMillis = calibration.referenceEpochMillis,
                                    offsetMillis = calibration.offsetMillis,
                                    roundTripMillis = calibration.roundTripMillis,
                                    driftPpm = calibration.driftPpm,
                                    accuracyMillis = calibration.accuracyMillis,
                                    lastUpdated = Instant.now(),
                                )
                            sessionController.updateTimelineEstimate(estimate)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to poll calibration", ex)
            }
            delay(CALIBRATION_INTERVAL_MILLIS)
        }
    }

    private fun parseHost(endpoint: String): String {
        return endpoint.substringAfter("://").substringBefore(":").substringBefore("/")
    }

    private fun parsePort(endpoint: String): Int {
        val default = 40125
        val afterHost = endpoint.substringAfter("://").substringAfter(":")
        val portString = afterHost.substringBefore("/")
        return portString.toIntOrNull() ?: default
    }

    companion object {
        private const val TAG = "GsrTimeSyncClient"
        private const val UDP_TIMEOUT_MILLIS = 500
        private const val UDP_INTERVAL_MILLIS = 750L
        private const val UDP_RETRY_DELAY_MILLIS = 1000L
        private const val CALIBRATION_INTERVAL_MILLIS = 10_000L
    }
}

@Serializable
data class TimeCalibration(
    val referenceEpochMillis: Long,
    val offsetMillis: Double,
    val roundTripMillis: Double,
    val driftPpm: Double,
    val accuracyMillis: Double,
)
