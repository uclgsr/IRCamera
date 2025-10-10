package mpdc4gsr.feature.capture.camera.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Lightweight helper that tracks camera throughput so we can surface actionable
 * diagnostics in the UI and throttle background work when the device starts to lag.
 */
class CameraPerformanceManager(
    private val context: Context,
) {
    data class PerformanceSnapshot(
        val averageFps: Double,
        val frameIntervalNs: Long,
        val framesCaptured: Int,
        val droppedFrames: Int,
        val availableDiskMb: Long,
    )

    private val frameTimestamps = ArrayDeque<Long>()
    private val maxHistory = 180
    private var droppedFrames: Int = 0

    fun recordFrame(timestampNs: Long) {
        synchronized(frameTimestamps) {
            frameTimestamps.addLast(timestampNs)
            if (frameTimestamps.size > maxHistory) {
                frameTimestamps.removeFirst()
            }
        }
    }

    fun recordDroppedFrame() {
        droppedFrames = min(droppedFrames + 1, Int.MAX_VALUE)
    }

    fun reset() {
        synchronized(frameTimestamps) {
            frameTimestamps.clear()
        }
        droppedFrames = 0
    }

    fun snapshot(): PerformanceSnapshot {
        val timestampsCopy = synchronized(frameTimestamps) { frameTimestamps.toList() }
        if (timestampsCopy.size < 2) {
            return PerformanceSnapshot(0.0, 0, timestampsCopy.size, droppedFrames, getAvailableDiskMb())
        }
        val intervals = timestampsCopy.zipWithNext { previous, current -> current - previous }
        val averageInterval = intervals.average().toLong()
        val averageFps =
            if (averageInterval == 0L) {
                0.0
            } else {
                TimeUnit.SECONDS.toNanos(1).toDouble() / averageInterval.toDouble()
            }
        return PerformanceSnapshot(
            averageFps = averageFps,
            frameIntervalNs = averageInterval,
            framesCaptured = timestampsCopy.size,
            droppedFrames = droppedFrames,
            availableDiskMb = getAvailableDiskMb(),
        )
    }

    fun shouldThrottleCapture(): Boolean {
        val snapshot = snapshot()
        return snapshot.averageFps > 0 && snapshot.averageFps < THROTTLE_THRESHOLD_FPS
    }

    private fun getAvailableDiskMb(): Long =
        runCatching {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            val stat = StatFs(dir.path)
            val bytesAvailable = stat.availableBytes
            bytesAvailable / (1024 * 1024)
        }.getOrElse { 0L }

    companion object {
        private const val THROTTLE_THRESHOLD_FPS = 15.0
    }
}
