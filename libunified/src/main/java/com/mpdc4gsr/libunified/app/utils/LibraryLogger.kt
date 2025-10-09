package com.mpdc4gsr.libunified.app.utils

import android.util.Log

/**
 * Lightweight logger for the libunified module that safely forwards to AppLogger when available.
 */
object LibraryLogger {
    private const val DEFAULT_TAG = "LibUnified"

    fun e(tag: String?, message: String, throwable: Throwable? = null) {
        val safeTag = tag?.takeIf { it.isNotBlank() } ?: DEFAULT_TAG
        // Local logging for library usage. The application layer may provide additional logging.
        if (throwable != null) {
            Log.e(safeTag, message, throwable)
        } else {
            Log.e(safeTag, message)
        }
    }
}
