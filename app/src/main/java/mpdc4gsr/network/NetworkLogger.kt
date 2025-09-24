package mpdc4gsr.network

import android.util.Log

/**
 * Configurable logging for network components with debug/release level support
 */
object NetworkLogger {
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }
    
    private var currentLogLevel = LogLevel.DEBUG
    private var enableFileLogging = false
    
    /**
     * Set the current log level
     */
    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
        Log.i("NetworkLogger", "Log level set to: $level")
    }
    
    /**
     * Enable or disable file logging
     */
    fun setFileLogging(enabled: Boolean) {
        enableFileLogging = enabled
        Log.i("NetworkLogger", "File logging ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Log verbose message
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.VERBOSE)) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }
    
    /**
     * Log debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.DEBUG)) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.INFO)) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.WARN)) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.ERROR)) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= currentLogLevel.ordinal
    }
    
    /**
     * Configure logging for debug builds
     */
    fun configureForDebug() {
        setLogLevel(LogLevel.DEBUG)
        setFileLogging(true)
    }
    
    /**
     * Configure logging for release builds
     */
    fun configureForRelease() {
        setLogLevel(LogLevel.WARN)
        setFileLogging(false)
    }
}