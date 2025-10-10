package mpdc4gsr.core.data

import android.os.SystemClock
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
    }

    fun nowNanos(): Long = System.nanoTime()

    fun getCurrentTimestampNanos(): Long = SystemClock.elapsedRealtimeNanos()

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

    fun getCurrentSystemTimeMs(): Long = System.currentTimeMillis()

    fun getCurrentElapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()

    fun getDeviceTimestampMs(): Long = bootTimeReference.get() + SystemClock.elapsedRealtime()

    fun getSessionRelativeTimestampMs(): Long {
        val sessionStart = sessionStartTime.get()
        if (sessionStart == 0L) {
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
        val reference =
            SessionTimestampReference(
                sessionStartElapsedMs = sessionStart,
                sessionStartSystemMs = systemStart,
                sessionStartMonotonicNs = monotonicStart,
                bootTimeReferenceMs = bootTimeReference.get(),
            )
        return reference
    }

    fun endSession(): Long {
        val sessionEnd = getCurrentElapsedRealtimeMs()
        val sessionDuration = sessionEnd - sessionStartTime.get()
        sessionStartTime.set(0L)
        sessionStartSystemMs.set(0L)
        sessionStartMonotonicNs.set(0L)
        return sessionDuration
    }

    fun setClockOffset(offsetMs: Long) {
        clockOffset.set(offsetMs)
    }

    fun getClockOffsetMs(): Long = clockOffset.get()

    fun getSynchronizedTimestampMs(): Long = getDeviceTimestampMs() + clockOffset.get()

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
            return getCurrentSystemTimeMs()
        }
        val offsetNs = monotonicNs - sessionStartMono
        val offsetMs = offsetNs / 1_000_000
        return sessionStartSys + offsetMs
    }

    fun getSessionRelativeNanos(currentMonotonicNs: Long = getCurrentTimestampNanos()): Long {
        val sessionStartMono = sessionStartMonotonicNs.get()
        if (sessionStartMono == 0L) {
            return currentMonotonicNs
        }
        return currentMonotonicNs - sessionStartMono
    }

    inline fun <T> measureExecutionTime(block: () -> T): Pair<T, Long> {
        var result: T
        val executionTime =
            measureNanoTime {
                result = block()
            }
        return Pair(result, executionTime)
    }
}

data class SessionTimestampReference(
    val sessionStartElapsedMs: Long,
    val sessionStartSystemMs: Long,
    val sessionStartMonotonicNs: Long,
    val bootTimeReferenceMs: Long,
) {
    fun toCsvMetadata(): String =
        "# Session Reference Timestamps\n" +
            "# session_start_elapsed_ms=$sessionStartElapsedMs\n" +
            "# session_start_system_ms=$sessionStartSystemMs\n" +
            "# session_start_monotonic_ns=$sessionStartMonotonicNs\n" +
            "# boot_time_reference_ms=$bootTimeReferenceMs\n"
}

data class TimestampRecord(
    val systemNanos: Long,
    val systemTimeMs: Long,
    val elapsedRealtimeMs: Long,
    val deviceTimestampMs: Long,
    val sessionRelativeMs: Long,
    val synchronizedTimestampMs: Long,
) {
    fun toCsvFormat(): String =
        "$systemNanos,$systemTimeMs,$elapsedRealtimeMs,$deviceTimestampMs,$sessionRelativeMs,$synchronizedTimestampMs"

    companion object {
        fun getCsvHeader(): String =
            "system_nanos,system_time_ms,elapsed_realtime_ms,device_timestamp_ms,session_relative_ms,synchronized_timestamp_ms"
    }
}
