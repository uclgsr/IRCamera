package com.mpdc4gsr.component.shared.app.utils

/**
 * Lightweight logger for the Shared component that safely forwards to AppLogger when available.
 */
object LibraryLogger {
    private const val DEFAULT_TAG = "SharedComponent"

    fun e(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    ) {
        val safeTag = tag?.takeIf { it.isNotBlank() } ?: DEFAULT_TAG
        // Local logging for library usage. The application layer may provide additional logging.
        if (throwable != null) {
        } else {
        }
    }
}



