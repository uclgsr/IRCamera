package mpdc4gsr.sensors.gsr

import android.content.Context
import android.util.Log
import mpdc4gsr.sensors.thermal.ThermalCameraRecorder
import mpdc4gsr.data.SessionMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.RecordingStatus
import mpdc4gsr.sensors.SensorError
import mpdc4gsr.sensors.RecordingStats
import java.io.File
import java.io.FileWriter


class EnhancedThermalRecorder(private val context: Context) {

    companion object {
        private const val TAG = "EnhancedThermalRecorder"
    }

    private val thermalCameraRecorder = ThermalCameraRecorder(context)
    private var currentSessionDirectory: File? = null
    private var syncEventWriter: FileWriter? = null


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


            GlobalScope.launch {
                val success = if (sessionMetadata != null) {
                    thermalCameraRecorder.startRecording(
                        currentSessionDirectory!!.absolutePath,
                        sessionMetadata
                    )
                } else {
                    thermalCameraRecorder.startRecording(currentSessionDirectory!!.absolutePath)
                }

                if (success) {
                    Log.i(TAG, "Enhanced thermal recording started for session: $sessionId")
                } else {
                    Log.e(TAG, "Failed to start thermal recording for session: $sessionId")
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start enhanced thermal recording", e)
            return false
        }
    }


    fun stopRecording(): SessionInfo? {
        return try {

            GlobalScope.launch {
                thermalCameraRecorder.stopRecording()
            }

            closeSyncEventsFile()
            val sessionInfo = SessionInfo(
                sessionDirectory = currentSessionDirectory,
                sampleCount = thermalCameraRecorder.getRecordingStats().frameCount
            )
            Log.i(TAG, "Enhanced thermal recording stopped successfully")
            sessionInfo
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop enhanced thermal recording", e)
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

            Log.d(TAG, "Sync event triggered: $eventType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger sync event: $eventType", e)
        }
    }


    fun getSessionDirectory(): File? {
        return currentSessionDirectory
    }


    fun cleanup() {
        try {
            closeSyncEventsFile()

            kotlinx.coroutines.GlobalScope.launch {
                thermalCameraRecorder.cleanup()
            }
            currentSessionDirectory = null
            Log.i(TAG, "Enhanced thermal recorder cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
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
            Log.e(TAG, "Failed to setup sync events file", e)
        }
    }

    private fun closeSyncEventsFile() {
        try {
            syncEventWriter?.close()
            syncEventWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close sync events file", e)
        }
    }


    data class SessionInfo(
        val sessionDirectory: File?,
        val sampleCount: Long,
        val startTime: Long = System.currentTimeMillis()
    )
}