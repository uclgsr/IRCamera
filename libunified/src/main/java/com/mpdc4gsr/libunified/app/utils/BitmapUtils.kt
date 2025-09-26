package com.mpdc4gsr.libunified.app.utils

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * BitmapUtils - Bitmap manipulation utilities
 * Maps to UnifiedDataUtils and other bitmap functionality
 */
object BitmapUtils {
    
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
}