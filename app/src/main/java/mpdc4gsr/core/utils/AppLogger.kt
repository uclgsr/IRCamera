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

    fun v(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (shouldLog(LogLevel.VERBOSE)) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }

    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (shouldLog(LogLevel.DEBUG)) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }

    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        component: String? = null,
    ) {
        if (shouldLog(LogLevel.INFO)) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
            logToStructured(
                LogLevel.INFO,
                component ?: tag,
                message,
                throwable,
            )
        }
    }

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        component: String? = null,
    ) {
        if (shouldLog(LogLevel.WARN)) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
            logToStructured(
                LogLevel.WARN,
                component ?: tag,
                message,
                throwable,
            )
        }
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        component: String? = null,
    ) {
        if (shouldLog(LogLevel.ERROR)) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
            logToStructured(
                LogLevel.ERROR,
                component ?: tag,
                message,
                throwable,
            )
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
        if (!enableStructuredLogging || structuredLogger == null) {
            return
        }

        val details =
            mutableMapOf<String, Any>("message" to message)
        throwable?.let {
            details["error"] = it.javaClass.simpleName
            details["error_message"] = it.message ?: "Unknown error"
            details["stack_trace"] = it.stackTraceToString()
        }

        val structuredLevel =
            when (level) {
                LogLevel.VERBOSE, LogLevel.DEBUG -> StructuredLogger.LogLevel.DEBUG
                LogLevel.INFO -> StructuredLogger.LogLevel.INFO
                LogLevel.WARN -> StructuredLogger.LogLevel.WARNING
                LogLevel.ERROR -> StructuredLogger.LogLevel.ERROR
            }

        structuredLogger?.log(
            structuredLevel,
            component,
            "log_message",
            details,
        )
    }
}
