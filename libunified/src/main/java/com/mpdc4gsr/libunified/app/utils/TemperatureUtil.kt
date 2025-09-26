package com.mpdc4gsr.libunified.app.utils

/**
 * TemperatureUtil - Adapter for UnifiedTemperatureUtils
 * Provides Java compatibility for CarDetectChildBean usage
 */
object TemperatureUtil {
    
    /**
     * Get temperature string for range
     */
    fun getTempStr(minTemp: Int, maxTemp: Int): String {
        val minTempF = minTemp.toFloat()
        val maxTempF = maxTemp.toFloat()
        return "(${UnifiedTemperatureUtils.formatTemperature(minTempF)}~${UnifiedTemperatureUtils.formatTemperature(maxTempF)})"
    }
}