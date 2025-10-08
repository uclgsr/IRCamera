package mpdc4gsr.feature.network.data


object NetworkLogger {
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }

    private var currentLogLevel = LogLevel.DEBUG
    private var enableFileLogging = false

    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
    }

    fun setFileLogging(enabled: Boolean) {
        enableFileLogging = enabled
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.VERBOSE)) {
            if (throwable != null) {
            } else {
            }
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.DEBUG)) {
            if (throwable != null) {
            } else {
            }
        }
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.INFO)) {
            if (throwable != null) {
            } else {
            }
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.WARN)) {
            if (throwable != null) {
            } else {
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.ERROR)) {
            if (throwable != null) {
            } else {
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