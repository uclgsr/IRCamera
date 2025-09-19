package com.topdon.tc001.gsr

import android.content.Context
import android.util.Log
import com.topdon.tc001.data.SessionMetadata
import com.topdon.tc001.sensors.thermal.ThermalRecorder
import com.topdon.gsr.model.SessionInfo
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * EnhancedThermalRecorder combines thermal recording capabilities with session management
 * and synchronization features required by the SynchronizedMultiModalRecorder.
 */
class EnhancedThermalRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedThermalRecorder"
        private const val SESSIONS_DIR = "IRCamera_Sessions"
    }
    
    private val thermalRecorder = ThermalRecorder(context)
    private var currentSessionInfo: SessionInfo? = null
    private var sessionDirectory: File? = null
    private val isRecording = AtomicBoolean(false)
    
    /**
     * Start recording with session ID and optional session metadata
     */
    suspend fun startRecording(
        sessionId: String,
        sessionMetadata: SessionMetadata? = null,
        saveImages: Boolean = false
    ): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "Thermal recording already in progress")
            return false
        }
        
        try {
            // Create session directory
            sessionDirectory = createSessionDirectory(sessionId)
            if (sessionDirectory == null) {
                Log.e(TAG, "Failed to create session directory")
                return false
            }
            
            // Create session info
            currentSessionInfo = SessionInfo(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                participantId = null,
                studyName = "ThermalRecording"
            )
            
            // Start thermal recording
            val success = thermalRecorder.startRecording(
                sessionDirectory!!.absolutePath,
                sessionMetadata ?: createDefaultSessionMetadata(sessionId),
                saveImages
            )
            
            if (success) {
                isRecording.set(true)
                Log.i(TAG, "Enhanced thermal recording started: $sessionId")
            }
            
            return success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start enhanced thermal recording", e)
            return false
        }
    }
    
    /**
     * Stop recording and return session information
     */
    suspend fun stopRecording(): SessionInfo? {
        if (!isRecording.get()) {
            Log.w(TAG, "No thermal recording in progress")
            return null
        }
        
        try {
            val success = thermalRecorder.stopRecording()
            
            if (success && currentSessionInfo != null) {
                val sessionInfo = currentSessionInfo!!.copy(
                    endTime = System.currentTimeMillis()
                )
                
                isRecording.set(false)
                Log.i(TAG, "Enhanced thermal recording stopped: ${sessionInfo.sessionId}")
                return sessionInfo
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop enhanced thermal recording", e)
            return null
        } finally {
            isRecording.set(false)
            currentSessionInfo = null
        }
    }
    
    /**
     * Trigger a synchronization event
     */
    fun triggerSyncEvent(eventType: String, metadata: Map<String, String>) {
        if (!isRecording.get()) {
            Log.w(TAG, "Cannot trigger sync event - not recording")
            return
        }
        
        try {
            Log.d(TAG, "Triggered sync event: $eventType with metadata: $metadata")
            // In a real implementation, this would write to a sync markers file
            // For now, we just log the event
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger sync event", e)
        }
    }
    
    /**
     * Get the current session directory
     */
    fun getSessionDirectory(): File? {
        return sessionDirectory
    }
    
    /**
     * Check if currently recording
     */
    val isRecordingActive: Boolean
        get() = isRecording.get()
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        if (isRecording.get()) {
            runBlocking { stopRecording() }
        }
        sessionDirectory = null
        currentSessionInfo = null
    }
    
    private fun createSessionDirectory(sessionId: String): File? {
        return try {
            val baseDir = File(context.getExternalFilesDir(null), SESSIONS_DIR)
            val sessionDir = File(baseDir, sessionId)
            
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            
            sessionDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session directory", e)
            null
        }
    }
    
    private fun createDefaultSessionMetadata(sessionId: String): SessionMetadata {
        val currentTimeMs = System.currentTimeMillis()
        val currentTimeNs = System.nanoTime()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        return SessionMetadata(
            sessionId = sessionId,
            sessionStartTimestampMs = currentTimeMs,
            sessionStartMonotonicNs = currentTimeNs,
            sessionStartIso = dateFormat.format(Date(currentTimeMs))
        )
    }
}