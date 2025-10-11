package mpdc4gsr.core.common.logging

import android.content.Context
import android.util.Log

/**
 * Minimal logger that provides the API surface expected by the rewritten modules.
 * Messages are forwarded to [Log] with a structured tag/event/metadata triple.
 */
class StructuredLogger private constructor(
    private val appContext: Context,
) {

    fun log(
        level: LogLevel,
        tag: String,
        event: String,
        metadata: Map<String, Any?> = emptyMap(),
        throwable: Throwable? = null,
    ) {
        val message =
            buildString {
                append(event)
                if (metadata.isNotEmpty()) {
                    append(" | ")
                    append(
                        metadata.entries.joinToString(
                            separator = ", ",
                            transform = { (key, value) -> "$key=$value" },
                        ),
                    )
                }
            }
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }

    companion object {
        fun getInstance(context: Context): StructuredLogger =
            StructuredLogger(context.applicationContext)
    }

    enum class LogLevel { DEBUG, INFO, WARN, ERROR }
}
