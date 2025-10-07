// Merged .kt under 'app\src\main\java\mpdc4gsr\core\utils' subtree
// Files: 3; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\core\utils\AppLogger.kt =====

package mpdc4gsr.core.utils

import android.util.Log
import mpdc4gsr.core.StructuredLogger

object AppLogger {
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }

    private var minLogLevel = LogLevel.DEBUG
    private var enableStructuredLogging = false
    private var structuredLogger: StructuredLogger? = null
    fun initialize(
        minLevel: LogLevel = LogLevel.DEBUG,
        enableStructured: Boolean = false,
        structuredLoggerInstance: StructuredLogger? = null,
    ) {
        minLogLevel = minLevel
        enableStructuredLogging = enableStructured
        structuredLogger = structuredLoggerInstance
    }

    fun setMinLogLevel(level: LogLevel) {
        minLogLevel = level
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null, component: String? = null) {
        log(LogLevel.INFO, tag, message, throwable, component)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null, component: String? = null) {
        log(LogLevel.WARN, tag, message, throwable, component)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, component: String? = null) {
        log(LogLevel.ERROR, tag, message, throwable, component)
    }

    private fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        component: String? = null,
    ) {
        if (!shouldLog(level)) return
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
        if (level.ordinal >= LogLevel.INFO.ordinal) {
            logToStructured(level, component ?: tag, message, throwable)
        }
    }

    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= minLogLevel.ordinal
    }

    private fun logToStructured(
        level: LogLevel,
        component: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (!enableStructuredLogging || structuredLogger == null) return
        val details = mutableMapOf<String, Any>("message" to message)
        throwable?.let {
            details["error"] = it.javaClass.simpleName
            details["error_message"] = it.message ?: "Unknown error"
            details["stack_trace"] = it.stackTraceToString()
        }
        val structuredLevel = when (level) {
            LogLevel.VERBOSE, LogLevel.DEBUG -> StructuredLogger.LogLevel.DEBUG
            LogLevel.INFO -> StructuredLogger.LogLevel.INFO
            LogLevel.WARN -> StructuredLogger.LogLevel.WARNING
            LogLevel.ERROR -> StructuredLogger.LogLevel.ERROR
        }
        structuredLogger?.log(structuredLevel, component, "log_message", details)
    }
}


// ===== app\src\main\java\mpdc4gsr\core\utils\DimensionUtils.kt =====

package mpdc4gsr.core.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp


fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Int.pxToDp(context: Context): Int {
    return (this / context.resources.displayMetrics.density).toInt()
}

fun Int.spToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

fun Float.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}

fun Float.pxToDp(context: Context): Float {
    return this / context.resources.displayMetrics.density
}

fun Float.spToPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        context.resources.displayMetrics
    )
}

val Int.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }
val Float.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }

@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)")
)
val Int.dpLegacy: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)")
)
val Int.pxLegacy: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

class ScreenDimensions(private val context: Context) {
    val screenWidthPx: Int
        get() = context.resources.displayMetrics.widthPixels
    val screenHeightPx: Int
        get() = context.resources.displayMetrics.heightPixels
    val screenDensity: Float
        get() = context.resources.displayMetrics.density
    val screenDensityDpi: Int
        get() = context.resources.displayMetrics.densityDpi
    val screenWidthDp: Int
        get() = screenWidthPx.pxToDp(context)
    val screenHeightDp: Int
        get() = screenHeightPx.pxToDp(context)
}

@Composable
fun rememberScreenDimensions(): ScreenDimensions {
    val context = LocalContext.current
    return ScreenDimensions(context)
}


// ===== app\src\main\java\mpdc4gsr\core\utils\ErrorHandler.kt =====

package mpdc4gsr.core.utils

object ErrorHandler {
    inline fun <T> runSafely(tag: String, operation: String, block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to $operation: ${e.message}", e)
            Result.failure(e)
        }
    }

    inline fun <T> runSafelyWithDefault(
        tag: String,
        operation: String,
        defaultValue: T,
        block: () -> T,
    ): T = runSafely(tag, operation, block).getOrDefault(defaultValue)

    inline fun runSafelyIgnoreResult(tag: String, operation: String, block: () -> Unit) {
        runSafely(tag, operation, block)
    }

    suspend inline fun <T> runSafelySuspend(
        tag: String,
        operation: String,
        block: suspend () -> T,
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to $operation: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend inline fun <T> runSafelySuspendWithDefault(
        tag: String,
        operation: String,
        defaultValue: T,
        block: suspend () -> T,
    ): T = runSafelySuspend(tag, operation, block).getOrDefault(defaultValue)
}


