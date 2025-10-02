package mpdc4gsr.feature.network.data

import android.graphics.Bitmap
import android.util.Log
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
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
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
                    delay(1000)
                }
            }
        }
    }


    fun stopDataPolling() {
        if (!isRunning) {
            return
        }

        Log.i(TAG, "Stopping sensor data polling")
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
    }


    fun setThermalCameraManager(manager: CameraPreviewManager?) {
        thermalCameraManager.set(manager)
        Log.d(TAG, "Thermal camera manager ${if (manager != null) "set" else "cleared"}")
    }


    fun setGsrRecorder(recorder: GSRSensorRecorder?) {
        gsrRecorder.set(recorder)
        Log.d(TAG, "GSR recorder ${if (recorder != null) "set" else "cleared"}")
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
                    previewStreamer.updateThermalFrame(thermalBitmap)
                    Log.v(
                        TAG,
                        "Updated thermal frame: ${thermalBitmap.width}x${thermalBitmap.height}"
                    )
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


                val stats = recorder.getRecordingStats()

                val mockGsrValue = if (stats.totalSamplesRecorded > 0) {
                    10.0f + (System.currentTimeMillis() % 1000) / 100.0f
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
                recordingService.isConnectedToPC -> "CONNECTED"
                else -> "IDLE"
            }

            previewStreamer.updateRecordingStatus(status)
            Log.v(TAG, "Updated recording status: $status")
        } catch (e: Exception) {
            Log.w(TAG, "Error updating recording status", e)
        }
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
        scope.cancel()
        Log.i(TAG, "PreviewDataAdapter cleaned up")
    }
}
