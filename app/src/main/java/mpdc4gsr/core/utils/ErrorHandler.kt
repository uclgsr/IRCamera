package mpdc4gsr.core.utils

object ErrorHandler {
    inline fun <T> runSafely(tag: String, operation: String, block: () -> T): Result<T> {
        return (
            Result.success(block())
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
        return (
            Result.success(block())
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
