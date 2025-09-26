package com.mpdc4gsr.libunified.ir.utils

import android.graphics.Bitmap

/**
 * SupRUtils - Bitmap conversion utilities
 * Used by OpencvTools for bitmap/byte array conversions
 */
object SupRUtils {
    
    /**
     * Convert bitmap to byte array
     */
    @JvmStatic
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        return com.mpdc4gsr.libunified.app.utils.UnifiedDataUtils.bitmapToByteArray(bitmap)
    }
    
    /**
     * Convert byte array to bitmap
     */
    @JvmStatic
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}