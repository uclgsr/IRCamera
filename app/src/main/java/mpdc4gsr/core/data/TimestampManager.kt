package mpdc4gsr.core.data

import android.os.SystemClock
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

object TimestampManager {
    private const val TAG = "TimestampManager"

    private val bootTimeReference = AtomicLong(0L)
    private val clockOffset = AtomicLong(0L)
    private val sessionStartTime = AtomicLong(0L)

    private val sessionStartSystemMs = AtomicLong(0L)
    private val sessionStartMonotonicNs = AtomicLong(0L)

    init {
        initializeTimestampSystem()
    }

    private fun initializeTimestampSystem() {
        bootTimeReference.set(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        AppLogger.i(TAG, "Timestamp system initialized with boot reference: ${bootTimeReference.get()}")
    }

    fun nowNanos(): Long {
        return System.nanoTime()
    }

    fun getCurrentTimestampNanos(): Long {
        return SystemClock.elapsedRealtimeNanos()
    }

    /**
     * Formats a nanosecond timestamp to ISO 8601 string format
     */
    private val iso8601Format by lazy {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
    }

    fun formatTimestampIso(timestampNanos: Long): String {
        val timestampMillis = timestampNanos / 1_000_000
        val date = java.util.Date(timestampMillis)
        return iso8601Format.format(date)
    }

    fun getCurrentSystemTimeMs(): Long {
        return System.currentTimeMillis()
    }

    fun getCurrentElapsedRealtimeMs(): Long {
        return SystemClock.elapsedRealtime()
    }

    fun getDeviceTimestampMs(): Long {
        return bootTimeReference.get() + SystemClock.elapsedRealtime()
    }

    fun getSessionRelativeTimestampMs(): Long {
        val sessionStart = sessionStartTime.get()
        if (sessionStart == 0L) {
            AppLogger.w(TAG, "Session not started, returning absolute timestamp")
            return getCurrentElapsedRealtimeMs()
        }
        return getCurrentElapsedRealtimeMs() - sessionStart
    }

    fun startSession(): SessionTimestampReference {
        val sessionStart = getCurrentElapsedRealtimeMs()
        val systemStart = getCurrentSystemTimeMs()
        val monotonicStart = getCurrentTimestampNanos()

        sessionStartTime.set(sessionStart)
        sessionStartSystemMs.set(systemStart)
        sessionStartMonotonicNs.set(monotonicStart)

        val reference = SessionTimestampReference(
            sessionStartElapsedMs = sessionStart,
            sessionStartSystemMs = systemStart,
            sessionStartMonotonicNs = monotonicStart,
            bootTimeReferenceMs = bootTimeReference.get()
        )

        Log.i(
            TAG,
            "Session started with reference: system=${systemStart}ms, monotonic=${monotonicStart}ns"
        )
        return reference
    }

    fun endSession(): Long {
        val sessionEnd = getCurrentElapsedRealtimeMs()
        val sessionDuration = sessionEnd - sessionStartTime.get()
        sessionStartTime.set(0L)
        sessionStartSystemMs.set(0L)
        sessionStartMonotonicNs.set(0L)
        AppLogger.i(TAG, "Session ended. Duration: $sessionDuration ms")
        return sessionDuration
    }

    /**
     * Set clock offset for PC time synchronization.
     * This offset is applied to all synchronized timestamps.
     * Called by TimeSyncManager when sync completes.
     */
    fun setClockOffset(offsetMs: Long) {
        clockOffset.set(offsetMs)
        AppLogger.i(TAG, "Clock offset set to: $offsetMs ms")
    }

    /**
     * Get the current clock offset in milliseconds.
     * This is the offset calculated from time sync protocol.
     */
    fun getClockOffsetMs(): Long {
        return clockOffset.get()
    }

    /**
     * Get timestamp synchronized with PC clock.
     * This applies the offset calculated from time sync protocol.
     */
    fun getSynchronizedTimestampMs(): Long {
        return getDeviceTimestampMs() + clockOffset.get()
    }

    fun createTimestampRecord(): TimestampRecord {
        val currentNanos = getCurrentTimestampNanos()
        val systemMs = getCurrentSystemTimeMs()
        val elapsedMs = getCurrentElapsedRealtimeMs()
        val deviceMs = getDeviceTimestampMs()
        val sessionRelativeMs = getSessionRelativeTimestampMs()
        val synchronizedMs = getSynchronizedTimestampMs()

        return TimestampRecord(
            systemNanos = currentNanos,
            systemTimeMs = systemMs,
            elapsedRealtimeMs = elapsedMs,
            deviceTimestampMs = deviceMs,
            sessionRelativeMs = sessionRelativeMs,
            synchronizedTimestampMs = synchronizedMs,
        )
    }

    fun convertMonotonicToWallClock(monotonicNs: Long): Long {
        val sessionStartMono = sessionStartMonotonicNs.get()
        val sessionStartSys = sessionStartSystemMs.get()

        if (sessionStartMono == 0L) {
            AppLogger.w(TAG, "No session reference available for monotonic to wall-clock conversion")
            return getCurrentSystemTimeMs()
        }

        val offsetNs = monotonicNs - sessionStartMono
        val offsetMs = offsetNs / 1_000_000
        return sessionStartSys + offsetMs
    }

    fun getSessionRelativeNanos(currentMonotonicNs: Long = getCurrentTimestampNanos()): Long {
        val sessionStartMono = sessionStartMonotonicNs.get()
        if (sessionStartMono == 0L) {
            AppLogger.w(TAG, "No session started for relative timestamp")
            return currentMonotonicNs
        }
        return currentMonotonicNs - sessionStartMono
    }

    inline fun <T> measureExecutionTime(block: () -> T): Pair<T, Long> {
        var result: T
        val executionTime = measureNanoTime {
            result = block()
        }
        return Pair(result, executionTime)
    }
}

data class SessionTimestampReference(
    val sessionStartElapsedMs: Long,
    val sessionStartSystemMs: Long,
    val sessionStartMonotonicNs: Long,
    val bootTimeReferenceMs: Long
) {
    fun toCsvMetadata(): String {
        return "# Session Reference Timestamps\n" +
                "# session_start_elapsed_ms=$sessionStartElapsedMs\n" +
                "# session_start_system_ms=$sessionStartSystemMs\n" +
                "# session_start_monotonic_ns=$sessionStartMonotonicNs\n" +
                "# boot_time_reference_ms=$bootTimeReferenceMs\n"
    }
}

data class TimestampRecord(
    val systemNanos: Long,
    val systemTimeMs: Long,
    val elapsedRealtimeMs: Long,
    val deviceTimestampMs: Long,
    val sessionRelativeMs: Long,
    val synchronizedTimestampMs: Long,
) {

    fun toCsvFormat(): String {
        return "$systemNanos,$systemTimeMs,$elapsedRealtimeMs,$deviceTimestampMs,$sessionRelativeMs,$synchronizedTimestampMs"
    }

    companion object {
        fun getCsvHeader(): String {
            return "system_nanos,system_time_ms,elapsed_realtime_ms,device_timestamp_ms,session_relative_ms,synchronized_timestamp_ms"
        }
    }
}
