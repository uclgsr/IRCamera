package com.mpdc4gsr.component.shared.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object SharedDataUtils {
    fun inputStreamToByteArray(inputStream: InputStream): ByteArray = inputStream.use { it.readBytes() }

    @JvmStatic
    fun bitmapToByteArray(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return outputStream.toByteArray()
    }

    fun intArrayToByteArray(intArray: IntArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(intArray.size * 4)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        for (value in intArray) {
            byteBuffer.putInt(value)
        }
        return byteBuffer.array()
    }

    fun byteArrayToIntArray(byteArray: ByteArray): IntArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val intArray = IntArray(byteArray.size / 4)
        for (i in intArray.indices) {
            intArray[i] = byteBuffer.int
        }
        return intArray
    }

    fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        for (value in floatArray) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val floatArray = FloatArray(byteArray.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = byteBuffer.float
        }
        return floatArray
    }

    fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(shortArray.size * 2)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        for (value in shortArray) {
            byteBuffer.putShort(value)
        }
        return byteBuffer.array()
    }

    fun byteArrayToShortArray(byteArray: ByteArray): ShortArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val shortArray = ShortArray(byteArray.size / 2)
        for (i in shortArray.indices) {
            shortArray[i] = byteBuffer.short
        }
        return shortArray
    }

    inline fun <reified T> listToArray(list: List<T>): Array<T> = list.toTypedArray()

    fun <T> arrayToList(array: Array<T>): List<T> = array.toList()

    fun deepCopyByteArray(original: ByteArray): ByteArray = original.copyOf()

    fun concatenateByteArrays(vararg arrays: ByteArray): ByteArray {
        val totalLength = arrays.sumOf { it.size }
        val result = ByteArray(totalLength)
        var offset = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }
        return result
    }

    fun splitByteArray(
        array: ByteArray,
        chunkSize: Int,
    ): List<ByteArray> {
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < array.size) {
            val size = minOf(chunkSize, array.size - offset)
            val chunk = ByteArray(size)
            System.arraycopy(array, offset, chunk, 0, size)
            chunks.add(chunk)
            offset += size
        }
        return chunks
    }

    fun reverseByteArray(array: ByteArray): ByteArray = array.reversedArray()

    fun byteArraysEqual(
        array1: ByteArray,
        array2: ByteArray,
    ): Boolean = array1.contentEquals(array2)

    fun formatDataSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", size, units[unitIndex])
    }

    @JvmStatic
    fun scaleWithWH(
        bitmap: Bitmap?,
        targetWidth: Int,
        targetHeight: Int,
    ): Bitmap? {
        if (bitmap == null || bitmap.isRecycled) return null
        val scaleX = targetWidth.toFloat() / bitmap.width
        val scaleY = targetHeight.toFloat() / bitmap.height
        val matrix = Matrix()
        matrix.postScale(scaleX, scaleY)
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    @JvmStatic
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? =
        try {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
}



