package com.topdon.tc001.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

class TimeManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "TimeManager"
        private const val SYNC_TIMEOUT_MS = 5000L
        private const val SYNC_RETRY_COUNT = 3
        private const val SYNC_QUALITY_THRESHOLD_MS = 5.0
        private const val DRIFT_MONITORING_INTERVAL_MS = 30000L

        // Singleton instance
        @Volatile
        private var INSTANCE: TimeManager? = null

        fun getInstance(context: Context): TimeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Time synchronization state
    private var clockOffsetNs = AtomicLong(0) // Offset to align with PC Controller
    private var lastSyncTimestamp = AtomicLong(0)
    private var syncQualityMs = AtomicLong(Long.MAX_VALUE)
    private var isTimeSynced = false

    // Network connectivity
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Monitoring
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var driftMonitoringJob: Job? = null

    fun getCurrentTimestampNs(): Long {
        val monotonicTime = SystemClock.elapsedRealtimeNanos()
        val offset = clockOffsetNs.get()
        return monotonicTime + offset
    }

    fun getCurrentTimestampMs(): Long {
        return getCurrentTimestampNs() / 1_000_000
    }

    suspend fun synchronizeWithPC(
        pcControllerAddress: String,
        port: Int = 8082,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(
                    TAG,
                    "Starting time synchronization with PC Controller: $pcControllerAddress:$port"
                )

                if (!isNetworkAvailable()) {
                    Log.w(TAG, "Network not available for time synchronization")
                    return@withContext false
                }

                var bestOffset: Long? = null
                var bestRtt = Long.MAX_VALUE
                var successCount = 0

                // Perform multiple sync rounds for accuracy
                repeat(SYNC_RETRY_COUNT) { attempt ->
                    try {
                        val syncResult = performTimeSyncRound(pcControllerAddress, port)
                        if (syncResult != null) {
                            successCount++

                            // Use the measurement with the lowest RTT for best accuracy
                            if (syncResult.roundTripTimeNs < bestRtt) {
                                bestRtt = syncResult.roundTripTimeNs
                                bestOffset = syncResult.clockOffsetNs
                            }

                            Log.d(
                                TAG,
                                "Sync round ${attempt + 1}: offset=${syncResult.clockOffsetNs}ns, RTT=${syncResult.roundTripTimeNs / 1_000_000}ms",
                            )
                        }

                        // Brief delay between rounds
                        delay(100)
                    } catch (e: Exception) {
                        Log.w(TAG, "Sync round ${attempt + 1} failed", e)
                    }
                }

                if (bestOffset != null && successCount > 0) {
                    // Apply the best clock offset
                    clockOffsetNs.set(bestOffset!!)
                    lastSyncTimestamp.set(getCurrentTimestampNs())
                    syncQualityMs.set(bestRtt / 1_000_000)
                    isTimeSynced = true

                    // Start drift monitoring
                    startDriftMonitoring()

                    Log.i(
                        TAG,
                        "Time synchronization successful: offset=${bestOffset}ns, quality=${bestRtt / 1_000_000}ms"
                    )
                    return@withContext true
                } else {
                    Log.e(
                        TAG,
                        "Time synchronization failed: $successCount/$SYNC_RETRY_COUNT rounds succeeded"
                    )
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Time synchronization error", e)
                return@withContext false
            }
        }
    }

    private suspend fun performTimeSyncRound(
        pcAddress: String,
        port: Int,
    ): TimeSyncResult? {
        return withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            try {
                // Record local time before request
                val t1 = SystemClock.elapsedRealtimeNanos()

                // Send sync request to PC Controller
                val syncResponse = sendTimeSyncRequest(pcAddress, port, t1)

                // Record local time after response
                val t4 = SystemClock.elapsedRealtimeNanos()

                if (syncResponse != null) {
                    // Calculate clock offset using NTP algorithm
                    val t2 = syncResponse.pcReceiveTime
                    val t3 = syncResponse.pcSendTime

                    val roundTripTime = (t4 - t1)
                    val networkDelay = roundTripTime / 2
                    val clockOffset = ((t2 - t1) + (t3 - t4)) / 2

                    TimeSyncResult(
                        clockOffsetNs = clockOffset,
                        roundTripTimeNs = roundTripTime,
                        networkDelayNs = networkDelay,
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Time sync round failed", e)
                null
            }
        }
    }

    private suspend fun sendTimeSyncRequest(
        pcAddress: String,
        port: Int,
        localTime: Long,
    ): TimeSyncResponse? {
        // Real network communication with PC Controller using TCP socket
        return try {
            withContext(Dispatchers.IO) {
                // Create TCP socket connection to PC Controller
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(pcAddress, port), SYNC_TIMEOUT_MS.toInt())

                try {
                    val outputStream = socket.getOutputStream()
                    val inputStream = socket.getInputStream()

                    // Create time sync request message
                    val requestJson =
                        """
                        {
                            "type": "time_sync_request",
                            "client_send_time": $localTime,
                            "session_id": "${UUID.randomUUID()}"
                        }
                        """.trimIndent()

                    // Send request to PC Controller
                    outputStream.write(requestJson.toByteArray(Charsets.UTF_8))
                    outputStream.flush()

                    // Read response from PC Controller
                    val buffer = ByteArray(1024)
                    val bytesRead = inputStream.read(buffer)
                    val responseStr = String(buffer, 0, bytesRead, Charsets.UTF_8)

                    // Parse JSON response
                    val response = parseTimeSyncResponse(responseStr)

                    Log.d(TAG, "Real time sync response received from PC Controller")
                    response
                } finally {
                    socket.close()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send real time sync request to PC Controller", e)
            null
        }
    }

    private fun parseTimeSyncResponse(responseJson: String): TimeSyncResponse? {
        return try {
            // Parse real JSON response from PC Controller
            // Expected format: {"pc_receive_time": ..., "pc_send_time": ...}
            val lines = responseJson.split(",")
            var pcReceiveTime: Long? = null
            var pcSendTime: Long? = null

            for (line in lines) {
                when {
                    line.contains("pc_receive_time") -> {
                        pcReceiveTime =
                            line.substringAfter(":").trim().removeSuffix("}").toLongOrNull()
                    }

                    line.contains("pc_send_time") -> {
                        pcSendTime =
                            line.substringAfter(":").trim().removeSuffix("}").toLongOrNull()
                    }
                }
            }

            if (pcReceiveTime != null && pcSendTime != null) {
                TimeSyncResponse(
                    pcReceiveTime = pcReceiveTime,
                    pcSendTime = pcSendTime,
                )
            } else {
                Log.w(TAG, "Invalid time sync response format from PC Controller")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time sync response from PC Controller", e)
            null
        }
    }

    private fun startDriftMonitoring() {
        driftMonitoringJob?.cancel()
        driftMonitoringJob =
            syncScope.launch {
                while (isActive && isTimeSynced) {
                    delay(DRIFT_MONITORING_INTERVAL_MS)

                    try {
                        // Check if resync is needed based on time since last sync
                        val timeSinceSync =
                            (getCurrentTimestampNs() - lastSyncTimestamp.get()) / 1_000_000

                        if (timeSinceSync > 300_000) { // 5 minutes
                            Log.i(
                                TAG,
                                "Clock drift monitoring: time since last sync = ${timeSinceSync}ms"
                            )
                            // Could trigger automatic resync here if needed
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Drift monitoring error", e)
                    }
                }
            }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    fun getSyncQuality(): SyncQuality {
        val qualityMs = syncQualityMs.get()
        val timeSinceSync =
            if (lastSyncTimestamp.get() > 0) {
                (getCurrentTimestampNs() - lastSyncTimestamp.get()) / 1_000_000
            } else {
                Long.MAX_VALUE
            }

        val quality =
            when {
                !isTimeSynced -> SyncQualityLevel.NOT_SYNCED
                qualityMs <= SYNC_QUALITY_THRESHOLD_MS -> SyncQualityLevel.EXCELLENT
                qualityMs <= SYNC_QUALITY_THRESHOLD_MS * 2 -> SyncQualityLevel.GOOD
                qualityMs <= SYNC_QUALITY_THRESHOLD_MS * 4 -> SyncQualityLevel.FAIR
                else -> SyncQualityLevel.POOR
            }

        return SyncQuality(
            level = quality,
            offsetNs = clockOffsetNs.get(),
            qualityMs = if (qualityMs == Long.MAX_VALUE) null else qualityMs,
            timeSinceSyncMs = if (timeSinceSync == Long.MAX_VALUE) null else timeSinceSync,
            isSynced = isTimeSynced,
        )
    }

    fun createSyncMarker(markerType: String): SyncMarker {
        val timestamp = getCurrentTimestampNs()
        return SyncMarker(
            markerType = markerType,
            timestampNs = timestamp,
            clockOffsetNs = clockOffsetNs.get(),
            syncQuality = getSyncQuality(),
        )
    }

    fun calculateTimeDifferenceNs(
        timestamp1: Long,
        timestamp2: Long,
    ): Long {
        return abs(timestamp2 - timestamp1)
    }

    fun areTimestampsSynchronized(
        timestamp1: Long,
        timestamp2: Long,
        toleranceMs: Double = SYNC_QUALITY_THRESHOLD_MS,
    ): Boolean {
        val differenceMs = calculateTimeDifferenceNs(timestamp1, timestamp2) / 1_000_000.0
        return differenceMs <= toleranceMs
    }

    fun cleanup() {
        driftMonitoringJob?.cancel()
        syncScope.cancel()
        isTimeSynced = false
        Log.i(TAG, "TimeManager cleaned up")
    }
}

private data class TimeSyncResult(
    val clockOffsetNs: Long,
    val roundTripTimeNs: Long,
    val networkDelayNs: Long,
)

private data class TimeSyncResponse(
    val pcReceiveTime: Long,
    val pcSendTime: Long,
)

enum class SyncQualityLevel {
    NOT_SYNCED,
    EXCELLENT, // < 5ms
    GOOD, // 5-10ms
    FAIR, // 10-20ms
    POOR, // > 20ms
}

data class SyncQuality(
    val level: SyncQualityLevel,
    val offsetNs: Long,
    val qualityMs: Long?,
    val timeSinceSyncMs: Long?,
    val isSynced: Boolean,
)

data class SyncMarker(
    val markerType: String,
    val timestampNs: Long,
    val clockOffsetNs: Long,
    val syncQuality: SyncQuality,
)
