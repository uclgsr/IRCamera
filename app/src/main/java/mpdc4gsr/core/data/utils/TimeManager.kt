package mpdc4gsr.core.data.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import kotlinx.coroutines.*
import mpdc4gsr.core.utils.AppLogger
import java.util.*
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
        private const val HIGH_LATENCY_THRESHOLD_MS = 50.0
        private const val POOR_NETWORK_RETRY_COUNT = 5
        private const val AUTO_RESYNC_THRESHOLD_MS = 300_000L
        private const val CRITICAL_DRIFT_THRESHOLD_MS = 100.0

        @Volatile
        private var INSTANCE: TimeManager? = null

        fun getInstance(context: Context): TimeManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private var clockOffsetNs = AtomicLong(0)
    private var lastSyncTimestamp = AtomicLong(0)
    private var syncQualityMs = AtomicLong(Long.MAX_VALUE)
    private var isTimeSynced = false
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var driftMonitoringJob: Job? = null

    fun getCurrentTimestampNs(): Long {
        val monotonicTime = SystemClock.elapsedRealtimeNanos()
        val offset = clockOffsetNs.get()
        return monotonicTime + offset
    }

    fun getCurrentTimestampMs(): Long = getCurrentTimestampNs() / 1_000_000

    suspend fun synchronizeWithPC(
        pcControllerAddress: String,
        port: Int = 8082,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                setPCConnectionInfo(pcControllerAddress, port)
                if (!isNetworkAvailable()) {
                    AppLogger.w(TAG, "Network not available for time synchronization")
                    return@withContext false
                }
                val success = performEnhancedTimeSync(pcControllerAddress, port, SYNC_RETRY_COUNT)
                if (success) {
                    isTimeSynced = true
                    logSyncQualityInfo()
                    startDriftMonitoring()
                    AppLogger.i(TAG, "Cross-device synchronization established for timestamp alignment")
                }
                return@withContext success
                var bestOffset: Long? = null
                var bestRtt = Long.MAX_VALUE
                var successCount = 0
                repeat(SYNC_RETRY_COUNT) { attempt ->
                    try {
                        val syncResult = performTimeSyncRound(pcControllerAddress, port)
                        if (syncResult != null) {
                            successCount++
                            if (syncResult.roundTripTimeNs < bestRtt) {
                                bestRtt = syncResult.roundTripTimeNs
                                bestOffset = syncResult.clockOffsetNs
                            }
                        }
                        delay(100)
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Sync round ${attempt + 1} failed", e)
                    }
                }
                if (bestOffset != null && successCount > 0) {
                    clockOffsetNs.set(bestOffset!!)
                    lastSyncTimestamp.set(getCurrentTimestampNs())
                    syncQualityMs.set(bestRtt / 1_000_000)
                    isTimeSynced = true
                    startDriftMonitoring()
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Time synchronization error", e)
                return@withContext false
            }
        }
    }

    private suspend fun performTimeSyncRound(
        pcAddress: String,
        port: Int,
    ): TimeSyncResult? =
        withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            try {
                val t1 = SystemClock.elapsedRealtimeNanos()
                val syncResponse = sendTimeSyncRequest(pcAddress, port, t1)
                val t4 = SystemClock.elapsedRealtimeNanos()
                if (syncResponse != null) {
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
                AppLogger.w(TAG, "Time sync round failed", e)
                null
            }
        }

    private suspend fun sendTimeSyncRequest(
        pcAddress: String,
        port: Int,
        localTime: Long,
    ): TimeSyncResponse? =
        try {
            withContext(Dispatchers.IO) {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(pcAddress, port), SYNC_TIMEOUT_MS.toInt())
                try {
                    val outputStream = socket.getOutputStream()
                    val inputStream = socket.getInputStream()
                    val requestJson =
                        """
                        {
                            "message_type": "time_sync_request",
                            "client_timestamp": $localTime,
                            "device_id": "android_${android.os.Build.MODEL.replace(" ", "_")}",
                            "session_id": "${UUID.randomUUID()}"
                        }
                        """.trimIndent()
                    val requestBytes = requestJson.toByteArray(Charsets.UTF_8)
                    val lengthBytes =
                        java.nio.ByteBuffer
                            .allocate(4)
                            .putInt(requestBytes.size)
                            .array()
                    outputStream.write(lengthBytes)
                    outputStream.write(requestBytes)
                    outputStream.flush()
                    val lengthBuffer = ByteArray(4)
                    inputStream.read(lengthBuffer, 0, 4)
                    val responseLength =
                        java.nio.ByteBuffer
                            .wrap(lengthBuffer)
                            .getInt()
                    val responseBuffer = ByteArray(responseLength)
                    inputStream.read(responseBuffer, 0, responseLength)
                    val responseStr = String(responseBuffer, Charsets.UTF_8)
                    val response = parseTimeSyncResponse(responseStr)
                    AppLogger.d(TAG, "Real time sync response received from PC Controller")
                    response
                } finally {
                    socket.close()
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to send real time sync request to PC Controller", e)
            null
        }

    private fun parseTimeSyncResponse(responseJson: String): TimeSyncResponse? {
        return try {
            var serverReceiveTime: Long? = null
            var serverSendTime: Long? = null
            try {
                val json = org.json.JSONObject(responseJson)
                if (json.has("server_receive_time") && json.has("server_send_time")) {
                    AppLogger.d(TAG, "Enhanced time sync protocol response received from PC Controller")
                    serverReceiveTime = json.getLong("server_receive_time")
                    serverSendTime = json.getLong("server_send_time")
                    return TimeSyncResponse(
                        pcReceiveTime = serverReceiveTime,
                        pcSendTime = serverSendTime,
                    )
                }
            } catch (e: org.json.JSONException) {
                AppLogger.w(TAG, "Could not parse as JSON, will attempt legacy parsing: $e")
            }
            val lines = responseJson.split(",")
            var pcReceiveTime: Long? = null
            var pcSendTime: Long? = null
            for (line in lines) {
                when {
                    line.contains("pc_receive_time") -> {
                        pcReceiveTime =
                            line
                                .substringAfter(":")
                                .trim()
                                .removeSuffix("}")
                                .toLongOrNull()
                    }

                    line.contains("pc_send_time") -> {
                        pcSendTime =
                            line
                                .substringAfter(":")
                                .trim()
                                .removeSuffix("}")
                                .toLongOrNull()
                    }

                    line.contains("server_timestamp") && pcReceiveTime == null -> {
                        pcReceiveTime =
                            line
                                .substringAfter(":")
                                .trim()
                                .removeSuffix("}")
                                .removeSuffix(",")
                                .toLongOrNull()
                    }
                }
            }
            if (pcReceiveTime != null && pcSendTime != null) {
                TimeSyncResponse(
                    pcReceiveTime = pcReceiveTime,
                    pcSendTime = pcSendTime,
                )
            } else {
                AppLogger.w(TAG, "Invalid time sync response format from PC Controller: $responseJson")
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse time sync response from PC Controller", e)
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
                        val timeSinceSync =
                            (getCurrentTimestampNs() - lastSyncTimestamp.get()) / 1_000_000
                        val currentQuality = syncQualityMs.get()
                        when {
                            timeSinceSync > AUTO_RESYNC_THRESHOLD_MS -> {
                                attemptAutoResync("time_threshold")
                            }

                            currentQuality > CRITICAL_DRIFT_THRESHOLD_MS -> {
                                attemptAutoResync("quality_degradation")
                            }

                            timeSinceSync > 120_000L -> {
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Drift monitoring error", e)
                    }
                }
            }
    }

    private fun attemptAutoResync(reason: String) {
        syncScope.launch {
            try {
                AppLogger.i(TAG, "Attempting auto-resync (reason: $reason)")
                val retryCount =
                    if (syncQualityMs.get() > HIGH_LATENCY_THRESHOLD_MS) {
                        POOR_NETWORK_RETRY_COUNT
                    } else {
                        SYNC_RETRY_COUNT
                    }
                val originalRetryCount = SYNC_RETRY_COUNT
                val success =
                    performEnhancedTimeSync(getCurrentPCAddress(), getCurrentPCPort(), retryCount)
                if (success) {
                    AppLogger.i(TAG, "Auto-resync successful (reason: $reason)")
                } else {
                    AppLogger.w(TAG, "Auto-resync failed (reason: $reason) - will retry at next interval")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Auto-resync error (reason: $reason)", e)
            }
        }
    }

    private suspend fun performEnhancedTimeSync(
        pcAddress: String?,
        pcPort: Int?,
        retryCount: Int,
    ): Boolean {
        if (pcAddress == null || pcPort == null) return false
        return withContext(Dispatchers.IO) {
            var bestOffset: Long? = null
            var bestRtt = Long.MAX_VALUE
            var successCount = 0
            val measurements = mutableListOf<Long>()
            repeat(retryCount) { attempt ->
                try {
                    val syncResult = performTimeSyncRound(pcAddress, pcPort)
                    if (syncResult != null) {
                        successCount++
                        measurements.add(syncResult.roundTripTimeNs / 1_000_000)
                        if (syncResult.roundTripTimeNs < bestRtt) {
                            bestRtt = syncResult.roundTripTimeNs
                            bestOffset = syncResult.clockOffsetNs
                        }
                    }
                    val avgLatency = if (measurements.isNotEmpty()) measurements.average() else 0.0
                    val delayMs = if (avgLatency > HIGH_LATENCY_THRESHOLD_MS) 500L else 100L
                    delay(delayMs)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Enhanced sync round ${attempt + 1} failed", e)
                }
            }
            if (bestOffset != null && successCount > 0) {
                clockOffsetNs.set(bestOffset!!)
                lastSyncTimestamp.set(getCurrentTimestampNs())
                syncQualityMs.set(bestRtt / 1_000_000)
                if (measurements.isNotEmpty()) {
                    val avgLatency = measurements.average()
                    val minLatency = measurements.minOrNull() ?: 0L
                    val maxLatency = measurements.maxOrNull() ?: 0L
                }
                true
            } else {
                AppLogger.e(TAG, "Enhanced time sync failed: $successCount/$retryCount rounds succeeded")
                false
            }
        }
    }

    private var cachedPCAddress: String? = null
    private var cachedPCPort: Int? = null

    private fun getCurrentPCAddress(): String? = cachedPCAddress

    private fun getCurrentPCPort(): Int? = cachedPCPort

    fun setPCConnectionInfo(
        address: String,
        port: Int,
    ) {
        cachedPCAddress = address
        cachedPCPort = port
    }

    private fun isNetworkAvailable(): Boolean =
        try {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
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
    ): Long = abs(timestamp2 - timestamp1)

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
        AppLogger.i(TAG, "TimeManager cleaned up")
    }

    private fun logSyncQualityInfo() {
        val quality = getSyncQuality()
        val qualityLevel =
            when (quality.level) {
                SyncQualityLevel.EXCELLENT -> "EXCELLENT (<= ${SYNC_QUALITY_THRESHOLD_MS}ms)"
                SyncQualityLevel.GOOD -> "GOOD (<= ${SYNC_QUALITY_THRESHOLD_MS * 2}ms)"
                SyncQualityLevel.FAIR -> "FAIR (<= ${SYNC_QUALITY_THRESHOLD_MS * 4}ms)"
                SyncQualityLevel.POOR -> "POOR (> ${SYNC_QUALITY_THRESHOLD_MS * 4}ms)"
                SyncQualityLevel.NOT_SYNCED -> "NOT_SYNCED"
            }
        AppLogger.i(TAG, "Cross-device sync quality: $qualityLevel")
        quality.qualityMs?.let {
            AppLogger.i(TAG, "Network latency quality: ${it}ms")
        }
        AppLogger.i(TAG, "Clock offset: ${quality.offsetNs}ns (${quality.offsetNs / 1_000_000}ms)")
    }

    fun setClockOffsetFromProtocolSync(
        offsetNs: Long,
        estimatedLatencyMs: Long = 0,
    ) {
        clockOffsetNs.set(offsetNs)
        lastSyncTimestamp.set(getCurrentTimestampNs())
        syncQualityMs.set(estimatedLatencyMs)
        isTimeSynced = true
        // Start drift monitoring if not already active
        if (driftMonitoringJob?.isActive != true) {
            startDriftMonitoring()
        }
    }

    fun getClockOffsetNs(): Long = clockOffsetNs.get()
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
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
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
