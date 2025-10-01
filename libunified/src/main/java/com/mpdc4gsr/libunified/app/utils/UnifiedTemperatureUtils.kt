package com.mpdc4gsr.libunified.app.utils

import android.graphics.Point
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Consolidated temperature and thermal utilities
 * Replaces:
 * - libunified/src/main/java/com/mpdc4gsr/libunified/ir/utils/TempUtils.kt
 * - Various temperature calculation utilities across modules
 */
object UnifiedTemperatureUtils {

    /**
     * Get temperature values along a line between two points
     */
    @JvmStatic
    fun getLineTemperatures(
        point1: Point,
        point2: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int
    ): List<Float> {
        if (point1 == point2) {
            return emptyList()
        }

        val points = getLinePoints(point1, point2)
        val temperatures = mutableListOf<Float>()

        for (point in points) {
            if (point.x >= 0 && point.x < width && point.y >= 0 && point.y < height) {
                val index = point.y * width + point.x
                if (index < temperatureArray.size) {
                    temperatures.add(byteToTemperature(temperatureArray[index]))
                }
            }
        }

        return temperatures
    }

    /**
     * Get temperature values within a rectangular region
     */
    fun getRectangleTemperatures(
        topLeft: Point,
        bottomRight: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int
    ): List<Float> {
        val temperatures = mutableListOf<Float>()

        val minX = max(0, min(topLeft.x, bottomRight.x))
        val maxX = min(width - 1, max(topLeft.x, bottomRight.x))
        val minY = max(0, min(topLeft.y, bottomRight.y))
        val maxY = min(height - 1, max(topLeft.y, bottomRight.y))

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val index = y * width + x
                if (index < temperatureArray.size) {
                    temperatures.add(byteToTemperature(temperatureArray[index]))
                }
            }
        }

        return temperatures
    }

    /**
     * Get temperature value at a specific point
     */
    fun getPointTemperature(
        point: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int
    ): Float? {
        if (point.x < 0 || point.x >= width || point.y < 0 || point.y >= height) {
            return null
        }

        val index = point.y * width + point.x
        return if (index < temperatureArray.size) {
            byteToTemperature(temperatureArray[index])
        } else {
            null
        }
    }

    /**
     * Find maximum temperature in region
     */
    fun findMaxTemperature(temperatures: List<Float>): Float? {
        return temperatures.maxOrNull()
    }

    /**
     * Find minimum temperature in region
     */
    fun findMinTemperature(temperatures: List<Float>): Float? {
        return temperatures.minOrNull()
    }

    /**
     * Calculate average temperature in region
     */
    fun calculateAverageTemperature(temperatures: List<Float>): Float {
        return if (temperatures.isEmpty()) 0f else temperatures.average().toFloat()
    }

    /**
     * Find hotspot (maximum temperature point) in region
     */
    fun findHotspot(
        topLeft: Point,
        bottomRight: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int
    ): Pair<Point, Float>? {
        var maxTemp = Float.MIN_VALUE
        var hotspotPoint: Point? = null

        val minX = max(0, min(topLeft.x, bottomRight.x))
        val maxX = min(width - 1, max(topLeft.x, bottomRight.x))
        val minY = max(0, min(topLeft.y, bottomRight.y))
        val maxY = min(height - 1, max(topLeft.y, bottomRight.y))

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val index = y * width + x
                if (index < temperatureArray.size) {
                    val temp = byteToTemperature(temperatureArray[index])
                    if (temp > maxTemp) {
                        maxTemp = temp
                        hotspotPoint = Point(x, y)
                    }
                }
            }
        }

        return hotspotPoint?.let { Pair(it, maxTemp) }
    }

    /**
     * Find coldspot (minimum temperature point) in region
     */
    fun findColdspot(
        topLeft: Point,
        bottomRight: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int
    ): Pair<Point, Float>? {
        var minTemp = Float.MAX_VALUE
        var coldspotPoint: Point? = null

        val minX = max(0, min(topLeft.x, bottomRight.x))
        val maxX = min(width - 1, max(topLeft.x, bottomRight.x))
        val minY = max(0, min(topLeft.y, bottomRight.y))
        val maxY = min(height - 1, max(topLeft.y, bottomRight.y))

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val index = y * width + x
                if (index < temperatureArray.size) {
                    val temp = byteToTemperature(temperatureArray[index])
                    if (temp < minTemp) {
                        minTemp = temp
                        coldspotPoint = Point(x, y)
                    }
                }
            }
        }

        return coldspotPoint?.let { Pair(it, minTemp) }
    }

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
     * Format temperature with unit
     */
    @JvmStatic
    fun formatTemperature(
        temperature: Float,
        unit: TemperatureUnit = TemperatureUnit.CELSIUS
    ): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "%.1f°C".format(temperature)
            TemperatureUnit.FAHRENHEIT -> "%.1f°F".format(celsiusToFahrenheit(temperature))
            TemperatureUnit.KELVIN -> "%.1f K".format(celsiusToKelvin(temperature))
        }
    }

    enum class TemperatureUnit {
        CELSIUS, FAHRENHEIT, KELVIN
    }

    /**
     * Convert byte value to temperature (implementation specific to thermal sensor)
     */
    private fun byteToTemperature(byte: Byte): Float {
        // This is a simplified conversion - actual implementation depends on sensor specs
        return byte.toFloat() / 10f
    }

    /**
     * Get all points along a line using Bresenham's algorithm
     */
    private fun getLinePoints(point1: Point, point2: Point): List<Point> {
        val points = mutableListOf<Point>()

        if (point1.x == point2.x) {
            // Vertical line
            val startY = min(point1.y, point2.y)
            val endY = max(point1.y, point2.y)
            for (y in startY..endY) {
                points.add(Point(point1.x, y))
            }
        } else if (point1.y == point2.y) {
            // Horizontal line
            val startX = min(point1.x, point2.x)
            val endX = max(point1.x, point2.x)
            for (x in startX..endX) {
                points.add(Point(x, point1.y))
            }
        } else {
            // Diagonal line - use Bresenham's algorithm
            val dx = abs(point2.x - point1.x)
            val dy = abs(point2.y - point1.y)
            val sx = if (point1.x < point2.x) 1 else -1
            val sy = if (point1.y < point2.y) 1 else -1
            var err = dx - dy

            var x = point1.x
            var y = point1.y

            while (true) {
                points.add(Point(x, y))

                if (x == point2.x && y == point2.y) break

                val e2 = 2 * err
                if (e2 > -dy) {
                    err -= dy
                    x += sx
                }
                if (e2 < dx) {
                    err += dx
                    y += sy
                }
            }
        }

        return points
    }
}