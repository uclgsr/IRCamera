package com.mpdc4gsr.libunified.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Consolidated data conversion and manipulation utilities
 * Replaces:
 * - libunified/src/main/java/com/mpdc4gsr/libunified/app/matrix/utils/BaseDataTypeConvertUtils.kt
 * - Various data conversion utilities scattered across modules
 */
object UnifiedDataUtils {

    /**
     * Convert InputStream to ByteArray
     */
    fun inputStreamToByteArray(inputStream: InputStream): ByteArray {
        return inputStream.use { it.readBytes() }
    }

    /**
     * Convert Bitmap to ByteArray
     */
    @JvmStatic
    fun bitmapToByteArray(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Convert int array to byte array (big endian)
     */
    fun intArrayToByteArray(intArray: IntArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(intArray.size * 4)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        for (value in intArray) {
            byteBuffer.putInt(value)
        }
        return byteBuffer.array()
    }

    /**
     * Convert byte array to int array (big endian)
     */
    fun byteArrayToIntArray(byteArray: ByteArray): IntArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val intArray = IntArray(byteArray.size / 4)
        for (i in intArray.indices) {
            intArray[i] = byteBuffer.int
        }
        return intArray
    }

    /**
     * Convert float array to byte array
     */
    fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        for (value in floatArray) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    /**
     * Convert byte array to float array
     */
    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val floatArray = FloatArray(byteArray.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = byteBuffer.float
        }
        return floatArray
    }

    /**
     * Convert short array to byte array
     */
    fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(shortArray.size * 2)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        for (value in shortArray) {
            byteBuffer.putShort(value)
        }
        return byteBuffer.array()
    }

    /**
     * Convert byte array to short array
     */
    fun byteArrayToShortArray(byteArray: ByteArray): ShortArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val shortArray = ShortArray(byteArray.size / 2)
        for (i in shortArray.indices) {
            shortArray[i] = byteBuffer.short
        }
        return shortArray
    }

    /**
     * Convert List<T> to Array
     */
    inline fun <reified T> listToArray(list: List<T>): Array<T> {
        return list.toTypedArray()
    }

    /**
     * Convert Array to List
     */
    fun <T> arrayToList(array: Array<T>): List<T> {
        return array.toList()
    }

    /**
     * Deep copy byte array
     */
    fun deepCopyByteArray(original: ByteArray): ByteArray {
        return original.copyOf()
    }

    /**
     * Concatenate multiple byte arrays
     */
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

    /**
     * Split byte array into chunks
     */
    fun splitByteArray(array: ByteArray, chunkSize: Int): List<ByteArray> {
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

    /**
     * Reverse byte array
     */
    fun reverseByteArray(array: ByteArray): ByteArray {
        return array.reversedArray()
    }

    /**
     * Check if two byte arrays are equal
     */
    fun byteArraysEqual(array1: ByteArray, array2: ByteArray): Boolean {
        return array1.contentEquals(array2)
    }

    /**
     * Convert data size to human readable format
     */
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

    /**
     * Scale bitmap with specified width and height
     */
    @JvmStatic
    fun scaleWithWH(bitmap: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap? {
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

    /**
     * Convert byte array to bitmap
     */
    @JvmStatic
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}