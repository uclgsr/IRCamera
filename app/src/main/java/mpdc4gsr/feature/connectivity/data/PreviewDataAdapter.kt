package mpdc4gsr.feature.connectivity.data

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import mpdc4gsr.app.runtime.RecordingService
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator
import mpdc4gsr.gsr.model.SessionStateStore

class PreviewDataAdapter(
    private val previewStreamer: PreviewStreamer,
    private val recordingService: RecordingService,
    private val sessionStateStore: SessionStateStore,
    private val thermalCoordinator: ThermalCaptureCoordinator,
    private val pollingDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val POLLING_INTERVAL_MS = 500L
    }

    private val scope = CoroutineScope(SupervisorJob() + pollingDispatcher)
    private var pollingJob: Job? = null
    private var thermalPreviewJob: Job? = null

    @Volatile
    private var isRunning = false

    fun startDataPolling() {
        if (isRunning) return
        isRunning = true
        thermalPreviewJob =
            scope.launch {
                thermalCoordinator
                    .previewFlow()
                    .collectLatest { frame ->
                        if (!isRunning) return@collectLatest
                        frame?.bitmap
                            ?.takeUnless { it.isRecycled }
                            ?.let(previewStreamer::updateThermalFrame)
                    }
            }
        pollingJob =
            scope.launch {
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
        thermalPreviewJob?.cancel()
        thermalPreviewJob = null
    }

    private suspend fun pollSensorData() {
        pollGsrData()
        updateRecordingStatus()
    }

    private suspend fun pollGsrData() {
        try {
            val telemetry =
                sessionStateStore.deviceTelemetry.value.values.firstOrNull { it.gsrMicrosiemens != null }
            val gsrValue = telemetry?.gsrMicrosiemens
            if (gsrValue != null && gsrValue > 0f) {
                previewStreamer.updateGsrValue(gsrValue)
            }
        } catch (exception: Throwable) {
            mpdc4gsr.core.common.AppLogger.e(
                "PreviewDataAdapter",
                "Unexpected Throwable in PreviewDataAdapter catch block",
                exception,
            )
        }
    }

    private suspend fun updateRecordingStatus() {
        try {
            val controller = recordingService.getRecordingController()
            val status =
                when {
                    controller.isRecording -> "RECORDING"
                    recordingService.isConnectedToPC -> "CONNECTED"
                    else -> "IDLE"
                }

            previewStreamer.updateRecordingStatus(status)
        } catch (exception: Throwable) {
            mpdc4gsr.core.common.AppLogger.e(
                "PreviewDataAdapter",
                "Unexpected Throwable in PreviewDataAdapter catch block",
                exception,
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
