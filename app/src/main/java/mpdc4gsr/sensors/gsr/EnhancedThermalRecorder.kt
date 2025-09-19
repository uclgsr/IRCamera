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

/**
 * Enhanced thermal recorder that wraps ThermalCameraRecorder and provides additional
 * synchronization and session management functionality needed by multi-modal recording.
 */
class EnhancedThermalRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedThermalRecorder"
    }
    
    private val thermalCameraRecorder = ThermalCameraRecorder(context)
    private var currentSessionDirectory: File? = null
    private var syncEventWriter: FileWriter? = null
    
    // Initialize the underlying thermal camera recorder
    suspend fun initialize(): Boolean {
        return thermalCameraRecorder.initialize()
    }
    
    /**
     * Start recording with session management
     */
    fun startRecording(
        sessionId: String,
        sessionMetadata: SessionMetadata?,
        saveImages: Boolean = false
    ): Boolean {
        try {
            // Set up session directory using Android external storage
            val externalDir = File(context.getExternalFilesDir(null), "IRCamera/sessions")
            currentSessionDirectory = File(externalDir, sessionId)
            currentSessionDirectory?.mkdirs()
            
            // Initialize sync events file
            setupSyncEventsFile()
            
            // Launch recording in background since underlying method is suspend
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
            
            return true // Return immediately, actual result handled asynchronously
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start enhanced thermal recording", e)
            return false
        }
    }
    
    /**
     * Stop recording and return session info
     */
    fun stopRecording(): SessionInfo? {
        return try {
            // Stop recording asynchronously since underlying method is suspend
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
    
    /**
     * Trigger synchronization event for multi-modal coordination
     */
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
    
    /**
     * Get the current session directory
     */
    fun getSessionDirectory(): File? {
        return currentSessionDirectory
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            closeSyncEventsFile()
            // Launch cleanup in a coroutine since underlying cleanup is suspend
            kotlinx.coroutines.GlobalScope.launch {
                thermalCameraRecorder.cleanup()
            }
            currentSessionDirectory = null
            Log.i(TAG, "Enhanced thermal recorder cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    // Delegate methods to underlying thermal camera recorder
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
    
    /**
     * Session information returned when recording stops
     */
    data class SessionInfo(
        val sessionDirectory: File?,
        val sampleCount: Long,
        val startTime: Long = System.currentTimeMillis()
    )
}