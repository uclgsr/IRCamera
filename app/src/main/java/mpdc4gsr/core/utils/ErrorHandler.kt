package mpdc4gsr.core.utils

object ErrorHandler {

    inline fun <T> runSafely(
        tag: String,
        operation: String,
        block: () -> T,
    ): Result<T> {
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
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to $operation: ${e.message}, using default", e)
            defaultValue
        }
    }

    inline fun runSafelyIgnoreResult(
        tag: String,
        operation: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to $operation: ${e.message}", e)
        }
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
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to $operation: ${e.message}, using default", e)
            defaultValue
        }
    }
}
