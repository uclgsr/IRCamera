package com.topdon.lib.ui.utils
import android.os.Handler
import android.os.Looper

/**
 * Main thread handler utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
object MainThreadHandler {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Executes runonuithread functionality.
     */
    fun runOnUiThread(r: Runnable?) {
        handler.post(r!!)
    }

    /**
     * Executes postdelayed functionality.
     */
    fun postDelayed(
        r: Runnable?,
        millis: Long,
    ) {
        handler.postDelayed(r!!, millis)
    }

    /**
     * Removes the specified  from the system.
     */
    fun remove(r: Runnable?) {
        handler.removeCallbacks(r!!)
    }
}
