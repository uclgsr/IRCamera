package com.topdon.lib.core.sync

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs



class TimeSyncService {
    companion object {
    private const val TAG = "TimeSyncService"
    private const val SYNC_TIMEOUT_MS = 5000L
    private const val MAX_SYNC_ATTEMPTS = 5
    private const val MIN_SAMPLES = 3
    private const val MAX_ACCEPTABLE_DELAY_MS = 100L // Maximum acceptable round-trip delay
    private const val SYNC_INTERVAL_MS = 30000L // Re-sync every 30 seconds
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicSyncJob: Job? = null

    data class SyncResult(
        val isSuccess: Boolean,
        val clockOffsetMs: Long = 0L,
        val roundTripDelayMs: Long = 0L,
        val accuracyMs: Long = 0L,
        val errorMessage: String? = null,
    )

    data class SyncSample(
        val t1: Long, // Client send time
        val t2: Long, // Server receive time
        val t3: Long, // Server response time
        val t4: Long, // Client receive time
        val roundTripDelay: Long,
        val clockOffset: Long,
    )


    interface TimeSyncListener {

        fun onSyncCompleted(result: SyncResult)


        fun onSyncStarted(targetHost: String)


        fun onSyncError(error: String)
    }

    private var listener: TimeSyncListener? = null

    fun setListener(listener: TimeSyncListener?) {
    this.listener = listener
    }


    suspend fun synchronizeTime(
        targetHost: String,
        targetPort: Int = 8080,
    ): SyncResult =
        withContext(Dispatchers.IO) {
            listener?.onSyncStarted(targetHost)
            Log.i(TAG, "Starting time synchronization with $targetHost:$targetPort")

            val samples = mutableListOf<SyncSample>()
            var lastError: String? = null

            // Collect multiple samples for better accuracy
            repeat(MAX_SYNC_ATTEMPTS) { attempt ->
                try {
                    val sample = performSyncRequest(targetHost, targetPort)

                    // Only accept samples with reasonable round-trip delay
                    if (sample.roundTripDelay <= MAX_ACCEPTABLE_DELAY_MS) {
                        samples.add(sample)
                        Log.d(TAG, "Sample ${attempt + 1}: offset=${sample.clockOffset}ms, delay=${sample.roundTripDelay}ms")
                    } else {
                        Log.w(TAG, "Sample ${attempt + 1} rejected: delay too high (${sample.roundTripDelay}ms)")
                    }

                    // Short delay between samples
                    if (attempt < MAX_SYNC_ATTEMPTS - 1) {
                        delay(100)
                    }
                } catch (e: Exception) {
                    lastError = e.message
                    Log.w(TAG, "Sync attempt ${attempt + 1} failed: ${e.message}")
                    delay(500) // Longer delay after failures
                }
            }

            if (samples.size < MIN_SAMPLES) {
                val error = "Insufficient samples for reliable sync (got ${samples.size}, need $MIN_SAMPLES)"
                Log.e(TAG, error)
                listener?.onSyncError(error)
                return@withContext SyncResult(
                    isSuccess = false,
                    errorMessage = lastError ?: error,
                )
            }

            // Calculate final offset using median to reduce outlier impact
            val result = calculateSyncResult(samples)

            Log.i(TAG, "Time sync completed: offset=${result.clockOffsetMs}ms, accuracy=±${result.accuracyMs}ms")
            listener?.onSyncCompleted(result)

            result
        }


    fun startPeriodicSync(
        targetHost: String,
        targetPort: Int = 8080,
        intervalMs: Long = SYNC_INTERVAL_MS,
    ) {
        stopPeriodicSync()

        periodicSyncJob =
            syncScope.launch {
                while (isActive) {
                    try {
                        synchronizeTime(targetHost, targetPort)
                        delay(intervalMs)
                    } catch (e: Exception) {
                        Log.e(TAG, "Periodic sync error", e)
                        listener?.onSyncError("Periodic sync failed: ${e.message}")
                        delay(intervalMs) // Continue trying
                    }
                }
            }

        Log.i(TAG, "Started periodic time sync with $targetHost:$targetPort (interval: ${intervalMs}ms)")
    }


    fun stopPeriodicSync() {
    periodicSyncJob?.cancel()
    periodicSyncJob = null
    Log.i(TAG, "Stopped periodic time sync")
    }


    private suspend fun performSyncRequest(
        host: String,
        port: Int,
    ): SyncSample {
        return withContext(Dispatchers.IO) {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), SYNC_TIMEOUT_MS.toInt())
            socket.soTimeout = SYNC_TIMEOUT_MS.toInt()

    val output = DataOutputStream(socket.getOutputStream())
    val input = DataInputStream(socket.getInputStream())

            try {
                // T1: Client send time (high precision)
                val t1 = getHighPrecisionTime()

                // Send sync request
                val request =
                    JSONObject().apply {
                        put("message_type", "time_sync_request")
                        put("client_time", t1)
                        put("version", "1.0")
                    }

                val requestData = request.toString().toByteArray(Charsets.UTF_8)
                output.writeInt(requestData.size)
                output.write(requestData)
                output.flush()

                // Read response
                val responseLength = input.readInt()
                val responseData = ByteArray(responseLength)
                input.readFully(responseData)

                // T4: Client receive time (high precision)
                val t4 = getHighPrecisionTime()

                val response = JSONObject(String(responseData, Charsets.UTF_8))

                if (response.optString("message_type") != "time_sync_response") {
                    throw IllegalStateException("Invalid sync response")
                }

                // Extract server timestamps
                val t2 = response.getLong("server_receive_time") // Server receive time
                val t3 = response.getLong("server_send_time") // Server send time

                // Calculate round-trip delay and clock offset
                // Round-trip delay: (T4 - T1) - (T3 - T2)
                val roundTripDelay = (t4 - t1) - (t3 - t2)

                // Clock offset: ((T2 - T1) + (T3 - T4)) / 2
                val clockOffset = ((t2 - t1) + (t3 - t4)) / 2

                SyncSample(t1, t2, t3, t4, roundTripDelay, clockOffset)
            } finally {
                socket.close()
            }
        }
    }


    private fun calculateSyncResult(samples: List<SyncSample>): SyncResult {
        // Sort samples by round-trip delay (prefer lower delay samples)
        val sortedSamples = samples.sortedBy { it.roundTripDelay }

        // Use median offset for robustness
        val offsets = sortedSamples.map { it.clockOffset }.sorted()
        val medianOffset = offsets[offsets.size / 2]

        // Calculate accuracy as standard deviation
        val meanOffset = offsets.average()
        val variance = offsets.map { (it - meanOffset) * (it - meanOffset) }.average()
        val accuracy = kotlin.math.sqrt(variance).toLong()

        // Use minimum round-trip delay as quality indicator
        val minDelay = sortedSamples.first().roundTripDelay

        return SyncResult(
            isSuccess = true,
            clockOffsetMs = medianOffset,
            roundTripDelayMs = minDelay,
            accuracyMs = accuracy,
        )
    }


    private fun getHighPrecisionTime(): Long {
    // Use nanoTime for precision, but convert to wall-clock time
    val systemTime = System.currentTimeMillis()
    val nanoOffset = (System.nanoTime() % 1000000) / 1000 // microsecond precision
    return systemTime * 1000 + nanoOffset // Return in microseconds
    }


    fun getSynchronizedTime(clockOffsetMs: Long): Long {
    return getHighPrecisionTime() + (clockOffsetMs * 1000) // Convert offset to microseconds
    }


    fun validateSync(
        localTime: Long,
        remoteTime: Long,
        clockOffsetMs: Long,
        toleranceMs: Long = 5L,
    ): Boolean {
        val synchronizedLocalTime = localTime + (clockOffsetMs * 1000)
        val diff = abs(synchronizedLocalTime - (remoteTime * 1000)) / 1000 // Convert back to ms

        return diff <= toleranceMs
    }


    fun createSyncPacket(): JSONObject {
        val currentTime = getHighPrecisionTime()

        return JSONObject().apply {
            put("message_type", "time_sync_broadcast")
            put("sender_time", currentTime)
            put("version", "1.0")
            put("sender_id", android.os.Build.MODEL)
        }
    }


    fun processSyncPacket(packet: JSONObject): Long? {
        return try {
            if (packet.optString("message_type") == "time_sync_broadcast") {
                val senderTime = packet.getLong("sender_time")
                val receiveTime = getHighPrecisionTime()

                // Simple offset calculation (assume minimal network delay)
                senderTime - receiveTime
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sync packet", e)
            null
        }
    }


    fun cleanup() {
    stopPeriodicSync()
    syncScope.cancel()
    }
}
