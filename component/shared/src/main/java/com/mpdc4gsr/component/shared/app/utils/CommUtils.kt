package com.mpdc4gsr.component.shared.app.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import com.mpdc4gsr.component.shared.compat.ContextProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CommUtils {
    fun getAppName(): String {
        var msg = ""
        val context = ContextProvider.getContext()
        val appInfo: ApplicationInfo? =
            context.packageManager
                .getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA,
                )
        try {
            msg = appInfo?.metaData?.getString("app_name")?.toString() ?: ""
        } catch (e: Exception) {
        }
        return msg
    }

    // Additional compatibility methods
    private const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss"

    fun getCurrentTimeString(): String {
        val formatter = SimpleDateFormat(DATE_FORMAT_DEFAULT, Locale.getDefault())
        return formatter.format(Date())
    }

    fun getAppStorageDir(context: Context): File =
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir

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
        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups],
        )
    }

    fun isValidString(str: String?): Boolean = !str.isNullOrEmpty() && str.trim().isNotEmpty()

    fun getFileExtension(fileName: String): String =
        if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            ""
        }

    fun generateUniqueFileName(
        prefix: String,
        extension: String,
    ): String {
        val timestamp = System.currentTimeMillis()
        return "${prefix}_$timestamp.$extension"
    }
}


