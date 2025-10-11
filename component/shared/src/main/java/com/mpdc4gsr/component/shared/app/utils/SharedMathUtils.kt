package com.mpdc4gsr.component.shared.app.utils

import kotlin.math.pow

object SharedMathUtils {
    fun setDoubleAccuracy(
        num: Double,
        scale: Int,
    ): Double {
        val factor = 10.0.pow(scale)
        return (num * factor).toInt() / factor
    }

    fun getPercents(
        scale: Int,
        vararg values: Float,
    ): FloatArray {
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

    fun numberToBytes(
        bigEndian: Boolean,
        value: Long,
        len: Int,
    ): ByteArray {
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

    fun splitPackage(
        src: ByteArray,
        size: Int,
    ): List<ByteArray> {
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

    fun clamp(
        value: Int,
        min: Int,
        max: Int,
    ): Int =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun clamp(
        value: Float,
        min: Float,
        max: Float,
    ): Float =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun clamp(
        value: Double,
        min: Double,
        max: Double,
    ): Double =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun lerp(
        start: Float,
        end: Float,
        fraction: Float,
    ): Float = start + fraction * (end - start)

    fun inRange(
        value: Int,
        min: Int,
        max: Int,
    ): Boolean = value in min..max

    fun inRange(
        value: Float,
        min: Float,
        max: Float,
    ): Boolean = value in min..max

    fun roundToNearest(
        value: Int,
        multiple: Int,
    ): Int = ((value + multiple / 2) / multiple) * multiple

    fun average(values: IntArray): Double = if (values.isEmpty()) 0.0 else values.average()

    fun average(values: FloatArray): Double = if (values.isEmpty()) 0.0 else values.average()
}



