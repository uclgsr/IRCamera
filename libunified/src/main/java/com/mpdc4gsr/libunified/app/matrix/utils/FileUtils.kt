package com.mpdc4gsr.libunified.app.matrix.utils

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * File utilities for matrix operations
 */
class FileUtils {
    companion object {
        
        /**
         * Save byte array to file
         */
        fun saveFile(data: ByteArray?, filePath: String, append: Boolean): Boolean {
            if (data == null) return false
            
            return try {
                val file = File(filePath)
                file.parentFile?.mkdirs()
                
                FileOutputStream(file, append).use { fos ->
                    fos.write(data)
                    fos.flush()
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
        
        /**
         * Save bitmap as JPEG file
         */
        fun saveBitmap2JpegFile(bitmap: Bitmap?, filePath: String): Boolean {
            if (bitmap == null) return false
            
            return try {
                val file = File(filePath)
                file.parentFile?.mkdirs()
                
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    fos.flush()
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }
}