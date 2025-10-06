package com.mpdc4gsr.libunified.app.utils

import com.mpdc4gsr.libunified.app.common.SharedManager

object TemperatureUtils {
    private const val CELSIUS_TO_FAHRENHEIT_MULTIPLIER = 1.8
    private const val CELSIUS_TO_FAHRENHEIT_OFFSET = 32
    fun celsiusToFahrenheit(temp: Int): Int {
        return (temp * CELSIUS_TO_FAHRENHEIT_MULTIPLIER + CELSIUS_TO_FAHRENHEIT_OFFSET).toInt()
    }

    fun getTempStr(min: Int, max: Int): String = if (SharedManager.getTemperature() == 1) {
        "${min}°C~${max}°C"
    } else {
        "${celsiusToFahrenheit(min)}°F~${celsiusToFahrenheit(max)}°F"
    }
}