package mpdc4gsr.feature.network.data

import android.content.Context
import mpdc4gsr.core.StructuredLogger
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class DataManagementService(private val context: Context) {
    companion object {        private const val BASE_DIR = "IRCamera_Data"
        private const val SESSIONS_DIR = "sessions"
        private const val TEMP_DIR = "temp"
        private const val ARCHIVE_DIR = "archive"
        private const val EXPORTS_DIR = "exports"
        private const val METADATA_FILE = "session_metadata.json"
        private const val MANIFEST_FILE = "file_manifest.json"

        enum class ExportFormat {
            JSON,
            CSV,
            HDF5,
            ZIP,
        }

        enum class SessionStatus {
            ACTIVE,
            COMPLETED,
            ARCHIVED,
            EXPORTED,
            ERROR,
        }
    }

    private val logger = StructuredLogger.getInstance(context)
    private val activeSessions = ConcurrentHashMap<String, SessionData>()
    private val fileRegistry = ConcurrentHashMap<String, FileMetadata>()
    private val isInitialized = AtomicBoolean(false)
    private lateinit var baseDirectory: File
    private lateinit var sessionsDirectory: File
    private lateinit var tempDirectory: File
    private lateinit var archiveDirectory: File
    private lateinit var exportsDirectory: File
    private var fileUploadService: FileUploadService? = null

    data class SessionData(
        val sessionId: String,
        val deviceId: String,
        val startTime: Long,
        var endTime: Long? = null,
        var status: SessionStatus = SessionStatus.ACTIVE,
        val metadata: MutableMap<String, Any> = mutableMapOf(),
        val files: MutableList<FileMetadata> = mutableListOf(),
        val participantId: String? = null,
        val studyId: String? = null,
        val conditions: MutableList<String> = mutableListOf(),
        var totalSamples: Long = 0,
        val deviceInfo: MutableMap<String, Any> = mutableMapOf(),
    ) {
        fun getDurationMs(): Long {
            return (endTime ?: System.currentTimeMillis()) - startTime
        }

        fun getTotalFileSize(): Long {
            return files.sumOf { it.sizeBytes }
        }

        fun getFileCount(): Int {
            return files.size
        }

        fun getFilesByType(type: String): List<FileMetadata> {
            return files.filter { it.fileType == type }
        }
    }

    data class FileMetadata(
        val fileId: String,
        val fileName: String,
        val filePath: String,
        val fileType: String,
        val sizeBytes: Long,
        val checksum: String,
        val timestamp: Long,
        val sessionId: String,
        val deviceId: String,
        val mimeType: String,
        val metadata: MutableMap<String, Any> = mutableMapOf(),
        var uploadStatus: FileUploadService.UploadStatus = FileUploadService.UploadStatus.PENDING,
        var uploadJobId: String? = null,
    ) {
        val type: String
            get() = fileType
        val relativePath: String
            get() = "$sessionId/$deviceId/$fileName"
        val createdAt: Long
            get() = timestamp
        val absolutePath: String
            get() = filePath
        val exists: Boolean
            get() = File(filePath).exists()
        val isFile: Boolean
            get() = File(filePath).isFile

        fun isUploaded(): Boolean {
            return uploadStatus == FileUploadService.UploadStatus.COMPLETED
        }
    }

    fun initialize(fileUploadService: FileUploadService? = null) {
        this.fileUploadService = fileUploadService
        setupStorageDirectories()
        loadExistingSessions()
        isInitialized.set(true)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "service_initialized",
            details = mapOf(
                "base_directory" to baseDirectory.absolutePath,
                "existing_sessions" to activeSessions.size,
                "registered_files" to fileRegistry.size,
            )
        )
    }

    fun createSession(
        sessionId: String,
        deviceId: String,
        participantId: String? = null,
        studyId: String? = null,
        conditions: List<String> = emptyList(),
        customMetadata: Map<String, Any> = emptyMap(),
    ): SessionData {
        val session =
            SessionData(
                sessionId = sessionId,
                deviceId = deviceId,
                startTime = System.currentTimeMillis(),
                participantId = participantId,
                studyId = studyId,
                conditions = conditions.toMutableList(),
            )
        session.metadata.putAll(customMetadata)
        session.metadata["created_timestamp"] = System.currentTimeMillis()
        session.metadata["platform"] = "Android"
        session.metadata["app_version"] =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        val sessionDir = File(sessionsDirectory, sessionId)
        val deviceDir = File(sessionDir, deviceId)
        deviceDir.mkdirs()
        saveSessionMetadata(session)
        activeSessions[sessionId] = session
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "session_created",
            details = mapOf(
                "session_id" to sessionId,
                "device_id" to deviceId,
                "participant_id" to (participantId ?: "anonymous"),
                "study_id" to (studyId ?: "default"),
                "conditions" to conditions.joinToString(","),
            )
        )
        return session
    }

    fun endSession(sessionId: String): Boolean {
        val session = activeSessions[sessionId] ?: return false
        session.endTime = System.currentTimeMillis()
        session.status = SessionStatus.COMPLETED
        saveSessionMetadata(session)
        createFileManifest(session)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "session_ended",
            details = mapOf(
                "session_id" to sessionId,
                "duration_ms" to session.getDurationMs(),
                "file_count" to session.getFileCount(),
                "total_size_bytes" to session.getTotalFileSize(),
            ),
        )
        return true
    }

    fun registerFile(
        filePath: String,
        sessionId: String,
        deviceId: String,
        fileType: String,
        customMetadata: Map<String, Any> = emptyMap(),
    ): FileMetadata? {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                logger.log(
                    StructuredLogger.LogLevel.WARNING,
                    TAG,
                    "file_registration_error",
                    details = mapOf(
                        "file_path" to filePath,
                        "error" to "File does not exist",
                    ),
                )
                return null
            }
            val fileId = generateFileId(sessionId, deviceId, file.name)
            val checksum = calculateFileChecksum(file)
            val mimeType = getMimeType(file.extension)
            val metadata =
                FileMetadata(
                    fileId = fileId,
                    fileName = file.name,
                    filePath = filePath,
                    fileType = fileType,
                    sizeBytes = file.length(),
                    checksum = checksum,
                    timestamp = System.currentTimeMillis(),
                    sessionId = sessionId,
                    deviceId = deviceId,
                    mimeType = mimeType,
                )
            metadata.metadata.putAll(customMetadata)
            metadata.metadata["created_timestamp"] = file.lastModified()
            metadata.metadata["file_extension"] = file.extension
            fileRegistry[fileId] = metadata
            activeSessions[sessionId]?.files?.add(metadata)
            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "file_registered",
                details = mapOf(
                    "file_id" to fileId,
                    "file_name" to file.name,
                    "file_type" to fileType,
                    "file_size" to file.length(),
                    "session_id" to sessionId,
                ),
            )
            return metadata
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "file_registration_error",
                details = mapOf(
                    "file_path" to filePath,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
            return null
        }
    }

    suspend fun queueFilesForUpload(sessionId: String): List<String> {
        val uploadService = fileUploadService ?: return emptyList()
        val session = activeSessions[sessionId] ?: return emptyList()
        val uploadJobIds = mutableListOf<String>()
        for (fileMetadata in session.files) {
            if (fileMetadata.uploadStatus == FileUploadService.UploadStatus.COMPLETED) {
                continue
            }
            try {
                val uploadFileType =
                    when (fileMetadata.fileName.substringAfterLast(".", "").lowercase()) {
                        "mp4" -> FileUploadService.FileType.VISUAL_VIDEO
                        "csv" -> FileUploadService.FileType.GSR_DATA
                        "json" -> FileUploadService.FileType.METADATA
                        "wav" -> FileUploadService.FileType.AUDIO
                        else -> FileUploadService.FileType.METADATA
                    }
                val jobId =
                    uploadService.queueUpload(
                        filePath = fileMetadata.filePath,
                        sessionId = sessionId,
                        deviceId = fileMetadata.deviceId,
                        fileType = uploadFileType,
                    )
                fileMetadata.uploadJobId = jobId
                fileMetadata.uploadStatus = FileUploadService.UploadStatus.PENDING
                uploadJobIds.add(jobId)
            } catch (e: Exception) {
                logger.log(
                    StructuredLogger.LogLevel.ERROR,
                    TAG,
                    "upload_queue_error",
                    details = mapOf(
                        "file_id" to fileMetadata.fileId,
                        "error" to (e.message ?: "Unknown error"),
                    ),
                )
            }
        }
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "files_queued_for_upload",
            details = mapOf(
                "session_id" to sessionId,
                "queued_files" to uploadJobIds.size,
                "job_ids" to uploadJobIds.joinToString(","),
            ),
        )
        return uploadJobIds
    }

    suspend fun exportSession(
        sessionId: String,
        format: ExportFormat,
        includeFiles: Boolean = false,
    ): String? {
        val session = activeSessions[sessionId] ?: return null
        try {
            val exportDir = File(exportsDirectory, sessionId)
            exportDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
            val exportFileName = "session_${sessionId}_$timestamp.${format.name.lowercase()}"
            val exportFile = File(exportDir, exportFileName)
            when (format) {
                ExportFormat.JSON -> exportSessionAsJSON(session, exportFile, includeFiles)
                ExportFormat.CSV -> exportSessionAsCSV(session, exportFile)
                ExportFormat.HDF5 -> exportSessionAsHDF5(session, exportFile)
                ExportFormat.ZIP -> exportSessionAsZIP(session, exportFile, includeFiles)
            }
            session.status = SessionStatus.EXPORTED
            saveSessionMetadata(session)
            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "session_exported",
                details = mapOf(
                    "session_id" to sessionId,
                    "export_format" to format.name,
                    "export_file" to exportFile.absolutePath,
                    "include_files" to includeFiles,
                    "export_size" to exportFile.length(),
                ),
            )
            return exportFile.absolutePath
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "session_export_error",
                details = mapOf(
                    "session_id" to sessionId,
                    "format" to format.name,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
            return null
        }
    }

    fun getSession(sessionId: String): SessionData? {
        return activeSessions[sessionId]
    }

    fun getAllSessions(): List<SessionData> {
        return activeSessions.values.toList()
    }

    fun getFile(fileId: String): FileMetadata? {
        return fileRegistry[fileId]
    }

    fun getSessionFiles(sessionId: String): List<FileMetadata> {
        return activeSessions[sessionId]?.files ?: emptyList()
    }

    fun getStorageStats(): Map<String, Any> {
        val totalFiles = fileRegistry.size
        val totalSize = fileRegistry.values.sumOf { it.sizeBytes }
        val uploadedFiles = fileRegistry.values.count { it.isUploaded() }
        return mapOf(
            "total_sessions" to activeSessions.size,
            "total_files" to totalFiles,
            "total_size_bytes" to totalSize,
            "total_size_mb" to String.format("%.2f", totalSize / (1024.0 * 1024.0)),
            "uploaded_files" to uploadedFiles,
            "upload_progress" to if (totalFiles > 0) "${(uploadedFiles * 100) / totalFiles}%" else "0%",
            "base_directory" to baseDirectory.absolutePath,
            "free_space_bytes" to baseDirectory.freeSpace,
            "free_space_mb" to String.format("%.2f", baseDirectory.freeSpace / (1024.0 * 1024.0)),
        )
    }

    suspend fun performCleanup(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        val currentTime = System.currentTimeMillis()
        var cleanedSessions = 0
        var cleanedFiles = 0
        var freedBytes = 0L
        val sessionsToArchive =
            activeSessions.values.filter { session ->
                val age = currentTime - (session.endTime ?: session.startTime)
                age > maxAgeMs && (session.status == SessionStatus.COMPLETED || session.status == SessionStatus.EXPORTED)
            }
        for (session in sessionsToArchive) {
            if (archiveSession(session.sessionId)) {
                freedBytes += session.getTotalFileSize()
                cleanedSessions++
                cleanedFiles += session.getFileCount()
            }
        }
        val tempFiles = tempDirectory.listFiles() ?: emptyArray()
        for (file in tempFiles) {
            if (currentTime - file.lastModified() > maxAgeMs) {
                freedBytes += file.length()
                file.delete()
            }
        }
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "cleanup_completed",
            details = mapOf(
                "cleaned_sessions" to cleanedSessions,
                "cleaned_files" to cleanedFiles,
                "freed_bytes" to freedBytes,
                "freed_mb" to String.format("%.2f", freedBytes / (1024.0 * 1024.0)),
            ),
        )
    }

    private fun archiveSession(sessionId: String): Boolean {
        val session = activeSessions[sessionId] ?: return false
        val sessionDir = File(sessionsDirectory, sessionId)
        try {
            val archiveSessionDir = File(archiveDirectory, sessionId)
            archiveSessionDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
            val archiveFile = File(archiveSessionDir, "session_${sessionId}_$timestamp.zip")
            exportSessionAsZIP(session, archiveFile, includeFiles = true)
            sessionDir.deleteRecursively()
            activeSessions.remove(sessionId)
            session.files.forEach { file ->
                fileRegistry.remove(file.fileId)
            }
            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "session_archived",
                details = mapOf(
                    "session_id" to sessionId,
                    "archive_file" to archiveFile.absolutePath,
                    "archive_size" to archiveFile.length(),
                ),
            )
            return true
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "session_archive_error",
                details = mapOf(
                    "session_id" to sessionId,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
            return false
        }
    }

    private fun setupStorageDirectories() {
        val externalDir = context.getExternalFilesDir(null) ?: context.filesDir
        baseDirectory = File(externalDir, BASE_DIR)
        sessionsDirectory = File(baseDirectory, SESSIONS_DIR)
        tempDirectory = File(baseDirectory, TEMP_DIR)
        archiveDirectory = File(baseDirectory, ARCHIVE_DIR)
        exportsDirectory = File(baseDirectory, EXPORTS_DIR)
        baseDirectory.mkdirs()
        sessionsDirectory.mkdirs()
        tempDirectory.mkdirs()
        archiveDirectory.mkdirs()
        exportsDirectory.mkdirs()
    }

    private fun loadExistingSessions() {
        try {
            val sessionDirs = sessionsDirectory.listFiles { file -> file.isDirectory } ?: return
            for (sessionDir in sessionDirs) {
                val metadataFile = File(sessionDir, METADATA_FILE)
                if (metadataFile.exists()) {
                    loadSessionFromMetadata(metadataFile)
                }
            }
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "load_sessions_error",
                details = mapOf("error" to (e.message ?: "Unknown error")),
            )
        }
    }

    private fun loadSessionFromMetadata(metadataFile: File) {
        try {
            val jsonContent = metadataFile.readText()
            val json = JSONObject(jsonContent)
            val sessionId = json.getString("session_id")
            val deviceId = json.getString("device_id")
            val startTime = json.getLong("start_time")
            val endTime = if (json.has("end_time")) json.getLong("end_time") else null
            val status = SessionStatus.valueOf(json.optString("status", "COMPLETED"))
            val session =
                SessionData(
                    sessionId = sessionId,
                    deviceId = deviceId,
                    startTime = startTime,
                    endTime = endTime,
                    status = status,
                    participantId = if (json.has("participant_id")) json.getString("participant_id") else null,
                    studyId = if (json.has("study_id")) json.getString("study_id") else null,
                )
            if (json.has("metadata")) {
                val metadataJson = json.getJSONObject("metadata")
                metadataJson.keys().forEach { key ->
                    session.metadata[key] = metadataJson.get(key)
                }
            }
            if (json.has("conditions")) {
                val conditionsJson = json.getJSONArray("conditions")
                for (i in 0 until conditionsJson.length()) {
                    session.conditions.add(conditionsJson.getString(i))
                }
            }
            activeSessions[sessionId] = session
            loadFileManifest(session)
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "load_session_metadata_error",
                details = mapOf(
                    "metadata_file" to metadataFile.absolutePath,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
        }
    }

    private fun loadFileManifest(session: SessionData) {
        try {
            val sessionDir = File(sessionsDirectory, session.sessionId)
            val manifestFile = File(sessionDir, MANIFEST_FILE)
            if (!manifestFile.exists()) return
            val jsonContent = manifestFile.readText()
            val json = JSONObject(jsonContent)
            val filesJson = json.getJSONArray("files")
            for (i in 0 until filesJson.length()) {
                val fileJson = filesJson.getJSONObject(i)
                val fileMetadata =
                    FileMetadata(
                        fileId = fileJson.getString("file_id"),
                        fileName = fileJson.getString("file_name"),
                        filePath = fileJson.getString("file_path"),
                        fileType = fileJson.getString("file_type"),
                        sizeBytes = fileJson.getLong("size_bytes"),
                        checksum = fileJson.getString("checksum"),
                        timestamp = fileJson.getLong("timestamp"),
                        sessionId = fileJson.getString("session_id"),
                        deviceId = fileJson.getString("device_id"),
                        mimeType = fileJson.getString("mime_type"),
                    )
                if (fileJson.has("metadata")) {
                    val metadataJson = fileJson.getJSONObject("metadata")
                    metadataJson.keys().forEach { key ->
                        fileMetadata.metadata[key] = metadataJson.get(key)
                    }
                }
                session.files.add(fileMetadata)
                fileRegistry[fileMetadata.fileId] = fileMetadata
            }
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "load_file_manifest_error",
                details = mapOf(
                    "session_id" to session.sessionId,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
        }
    }

    private fun saveSessionMetadata(session: SessionData) {
        try {
            val sessionDir = File(sessionsDirectory, session.sessionId)
            sessionDir.mkdirs()
            val metadataFile = File(sessionDir, METADATA_FILE)
            val json =
                JSONObject().apply {
                    put("session_id", session.sessionId)
                    put("device_id", session.deviceId)
                    put("start_time", session.startTime)
                    session.endTime?.let { put("end_time", it) }
                    put("status", session.status.name)
                    session.participantId?.let { put("participant_id", it) }
                    session.studyId?.let { put("study_id", it) }
                    val metadataJson = JSONObject()
                    session.metadata.forEach { (key, value) -> metadataJson.put(key, value) }
                    put("metadata", metadataJson)
                    val conditionsJson = JSONArray()
                    session.conditions.forEach { condition -> conditionsJson.put(condition) }
                    put("conditions", conditionsJson)
                }
            metadataFile.writeText(json.toString(2))
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "save_session_metadata_error",
                details = mapOf(
                    "session_id" to session.sessionId,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
        }
    }

    private fun createFileManifest(session: SessionData) {
        try {
            val sessionDir = File(sessionsDirectory, session.sessionId)
            val manifestFile = File(sessionDir, MANIFEST_FILE)
            val json =
                JSONObject().apply {
                    put("session_id", session.sessionId)
                    put("created_timestamp", System.currentTimeMillis())
                    put("file_count", session.files.size)
                    put("total_size_bytes", session.getTotalFileSize())
                    val filesJson = JSONArray()
                    session.files.forEach { file ->
                        val fileJson =
                            JSONObject().apply {
                                put("file_id", file.fileId)
                                put("file_name", file.fileName)
                                put("file_path", file.filePath)
                                put("file_type", file.fileType)
                                put("size_bytes", file.sizeBytes)
                                put("checksum", file.checksum)
                                put("timestamp", file.timestamp)
                                put("session_id", file.sessionId)
                                put("device_id", file.deviceId)
                                put("mime_type", file.mimeType)
                                val metadataJson = JSONObject()
                                file.metadata.forEach { (key, value) ->
                                    metadataJson.put(
                                        key,
                                        value
                                    )
                                }
                                put("metadata", metadataJson)
                            }
                        filesJson.put(fileJson)
                    }
                    put("files", filesJson)
                }
            manifestFile.writeText(json.toString(2))
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "create_file_manifest_error",
                details = mapOf(
                    "session_id" to session.sessionId,
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
        }
    }

    private fun exportSessionAsJSON(
        session: SessionData,
        exportFile: File,
        includeFiles: Boolean,
    ) {
        val json =
            JSONObject().apply {
                put("session_id", session.sessionId)
                put("device_id", session.deviceId)
                put("start_time", session.startTime)
                session.endTime?.let { put("end_time", it) }
                put("duration_ms", session.getDurationMs())
                put("status", session.status.name)
                session.participantId?.let { put("participant_id", it) }
                session.studyId?.let { put("study_id", it) }
                val filesJson = JSONArray()
                session.files.forEach { file ->
                    val fileJson =
                        JSONObject().apply {
                            put("file_id", file.fileId)
                            put("file_name", file.fileName)
                            put("file_type", file.fileType)
                            put("size_bytes", file.sizeBytes)
                            put("checksum", file.checksum)
                            put("timestamp", file.timestamp)
                            put("mime_type", file.mimeType)
                            if (includeFiles) {
                                put("relative_path", file.relativePath)
                            }
                        }
                    filesJson.put(fileJson)
                }
                put("files", filesJson)
                put("export_timestamp", System.currentTimeMillis())
                put("export_format", "JSON")
            }
        exportFile.writeText(json.toString(2))
    }

    private fun exportSessionAsCSV(session: SessionData, exportFile: File) {
        val csvContent = StringBuilder()
        csvContent.appendLine("file_id,file_name,file_type,size_bytes,checksum,timestamp,mime_type")
        session.files.forEach { file ->
            csvContent.appendLine(
                "\"${file.fileId}\",\"${file.fileName}\",\"${file.fileType}\",${file.sizeBytes},\"${file.checksum}\",${file.timestamp},\"${file.mimeType}\"",
            )
        }
        exportFile.writeText(csvContent.toString())
    }

    private fun exportSessionAsHDF5(session: SessionData, exportFile: File) {
        try {
            val hdf5Structure = JSONObject().apply {
                put("format", "HDF5-Compatible JSON")
                put("version", "1.0")
                put(
                    "created",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(
                        Date()
                    )
                )
                val rootGroup = JSONObject().apply {
                    put("attributes", JSONObject().apply {
                        put("title", "IRCamera Session Data")
                        put("session_id", session.sessionId)
                        put("participant_id", session.participantId)
                        put("start_time", session.startTime)
                        put("end_time", session.endTime)
                        put(
                            "duration_sec",
                            ((session.endTime
                                ?: System.currentTimeMillis()) - session.startTime) / 1000.0
                        )
                    })
                    val dataGroups = JSONObject()
                    if (session.files.any { it.type == "gsr_data" }) {
                        dataGroups.put("gsr", JSONObject().apply {
                            put("attributes", JSONObject().apply {
                                put("sensor_type", "Shimmer3_GSR")
                                put("sampling_rate_hz", 128)
                                put("units", JSONObject().apply {
                                    put("gsr", "microsiemens")
                                    put("timestamp", "nanoseconds")
                                    put("ppg", "arbitrary_units")
                                })
                            })
                            put("datasets", JSONObject().apply {
                                put("timestamps", JSONObject().apply {
                                    put("shape", JSONArray().put(session.totalSamples))
                                    put("dtype", "int64")
                                    put("description", "Monotonic timestamps in nanoseconds")
                                })
                                put("gsr_microsiemens", JSONObject().apply {
                                    put("shape", JSONArray().put(session.totalSamples))
                                    put("dtype", "float64")
                                    put("description", "GSR values in microsiemens")
                                })
                                put("gsr_raw", JSONObject().apply {
                                    put("shape", JSONArray().put(session.totalSamples))
                                    put("dtype", "int16")
                                    put("description", "Raw 12-bit ADC values (0-4095)")
                                })
                                put("ppg_raw", JSONObject().apply {
                                    put("shape", JSONArray().put(session.totalSamples))
                                    put("dtype", "int16")
                                    put("description", "Raw PPG sensor values")
                                })
                                put("quality_scores", JSONObject().apply {
                                    put("shape", JSONArray().put(session.totalSamples))
                                    put("dtype", "float32")
                                    put("description", "Signal quality scores (0.0-1.0)")
                                })
                            })
                        })
                    }
                    if (session.files.any { it.type == "rgb_video" }) {
                        dataGroups.put("rgb_video", JSONObject().apply {
                            put("attributes", JSONObject().apply {
                                put("resolution", "3840x2160")
                                put("fps", 60)
                                put("codec", "H.264")
                                put("format", "MP4")
                            })
                            put("datasets", JSONObject().apply {
                                put("video_file_ref", JSONObject().apply {
                                    put(
                                        "path",
                                        session.files.find { it.type == "rgb_video" }?.relativePath
                                            ?: ""
                                    )
                                    put("description", "Reference to external video file")
                                })
                                put("frame_timestamps", JSONObject().apply {
                                    put("shape", JSONArray().put("estimated_frames"))
                                    put("dtype", "int64")
                                    put("description", "Frame timestamps in nanoseconds")
                                })
                            })
                        })
                    }
                    if (session.files.any { it.type == "thermal_data" }) {
                        dataGroups.put("thermal", JSONObject().apply {
                            put("attributes", JSONObject().apply {
                                put("sensor_type", "Topdon_TC001")
                                put("resolution", "256x192")
                                put("fps", 10)
                                put("temperature_range_c", JSONObject().apply {
                                    put("min", -20)
                                    put("max", 400)
                                })
                                put("units", JSONObject().apply {
                                    put("temperature", "celsius")
                                    put("timestamp", "nanoseconds")
                                })
                            })
                            put("datasets", JSONObject().apply {
                                put("temperature_matrix", JSONObject().apply {
                                    put("shape", JSONArray().put("frames").put(192).put(256))
                                    put("dtype", "float32")
                                    put("description", "3D array of temperature matrices")
                                })
                                put("frame_timestamps", JSONObject().apply {
                                    put("shape", JSONArray().put("frames"))
                                    put("dtype", "int64")
                                    put("description", "Frame timestamps in nanoseconds")
                                })
                            })
                        })
                    }
                    put("groups", dataGroups)
                    put("sync_markers", JSONObject().apply {
                        put("attributes", JSONObject().apply {
                            put("description", "Synchronization markers for multi-modal alignment")
                        })
                        put("datasets", JSONObject().apply {
                            put("timestamps", JSONObject().apply {
                                put("dtype", "int64")
                                put("description", "Sync marker timestamps in nanoseconds")
                            })
                            put("marker_types", JSONObject().apply {
                                put("dtype", "string")
                                put("description", "Sync marker type identifiers")
                            })
                            put("metadata", JSONObject().apply {
                                put("dtype", "string")
                                put("description", "JSON-encoded marker metadata")
                            })
                        })
                    })
                }
                put("root", rootGroup)
                val fileManifest = JSONArray()
                session.files.forEach { file ->
                    fileManifest.put(JSONObject().apply {
                        put("path", file.relativePath)
                        put("type", file.type)
                        put("size_bytes", file.sizeBytes)
                        put("checksum", file.checksum)
                        put("created", file.createdAt)
                    })
                }
                put("external_files", fileManifest)
            }
            exportFile.writeText(hdf5Structure.toString(2))        } catch (e: Exception) {            exportSessionAsJSON(session, exportFile, includeFiles = true)
        }
    }

    private fun exportSessionAsZIP(
        session: SessionData,
        exportFile: File,
        includeFiles: Boolean,
    ) {
        try {
            val zipOutputStream = java.util.zip.ZipOutputStream(exportFile.outputStream())
            val sessionMetadata = JSONObject().apply {
                put("session_id", session.sessionId)
                put("participant_id", session.participantId)
                put("start_time", session.startTime)
                put("end_time", session.endTime)
                put(
                    "duration_sec",
                    ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 1000.0
                )
                put("total_samples", session.totalSamples)
                put("device_info", session.deviceInfo)
                put("export_format", "ZIP Archive")
                put(
                    "export_timestamp",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(
                        Date()
                    )
                )
            }
            zipOutputStream.putNextEntry(java.util.zip.ZipEntry("session_metadata.json"))
            zipOutputStream.write(sessionMetadata.toString(2).toByteArray())
            zipOutputStream.closeEntry()
            val manifest = JSONArray()
            session.files.forEach { fileInfo ->
                manifest.put(JSONObject().apply {
                    put("filename", fileInfo.relativePath)
                    put("type", fileInfo.type)
                    put("size_bytes", fileInfo.sizeBytes)
                    put("checksum", fileInfo.checksum)
                    put("created_at", fileInfo.createdAt)
                })
            }
            zipOutputStream.putNextEntry(java.util.zip.ZipEntry("file_manifest.json"))
            zipOutputStream.write(manifest.toString(2).toByteArray())
            zipOutputStream.closeEntry()
            if (includeFiles) {
                session.files.forEach { fileInfo ->
                    try {
                        val sourceFile = File(fileInfo.absolutePath)
                        if (sourceFile.exists() && sourceFile.isFile) {
                            zipOutputStream.putNextEntry(java.util.zip.ZipEntry("data/${fileInfo.relativePath}"))
                            sourceFile.inputStream().use { input ->
                                input.copyTo(zipOutputStream)
                            }
                            zipOutputStream.closeEntry()                        } else {                        }
                    } catch (e: Exception) {                    }
                }
            }
            val readme = """
                IRCamera Session Export (ZIP Format)
                ===================================
                
                Session ID: ${session.sessionId}
                Participant: ${session.participantId ?: "Unknown"}
                Duration: ${((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 1000.0} seconds
                Total Samples: ${session.totalSamples}
                
                Files included:
                - session_metadata.json: Complete session metadata
                - file_manifest.json: List of all data files with checksums
                ${if (includeFiles) "- data/: Directory containing all session data files" else "- Data files not included (metadata only export)"}
                
                File Types:
                ${
                session.files.groupBy { file -> file.type }.entries.joinToString("\n") { entry ->
                    "- ${entry.key}: ${entry.value.size} file(s)"
                }
            }
                
                Generated: ${
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )
            }
                Export Tool: IRCamera Data Management Service v1.0
            """.trimIndent()
            zipOutputStream.putNextEntry(java.util.zip.ZipEntry("README.txt"))
            zipOutputStream.write(readme.toByteArray())
            zipOutputStream.closeEntry()
            zipOutputStream.close()            Log.i(
                TAG,
                "ZIP contains ${session.files.size} files (${if (includeFiles) "with" else "without"} data)"
            )
        } catch (e: Exception) {            exportSessionAsJSON(session, exportFile, includeFiles)
        }
    }

    private fun calculateFileChecksum(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "error_calculating_checksum"
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "wav" -> "audio/wav"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }

    private fun generateFileId(
        sessionId: String,
        deviceId: String,
        fileName: String,
    ): String {
        val timestamp = System.currentTimeMillis()
        val uniqueString = "$sessionId-$deviceId-$fileName-$timestamp-${UUID.randomUUID()}"
        val digest = java.security.MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(uniqueString.toByteArray())
        return "file_" + hashBytes.joinToString("") { "%02x".format(it) }.substring(0, 16)
    }
}
