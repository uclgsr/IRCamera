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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import mpdc4gsr.gsr.model.TimelineEstimate
import mpdc4gsr.gsr.session.SessionController
import mpdc4gsr.gsr.session.TimelineClock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.HttpURLConnection.HTTP_NO_CONTENT
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
    @Volatile
    private var started = false
    @Volatile
    private var lastPublishedAtMillis = 0L
    @Volatile
    private var lastPublishedAccuracy = Double.MAX_VALUE

    fun start() {
        if (started) return
        started = true
        udpJob = scope.launch { runUdpLoop() }
        scope.launch { pollCalibration() }
    }

    private fun maybePublishCalibration(estimate: TimelineEstimate) {
        if (estimate.accuracyMillis > PUBLISH_MAX_ACCURACY_MILLIS) return
        val now = System.currentTimeMillis()
        val recentlyPublished = now - lastPublishedAtMillis < PUBLISH_INTERVAL_MILLIS
        val accuracyImproved = estimate.accuracyMillis < lastPublishedAccuracy - PUBLISH_ACCURACY_EPSILON
        if (recentlyPublished && !accuracyImproved) return

        lastPublishedAtMillis = now
        lastPublishedAccuracy = estimate.accuracyMillis
        scope.launch {
            publishCalibration(estimate)
        }
    }

    private suspend fun publishCalibration(estimate: TimelineEstimate) =
        withContext(dispatcher) {
            try {
                val calibration =
                    TimeCalibration(
                        referenceEpochMillis = estimate.referenceEpochMillis,
                        offsetMillis = estimate.offsetMillis,
                        roundTripMillis = estimate.roundTripMillis,
                        driftPpm = estimate.driftPpm,
                        accuracyMillis = estimate.accuracyMillis,
                    )
                val payload = json.encodeToString(calibration)
                val request =
                    Request.Builder()
                        .url("$endpoint/time/calibration")
                        .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                okHttpClient
                    .newCall(request)
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful && response.code !in 200..299) {
                            Log.d(TAG, "Calibration publish returned ${response.code}")
                        }
                    }
            } catch (ex: Exception) {
                Log.d(TAG, "Failed to publish calibration", ex)
            }
        }

    private fun decodeEstimate(
        buffer: ByteArray,
        length: Int,
        t0: Long,
        t3: Long,
    ): TimelineEstimate? {
        if (length <= 0) return null
        val first = buffer[0].toInt()
        return when {
            length >= 16 && first != JSON_OBJECT_START -> decodeBinaryEstimate(buffer, length, t0, t3)
            first == JSON_OBJECT_START -> decodeJsonEstimate(buffer, length, t0, t3)
            else -> null
        }
    }

    private fun decodeBinaryEstimate(
        buffer: ByteArray,
        length: Int,
        t0: Long,
        t3: Long,
    ): TimelineEstimate? {
        val response = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.BIG_ENDIAN)
        if (response.remaining() < 16) return null
        val t1 = response.long
        val t2 = if (response.remaining() >= 8) response.long else t1
        return buildEstimate(t0, t1, t2, t3)
    }

    private fun decodeJsonEstimate(
        buffer: ByteArray,
        length: Int,
        t0: Long,
        t3: Long,
    ): TimelineEstimate? {
        val payload = String(buffer, 0, length, Charsets.UTF_8)
        return runCatching {
            val element = json.parseToJsonElement(payload).jsonObject
            val receiveNs =
                element["server_monotonic"]?.jsonPrimitive?.doubleOrNull?.let { (it * NANOS_PER_SECOND).toLong() }
                    ?: element["server_receive_unix"]?.jsonPrimitive?.doubleOrNull?.let { (it * NANOS_PER_SECOND).toLong() }
                    ?: element["receive_monotonic_ns"]?.jsonPrimitive?.longOrNull
            val transmitNs =
                element["responded_monotonic"]?.jsonPrimitive?.doubleOrNull?.let { (it * NANOS_PER_SECOND).toLong() }
                    ?: element["responded_unix"]?.jsonPrimitive?.doubleOrNull?.let { (it * NANOS_PER_SECOND).toLong() }
                    ?: element["transmit_monotonic_ns"]?.jsonPrimitive?.longOrNull
                    ?: receiveNs
            if (receiveNs == null) null else buildEstimate(t0, receiveNs, transmitNs ?: receiveNs, t3)
        }.getOrNull()
    }

    private fun buildEstimate(
        t0: Long,
        t1: Long,
        t2: Long,
        t3: Long,
    ): TimelineEstimate {
        val offset = ((t1 - t0) + (t2 - t3)) / 2.0 / 1_000_000.0
        val rawRoundTrip = (t3 - t0) - (t2 - t1)
        val roundTrip = rawRoundTrip.coerceAtLeast(0L) / 1_000_000.0
        val drift =
            if (t3 == t0) {
                0.0
            } else {
                ((t2 - t1) - (t3 - t0)) / (t3 - t0).toDouble() * 1_000_000.0
            }
        val accuracy = abs(offset) + roundTrip / 2.0
        return TimelineEstimate(
            referenceEpochMillis = System.currentTimeMillis(),
            offsetMillis = offset,
            roundTripMillis = roundTrip,
            driftPpm = drift,
            accuracyMillis = accuracy,
            lastUpdated = Instant.now(),
        )
    }

    override fun close() {
        scope.cancel()
        started = false
    }

    private suspend fun runUdpLoop() = withContext(dispatcher) {
        val address = InetSocketAddress(parseHost(endpoint), parsePort(endpoint))
        DatagramSocket().use { socket ->
            socket.soTimeout = UDP_TIMEOUT_MILLIS
            val buffer = ByteArray(64)
            while (isActive) {
                try {
                    val t0 = System.nanoTime()
                    val packet = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                    packet.putLong(t0)
                    val outgoing = DatagramPacket(packet.array(), packet.position(), address)
                    socket.send(outgoing)

                    val incomingPacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(incomingPacket)
                    val t3 = System.nanoTime()

                    val estimate = decodeEstimate(buffer, incomingPacket.length, t0, t3)
                    if (estimate != null) {
                        sessionController.updateTimelineEstimate(estimate)
                        maybePublishCalibration(estimate)
                    }
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
                okHttpClient
                    .newCall(request)
                    .execute()
                    .use { response ->
                        when {
                            response.code == HTTP_NO_CONTENT -> Unit
                            response.isSuccessful -> {
                                val body = response.body?.string()
                                if (!body.isNullOrBlank()) {
                                    val calibration = json.decodeFromString<TimeCalibration>(body)
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

                            else -> Log.d(TAG, "Calibration endpoint returned ${response.code}")
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
        private const val JSON_OBJECT_START = '{'.code
        private const val PUBLISH_INTERVAL_MILLIS = 5_000L
        private const val PUBLISH_MAX_ACCURACY_MILLIS = 15.0
        private const val PUBLISH_ACCURACY_EPSILON = 0.5
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
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
