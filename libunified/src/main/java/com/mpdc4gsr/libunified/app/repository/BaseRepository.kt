package com.mpdc4gsr.libunified.app.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

abstract class BaseRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Throwable) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }

    data class CachedData<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val ttlMs: Long = DEFAULT_CACHE_TTL
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > ttlMs
    }

    // Simple in-memory cache
    private val cache = mutableMapOf<String, CachedData<Any>>()
    protected suspend fun <T> safeCall(
        operation: suspend () -> T
    ): Result<T> {
        return withContext(ioDispatcher) {
            try {
                val result = operation()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    protected fun <T> safeFlow(
        operation: suspend () -> T
    ): Flow<Result<T>> = flow {
        emit(Result.Loading)
        try {
            val result = operation()
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(ioDispatcher)

    // The unchecked cast from CachedData<Any> to CachedData<T> is safe here because
    // each cacheKey is always associated with a single type T for the lifetime of the cache entry.
    // The function contract ensures that the same key is not reused for different types.
    @Suppress("UNCHECKED_CAST")
    protected suspend fun <T> getCachedOrExecute(
        cacheKey: String,
        ttlMs: Long = DEFAULT_CACHE_TTL,
        operation: suspend () -> T
    ): T {
        val cached = cache[cacheKey] as? CachedData<T>
        return if (cached != null && !cached.isExpired) {
            cached.data
        } else {
            val result = operation()
            cache[cacheKey] = CachedData(result as Any, ttlMs = ttlMs)
            result
        }
    }

    protected fun clearCache(key: String? = null) {
        if (key != null) {
            cache.remove(key)
        } else {
            cache.clear()
        }
    }

    protected fun <T> networkBoundResource(
        query: () -> Flow<T?>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit,
        shouldFetch: (T?) -> Boolean = { true }
    ): Flow<Result<T>> = flow {
        emit(Result.Loading)
        val data = query().collect { localData ->
            if (shouldFetch(localData)) {
                try {
                    val networkData = fetch()
                    saveFetchResult(networkData)
                    emit(Result.Success(networkData))
                } catch (e: Exception) {
                    if (localData != null) {
                        emit(Result.Success(localData))
                    } else {
                        emit(Result.Error(e))
                    }
                }
            } else if (localData != null) {
                emit(Result.Success(localData))
            }
        }
    }.flowOn(ioDispatcher)

    companion object {
        private const val DEFAULT_CACHE_TTL = 5 * 60 * 1000L // 5 minutes
    }
}