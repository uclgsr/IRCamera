package mpdc4gsr.sensors.gsr

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.sensors.RecordingStats
import mpdc4gsr.sensors.RecordingStatus
import mpdc4gsr.sensors.SensorError
import mpdc4gsr.sensors.thermal.ThermalCameraRecorder
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

                if (success) {                } else {                }
            }

            return true
        } catch (e: Exception) {            return false
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
                sampleCount = thermalCameraRecorder.getRecordingStats().totalSamplesRecorded
            )            sessionInfo
        } catch (e: Exception) {            null
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
            }        } catch (e: Exception) {        }
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
            currentSessionDirectory = null        } catch (e: Exception) {        }
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
        } catch (e: Exception) {        }
    }

    private fun closeSyncEventsFile() {
        try {
            syncEventWriter?.close()
            syncEventWriter = null
        } catch (e: Exception) {        }
    }


    data class SessionInfo(
        val sessionDirectory: File?,
        val sampleCount: Long,
        val startTime: Long = System.currentTimeMillis()
    )
}