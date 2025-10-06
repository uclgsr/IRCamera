CENTRALIZED LOGGING AND ERROR HANDLING

This package provides centralized logging and error handling utilities for the application.

APPLOGGER
---------

AppLogger is a centralized logging utility that provides:

- Configurable log levels (VERBOSE, DEBUG, INFO, WARN, ERROR)
- Integration with StructuredLogger for file-based logging
- Consistent API across the application
- Support for exception logging

Usage:

    // Initialize in Application.onCreate()
    AppLogger.initialize(
        minLevel = AppLogger.LogLevel.DEBUG,
        enableStructured = true,
        structuredLoggerInstance = StructuredLogger.getInstance(context)
    )

    // Basic logging
    AppLogger.d("TAG", "Debug message")
    AppLogger.i("TAG", "Info message")
    AppLogger.w("TAG", "Warning message")
    AppLogger.e("TAG", "Error message")

    // Logging with exceptions
    AppLogger.e("TAG", "Error occurred", throwable)

    // Logging with structured logging component
    AppLogger.e("TAG", "Error message", throwable, component = "SensorManager")

    // Change log level at runtime
    AppLogger.setMinLogLevel(AppLogger.LogLevel.WARN)

ERRORHANDLER
------------

ErrorHandler provides utilities for safe error handling using Kotlin Result types.

Usage:

    // Safe execution with Result
    val result = ErrorHandler.runSafely("TAG", "connect to device") {
        deviceManager.connect()
    }
    result.onSuccess { device ->
        // Handle success
    }.onFailure { error ->
        // Handle failure
    }

    // Safe execution with default value
    val value = ErrorHandler.runSafelyWithDefault("TAG", "read sensor", defaultValue = 0.0) {
        sensor.readValue()
    }

    // Safe execution ignoring result
    ErrorHandler.runSafelyIgnoreResult("TAG", "update UI") {
        updateDisplay()
    }

    // Suspend functions
    suspend fun connectDevice() {
        val result = ErrorHandler.runSafelySuspend("TAG", "connect") {
            deviceManager.connectAsync()
        }
    }

MIGRATION FROM EXISTING CODE
-----------------------------

Replace direct android.util.Log calls:

    // Before
    Log.d("TAG", "Message")
    Log.e("TAG", "Error", exception)

    // After
    AppLogger.d("TAG", "Message")
    AppLogger.e("TAG", "Error", exception)

Replace try-catch blocks:

    // Before
    try {
        val result = riskyOperation()
        processResult(result)
    } catch (e: Exception) {
        Log.e("TAG", "Operation failed", e)
        showError()
    }

    // After
    ErrorHandler.runSafely("TAG", "perform risky operation") {
        riskyOperation()
    }.onSuccess { result ->
        processResult(result)
    }.onFailure {
        showError()
    }

EXISTING LOGGERS
----------------

The following specialized loggers remain available but should consider using AppLogger:

- StructuredLogger: For structured file-based logging (PC-to-phone protocol)
- NetworkLogger: For network-specific logging (can be migrated to use AppLogger)
- CrashHandler: For uncaught exception handling (in libunified module)

These specialized loggers can continue to be used for their specific purposes, but new code
should prefer AppLogger for consistency.
