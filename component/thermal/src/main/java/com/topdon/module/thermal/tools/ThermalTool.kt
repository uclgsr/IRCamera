package com.topdon.module.thermal.tools

/**
 * Thermal tool tools for thermal imaging processing.
 * Contains specialized algorithms and processing functions.
 */
object ThermalTool {
    fun getRotate(rotateType: Int): Float {
        return when (rotateType) {
            1 -> 90f
            2 -> 180f
            3 -> 270f
            else -> 0f
        }
    }
}
