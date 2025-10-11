package com.mpdc4gsr.component.shared.app.comm.util

open class SingletonHolder<out T, in A>(
    private val creator: (A) -> T,
) {
    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        // First check without synchronization for performance
        return instance ?: synchronized(this) {
            // Second check with synchronization to ensure thread safety
            instance ?: creator(arg).also { instance = it }
        }
    }
}


