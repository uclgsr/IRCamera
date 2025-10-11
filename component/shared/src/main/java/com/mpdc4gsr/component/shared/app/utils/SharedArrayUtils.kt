package com.mpdc4gsr.component.shared.app.utils

object SharedArrayUtils {
    fun getMaxIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int =
        when (rotateType) {
            1, 2, 3 -> getRotateMaxIndex(data, rotateType, selectIndexList)
            else -> getMaxIndex(data, selectIndexList)
        }

    fun getMinIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int =
        when (rotateType) {
            1, 2, 3 -> getRotateMinIndex(data, rotateType, selectIndexList)
            else -> getMinIndex(data, selectIndexList)
        }

    private fun getMaxIndex(
        data: FloatArray,
        selectIndexList: ArrayList<Int>,
    ): Int {
        if (data.isEmpty()) return -1
        var maxIndex = 0
        var maxValue = Float.MIN_VALUE
        for (i in data.indices) {
            if (selectIndexList.isNotEmpty() && !selectIndexList.contains(i)) continue
            if (data[i] > maxValue) {
                maxValue = data[i]
                maxIndex = i
            }
        }
        return maxIndex
    }

    private fun getMinIndex(
        data: FloatArray,
        selectIndexList: ArrayList<Int>,
    ): Int {
        if (data.isEmpty()) return -1
        var minIndex = 0
        var minValue = Float.MAX_VALUE
        for (i in data.indices) {
            if (selectIndexList.isNotEmpty() && !selectIndexList.contains(i)) continue
            if (data[i] < minValue) {
                minValue = data[i]
                minIndex = i
            }
        }
        return minIndex
    }

    private fun getRotateMaxIndex(
        data: FloatArray,
        rotateType: Int,
        selectIndexList: ArrayList<Int>,
    ): Int {
        val maxIndex = getMaxIndex(data, selectIndexList)
        return rotateIndex(maxIndex, data.size, rotateType)
    }

    private fun getRotateMinIndex(
        data: FloatArray,
        rotateType: Int,
        selectIndexList: ArrayList<Int>,
    ): Int {
        val minIndex = getMinIndex(data, selectIndexList)
        return rotateIndex(minIndex, data.size, rotateType)
    }

    private fun rotateIndex(
        index: Int,
        arraySize: Int,
        rotateType: Int,
        width: Int = 256,
        height: Int = 192,
    ): Int {
        // Support for thermal data arrays (typically 256x192 for IR cameras)
        val actualWidth =
            if (width * height == arraySize) {
                width
            } else {
                kotlin.math
                    .sqrt(arraySize.toDouble())
                    .toInt()
            }
        val actualHeight = if (width * height == arraySize) height else arraySize / actualWidth
        if (actualWidth * actualHeight != arraySize) return index
        val x = index % width
        val y = index / width
        val (newX, newY, newWidth) =
            when (rotateType) {
                1 ->
                    Triple(
                        height - 1 - y,
                        x,
                        height,
                    ) // 90 degrees clockwise, width and height swapped
                2 -> Triple(width - 1 - x, height - 1 - y, width) // 180 degrees, dimensions unchanged
                3 ->
                    Triple(
                        y,
                        width - 1 - x,
                        height,
                    ) // 270 degrees clockwise, width and height swapped
                else -> Triple(x, y, width) // No rotation
            }
        return newY * newWidth + newX
    }

    fun findAllMaxIndices(data: FloatArray): List<Int> {
        if (data.isEmpty()) return emptyList()
        val maxValue = data.maxOrNull() ?: return emptyList()
        return data.indices.filter { data[it] == maxValue }
    }

    fun findAllMinIndices(data: FloatArray): List<Int> {
        if (data.isEmpty()) return emptyList()
        val minValue = data.minOrNull() ?: return emptyList()
        return data.indices.filter { data[it] == minValue }
    }

    fun getIndicesInRange(
        data: FloatArray,
        minValue: Float,
        maxValue: Float,
    ): List<Int> =
        data.indices.filter {
            data[it] in minValue..maxValue
        }

    data class ArrayStats(
        val min: Float,
        val max: Float,
        val mean: Float,
        val median: Float,
        val standardDeviation: Float,
    )

    fun calculateStats(data: FloatArray): ArrayStats? {
        if (data.isEmpty()) return null
        val sorted = data.sorted()
        val min = sorted.first()
        val max = sorted.last()
        val mean = data.average().toFloat()
        val median =
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
            } else {
                sorted[sorted.size / 2]
            }
        val variance = data.map { (it - mean) * (it - mean) }.average().toFloat()
        val standardDeviation = kotlin.math.sqrt(variance)
        return ArrayStats(min, max, mean, median, standardDeviation)
    }

    fun applyGaussianFilter(
        data: FloatArray,
        width: Int,
        height: Int,
        sigma: Float = 1.0f,
    ): FloatArray {
        if (width * height != data.size) return data.copyOf()
        val result = data.copyOf()
        val kernelSize = (6 * sigma).toInt() or 1 // Ensure odd size
        val kernel = generateGaussianKernel(kernelSize, sigma)
        // Apply horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var weightSum = 0f
                for (kx in -kernelSize / 2..kernelSize / 2) {
                    val nx = x + kx
                    if (nx in 0 until width) {
                        val weight = kernel[kx + kernelSize / 2]
                        sum += data[y * width + nx] * weight
                        weightSum += weight
                    }
                }
                result[y * width + x] = sum / weightSum
            }
        }
        // Apply vertical pass
        val temp = result.copyOf()
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var weightSum = 0f
                for (ky in -kernelSize / 2..kernelSize / 2) {
                    val ny = y + ky
                    if (ny in 0 until height) {
                        val weight = kernel[ky + kernelSize / 2]
                        sum += temp[ny * width + x] * weight
                        weightSum += weight
                    }
                }
                result[y * width + x] = sum / weightSum
            }
        }
        return result
    }

    private fun generateGaussianKernel(
        size: Int,
        sigma: Float,
    ): FloatArray {
        val kernel = FloatArray(size)
        val center = size / 2
        var sum = 0f
        for (i in 0 until size) {
            val x = i - center
            kernel[i] = kotlin.math.exp(-(x * x) / (2 * sigma * sigma)).toFloat()
            sum += kernel[i]
        }
        // Normalize
        for (i in 0 until size) {
            kernel[i] /= sum
        }
        return kernel
    }

    fun downsample(
        data: FloatArray,
        width: Int,
        height: Int,
        factor: Int,
    ): FloatArray {
        val newWidth = width / factor
        val newHeight = height / factor
        val result = FloatArray(newWidth * newHeight)
        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                var sum = 0f
                var count = 0
                for (dy in 0 until factor) {
                    for (dx in 0 until factor) {
                        val sx = x * factor + dx
                        val sy = y * factor + dy
                        if (sx < width && sy < height) {
                            sum += data[sy * width + sx]
                            count++
                        }
                    }
                }
                result[y * newWidth + x] = if (count > 0) sum / count else 0f
            }
        }
        return result
    }

    fun normalize(data: FloatArray): FloatArray {
        if (data.isEmpty()) return data.copyOf()
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 0f
        val range = max - min
        return if (range > 0) {
            data.map { (it - min) / range }.toFloatArray()
        } else {
            FloatArray(data.size) { 0.5f }
        }
    }
}



