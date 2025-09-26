package com.mpdc4gsr.libunified.app.utils

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Consolidated math utilities replacing multiple math utility classes
 * Replaces:
 * - BleModule/src/main/java/com/topdon/commons/util/MathUtils.java (key functionality)
 * - Various math utility functions scattered across modules
 */
object UnifiedMathUtils {

    /**
     * Set double precision to specified decimal places without rounding
     */
    fun setDoubleAccuracy(num: Double, scale: Int): Double {
        val factor = 10.0.pow(scale)
        return (num * factor).toInt() / factor
    }

    /**
     * Calculate percentages that sum to 100%
     */
    fun getPercents(scale: Int, vararg values: Float): FloatArray {
        val total = values.sum()
        if (total == 0f) {
            return FloatArray(values.size) { 0f }
        }

        val result = FloatArray(values.size)
        val scaleFactor = 10.0.pow(scale + 2).toInt()
        var sum = 0f

        for (i in values.indices) {
            if (i == values.size - 1) {
                result[i] = 1f - sum
            } else {
                result[i] = ((values[i] / total * scaleFactor).toInt().toFloat() / scaleFactor)
                sum += result[i]
            }
        }
        return result
    }

    /**
     * Convert number to byte array
     * @param bigEndian true for big-endian (high byte first), false for little-endian
     * @param value the number to convert
     * @param len number of bytes to return
     */
    fun numberToBytes(bigEndian: Boolean, value: Long, len: Int): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0..7) {
            val j = if (bigEndian) 7 - i else i
            bytes[i] = (value shr (8 * j) and 0xff).toByte()
        }

        return if (len > 8) {
            bytes
        } else {
            val startIndex = if (bigEndian) 8 - len else 0
            val endIndex = if (bigEndian) 8 else len
            bytes.sliceArray(startIndex until endIndex)
        }
    }

    /**
     * Split byte array into smaller chunks
     */
    fun splitPackage(src: ByteArray, size: Int): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var offset = 0

        while (offset < src.size) {
            val chunkSize = minOf(size, src.size - offset)
            val chunk = ByteArray(chunkSize)
            System.arraycopy(src, offset, chunk, 0, chunkSize)
            result.add(chunk)
            offset += chunkSize
        }

        return result
    }

    /**
     * Join multiple byte arrays into one
     */
    fun joinPackage(vararg src: ByteArray): ByteArray {
        val totalSize = src.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0

        for (array in src) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }

        return result
    }

    /**
     * Clamp value between min and max
     */
    fun clamp(value: Int, min: Int, max: Int): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    /**
     * Clamp float value between min and max
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    /**
     * Clamp double value between min and max
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    /**
     * Linear interpolation between two values
     */
    fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + fraction * (end - start)
    }

    /**
     * Check if value is within range (inclusive)
     */
    fun inRange(value: Int, min: Int, max: Int): Boolean {
        return value in min..max
    }

    /**
     * Check if float value is within range (inclusive)
     */
    fun inRange(value: Float, min: Float, max: Float): Boolean {
        return value in min..max
    }

    /**
     * Round to nearest multiple
     */
    fun roundToNearest(value: Int, multiple: Int): Int {
        return ((value + multiple / 2) / multiple) * multiple
    }

    /**
     * Calculate average of array
     */
    fun average(values: IntArray): Double {
        return if (values.isEmpty()) 0.0 else values.average()
    }

    /**
     * Calculate average of float array
     */
    fun average(values: FloatArray): Double {
        return if (values.isEmpty()) 0.0 else values.average()
    }
}