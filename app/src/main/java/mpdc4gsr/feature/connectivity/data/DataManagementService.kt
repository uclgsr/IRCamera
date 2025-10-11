package mpdc4gsr.feature.connectivity.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight replacement for the monolithic legacy data manager. It keeps
 * track of session directories and file registrations so thermal captures can
 * be reconciled during post-session export without dragging in the previous
 * implementation.
 */
@Singleton
class DataManagementService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {

        private val sessions = ConcurrentHashMap<String, SessionRecord>()

        fun initialize() {
            storageRoot().mkdirs()
        }

        fun createSession(
            sessionId: String,
            deviceId: String,
            customMetadata: Map<String, Any?> = emptyMap(),
        ): SessionRecord {
            val record =
                SessionRecord(
                    sessionId = sessionId,
                    deviceId = deviceId,
                    directory = storageRoot().resolve(sessionId).apply { mkdirs() },
                    metadata = customMetadata.toMutableMap(),
                )
            sessions[sessionId] = record
            return record
        }

        fun getSession(sessionId: String): SessionRecord? = sessions[sessionId]

        fun registerFile(
            filePath: String,
            sessionId: String,
            deviceId: String,
            fileType: String,
            customMetadata: Map<String, Any?> = emptyMap(),
        ) {
            val record = sessions.getOrPut(sessionId) {
                createSession(sessionId, deviceId)
            }
            record.files[filePath] =
                FileRecord(
                    filePath = filePath,
                    fileType = fileType,
                    metadata = customMetadata.toMutableMap(),
                )
        }

        private fun storageRoot(): File = File(context.filesDir, "sessions_archive")
    }

data class SessionRecord(
    val sessionId: String,
    val deviceId: String,
    val directory: File,
    val metadata: MutableMap<String, Any?>,
    val files: MutableMap<String, FileRecord> = mutableMapOf(),
)

data class FileRecord(
    val filePath: String,
    val fileType: String,
    val metadata: MutableMap<String, Any?>,
)
