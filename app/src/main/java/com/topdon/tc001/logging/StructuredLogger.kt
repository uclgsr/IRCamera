package com.topdon.tc001.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Structured JSON logging system for PC-to-phone communication
 * Phase 0 implementation - JSON logs with fields [ts, level, comp, device_id, conn_id, msg_id, event]
 */
class StructuredLogger private constructor(private val context: Context) {
    companion object {
        private const val TAG = "StructuredLogger"
        private const val LOG_DIRECTORY = "pc_to_phone_logs"
        private const val MAX_LOG_FILES = 10
        private const val MAX_LOG_SIZE_MB = 10
        private const val LOG_FLUSH_INTERVAL_MS = 5000L

        @Volatile
        private var instance: StructuredLogger? = null

        fun getInstance(context: Context): StructuredLogger {
            return instance ?: synchronized(this) {
                instance ?: StructuredLogger(context.applicationContext).also { instance = it }
            }
        }

        // Convenience methods
        fun logInfo(
            component: String,
            event: String,
            details: Map<String, Any> = emptyMap(),
        ) {
            instance?.log(LogLevel.INFO, component, event, details)
        }

        fun logWarning(
            component: String,
            event: String,
            details: Map<String, Any> = emptyMap(),
        ) {
            instance?.log(LogLevel.WARNING, component, event, details)
        }

        fun logError(
            component: String,
            event: String,
            details: Map<String, Any> = emptyMap(),
        ) {
            instance?.log(LogLevel.ERROR, component, event, details)
        }

        fun logDebug(
            component: String,
            event: String,
            details: Map<String, Any> = emptyMap(),
        ) {
            instance?.log(LogLevel.DEBUG, component, event, details)
        }
    }

    enum class LogLevel(val value: String) {
        DEBUG("DEBUG"),
        INFO("INFO"),
        WARNING("WARNING"),
        ERROR("ERROR"),
    }

    private val deviceId =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )

    private val logQueue = ConcurrentLinkedQueue<JSONObject>()
    private val logExecutor =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "StructuredLogger").apply { isDaemon = true }
        }

    private var currentLogFile: File? = null
    private var currentLogWriter: BufferedWriter? = null
    private var currentLogSize = 0L
    private val dateFormatter =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    // Message ID counter for tracking
    private var messageIdCounter = 0L

    // Coroutine scope for periodic flushing
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        initializeLogging()
        startPeriodicFlush()
    }

    private fun initializeLogging() {
        try {
            val logDir = File(context.getExternalFilesDir(null), LOG_DIRECTORY)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            // Clean up old log files
            cleanupOldLogs(logDir)

            // Create new log file
            createNewLogFile(logDir)

            // Log initialization
            log(
                LogLevel.INFO,
                "StructuredLogger",
                "logging_initialized",
                mapOf(
                    "log_directory" to logDir.absolutePath,
                    "device_id" to deviceId,
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize structured logging", e)
        }
    }

    private fun createNewLogFile(logDir: File) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        currentLogFile = File(logDir, "pc_to_phone_$timestamp.jsonl")

        currentLogWriter?.close()
        currentLogWriter = BufferedWriter(FileWriter(currentLogFile, true))
        currentLogSize = currentLogFile?.length() ?: 0L

        Log.i(TAG, "Created new log file: ${currentLogFile?.name}")
    }

    private fun cleanupOldLogs(logDir: File) {
        try {
            val logFiles =
                logDir.listFiles { _, name ->
                    name.startsWith("pc_to_phone_") && name.endsWith(".jsonl")
                }?.sortedByDescending { it.lastModified() }

            if (logFiles != null && logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    if (file.delete()) {
                        Log.i(TAG, "Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up old logs", e)
        }
    }

    /**
     * Log a structured message
     */
    fun log(
        level: LogLevel,
        component: String,
        event: String,
        details: Map<String, Any> = emptyMap(),
        connectionId: String? = null,
        messageId: String? = null,
    ) {
        try {
            val timestamp = dateFormatter.format(Date())
            val msgId = messageId ?: generateMessageId()

            val logEntry =
                JSONObject().apply {
                    put("ts", timestamp)
                    put("level", level.value)
                    put("comp", component)
                    put("device_id", deviceId)
                    put("conn_id", connectionId ?: "")
                    put("msg_id", msgId)
                    put("event", event)

                    // Add details
                    details.forEach { (key, value) ->
                        put(key, value)
                    }
                }

            // Add to queue for async processing
            logQueue.offer(logEntry)

            // Also log to Android logcat for immediate debugging
            val logMessage = "$component: $event ${if (details.isNotEmpty()) details else ""}"
            when (level) {
                LogLevel.DEBUG -> Log.d(TAG, logMessage)
                LogLevel.INFO -> Log.i(TAG, logMessage)
                LogLevel.WARNING -> Log.w(TAG, logMessage)
                LogLevel.ERROR -> Log.e(TAG, logMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating log entry", e)
        }
    }

    /**
     * Log connection event with automatic connection ID tracking
     */
    fun logConnection(
        event: String,
        connectionId: String,
        details: Map<String, Any> = emptyMap(),
    ) {
        log(LogLevel.INFO, "NetworkConnection", event, details, connectionId)
    }

    /**
     * Log protocol message with automatic message ID
     */
    fun logProtocolMessage(
        event: String,
        messageId: String,
        connectionId: String? = null,
        details: Map<String, Any> = emptyMap(),
    ) {
        log(LogLevel.INFO, "ProtocolHandler", event, details, connectionId, messageId)
    }

    /**
     * Log server socket events
     */
    fun logServerEvent(
        event: String,
        details: Map<String, Any> = emptyMap(),
    ) {
        log(LogLevel.INFO, "ServerSocket", event, details)
    }

    /**
     * Log sensor events
     */
    fun logSensorEvent(
        event: String,
        sensorType: String,
        details: Map<String, Any> = emptyMap(),
    ) {
        val sensorDetails = details.toMutableMap()
        sensorDetails["sensor_type"] = sensorType
        log(LogLevel.INFO, "SensorRecorder", event, sensorDetails)
    }

    /**
     * Log recording session events
     */
    fun logSessionEvent(
        event: String,
        sessionId: String,
        details: Map<String, Any> = emptyMap(),
    ) {
        val sessionDetails = details.toMutableMap()
        sessionDetails["session_id"] = sessionId
        log(LogLevel.INFO, "RecordingSession", event, sessionDetails)
    }

    private fun generateMessageId(): String {
        return "${System.currentTimeMillis()}_${++messageIdCounter}"
    }

    private fun startPeriodicFlush() {
        logScope.launch {
            while (true) {
                delay(LOG_FLUSH_INTERVAL_MS)
                flushLogs()
            }
        }

        // Process logs in background thread
        logExecutor.execute {
            while (true) {
                try {
                    processLogQueue()
                    Thread.sleep(100) // Small delay to prevent busy waiting
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing log queue", e)
                }
            }
        }
    }

    private fun processLogQueue() {
        val writer = currentLogWriter ?: return

        while (true) {
            val logEntry = logQueue.poll() ?: break

            try {
                writer.write(logEntry.toString())
                writer.newLine()
                currentLogSize += logEntry.toString().length + 1

                // Check if we need to rotate log file
                if (currentLogSize > MAX_LOG_SIZE_MB * 1024 * 1024) {
                    rotateLogFile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing log entry", e)
            }
        }
    }

    private fun flushLogs() {
        try {
            currentLogWriter?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing logs", e)
        }
    }

    private fun rotateLogFile() {
        try {
            currentLogWriter?.close()

            val logDir = File(context.getExternalFilesDir(null), LOG_DIRECTORY)
            createNewLogFile(logDir)
            cleanupOldLogs(logDir)

            log(
                LogLevel.INFO,
                "StructuredLogger",
                "log_file_rotated",
                mapOf(
                    "new_file" to (currentLogFile?.name ?: "unknown"),
                    "previous_size_mb" to (currentLogSize / (1024 * 1024)),
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating log file", e)
        }
    }

    /**
     * Get current log file path for debugging
     */
    fun getCurrentLogFile(): String? {
        return currentLogFile?.absolutePath
    }

    /**
     * Get log directory contents for debugging
     */
    fun getLogFiles(): List<String> {
        return try {
            val logDir = File(context.getExternalFilesDir(null), LOG_DIRECTORY)
            logDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting log files", e)
            emptyList()
        }
    }

    /**
     * Export recent logs as string for debugging
     */
    fun exportRecentLogs(maxLines: Int = 100): String {
        return try {
            val logFile = currentLogFile ?: return "No log file available"

            val lines = mutableListOf<String>()
            BufferedReader(FileReader(logFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null && lines.size < maxLines) {
                    line?.let { lines.add(it) }
                }
            }

            lines.takeLast(maxLines).joinToString("\n")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting logs", e)
            "Error exporting logs: ${e.message}"
        }
    }

    /**
     * Cleanup and shutdown logging
     */
    fun cleanup() {
        try {
            logScope.cancel()
            flushLogs()
            currentLogWriter?.close()
            logExecutor.shutdown()
            logExecutor.awaitTermination(5, TimeUnit.SECONDS)

            Log.i(TAG, "Structured logging cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logging cleanup", e)
        }
    }
}
