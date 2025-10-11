package com.mpdc4gsr.component.thermal.tools

object ThermalTools {
    fun getRotate(rotateType: Int): Float =
        when (rotateType) {
            1 -> 90f
            2 -> 180f
            3 -> 270f
            else -> 0f
        }
}

