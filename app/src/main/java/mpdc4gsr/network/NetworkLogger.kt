package mpdc4gsr.network


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
        currentLogLevel = level    }

    /**
     * Enable or disable file logging
     */
    fun setFileLogging(enabled: Boolean) {
        enableFileLogging = enabled"enabled" else "disabled"}")
    }

    /**
     * Log verbose message
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.VERBOSE)) {
            if (throwable != null) {            } else {            }
        }
    }

    /**
     * Log debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.DEBUG)) {
            if (throwable != null) {            } else {            }
        }
    }

    /**
     * Log info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.INFO)) {
            if (throwable != null) {            } else {            }
        }
    }

    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.WARN)) {
            if (throwable != null) {            } else {            }
        }
    }

    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.ERROR)) {
            if (throwable != null) {            } else {            }
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