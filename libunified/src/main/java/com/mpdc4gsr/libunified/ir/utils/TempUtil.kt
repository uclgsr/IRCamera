package com.mpdc4gsr.libunified.ir.utils

import android.graphics.Point

/**
 * TempUtil - Temperature utility functions for IR camera
 * Maps to UnifiedTemperatureUtils functionality
 */
object TempUtil {
    
    /**
     * Get line temperatures between two points
     */
    @JvmStatic
    fun getLineTemps(point1: Point, point2: Point, temperatureArray: ByteArray, width: Int): List<Float> {
        // Delegate to UnifiedTemperatureUtils
        return com.mpdc4gsr.libunified.app.utils.UnifiedTemperatureUtils.getLineTemperatures(
            point1, point2, temperatureArray, width, temperatureArray.size / width
        )
    }
}