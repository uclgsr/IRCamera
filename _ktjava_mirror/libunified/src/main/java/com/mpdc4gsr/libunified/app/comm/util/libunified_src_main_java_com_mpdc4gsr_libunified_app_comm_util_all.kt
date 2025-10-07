// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util\SingletonHolder.kt =====

package com.mpdc4gsr.libunified.app.comm.util

open class SingletonHolder<out T, in A>(private val creator: (A) -> T) {
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


