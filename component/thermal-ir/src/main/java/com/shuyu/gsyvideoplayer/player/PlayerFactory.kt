package com.shuyu.gsyvideoplayer.player


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
