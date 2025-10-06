package mpdc4gsr.core.utils

import android.util.Log
import mpdc4gsr.core.StructuredLogger

object AppLogger {
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }

    private var minLogLevel = LogLevel.DEBUG
    private var enableStructuredLogging = false
    private var structuredLogger: StructuredLogger? = null
    fun initialize(
        minLevel: LogLevel = LogLevel.DEBUG,
        enableStructured: Boolean = false,
        structuredLoggerInstance: StructuredLogger? = null,
    ) {
        minLogLevel = minLevel
        enableStructuredLogging = enableStructured
        structuredLogger = structuredLoggerInstance
    }

    fun setMinLogLevel(level: LogLevel) {
        minLogLevel = level
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null, component: String? = null) {
        log(LogLevel.INFO, tag, message, throwable, component)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null, component: String? = null) {
        log(LogLevel.WARN, tag, message, throwable, component)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, component: String? = null) {
        log(LogLevel.ERROR, tag, message, throwable, component)
    }

    private fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        component: String? = null,
    ) {
        if (!shouldLog(level)) return
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
        if (level.ordinal >= LogLevel.INFO.ordinal) {
            logToStructured(level, component ?: tag, message, throwable)
        }
    }

    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= minLogLevel.ordinal
    }

    private fun logToStructured(
        level: LogLevel,
        component: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (!enableStructuredLogging || structuredLogger == null) return
        val details = mutableMapOf<String, Any>("message" to message)
        throwable?.let {
            details["error"] = it.javaClass.simpleName
            details["error_message"] = it.message ?: "Unknown error"
            details["stack_trace"] = it.stackTraceToString()
        }
        val structuredLevel = when (level) {
            LogLevel.VERBOSE, LogLevel.DEBUG -> StructuredLogger.LogLevel.DEBUG
            LogLevel.INFO -> StructuredLogger.LogLevel.INFO
            LogLevel.WARN -> StructuredLogger.LogLevel.WARNING
            LogLevel.ERROR -> StructuredLogger.LogLevel.ERROR
        }
        structuredLogger?.log(structuredLevel, component, "log_message", details)
    }
}
