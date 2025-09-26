package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.config.FileConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Image utility class for handling image operations
 * Based on existing directory structure and CommUtils integration
 */
object ImageUtils {

    private const val IMAGE_QUALITY_HIGH = 90
    private const val IMAGE_QUALITY_MEDIUM = 75
    private const val IMAGE_QUALITY_LOW = 60

    /**
     * Get line IR gallery directory (external storage based)
     */
    fun getLineIrGalleryDir(context: Context): File {
        // Use FileConfig's lineIrGalleryDir property
        return File(FileConfig.lineIrGalleryDir)
    }

    /**
     * Save bitmap to IR gallery directory
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String? = null,
        quality: Int = IMAGE_QUALITY_HIGH
    ): File? {
        return try {
            val galleryDir = getLineIrGalleryDir(context)
            val finalFilename = filename ?: CommUtils.generateUniqueFileName("image", "jpg")
            val imageFile = File(galleryDir, finalFilename)
            
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            // Scan the file to make it visible in gallery
            scanMediaFile(context, imageFile.absolutePath)
            
            XLog.i("Image saved to: ${imageFile.absolutePath}")
            imageFile
        } catch (e: IOException) {
            XLog.e("Failed to save image: ${e.message}")
            null
        }
    }

    /**
     * Save thermal image with timestamp
     */
    fun saveThermalImage(
        context: Context,
        bitmap: Bitmap,
        temperature: Float? = null
    ): File? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = if (temperature != null) {
            "thermal_${timestamp}_${temperature}C.jpg"
        } else {
            "thermal_$timestamp.jpg"
        }
        
        return saveBitmapToGallery(context, bitmap, filename)
    }

    /**
     * Save RGB image with timestamp
     */
    fun saveRgbImage(context: Context, bitmap: Bitmap): File? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "rgb_$timestamp.jpg"
        return saveBitmapToGallery(context, bitmap, filename)
    }

    /**
     * Load bitmap from file path
     */
    fun loadBitmapFromFile(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(filePath)
            } else {
                XLog.w("Image file does not exist: $filePath")
                null
            }
        } catch (e: Exception) {
            XLog.e("Failed to load bitmap from file: ${e.message}")
            null
        }
    }

    /**
     * Resize bitmap while maintaining aspect ratio
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return bitmap
        }
        
        val ratio = minOf(
            maxWidth.toFloat() / originalWidth,
            maxHeight.toFloat() / originalHeight
        )
        
        val newWidth = (originalWidth * ratio).toInt()
        val newHeight = (originalHeight * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Get image file size in bytes
     */
    fun getImageFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Delete image file
     */
    fun deleteImageFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true // Already deleted
            }
        } catch (e: Exception) {
            XLog.e("Failed to delete image file: ${e.message}")
            false
        }
    }

    /**
     * Get all images in gallery directory
     */
    fun getGalleryImages(context: Context): List<File> {
        val galleryDir = getLineIrGalleryDir(context)
        return try {
            galleryDir.listFiles { file ->
                file.isFile && isImageFile(file.name)
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            XLog.e("Failed to get gallery images: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if file is an image file
     */
    fun isImageFile(filename: String): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "webp")
        val extension = CommUtils.getFileExtension(filename).lowercase()
        return imageExtensions.contains(extension)
    }

    /**
     * Scan media file to make it visible in gallery
     */
    private fun scanMediaFile(context: Context, filePath: String) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                null
            ) { path, uri ->
                XLog.i("Media scan completed for: $path")
            }
        } catch (e: Exception) {
            XLog.w("Failed to scan media file: ${e.message}")
        }
    }

    /**
     * Get formatted file size string
     */
    fun getFormattedFileSize(filePath: String): String {
        val size = getImageFileSize(filePath)
        return CommUtils.formatFileSize(size)
    }

    /**
     * Create thumbnail bitmap
     */
    fun createThumbnail(bitmap: Bitmap, size: Int = 128): Bitmap {
        return resizeBitmap(bitmap, size, size)
    }

    /**
     * Get image dimensions without loading full bitmap
     */
    fun getImageDimensions(filePath: String): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            XLog.e("Failed to get image dimensions: ${e.message}")
            null
        }
    }
}