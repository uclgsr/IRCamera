package com.mpdc4gsr.module.thermalunified.player
object PlayerFactory {
    const val SYSTEM_PLAYER = 0
    const val IJK_PLAYER = 1
    const val EXO_PLAYER = 2
    @JvmStatic
    fun setPlayManager(playerType: Int) {
    }
    @JvmStatic
    fun setPlayManager(clazz: Class<*>) {
    }
}
