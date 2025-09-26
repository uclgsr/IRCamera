package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.os.StatFs
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Consolidated session and directory management utilities
 * Replaces:
 * - app/src/main/java/mpdc4gsr/utils/SessionDirectoryManager.kt
 * - Various session management utilities scattered across modules
 */
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

    /**
     * Create a new session directory with timestamp
     */
    fun createSessionDirectory(context: Context, sessionName: String? = null): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dirName = sessionName?.let { "${it}_$timestamp" } ?: "session_$timestamp"

        val sessionDir = File(getSessionsRootDirectory(context), dirName)
        return createSessionDirectoryStructure(sessionDir)
    }

    /**
     * Create complete session directory structure
     */
    private fun createSessionDirectoryStructure(sessionDir: File): File {
        sessionDir.mkdirs()

        // Create subdirectories
        File(sessionDir, RGB_SUBDIR).mkdirs()
        File(sessionDir, THERMAL_SUBDIR).mkdirs()
        File(sessionDir, SHIMMER_SUBDIR).mkdirs()
        File(sessionDir, METADATA_SUBDIR).mkdirs()

        return sessionDir
    }

    /**
     * Get sessions root directory
     */
    fun getSessionsRootDirectory(context: Context): File {
        val rootDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(rootDir, SESSIONS_ROOT_DIR).apply { mkdirs() }
    }

    /**
     * Get RGB directory for session
     */
    fun getRGBDirectory(sessionDir: File): File {
        return File(sessionDir, RGB_SUBDIR).apply { mkdirs() }
    }

    /**
     * Get thermal directory for session
     */
    fun getThermalDirectory(sessionDir: File): File {
        return File(sessionDir, THERMAL_SUBDIR).apply { mkdirs() }
    }

    /**
     * Get Shimmer directory for session
     */
    fun getShimmerDirectory(sessionDir: File): File {
        return File(sessionDir, SHIMMER_SUBDIR).apply { mkdirs() }
    }

    /**
     * Get metadata directory for session
     */
    fun getMetadataDirectory(sessionDir: File): File {
        return File(sessionDir, METADATA_SUBDIR).apply { mkdirs() }
    }

    /**
     * Create session info file
     */
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

    /**
     * Get available storage space in bytes
     */
    fun getAvailableStorageSpace(context: Context): Long {
        return try {
            val sessionDir = getSessionsRootDirectory(context)
            val stat = StatFs(sessionDir.absolutePath)
            stat.blockSizeLong * stat.availableBlocksLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get total storage space in bytes
     */
    fun getTotalStorageSpace(context: Context): Long {
        return try {
            val sessionDir = getSessionsRootDirectory(context)
            val stat = StatFs(sessionDir.absolutePath)
            stat.blockSizeLong * stat.blockCountLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Clean up old sessions (older than specified days)
     */
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

    /**
     * List all session directories
     */
    fun listSessionDirectories(context: Context): List<File> {
        val sessionsDir = getSessionsRootDirectory(context)
        return sessionsDir.listFiles()?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Generate unique session ID
     */
    fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Validate session directory structure
     */
    fun validateSessionDirectory(sessionDir: File): Boolean {
        return sessionDir.exists() &&
                sessionDir.isDirectory &&
                File(sessionDir, RGB_SUBDIR).exists() &&
                File(sessionDir, THERMAL_SUBDIR).exists() &&
                File(sessionDir, SHIMMER_SUBDIR).exists()
    }
}