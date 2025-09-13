package com.shuyu.gsyvideoplayer.player

/**
 * Player factory utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
object PlayerFactory {
    const val SYSTEM_PLAYER = 0
    const val IJK_PLAYER = 1
    const val EXO_PLAYER = 2

    @JvmStatic
    fun setPlayManager(playerType: Int) {
        // Stub implementation
    }

    @JvmStatic
    fun setPlayManager(clazz: Class<*>) {
        // Stub implementation for Class parameter
    }
}
