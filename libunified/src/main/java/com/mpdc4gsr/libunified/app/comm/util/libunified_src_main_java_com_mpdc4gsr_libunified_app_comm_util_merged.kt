// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util\SingletonHolder.kt =====

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