package mpdc4gsr.feature.gsr.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RecordingStats
import mpdc4gsr.core.data.RecordingStatus
import mpdc4gsr.core.data.SensorError
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import java.io.File
import java.io.FileWriter

class EnhancedThermalRecorder(private val context: Context) {
    companion object {
        private const val TAG = "EnhancedThermalRecorder"
    }

    private val thermalCameraRecorder = ThermalCameraRecorder(context)
    private var currentSessionDirectory: File? = null
    private var syncEventWriter: FileWriter? = null
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    suspend fun initialize(): Boolean {
        return thermalCameraRecorder.initialize()
    }

    fun startRecording(
        sessionId: String,
        sessionMetadata: SessionMetadata?,
        saveImages: Boolean = false
    ): Boolean {
        try {
            val externalDir = File(context.getExternalFilesDir(null), "IRCamera/sessions")
            currentSessionDirectory = File(externalDir, sessionId)
            currentSessionDirectory?.mkdirs()
            setupSyncEventsFile()
            recorderScope.launch {
                val success = if (sessionMetadata != null) {
                    thermalCameraRecorder.startRecording(
                        currentSessionDirectory!!.absolutePath,
                        sessionMetadata
                    )
                } else {
                    thermalCameraRecorder.startRecording(currentSessionDirectory!!.absolutePath)
                }
                if (success) {
                    AppLogger.i(TAG, "Enhanced thermal recording started for session: $sessionId")
                } else {
                    AppLogger.e(TAG, "Failed to start thermal recording for session: $sessionId")
                }
            }
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start enhanced thermal recording", e)
            return false
        }
    }

    fun stopRecording(): SessionInfo? {
        return try {
            recorderScope.launch {
                thermalCameraRecorder.stopRecording()
            }
            closeSyncEventsFile()
            val sessionInfo = SessionInfo(
                sessionDirectory = currentSessionDirectory,
                sampleCount = thermalCameraRecorder.getRecordingStats().totalSamplesRecorded
            )
            AppLogger.i(TAG, "Enhanced thermal recording stopped successfully")
            sessionInfo
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop enhanced thermal recording", e)
            null
        }
    }

    fun triggerSyncEvent(eventType: String, eventData: Map<String, String>) {
        try {
            val timestamp = System.nanoTime()
            val eventLine = buildString {
                append(timestamp)
                append(",")
                append(eventType)
                append(",")
                append(eventData.entries.joinToString(";") { "${it.key}=${it.value}" })
            }
            syncEventWriter?.let { writer ->
                writer.write(eventLine)
                writer.write("\n")
                writer.flush()
            }
            AppLogger.d(TAG, "Sync event triggered: $eventType")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to trigger sync event: $eventType", e)
        }
    }

    fun getSessionDirectory(): File? {
        return currentSessionDirectory
    }

    fun cleanup() {
        try {
            closeSyncEventsFile()
            recorderScope.launch {
                thermalCameraRecorder.cleanup()
            }
            recorderScope.cancel()
            currentSessionDirectory = null
            AppLogger.i(TAG, "Enhanced thermal recorder cleaned up")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during cleanup", e)
        }
    }

    fun getStatusFlow(): Flow<RecordingStatus> = thermalCameraRecorder.getStatusFlow()
    fun getErrorFlow(): Flow<SensorError> = thermalCameraRecorder.getErrorFlow()
    fun getRecordingStats(): RecordingStats = thermalCameraRecorder.getRecordingStats()
    private fun setupSyncEventsFile() {
        try {
            currentSessionDirectory?.let { dir ->
                val syncEventsFile = File(dir, "sync_events.csv")
                syncEventWriter = FileWriter(syncEventsFile, true)
                syncEventWriter?.write("timestamp_ns,event_type,event_data\n")
                syncEventWriter?.flush()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to setup sync events file", e)
        }
    }

    private fun closeSyncEventsFile() {
        try {
            syncEventWriter?.close()
            syncEventWriter = null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to close sync events file", e)
        }
    }

    data class SessionInfo(
        val sessionDirectory: File?,
        val sampleCount: Long,
        val startTime: Long = System.currentTimeMillis()
    )
}