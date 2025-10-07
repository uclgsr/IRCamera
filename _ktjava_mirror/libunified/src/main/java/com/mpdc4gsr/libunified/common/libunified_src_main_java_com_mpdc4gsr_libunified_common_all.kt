// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\common' subtree
// Files: 1; Generated 2025-10-07 23:07:48


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\common\RotateDegree.kt =====

package com.mpdc4gsr.libunified.common

enum class RotateDegree(private val value: Int) {
    DEGREE_0(0),
    DEGREE_90(90),
    DEGREE_180(180),
    DEGREE_270(270);

    fun getValue(): Int {
        return value
    }
}


