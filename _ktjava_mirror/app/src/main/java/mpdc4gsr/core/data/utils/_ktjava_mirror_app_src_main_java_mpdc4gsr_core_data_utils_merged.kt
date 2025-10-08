// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\core\data\utils' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\core\data\utils\app_src_main_java_mpdc4gsr_core_data_utils_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\core\data\utils' subtree
// Files: 5; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\core\data\utils\BufferedDataWriter.kt =====

package mpdc4gsr.core.data.utils

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

open class BufferedDataWriter(
    private val outputFile: File,
    private val bufferSize: Int = 8192,
    private val flushIntervalMs: Long = 1000L,
    private val maxQueueSize: Int = 10000
) {
    companion object {
        private const val TAG = "BufferedDataWriter"
    }

    private var writer: BufferedWriter? = null
    private val writeQueue = LinkedBlockingQueue<String>(maxQueueSize)
    private val isRunning = AtomicBoolean(false)
    private val bytesWritten = AtomicLong(0)
    private val linesWritten = AtomicLong(0)
    private var writerJob: Job? = null
    private var flushJob: Job? = null
    private val writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            AppLogger.w(TAG, "Writer already running for ${outputFile.name}")
            return@withContext true
        }
        try {
            outputFile.parentFile?.mkdirs()
            writer = BufferedWriter(FileWriter(outputFile, true), bufferSize)
            isRunning.set(true)
            writerJob = writerScope.launch {
                runWriterLoop()
            }
            flushJob = writerScope.launch {
                runFlushLoop()
            }
            AppLogger.i(TAG, "Started buffered writer for ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start writer for ${outputFile.name}", e)
            cleanup()
            false
        }
    }

    fun writeLine(line: String): Boolean {
        if (!isRunning.get()) {
            AppLogger.w(TAG, "Writer not running, cannot write line")
            return false
        }
        val success = writeQueue.offer(line)
        if (!success) {
            AppLogger.w(TAG, "Write queue full, dropping line for ${outputFile.name}")
        }
        return success
    }

    fun writeLines(lines: List<String>): Int {
        if (!isRunning.get()) {
            return 0
        }
        var written = 0
        for (line in lines) {
            if (writeQueue.offer(line)) {
                written++
            } else {
                AppLogger.w(TAG, "Write queue full, stopping batch write")
                break
            }
        }
        return written
    }

    suspend fun flush() = withContext(Dispatchers.IO) {
        try {
            writer?.flush()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to flush writer for ${outputFile.name}", e)
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            return@withContext
        }
        AppLogger.i(TAG, "Stopping buffered writer for ${outputFile.name}")
        isRunning.set(false)
        try {
            writerJob?.cancel()
            flushJob?.cancel()
            writerJob?.join()
            drainQueue()
            writer?.flush()
            writer?.close()
            writer = null
            val stats = getWriteStats()
            Log.i(
                TAG,
                "Writer stopped for ${outputFile.name}: ${stats.linesWritten} lines, ${stats.bytesWritten} bytes"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping writer for ${outputFile.name}", e)
        }
    }

    fun getWriteStats(): WriteStats {
        return WriteStats(
            fileName = outputFile.name,
            bytesWritten = bytesWritten.get(),
            linesWritten = linesWritten.get(),
            queueSize = writeQueue.size,
            isRunning = isRunning.get()
        )
    }

    private suspend fun runWriterLoop() {
        AppLogger.d(TAG, "Starting writer loop for ${outputFile.name}")
        while (isRunning.get()) {
            try {
                val line = withContext(Dispatchers.IO) {
                    runInterruptible {
                        writeQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
                if (line != null) {
                    writer?.let { w ->
                        w.write(line)
                        w.newLine()
                        bytesWritten.addAndGet(line.length.toLong() + 1)
                        linesWritten.incrementAndGet()
                    }
                }
            } catch (e: InterruptedException) {
                AppLogger.d(TAG, "Writer loop interrupted for ${outputFile.name}")
                break
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error in writer loop for ${outputFile.name}", e)
                if (e is IOException) {
                    break
                }
            }
        }
        AppLogger.d(TAG, "Writer loop ended for ${outputFile.name}")
    }

    private suspend fun runFlushLoop() {
        while (isRunning.get()) {
            try {
                delay(flushIntervalMs)
                if (isRunning.get()) {
                    writer?.flush()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    AppLogger.e(TAG, "Error in flush loop for ${outputFile.name}", e)
                }
            }
        }
    }

    private fun drainQueue() {
        try {
            val remainingLines = mutableListOf<String>()
            writeQueue.drainTo(remainingLines)
            if (remainingLines.isNotEmpty()) {
                AppLogger.i(TAG, "Writing ${remainingLines.size} remaining lines for ${outputFile.name}")
                writer?.let { w ->
                    for (line in remainingLines) {
                        w.write(line)
                        w.newLine()
                        bytesWritten.addAndGet(line.length.toLong() + 1)
                        linesWritten.incrementAndGet()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error draining queue for ${outputFile.name}", e)
        }
    }

    private fun cleanup() {
        try {
            isRunning.set(false)
            writerJob?.cancel()
            flushJob?.cancel()
            writer?.close()
            writer = null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during cleanup for ${outputFile.name}", e)
        }
    }
}

data class WriteStats(
    val fileName: String,
    val bytesWritten: Long,
    val linesWritten: Long,
    val queueSize: Int,
    val isRunning: Boolean
) {
    val avgLineSize: Double
        get() = if (linesWritten > 0) bytesWritten.toDouble() / linesWritten else 0.0
    val formattedSize: String
        get() = when {
            bytesWritten > 1024 * 1024 -> String.format("%.2f MB", bytesWritten / (1024.0 * 1024.0))
            bytesWritten > 1024 -> String.format("%.2f KB", bytesWritten / 1024.0)
            else -> "$bytesWritten bytes"
        }
}


// ===== app\src\main\java\mpdc4gsr\core\data\utils\CSVBufferedWriter.kt =====

package mpdc4gsr.core.data .utils

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CSVBufferedWriter(
    outputFile: File,
    private val headers: List<String>,
    bufferSize: Int = 8192,
    flushIntervalMs: Long = 1000L
) : BufferedDataWriter(outputFile, bufferSize, flushIntervalMs) {
    companion object {
        private const val TAG = "CSVBufferedWriter"
    }

    private val headerWritten = AtomicBoolean(false)
    suspend fun startWithHeaders(): Boolean {
        val started = start()
        if (started && !headerWritten.get()) {
            writeHeaders()
        }
        return started
    }

    private suspend fun writeHeaders() {
        if (headerWritten.compareAndSet(false, true)) {
            val headerLine = headers.joinToString(",")
            writeLine(headerLine)
            AppLogger.d(TAG, "CSV headers written: $headerLine")
        }
    }

    fun writeRow(values: List<Any>): Boolean {
        val csvLine = values.joinToString(",") { value ->
            when (value) {
                is String -> escapeCSVValue(value)
                else -> value.toString()
            }
        }
        return writeLine(csvLine)
    }

    private fun escapeCSVValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun getCSVStats(): CSVWriteStats {
        val stats = getWriteStats()
        return CSVWriteStats(
            baseStats = stats,
            headerWritten = headerWritten.get(),
            columnCount = headers.size,
            headers = headers
        )
    }
}

data class CSVWriteStats(
    val baseStats: WriteStats,
    val headerWritten: Boolean,
    val columnCount: Int,
    val headers: List<String>
) {
    val rowsWritten: Long
        get() = if (headerWritten) baseStats.linesWritten - 1 else baseStats.linesWritten
    val averageRowSize: Double
        get() = if (rowsWritten > 0) baseStats.bytesWritten.toDouble() / rowsWritten else 0.0
}


// ===== app\src\main\java\mpdc4gsr\core\data\utils\SessionDirectoryManager.kt =====

package mpdc4gsr.core.data .utils

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SessionDirectoryManager(private val context: Context) {
    companion object {
        private const val TAG = "SessionDirectoryManager"
        private const val SESSIONS_ROOT_DIR = "sessions"
        private const val RGB_SUBDIR = "RGB"
        private const val THERMAL_SUBDIR = "Thermal"
        private const val SHIMMER_SUBDIR = "Shimmer"
        const val RGB_VIDEO_FILE = "rgb_video.mp4"
        const val SHIMMER_DATA_FILE = "shimmer_data.csv"
        const val THERMAL_FRAMES_FILE = "thermal_frames.csv"
        const val THERMAL_METADATA_FILE = "thermal_metadata.csv"
        const val SESSION_METADATA_FILE = "session_metadata.json"
        const val SYNC_MARKERS_FILE = "sync_markers.csv"
        private const val MIN_FREE_SPACE_MB = 500L
        private const val WARNING_FREE_SPACE_MB = 1000L
        private val SESSION_ID_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
    }

    private val baseDirectory: File by lazy {
        File(context.getExternalFilesDir(null), SESSIONS_ROOT_DIR).also {
            it.mkdirs()
        }
    }

    fun generateSessionId(): String {
        val timestamp = SESSION_ID_FORMAT.format(Date())
        val deviceModel = Build.MODEL.replace(Regex("[^a-zA-Z0-9]"), "")
        val uuid = UUID.randomUUID().toString().take(8)
        return "${timestamp}_${deviceModel}_${uuid}"
    }

    fun createSessionDirectory(sessionId: String): SessionDirectory {
        val sessionDir = File(baseDirectory, sessionId)
        if (!sessionDir.mkdirs() && !sessionDir.exists()) {
            throw IllegalStateException("Failed to create session directory: ${sessionDir.absolutePath}")
        }
        val rgbDir = File(sessionDir, RGB_SUBDIR).also { it.mkdirs() }
        val thermalDir = File(sessionDir, THERMAL_SUBDIR).also { it.mkdirs() }
        val shimmerDir = File(sessionDir, SHIMMER_SUBDIR).also { it.mkdirs() }
        AppLogger.i(TAG, "Created session directory structure: $sessionId")
        return SessionDirectory(
            sessionId = sessionId,
            rootDir = sessionDir,
            rgbDir = rgbDir,
            thermalDir = thermalDir,
            shimmerDir = shimmerDir
        )
    }

    fun createSessionMetadata(sessionDir: SessionDirectory, metadata: SessionMetadata): File {
        val metadataFile = File(sessionDir.rootDir, SESSION_METADATA_FILE)
        val jsonMetadata = JSONObject().apply {
            put("session_id", sessionDir.sessionId)
            put("start_time", metadata.startTime)
            put("device_model", Build.MODEL)
            put("device_manufacturer", Build.MANUFACTURER)
            put("app_version", getAppVersion())
            put("enabled_sensors", metadata.enabledSensors)
            put("participant_id", metadata.participantId ?: "")
            put("study_name", metadata.studyName ?: "")
            put("status", "ACTIVE")
            put("metadata", JSONObject(metadata.customMetadata))
        }
        metadataFile.writeText(jsonMetadata.toString(2))
        AppLogger.i(TAG, "Created session metadata: ${metadataFile.absolutePath}")
        return metadataFile
    }

    fun updateSessionMetadata(
        sessionDir: SessionDirectory,
        endTime: Long,
        status: String,
        errors: Map<String, String> = emptyMap()
    ) {
        val metadataFile = File(sessionDir.rootDir, SESSION_METADATA_FILE)
        if (metadataFile.exists()) {
            try {
                val jsonMetadata = JSONObject(metadataFile.readText())
                jsonMetadata.put("end_time", endTime)
                jsonMetadata.put("status", status)
                jsonMetadata.put("duration_ms", endTime - jsonMetadata.getLong("start_time"))
                if (errors.isNotEmpty()) {
                    jsonMetadata.put("errors", JSONObject(errors))
                }
                val filesInfo = getSessionFilesInfo(sessionDir)
                jsonMetadata.put("files", JSONObject(filesInfo))
                metadataFile.writeText(jsonMetadata.toString(2))
                AppLogger.i(TAG, "Updated session metadata: ${sessionDir.sessionId}")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to update session metadata", e)
            }
        }
    }

    fun checkStorageSpace(): StorageStatus {
        val stat = StatFs(baseDirectory.absolutePath)
        val availableBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        val availableMB = availableBytes / (1024 * 1024)
        return StorageStatus(
            availableMB = availableMB,
            totalMB = totalBytes / (1024 * 1024),
            isLowStorage = availableMB < MIN_FREE_SPACE_MB,
            shouldWarn = availableMB < WARNING_FREE_SPACE_MB
        )
    }

    fun cleanupFailedSessions(): List<String> {
        val cleanedSessions = mutableListOf<String>()
        baseDirectory.listFiles()?.forEach { sessionDir ->
            if (sessionDir.isDirectory && isFailedSession(sessionDir)) {
                try {
                    sessionDir.deleteRecursively()
                    cleanedSessions.add(sessionDir.name)
                    AppLogger.i(TAG, "Cleaned up failed session: ${sessionDir.name}")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to cleanup session: ${sessionDir.name}", e)
                }
            }
        }
        return cleanedSessions
    }

    private fun getSessionFilesInfo(sessionDir: SessionDirectory): Map<String, Any> {
        val filesInfo = mutableMapOf<String, Any>()
        val rgbVideo = File(sessionDir.rgbDir, RGB_VIDEO_FILE)
        val shimmerData = File(sessionDir.shimmerDir, SHIMMER_DATA_FILE)
        val thermalFrames = File(sessionDir.thermalDir, THERMAL_FRAMES_FILE)
        val syncMarkers = File(sessionDir.rootDir, SYNC_MARKERS_FILE)
        filesInfo["rgb_video"] = mapOf(
            "exists" to rgbVideo.exists(),
            "size_bytes" to if (rgbVideo.exists()) rgbVideo.length() else 0,
            "path" to rgbVideo.absolutePath
        )
        filesInfo["shimmer_data"] = mapOf(
            "exists" to shimmerData.exists(),
            "size_bytes" to if (shimmerData.exists()) shimmerData.length() else 0,
            "path" to shimmerData.absolutePath
        )
        filesInfo["thermal_frames"] = mapOf(
            "exists" to thermalFrames.exists(),
            "size_bytes" to if (thermalFrames.exists()) thermalFrames.length() else 0,
            "path" to thermalFrames.absolutePath
        )
        filesInfo["sync_markers"] = mapOf(
            "exists" to syncMarkers.exists(),
            "size_bytes" to if (syncMarkers.exists()) syncMarkers.length() else 0,
            "path" to syncMarkers.absolutePath
        )
        return filesInfo
    }

    private fun isFailedSession(sessionDir: File): Boolean {
        val metadataFile = File(sessionDir, SESSION_METADATA_FILE)
        if (!metadataFile.exists()) {
            val totalSize = sessionDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            return totalSize < 1024
        }
        try {
            val metadata = JSONObject(metadataFile.readText())
            val status = metadata.optString("status", "")
            if (status == "FAILED" || status == "ERROR") {
                val hasDataFiles = sessionDir.walkTopDown()
                    .filter { it.isFile && it.name != SESSION_METADATA_FILE }
                    .any { it.length() > 10240 }
                return !hasDataFiles
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to parse metadata for session ${sessionDir.name}", e)
            return false
        }
        return false
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun getStandardFilePath(sessionDir: SessionDirectory, sensor: String, fileName: String): File {
        val sensorDir = when (sensor.lowercase()) {
            "rgb", "camera", "rgbcamera" -> sessionDir.rgbDir
            "thermal", "thermalcamera" -> sessionDir.thermalDir
            "gsr", "shimmer", "shimmer3" -> sessionDir.shimmerDir
            else -> sessionDir.rootDir
        }
        return File(sensorDir, fileName)
    }

    fun deleteSession(sessionId: String): Boolean {
        return try {
            val sessionDir = File(baseDirectory, sessionId)
            val legacyDir = File(context.getExternalFilesDir(null), "recordings/$sessionId")
            var deleted = false
            if (sessionDir.exists()) {
                deleted = sessionDir.deleteRecursively()
                AppLogger.i(TAG, "Deleted session directory: $sessionId")
            }
            if (legacyDir.exists()) {
                deleted = legacyDir.deleteRecursively() || deleted
                AppLogger.i(TAG, "Deleted legacy session directory: $sessionId")
            }
            deleted
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete session: $sessionId", e)
            false
        }
    }

    fun exportSession(sessionId: String): Boolean {
        return try {
            val sessionDir = File(baseDirectory, sessionId)
            if (!sessionDir.exists()) {
                AppLogger.w(TAG, "Session directory not found for export: $sessionId")
                return false
            }
            // Export functionality is not implemented yet
            AppLogger.w(TAG, "Export functionality not implemented for session: $sessionId")
            // Return false to indicate the feature is not implemented
            false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export session: $sessionId", e)
            false
        }
    }
}

data class SessionDirectory(
    val sessionId: String,
    val rootDir: File,
    val rgbDir: File,
    val thermalDir: File,
    val shimmerDir: File
)

data class SessionMetadata(
    val startTime: Long,
    val enabledSensors: List<String>,
    val participantId: String? = null,
    val studyName: String? = null,
    val customMetadata: Map<String, Any> = emptyMap()
)

data class StorageStatus(
    val availableMB: Long,
    val totalMB: Long,
    val isLowStorage: Boolean,
    val shouldWarn: Boolean
) {
    val usagePercentage: Int
        get() = if (totalMB > 0) ((totalMB - availableMB) * 100 / totalMB).toInt() else 0
    val formattedAvailable: String
        get() = if (availableMB > 1024) {
            String.format("%.1f GB", availableMB / 1024.0)
        } else {
            "$availableMB MB"
        }
}


// ===== app\src\main\java\mpdc4gsr\core\data\utils\TimeManager.kt =====

package mpdc4gsr.core.data .utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
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
        fun getInstance(context: Context): TimeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeManager(context.applicationContext).also { INSTANCE = it }
            }
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
                    "Starting enhanced NTP-like time synchronization with PC Controller: $pcControllerAddress:$port"
                )
                Log.i(
                    TAG,
                    "Assumption: Both devices are synchronized to internet time servers for baseline accuracy"
                )
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
                    Log.i(
                        TAG,
                        "Enhanced NTP-like time synchronization successful with automatic drift monitoring"
                    )
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
                            Log.d(
                                TAG,
                                "Sync round ${attempt + 1}: offset=${syncResult.clockOffsetNs}ns, RTT=${syncResult.roundTripTimeNs / 1_000_000}ms",
                            )
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
                AppLogger.e(TAG, "Time synchronization error", e)
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
    }

    private suspend fun sendTimeSyncRequest(
        pcAddress: String,
        port: Int,
        localTime: Long,
    ): TimeSyncResponse? {
        return try {
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
                        java.nio.ByteBuffer.allocate(4).putInt(requestBytes.size).array()
                    outputStream.write(lengthBytes)
                    outputStream.write(requestBytes)
                    outputStream.flush()
                    val lengthBuffer = ByteArray(4)
                    inputStream.read(lengthBuffer, 0, 4)
                    val responseLength = java.nio.ByteBuffer.wrap(lengthBuffer).getInt()
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
                            line.substringAfter(":").trim().removeSuffix("}").toLongOrNull()
                    }

                    line.contains("pc_send_time") -> {
                        pcSendTime =
                            line.substringAfter(":").trim().removeSuffix("}").toLongOrNull()
                    }

                    line.contains("server_timestamp") && pcReceiveTime == null -> {
                        pcReceiveTime = line.substringAfter(":").trim()
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
                                Log.i(
                                    TAG,
                                    "Auto-resync triggered: ${timeSinceSync}ms since last sync"
                                )
                                attemptAutoResync("time_threshold")
                            }

                            currentQuality > CRITICAL_DRIFT_THRESHOLD_MS -> {
                                Log.w(
                                    TAG,
                                    "Auto-resync triggered: quality degraded to ${currentQuality}ms"
                                )
                                attemptAutoResync("quality_degradation")
                            }

                            timeSinceSync > 120_000L -> {
                                Log.d(
                                    TAG,
                                    "Drift monitoring: ${timeSinceSync}ms since sync, quality: ${currentQuality}ms"
                                )
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
                val retryCount = if (syncQualityMs.get() > HIGH_LATENCY_THRESHOLD_MS) {
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
        retryCount: Int
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
                        Log.d(
                            TAG,
                            "Enhanced sync round ${attempt + 1}: offset=${syncResult.clockOffsetNs}ns, RTT=${syncResult.roundTripTimeNs / 1_000_000}ms"
                        )
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
                    Log.i(
                        TAG,
                        "Enhanced sync completed: offset=${bestOffset}ns, latency: avg=${avgLatency.toInt()}ms, range=${minLatency}-${maxLatency}ms"
                    )
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
    fun setPCConnectionInfo(address: String, port: Int) {
        cachedPCAddress = address
        cachedPCPort = port
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
        AppLogger.i(TAG, "TimeManager cleaned up")
    }

    private fun logSyncQualityInfo() {
        val quality = getSyncQuality()
        val qualityLevel = when (quality.level) {
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

    fun setClockOffsetFromProtocolSync(offsetNs: Long, estimatedLatencyMs: Long = 0) {
        clockOffsetNs.set(offsetNs)
        lastSyncTimestamp.set(getCurrentTimestampNs())
        syncQualityMs.set(estimatedLatencyMs)
        isTimeSynced = true
        Log.i(
            TAG,
            "Clock offset set from protocol sync: ${offsetNs}ns (quality: ${estimatedLatencyMs}ms)"
        )
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


// ===== app\src\main\java\mpdc4gsr\core\data\utils\VersionUtils.kt =====

package mpdc4gsr.core.data .utils

import android.content.Context
import com.mpdc4gsr.libunified.app.utils.UnifiedVersionUtils

object VersionUtils {

    fun getCodeStr(context: Context): String {
        return UnifiedVersionUtils.getVersionName(context)
    }

    fun getVersionName(context: Context): String {
        return UnifiedVersionUtils.getVersionName(context)
    }

    fun getVersionCode(context: Context): Long {
        return UnifiedVersionUtils.getVersionCode(context)
    }

    fun isUpdateNeeded(context: Context, serverVersion: String): Boolean {
        return UnifiedVersionUtils.isUpdateNeeded(context, serverVersion)
    }
}