package com.topdon.tc001.sensors

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

object TimestampManager {
    private const val TAG = "TimestampManager"

    private val bootTimeReference = AtomicLong(0L)

    private val clockOffset = AtomicLong(0L)

    private val sessionStartTime = AtomicLong(0L)

    init {
        initializeTimestampSystem()
    }

    private fun initializeTimestampSystem() {
        bootTimeReference.set(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        Log.i(TAG, "Timestamp system initialized with boot reference: ${bootTimeReference.get()}")
    }

    fun getCurrentTimestampNanos(): Long {
        return System.nanoTime()
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
            Log.w(TAG, "Session not started, returning absolute timestamp")
            return getCurrentElapsedRealtimeMs()
        }
        return getCurrentElapsedRealtimeMs() - sessionStart
    }

    fun startSession(): Long {
        val sessionStart = getCurrentElapsedRealtimeMs()
        sessionStartTime.set(sessionStart)
        Log.i(TAG, "Session started at: $sessionStart ms")
        return sessionStart
    }

    fun endSession(): Long {
        val sessionEnd = getCurrentElapsedRealtimeMs()
        val sessionDuration = sessionEnd - sessionStartTime.get()
        sessionStartTime.set(0L)
        Log.i(TAG, "Session ended. Duration: $sessionDuration ms")
        return sessionDuration
    }

    fun setClockOffset(offsetMs: Long) {
        clockOffset.set(offsetMs)
        Log.i(TAG, "Clock offset set to: $offsetMs ms")
    }

    fun getSynchronizedTimestampMs(): Long {
        return getDeviceTimestampMs() + clockOffset.get()
    }

    fun createTimestampRecord(): TimestampRecord {
        val currentNanos = getCurrentTimestampNanos()
        val elapsedMs = getCurrentElapsedRealtimeMs()
        val deviceMs = getDeviceTimestampMs()
        val sessionRelativeMs = getSessionRelativeTimestampMs()
        val synchronizedMs = getSynchronizedTimestampMs()

        return TimestampRecord(
            systemNanos = currentNanos,
            elapsedRealtimeMs = elapsedMs,
            deviceTimestampMs = deviceMs,
            sessionRelativeMs = sessionRelativeMs,
            synchronizedTimestampMs = synchronizedMs,
        )
    }

    inline fun <T> measureExecutionTime(block: () -> T): Pair<T, Long> {
        val executionTime =
            measureNanoTime {
                return block() to 0L
            }
        return Pair(block(), executionTime)
    }
}

data class TimestampRecord(
    val systemNanos: Long, // High-precision nanosecond timestamp (for GSR samples)
    val elapsedRealtimeMs: Long, // System elapsed time (for sensor alignment)
    val deviceTimestampMs: Long, // Absolute device time (for data export)
    val sessionRelativeMs: Long, // Session-relative time (for analysis)
    val synchronizedTimestampMs: Long, // Hub-synchronized time (for multi-device sessions)
) {

    fun toCsvFormat(): String {
        return "$systemNanos,$elapsedRealtimeMs,$deviceTimestampMs,$sessionRelativeMs,$synchronizedTimestampMs"
    }

    companion object {
        fun getCsvHeader(): String {
            return "system_nanos,elapsed_realtime_ms,device_timestamp_ms,session_relative_ms,synchronized_timestamp_ms"
        }
    }
}
