package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.*

/**
 * Consolidated file utilities replacing multiple FileUtils classes
 * Replaces:
 * - libunified/src/main/java/com/mpdc4gsr/libunified/app/matrix/utils/FileUtils.kt
 * - libunified/src/main/java/com/mpdc4gsr/libunified/ui/utils/FileUtils.java
 */
object UnifiedFileUtils {

    /**
     * Check if file exists and is a file (not directory)
     */
    fun isFileExist(filePath: String): Boolean {
        if (UnifiedStringUtils.isBlank(filePath)) {
            return false
        }
        val file = File(filePath)
        return file.exists() && file.isFile
    }

    /**
     * Check if directory exists
     */
    fun isDirectoryExist(dirPath: String): Boolean {
        if (UnifiedStringUtils.isBlank(dirPath)) {
            return false
        }
        val dir = File(dirPath)
        return dir.exists() && dir.isDirectory
    }

    /**
     * Create directory if it doesn't exist
     */
    fun createDirectory(dirPath: String): Boolean {
        if (UnifiedStringUtils.isBlank(dirPath)) {
            return false
        }
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }

    /**
     * Delete directory and all its contents
     */
    fun deleteDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            deleteRecursively(dir)
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                if (!deleteRecursively(child)) {
                    return false
                }
            }
        }
        return file.delete()
    }

    /**
     * Delete file
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file size in bytes
     */
    fun getFileSize(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Read text file content
     */
    fun readTextFile(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write text to file
     */
    fun writeTextFile(filePath: String, content: String): Boolean {
        return try {
            File(filePath).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Append text to file
     */
    fun appendTextFile(filePath: String, content: String): Boolean {
        return try {
            File(filePath).appendText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copy file from source to destination
     */
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            
            // Create parent directories if they don't exist
            destFile.parentFile?.mkdirs()
            
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get external storage directory
     */
    fun getExternalStorageDirectory(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    /**
     * Get app-specific external files directory
     */
    fun getAppExternalFilesDir(context: Context, type: String? = null): String? {
        return context.getExternalFilesDir(type)?.absolutePath
    }

    /**
     * Save bitmap to file
     */
    fun saveBitmapToFile(bitmap: Bitmap, filePath: String, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { fos ->
                bitmap.compress(format, quality, fos)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}