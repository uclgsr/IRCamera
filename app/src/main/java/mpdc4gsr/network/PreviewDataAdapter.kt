package mpdc4gsr.network

import android.graphics.Bitmap
import com.mpdc4gsr.module.thermalunified.lite.camera.CameraPreviewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mpdc4gsr.core.RecordingService
import mpdc4gsr.sensors.gsr.GSRSensorRecorder
import java.util.concurrent.atomic.AtomicReference


class PreviewDataAdapter(
    private val previewStreamer: PreviewStreamer,
    private val recordingService: RecordingService
) {
    companion object {
        private const val TAG = "PreviewDataAdapter"
        private const val POLLING_INTERVAL_MS = 500L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var isRunning = false


    private val thermalCameraManager = AtomicReference<CameraPreviewManager?>()
    private val gsrRecorder = AtomicReference<GSRSensorRecorder?>()


    fun startDataPolling() {
        if (isRunning) {            return
        }        isRunning = true

        pollingJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    pollSensorData()
                    delay(POLLING_INTERVAL_MS)
                } catch (e: Exception) {                    delay(1000)
                }
            }
        }
    }


    fun stopDataPolling() {
        if (!isRunning) {
            return
        }        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
    }


    fun setThermalCameraManager(manager: CameraPreviewManager?) {
        thermalCameraManager.set(manager)"set" else "cleared"}")
    }


    fun setGsrRecorder(recorder: GSRSensorRecorder?) {
        gsrRecorder.set(recorder)"set" else "cleared"}")
    }

    private suspend fun pollSensorData() {

        pollThermalFrame()


        pollGsrData()


        updateRecordingStatus()
    }

    private suspend fun pollThermalFrame() {
        try {
            val manager = thermalCameraManager.get()
            if (manager != null) {

                val thermalBitmap = manager.scaledBitmap()
                if (thermalBitmap != null && !thermalBitmap.isRecycled) {
                    previewStreamer.updateThermalFrame(thermalBitmap)                }
            }
        } catch (e: Exception) {        }
    }

    private suspend fun pollGsrData() {
        try {
            val recorder = gsrRecorder.get()
            if (recorder != null && recorder.isRecording) {


                val stats = recorder.getRecordingStats()

                // Use a mock GSR value based on recording activity
                // In real implementation, extract from actual data stream
                val mockGsrValue = if (stats.totalSamplesRecorded > 0) {
                    10.0f + (System.currentTimeMillis() % 1000) / 100.0f // Mock varying value
                } else {
                    0.0f
                }

                previewStreamer.updateGsrValue(mockGsrValue)            }
        } catch (e: Exception) {        }
    }

    private suspend fun updateRecordingStatus() {
        try {
            val recordingController = recordingService.getRecordingController()
            val status = when {
                recordingController.isRecording -> "RECORDING"
                recordingService.isConnectedToPC -> "CONNECTED"
                else -> "IDLE"
            }

            previewStreamer.updateRecordingStatus(status)        } catch (e: Exception) {        }
    }


    fun updateRgbFrame(bitmap: Bitmap) {
        previewStreamer.updateRgbFrame(bitmap)
    }

    fun updateThermalFrameDirect(bitmap: Bitmap) {
        previewStreamer.updateThermalFrame(bitmap)
    }

    fun updateGsrValueDirect(gsrValue: Float) {
        previewStreamer.updateGsrValue(gsrValue)
    }


    fun cleanup() {
        stopDataPolling()
        scope.cancel()    }
}
