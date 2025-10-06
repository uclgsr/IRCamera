package mpdc4gsr.feature.network.data

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler

object NetworkLogger {
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }

    private var currentLogLevel = LogLevel.DEBUG
    private var enableFileLogging = false

    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
        AppLogger.i("NetworkLogger", "Log level set to: $level")
    }

    fun setFileLogging(enabled: Boolean) {
        enableFileLogging = enabled
        AppLogger.i("NetworkLogger", "File logging ${if (enabled) "enabled" else "disabled"}")
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.VERBOSE)) {
            if (throwable != null) {
                AppLogger.v(tag, message, throwable)
            } else {
                AppLogger.v(tag, message)
            }
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.DEBUG)) {
            if (throwable != null) {
                AppLogger.d(tag, message, throwable)
            } else {
                AppLogger.d(tag, message)
            }
        }
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.INFO)) {
            if (throwable != null) {
                AppLogger.i(tag, message, throwable)
            } else {
                AppLogger.i(tag, message)
            }
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.WARN)) {
            if (throwable != null) {
                AppLogger.w(tag, message, throwable)
            } else {
                AppLogger.w(tag, message)
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.ERROR)) {
            if (throwable != null) {
                AppLogger.e(tag, message, throwable)
            } else {
                AppLogger.e(tag, message)
            }
        }
    }

    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= currentLogLevel.ordinal
    }

    fun configureForDebug() {
        setLogLevel(LogLevel.DEBUG)
        setFileLogging(true)
    }

    fun configureForRelease() {
        setLogLevel(LogLevel.WARN)
        setFileLogging(false)
    }
}