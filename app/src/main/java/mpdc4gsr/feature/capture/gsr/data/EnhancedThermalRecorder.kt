package mpdc4gsr.feature.capture.gsr.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.hardware.api.RecordingStats
import mpdc4gsr.core.hardware.api.RecordingStatus
import mpdc4gsr.core.hardware.api.SensorError
import mpdc4gsr.feature.capture.thermal.ui.ThermalCameraRecorder
import java.io.File
import java.io.FileWriter

class EnhancedThermalRecorder(
    private val context: Context,
) {
    companion object {
        private const val TAG = "EnhancedThermalRecorder"
    }

    private val thermalCameraRecorder = ThermalCameraRecorder(context)
    private var currentSessionDirectory: File? = null
    private var syncEventWriter: FileWriter? = null
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun initialize(): Boolean = thermalCameraRecorder.initialize()

    fun startRecording(
        sessionId: String,
        sessionMetadata: SessionMetadata?,
        saveImages: Boolean = false,
    ): Boolean {
        try {
            val externalDir = File(context.getExternalFilesDir(null), "IRCamera/sessions")
            currentSessionDirectory = File(externalDir, sessionId)
            currentSessionDirectory?.mkdirs()
            setupSyncEventsFile()
            recorderScope.launch {
                val success =
                    if (sessionMetadata != null) {
                        thermalCameraRecorder.startRecording(
                            currentSessionDirectory!!.absolutePath,
                            sessionMetadata,
                        )
                    } else {
                        thermalCameraRecorder.startRecording(currentSessionDirectory!!.absolutePath)
                    }
                if (success) {
                } else {
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun stopRecording(): SessionInfo? =
        try {
            recorderScope.launch {
                thermalCameraRecorder.stopRecording()
            }
            closeSyncEventsFile()
            val sessionInfo =
                SessionInfo(
                    sessionDirectory = currentSessionDirectory,
                    sampleCount = thermalCameraRecorder.getRecordingStats().totalSamplesRecorded,
                )
            sessionInfo
        } catch (e: Exception) {
            null
        }

    fun triggerSyncEvent(
        eventType: String,
        eventData: Map<String, String>,
    ) {
        try {
            val timestamp = System.nanoTime()
            val eventLine =
                buildString {
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
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger
                .e("EnhancedThermalRecorder", "Unexpected Exception in EnhancedThermalRecorder catch block", e)
        }
    }

    fun getSessionDirectory(): File? = currentSessionDirectory

    fun cleanup() {
        try {
            closeSyncEventsFile()
            recorderScope.launch {
                thermalCameraRecorder.cleanup()
            }
            recorderScope.cancel()
            currentSessionDirectory = null
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger
                .e("EnhancedThermalRecorder", "Unexpected Exception in EnhancedThermalRecorder catch block", e)
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
            mpdc4gsr.core.common.AppLogger
                .e("EnhancedThermalRecorder", "Unexpected Exception in EnhancedThermalRecorder catch block", e)
        }
    }

    private fun closeSyncEventsFile() {
        try {
            syncEventWriter?.close()
            syncEventWriter = null
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger
                .e("EnhancedThermalRecorder", "Unexpected Exception in EnhancedThermalRecorder catch block", e)
        }
    }

    data class SessionInfo(
        val sessionDirectory: File?,
        val sampleCount: Long,
        val startTime: Long = System.currentTimeMillis(),
    )
}

