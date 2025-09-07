package com.example.thermal_lite.util

/**
 * Common utilities migrated from thermal-lite module
 */
object CommonUtil {
    
    fun formatTemperature(temp: Float): String {
        return String.format("%.1f°C", temp)
    }
    
    fun validateTemperatureRange(temp: Float): Boolean {
        return temp in -40.0..1000.0
    }
}