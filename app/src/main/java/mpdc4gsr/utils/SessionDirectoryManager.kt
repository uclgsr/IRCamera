package mpdc4gsr.util

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class SessionDirectoryManager(private val context: Context) {

    companion object {
        private const val TAG = "SessionDirectoryManager"

        
        private const val SESSIONS_ROOT_DIR = "sessions"
        private const val RGB_SUBDIR = "RGB"
        private const val THERMAL_SUBDIR = "Thermal"
        private const val SHIMMER_SUBDIR = "Shimmer"

        
        const val RGB_VIDEO_FILE = "rgb_video.mp4"
        const val SHIMMER_DATA_FILE = "shimmer_data.csv"
        const val THERMAL_FRAMES_FILE = "thermal_frames.csv"
        const val THERMAL_METADATA_FILE = "thermal_metadata.csv"
        const val SESSION_METADATA_FILE = "session_metadata.json"
        const val SYNC_MARKERS_FILE = "sync_markers.csv"

        
        private const val MIN_FREE_SPACE_MB = 500L 
        private const val WARNING_FREE_SPACE_MB = 1000L 

        
        private val SESSION_ID_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
    }

    private val baseDirectory: File by lazy {
        File(context.getExternalFilesDir(null), SESSIONS_ROOT_DIR).also {
            it.mkdirs()
        }
    }

    
    fun generateSessionId(): String {
        val timestamp = SESSION_ID_FORMAT.format(Date())
        val deviceModel = Build.MODEL.replace(Regex("[^a-zA-Z0-9]"), "")
        val uuid = UUID.randomUUID().toString().take(8)
        return "${timestamp}_${deviceModel}_${uuid}"
    }

    
    fun createSessionDirectory(sessionId: String): SessionDirectory {
        val sessionDir = File(baseDirectory, sessionId)

        
        if (!sessionDir.mkdirs() && !sessionDir.exists()) {
            throw IllegalStateException("Failed to create session directory: ${sessionDir.absolutePath}")
        }

        
        val rgbDir = File(sessionDir, RGB_SUBDIR).also { it.mkdirs() }
        val thermalDir = File(sessionDir, THERMAL_SUBDIR).also { it.mkdirs() }
        val shimmerDir = File(sessionDir, SHIMMER_SUBDIR).also { it.mkdirs() }

        Log.i(TAG, "Created session directory structure: $sessionId")

        return SessionDirectory(
            sessionId = sessionId,
            rootDir = sessionDir,
            rgbDir = rgbDir,
            thermalDir = thermalDir,
            shimmerDir = shimmerDir
        )
    }

    
    fun createSessionMetadata(sessionDir: SessionDirectory, metadata: SessionMetadata): File {
        val metadataFile = File(sessionDir.rootDir, SESSION_METADATA_FILE)

        val jsonMetadata = JSONObject().apply {
            put("session_id", sessionDir.sessionId)
            put("start_time", metadata.startTime)
            put("device_model", Build.MODEL)
            put("device_manufacturer", Build.MANUFACTURER)
            put("app_version", getAppVersion())
            put("enabled_sensors", metadata.enabledSensors)
            put("participant_id", metadata.participantId ?: "")
            put("study_name", metadata.studyName ?: "")
            put("status", "ACTIVE")
            put("metadata", JSONObject(metadata.customMetadata))
        }

        metadataFile.writeText(jsonMetadata.toString(2))
        Log.i(TAG, "Created session metadata: ${metadataFile.absolutePath}")

        return metadataFile
    }

    
    fun updateSessionMetadata(
        sessionDir: SessionDirectory,
        endTime: Long,
        status: String,
        errors: Map<String, String> = emptyMap()
    ) {
        val metadataFile = File(sessionDir.rootDir, SESSION_METADATA_FILE)

        if (metadataFile.exists()) {
            try {
                val jsonMetadata = JSONObject(metadataFile.readText())
                jsonMetadata.put("end_time", endTime)
                jsonMetadata.put("status", status)
                jsonMetadata.put("duration_ms", endTime - jsonMetadata.getLong("start_time"))

                if (errors.isNotEmpty()) {
                    jsonMetadata.put("errors", JSONObject(errors))
                }

                
                val filesInfo = getSessionFilesInfo(sessionDir)
                jsonMetadata.put("files", JSONObject(filesInfo))

                metadataFile.writeText(jsonMetadata.toString(2))
                Log.i(TAG, "Updated session metadata: ${sessionDir.sessionId}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update session metadata", e)
            }
        }
    }

    
    fun checkStorageSpace(): StorageStatus {
        val stat = StatFs(baseDirectory.absolutePath)
        val availableBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        val availableMB = availableBytes / (1024 * 1024)

        return StorageStatus(
            availableMB = availableMB,
            totalMB = totalBytes / (1024 * 1024),
            isLowStorage = availableMB < MIN_FREE_SPACE_MB,
            shouldWarn = availableMB < WARNING_FREE_SPACE_MB
        )
    }

    
    fun cleanupFailedSessions(): List<String> {
        val cleanedSessions = mutableListOf<String>()

        baseDirectory.listFiles()?.forEach { sessionDir ->
            if (sessionDir.isDirectory && isFailedSession(sessionDir)) {
                try {
                    sessionDir.deleteRecursively()
                    cleanedSessions.add(sessionDir.name)
                    Log.i(TAG, "Cleaned up failed session: ${sessionDir.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cleanup session: ${sessionDir.name}", e)
                }
            }
        }

        return cleanedSessions
    }

    
    private fun getSessionFilesInfo(sessionDir: SessionDirectory): Map<String, Any> {
        val filesInfo = mutableMapOf<String, Any>()

        
        val rgbVideo = File(sessionDir.rgbDir, RGB_VIDEO_FILE)
        val shimmerData = File(sessionDir.shimmerDir, SHIMMER_DATA_FILE)
        val thermalFrames = File(sessionDir.thermalDir, THERMAL_FRAMES_FILE)
        val syncMarkers = File(sessionDir.rootDir, SYNC_MARKERS_FILE)

        filesInfo["rgb_video"] = mapOf(
            "exists" to rgbVideo.exists(),
            "size_bytes" to if (rgbVideo.exists()) rgbVideo.length() else 0,
            "path" to rgbVideo.absolutePath
        )

        filesInfo["shimmer_data"] = mapOf(
            "exists" to shimmerData.exists(),
            "size_bytes" to if (shimmerData.exists()) shimmerData.length() else 0,
            "path" to shimmerData.absolutePath
        )

        filesInfo["thermal_frames"] = mapOf(
            "exists" to thermalFrames.exists(),
            "size_bytes" to if (thermalFrames.exists()) thermalFrames.length() else 0,
            "path" to thermalFrames.absolutePath
        )

        filesInfo["sync_markers"] = mapOf(
            "exists" to syncMarkers.exists(),
            "size_bytes" to if (syncMarkers.exists()) syncMarkers.length() else 0,
            "path" to syncMarkers.absolutePath
        )

        return filesInfo
    }

    
    private fun isFailedSession(sessionDir: File): Boolean {
        val metadataFile = File(sessionDir, SESSION_METADATA_FILE)

        
        if (!metadataFile.exists()) {
            
            val totalSize = sessionDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            return totalSize < 1024 
        }

        try {
            val metadata = JSONObject(metadataFile.readText())
            val status = metadata.optString("status", "")

            
            if (status == "FAILED" || status == "ERROR") {
                val hasDataFiles = sessionDir.walkTopDown()
                    .filter { it.isFile && it.name != SESSION_METADATA_FILE }
                    .any { it.length() > 10240 } 

                return !hasDataFiles
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse metadata for session ${sessionDir.name}", e)
            return false
        }

        return false
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    
    fun getStandardFilePath(sessionDir: SessionDirectory, sensor: String, fileName: String): File {
        val sensorDir = when (sensor.lowercase()) {
            "rgb", "camera", "rgbcamera" -> sessionDir.rgbDir
            "thermal", "thermalcamera" -> sessionDir.thermalDir
            "gsr", "shimmer", "shimmer3" -> sessionDir.shimmerDir
            else -> sessionDir.rootDir
        }
        return File(sensorDir, fileName)
    }
}

data class SessionDirectory(
    val sessionId: String,
    val rootDir: File,
    val rgbDir: File,
    val thermalDir: File,
    val shimmerDir: File
)

data class SessionMetadata(
    val startTime: Long,
    val enabledSensors: List<String>,
    val participantId: String? = null,
    val studyName: String? = null,
    val customMetadata: Map<String, Any> = emptyMap()
)

data class StorageStatus(
    val availableMB: Long,
    val totalMB: Long,
    val isLowStorage: Boolean,
    val shouldWarn: Boolean
) {
    val usagePercentage: Int
        get() = if (totalMB > 0) ((totalMB - availableMB) * 100 / totalMB).toInt() else 0

    val formattedAvailable: String
        get() = if (availableMB > 1024) {
            String.format("%.1f GB", availableMB / 1024.0)
        } else {
            "$availableMB MB"
        }
}
