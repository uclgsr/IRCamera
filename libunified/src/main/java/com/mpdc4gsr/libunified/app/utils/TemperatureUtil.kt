package com.mpdc4gsr.libunified.app.utils

import com.mpdc4gsr.libunified.app.common.SharedManager
import kotlin.math.roundToInt

/**
 * Temperature utility class for handling temperature conversions and formatting
 * Based on existing SharedManager integration and temperature handling patterns
 */
object TemperatureUtil {

    const val CELSIUS = 1
    const val FAHRENHEIT = 2
    const val KELVIN = 3

    /**
     * Convert Celsius to Fahrenheit
     */
    fun celsiusToFahrenheit(celsius: Float): Float {
        return celsius * 9f / 5f + 32f
    }

    /**
     * Convert Fahrenheit to Celsius
     */
    fun fahrenheitToCelsius(fahrenheit: Float): Float {
        return (fahrenheit - 32f) * 5f / 9f
    }

    /**
     * Convert Celsius to Kelvin
     */
    fun celsiusToKelvin(celsius: Float): Float {
        return celsius + 273.15f
    }

    /**
     * Convert Kelvin to Celsius
     */
    fun kelvinToCelsius(kelvin: Float): Float {
        return kelvin - 273.15f
    }

    /**
     * Convert temperature to the preferred unit based on SharedManager settings
     */
    fun convertToPreferredUnit(celsius: Float): Float {
        return when (SharedManager.getTemperature()) {
            FAHRENHEIT -> celsiusToFahrenheit(celsius)
            KELVIN -> celsiusToKelvin(celsius)
            else -> celsius // CELSIUS is default
        }
    }

    /**
     * Convert temperature from preferred unit back to Celsius
     */
    fun convertFromPreferredUnit(temperature: Float): Float {
        return when (SharedManager.getTemperature()) {
            FAHRENHEIT -> fahrenheitToCelsius(temperature)
            KELVIN -> kelvinToCelsius(temperature)
            else -> temperature // Already Celsius
        }
    }

    /**
     * Format temperature with unit symbol
     */
    fun formatTemperature(celsius: Float, includeDegreeSymbol: Boolean = true): String {
        val convertedTemp = convertToPreferredUnit(celsius)
        val unit = getTemperatureUnitSymbol()
        val formatted = String.format("%.1f", convertedTemp)
        
        return if (includeDegreeSymbol) {
            "$formatted°$unit"
        } else {
            "$formatted$unit"
        }
    }

    /**
     * Format temperature with specified decimal places
     */
    fun formatTemperature(celsius: Float, decimalPlaces: Int): String {
        val convertedTemp = convertToPreferredUnit(celsius)
        val unit = getTemperatureUnitSymbol()
        val formatted = String.format("%.${decimalPlaces}f", convertedTemp)
        return "$formatted°$unit"
    }

    /**
     * Get temperature unit symbol based on SharedManager setting
     */
    fun getTemperatureUnitSymbol(): String {
        return when (SharedManager.getTemperature()) {
            FAHRENHEIT -> "F"
            KELVIN -> "K"
            else -> "C"
        }
    }

    /**
     * Get temperature unit name based on SharedManager setting
     */
    fun getTemperatureUnitName(): String {
        return when (SharedManager.getTemperature()) {
            FAHRENHEIT -> "Fahrenheit"
            KELVIN -> "Kelvin"
            else -> "Celsius"
        }
    }

    /**
     * Parse temperature string and convert to Celsius
     */
    fun parseTemperature(temperatureString: String): Float? {
        return try {
            val cleanString = temperatureString.replace(Regex("[°CFKcfk]"), "").trim()
            val value = cleanString.toFloat()
            
            when {
                temperatureString.contains("F", ignoreCase = true) -> fahrenheitToCelsius(value)
                temperatureString.contains("K", ignoreCase = true) -> kelvinToCelsius(value)
                else -> value // Assume Celsius if no unit specified
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Check if temperature is within a reasonable range for thermal imaging
     */
    fun isReasonableTemperature(celsius: Float): Boolean {
        // Typical thermal camera range: -40°C to 150°C
        return celsius in -40f..150f
    }

    /**
     * Get temperature difference in the preferred unit
     */
    fun getTemperatureDifference(celsius1: Float, celsius2: Float): Float {
        val temp1 = convertToPreferredUnit(celsius1)
        val temp2 = convertToPreferredUnit(celsius2)
        return temp1 - temp2
    }

    /**
     * Round temperature to nearest integer
     */
    fun roundTemperature(celsius: Float): Int {
        val convertedTemp = convertToPreferredUnit(celsius)
        return convertedTemp.roundToInt()
    }

    /**
     * Get temperature color coding for visualization
     */
    fun getTemperatureColorCode(celsius: Float): String {
        return when {
            celsius < 0 -> "#0000FF"    // Blue for very cold
            celsius < 10 -> "#00FFFF"   // Cyan for cold
            celsius < 20 -> "#00FF00"   // Green for cool
            celsius < 30 -> "#FFFF00"   // Yellow for warm
            celsius < 40 -> "#FFA500"   // Orange for hot
            else -> "#FF0000"           // Red for very hot
        }
    }

    /**
     * Validate temperature value
     */
    fun isValidTemperature(temperature: Float?): Boolean {
        return temperature != null && 
               !temperature.isNaN() && 
               !temperature.isInfinite() &&
               temperature > -273.15f // Above absolute zero
    }

    /**
     * Get temperature statistics from an array of values
     */
    data class TemperatureStats(
        val min: Float,
        val max: Float,
        val average: Float,
        val count: Int
    )

    fun getTemperatureStats(temperatures: List<Float>): TemperatureStats? {
        if (temperatures.isEmpty()) return null
        
        val validTemps = temperatures.filter { isValidTemperature(it) }
        if (validTemps.isEmpty()) return null
        
        return TemperatureStats(
            min = validTemps.minOrNull() ?: 0f,
            max = validTemps.maxOrNull() ?: 0f,
            average = validTemps.average().toFloat(),
            count = validTemps.size
        )
    }

    /**
     * Convert raw thermal data byte to temperature (simplified)
     */
    fun byteToTemperature(value: Byte): Float {
        // This is a simplified conversion - actual implementation would depend on camera calibration
        return value.toFloat() * 0.25f - 40f // Example scaling
    }

    /**
     * Convert temperature to raw thermal data byte (simplified)
     */
    fun temperatureToByte(celsius: Float): Byte {
        // Reverse of the above conversion
        val scaled = ((celsius + 40f) / 0.25f).roundToInt()
        return scaled.coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()
    }
}