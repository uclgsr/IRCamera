package mpdc4gsr.gsr.capture

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.recording.AudioRecorder
import mpdc4gsr.gsr.recording.GsrRecorder
import mpdc4gsr.gsr.recording.CoordinatorThermalRecorder
import mpdc4gsr.gsr.recording.Recorder
import mpdc4gsr.gsr.recording.VideoRecorder
import mpdc4gsr.gsr.session.TimelineClock
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator

/**
 * Simple factory that wires concrete recorder implementations. Allows toggling between hardware
 * and simulation pipelines without polluting the higher level coordinator.
 */
class RecorderFactory(
    private val appContext: Context,
    private val lifecycleOwnerProvider: () -> LifecycleOwner = { ProcessLifecycleOwner.get() },
    private val ioDispatcher: CoroutineDispatcher,
    private val shimmerController: ShimmerDeviceController,
    private val simulationSource: SimulationSource,
    private val timelineClock: TimelineClock,
    private val thermalCoordinator: ThermalCaptureCoordinator,
) {

    fun create(kind: RecorderKind): Recorder =
        when (kind) {
            RecorderKind.GSR ->
                GsrRecorder(appContext, ioDispatcher, shimmerController, simulationSource, timelineClock)
            RecorderKind.RGB_VIDEO ->
                VideoRecorder(appContext, lifecycleOwnerProvider(), ioDispatcher, timelineClock)
            RecorderKind.THERMAL_VIDEO ->
                CoordinatorThermalRecorder(thermalCoordinator)
            RecorderKind.AUDIO ->
                AudioRecorder(appContext, ioDispatcher, timelineClock)
        }
}
