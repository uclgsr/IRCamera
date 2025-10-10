package mpdc4gsr.feature.network.data

import android.graphics.Bitmap
import com.mpdc4gsr.module.thermalunified.tools.CameraPreviewManager
import kotlinx.coroutines.CoroutineDispatcher
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
    private val recordingService: RecordingService,
    private val pollingDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val POLLING_INTERVAL_MS = 500L
    }

    private val scope = CoroutineScope(SupervisorJob() + pollingDispatcher)
    private val thermalCameraManager = AtomicReference<CameraPreviewManager?>()
    private val gsrRecorder = AtomicReference<GSRSensorRecorder?>()
    private var pollingJob: Job? = null

    @Volatile
    private var isRunning = false

    fun startDataPolling() {
        if (isRunning) return
        isRunning = true
        pollingJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    pollSensorData()
                } catch (_: Throwable) {
                    delay(POLLING_INTERVAL_MS * 2)
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    fun stopDataPolling() {
        if (!isRunning) return
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setThermalCameraManager(manager: CameraPreviewManager?) {
        thermalCameraManager.set(manager)
    }

    fun setGsrRecorder(recorder: GSRSensorRecorder?) {
        gsrRecorder.set(recorder)
    }

    private suspend fun pollSensorData() {
        pollThermalFrame()
        pollGsrData()
        updateRecordingStatus()
    }

    private suspend fun pollThermalFrame() {
        val manager = thermalCameraManager.get() ?: return
        try {
            manager.scaledBitmap()
                ?.takeUnless { it.isRecycled }
                ?.let(previewStreamer::updateThermalFrame)
        } catch (exception: Throwable) {
            mpdc4gsr.core.utils.AppLogger.e(
                "PreviewDataAdapter",
                "Unexpected Throwable in PreviewDataAdapter catch block",
                exception
            )
        }
    }

    private suspend fun pollGsrData() {
        val recorder = gsrRecorder.get() ?: return
        if (!recorder.isRecording) return
        try {
            val gsrValue =
                recorder.currentGsrValue()
                    ?.toFloat()
                    ?: recorder.latestSample.value?.gsrMicrosiemens?.toFloat()
                    ?: 0f
            if (gsrValue > 0f) {
                previewStreamer.updateGsrValue(gsrValue)
            }
        } catch (exception: Throwable) {
            mpdc4gsr.core.utils.AppLogger.e(
                "PreviewDataAdapter",
                "Unexpected Throwable in PreviewDataAdapter catch block",
                exception
            )
        }
    }

    private suspend fun updateRecordingStatus() {
        try {
            val controller = recordingService.getRecordingController()
            val status = when {
                controller.isRecording -> "RECORDING"
                recordingService.isConnectedToPC -> "CONNECTED"
                else -> "IDLE"
            }
            previewStreamer.updateRecordingStatus(status)
        } catch (exception: Throwable) {
            mpdc4gsr.core.utils.AppLogger.e(
                "PreviewDataAdapter",
                "Unexpected Throwable in PreviewDataAdapter catch block",
                exception
            )
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
    }
}
