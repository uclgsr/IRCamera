package com.mpdc4gsr.libunified.app.utils

import android.graphics.Point
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object UnifiedTemperatureUtils {
    @JvmStatic
    fun getLineTemperatures(
        point1: Point,
        point2: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int,
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

    fun getRectangleTemperatures(
        topLeft: Point,
        bottomRight: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int,
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

    fun getPointTemperature(
        point: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int,
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

    fun findMaxTemperature(temperatures: List<Float>): Float? = temperatures.maxOrNull()

    fun findMinTemperature(temperatures: List<Float>): Float? = temperatures.minOrNull()

    fun calculateAverageTemperature(temperatures: List<Float>): Float =
        if (temperatures.isEmpty()) 0f else temperatures.average().toFloat()

    fun findHotspot(
        topLeft: Point,
        bottomRight: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int,
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

    fun findColdspot(
        topLeft: Point,
        bottomRight: Point,
        temperatureArray: ByteArray,
        width: Int,
        height: Int,
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

    fun celsiusToFahrenheit(celsius: Float): Float = celsius * 9f / 5f + 32f

    fun fahrenheitToCelsius(fahrenheit: Float): Float = (fahrenheit - 32f) * 5f / 9f

    fun celsiusToKelvin(celsius: Float): Float = celsius + 273.15f

    fun kelvinToCelsius(kelvin: Float): Float = kelvin - 273.15f

    @JvmStatic
    fun formatTemperature(
        temperature: Float,
        unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    ): String =
        when (unit) {
            TemperatureUnit.CELSIUS -> "%.1f°C".format(temperature)
            TemperatureUnit.FAHRENHEIT -> "%.1f°F".format(celsiusToFahrenheit(temperature))
            TemperatureUnit.KELVIN -> "%.1f K".format(celsiusToKelvin(temperature))
        }

    enum class TemperatureUnit {
        CELSIUS,
        FAHRENHEIT,
        KELVIN,
    }

    private fun byteToTemperature(byte: Byte): Float {
        // This is a simplified conversion - actual implementation depends on sensor specs
        return byte.toFloat() / 10f
    }

    private fun getLinePoints(
        point1: Point,
        point2: Point,
    ): List<Point> {
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
