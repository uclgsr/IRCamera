package com.topdon.tc001.network

import android.graphics.Bitmap
import android.util.Log
import com.example.thermal_lite.camera.CameraPreviewManager
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import com.topdon.tc001.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * PreviewDataAdapter connects sensor data sources to PreviewStreamer.
 * 
 * This adapter polls data from various sensors and cameras and feeds it to
 * the PreviewStreamer for transmission to the PC Controller.
 */
class PreviewDataAdapter(
    private val previewStreamer: PreviewStreamer,
    private val recordingService: RecordingService
) {
    companion object {
        private const val TAG = "PreviewDataAdapter"
        private const val POLLING_INTERVAL_MS = 500L // Poll sensors every 500ms
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var isRunning = false

    // Data polling references
    private val thermalCameraManager = AtomicReference<CameraPreviewManager?>()
    private val gsrRecorder = AtomicReference<GSRSensorRecorder?>()

    /**
     * Start polling sensor data and feeding to PreviewStreamer
     */
    fun startDataPolling() {
        if (isRunning) {
            Log.w(TAG, "Data polling already running")
            return
        }

        Log.i(TAG, "Starting sensor data polling for preview streaming")
        isRunning = true

        pollingJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    pollSensorData()
                    delay(POLLING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in sensor data polling", e)
                    delay(1000) // Wait longer on error
                }
            }
        }
    }

    /**
     * Stop sensor data polling
     */
    fun stopDataPolling() {
        if (!isRunning) {
            return
        }

        Log.i(TAG, "Stopping sensor data polling")
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Set thermal camera manager reference for preview data
     */
    fun setThermalCameraManager(manager: CameraPreviewManager?) {
        thermalCameraManager.set(manager)
        Log.d(TAG, "Thermal camera manager ${if (manager != null) "set" else "cleared"}")
    }

    /**
     * Set GSR sensor recorder reference for preview data
     */
    fun setGsrRecorder(recorder: GSRSensorRecorder?) {
        gsrRecorder.set(recorder)
        Log.d(TAG, "GSR recorder ${if (recorder != null) "set" else "cleared"}")
    }

    private suspend fun pollSensorData() {
        // Poll thermal camera frame
        pollThermalFrame()
        
        // Poll GSR data
        pollGsrData()
        
        // Update recording status
        updateRecordingStatus()
    }

    private suspend fun pollThermalFrame() {
        try {
            val manager = thermalCameraManager.get()
            if (manager != null) {
                // Get scaled bitmap from thermal camera
                val thermalBitmap = manager.scaledBitmap()
                if (thermalBitmap != null && !thermalBitmap.isRecycled) {
                    previewStreamer.updateThermalFrame(thermalBitmap)
                    Log.v(TAG, "Updated thermal frame: ${thermalBitmap.width}x${thermalBitmap.height}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error polling thermal frame", e)
        }
    }

    private suspend fun pollGsrData() {
        try {
            val recorder = gsrRecorder.get()
            if (recorder != null && recorder.isRecording) {
                // Get latest GSR value - this is a simplified approach
                // In a real implementation, you'd get the latest sample from the recorder
                val stats = recorder.getRecordingStats()
                
                // Use a mock GSR value based on recording activity
                // In real implementation, extract from actual data stream
                val mockGsrValue = if (stats.totalSamplesRecorded > 0) {
                    10.0f + (System.currentTimeMillis() % 1000) / 100.0f // Mock varying value
                } else {
                    0.0f
                }
                
                previewStreamer.updateGsrValue(mockGsrValue)
                Log.v(TAG, "Updated GSR value: $mockGsrValue µS")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error polling GSR data", e)
        }
    }

    private suspend fun updateRecordingStatus() {
        try {
            val recordingController = recordingService.getRecordingController()
            val status = when {
                recordingController.isRecording -> "RECORDING"
                recordingService.isConnectedToPC() -> "CONNECTED"
                else -> "IDLE"
            }
            
            previewStreamer.updateRecordingStatus(status)
            Log.v(TAG, "Updated recording status: $status")
        } catch (e: Exception) {
            Log.w(TAG, "Error updating recording status", e)
        }
    }

    /**
     * Manual update methods for direct sensor integration
     */
    fun updateRgbFrame(bitmap: Bitmap) {
        previewStreamer.updateRgbFrame(bitmap)
    }

    fun updateThermalFrameDirect(bitmap: Bitmap) {
        previewStreamer.updateThermalFrame(bitmap)  
    }

    fun updateGsrValueDirect(gsrValue: Float) {
        previewStreamer.updateGsrValue(gsrValue)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopDataPolling()
        scope.cancel()
        Log.i(TAG, "PreviewDataAdapter cleaned up")
    }
}