package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.os.StatFs
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object UnifiedSessionUtils {
    // Directory constants
    private const val SESSIONS_ROOT_DIR = "sessions"
    private const val RGB_SUBDIR = "RGB"
    private const val THERMAL_SUBDIR = "Thermal"
    private const val SHIMMER_SUBDIR = "Shimmer"
    private const val METADATA_SUBDIR = "metadata"

    // File constants
    const val RGB_VIDEO_FILE = "rgb_video.mp4"
    const val SHIMMER_DATA_FILE = "shimmer_data.csv"
    const val THERMAL_FRAMES_FILE = "thermal_frames.csv"
    const val THERMAL_METADATA_FILE = "thermal_metadata.csv"
    const val SESSION_INFO_FILE = "session_info.json"
    fun createSessionDirectory(context: Context, sessionName: String? = null): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dirName = sessionName?.let { "${it}_$timestamp" } ?: "session_$timestamp"
        val sessionDir = File(getSessionsRootDirectory(context), dirName)
        return createSessionDirectoryStructure(sessionDir)
    }

    private fun createSessionDirectoryStructure(sessionDir: File): File {
        sessionDir.mkdirs()
        // Create subdirectories
        File(sessionDir, RGB_SUBDIR).mkdirs()
        File(sessionDir, THERMAL_SUBDIR).mkdirs()
        File(sessionDir, SHIMMER_SUBDIR).mkdirs()
        File(sessionDir, METADATA_SUBDIR).mkdirs()
        return sessionDir
    }

    fun getSessionsRootDirectory(context: Context): File {
        val rootDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(rootDir, SESSIONS_ROOT_DIR).apply { mkdirs() }
    }

    fun getRGBDirectory(sessionDir: File): File {
        return File(sessionDir, RGB_SUBDIR).apply { mkdirs() }
    }

    fun getThermalDirectory(sessionDir: File): File {
        return File(sessionDir, THERMAL_SUBDIR).apply { mkdirs() }
    }

    fun getShimmerDirectory(sessionDir: File): File {
        return File(sessionDir, SHIMMER_SUBDIR).apply { mkdirs() }
    }

    fun getMetadataDirectory(sessionDir: File): File {
        return File(sessionDir, METADATA_SUBDIR).apply { mkdirs() }
    }

    fun createSessionInfo(
        sessionDir: File,
        sessionId: String,
        deviceId: String,
        additionalInfo: Map<String, Any> = emptyMap()
    ): File {
        val sessionInfo = JSONObject().apply {
            put("sessionId", sessionId)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
            put(
                "created",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            additionalInfo.forEach { (key, value) ->
                put(key, value)
            }
        }
        val infoFile = File(sessionDir, SESSION_INFO_FILE)
        infoFile.writeText(sessionInfo.toString(2))
        return infoFile
    }

    fun getAvailableStorageSpace(context: Context): Long {
        return try {
            val sessionDir = getSessionsRootDirectory(context)
            val stat = StatFs(sessionDir.absolutePath)
            stat.blockSizeLong * stat.availableBlocksLong
        } catch (e: Exception) {
            0L
        }
    }

    fun getTotalStorageSpace(context: Context): Long {
        return try {
            val sessionDir = getSessionsRootDirectory(context)
            val stat = StatFs(sessionDir.absolutePath)
            stat.blockSizeLong * stat.blockCountLong
        } catch (e: Exception) {
            0L
        }
    }

    fun cleanupOldSessions(context: Context, olderThanDays: Int): Int {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        val sessionsDir = getSessionsRootDirectory(context)
        var deletedCount = 0
        sessionsDir.listFiles()?.forEach { sessionDir ->
            if (sessionDir.isDirectory && sessionDir.lastModified() < cutoffTime) {
                if (sessionDir.deleteRecursively()) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    fun listSessionDirectories(context: Context): List<File> {
        val sessionsDir = getSessionsRootDirectory(context)
        return sessionsDir.listFiles()?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    fun validateSessionDirectory(sessionDir: File): Boolean {
        return sessionDir.exists() &&
                sessionDir.isDirectory &&
                File(sessionDir, RGB_SUBDIR).exists() &&
                File(sessionDir, THERMAL_SUBDIR).exists() &&
                File(sessionDir, SHIMMER_SUBDIR).exists()
    }
}