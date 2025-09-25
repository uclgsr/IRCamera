package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import java.io.File

/**
 * Consolidated directory management utilities for standardized file organization
 * Provides centralized directory structure definitions and management
 */
object UnifiedDirectoryUtils {

    // Root directory constants
    private const val APP_ROOT_DIR = "IRCamera"
    
    // Feature-based directory structure
    private const val RECORDINGS_DIR = "recordings"
    private const val THERMAL_DIR = "thermal"
    private const val RGB_DIR = "rgb"
    private const val GSR_DIR = "gsr"
    private const val SESSIONS_DIR = "sessions"
    private const val EXPORTS_DIR = "exports"
    private const val CACHE_DIR = "cache"
    private const val LOGS_DIR = "logs"
    private const val CONFIG_DIR = "config"
    private const val TEMP_DIR = "temp"

    /**
     * Get app root directory
     */
    fun getAppRootDirectory(context: Context): File {
        val rootDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(rootDir, APP_ROOT_DIR).apply { mkdirs() }
    }

    /**
     * Get recordings root directory
     */
    fun getRecordingsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), RECORDINGS_DIR).apply { mkdirs() }
    }

    /**
     * Get thermal data directory
     */
    fun getThermalDirectory(context: Context): File {
        return File(getRecordingsDirectory(context), THERMAL_DIR).apply { mkdirs() }
    }

    /**
     * Get RGB data directory
     */
    fun getRGBDirectory(context: Context): File {
        return File(getRecordingsDirectory(context), RGB_DIR).apply { mkdirs() }
    }

    /**
     * Get GSR data directory
     */
    fun getGSRDirectory(context: Context): File {
        return File(getRecordingsDirectory(context), GSR_DIR).apply { mkdirs() }
    }

    /**
     * Get sessions directory
     */
    fun getSessionsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), SESSIONS_DIR).apply { mkdirs() }
    }

    /**
     * Get exports directory
     */
    fun getExportsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), EXPORTS_DIR).apply { mkdirs() }
    }

    /**
     * Get cache directory
     */
    fun getCacheDirectory(context: Context): File {
        return File(getAppRootDirectory(context), CACHE_DIR).apply { mkdirs() }
    }

    /**
     * Get logs directory
     */
    fun getLogsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), LOGS_DIR).apply { mkdirs() }
    }

    /**
     * Get configuration directory
     */
    fun getConfigDirectory(context: Context): File {
        return File(getAppRootDirectory(context), CONFIG_DIR).apply { mkdirs() }
    }

    /**
     * Get temporary files directory
     */
    fun getTempDirectory(context: Context): File {
        return File(getAppRootDirectory(context), TEMP_DIR).apply { mkdirs() }
    }

    /**
     * Get feature-specific directory
     */
    fun getFeatureDirectory(context: Context, featureName: String): File {
        return File(getAppRootDirectory(context), featureName.lowercase()).apply { mkdirs() }
    }

    /**
     * Clean temporary directory
     */
    fun cleanTempDirectory(context: Context): Boolean {
        return try {
            val tempDir = getTempDirectory(context)
            tempDir.deleteRecursively()
            tempDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean cache directory
     */
    fun cleanCacheDirectory(context: Context): Boolean {
        return try {
            val cacheDir = getCacheDirectory(context)
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get directory size in bytes
     */
    fun getDirectorySize(directory: File): Long {
        if (!directory.exists() || !directory.isDirectory) return 0L
        
        var size = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    /**
     * Get formatted directory size
     */
    fun getFormattedDirectorySize(directory: File): String {
        val size = getDirectorySize(directory)
        return UnifiedDataUtils.formatDataSize(size)
    }

    /**
     * Ensure all app directories exist
     */
    fun ensureAppDirectoriesExist(context: Context) {
        getAppRootDirectory(context)
        getRecordingsDirectory(context)
        getThermalDirectory(context)
        getRGBDirectory(context)
        getGSRDirectory(context)
        getSessionsDirectory(context)
        getExportsDirectory(context)
        getCacheDirectory(context)
        getLogsDirectory(context)
        getConfigDirectory(context)
        getTempDirectory(context)
    }

    /**
     * Get all app directories info
     */
    data class DirectoryInfo(
        val name: String,
        val path: String,
        val size: Long,
        val fileCount: Int,
        val exists: Boolean
    )

    fun getAllDirectoriesInfo(context: Context): List<DirectoryInfo> {
        val directories = listOf(
            "Root" to getAppRootDirectory(context),
            "Recordings" to getRecordingsDirectory(context),
            "Thermal" to getThermalDirectory(context),
            "RGB" to getRGBDirectory(context),
            "GSR" to getGSRDirectory(context),
            "Sessions" to getSessionsDirectory(context),
            "Exports" to getExportsDirectory(context),
            "Cache" to getCacheDirectory(context),
            "Logs" to getLogsDirectory(context),
            "Config" to getConfigDirectory(context),
            "Temp" to getTempDirectory(context)
        )

        return directories.map { (name, dir) ->
            DirectoryInfo(
                name = name,
                path = dir.absolutePath,
                size = getDirectorySize(dir),
                fileCount = dir.listFiles()?.size ?: 0,
                exists = dir.exists()
            )
        }
    }
}