package mpdc4gsr.core.data.utils

import android.content.Context
import java.util.concurrent.atomic.AtomicReference

/**
 * Lightweight replacement for the legacy TimeManager. The implementation only exposes
 * the functionality currently required by the rewritten thermal pipeline.
 */
class TimeManager private constructor(
    @Suppress("UNUSED_PARAMETER")
    private val appContext: Context,
) {

    fun getCurrentTimestampMs(): Long = System.currentTimeMillis()

    companion object {
        private val instanceRef = AtomicReference<TimeManager>()

        fun getInstance(context: Context): TimeManager =
            instanceRef.updateAndGet { existing ->
                existing ?: TimeManager(context.applicationContext)
            }!!
    }
}

