package mpdc4gsr.core.monitoring

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StructuredLogger private constructor(private val context: Context) {
    companion object {
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
    private val writerLock = Any()
    private val dateFormatter =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    private var messageIdCounter = 0L
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        initializeLogging()
        startPeriodicFlush()
    }

    private fun initializeLogging() {
            val logDir = File(context.getExternalFilesDir(null), LOG_DIRECTORY)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            cleanupOldLogs(logDir)
            createNewLogFile(logDir)
            log(
                LogLevel.INFO,
                "StructuredLogger",
                "logging_initialized",
                mapOf(
                    "log_directory" to logDir.absolutePath,
                    "device_id" to deviceId,
                ),
            )
        }
    }

    private fun createNewLogFile(logDir: File) {
        synchronized(writerLock) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            currentLogFile = File(logDir, "pc_to_phone_$timestamp.jsonl")
                currentLogWriter?.close()
            }
            currentLogWriter = BufferedWriter(FileWriter(currentLogFile, true))
            currentLogSize = currentLogFile?.length() ?: 0L
        }
    }

    private fun cleanupOldLogs(logDir: File) {
            val logFiles =
                logDir.listFiles { _, name ->
                    name.startsWith("pc_to_phone_") && name.endsWith(".jsonl")
                }?.sortedByDescending { it.lastModified() }
            if (logFiles != null && logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    if (file.delete()) {
                    }
                }
            }
        }
    }

    fun log(
        level: LogLevel,
        component: String,
        event: String,
        details: Map<String, Any> = emptyMap(),
        connectionId: String? = null,
        messageId: String? = null,
    ) {
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
                    details.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            logQueue.offer(logEntry)
            val logMessage = "$component: $event ${if (details.isNotEmpty()) details else ""}"
            when (level) {
                LogLevel.DEBUG -> Log.d(TAG, logMessage)
                LogLevel.INFO -> Log.i(TAG, logMessage)
                LogLevel.WARNING -> Log.w(TAG, logMessage)
                LogLevel.ERROR -> Log.e(TAG, logMessage)
            }
        }
    }

    fun logConnection(event: String, connectionId: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, "NetworkConnection", event, details, connectionId)
    }

    fun logProtocolMessage(
        event: String,
        messageId: String,
        connectionId: String? = null,
        details: Map<String, Any> = emptyMap(),
    ) {
        log(LogLevel.INFO, "ProtocolHandler", event, details, connectionId, messageId)
    }

    fun logServerEvent(event: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, "ServerSocket", event, details)
    }

    fun logSensorEvent(event: String, sensorType: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, "SensorRecorder", event, details + ("sensor_type" to sensorType))
    }

    fun logSessionEvent(event: String, sessionId: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, "RecordingSession", event, details + ("session_id" to sessionId))
    }

    private fun generateMessageId(): String {
        return "${System.currentTimeMillis()}_${++messageIdCounter}"
    }

    private fun startPeriodicFlush() {
        logScope.launch {
            while (isActive) {
                delay(LOG_FLUSH_INTERVAL_MS)
                flushLogs()
            }
        }
        logScope.launch {
            while (isActive) {
                    processLogQueue()
                    delay(100)
                }
            }
        }
    }

    private fun processLogQueue() {
        while (true) {
            val logEntry = logQueue.poll() ?: break
            synchronized(writerLock) {
                val writer = currentLogWriter
                if (writer == null) {
                    return@synchronized
                }
                    writer.write(logEntry.toString())
                    writer.newLine()
                    currentLogSize += logEntry.toString().length + 1
                    if (currentLogSize > MAX_LOG_SIZE_MB * 1024 * 1024) {
                        rotateLogFile()
                    }
                    // If write fails, try to recreate the writer
                            recreateLogWriter()
                        }
                    }
                }
            }
        }
    }

    private fun recreateLogWriter() {
        synchronized(writerLock) {
                currentLogWriter?.close()
            }
            val logFile = currentLogFile
            if (logFile != null) {
                currentLogWriter = BufferedWriter(FileWriter(logFile, true))
            } else {
                val logDir = File(context.getExternalFilesDir(null), LOG_DIRECTORY)
                createNewLogFile(logDir)
            }
        }
    }

    private fun flushLogs() {
        synchronized(writerLock) {
                currentLogWriter?.flush()
            }
        }
    }

    private fun rotateLogFile() {
        synchronized(writerLock) {
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
            }
        }
    }

    fun getCurrentLogFile(): String? {
        return currentLogFile?.absolutePath
    }

    fun getLogFiles(): List<String> {
        return (
            val logDir = File(context.getExternalFilesDir(null), LOG_DIRECTORY)
            logDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
            emptyList()
        }
    }

    fun exportRecentLogs(maxLines: Int = 100): String {
        return (
            val logFile = currentLogFile ?: return "No log file available"
            val lines = mutableListOf<String>()
            BufferedReader(FileReader(logFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null && lines.size < maxLines) {
                    line?.let { lines.add(it) }
                }
            }
            lines.takeLast(maxLines).joinToString("\n")
        }
    }

    fun cleanup() {
            logScope.cancel()
            flushLogs()
            synchronized(writerLock) {
                currentLogWriter?.close()
                currentLogWriter = null
            }
            logExecutor.shutdown()
            logExecutor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}
