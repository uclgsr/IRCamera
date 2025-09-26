package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Common utilities for application-wide operations
 */
object CommUtils {
    
    private const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss"
    
    fun getCurrentTimeString(): String {
        val formatter = SimpleDateFormat(DATE_FORMAT_DEFAULT, Locale.getDefault())
        return formatter.format(Date())
    }
    
    fun getAppStorageDir(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
    }
    
    fun createDirectory(dirPath: String): Boolean {
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }
    
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
    
    fun isValidString(str: String?): Boolean {
        return !str.isNullOrEmpty() && str.trim().isNotEmpty()
    }
    
    fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            ""
        }
    }
    
    fun generateUniqueFileName(prefix: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        return "${prefix}_${timestamp}.${extension}"
    }
    
    fun getAppName(): String {
        return "IRCamera"
    }
}